package com.innowise.paymentservice.model.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TotalAmountResponseDto(
        Instant from,
        Instant to,
        Long userId,
        BigDecimal totalAmount
) {
}
