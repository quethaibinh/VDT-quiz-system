package com.auth_service.auth_service.model.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportUserResultDTO {

    private int totalRows;
    private int successCount;
    private int failedCount;
    private List<ImportUserErrorDTO> errors = new ArrayList<>();

    public void increaseSuccessCount() {
        this.successCount++;
    }

    public void addError(ImportUserErrorDTO error) {
        this.errors.add(error);
        this.failedCount = this.errors.size();
    }
}
