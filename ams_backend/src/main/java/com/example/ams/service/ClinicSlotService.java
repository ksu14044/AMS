package com.example.ams.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.common.WeekStartDateValidator;
import com.example.ams.domain.clazz.AssignmentEntityType;
import com.example.ams.domain.clazz.ClinicReservation;
import com.example.ams.domain.clazz.ClinicSlotOccurrence;
import com.example.ams.domain.clazz.ClinicSlot;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.DayOfWeek;
import com.example.ams.domain.user.User;
import com.example.ams.event.ClinicSlotUpdatedEvent;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.ClinicSlotRepository;
import com.example.ams.repository.ClinicWeekRepository;
import com.example.ams.repository.UserRepository;

@Service
public class ClinicSlotService {

	private final ClinicSlotRepository clinicSlotRepository;
	private final ClinicWeekRepository clinicWeekRepository;
	private final ClinicReservationRepository clinicReservationRepository;
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final AssignmentTargetService assignmentTargetService;
	private final ApplicationEventPublisher eventPublisher;

	public ClinicSlotService(
			ClinicSlotRepository clinicSlotRepository,
			ClinicWeekRepository clinicWeekRepository,
			ClinicReservationRepository clinicReservationRepository,
			UserRepository userRepository,
			ClassAccessService classAccessService,
			AssignmentTargetService assignmentTargetService,
			ApplicationEventPublisher eventPublisher) {
		this.clinicSlotRepository = clinicSlotRepository;
		this.clinicWeekRepository = clinicWeekRepository;
		this.clinicReservationRepository = clinicReservationRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
		this.assignmentTargetService = assignmentTargetService;
		this.eventPublisher = eventPublisher;
	}

	public List<ClinicSlot> listSlots(long classId, LocalDate weekStart) {
		classAccessService.requireClinicReadableClass(classId);
		LocalDate monday = WeekStartDateValidator.requireMonday(weekStart);
		return clinicSlotRepository.findByClassIdAndWeekStart(classId, monday);
	}

	public List<User> listAssistants(long classId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		return userRepository.findActiveAssistantsByAcademyId(clazz.academyId());
	}

	@Transactional
	public ClinicSlot createSlot(
			long classId,
			LocalDate weekStart,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			Long assistantId,
			int maxCapacity,
			List<Long> targetStudentIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		return insertSlot(classId, weekStart, dayOfWeek, startTime, assistantId, maxCapacity, null, targetStudentIds);
	}

	@Transactional
	public ClinicSlot createSlotForLessonRecord(
			long classId,
			long lessonRecordId,
			LocalDate clinicDate,
			LocalTime startTime,
			Long assistantId,
			int maxCapacity,
			List<Long> targetStudentIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireEditClassContent(clazz);
		DayOfWeek dayOfWeek = toDomainDayOfWeek(clinicDate);
		LocalDate monday = clinicDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
		clinicWeekRepository.ensureOpenWeek(classId, monday);
		return insertSlot(classId, monday, dayOfWeek, startTime, assistantId, maxCapacity, lessonRecordId, targetStudentIds);
	}

