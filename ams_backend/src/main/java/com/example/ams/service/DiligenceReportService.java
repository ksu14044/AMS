package com.example.ams.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.config.AmsUploadProperties;
import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.TestExam;
import com.example.ams.domain.clazz.TestScore;
import com.example.ams.api.dto.GenerateReportsRequest;
import com.example.ams.domain.report.DiligenceReport;
import com.example.ams.domain.report.ReportPeriodPreset;
import com.example.ams.domain.user.User;
import com.example.ams.domain.user.UserRole;
import com.example.ams.event.DiligenceReportCreatedEvent;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.ClazzRepository;
import com.example.ams.repository.DiligenceReportRepository;
import com.example.ams.repository.DiligenceReportRepository.ReportInsert;
import com.example.ams.repository.TestExamRepository;
import com.example.ams.repository.TestScoreRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class DiligenceReportService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
	private static final DateTimeFormatter PERIOD_LABEL_FMT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

	private final DiligenceReportRepository reportRepository;
	private final TestExamRepository testExamRepository;
	private final TestScoreRepository testScoreRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final ClazzRepository clazzRepository;
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final StudyRecordPeriodCalculator periodCalculator;
	private final DiligenceReportPdfService pdfService;
	private final AmsUploadProperties uploadProperties;
	private final ApplicationEventPublisher eventPublisher;
	private final ReportPeriodPresetService reportPeriodPresetService;
	private final ParentStudentLinkService parentStudentLinkService;

	public DiligenceReportService(
			DiligenceReportRepository reportRepository,
			TestExamRepository testExamRepository,
			TestScoreRepository testScoreRepository,
			ClassEnrollmentRepository enrollmentRepository,
			ClazzRepository clazzRepository,
			UserRepository userRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			StudyRecordPeriodCalculator periodCalculator,
			DiligenceReportPdfService pdfService,
			AmsUploadProperties uploadProperties,
			ApplicationEventPublisher eventPublisher,
			ReportPeriodPresetService reportPeriodPresetService,
			ParentStudentLinkService parentStudentLinkService) {
		this.reportRepository = reportRepository;
		this.testExamRepository = testExamRepository;
		this.testScoreRepository = testScoreRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.clazzRepository = clazzRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.periodCalculator = periodCalculator;
		this.pdfService = pdfService;
		this.uploadProperties = uploadProperties;
		this.eventPublisher = eventPublisher;
		this.reportPeriodPresetService = reportPeriodPresetService;
		this.parentStudentLinkService = parentStudentLinkService;
	}

	public List<ReportListRow> listByClass(long classId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		requireCanViewReports(clazz);
		List<DiligenceReport> reports = filterReportsForCurrentUser(reportRepository.findByClassId(classId));
		return toListRows(reports);
	}

	public ReportDetailRow getReport(long reportId) {
		DiligenceReport report = requireReadableReport(reportId);
		return toDetailRow(report);
	}

	@Transactional
	public ReportDetailRow updateComment(long reportId, String comment) {
		DiligenceReport report = requireReadableReport(reportId);
		Clazz clazz = clazzRepository.findById(report.classId()).orElseThrow();
		if (!classAccessService.canManageClassContent(clazz)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		reportRepository.updateComment(reportId, comment);
		return toDetailRow(reportRepository.findById(reportId).orElseThrow());
	}

	public Resource loadPdfResource(long reportId) {
		requireReadableReport(reportId);
		java.nio.file.Path path = pdfFilePath(reportId);
		if (!Files.exists(path)) {
			throw new BusinessException(ErrorCode.REPORT_NOT_FOUND, "PDF 파일이 없습니다.");
		}
		return new FileSystemResource(path);
	}

	public PdfArchive loadTestPdfArchive(long classId, long testId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		if (!classAccessService.canManageClassContent(clazz)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		TestExam test = testExamRepository.findById(testId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TEST_NOT_FOUND));
		if (test.classId() != classId) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "해당 반의 테스트가 아닙니다.");
		}
		List<DiligenceReport> reports = reportRepository.findByClassIdAndTestId(classId, testId);
		if (reports.isEmpty()) {
			throw new BusinessException(ErrorCode.REPORT_NOT_FOUND, "이 시험의 보고서가 없습니다.");
		}
		return buildZipArchive(sanitizeArchiveFileName(clazz.name() + "_" + test.title()), reports);
	}

	private java.nio.file.Path pdfFilePath(long reportId) {
		return java.nio.file.Path.of(uploadProperties.dir(), "reports", "report-" + reportId + ".pdf");
	}

	private static String sanitizeArchiveFileName(String name) {
		String cleaned = name.replaceAll("[\\\\/:*?\"<>|]", "_").strip();
		return cleaned.isEmpty() ? "report" : cleaned;
	}

	private static String uniqueZipEntryName(Set<String> used, String candidate) {
		String name = candidate;
		int suffix = 2;
		while (!used.add(name)) {
			int dot = candidate.lastIndexOf('.');
			if (dot > 0) {
				name = candidate.substring(0, dot) + "_" + suffix + candidate.substring(dot);
			} else {
				name = candidate + "_" + suffix;
			}
			suffix++;
		}
		return name;
	}

	public record PdfArchive(String filename, Resource resource) {
	}

	public List<ReportGenerationTargetRow> listGenerationTargets(long classId) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		if (!classAccessService.canManageClassContent(clazz)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		List<ReportGenerationTargetRow> rows = new ArrayList<>();
		for (TestExam test : testExamRepository.findByClassId(classId)) {
			if (test.status() != AssignmentStatus.COMPLETED) {
				continue;
			}
			rows.add(new ReportGenerationTargetRow(
					test.testId(),
					test.title(),
					test.testAt(),
					test.completedAt(),
					reportRepository.existsByTestId(test.testId())));
		}
		return rows;
	}

	@Transactional
	public GenerateReportsV3Result generateReportsForPeriod(long classId, GenerateReportsRequest request) {
		Clazz clazz = clazzRepository.findById(classId).orElseThrow();
		if (!classAccessService.canManageClassContent(clazz)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		Instant periodStart = startOfDay(request.periodStart());
		Instant periodEnd = endOfDay(request.periodEnd());
		if (periodEnd.isBefore(periodStart)) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "기간 종료일은 시작일 이후여야 합니다.");
		}

		List<Long> studentIds = distinctStudentIds(request.studentIds());
		if (studentIds.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "학생을 한 명 이상 선택하세요.");
		}
		for (long studentId : studentIds) {
			if (!enrollmentRepository.existsByClassIdAndStudentId(classId, studentId)) {
				throw new BusinessException(ErrorCode.INVALID_REQUEST, "반에 속하지 않은 학생이 포함되어 있습니다.");
			}
		}

		Long presetId = request.presetId();
		if (presetId != null) {
			reportPeriodPresetService.requirePreset(classId, presetId);
		}
		String periodLabel = resolvePeriodLabel(classId, presetId, request.periodStart(), request.periodEnd());

		reportRepository.deleteByClassIdAndPeriodAndStudentIds(classId, periodStart, periodEnd, studentIds);

		int created = 0;
		for (long studentId : studentIds) {
			createReportForStudent(
					clazz,
					studentId,
					null,
					periodStart,
					periodEnd,
					periodLabel,
					presetId,
					null);
			created++;
		}
		return new GenerateReportsV3Result(created, periodStart, periodEnd, periodLabel);
	}

	public PdfArchive loadPeriodPdfArchive(long classId, LocalDate periodStart, LocalDate periodEnd) {
		return loadPeriodPdfArchive(classId, startOfDay(periodStart), endOfDay(periodEnd));
	}

	public PdfArchive loadPeriodPdfArchive(long classId, Instant periodStart, Instant periodEnd) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		if (!classAccessService.canManageClassContent(clazz)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		List<DiligenceReport> reports = reportRepository.findByClassIdAndPeriod(classId, periodStart, periodEnd);
		if (reports.isEmpty()) {
			throw new BusinessException(ErrorCode.REPORT_NOT_FOUND, "해당 기간의 보고서가 없습니다.");
		}
		String zipBase = sanitizeArchiveFileName(
				clazz.name() + "_"
						+ PERIOD_LABEL_FMT.format(periodStart.atZone(SEOUL).toLocalDate())
						+ "-"
						+ PERIOD_LABEL_FMT.format(periodEnd.atZone(SEOUL).toLocalDate()));
		return buildZipArchive(zipBase, reports);
	}

	@Transactional
	public int generateReportsForTest(long classId, long testId) {
		TestExam test = testExamRepository.findById(testId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TEST_NOT_FOUND));
		if (test.classId() != classId) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "해당 반의 테스트가 아닙니다.");
		}
		Clazz clazz = clazzRepository.findById(classId).orElseThrow();
		if (!classAccessService.canManageClassContent(clazz)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		if (test.status() != AssignmentStatus.COMPLETED) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "완료된 테스트만 보고서를 생성할 수 있습니다.");
		}
		generateForCompletedTest(testId);
		return enrollmentRepository.findByClassId(classId).size();
	}

	@Transactional
	public void generateForCompletedTest(long testId) {
		TestExam test = testExamRepository.findById(testId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TEST_NOT_FOUND));
		Clazz clazz = clazzRepository.findById(test.classId()).orElseThrow();
		Instant periodEnd = resolvePeriodEnd(test);
		Optional<TestExam> previous = testExamRepository.findPreviousCompleted(test.classId(), test.testAt());

		reportRepository.deleteByTestId(testId);

		for (var enrollment : enrollmentRepository.findByClassId(test.classId())) {
			long studentId = enrollment.studentId();
			Instant periodStart = previous.map(TestExam::testAt)
					.orElse(enrollment.assignedAt());
			createReportForStudent(clazz, studentId, test, periodStart, periodEnd, null, null, test.title());
		}
	}

	private void createReportForStudent(
			Clazz clazz,
			long studentId,
			TestExam test,
			Instant periodStart,
			Instant periodEnd,
			String periodLabel,
			Long periodPresetId,
			String pdfTestTitle) {
		StudyRecordPeriodMetrics metrics = periodCalculator.compute(
				clazz.classId(),
				studentId,
				periodStart,
				periodEnd);

		StudyRecordPeriodTestMetrics testMetrics = test != null
				? periodCalculator.computeTestMetricsForSingleExam(test, studentId)
				: periodCalculator.computeTestMetrics(clazz.classId(), studentId, periodStart, periodEnd);
		Long testId = testMetrics.representativeTestId();
		BigDecimal raw = testMetrics.displayRawScore();
		BigDecimal classAvg = testMetrics.latestClassAvg();
		Integer testRank = testMetrics.latestRank();
		int testScorePercent = testMetrics.averageScorePercent();
		String testGrade = testMetrics.hasScoredTest()
				? StudyRecordGrades.letterGrade(testScorePercent)
				: null;

		Integer homeworkRate = metrics.homeworkRate();
		Integer clinicRate = metrics.clinicRate();
		Integer videoRate = metrics.videoRate();
		int totalScore = StudyRecordGrades.weightedTotalPercent(
				homeworkRate,
				clinicRate,
				testScorePercent,
				testMetrics.hasScoredTest());
		String overallGrade = StudyRecordGrades.letterGrade(totalScore);

		DiligenceReport saved = reportRepository.insert(new ReportInsert(
				clazz.classId(),
				studentId,
				testId,
				periodStart,
				periodEnd,
				periodLabel,
				periodPresetId,
				metrics.homeworkSubmitted(),
				metrics.homeworkTotal(),
				homeworkRate,
				StudyRecordGrades.letterGradeOrNull(homeworkRate),
				metrics.clinicAttended(),
				metrics.clinicTotal(),
				clinicRate,
				StudyRecordGrades.letterGradeOrNull(clinicRate),
				raw,
				classAvg,
				null,
				null,
				testRank,
				testGrade,
				metrics.videoCertified(),
				metrics.videoTotal(),
				videoRate,
				StudyRecordGrades.letterGradeOrNull(videoRate),
				totalScore,
				overallGrade,
				null,
				null));

		String titleForPdf = pdfTestTitle != null ? pdfTestTitle : (periodLabel != null ? periodLabel : "성실도 보고서");
		try {
			User student = userRepository.findById(studentId).orElseThrow();
			String pdfUrl = pdfService.writePdf(saved, clazz.name(), student.name(), titleForPdf);
			reportRepository.updatePdfPath(saved.reportId(), pdfUrl);
		} catch (java.io.IOException e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "PDF 생성에 실패했습니다.");
		}
		eventPublisher.publishEvent(new DiligenceReportCreatedEvent(
				clazz.classId(), studentId, saved.reportId(), titleForPdf));
	}

	private PdfArchive buildZipArchive(String zipBaseName, List<DiligenceReport> reports) {
		Set<String> usedEntryNames = new HashSet<>();
		int added = 0;
		try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				ZipOutputStream zip = new ZipOutputStream(buffer)) {
			for (DiligenceReport report : reports) {
				java.nio.file.Path pdfPath = pdfFilePath(report.reportId());
				if (!Files.exists(pdfPath)) {
					continue;
				}
				User student = userRepository.findById(report.studentId()).orElseThrow();
				String entryName = uniqueZipEntryName(
						usedEntryNames,
						sanitizeArchiveFileName(student.name() + "_" + report.reportId()) + ".pdf");
				zip.putNextEntry(new ZipEntry(entryName));
				Files.copy(pdfPath, zip);
				zip.closeEntry();
				added++;
			}
			zip.finish();
			if (added == 0) {
				throw new BusinessException(ErrorCode.REPORT_NOT_FOUND, "다운로드할 PDF가 없습니다.");
			}
			return new PdfArchive(zipBaseName + ".zip", new ByteArrayResource(buffer.toByteArray()));
		} catch (IOException e) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "ZIP 생성에 실패했습니다.");
		}
	}

	private String resolvePeriodLabel(long classId, Long presetId, LocalDate periodStart, LocalDate periodEnd) {
		if (presetId != null) {
			ReportPeriodPreset preset = reportPeriodPresetService.requirePreset(classId, presetId);
			return preset.name();
		}
		return PERIOD_LABEL_FMT.format(periodStart) + " ~ " + PERIOD_LABEL_FMT.format(periodEnd);
	}

	private static Instant startOfDay(LocalDate date) {
		return date.atStartOfDay(SEOUL).toInstant();
	}

	private static Instant endOfDay(LocalDate date) {
		return date.atTime(23, 59, 59).atZone(SEOUL).toInstant();
	}

	private static List<Long> distinctStudentIds(List<Long> studentIds) {
		return new ArrayList<>(new LinkedHashSet<>(studentIds));
	}

	private Instant resolvePeriodEnd(TestExam test) {
		Instant completedAt = test.completedAt();
		Instant testAt = test.testAt();
		if (completedAt == null) {
			return testAt;
		}
		return completedAt.isAfter(testAt) ? completedAt : testAt;
	}

	private List<DiligenceReport> filterReportsForCurrentUser(List<DiligenceReport> reports) {
		UserRole role = currentUserService.requireRole();
		if (role == UserRole.STUDENT) {
			long me = currentUserService.requireUserId();
			return reports.stream().filter(r -> r.studentId() == me).toList();
		}
		if (role.isAssistant()) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return reports;
	}

	private void requireCanViewReports(Clazz clazz) {
		UserRole role = currentUserService.requireRole();
		if (role == UserRole.ACADEMY_ADMIN) {
			return;
		}
		if (role == UserRole.STUDENT) {
			if (!enrollmentRepository.existsByClassIdAndStudentId(clazz.classId(), currentUserService.requireUserId())) {
				throw new BusinessException(ErrorCode.FORBIDDEN);
			}
			return;
		}
		if (role.isHomeroomTeacher()) {
			if (clazz.homeroomTeacherId() == currentUserService.requireUserId()) {
				return;
			}
		}
		if (role.isAssistant()) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		throw new BusinessException(ErrorCode.FORBIDDEN);
	}

	private DiligenceReport requireReadableReport(long reportId) {
		DiligenceReport report = reportRepository.findById(reportId)
				.orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));
		UserRole role = currentUserService.requireRole();
		long userId = currentUserService.requireUserId();

		if (role == UserRole.PARENT) {
			parentStudentLinkService.requireParentCanAccessClass(
					userId, report.studentId(), report.classId());
			Clazz clazz = clazzRepository.findById(report.classId())
					.orElseThrow(() -> new BusinessException(ErrorCode.CLASS_NOT_FOUND));
			currentUserService.assertSameAcademy(clazz.academyId());
			return report;
		}

		Clazz clazz = classAccessService.requireReadableClass(report.classId());
		requireCanViewReports(clazz);
		if (role == UserRole.STUDENT && report.studentId() != userId) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}
		return report;
	}

	private List<ReportListRow> toListRows(List<DiligenceReport> reports) {
		List<ReportListRow> rows = new ArrayList<>();
		for (DiligenceReport r : reports) {
			User student = userRepository.findById(r.studentId()).orElseThrow();
			rows.add(new ReportListRow(
					r.reportId(),
					r.testId(),
					resolveReportTitle(r),
					r.periodLabel(),
					r.studentId(),
					student.name(),
					r.periodStart(),
					r.periodEnd(),
					r.totalScore(),
					r.overallGrade(),
					r.createdAt()));
		}
		return rows;
	}

	private String resolveReportTitle(DiligenceReport r) {
		if (r.periodLabel() != null && !r.periodLabel().isBlank()) {
			return r.periodLabel();
		}
		if (r.testId() != null) {
			return testExamRepository.findById(r.testId()).map(TestExam::title).orElse("성실도 보고서");
		}
		return "성실도 보고서";
	}

	private ReportDetailRow toDetailRow(DiligenceReport r) {
		User student = userRepository.findById(r.studentId()).orElseThrow();
		Clazz clazz = clazzRepository.findById(r.classId()).orElseThrow();
		return new ReportDetailRow(
				r.reportId(),
				r.classId(),
				clazz.name(),
				r.studentId(),
				student.name(),
				r.testId(),
				resolveReportTitle(r),
				r.periodLabel(),
				r.periodStart(),
				r.periodEnd(),
				r.homeworkSubmitted(),
				r.homeworkTotal(),
				r.homeworkRate(),
				r.homeworkGrade(),
				r.clinicAttended(),
				r.clinicTotal(),
				r.clinicRate(),
				r.clinicGrade(),
				r.testRawScore(),
				r.testClassAvg(),
				r.testUpperRankPct(),
				r.testPercentileRank(),
				r.testRank(),
				r.testGrade(),
				r.videoCertified(),
				r.videoTotal(),
				r.videoRate(),
				r.videoGrade(),
				r.totalScore(),
				r.overallGrade(),
				r.teacherComment(),
				r.pdfPath(),
				r.createdAt());
	}

	public record ReportGenerationTargetRow(
			long testId,
			String title,
			Instant testAt,
			Instant completedAt,
			boolean reportGenerated) {
	}

	public record GenerateReportsV3Result(
			int created,
			Instant periodStart,
			Instant periodEnd,
			String periodLabel) {
	}

	public record ReportListRow(
			long reportId,
			Long testId,
			String testTitle,
			String periodLabel,
			long studentId,
			String studentName,
			Instant periodStart,
			Instant periodEnd,
			int totalScore,
			String overallGrade,
			Instant createdAt) {
	}

	public record ReportDetailRow(
			long reportId,
			long classId,
			String className,
			long studentId,
			String studentName,
			Long testId,
			String testTitle,
			String periodLabel,
			Instant periodStart,
			Instant periodEnd,
			int homeworkSubmitted,
			int homeworkTotal,
			Integer homeworkRate,
			String homeworkGrade,
			int clinicAttended,
			int clinicTotal,
			Integer clinicRate,
			String clinicGrade,
			BigDecimal testRawScore,
			BigDecimal testClassAvg,
			Integer testUpperRankPct,
			Integer testPercentileRank,
			Integer testRank,
			String testGrade,
			int videoCertified,
			int videoTotal,
			Integer videoRate,
			String videoGrade,
			int totalScore,
			String overallGrade,
			String teacherComment,
			String pdfPath,
			Instant createdAt) {
	}
}
