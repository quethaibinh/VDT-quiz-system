package com.examruntime_service.examruntime_service.model.dto.session;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Yeu cau nop bai, co the kem batch dap an cuoi de flush truoc deadline.
public class SubmitRequestDTO {

    @NotBlank
    @Size(max = 128)
    private String idempotencyKey;

    private long clientSeq;

    private List<AutosaveAnswerDTO> finalAnswers;
}
