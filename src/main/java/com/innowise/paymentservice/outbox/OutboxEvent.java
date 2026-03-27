package com.innowise.paymentservice.outbox;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {
    @Id
    private ObjectId id;

    @Field("aggregate_type")
    private String aggregateType;

    @Field("aggregate_id")
    private String aggregateId;

    @Field("event_type")
    private String eventType;

    @Field("payload")
    private String payload;

    @Field("status")
    private OutboxEventStatus status;

    @Field("attempt_count")
    private int attemptCount;

    @Field("created_at")
    private Instant createdAt;

    @Field("published_at")
    private Instant publishedAt;

    @Field("last_error")
    private String lastError;

    @Field("next_retry_at")
    private Instant nextRetryAt;

    @Field("locked_at")
    private Instant lockedAt;
}
