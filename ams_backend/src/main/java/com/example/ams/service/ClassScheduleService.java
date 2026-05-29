package com.example.ams.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.api.dto.ScheduleSlotRequest;
import com.example.ams.domain.clazz.ClassScheduleSlot;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.repository.ClassScheduleRepository;

@Service
public class ClassScheduleService {

	private final ClassScheduleRepository scheduleRepository;
	private final ClassAccessService classAccessService;

	public ClassScheduleService(ClassScheduleRepository scheduleRepository, ClassAccessService classAccessService) {
		this.scheduleRepository = scheduleRepository;
		this.classAccessService = classAccessService;
	}

	public List<ClassScheduleSlot> getSchedule(long classId) {
		classAccessService.requireReadableClass(classId);
		return scheduleRepository.findByClassId(classId);
	}

	@Transactional
	public List<ClassScheduleSlot> replaceSchedule(long classId, List<ScheduleSlotRequest> slots) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);

		for (ScheduleSlotRequest slot : slots) {
			if (!slot.endTime().isAfter(slot.startTime())) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, "종료 시간은 시작 시간보다 이후여야 합니다.");
			}
		}

		scheduleRepository.deleteByClassId(classId);
		for (ScheduleSlotRequest slot : slots) {
			scheduleRepository.insert(
					classId,
					slot.dayOfWeek(),
					slot.startTime(),
					slot.endTime(),
					slot.room());
		}
		return scheduleRepository.findByClassId(classId);
	}
}
