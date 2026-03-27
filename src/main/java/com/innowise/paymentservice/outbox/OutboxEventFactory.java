package com.innowise.paymentservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.innowise.paymentservice.event.PaymentCreatedEvent;
import com.innowise.paymentservice.exception.ExternalServiceException;
import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxEventFactory {
    private static final String PAYMENT_AGGREGATE_TYPE = "PAYMENT";
    private static final String PAYMENT_CREATED_EVENT_TYPE = "PAYMENT_CREATED";

    private final ObjectMapper objectMapper;

    public OutboxEventFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OutboxEvent paymentCreated(PaymentResponseDto payment) {
        Instant createdAt = Instant.now();
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                UUID.randomUUID().toString(),
                payment.id(),
                payment.orderId(),
                payment.userId(),
                payment.status(),
                payment.timestamp(),
                payment.paymentAmount()
        );

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType(PAYMENT_AGGREGATE_TYPE);
        outboxEvent.setAggregateId(payment.id());
        outboxEvent.setEventType(PAYMENT_CREATED_EVENT_TYPE);
        outboxEvent.setPayload(serialize(event));
        outboxEvent.setStatus(OutboxEventStatus.NEW);
        outboxEvent.setAttemptCount(0);
        outboxEvent.setCreatedAt(createdAt);
        outboxEvent.setNextRetryAt(createdAt);
        return outboxEvent;
    }

    public PaymentCreatedEvent readPaymentCreated(String payload) {
        try {
            return objectMapper.readValue(payload, PaymentCreatedEvent.class);
        } catch (JsonProcessingException exception) {
            throw new ExternalServiceException("Failed to deserialize outbox payload", exception);
        }
    }

    private String serialize(PaymentCreatedEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new ExternalServiceException("Failed to serialize outbox payload", exception);
        }
    }
}
