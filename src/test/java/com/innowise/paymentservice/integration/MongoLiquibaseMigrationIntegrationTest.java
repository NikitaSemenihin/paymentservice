package com.innowise.paymentservice.integration;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class MongoLiquibaseMigrationIntegrationTest {
    @Container
    static final MongoDBContainer MONGO_DB_CONTAINER =
            new MongoDBContainer(DockerImageName.parse("mongo:7.0"));

    @Autowired
    private MongoTemplate mongoTemplate;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", MONGO_DB_CONTAINER::getReplicaSetUrl);
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("app.random-number.base-url", () -> "http://localhost:8080");
    }

    @Test
    void liquibaseShouldCreateCollectionIndexesAndValidator() {
        assertThat(mongoTemplate.collectionExists("payments")).isTrue();

        List<String> indexNames = mongoTemplate.getCollection("payments")
                .listIndexes(Document.class)
                .into(new java.util.ArrayList<>())
                .stream()
                .map(index -> index.getString("name"))
                .toList();

        assertThat(indexNames).contains(
                "_id_",
                "idx_payments_order_id",
                "idx_payments_user_id",
                "idx_payments_timestamp",
                "idx_payments_status",
                "idx_payments_user_id_timestamp"
        );

        Document listCollectionsResult = mongoTemplate.getDb().runCommand(new Document("listCollections", 1)
                .append("filter", new Document("name", "payments")));
        Document firstCollection = listCollectionsResult
                .get("cursor", Document.class)
                .getList("firstBatch", Document.class)
                .getFirst();

        Document validator = firstCollection.get("options", Document.class)
                .get("validator", Document.class)
                .get("$jsonSchema", Document.class);

        assertThat(validator.getString("bsonType")).isEqualTo("object");
        assertThat(validator.getList("required", String.class)).containsExactlyInAnyOrder(
                "order_id",
                "user_id",
                "status",
                "timestamp",
                "payment_amount"
        );

        Document properties = validator.get("properties", Document.class);
        assertThat(properties.get("_id", Document.class).getString("bsonType")).isEqualTo("objectId");
        assertThat(properties.get("order_id", Document.class).getString("bsonType")).isEqualTo("long");
        assertThat(properties.get("user_id", Document.class).getString("bsonType")).isEqualTo("long");
        assertThat(properties.get("timestamp", Document.class).getString("bsonType")).isEqualTo("date");
        assertThat(properties.get("payment_amount", Document.class).getString("bsonType")).isEqualTo("decimal");
        assertThat(validator.getBoolean("additionalProperties")).isFalse();
    }

    @Test
    void liquibaseValidatorShouldRejectInvalidDocuments() {
        Document missingRequiredField = new Document("order_id", 101L)
                .append("user_id", 202L)
                .append("status", "SUCCESS")
                .append("timestamp", java.util.Date.from(Instant.parse("2026-03-24T00:00:00Z")));

        assertThatThrownBy(() -> mongoTemplate.getCollection("payments").insertOne(missingRequiredField))
                .isInstanceOf(RuntimeException.class);

        Document extraField = new Document("order_id", 101L)
                .append("user_id", 202L)
                .append("status", "SUCCESS")
                .append("timestamp", java.util.Date.from(Instant.parse("2026-03-24T00:00:00Z")))
                .append("payment_amount", new Decimal128(new java.math.BigDecimal("10.50")))
                .append("unexpected_field", "boom");

        assertThatThrownBy(() -> mongoTemplate.getCollection("payments").insertOne(extraField))
                .isInstanceOf(RuntimeException.class);
    }
}
