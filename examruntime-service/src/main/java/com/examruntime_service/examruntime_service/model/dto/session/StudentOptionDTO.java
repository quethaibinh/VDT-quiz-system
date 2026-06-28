package com.examruntime_service.examruntime_service.model.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO option an toan cho hoc sinh (khong chua truong dung/sai)
public class StudentOptionDTO {
    private UUID optionId;
    private String key;
    private String content;
    private String contentFormat;
}
