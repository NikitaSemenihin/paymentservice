package com.innowise.paymentservice.model.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PaymentRequestDto(
        @NotNull(message = "orderId is required")
        @Positive(message = "orderId must be positive")
        Long orderId,
        @NotNull(message = "userId is required")
        @Positive(message = "userId must be positive")
        Long userId,
        @NotNull(message = "paymentAmount is required")
        @DecimalMin(value = "0.01", message = "paymentAmount must be greater than zero")
        BigDecimal paymentAmount
) {
}
