package com.innowise.paymentservice.config;

public record RequestAuthContext(
        Long userId,
        RequesterRole role,
        String serviceName
) {
    public boolean isAdmin() {
        return role == RequesterRole.ADMIN;
    }

    public boolean hasUserOrAdminRole() {
        return userId != null && role != null;
    }
}
