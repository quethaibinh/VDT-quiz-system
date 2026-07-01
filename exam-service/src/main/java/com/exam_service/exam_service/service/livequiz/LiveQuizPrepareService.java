package com.exam_service.exam_service.service.livequiz;

import com.exam_service.exam_service.client.QuestionCollectionSnapshot;
import com.exam_service.exam_service.client.QuestionServiceClient;
import com.exam_service.exam_service.client.RuntimeLiveQuizClient;
import com.exam_service.exam_service.model.dto.livequiz.LiveQuizPrepareResponseDTO;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.LiveQuizJoinPolicy;
import com.exam_service.exam_service.repository.ExamQuestionRepo;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.util.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
/**
 * Dieu phoi prepare live quiz: lay snapshot ngoai transaction, commit DB, roi tao room Runtime.
 *
 * Tach service dieu phoi khoi service transaction giup transaction DB ngan gon va
 * khong bi treo boi call HTTP sang service khac.
 */
public class LiveQuizPrepareService {

    private final ExamRepo examRepo;
    private final ExamQuestionRepo questionRepo;
    private final QuestionServiceClient questionServiceClient;
    private final RuntimeLiveQuizClient runtimeLiveQuizClient;
    private final LiveQuizPrepareTransactionService transactionService;
    private final TeacherLiveQuizService liveQuizService;

    public LiveQuizPrepareService(
            ExamRepo examRepo,
            ExamQuestionRepo questionRepo,
            QuestionServiceClient questionServiceClient,
            RuntimeLiveQuizClient runtimeLiveQuizClient,
            LiveQuizPrepareTransactionService transactionService,
            TeacherLiveQuizService liveQuizService
    ) {
        this.examRepo = examRepo;
        this.questionRepo = questionRepo;
        this.questionServiceClient = questionServiceClient;
        this.runtimeLiveQuizClient = runtimeLiveQuizClient;
        this.transactionService = transactionService;
        this.liveQuizService = liveQuizService;
    }

    public LiveQuizPrepareResponseDTO prepare(UUID subjectId, UUID quizId, UUID teacherId) {
        Exam quiz = liveQuizService.requireOwnedLiveQuiz(subjectId, quizId, teacherId);
        Exam prepared;
        if (quiz.getStatus() == ExamStatus.PREPARED && questionRepo.existsByExamId(quizId)) {
            // Idempotent retry: neu DB da co snapshot thi khong goi lai Question Service.
            prepared = quiz;
        } else {
            // Snapshot lay ngoai transaction de transaction khong giu lock trong luc goi service ngoai.
            QuestionCollectionSnapshot snapshot = questionServiceClient.getLiveQuizSnapshot(
                    subjectId,
                    quiz.getCollectionId(),
                    teacherId
            );
            prepared = transactionService.prepare(subjectId, quizId, teacherId, quiz.getVersion(), snapshot);
        }
        // Runtime createRoom cung idempotent theo examId + active status; goi lai se tra room cu.
        RuntimeLiveQuizClient.LiveQuizRoomResponse room = runtimeLiveQuizClient.createRoom(
                prepared.getId(),
                prepared.getCreatedByTeacherId(),
                prepared.getSnapshotVersion(),
                prepared.getLiveQuizJoinPolicy() == null
                        ? LiveQuizJoinPolicy.CODE_ONLY
                        : prepared.getLiveQuizJoinPolicy()
        );
        return new LiveQuizPrepareResponseDTO(
                liveQuizService.toDetail(prepared),
                room.roomId(),
                room.roomCode(),
                room.status()
        );
    }
}
