package com.result_service.result_service.service.livequiz;

import com.result_service.result_service.model.dto.common.PageResponseDTO;
import com.result_service.result_service.model.dto.livequiz.LiveQuizResultAnswerDTO;
import com.result_service.result_service.model.dto.livequiz.StudentLiveQuizFinalResultDTO;
import com.result_service.result_service.model.dto.livequiz.TeacherLiveQuizResultDetailDTO;
import com.result_service.result_service.model.dto.livequiz.TeacherLiveQuizResultRowDTO;
import com.result_service.result_service.model.dto.livequiz.TeacherLiveQuizResultsDTO;
import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.model.entity.ResultAnswer;
import com.result_service.result_service.model.entity.enums.ResultType;
import com.result_service.result_service.repository.ExamResultRepo;
import com.result_service.result_service.repository.ResultAnswerRepo;
import com.result_service.result_service.util.exception.NotFoundException;
import com.result_service.result_service.util.exception.UnauthorizedException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class LiveQuizResultQueryService {

    private final ExamResultRepo examResultRepo;
    private final ResultAnswerRepo resultAnswerRepo;
    private final LiveQuizResultSnapshotReader snapshotReader;
    private final ObjectMapper objectMapper;

    public LiveQuizResultQueryService(
            ExamResultRepo examResultRepo,
            ResultAnswerRepo resultAnswerRepo,
            LiveQuizResultSnapshotReader snapshotReader,
            ObjectMapper objectMapper
    ) {
        this.examResultRepo = examResultRepo;
        this.resultAnswerRepo = resultAnswerRepo;
        this.snapshotReader = snapshotReader;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public TeacherLiveQuizResultsDTO teacherResults(UUID teacherId, UUID roomId, int page, int size) {
        // Teacher xem danh sach theo room sau khi Runtime da chot va Result Service da ingest.
        // Neu chua co row nao thi API tra NOT_FOUND de frontend tiep tuc doi/refresh.
        List<ExamResult> all = sortedRoomResults(roomId);
        if (all.isEmpty()) {
            throw new NotFoundException("LIVE_QUIZ_RESULTS_NOT_FOUND");
        }
        // Check quyen mot lan bang snapshot ownerTeacherId, vi tat ca row trong room cung nguon event.
        requireTeacher(teacherId, all.getFirst());
        int from = Math.min(page * size, all.size());
        int to = Math.min(from + size, all.size());
        List<TeacherLiveQuizResultRowDTO> rows = all.subList(from, to).stream().map(this::teacherRow).toList();
        LiveQuizResultSnapshot snapshot = snapshotReader.read(all.getFirst());
        return new TeacherLiveQuizResultsDTO(
                roomId,
                all.getFirst().getExamId(),
                snapshot.roomCode(),
                snapshot.quizTitle(),
                snapshot.subjectId(),
                snapshot.subjectName(),
                snapshot.closedAt(),
                all.getFirst().getReleasedAt(),
                all.size(),
                average(all),
                all.stream().map(ExamResult::getTotalScore).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO),
                all.stream().map(ExamResult::getTotalScore).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO),
                PageResponseDTO.from(new PageImpl<>(rows, PageRequest.of(page, size), all.size()))
        );
    }

    @Transactional(readOnly = true)
    public TeacherLiveQuizResultDetailDTO teacherDetail(UUID teacherId, UUID roomId, UUID resultId) {
        // Detail chi danh cho giao vien: gom dap an hoc sinh, dap an dung va snapshot cau hoi.
        // Student endpoint khong dung DTO nay de tranh lo dap an chi tiet cho hoc sinh.
        ExamResult result = examResultRepo.findById(resultId)
                .filter(candidate -> candidate.getResultType() == ResultType.LIVE_QUIZ)
                .filter(candidate -> roomId.equals(candidate.getRoomId()))
                .orElseThrow(() -> new NotFoundException("LIVE_QUIZ_RESULT_NOT_FOUND"));
        requireTeacher(teacherId, result);
        List<LiveQuizResultAnswerDTO> answers = resultAnswerRepo.findByResultIdOrderByQuestionPositionAsc(result.getId())
                .stream()
                .map(this::answerDto)
                .toList();
        return new TeacherLiveQuizResultDetailDTO(teacherRow(result), answers);
    }

    @Transactional(readOnly = true)
    public StudentLiveQuizFinalResultDTO studentResult(UUID studentId, UUID roomId) {
        // Student chi duoc doc ket qua cua chinh minh trong room.
        // Neu giao vien chua close hoac consumer chua ingest xong thi frontend se nhan LIVE_QUIZ_RESULT_NOT_READY.
        ExamResult result = examResultRepo.findByResultTypeAndRoomIdAndStudentId(ResultType.LIVE_QUIZ, roomId, studentId)
                .orElseThrow(() -> new NotFoundException("LIVE_QUIZ_RESULT_NOT_READY"));
        LiveQuizResultSnapshot snapshot = snapshotReader.read(result);
        return new StudentLiveQuizFinalResultDTO(
                roomId,
                result.getExamId(),
                snapshot.roomCode(),
                snapshot.quizTitle(),
                snapshot.subjectId(),
                snapshot.subjectName(),
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

    List<ExamResult> sortedRoomResults(UUID roomId) {
        // Sap xep uu tien finalRank da duoc Runtime tinh chot.
        // Diem chi la fallback neu co du lieu cu/thieu rank trong snapshot.
        return examResultRepo.findByResultTypeAndRoomId(ResultType.LIVE_QUIZ, roomId).stream()
                .sorted(Comparator
                        .comparingInt((ExamResult result) -> {
                            int rank = snapshotReader.read(result).finalRank();
                            return rank > 0 ? rank : Integer.MAX_VALUE;
                        })
                        .thenComparing(ExamResult::getTotalScore, Comparator.reverseOrder()))
                .toList();
    }

    TeacherLiveQuizResultRowDTO teacherRow(ExamResult result) {
        // Row nay la view model gon cho bang xep hang cua giao vien.
        // Cac thong tin phu nhu timeout/notReached doc tu examSnapshot vi ExamResult cu khong co cot rieng.
        LiveQuizResultSnapshot snapshot = snapshotReader.read(result);
        return new TeacherLiveQuizResultRowDTO(
                result.getId(),
                result.getRoomId(),
                result.getExamId(),
                result.getParticipantId(),
                result.getStudentId(),
                snapshotReader.studentCode(result),
                snapshotReader.studentName(result),
                snapshot.finalRank(),
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
                result.getReleasedAt()
        );
    }

    private LiveQuizResultAnswerDTO answerDto(ResultAnswer answer) {
        // Convert JSON string trong DB thanh list/map de frontend co the render chi tiet cau tra loi.
        // Neu parse loi thi helper tra rong, tranh lam hong toan bo man hinh detail.
        return new LiveQuizResultAnswerDTO(
                answer.getQuestionId(),
                answer.getQuestionPosition() != null ? answer.getQuestionPosition() : answer.getQuestionOrder(),
                readUuidList(answer.getSelectedOptionIds()),
                readUuidList(answer.getCorrectOptionIds()),
                answer.isCorrect(),
                answer.getScoreAwarded(),
                answer.getMaxScore(),
                answer.getAnswerStatus(),
                answer.getResponseTimeMs(),
                answer.getAnsweredAt(),
                readMap(answer.getQuestionSnapshot())
        );
    }

    private void requireTeacher(UUID teacherId, ExamResult result) {
        // Live quiz dung ownerTeacherId trong snapshot da dong bang tai luc close.
        // Khong goi sang Exam Service de tranh tron voi policy cua exam thuong.
        UUID owner = snapshotReader.read(result).ownerTeacherId();
        if (owner == null || !owner.equals(teacherId)) {
            throw new UnauthorizedException("LIVE_QUIZ_RESULT_TEACHER_NOT_ALLOWED");
        }
    }

    private BigDecimal average(List<ExamResult> results) {
        if (results.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = results.stream().map(ExamResult::getTotalScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(results.size()), 4, RoundingMode.HALF_UP);
    }

    private List<UUID> readUuidList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception exception) {
            return Map.of();
        }
    }
}
