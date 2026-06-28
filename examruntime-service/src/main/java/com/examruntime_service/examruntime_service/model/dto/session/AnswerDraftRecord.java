package com.examruntime_service.examruntime_service.model.dto.session;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Ban ghi dap an nong trong Redis cho mot cau hoi cua mot session.
public class AnswerDraftRecord {
    private UUID questionId;
    private List<UUID> selectedOptionIds;
    private String answerText;
    private boolean markedForReview;
    private long clientSeq;
    private long serverSeq;
    private OffsetDateTime serverReceivedAt;
}
