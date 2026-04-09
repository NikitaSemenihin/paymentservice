package com.innowise.paymentservice.repository.impl;

import com.innowise.paymentservice.model.entity.Payment;
import com.innowise.paymentservice.repository.PaymentRepositoryCustom;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentRepositoryCustomImpl implements PaymentRepositoryCustom {
    private final MongoTemplate mongoTemplate;

    public PaymentRepositoryCustomImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public BigDecimal totalSumForCurrentUser(Instant from, Instant to, Long userId) {
        Criteria criteria = Criteria.where("user_id").is(userId)
                .and("timestamp").gte(from).lt(to);
        return calculateTotalSum(criteria);
    }

    @Override
    public BigDecimal totalSumForAllUsers(Instant from, Instant to) {
        Criteria criteria = Criteria.where("timestamp").gte(from).lt(to);
        return calculateTotalSum(criteria);
    }

    private BigDecimal calculateTotalSum(Criteria criteria) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group().sum("payment_amount").as("total")
        );

        AggregationResults<TotalSumResult> results = mongoTemplate.aggregate(
                aggregation,
                Payment.class,
                TotalSumResult.class
        );

        TotalSumResult result = results.getUniqueMappedResult();
        return result != null && result.getTotal() != null
                ? result.getTotal()
                : BigDecimal.ZERO;
    }

    @Setter
    @Getter
    private static class TotalSumResult {
        private BigDecimal total;

    }
}
