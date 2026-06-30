package com.examruntime_service.examruntime_service.config.websocket;

import com.examruntime_service.examruntime_service.config.security.ExamRuntimeUserPrincipal;

import java.security.Principal;

public record WebSocketPrincipal(ExamRuntimeUserPrincipal user) implements Principal {

    @Override
    public String getName() {
        return user.userId();
    }

    public boolean hasRole(String role) {
        return user.role() != null && user.role().equals("ROLE_" + role);
    }
}
