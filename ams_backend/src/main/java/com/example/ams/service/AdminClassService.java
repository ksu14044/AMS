package com.example.ams.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.ClassEnrollment;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.user.Subject;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.domain.user.UserStatus;
import com.example.ams.event.ClassEnrollmentCreatedEvent;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.ClazzRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class AdminClassService {

	private final ClazzRepository clazzRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;
	private final CurrentUserService currentUserService;
	private final ApplicationEventPublisher eventPublisher;

	public AdminClassService(
			ClazzRepository clazzRepository,
			ClassEnrollmentRepository enrollmentRepository,
			UserRepository userRepository,
			CurrentUserService currentUserService,
			ApplicationEventPublisher eventPublisher) {
		this.clazzRepository = clazzRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.currentUserService = currentUserService;
		this.eventPublisher = eventPublisher;
	}

	public List<Clazz> listClasses() {
		long academyId = currentUserService.requireAcademyId();
		return clazzRepository.findByAcademyId(academyId);
	}

	@Transactional
	public Clazz createClass(Subject subject, String name, long homeroomTeacherId, String classroom) {
		long academyId = currentUserService.requireAcademyId();
		if (clazzRepository.existsByAcademyIdAndName(academyId, name)) {
			throw new BusinessException(ErrorCode.CLASS_NAME_ALREADY_EXISTS);
		}
		User teacher = requireHomeroomTeacher(homeroomTeacherId, subject, academyId);
		return clazzRepository.insert(academyId, subject, name, teacher.userId(), classroom);
	}

	@Transactional
	public Clazz updateClass(long classId, Subject subject, String name, long homeroomTeacherId, String classroom) {
		Clazz existing = requireClassInAcademy(classId);
		long academyId = existing.academyId();
		if (clazzRepository.existsByAcademyIdAndNameExcluding(academyId, name, classId)) {
			throw new BusinessException(ErrorCode.CLASS_NAME_ALREADY_EXISTS);
		}
		User teacher = requireHomeroomTeacher(homeroomTeacherId, subject, academyId);
		return clazzRepository.update(classId, subject, name, teacher.userId(), classroom);
	}

	public List<ClassEnrollment> listEnrollments(long classId) {
		Clazz clazz = requireClassInAcademy(classId);
		return enrollmentRepository.findByClassId(clazz.classId());
	}

	@Transactional
	public ClassEnrollment enrollStudent(long classId, long studentId) {
		Clazz clazz = requireClassInAcademy(classId);
		long adminId = currentUserService.requireUserId();
		User student = requireActiveStudent(studentId, clazz.academyId());
		if (enrollmentRepository.existsByClassIdAndStudentId(clazz.classId(), student.userId())) {
			throw new BusinessException(ErrorCode.ALREADY_ENROLLED);
		}
		ClassEnrollment enrollment = enrollmentRepository.insert(clazz.classId(), student.userId(), adminId);
		eventPublisher.publishEvent(new ClassEnrollmentCreatedEvent(clazz.classId(), student.userId()));
		return enrollment;
	}

	@Transactional
	public void unenroll(long enrollmentId) {
		ClassEnrollment enrollment = enrollmentRepository.findById(enrollmentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.ENROLLMENT_NOT_FOUND));
		requireClassInAcademy(enrollment.classId());
		enrollmentRepository.deleteById(enrollmentId);
	}

	public List<User> listActiveStudents() {
		long academyId = currentUserService.requireAcademyId();
		return userRepository.findActiveStudentsByAcademyId(academyId);
	}

	public List<User> listHomeroomTeachers() {
		long academyId = currentUserService.requireAcademyId();
		return userRepository.findActiveHomeroomTeachersByAcademyId(academyId);
	}

	private Clazz requireClassInAcademy(long classId) {
		Clazz clazz = clazzRepository.findById(classId)
				.orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
		currentUserService.assertSameAcademy(clazz.academyId());
		return clazz;
	}

	private User requireHomeroomTeacher(long teacherId, Subject classSubject, long academyId) {
		User teacher = userRepository.findById(teacherId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		if (teacher.academyId() != academyId
				|| teacher.status() != UserStatus.ACTIVE
				|| !teacher.role().isHomeroomTeacher()
				|| teacher.subject() != classSubject) {
			throw new BusinessException(ErrorCode.INVALID_HOMEROOM_TEACHER);
		}
		return teacher;
	}

	private User requireActiveStudent(long studentId, long academyId) {
		User student = userRepository.findById(studentId)
				.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		if (student.academyId() != academyId
				|| student.status() != UserStatus.ACTIVE
				|| student.role() != UserRole.STUDENT) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "활성 학생만 배정할 수 있습니다.");
		}
		return student;
	}
}
