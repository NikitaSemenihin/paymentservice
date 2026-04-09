package com.innowise.paymentservice.outbox;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OutboxEventRepository extends MongoRepository<OutboxEvent, ObjectId> {
    List<OutboxEvent> findTopByStatusInAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            Collection<OutboxEventStatus> statuses,
            Instant nextRetryAt,
            org.springframework.data.domain.Limit limit
    );
}
