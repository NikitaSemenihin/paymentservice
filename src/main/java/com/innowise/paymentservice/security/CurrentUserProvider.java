package com.innowise.paymentservice.security;

import com.innowise.paymentservice.exception.AccessDeniedException;
import com.innowise.paymentservice.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {
    private final String adminRole;

    public CurrentUserProvider(@Value("${app.security.admin-role}") String adminRole) {
        this.adminRole = adminRole;
    }

    public Long getCurrentUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("X-User-Id header must contain a numeric value");
        }
    }

    public void ensureAuthenticated(Long currentUserId, String userRoleHeader) {
        if (currentUserId == null) {
            throw new AccessDeniedException("X-User-Id header is required");
        }
    }

    public void ensureAdmin(Long currentUserId, String userRoleHeader) {
        ensureAuthenticated(currentUserId, userRoleHeader);
        if (userRoleHeader == null || !adminRole.equalsIgnoreCase(userRoleHeader)) {
            throw new AccessDeniedException("Admin role is required to access this resource");
        }
    }
}
