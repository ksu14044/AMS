package com.example.ams.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ClinicBookingPolicy;
import com.example.ams.common.ErrorCode;
import com.example.ams.common.WeekStartDateValidator;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.ClinicReservation;
import com.example.ams.domain.clazz.ClinicSlot;
import com.example.ams.domain.clazz.ClinicWeek;
import com.example.ams.domain.clazz.ClinicWeekStatus;
import com.example.ams.domain.user.UserRole;
import com.example.ams.event.ClinicResultUpdatedEvent;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.ClinicSlotRepository;
import com.example.ams.repository.ClinicWeekRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class ClinicReservationService {

	private final ClinicWeekRepository weekRepository;
	private final ClinicSlotRepository slotRepository;
	private final ClinicReservationRepository reservationRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final ApplicationEventPublisher eventPublisher;

	public ClinicReservationService(
			ClinicWeekRepository weekRepository,
			ClinicSlotRepository slotRepository,
			ClinicReservationRepository reservationRepository,
			ClassEnrollmentRepository enrollmentRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			ApplicationEventPublisher eventPublisher) {
		this.weekRepository = weekRepository;
		this.slotRepository = slotRepository;
		this.reservationRepository = reservationRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.eventPublisher = eventPublisher;
	}

	public ClinicWeekView getWeekView(long classId, LocalDate weekStart) {
		classAccessService.requireClinicReadableClass(classId);
		LocalDate monday = WeekStartDateValidator.requireMonday(weekStart);
		ClinicWeek week = weekRepository.ensureOpenWeek(classId, monday);
		List<ClinicSlot> slots = slotRepository.findByClassIdAndWeekStart(classId, monday);
		List<Long> slotIds = slots.stream().map(ClinicSlot::slotId).toList();
		List<ClinicReservation> reservations = reservationRepository.findBySlotIds(slotIds);
		Map<Long, List<ClinicReservation>> bySlot = reservations.stream()
				.collect(Collectors.groupingBy(ClinicReservation::slotId));

		long currentStudentId = currentUserService.requireRole() == UserRole.STUDENT
				? currentUserService.requireUserId()
				: -1L;

		boolean bookingOpen = ClinicBookingPolicy.canStudentBook(week.status(), monday);
		Set<String> myBookedTimeKeys = new HashSet<>();
		if (currentStudentId > 0) {
			for (ClinicReservation r : reservations) {
				if (r.studentId() == currentStudentId) {
					ClinicSlot bookedSlot = slots.stream()
							.filter(s -> s.slotId() == r.slotId())
							.findFirst()
							.orElse(null);
					if (bookedSlot != null) {
						myBookedTimeKeys.add(timeKey(bookedSlot));
					}
				}
			}
		}

		List<SlotBookingView> slotViews = new ArrayList<>();
		for (ClinicSlot slot : slots) {
			List<ClinicReservation> slotReservations = bySlot.getOrDefault(slot.slotId(), List.of());
			int booked = slotReservations.size();
			Long myReservationId = null;
			if (currentStudentId > 0) {
				myReservationId = slotReservations.stream()
						.filter(r -> r.studentId() == currentStudentId)
						.map(ClinicReservation::reservationId)
						.findFirst()
						.orElse(null);
			}
			boolean studentTimeConflict = currentStudentId > 0
					&& myReservationId == null
					&& myBookedTimeKeys.contains(timeKey(slot));
			slotViews.add(new SlotBookingView(
					slot,
					booked,
					slot.maxCapacity(),
					booked >= slot.maxCapacity(),
					myReservationId,
					studentTimeConflict,
					slotReservations));
		}

		return new ClinicWeekView(
				monday,
				week.status(),
				bookingOpen,
				ClinicBookingPolicy.isWithinBookingWindow(monday),
				slotViews);
	}

	public MyAssistantClinicWeekView getMyAssistantWeekView(LocalDate weekStart) {
		if (!currentUserService.requireRole().isAssistant()) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		long assistantId = currentUserService.requireUserId();
		long academyId = currentUserService.requireAcademyId();
		LocalDate monday = WeekStartDateValidator.requireMonday(weekStart);

		List<com.example.ams.domain.clazz.AssistantClinicSlotRow> rows =
				slotRepository.findByAssistantIdAndWeekStart(assistantId, monday, academyId);
		if (rows.isEmpty()) {
			return new MyAssistantClinicWeekView(monday, List.of());
		}

		List<Long> slotIds = rows.stream().map(r -> r.slot().slotId()).toList();
		Map<Long, List<ClinicReservation>> bySlot = reservationRepository.findBySlotIds(slotIds).stream()
				.collect(Collectors.groupingBy(ClinicReservation::slotId));

		List<AssistantClinicSlotItem> items = new ArrayList<>();
		for (com.example.ams.domain.clazz.AssistantClinicSlotRow row : rows) {
			ClinicSlot slot = row.slot();
			List<ClinicReservation> slotReservations = bySlot.getOrDefault(slot.slotId(), List.of());
			int booked = slotReservations.size();
			items.add(new AssistantClinicSlotItem(
					slot.classId(),
					row.className(),
					slot,
					booked,
					slot.maxCapacity(),
					booked >= slot.maxCapacity(),
					slotReservations));
		}
		return new MyAssistantClinicWeekView(monday, items);
	}

	@Transactional
	public ClinicWeekView reserveSlot(long classId, long slotId) {
		requireStudent();
		long studentId = currentUserService.requireUserId();
		classAccessService.requireReadableClass(classId);
		if (!enrollmentRepository.existsByClassIdAndStudentId(classId, studentId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		ClinicSlot slot = requireSlotInClass(classId, slotId);
		ClinicWeek week = weekRepository.ensureOpenWeek(classId, slot.weekStartDate());
		if (!ClinicBookingPolicy.canStudentBook(week.status(), slot.weekStartDate())) {
			throw new BusinessException(ErrorCode.CLINIC_WEEK_LOCKED, "예약·변경 기간이 종료되었습니다. (토 23:00 마감)");
		}

		if (reservationRepository.findBySlotIdAndStudentId(slotId, studentId).isPresent()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "이미 예약한 슬롯입니다.");
		}
		if (reservationRepository.countBySlotId(slotId) >= slot.maxCapacity()) {
			throw new BusinessException(ErrorCode.CLINIC_SLOT_FULL);
		}
		if (reservationRepository.existsStudentReservationAtSameTimeExcludingSlot(
				classId,
				studentId,
				slot.weekStartDate(),
				slot.dayOfWeek(),
				slot.startTime(),
				slotId)) {
			throw new BusinessException(
					ErrorCode.CLINIC_TIME_CONFLICT,
					"같은 요일·시간에는 하나의 클리닉만 예약할 수 있습니다.");
		}

		reservationRepository.insert(slotId, studentId);
		return getWeekView(classId, slot.weekStartDate());
	}

	@Transactional
	public ClinicWeekView cancelReservation(long classId, long slotId) {
		requireStudent();
		long studentId = currentUserService.requireUserId();
		classAccessService.requireReadableClass(classId);

		ClinicSlot slot = requireSlotInClass(classId, slotId);
		ClinicWeek week = weekRepository.ensureOpenWeek(classId, slot.weekStartDate());
		if (!ClinicBookingPolicy.canStudentBook(week.status(), slot.weekStartDate())) {
			throw new BusinessException(ErrorCode.CLINIC_WEEK_LOCKED, "예약·변경 기간이 종료되었습니다.");
		}

		ClinicReservation existing = reservationRepository.findBySlotIdAndStudentId(slotId, studentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
		reservationRepository.delete(existing.reservationId());
		return getWeekView(classId, slot.weekStartDate());
	}

	@Transactional
	public ClinicReservation updateResult(long reservationId, Boolean attended, String memo) {
		ClinicReservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
		ClinicSlot slot = slotRepository.findById(reservation.slotId())
				.orElseThrow(() -> new BusinessException(ErrorCode.CLINIC_SLOT_NOT_FOUND));
		Clazz clazz = classAccessService.requireClinicReadableClass(slot.classId());
		requireStaffCanManageResults(clazz, slot);
		ClinicReservation updated = reservationRepository.updateResult(reservationId, attended, memo);
		if (attended != null || (memo != null && !memo.isBlank())) {
			String slotLabel = NotificationMessages.clinicSlotLabel(slot.dayOfWeek(), slot.startTime());
			eventPublisher.publishEvent(new ClinicResultUpdatedEvent(
					slot.classId(), reservationId, reservation.studentId(), slotLabel));
		}
		return updated;
	}

	private ClinicSlot requireSlotInClass(long classId, long slotId) {
		return slotRepository.findByIdAndClassId(slotId, classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CLINIC_SLOT_NOT_FOUND));
	}

	private void requireStudent() {
		if (currentUserService.requireRole() != UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "학생만 예약할 수 있습니다.");
		}
	}

	private void requireStaffCanManageResults(Clazz clazz, ClinicSlot slot) {
		UserRole role = currentUserService.requireRole();
		if (role == UserRole.ACADEMY_ADMIN) {
			return;
		}
		if (role.isHomeroomTeacher() && clazz.homeroomTeacherId() == currentUserService.requireUserId()) {
			return;
		}
		if (role.isAssistant()
				&& slot.assistantId() != null
				&& slot.assistantId() == currentUserService.requireUserId()) {
			return;
		}
		throw new BusinessException(ErrorCode.FORBIDDEN);
	}

	public record ClinicWeekView(
			LocalDate weekStart,
			ClinicWeekStatus weekStatus,
			boolean bookingOpen,
			boolean withinBookingWindow,
			List<SlotBookingView> slots) {
	}

	public record SlotBookingView(
			ClinicSlot slot,
			int bookedCount,
			int maxCapacity,
			boolean full,
			Long myReservationId,
			boolean studentTimeConflict,
			List<ClinicReservation> reservations) {
	}

	public record MyAssistantClinicWeekView(LocalDate weekStart, List<AssistantClinicSlotItem> slots) {
	}

	public record AssistantClinicSlotItem(
			long classId,
			String className,
			ClinicSlot slot,
			int bookedCount,
			int maxCapacity,
			boolean full,
			List<ClinicReservation> reservations) {
	}

	private static String timeKey(ClinicSlot slot) {
		return slot.dayOfWeek().name() + "|" + slot.startTime().toString();
	}
}
