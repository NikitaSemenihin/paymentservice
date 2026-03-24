package com.innowise.paymentservice.event;

import com.innowise.paymentservice.model.dto.PaymentResponseDto;

public interface PaymentEventPublisher {
    void publishPaymentCreated(PaymentResponseDto payment);
}
