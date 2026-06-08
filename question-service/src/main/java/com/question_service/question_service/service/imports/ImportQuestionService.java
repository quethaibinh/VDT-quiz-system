package com.question_service.question_service.service.imports;

import com.question_service.question_service.model.dto.imports.ImportQuestionErrorDTO;
import com.question_service.question_service.model.dto.imports.ImportQuestionResultDTO;
import com.question_service.question_service.model.dto.imports.ImportQuestionRowDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class ImportQuestionService {

    private final ExcelQuestionParser excelQuestionParser;
    private final ImportQuestionValidator importQuestionValidator;
    private final ImportedQuestionCreationService importedQuestionCreationService;

    public ImportQuestionService(
            ExcelQuestionParser excelQuestionParser,
            ImportQuestionValidator importQuestionValidator,
            ImportedQuestionCreationService importedQuestionCreationService
    ) {
        this.excelQuestionParser = excelQuestionParser;
        this.importQuestionValidator = importQuestionValidator;
        this.importedQuestionCreationService = importedQuestionCreationService;
    }

    public ImportQuestionResultDTO importQuestions(
            UUID subjectId,
            UUID teacherId,
            MultipartFile file
    ) throws Exception {
        ImportQuestionResultDTO result = new ImportQuestionResultDTO();
        List<ImportQuestionRowDTO> rows;

        try {
            rows = excelQuestionParser.parse(file);
        } catch (Exception exception) {
            result.addError(new ImportQuestionErrorDTO(
                    0,
                    "file",
                    "INVALID_FILE",
                    exception.getMessage()
            ));
            return result;
        }

        result.setTotalRows(rows.size());
        if (rows.isEmpty()) {
            result.addError(new ImportQuestionErrorDTO(
                    0,
                    "file",
                    "NO_ROWS",
                    "Excel file has no question rows"
            ));
            return result;
        }

        ImportQuestionValidationResult validationResult = importQuestionValidator.validate(subjectId, rows);

        if (validationResult.hasErrors()) {
            for (ImportQuestionErrorDTO error : validationResult.getErrors()) {
                result.addError(error);
            }
            return result;
        }

        ImportedQuestionCreationResult creationResult = importedQuestionCreationService.createQuestions(
                subjectId,
                teacherId,
                file,
                validationResult.getRows()
        );

        result.setImported(true);
        result.setImportJobId(creationResult.getImportJobId());
        result.setCreatedTopicCount(creationResult.getCreatedTopicCount());
        result.setCreatedQuestionCount(creationResult.getCreatedQuestionCount());
        result.setSuccessCount(validationResult.getRows().size());
        result.setFailedCount(0);
        return result;
    }

}
