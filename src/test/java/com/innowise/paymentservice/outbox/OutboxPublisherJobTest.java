package com.innowise.paymentservice.outbox;

import com.innowise.paymentservice.event.PaymentCreatedEvent;
import com.innowise.paymentservice.event.PaymentEventPublisher;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherJobTest {
    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private OutboxEventFactory outboxEventFactory;

    @Mock
    private PaymentEventPublisher paymentEventPublisher;

    private OutboxPublisherJob outboxPublisherJob;

    @BeforeEach
    void setUp() {
        outboxPublisherJob = new OutboxPublisherJob(
                outboxEventRepository,
                mongoTemplate,
                outboxEventFactory,
                paymentEventPublisher,
                3,
                1000
        );
    }

    @Test
    void publishPendingEventsShouldPublishAndMarkOutboxEventAsPublished() {
        OutboxEvent pendingEvent = new OutboxEvent();
        ObjectId outboxId = new ObjectId("64f0c3d5a3a8435b2dd5d911");
        pendingEvent.setId(outboxId);
        pendingEvent.setStatus(OutboxEventStatus.NEW);
        pendingEvent.setEventType("PAYMENT_CREATED");
        pendingEvent.setPayload("{\"eventId\":\"evt-1\"}");
        pendingEvent.setCreatedAt(Instant.parse("2026-03-27T00:00:00Z"));
        pendingEvent.setNextRetryAt(Instant.parse("2026-03-27T00:00:00Z"));

        PaymentCreatedEvent createdEvent = new PaymentCreatedEvent(
                "evt-1",
                "payment-1",
                10L,
                20L,
                PaymentStatus.SUCCESS,
                Instant.parse("2026-03-27T00:00:00Z"),
                BigDecimal.ONE
        );

        when(outboxEventRepository.findTopByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(any(), any(), any(Limit.class)))
                .thenReturn(List.of(pendingEvent));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(OutboxEvent.class)))
                .thenReturn(pendingEvent);
        when(outboxEventFactory.readPaymentCreated(pendingEvent.getPayload())).thenReturn(createdEvent);

        outboxPublisherJob.publishPendingEvents();

        verify(paymentEventPublisher).publishPaymentCreated(createdEvent);
        verify(mongoTemplate).updateFirst(any(Query.class), any(Update.class), eq(OutboxEvent.class));
    }

    @Test
    void publishPendingEventsShouldScheduleRetryWhenPublisherFails() {
        OutboxEvent pendingEvent = new OutboxEvent();
        ObjectId outboxId = new ObjectId("64f0c3d5a3a8435b2dd5d912");
        pendingEvent.setId(outboxId);
        pendingEvent.setStatus(OutboxEventStatus.NEW);
        pendingEvent.setEventType("PAYMENT_CREATED");
        pendingEvent.setPayload("{\"eventId\":\"evt-2\"}");
        pendingEvent.setAttemptCount(1);
        pendingEvent.setCreatedAt(Instant.parse("2026-03-27T00:00:00Z"));
        pendingEvent.setNextRetryAt(Instant.parse("2026-03-27T00:00:00Z"));

        PaymentCreatedEvent createdEvent = new PaymentCreatedEvent(
                "evt-2",
                "payment-2",
                11L,
                21L,
                PaymentStatus.FAILED,
                Instant.parse("2026-03-27T00:00:00Z"),
                BigDecimal.TEN
        );

        when(outboxEventRepository.findTopByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(any(), any(), any(Limit.class)))
                .thenReturn(List.of(pendingEvent));
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class), eq(OutboxEvent.class)))
                .thenReturn(pendingEvent);
        when(outboxEventFactory.readPaymentCreated(pendingEvent.getPayload())).thenReturn(createdEvent);
        doThrow(new IllegalStateException("Kafka unavailable"))
                .when(paymentEventPublisher).publishPaymentCreated(createdEvent);

        outboxPublisherJob.publishPendingEvents();

        ArgumentCaptor<Update> updateCaptor = ArgumentCaptor.forClass(Update.class);
        verify(mongoTemplate).updateFirst(any(Query.class), updateCaptor.capture(), eq(OutboxEvent.class));

        Document updateDocument = updateCaptor.getValue().getUpdateObject();
        Document setDocument = updateDocument.get("$set", Document.class);
        assertThat(setDocument).containsKeys("attempt_count", "next_retry_at", "last_error", "status");
    }
}
