package com.innowise.paymentservice.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.innowise.paymentservice.model.entity.PaymentStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCreatedEventContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void paymentCreatedEventShouldSerializeWithDocumentedContractShape() throws Exception {
        PaymentCreatedEvent event = new PaymentCreatedEvent(
                "evt-123",
                "67e47f9b3f8a4b12c8f6ab21",
                101L,
                202L,
                PaymentStatus.SUCCESS,
                Instant.parse("2026-03-27T10:15:30Z"),
                BigDecimal.valueOf(49.99)
        );

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsBytes(event));
        List<String> fieldNames = new ArrayList<>();
        json.fieldNames().forEachRemaining(fieldNames::add);

        assertThat(fieldNames)
                .containsExactly(
                        "eventId",
                        "id",
                        "orderId",
                        "userId",
                        "status",
                        "timestamp",
                        "paymentAmount"
                );
        assertThat(json.get("eventId").asText()).isEqualTo("evt-123");
        assertThat(json.get("id").asText()).isEqualTo("67e47f9b3f8a4b12c8f6ab21");
        assertThat(json.get("orderId").asLong()).isEqualTo(101L);
        assertThat(json.get("userId").asLong()).isEqualTo(202L);
        assertThat(json.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(json.get("timestamp").asText()).isEqualTo("2026-03-27T10:15:30Z");
        assertThat(json.get("paymentAmount").decimalValue()).isEqualByComparingTo("49.99");
    }
}
