package com.result_service.result_service.client;

import java.util.UUID;

public record StudentSummary(
        UUID id,
        String studentCode,
        String fullName,
        String displayName
) {
}
