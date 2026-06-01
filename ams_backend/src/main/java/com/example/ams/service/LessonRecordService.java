package com.example.ams.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.api.dto.AddLessonRecordLinkedItemsRequest;
import com.example.ams.api.dto.AssignmentTargetResponse;
import com.example.ams.api.dto.CreateLessonRecordRequest;
import com.example.ams.api.dto.LessonRecordClinicItem;
import com.example.ams.api.dto.LessonRecordHomeworkItem;
import com.example.ams.api.dto.LessonRecordLinkedItemResponse;
import com.example.ams.api.dto.LessonRecordTestItem;
import com.example.ams.api.dto.LessonRecordVideoItem;
import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.AssignmentEntityType;
import com.example.ams.domain.clazz.AssignmentStatus;
import com.example.ams.domain.clazz.ClinicSlotOccurrence;
import com.example.ams.domain.clazz.DayOfWeek;
import com.example.ams.domain.clazz.LessonRecord;
import com.example.ams.domain.user.UserRole;
import com.example.ams.repository.ClassEnrollmentRepository;
import com.example.ams.repository.ClinicReservationRepository;
import com.example.ams.repository.ClinicSlotRepository;
import com.example.ams.repository.HomeworkRepository;
import com.example.ams.repository.LessonRecordRepository;
import com.example.ams.repository.TestExamRepository;
import com.example.ams.repository.UserRepository;
import com.example.ams.repository.VideoLessonRepository;
import com.example.ams.security.CurrentUserService;

@Service
public class LessonRecordService {

	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final LessonRecordRepository lessonRecordRepository;
	private final HomeworkRepository homeworkRepository;
	private final TestExamRepository testExamRepository;
	private final VideoLessonRepository videoLessonRepository;
	private final ClinicSlotRepository clinicSlotRepository;
	private final ClinicReservationRepository clinicReservationRepository;
	private final ClassEnrollmentRepository enrollmentRepository;
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
	private final AssignmentTargetService assignmentTargetService;
	private final HomeworkService homeworkService;
	private final TestExamService testExamService;
	private final VideoLessonService videoLessonService;
	private final ClinicSlotService clinicSlotService;

	public LessonRecordService(
			LessonRecordRepository lessonRecordRepository,
			HomeworkRepository homeworkRepository,
			TestExamRepository testExamRepository,
			VideoLessonRepository videoLessonRepository,
			ClinicSlotRepository clinicSlotRepository,
			ClinicReservationRepository clinicReservationRepository,
			ClassEnrollmentRepository enrollmentRepository,
			UserRepository userRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			AssignmentTargetService assignmentTargetService,
			HomeworkService homeworkService,
			TestExamService testExamService,
			VideoLessonService videoLessonService,
			ClinicSlotService clinicSlotService) {
		this.lessonRecordRepository = lessonRecordRepository;
		this.homeworkRepository = homeworkRepository;
		this.testExamRepository = testExamRepository;
		this.videoLessonRepository = videoLessonRepository;
		this.clinicSlotRepository = clinicSlotRepository;
		this.clinicReservationRepository = clinicReservationRepository;
		this.enrollmentRepository = enrollmentRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.assignmentTargetService = assignmentTargetService;
		this.homeworkService = homeworkService;
		this.testExamService = testExamService;
		this.videoLessonService = videoLessonService;
		this.clinicSlotService = clinicSlotService;
	}

	public List<LessonRecordRow> listLessonRecords(long classId) {
		classAccessService.requireReadableClass(classId);
		UserRole role = currentUserService.requireRole();
		long me = role == UserRole.STUDENT ? currentUserService.requireUserId() : -1L;
		LocalDate accessibleFrom = role == UserRole.STUDENT
				? enrollmentRepository.findByClassIdAndStudentId(classId, me)
						.map(e -> e.accessibleFrom())
						.orElse(null)
				: null;
		return lessonRecordRepository.findByClassId(classId).stream()
				.filter(r -> accessibleFrom == null || !r.lessonDate().isBefore(accessibleFrom))
				.map(this::toRow)
				.toList();
	}

	public LessonRecordRow getLessonRecord(long lessonRecordId) {
		LessonRecord record = requireRecord(lessonRecordId);
		classAccessService.requireReadableClass(record.classId());
		UserRole role = currentUserService.requireRole();
		if (role == UserRole.STUDENT) {
			long me = currentUserService.requireUserId();
			LocalDate accessibleFrom = enrollmentRepository.findByClassIdAndStudentId(record.classId(), me)
					.map(e -> e.accessibleFrom())
					.orElse(null);
			if (accessibleFrom != null && record.lessonDate().isBefore(accessibleFrom)) {
				throw new BusinessException(ErrorCode.LESSON_RECORD_NOT_FOUND);
			}
		}
		return toDetailRow(record);
	}

