package com.result_service.result_service.service.livequiz;

import com.result_service.result_service.model.dto.common.PageResponseDTO;
import com.result_service.result_service.model.dto.livequiz.StudentLiveQuizResultSummaryDTO;
import com.result_service.result_service.model.dto.livequiz.TeacherLiveQuizResultIndexDTO;
import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.model.entity.enums.ResultType;
import com.result_service.result_service.repository.ExamResultRepo;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LiveQuizResultIndexService {

    private final ExamResultRepo examResultRepo;
    private final LiveQuizResultSnapshotReader snapshotReader;

    public LiveQuizResultIndexService(
            ExamResultRepo examResultRepo,
            LiveQuizResultSnapshotReader snapshotReader
    ) {
        this.examResultRepo = examResultRepo;
        this.snapshotReader = snapshotReader;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<TeacherLiveQuizResultIndexDTO> teacherSubjectResults(
            UUID teacherId,
            UUID subjectId,
            int page,
            int size
    ) {
        // Snapshot live quiz dang nam trong examSnapshot JSON, nen MVP loc trong service.
        // Chi group sau khi da check ownerTeacherId va subjectId de tranh lo ket qua cua giao vien khac.
        Map<UUID, List<ExamResult>> byRoom = examResultRepo.findByResultType(ResultType.LIVE_QUIZ)
                .stream()
                .filter(result -> result.getRoomId() != null)
                .filter(result -> {
                    LiveQuizResultSnapshot snapshot = snapshotReader.read(result);
                    return teacherId.equals(snapshot.ownerTeacherId()) && subjectId.equals(snapshot.subjectId());
                })
                .collect(LinkedHashMap::new, (map, result) -> map.computeIfAbsent(result.getRoomId(), ignored -> new java.util.ArrayList<>()).add(result), Map::putAll);

        List<TeacherLiveQuizResultIndexDTO> rows = byRoom.values().stream()
                .map(this::teacherRow)
                .sorted(Comparator.comparing(TeacherLiveQuizResultIndexDTO::closedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        return page(rows, page, size);
    }

    @Transactional(readOnly = true)
    public List<StudentLiveQuizResultSummaryDTO> studentResults(UUID studentId, UUID subjectId) {
        return examResultRepo.findByResultTypeAndStudentIdOrderByReleasedAtDesc(ResultType.LIVE_QUIZ, studentId)
                .stream()
                .filter(result -> {
                    LiveQuizResultSnapshot snapshot = snapshotReader.read(result);
                    return subjectId == null || subjectId.equals(snapshot.subjectId());
                })
                .map(this::studentRow)
                .toList();
    }

    private TeacherLiveQuizResultIndexDTO teacherRow(List<ExamResult> roomResults) {
        ExamResult first = roomResults.getFirst();
        LiveQuizResultSnapshot snapshot = snapshotReader.read(first);
        return new TeacherLiveQuizResultIndexDTO(
                first.getRoomId(),
                first.getExamId(),
                snapshot.subjectId(),
                snapshot.subjectName(),
                snapshot.roomCode(),
                snapshot.quizTitle(),
                snapshot.closedAt(),
                releasedAt(roomResults),
                roomResults.size(),
                average(roomResults),
                roomResults.stream().map(ExamResult::getTotalScore).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO),
                roomResults.stream().map(ExamResult::getTotalScore).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO)
        );
    }

    private StudentLiveQuizResultSummaryDTO studentRow(ExamResult result) {
        LiveQuizResultSnapshot snapshot = snapshotReader.read(result);
        return new StudentLiveQuizResultSummaryDTO(
                result.getRoomId(),
                result.getExamId(),
                snapshot.subjectId(),
                snapshot.subjectName(),
                snapshot.roomCode(),
                snapshot.quizTitle(),
                snapshot.finalRank(),
                snapshot.participantCount(),
                result.getTotalScore(),
                result.getMaxScore(),
                result.getPercentage(),
                result.getAnsweredQuestions(),
                result.getTotalQuestions(),
                result.getCorrectCount(),
                result.getWrongCount(),
                snapshot.timeoutCount(),
                snapshot.notReachedCount(),
                result.getAverageResponseMs(),
                result.getFinishedAt(),
                snapshot.closedAt(),
                result.getReleasedAt()
        );
    }

    private PageResponseDTO<TeacherLiveQuizResultIndexDTO> page(List<TeacherLiveQuizResultIndexDTO> rows, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        int safePage = Math.max(page, 0);
        int from = Math.min(safePage * safeSize, rows.size());
        int to = Math.min(from + safeSize, rows.size());
        return PageResponseDTO.from(new PageImpl<>(rows.subList(from, to), PageRequest.of(safePage, safeSize), rows.size()));
    }

    private OffsetDateTime releasedAt(List<ExamResult> results) {
        return results.stream()
                .map(ExamResult::getReleasedAt)
                .filter(value -> value != null)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private BigDecimal average(List<ExamResult> results) {
        if (results.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = results.stream().map(ExamResult::getTotalScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(results.size()), 4, RoundingMode.HALF_UP);
    }
}
