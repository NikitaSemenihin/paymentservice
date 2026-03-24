package com.innowise.paymentservice.event;

import com.innowise.paymentservice.model.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCreatedEvent(
        String id,
        Long orderId,
        Long userId,
        PaymentStatus status,
        Instant timestamp,
        BigDecimal paymentAmount
) {
}