	@Transactional
	public LessonRecordRow createLessonRecord(long classId, CreateLessonRecordRequest request) {
		Clazz clazz = classAccessService.requireReadableClass(classId);
		classAccessService.requireEditClassContent(clazz);
		LocalDate lessonDate = request.lessonDate();
		if (lessonRecordRepository.existsByClassIdAndLessonDate(classId, lessonDate)) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_DATE_EXISTS);
		}
		LessonRecord created = lessonRecordRepository.insert(
				classId,
				lessonDate,
				request.summary().trim(),
				currentUserService.requireUserId());
		long lessonRecordId = created.lessonRecordId();
		applyLinkedItems(classId, lessonRecordId, lessonDate, new AddLessonRecordLinkedItemsRequest(
				request.homework(), request.test(), request.video(), request.clinic()));

		return toDetailRow(lessonRecordRepository.findById(lessonRecordId).orElseThrow());
	}

	@Transactional
	public LessonRecordRow addLinkedItems(long lessonRecordId, AddLessonRecordLinkedItemsRequest request) {
		LessonRecord record = requireRecord(lessonRecordId);
		Clazz clazz = classAccessService.requireReadableClass(record.classId());
		classAccessService.requireEditClassContent(clazz);
		if (request.homework() == null && request.test() == null && request.video() == null && request.clinic() == null) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "추가할 항목을 선택하세요.");
		}
		applyLinkedItems(record.classId(), lessonRecordId, record.lessonDate(), request);
		return toDetailRow(lessonRecordRepository.findById(lessonRecordId).orElseThrow());
	}

	@Transactional
	public LessonRecordRow updateLinkedHomework(long lessonRecordId, long homeworkId, LessonRecordHomeworkItem item) {
		requireLinkedHomework(lessonRecordId, homeworkId);
		homeworkService.updateHomeworkMetadata(
				homeworkId, item.title(), item.questionCount(), item.targetStudentIds());
		return toDetailRow(requireRecord(lessonRecordId));
	}

	@Transactional
	public LessonRecordRow updateLinkedTest(long lessonRecordId, long testId, LessonRecordTestItem item) {
		requireLinkedTest(lessonRecordId, testId);
		testExamService.updateTestMetadata(
				testId,
				item.title(),
				item.questionCount(),
				item.retakeThresholdCount(),
				item.targetStudentIds());
		return toDetailRow(requireRecord(lessonRecordId));
	}

	@Transactional
	public LessonRecordRow updateLinkedVideo(long lessonRecordId, long videoId, LessonRecordVideoItem item) {
		LessonRecord record = requireRecord(lessonRecordId);
		requireLinkedVideo(lessonRecordId, videoId);
		videoLessonService.updateVideoWithTargets(
				record.classId(),
				videoId,
				item.youtubeUrl(),
				item.title(),
				item.targetStudentIds());
		return toDetailRow(record);
	}

	@Transactional
	public LessonRecordRow updateLinkedClinicSlot(long lessonRecordId, long slotId, LessonRecordClinicItem item) {
		LessonRecord record = requireRecord(lessonRecordId);
		requireLinkedClinicSlot(lessonRecordId, slotId);
		clinicSlotService.updateSlotForLessonRecord(
				record.classId(),
				slotId,
				item.clinicDate(),
				item.startTime(),
				item.assistantId(),
				item.presetId(),
				item.resolvedMaxCapacity(),
				item.targetStudentIds());
		return toDetailRow(record);
	}

	@Transactional
	public LessonRecordRow deleteLinkedHomework(long lessonRecordId, long homeworkId) {
		requireLinkedHomework(lessonRecordId, homeworkId);
		homeworkService.deleteHomeworkIfAllowed(homeworkId);
		return toDetailRow(requireRecord(lessonRecordId));
	}

	@Transactional
	public LessonRecordRow deleteLinkedTest(long lessonRecordId, long testId) {
		requireLinkedTest(lessonRecordId, testId);
		testExamService.deleteTestIfAllowed(testId);
		return toDetailRow(requireRecord(lessonRecordId));
	}

	@Transactional
	public LessonRecordRow deleteLinkedVideo(long lessonRecordId, long videoId) {
		LessonRecord record = requireRecord(lessonRecordId);
		requireLinkedVideo(lessonRecordId, videoId);
		videoLessonService.deleteVideoIfAllowed(record.classId(), videoId);
		return toDetailRow(record);
	}

	@Transactional
	public LessonRecordRow deleteLinkedClinicSlot(long lessonRecordId, long slotId) {
		LessonRecord record = requireRecord(lessonRecordId);
		requireLinkedClinicSlot(lessonRecordId, slotId);
		clinicSlotService.deleteSlotIfAllowed(record.classId(), slotId);
		return toDetailRow(record);
	}

	private void applyLinkedItems(
			long classId,
			long lessonRecordId,
			LocalDate lessonDate,
			AddLessonRecordLinkedItemsRequest request) {
		if (request.homework() != null) {
			var item = request.homework();
			homeworkService.createHomeworkForLessonRecord(
					classId, lessonRecordId, item.title().trim(), item.questionCount(), item.targetStudentIds());
		}
		if (request.test() != null) {
			var item = request.test();
			testExamService.createTestForLessonRecord(
					classId,
					lessonRecordId,
					item.title().trim(),
					instantAt(lessonDate, LocalTime.of(14, 0)),
					item.questionCount(),
					item.retakeThresholdCount(),
					item.targetStudentIds());
		}
		if (request.video() != null) {
			var item = request.video();
			videoLessonService.createVideoForLessonRecord(
					classId,
					lessonRecordId,
					item.youtubeUrl(),
					item.title().trim(),
					instantAt(lessonDate, LocalTime.of(9, 0)),
					item.targetStudentIds());
		}
		if (request.clinic() != null) {
			var clinic = request.clinic();
			clinicSlotService.createSlotForLessonRecord(
					classId,
					lessonRecordId,
					clinic.clinicDate(),
					clinic.startTime(),
					clinic.assistantId(),
					clinic.presetId(),
					clinic.resolvedMaxCapacity(),
					clinic.targetStudentIds());
		}
	}

	private void requireLinkedHomework(long lessonRecordId, long homeworkId) {
		Long linked = homeworkRepository.findLessonRecordId(homeworkId);
		if (linked == null || linked != lessonRecordId) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_NOT_FOUND);
		}
	}

	private void requireLinkedTest(long lessonRecordId, long testId) {
		Long linked = testExamRepository.findLessonRecordId(testId);
		if (linked == null || linked != lessonRecordId) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_NOT_FOUND);
		}
	}

	private void requireLinkedVideo(long lessonRecordId, long videoId) {
		Long linked = videoLessonRepository.findLessonRecordId(videoId);
		if (linked == null || linked != lessonRecordId) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_NOT_FOUND);
		}
	}

	private void requireLinkedClinicSlot(long lessonRecordId, long slotId) {
		Long linked = clinicSlotRepository.findLessonRecordId(slotId);
		if (linked == null || linked != lessonRecordId) {
			throw new BusinessException(ErrorCode.LESSON_RECORD_LINK_NOT_FOUND);
		}
	}

	@Transactional
	public LessonRecordRow updateLessonRecord(long lessonRecordId, String summary) {
		LessonRecord record = requireRecord(lessonRecordId);
		Clazz clazz = classAccessService.requireReadableClass(record.classId());
		classAccessService.requireEditClassContent(clazz);
		lessonRecordRepository.updateSummary(lessonRecordId, summary.trim());
		return toDetailRow(lessonRecordRepository.findById(lessonRecordId).orElseThrow());
	}

	private Instant instantAt(LocalDate date, LocalTime time) {
		return date.atTime(time).atZone(SEOUL).toInstant();
	}

	private LessonRecord requireRecord(long lessonRecordId) {
		return lessonRecordRepository.findById(lessonRecordId)
				.orElseThrow(() -> new BusinessException(ErrorCode.LESSON_RECORD_NOT_FOUND));
	}

	private LessonRecordRow toRow(LessonRecord record) {
		String authorName = userRepository.findById(record.authorId())
				.map(u -> u.name())
				.orElse("—");
		LessonRecordCounts counts = loadCounts(record.lessonRecordId());
		return new LessonRecordRow(record, authorName, counts, List.of());
	}

	private LessonRecordRow toDetailRow(LessonRecord record) {
		String authorName = userRepository.findById(record.authorId())
				.map(u -> u.name())
				.orElse("—");
		long lessonRecordId = record.lessonRecordId();
		LessonRecordCounts counts = loadCounts(lessonRecordId);
		return new LessonRecordRow(record, authorName, counts, loadLinkedItems(lessonRecordId));
	}

	private LessonRecordCounts loadCounts(long lessonRecordId) {
		return new LessonRecordCounts(
				lessonRecordRepository.countHomework(lessonRecordId),
				lessonRecordRepository.countTests(lessonRecordId),
				lessonRecordRepository.countVideos(lessonRecordId),
				lessonRecordRepository.countClinicSlots(lessonRecordId));
	}

	private List<LessonRecordLinkedItemResponse> loadLinkedItems(long lessonRecordId) {
		LessonRecord record = requireRecord(lessonRecordId);
		long classId = record.classId();
		List<LessonRecordLinkedItemResponse> items = new ArrayList<>();
		for (var hw : homeworkRepository.findSummariesByLessonRecordId(lessonRecordId)) {
			items.add(new LessonRecordLinkedItemResponse(
					"homework",
					hw.homeworkId(),
					hw.title(),
					hw.questionCount(),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					toTargetResponse(AssignmentEntityType.HOMEWORK, hw.homeworkId(), classId),
					hw.status() == AssignmentStatus.SCHEDULED,
					true));
		}
		for (var test : testExamRepository.findSummariesByLessonRecordId(lessonRecordId)) {
			items.add(new LessonRecordLinkedItemResponse(
					"test",
					test.testId(),
					test.title(),
					test.questionCount(),
					test.retakeThresholdCount(),
					null,
					null,
					null,
					null,
					null,
					null,
					null,
					toTargetResponse(AssignmentEntityType.TEST, test.testId(), classId),
					test.status() == AssignmentStatus.SCHEDULED && test.retakeAttemptNo() == 0,
					test.retakeAttemptNo() == 0));
		}
		for (var video : videoLessonRepository.findSummariesByLessonRecordId(lessonRecordId)) {
			items.add(new LessonRecordLinkedItemResponse(
					"video",
					video.videoId(),
					video.title(),
					null,
					null,
					video.youtubeUrl(),
					null,
					null,
					null,
					null,
					null,
					null,
					toTargetResponse(AssignmentEntityType.VIDEO, video.videoId(), classId),
					true,
					true));
		}
		for (var clinic : clinicSlotRepository.findSummariesByLessonRecordId(lessonRecordId)) {
			LocalDate clinicDate = ClinicSlotOccurrence.slotDate(clinic.weekStartDate(), clinic.dayOfWeek());
			items.add(new LessonRecordLinkedItemResponse(
					"clinic",
					clinic.slotId(),
					formatClinicTitle(clinic),
					null,
					null,
					null,
					clinicDate,
					formatTime(clinic.startTime()),
					clinic.assistantId(),
					clinic.maxCapacity(),
					clinic.presetId(),
					clinic.presetName(),
					toTargetResponse(AssignmentEntityType.CLINIC_SLOT, clinic.slotId(), classId),
					clinicReservationRepository.countBySlotId(clinic.slotId()) == 0,
					true));
		}
		return items;
	}

	private AssignmentTargetResponse toTargetResponse(AssignmentEntityType type, long entityId, long classId) {
		return AssignmentTargetResponse.from(
				assignmentTargetService.getTargetView(type, entityId, classId));
	}

	private static String formatTime(LocalTime time) {
		return String.format("%02d:%02d", time.getHour(), time.getMinute());
	}

	private String formatClinicTitle(ClinicSlotRepository.ClinicSlotSummary clinic) {
		LocalDate slotDate = ClinicSlotOccurrence.slotDate(clinic.weekStartDate(), clinic.dayOfWeek());
		String date = String.format(
				"%d.%02d.%02d",
				slotDate.getYear(),
				slotDate.getMonthValue(),
				slotDate.getDayOfMonth());
		String when = date + " (" + clinicDayLabel(clinic.dayOfWeek()) + ") "
				+ String.format("%d:%02d", clinic.startTime().getHour(), clinic.startTime().getMinute());
		String assistant = clinic.assistantName() != null ? clinic.assistantName() : "조교";
		return when + " · " + assistant;
	}

	private String clinicDayLabel(DayOfWeek dayOfWeek) {
		return switch (dayOfWeek) {
			case MON -> "월";
			case TUE -> "화";
			case WED -> "수";
			case THU -> "목";
			case FRI -> "금";
			case SAT -> "토";
			case SUN -> "일";
		};
	}

	public record LessonRecordCounts(int homeworkCount, int testCount, int videoCount, int clinicCount) {
	}

	public record LessonRecordRow(
			LessonRecord record,
			String authorName,
			LessonRecordCounts counts,
			List<LessonRecordLinkedItemResponse> linkedItems) {
	}
}
