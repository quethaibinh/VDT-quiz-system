package com.examruntime_service.examruntime_service.model.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO chua danh sach cau tra loi kem client sequence trong yeu cau tu dong luu (autosave)
public class AutosaveRequestDTO {
    private long clientSeq;
    private List<AutosaveAnswerDTO> answers;
}
