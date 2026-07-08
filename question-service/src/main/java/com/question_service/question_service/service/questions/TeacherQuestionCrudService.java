package com.question_service.question_service.service.questions;

import com.question_service.question_service.model.dto.questions.QuestionDetailResponseDTO;
import com.question_service.question_service.model.dto.questions.QuestionOptionResponseDTO;
import com.question_service.question_service.model.dto.questions.QuestionUpsertRequestDTO;
import com.question_service.question_service.model.entity.Question;
import com.question_service.question_service.model.entity.QuestionOption;
import com.question_service.question_service.model.entity.enums.QuestionStatus;
import com.question_service.question_service.model.entity.enums.QuestionVisibility;
import com.question_service.question_service.model.entity.enums.Source;
import com.question_service.question_service.repository.QuestionOptionRepo;
import com.question_service.question_service.repository.QuestionRepo;
import com.question_service.question_service.repository.TopicRepo;
import com.question_service.question_service.service.media.QuestionMediaStorageService;
import com.question_service.question_service.service.subjects.TeacherSubjectAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class TeacherQuestionCrudService {

    private final QuestionRepo questionRepo;
    private final QuestionOptionRepo optionRepo;
    private final TopicRepo topicRepo;
    private final TeacherSubjectAccessService subjectAccessService;
    private final QuestionContentValidator contentValidator;
    private final QuestionMediaStorageService mediaStorageService;
    private final ObjectMapper objectMapper;

    public TeacherQuestionCrudService(
            QuestionRepo questionRepo,
            QuestionOptionRepo optionRepo,
            TopicRepo topicRepo,
            TeacherSubjectAccessService subjectAccessService,
            QuestionContentValidator contentValidator,
            QuestionMediaStorageService mediaStorageService,
            ObjectMapper objectMapper
    ) {
        this.questionRepo = questionRepo;
        this.optionRepo = optionRepo;
        this.topicRepo = topicRepo;
        this.subjectAccessService = subjectAccessService;
        this.contentValidator = contentValidator;
        this.mediaStorageService = mediaStorageService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public QuestionDetailResponseDTO create(
            UUID subjectId,
            UUID teacherId,
            QuestionUpsertRequestDTO request
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        requireTopic(subjectId, request.topicId());
        QuestionContentValidator.ValidatedQuestion validated = contentValidator.validate(request);

        Question question = new Question();
        question.setSubjectId(subjectId);
        question.setOwnerTeacherId(teacherId);
        question.setStatus(QuestionStatus.ACTIVE);
        question.setSource(Source.MANUAL);
        apply(question, request.topicId(), validated);
        question = questionRepo.save(question);
        saveOptions(question.getId(), validated.options());
        return toDetail(question, true);
    }

    @Transactional(readOnly = true)
    public QuestionDetailResponseDTO getDetail(
            UUID subjectId,
            UUID questionId,
            UUID teacherId
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        Question question = load(subjectId, questionId);
        boolean owner = teacherId.equals(question.getOwnerTeacherId());
        if (!owner && (question.getVisibility() != QuestionVisibility.PUBLIC
                || question.getStatus() != QuestionStatus.ACTIVE)) {
            throw error(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND");
        }
        return toDetail(question, owner);
    }

    @Transactional
    public QuestionDetailResponseDTO update(
            UUID subjectId,
            UUID questionId,
            UUID teacherId,
            QuestionUpsertRequestDTO request
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        Question question = loadOwned(subjectId, questionId, teacherId);
        if (question.getStatus() != QuestionStatus.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "QUESTION_ARCHIVED");
        }
        requireTopic(subjectId, request.topicId());
        QuestionContentValidator.ValidatedQuestion validated = contentValidator.validate(request);
        apply(question, request.topicId(), validated);
        question = questionRepo.save(question);
        optionRepo.deleteByQuestionId(questionId);
        saveOptions(questionId, validated.options());
        return toDetail(question, true);
    }

    @Transactional
    public QuestionDetailResponseDTO archive(UUID subjectId, UUID questionId, UUID teacherId) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        Question question = loadOwned(subjectId, questionId, teacherId);
        if (question.getStatus() != QuestionStatus.ACTIVE) {
            throw error(HttpStatus.CONFLICT, "QUESTION_ALREADY_ARCHIVED");
        }
        question.setStatus(QuestionStatus.ARCHIVED);
        question.setDeletedAt(LocalDateTime.now());
        return toDetail(questionRepo.save(question), true);
    }

    @Transactional
    public QuestionDetailResponseDTO restore(UUID subjectId, UUID questionId, UUID teacherId) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        Question question = loadOwned(subjectId, questionId, teacherId);
        if (question.getStatus() != QuestionStatus.ARCHIVED) {
            throw error(HttpStatus.CONFLICT, "QUESTION_ALREADY_ACTIVE");
        }
        question.setStatus(QuestionStatus.ACTIVE);
        question.setDeletedAt(null);
        return toDetail(questionRepo.save(question), true);
    }

    private void apply(
            Question question,
            UUID topicId,
            QuestionContentValidator.ValidatedQuestion validated
    ) {
        question.setTopicId(topicId);
        question.setQuestionType(validated.questionType());
        question.setContent(validated.content());
        question.setContentFormat(validated.contentFormat());
        question.setExplanation(validated.explanation());
        question.setDifficulty(validated.difficulty());
        question.setDefaultScore(validated.defaultScore());
        question.setEstimatedSecond(validated.estimatedSecond());
        question.setVisibility(validated.visibility());
        question.setMetadata(metadataWithImageObjectKey(
                question.getMetadata(),
                mediaStorageService.validateObjectKey(validated.imageObjectKey())
        ));
    }

    private void saveOptions(
            UUID questionId,
            List<QuestionContentValidator.ValidatedOption> options
    ) {
        List<QuestionOption> entities = options.stream().map(value -> {
            QuestionOption option = new QuestionOption();
            option.setQuestionId(questionId);
            option.setOptionKey(value.optionKey());
            option.setContent(value.content());
            option.setContentFormat(value.contentFormat());
            option.setExplanation(value.explanation());
            option.setCorrect(value.correct());
            return option;
        }).toList();
        optionRepo.saveAll(entities);
    }

    private Question load(UUID subjectId, UUID questionId) {
        return questionRepo.findByIdAndSubjectId(questionId, subjectId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND"));
    }

    private Question loadOwned(UUID subjectId, UUID questionId, UUID teacherId) {
        Question question = load(subjectId, questionId);
        if (!teacherId.equals(question.getOwnerTeacherId())) {
            if (question.getVisibility() == QuestionVisibility.PRIVATE) {
                throw error(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND");
            }
            throw error(HttpStatus.FORBIDDEN, "QUESTION_FORBIDDEN");
        }
        return question;
    }

    private void requireTopic(UUID subjectId, UUID topicId) {
        if (topicId == null || !topicRepo.existsByIdAndSubjectId(topicId, subjectId)) {
            throw error(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND");
        }
    }

    private QuestionDetailResponseDTO toDetail(Question question, boolean owner) {
        List<QuestionOptionResponseDTO> options = optionRepo
                .findByQuestionIdOrderByOptionKeyAsc(question.getId())
                .stream()
                .map(option -> new QuestionOptionResponseDTO(
                        option.getId(),
                        option.getOptionKey(),
                        option.getContent(),
                        option.getContentFormat(),
                        owner ? option.getExplanation() : null,
                        owner ? option.isCorrect() : null
                ))
                .toList();
        return new QuestionDetailResponseDTO(
                question.getId(),
                question.getSubjectId(),
                question.getTopicId(),
                question.getOwnerTeacherId(),
                question.getQuestionType(),
                question.getContent(),
                question.getContentFormat(),
                owner ? question.getExplanation() : null,
                question.getDifficulty(),
                question.getDefaultScore(),
                question.getEstimatedSecond(),
                question.getVisibility(),
                imageObjectKey(question.getMetadata()),
                question.getStatus(),
                question.getSource(),
                options,
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    private ResponseStatusException error(HttpStatus status, String code) {
        return new ResponseStatusException(status, code);
    }

    private String metadataWithImageObjectKey(String existingMetadata, String imageObjectKey) {
        Map<String, Object> metadata = readMetadata(existingMetadata);
        if (imageObjectKey == null) {
            metadata.remove("imageObjectKey");
        } else {
            metadata.put("imageObjectKey", imageObjectKey);
        }
        if (metadata.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception exception) {
            throw error(HttpStatus.BAD_REQUEST, "QUESTION_METADATA_INVALID");
        }
    }

    private String imageObjectKey(String metadataJson) {
        Object value = readMetadata(metadataJson).get("imageObjectKey");
        return value instanceof String text ? mediaStorageService.validateObjectKey(text) : null;
    }

    private Map<String, Object> readMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> value = objectMapper.readValue(
                    metadataJson,
                    new TypeReference<Map<String, Object>>() {}
            );
            return value == null ? new LinkedHashMap<>() : new LinkedHashMap<>(value);
        } catch (Exception exception) {
            throw error(HttpStatus.BAD_REQUEST, "QUESTION_METADATA_INVALID");
        }
    }
}
