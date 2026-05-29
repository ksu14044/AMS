package com.example.ams.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.common.WeekStartDateValidator;
import com.example.ams.domain.clazz.ClinicReservation;
import com.example.ams.domain.clazz.ClinicSlot;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.DayOfWeek;
import com.example.ams.domain.user.User;
import com.example.ams.event.ClinicSlotUpdatedEvent;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.ClinicSlotRepository;
import com.example.ams.repository.UserRepository;

@Service
public class ClinicSlotService {

	private final ClinicSlotRepository clinicSlotRepository;
	private final ClinicReservationRepository clinicReservationRepository;
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final ApplicationEventPublisher eventPublisher;

	public ClinicSlotService(
			ClinicSlotRepository clinicSlotRepository,
			ClinicReservationRepository clinicReservationRepository,
			UserRepository userRepository,
			ClassAccessService classAccessService,
			ApplicationEventPublisher eventPublisher) {
		this.clinicSlotRepository = clinicSlotRepository;
		this.clinicReservationRepository = clinicReservationRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
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
			int maxCapacity) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);
		validateClinicDay(dayOfWeek);
		validateCapacity(maxCapacity);
		LocalDate monday = WeekStartDateValidator.requireMonday(weekStart);
		validateAssistant(clazz.academyId(), assistantId);
		try {
			return clinicSlotRepository.insert(classId, monday, dayOfWeek, startTime, assistantId, maxCapacity);
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
		validateClinicDay(dayOfWeek);
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

	private ClinicSlot requireSlot(long classId, long slotId) {
		return clinicSlotRepository.findByIdAndClassId(slotId, classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CLINIC_SLOT_NOT_FOUND));
	}

	private void validateClinicDay(DayOfWeek dayOfWeek) {
		if (dayOfWeek == DayOfWeek.SAT || dayOfWeek == DayOfWeek.SUN) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "클리닉은 월~금만 등록할 수 있습니다.");
		}
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
