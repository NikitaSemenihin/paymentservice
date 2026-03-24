package com.innowise.paymentservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.innowise.paymentservice.event.PaymentCreatedEvent;
import com.innowise.paymentservice.model.dto.PaymentRequestDto;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class PaymentFlowIntegrationTest {
    private static final String TOPIC = "payment.created";

    @Container
    static final MongoDBContainer MONGO_DB_CONTAINER =
            new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Container
    static final KafkaContainer KAFKA_CONTAINER =
            new KafkaContainer(DockerImageName.parse("apache/kafka-native:3.8.0"));

    @RegisterExtension
    static final WireMockExtension WIRE_MOCK = WireMockExtension.newInstance()
            .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_DB_CONTAINER::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", KAFKA_CONTAINER::getBootstrapServers);
        registry.add("app.random-number.base-url", WIRE_MOCK::baseUrl);
        registry.add("app.kafka.payment-created-topic", () -> TOPIC);
    }

    @Test
    void createPaymentShouldPersistDocumentAndPublishKafkaEvent() throws Exception {
        WIRE_MOCK.stubFor(get(urlPathEqualTo("/api/v1.0/random"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[8]")));

        PaymentRequestDto request = new PaymentRequestDto(101L, 202L, BigDecimal.valueOf(49.99));

        mockMvc.perform(post("/api/payments")
                        .header("X-User-Id", "202")
                        .header("X-User-Role", "USER")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(101))
                .andExpect(jsonPath("$.userId").value(202))
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        List<Map> payments = mongoTemplate.findAll(Map.class, "payments");
        assertThat(payments).hasSize(1);
        assertThat(payments.getFirst())
                .containsEntry("order_id", 101L)
                .containsEntry("user_id", 202L)
                .containsEntry("status", "SUCCESS");

        ConsumerRecord<String, PaymentCreatedEvent> record = pollSingleRecord();
        assertThat(record.topic()).isEqualTo(TOPIC);
        assertThat(record.value().orderId()).isEqualTo(101L);
        assertThat(record.value().userId()).isEqualTo(202L);
        assertThat(record.value().status().name()).isEqualTo("SUCCESS");
        assertThat(record.value().paymentAmount()).isEqualByComparingTo("49.99");
    }

    private ConsumerRecord<String, PaymentCreatedEvent> pollSingleRecord() {
        try (KafkaConsumer<String, PaymentCreatedEvent> consumer = new KafkaConsumer<>(consumerProperties())) {
            consumer.subscribe(List.of(TOPIC));

            long deadline = System.currentTimeMillis() + Duration.ofSeconds(15).toMillis();
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, PaymentCreatedEvent> records = consumer.poll(Duration.ofSeconds(1));
                if (!records.isEmpty()) {
                    return records.iterator().next();
                }
            }
        }

        throw new AssertionError("Expected Kafka event was not published");
    }

    private Properties consumerProperties() {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KAFKA_CONTAINER.getBootstrapServers());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, "payment-flow-test");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        properties.put(JsonDeserializer.TRUSTED_PACKAGES, "com.innowise.paymentservice.event");
        properties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentCreatedEvent.class.getName());
        return properties;
    }
}
