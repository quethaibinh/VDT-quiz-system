package com.question_service.question_service.service.imports;

import com.question_service.question_service.model.dto.imports.ImportQuestionErrorDTO;
import com.question_service.question_service.model.dto.imports.NormalizedImportQuestionRow;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportQuestionValidationResult {

    private List<NormalizedImportQuestionRow> rows = new ArrayList<>();
    private List<ImportQuestionErrorDTO> errors = new ArrayList<>();

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void addError(ImportQuestionErrorDTO error) {
        errors.add(error);
    }

}
