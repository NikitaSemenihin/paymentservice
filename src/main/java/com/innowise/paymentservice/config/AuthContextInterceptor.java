package com.innowise.paymentservice.config;

import com.innowise.paymentservice.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;

@Component
public class AuthContextInterceptor implements HandlerInterceptor {
    public static final String AUTH_CONTEXT_ATTR = "REQUEST_AUTH_CONTEXT";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String SERVICE_NAME_HEADER = "X-Service-Name";

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        String serviceNameHeader = request.getHeader(SERVICE_NAME_HEADER);
        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String userRoleHeader = request.getHeader(USER_ROLE_HEADER);

        String serviceName = normalizeServiceName(serviceNameHeader);
        RequestAuthContext context;

        if (serviceName != null) {
            context = new RequestAuthContext(null, null, serviceName);
        } else {
            Long userId = parseUserId(userIdHeader);
            RequesterRole role = parseRole(userRoleHeader);
            context = new RequestAuthContext(userId, role, null);
        }

        request.setAttribute(AUTH_CONTEXT_ATTR, context);
        return true;
    }

    private String normalizeServiceName(String serviceNameHeader) {
        if (serviceNameHeader == null || serviceNameHeader.isBlank()) {
            return null;
        }
        return serviceNameHeader.trim().toLowerCase(Locale.ROOT);
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new UnauthorizedException("Missing X-User-Id header");
        }
        try {
            Long userId = Long.parseLong(userIdHeader);
            if (userId <= 0) {
                throw new UnauthorizedException("X-User-Id must be positive");
            }
            return userId;
        } catch (NumberFormatException exception) {
            throw new UnauthorizedException("Invalid X-User-Id header");
        }
    }

    private RequesterRole parseRole(String userRoleHeader) {
        if (userRoleHeader == null || userRoleHeader.isBlank()) {
            throw new UnauthorizedException("Missing X-User-Role header");
        }
        try {
            return RequesterRole.valueOf(userRoleHeader.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new UnauthorizedException("Invalid X-User-Role header");
        }
    }
}
