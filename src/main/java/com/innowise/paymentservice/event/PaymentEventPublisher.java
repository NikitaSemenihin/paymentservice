package com.innowise.paymentservice.event;

public interface PaymentEventPublisher {
    void publishPaymentCreated(PaymentCreatedEvent event);
}
