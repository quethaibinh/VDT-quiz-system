package com.question_service.question_service.service.imports;

import com.question_service.question_service.model.dto.imports.NormalizedImportQuestionRow;
import com.question_service.question_service.model.dto.imports.NormalizedQuestionOptionDTO;
import com.question_service.question_service.model.entity.ImportJob;
import com.question_service.question_service.model.entity.ImportStatus;
import com.question_service.question_service.model.entity.Question;
import com.question_service.question_service.model.entity.QuestionOption;
import com.question_service.question_service.model.entity.QuestionStatus;
import com.question_service.question_service.model.entity.Source;
import com.question_service.question_service.model.entity.Topic;
import com.question_service.question_service.repository.ImportJobRepo;
import com.question_service.question_service.repository.QuestionOptionRepo;
import com.question_service.question_service.repository.QuestionRepo;
import com.question_service.question_service.repository.TopicRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
/**
 * Luu import job, topic, cau hoi va dap an trong cung mot transaction.
 */
public class ImportedQuestionCreationService {

    private final ImportJobRepo importJobRepo;
    private final TopicRepo topicRepo;
    private final QuestionRepo questionRepo;
    private final QuestionOptionRepo questionOptionRepo;

    public ImportedQuestionCreationService(
            ImportJobRepo importJobRepo,
            TopicRepo topicRepo,
            QuestionRepo questionRepo,
            QuestionOptionRepo questionOptionRepo
    ) {
        this.importJobRepo = importJobRepo;
        this.topicRepo = topicRepo;
        this.questionRepo = questionRepo;
        this.questionOptionRepo = questionOptionRepo;
    }

    @Transactional
    /**
     * Tao topic khi chua ton tai va gan moi cau hoi vao import job hien tai.
     */
    public ImportedQuestionCreationResult createQuestions(
            UUID subjectId,
            UUID teacherId,
            MultipartFile file,
            List<NormalizedImportQuestionRow> rows
    ) {
        ImportJob importJob = createImportJob(subjectId, teacherId, file);
        Map<String, Topic> topicCache = new HashMap<>();
        int createdTopicCount = 0;
        int createdQuestionCount = 0;

        for (NormalizedImportQuestionRow row : rows) {
            TopicResolution topicResolution = resolveTopic(subjectId, row.getTopicName(), topicCache);
            if (topicResolution.created()) {
                createdTopicCount++;
            }

            Question question = createQuestion(subjectId, teacherId, importJob.getId(), topicResolution.topic(), row);
            createOptions(question, row);
            createdQuestionCount++;
        }

        importJob.setImportStatus(ImportStatus.IMPORTED);
        importJob.setCreatedQuestionCount(createdQuestionCount);
        importJobRepo.save(importJob);

        return new ImportedQuestionCreationResult(
                importJob.getId(),
                createdTopicCount,
                createdQuestionCount
        );
    }

    private ImportJob createImportJob(UUID subjectId, UUID teacherId, MultipartFile file) {
        ImportJob importJob = new ImportJob();
        importJob.setUploadBy(teacherId);
        importJob.setSubjectId(subjectId);
        importJob.setFileName(file.getOriginalFilename());
        importJob.setFileSize((double) file.getSize());
        importJob.setImportStatus(ImportStatus.VALIDATING);
        importJob.setCreatedQuestionCount(0);
        return importJobRepo.save(importJob);
    }

    private TopicResolution resolveTopic(
            UUID subjectId,
            String topicName,
            Map<String, Topic> topicCache
    ) {
        String normalizedName = topicName.trim();
        String cacheKey = normalizedName.toLowerCase(Locale.ROOT);
        Topic cachedTopic = topicCache.get(cacheKey);
        // Bo nho tam tranh truy van hoac tao lai cung chu de trong mot tep import.
        if (cachedTopic != null) {
            return new TopicResolution(cachedTopic, false);
        }

        Topic existingTopic = topicRepo.findBySubjectIdAndNameIgnoreCase(subjectId, normalizedName).orElse(null);
        if (existingTopic != null) {
            topicCache.put(cacheKey, existingTopic);
            return new TopicResolution(existingTopic, false);
        }

        Topic topic = new Topic();
        topic.setSubjectId(subjectId);
        topic.setName(normalizedName);
        topic.setSlug(toSlug(normalizedName));
        topic = topicRepo.save(topic);
        topicCache.put(cacheKey, topic);
        return new TopicResolution(topic, true);
    }

    private Question createQuestion(
            UUID subjectId,
            UUID teacherId,
            UUID importJobId,
            Topic topic,
            NormalizedImportQuestionRow row
    ) {
        Question question = new Question();
        question.setSubjectId(subjectId);
        question.setTopicId(topic.getId());
        question.setOwnerTeacherId(teacherId);
        question.setQuestionType(row.getQuestionType());
        question.setContent(row.getContent());
        question.setContentFormat(row.getContentFormat());
        question.setExplanation(row.getExplanation());
        question.setDifficulty(row.getDifficulty());
        question.setDefaultScore(row.getDefaultScore());
        question.setEstimatedSecond(row.getEstimatedSecond());
        question.setVisibility(row.getVisibility());
        question.setStatus(QuestionStatus.ACTIVE);
        question.setSource(Source.EXCEL_IMPORT);
        question.setImportJobId(importJobId);
        return questionRepo.save(question);
    }

    private void createOptions(Question question, NormalizedImportQuestionRow row) {
        for (NormalizedQuestionOptionDTO optionDTO : row.getOptions()) {
            QuestionOption option = new QuestionOption();
            option.setQuestionId(question.getId());
            option.setOptionKey(optionDTO.getOptionKey());
            option.setContent(optionDTO.getContent());
            option.setContentFormat(row.getContentFormat());
            option.setCorrect(row.getCorrectOptionKeys().contains(optionDTO.getOptionKey()));
            questionOptionRepo.save(option);
        }
    }

    private String toSlug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "topic" : normalized;
    }

    private record TopicResolution(Topic topic, boolean created) {
    }

}
