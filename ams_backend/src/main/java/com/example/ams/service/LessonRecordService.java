package com.example.ams.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ams.api.dto.CreateLessonRecordRequest;
import com.example.ams.api.dto.LessonRecordLinkedItemResponse;
import com.example.ams.common.BusinessException;
import com.example.ams.common.ErrorCode;
import com.example.ams.domain.clazz.Clazz;
import com.example.ams.domain.clazz.ClinicSlotOccurrence;
import com.example.ams.domain.clazz.DayOfWeek;
import com.example.ams.domain.clazz.LessonRecord;
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
	private final UserRepository userRepository;
	private final ClassAccessService classAccessService;
	private final CurrentUserService currentUserService;
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
			UserRepository userRepository,
			ClassAccessService classAccessService,
			CurrentUserService currentUserService,
			HomeworkService homeworkService,
			TestExamService testExamService,
			VideoLessonService videoLessonService,
			ClinicSlotService clinicSlotService) {
		this.lessonRecordRepository = lessonRecordRepository;
		this.homeworkRepository = homeworkRepository;
		this.testExamRepository = testExamRepository;
		this.videoLessonRepository = videoLessonRepository;
		this.clinicSlotRepository = clinicSlotRepository;
		this.userRepository = userRepository;
		this.classAccessService = classAccessService;
		this.currentUserService = currentUserService;
		this.homeworkService = homeworkService;
		this.testExamService = testExamService;
		this.videoLessonService = videoLessonService;
		this.clinicSlotService = clinicSlotService;
	}

	public List<LessonRecordRow> listLessonRecords(long classId) {
		classAccessService.requireReadableClass(classId);
		return lessonRecordRepository.findByClassId(classId).stream()
				.map(this::toRow)
				.toList();
	}

	public LessonRecordRow getLessonRecord(long lessonRecordId) {
		LessonRecord record = requireRecord(lessonRecordId);
		classAccessService.requireReadableClass(record.classId());
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

		if (request.homework() != null) {
			homeworkService.createHomeworkForLessonRecord(
					classId,
					lessonRecordId,
					request.homework().title().trim(),
					request.homework().questionCount(),
					request.homework().targetStudentIds());
		}
		if (request.test() != null) {
			var testItem = request.test();
			testExamService.createTestForLessonRecord(
					classId,
					lessonRecordId,
					testItem.title().trim(),
					instantAt(lessonDate, LocalTime.of(14, 0)),
					testItem.questionCount(),
					testItem.retakeThresholdCount(),
					testItem.targetStudentIds());
		}
		if (request.video() != null) {
			videoLessonService.createVideoForLessonRecord(
					classId,
					lessonRecordId,
					request.video().youtubeUrl(),
					request.video().title().trim(),
					instantAt(lessonDate, LocalTime.of(9, 0)),
					request.video().targetStudentIds());
		}
		if (request.clinic() != null) {
			var clinic = request.clinic();
			clinicSlotService.createSlotForLessonRecord(
					classId,
					lessonRecordId,
					clinic.clinicDate(),
					clinic.startTime(),
					clinic.assistantId(),
					clinic.resolvedMaxCapacity(),
					clinic.targetStudentIds());
		}

		return toDetailRow(lessonRecordRepository.findById(lessonRecordId).orElseThrow());
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
		List<LessonRecordLinkedItemResponse> items = new ArrayList<>();
		for (var hw : homeworkRepository.findSummariesByLessonRecordId(lessonRecordId)) {
			items.add(new LessonRecordLinkedItemResponse("homework", hw.homeworkId(), hw.title()));
		}
		for (var test : testExamRepository.findSummariesByLessonRecordId(lessonRecordId)) {
			items.add(new LessonRecordLinkedItemResponse("test", test.testId(), test.title()));
		}
		for (var video : videoLessonRepository.findSummariesByLessonRecordId(lessonRecordId)) {
			items.add(new LessonRecordLinkedItemResponse("video", video.videoId(), video.title()));
		}
		for (var clinic : clinicSlotRepository.findSummariesByLessonRecordId(lessonRecordId)) {
			items.add(new LessonRecordLinkedItemResponse("clinic", clinic.slotId(), formatClinicTitle(clinic)));
		}
		return items;
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
