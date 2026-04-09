package com.innowise.paymentservice.repository;

import java.math.BigDecimal;
import java.time.Instant;

public interface PaymentRepositoryCustom {
    BigDecimal totalSumForCurrentUser(Instant from, Instant to, Long userId);
    BigDecimal totalSumForAllUsers(Instant from, Instant to);
}
