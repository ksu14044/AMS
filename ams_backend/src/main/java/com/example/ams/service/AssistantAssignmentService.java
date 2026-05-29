package com.example.ams.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.AssistantClassAssignmentRepository;
import com.example.ams.repository.AssistantClassAssignmentRepository.AssignedAssistant;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class AssistantAssignmentService {

	private final AssistantClassAssignmentRepository assignmentRepository;
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;

	public AssistantAssignmentService(
			AssistantClassAssignmentRepository assignmentRepository,
			UserRepository userRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService) {
		this.assignmentRepository = assignmentRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
	}

	public ClassAssistantsView getAssistantsForClass(long classId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		List<AssignedAssistant> assigned = assignmentRepository.findAssignedByClassId(classId);
		List<AssistantOption> available = List.of();
		if (classAccessService.canManageClassContent(clazz)) {
			available = userRepository.findActiveAssistantsByAcademyId(clazz.academyId()).stream()
					.filter(u -> matchesClassSubject(u, clazz.subject()))
					.map(u -> new AssistantOption(u.userId(), u.name(), u.role()))
					.toList();
		}
		return new ClassAssistantsView(assigned, available);
	}

	@Transactional
	public ClassAssistantsView replaceAssignments(long classId, List<Long> assistantIds) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireManageClassContent(clazz);

		Set<Long> unique = new LinkedHashSet<>();
		if (assistantIds != null) {
			for (Long id : assistantIds) {
				if (id != null) {
					unique.add(id);
				}
			}
		}

		for (Long assistantId : unique) {
			validateAssistantForClass(clazz, assistantId);
		}

		long assignedBy = currentUserService.requireUserId();
		assignmentRepository.deleteByClassId(classId);
		for (Long assistantId : unique) {
			assignmentRepository.insert(classId, assistantId, assignedBy);
		}
		return getAssistantsForClass(classId);
	}

	private void validateAssistantForClass(Clazz clazz, long assistantId) {
		User assistant = userRepository.findById(assistantId)
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "조교를 찾을 수 없습니다."));
		if (assistant.academyId() != clazz.academyId() || !assistant.role().isAssistant()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "해당 학원의 조교만 배정할 수 있습니다.");
		}
		if (!matchesClassSubject(assistant, clazz.subject())) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "반 과목과 같은 과목 조교만 배정할 수 있습니다.");
		}
	}

	private boolean matchesClassSubject(User assistant, Subject classSubject) {
		return switch (assistant.role()) {
			case ASSISTANT_KO -> classSubject == Subject.KO;
			case ASSISTANT_EN -> classSubject == Subject.EN;
			case ASSISTANT_MATH -> classSubject == Subject.MATH;
			default -> false;
		};
	}

	public record AssistantOption(long assistantId, String name, UserRole role) {
	}

	public record ClassAssistantsView(List<AssignedAssistant> assigned, List<AssistantOption> available) {
	}
}
