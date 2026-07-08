package com.examruntime_service.examruntime_service.service.paper;

import com.examruntime_service.examruntime_service.client.QuestionMediaClient;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperOptionDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperQuestionDTO;
import com.examruntime_service.examruntime_service.model.dto.session.StudentAnswerDTO;
import com.examruntime_service.examruntime_service.model.dto.session.StudentOptionDTO;
import com.examruntime_service.examruntime_service.model.dto.session.StudentQuestionDTO;
import com.examruntime_service.examruntime_service.model.entity.SessionAnswer;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
// Mapper anh xa cac thong tin de thi va bai lam sang dinh dang an toan cho hoc sinh
public class StudentPaperMapper {

    private final ObjectMapper objectMapper;
    private final QuestionMediaClient questionMediaClient;

    public StudentPaperMapper(ObjectMapper objectMapper, QuestionMediaClient questionMediaClient) {
        this.objectMapper = objectMapper;
        this.questionMediaClient = questionMediaClient;
    }

    // Chuyen doi pool cau hoi va thu tu sap xep thanh danh sach cau hoi da sap xep dung
    public List<StudentQuestionDTO> mapToStudentQuestions(
            ExamPaperPoolDTO pool,
            List<UUID> questionOrder, // truyen danh sach id vao nen can mapper ra cau hoi thuc de tra cho frontend
            Map<UUID, List<UUID>> optionOrders
    ) {
        if (pool == null || pool.questions() == null) {
            return Collections.emptyList();
        }

        Map<UUID, PaperQuestionDTO> questionMap = pool.questions().stream()
                .collect(Collectors.toMap(PaperQuestionDTO::questionId, Function.identity()));
        Map<String, String> imageUrls = questionMediaClient.signedUrls(pool.questions().stream()
                .map(PaperQuestionDTO::imageObjectKey)
                .filter(key -> key != null && !key.isBlank())
                .distinct()
                .toList());

        List<StudentQuestionDTO> result = new ArrayList<>();
        for (UUID questionId : questionOrder) {
            PaperQuestionDTO question = questionMap.get(questionId);
            if (question == null) {
                continue;
            }

            List<UUID> optOrder = optionOrders.get(questionId);
            List<StudentOptionDTO> studentOptions = mapToStudentOptions(question.options(), optOrder);

            StudentQuestionDTO sq = StudentQuestionDTO.builder()
                    .questionId(question.questionId())
                    .difficulty(question.difficulty())
                    .type(question.type())
                    .content(question.content())
                    .contentFormat(question.contentFormat())
                    .score(question.score())
                    .imageObjectKey(question.imageObjectKey())
                    .imageUrl(imageUrl(imageUrls, question.imageObjectKey()))
                    .options(studentOptions)
                    .build();

            result.add(sq);
        }

        return result;
    }

    private String imageUrl(Map<String, String> imageUrls, String imageObjectKey) {
        if (imageObjectKey == null || imageObjectKey.isBlank()) {
            return null;
        }
        return imageUrls.get(imageObjectKey);
    }

    private List<StudentOptionDTO> mapToStudentOptions(List<PaperOptionDTO> options, List<UUID> optOrder) {
        if (options == null || optOrder == null) {
            return Collections.emptyList();
        }

        Map<UUID, PaperOptionDTO> optionMap = options.stream()
                .collect(Collectors.toMap(PaperOptionDTO::optionId, Function.identity()));

        List<StudentOptionDTO> result = new ArrayList<>();
        for (UUID optionId : optOrder) {
            PaperOptionDTO option = optionMap.get(optionId);
            if (option == null) {
                continue;
            }

            StudentOptionDTO so = StudentOptionDTO.builder()
                    .optionId(option.optionId())
                    .key(option.key())
                    .content(option.content())
                    .contentFormat(option.contentFormat())
                    .build();

            result.add(so);
        }

        return result;
    }

    // Anh xa danh sach cau tra loi dang duoc luu tru
    public List<StudentAnswerDTO> mapToStudentAnswers(List<SessionAnswer> answers) {
        if (answers == null) {
            return Collections.emptyList();
        }

        return answers.stream()
                .map(ans -> {
                    List<UUID> selectedOptIds = new ArrayList<>();
                    try {
                        if (ans.getSelectedOptionIds() != null && !ans.getSelectedOptionIds().isBlank()) {
                            selectedOptIds = objectMapper.readValue(ans.getSelectedOptionIds(), new TypeReference<List<UUID>>() {});
                        }
                    } catch (Exception ignored) {
                    }
                    return StudentAnswerDTO.builder()
                            .questionId(ans.getQuestionId())
                            .selectedOptionIds(selectedOptIds)
                            .answerText(ans.getAnswerText())
                            .markedForReview(ans.isMarkedForReview())
                            .build();
                })
                .collect(Collectors.toList());
    }
}
