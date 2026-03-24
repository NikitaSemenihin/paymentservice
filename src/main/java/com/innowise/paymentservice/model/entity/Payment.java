package com.innowise.paymentservice.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(collection = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {
    @Id
    private ObjectId id;

    @Field("order_id")
    private Long orderId;

    @Field("user_id")
    private Long userId;

    @Field("status")
    private PaymentStatus status;

    @Field("timestamp")
    private Instant timestamp;

    @Field(value = "payment_amount", targetType = FieldType.DECIMAL128)
    private BigDecimal paymentAmount;

    public Payment(ObjectId id, Long orderId, Long userId, PaymentStatus status, Instant timestamp,
                   BigDecimal paymentAmount) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.status = status;
        this.timestamp = timestamp;
        this.paymentAmount = paymentAmount;
    }
}
