package com.question_service.question_service.model.dto.collections;

import com.question_service.question_service.model.entity.enums.ContentFormat;
import com.question_service.question_service.model.entity.enums.OptionKey;

import java.util.UUID;

/**
 * Du lieu option noi bo dung de dong bang de thi.
 * DTO nay co dap an dung nen khong duoc tai su dung cho API hoc sinh.
 */
public record ExamOptionSnapshotDTO(
        UUID optionId,
        OptionKey key,
        String content,
        ContentFormat contentFormat,
        boolean correct
) {
}