	public AssignmentTargetService.TargetView getTargets(long slotId) {
		ClinicSlot slot = clinicSlotRepository.findById(slotId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CLINIC_SLOT_NOT_FOUND));
		classAccessService.requireReadableClass(slot.classId());
		return assignmentTargetService.getTargetView(
				AssignmentEntityType.CLINIC_SLOT, slotId, slot.classId());
	}

	private DayOfWeek toDomainDayOfWeek(LocalDate date) {
		return DayOfWeek.valueOf(date.getDayOfWeek().name().substring(0, 3));
	}

	private ClinicSlot insertSlot(
			long classId,
			LocalDate weekStart,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			Long assistantId,
			int maxCapacity,
			Long lessonRecordId,
			List<Long> targetStudentIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		validateCapacity(maxCapacity);
		validateAssistant(clazz.academyId(), assistantId);
		LocalDate monday = WeekStartDateValidator.requireMonday(weekStart);
		try {
			ClinicSlot slot = clinicSlotRepository.insert(
					classId,
					monday,
					dayOfWeek,
					startTime,
					assistantId,
					maxCapacity,
					lessonRecordId);
			assignmentTargetService.applyOnCreate(
					AssignmentEntityType.CLINIC_SLOT, slot.slotId(), classId, targetStudentIds);
			return slot;
		} catch (DuplicateKeyException ex) {
			throw duplicateSlotException();
		}
	}

	@Transactional
	public ClinicSlot updateSlot(
			long classId,
			long slotId,
			DayOfWeek dayOfWeek,
			LocalTime startTime,
			Long assistantId,
			int maxCapacity) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		requireSlot(classId, slotId);
		validateCapacity(maxCapacity);
		validateAssistant(clazz.academyId(), assistantId);
		var affectedStudents = clinicReservationRepository.findBySlotId(slotId).stream()
				.map(ClinicReservation::studentId)
				.toList();
		try {
			ClinicSlot updated = clinicSlotRepository.update(slotId, classId, dayOfWeek, startTime, assistantId, maxCapacity);
			if (!affectedStudents.isEmpty()) {
				String slotLabel = NotificationMessages.clinicSlotLabel(dayOfWeek, startTime);
				eventPublisher.publishEvent(new ClinicSlotUpdatedEvent(classId, slotId, slotLabel, affectedStudents));
			}
			return updated;
		} catch (DuplicateKeyException ex) {
			throw duplicateSlotException();
		}
	}

	@Transactional
	public void deleteSlot(long classId, long slotId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		requireSlot(classId, slotId);
		clinicSlotRepository.delete(slotId, classId);
	}

	@Transactional
	public void deleteSlotIfAllowed(long classId, long slotId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireEditClassContent(clazz);
		requireSlot(classId, slotId);
		if (clinicReservationRepository.countBySlotId(slotId) > 0) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_LOCKED, "예약이 있는 클리닉 슬롯은 삭제할 수 없습니다.");
		}
		assignmentTargetService.clearTargets(AssignmentEntityType.CLINIC_SLOT, slotId);
		clinicSlotRepository.delete(slotId, classId);
	}

	@Transactional
	public ClinicSlot updateSlotForLessonRecord(
			long classId,
			long slotId,
			LocalDate clinicDate,
			LocalTime startTime,
			Long assistantId,
			int maxCapacity,
			List<Long> targetStudentIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireEditClassContent(clazz);
		ClinicSlot existing = requireSlot(classId, slotId);
		validateCapacity(maxCapacity);
		validateAssistant(clazz.academyId(), assistantId);
		if (clinicReservationRepository.countBySlotId(slotId) > 0
				&& scheduleChanged(existing, clinicDate, startTime)) {
			throw new BusinessException(
					ErrorCode.LESSON_RECORD_LINK_LOCKED,
					"예약이 있는 슬롯은 날짜·시간을 변경할 수 없습니다.");
		}
		DayOfWeek dayOfWeek = toDomainDayOfWeek(clinicDate);
		LocalDate monday = clinicDate.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
		clinicWeekRepository.ensureOpenWeek(classId, monday);
		try {
			ClinicSlot updated = clinicSlotRepository.updateSchedule(
					slotId, classId, monday, dayOfWeek, startTime, assistantId, maxCapacity);
			assignmentTargetService.updateTargets(
					AssignmentEntityType.CLINIC_SLOT, slotId, classId, targetStudentIds);
			return updated;
		} catch (DuplicateKeyException ex) {
			throw duplicateSlotException();
		}
	}

	private static boolean scheduleChanged(ClinicSlot slot, LocalDate clinicDate, LocalTime startTime) {
		LocalDate slotDate = ClinicSlotOccurrence.slotDate(slot.weekStartDate(), slot.dayOfWeek());
		return !slotDate.equals(clinicDate) || !slot.startTime().equals(startTime);
	}

	private ClinicSlot requireSlot(long classId, long slotId) {
		return clinicSlotRepository.findByIdAndClassId(slotId, classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CLINIC_SLOT_NOT_FOUND));
	}

	private void validateCapacity(int maxCapacity) {
		if (maxCapacity < 1 || maxCapacity > 20) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "정원은 1~20명입니다.");
		}
	}

	private BusinessException duplicateSlotException() {
		return new BusinessException(
				ErrorCode.INVALID_REQUEST,
				"같은 주·요일·시간·조교 조합의 슬롯이 이미 있습니다.");
	}

	private void validateAssistant(long academyId, Long assistantId) {
		if (assistantId == null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "조교를 지정해 주세요.");
		}
		User assistant = userRepository.findById(assistantId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "조교를 찾을 수 없습니다."));
		if (assistant.academyId() != academyId || !assistant.role().isAssistant()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "해당 학원의 조교만 배정할 수 있습니다.");
		}
	}
}
