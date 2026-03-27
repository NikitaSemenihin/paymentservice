package com.innowise.paymentservice.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

@Component
public class KafkaPaymentEventPublisher implements PaymentEventPublisher {
    private static final Duration SEND_TIMEOUT = Duration.ofSeconds(10);

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
    public void publishPaymentCreated(PaymentCreatedEvent event) {
        try {
            kafkaTemplate.send(paymentCreatedTopic, event.id(), event)
                    .get(SEND_TIMEOUT.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new com.innowise.paymentservice.exception.ExternalServiceException(
                    "Interrupted while publishing payment.created event",
                    exception
            );
        } catch (ExecutionException | java.util.concurrent.TimeoutException exception) {
            throw new com.innowise.paymentservice.exception.ExternalServiceException(
                    "Failed to publish payment.created event",
                    exception
            );
        }
    }
}
