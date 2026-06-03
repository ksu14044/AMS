package com.example.ams.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.ams.api.dto.ClassResponse;
import com.example.ams.api.dto.DiligenceReportListResponse;
import com.example.ams.api.dto.ParentChildResponse;
import com.example.ams.api.dto.ParentLinkResponse;
import com.example.ams.api.dto.PendingTaskResponse;
import com.example.ams.api.dto.StudyRecordResponse;
import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.link.ParentStudentLink;
import com.example.ams.domain.report.DiligenceReport;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.ClazzRepository;
import com.example.ams.repository.DiligenceReportRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class ParentPortalService {

	private final CurrentUserService currentUserService;
	private final ParentStudentLinkService linkService;
	private final UserRepository userRepository;
	private final ClazzRepository clazzRepository;
	private final StudyRecordService studyRecordService;
	private final PendingTaskService pendingTaskService;
	private final DiligenceReportRepository reportRepository;

	public ParentPortalService(
			CurrentUserService currentUserService,
			ParentStudentLinkService linkService,
			UserRepository userRepository,
			ClazzRepository clazzRepository,
			StudyRecordService studyRecordService,
			PendingTaskService pendingTaskService,
			DiligenceReportRepository reportRepository) {
		this.currentUserService = currentUserService;
		this.linkService = linkService;
		this.userRepository = userRepository;
		this.clazzRepository = clazzRepository;
		this.studyRecordService = studyRecordService;
		this.pendingTaskService = pendingTaskService;
		this.reportRepository = reportRepository;
	}

	public List<ParentChildResponse> listChildren() {
		long parentId = requireParentId();
		List<ParentChildResponse> result = new ArrayList<>();
		for (ParentStudentLink link : linkService.listLinksForCurrentParent()) {
			User student = userRepository.findById(link.studentId()).orElseThrow();
			List<ClassResponse> classes = clazzRepository.findByStudentId(link.studentId()).stream()
					.map(ClassResponse::from)
					.toList();
			result.add(new ParentChildResponse(student.userId(), student.name(), classes));
		}
		return result;
	}

	public List<PendingTaskResponse> listPendingTasks(long studentId) {
		long parentId = requireParentId();
		linkService.requireParentLinkedToStudent(parentId, studentId);
		return pendingTaskService.listPending(studentId).stream()
				.map(PendingTaskResponse::from)
				.toList();
	}

	public StudyRecordResponse getStudyRecord(long studentId, long classId) {
		long parentId = requireParentId();
		linkService.requireParentCanAccessClass(parentId, studentId, classId);
		return studyRecordService.buildRecordForParentView(classId, studentId);
	}

	public List<DiligenceReportListResponse> listReports(long studentId) {
		long parentId = requireParentId();
		linkService.requireParentLinkedToStudent(parentId, studentId);
		User student = userRepository.findById(studentId).orElseThrow();
		List<DiligenceReportListResponse> items = new ArrayList<>();
		for (Clazz clazz : clazzRepository.findByStudentId(studentId)) {
			for (DiligenceReport report : reportRepository.findByClassIdAndStudentId(clazz.classId(), studentId)) {
				String title = report.periodLabel() != null && !report.periodLabel().isBlank()
						? report.periodLabel()
						: "성실도 보고서";
				items.add(DiligenceReportListResponse.from(new DiligenceReportService.ReportListRow(
						report.reportId(),
						report.testId(),
						title,
						report.periodLabel(),
						studentId,
						student.name(),
						report.periodStart(),
						report.periodEnd(),
						report.totalScore(),
						report.overallGrade(),
						report.createdAt())));
			}
		}
		items.sort((a, b) -> b.periodEnd().compareTo(a.periodEnd()));
		return items;
	}

	public ParentLinkResponse createLink(String parentEmail, long studentId) {
		ParentStudentLink link = linkService.createLink(parentEmail, studentId);
		return toLinkResponse(link);
	}

	public List<ParentLinkResponse> listLinksForStudentAsStaff(long studentId) {
		return linkService.listLinksForStudent(studentId).stream()
				.map(this::toLinkResponse)
				.toList();
	}

	private ParentLinkResponse toLinkResponse(ParentStudentLink link) {
		User parent = userRepository.findById(link.parentId()).orElseThrow();
		return new ParentLinkResponse(link.linkId(), parent.userId(), parent.name(), parent.email(), link.linkedAt());
	}

	private long requireParentId() {
		if (currentUserService.requireRole() != UserRole.PARENT) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return currentUserService.requireUserId();
	}
}
