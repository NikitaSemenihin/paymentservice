package com.innowise.paymentservice.outbox;

import com.innowise.paymentservice.event.PaymentCreatedEvent;
import com.innowise.paymentservice.event.PaymentEventPublisher;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class OutboxPublisherJob {
    private static final String PAYMENT_CREATED_EVENT_TYPE = "PAYMENT_CREATED";
    private static final int MAX_ERROR_LENGTH = 1000;
    private static final int BATCH_SIZE = 50;

    private final OutboxEventRepository outboxEventRepository;
    private final MongoTemplate mongoTemplate;
    private final OutboxEventFactory outboxEventFactory;
    private final PaymentEventPublisher paymentEventPublisher;
    private final int maxAttempts;
    private final long initialBackoffMs;

    public OutboxPublisherJob(
            OutboxEventRepository outboxEventRepository,
            MongoTemplate mongoTemplate,
            OutboxEventFactory outboxEventFactory,
            PaymentEventPublisher paymentEventPublisher,
            @Value("${app.outbox.max-attempts:10}") int maxAttempts,
            @Value("${app.outbox.initial-backoff-ms:5000}") long initialBackoffMs
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.mongoTemplate = mongoTemplate;
        this.outboxEventFactory = outboxEventFactory;
        this.paymentEventPublisher = paymentEventPublisher;
        this.maxAttempts = maxAttempts;
        this.initialBackoffMs = initialBackoffMs;
    }

    @Scheduled(fixedDelayString = "${app.outbox.fixed-delay-ms:5000}")
    public void publishPendingEvents() {
        Instant now = Instant.now();
        List<OutboxEvent> candidates = outboxEventRepository
                .findTopByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(OutboxEventStatus.NEW, OutboxEventStatus.FAILED),
                        now,
                        Limit.of(BATCH_SIZE)
                );

        candidates.stream()
                .map(candidate -> claim(candidate.getId(), now))
                .filter(claimed -> claimed != null)
                .forEach(this::publishClaimedEvent);
    }

    private OutboxEvent claim(ObjectId id, Instant now) {
        Query query = new Query(Criteria.where("_id").is(id)
                .and("status").in(OutboxEventStatus.NEW, OutboxEventStatus.FAILED)
                .and("next_retry_at").lte(now));

        Update update = new Update()
                .set("status", OutboxEventStatus.PROCESSING)
                .set("locked_at", now);

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true),
                OutboxEvent.class
        );
    }

    private void publishClaimedEvent(OutboxEvent outboxEvent) {
        try {
            publish(outboxEvent);
            markPublished(outboxEvent.getId());
        } catch (Exception exception) {
            markFailed(outboxEvent, exception);
        }
    }

    private void publish(OutboxEvent outboxEvent) {
        if (!PAYMENT_CREATED_EVENT_TYPE.equals(outboxEvent.getEventType())) {
            throw new IllegalStateException("Unsupported outbox event type: " + outboxEvent.getEventType());
        }

        PaymentCreatedEvent event = outboxEventFactory.readPaymentCreated(outboxEvent.getPayload());
        paymentEventPublisher.publishPaymentCreated(event);
    }

    private void markPublished(ObjectId id) {
        Query query = new Query(Criteria.where("_id").is(id));
        Update update = new Update()
                .set("status", OutboxEventStatus.PUBLISHED)
                .set("published_at", Instant.now())
                .unset("last_error")
                .unset("locked_at");

        mongoTemplate.updateFirst(query, update, OutboxEvent.class);
    }

    private void markFailed(OutboxEvent outboxEvent, Exception exception) {
        int nextAttempt = outboxEvent.getAttemptCount() + 1;
        boolean terminalFailure = nextAttempt >= maxAttempts;
        Query query = new Query(Criteria.where("_id").is(outboxEvent.getId()));
        Update update = new Update()
                .set("status", OutboxEventStatus.FAILED)
                .set("attempt_count", nextAttempt)
                .set("last_error", abbreviateError(exception))
                .unset("locked_at");

        if (terminalFailure) {
            update.unset("next_retry_at");
        } else {
            update.set("next_retry_at", Instant.now().plus(calculateBackoff(nextAttempt)));
        }

        mongoTemplate.updateFirst(query, update, OutboxEvent.class);
    }

    private Duration calculateBackoff(int attempt) {
        long multiplier = 1L << Math.min(attempt - 1, 10);
        return Duration.ofMillis(initialBackoffMs * multiplier);
    }

    private String abbreviateError(Exception exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }
        return message.length() <= MAX_ERROR_LENGTH
                ? message
                : message.substring(0, MAX_ERROR_LENGTH);
    }
}
