package com.innowise.paymentservice.event;

import com.innowise.paymentservice.model.dto.PaymentResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {
    private final KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate;
    private final String paymentCreatedTopic;

    public KafkaPaymentEventPublisher(
            KafkaTemplate<String, PaymentCreatedEvent> kafkaTemplate,
            @Value("${app.kafka.payment-created-topic}") String paymentCreatedTopic
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentCreatedTopic = paymentCreatedTopic;
    }

    @Override
    public void publishPaymentCreated(PaymentResponseDto payment) {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                payment.id(),
                payment.orderId(),
                payment.userId(),
                payment.status(),
                payment.timestamp(),
                payment.paymentAmount()
        );

        kafkaTemplate.send(paymentCreatedTopic, payment.id(), event);
    }
}
