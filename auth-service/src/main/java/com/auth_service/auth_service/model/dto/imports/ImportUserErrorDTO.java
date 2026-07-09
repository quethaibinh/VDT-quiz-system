package com.auth_service.auth_service.model.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportUserErrorDTO {

    private int rowNumber;
    private String fieldName;
    private String errorCode;
    private String message;
}
