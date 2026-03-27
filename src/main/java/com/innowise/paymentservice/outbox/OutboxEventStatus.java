package com.innowise.paymentservice.outbox;

public enum OutboxEventStatus {
    NEW,
    PROCESSING,
    PUBLISHED,
    FAILED
}
