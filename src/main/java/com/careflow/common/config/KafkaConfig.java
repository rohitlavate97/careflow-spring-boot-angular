package com.careflow.common.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Enterprise Apache Kafka configuration for CareFlow domain event streaming (§51, §53, §76).
 * Defines partitioned topics, consumer groups, dead-letter routing (DLT), and bounded retries.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaConfig.class);

    public static final String TOPIC_APPOINTMENTS = "careflow.appointments";
    public static final String TOPIC_BILLING = "careflow.billing";
    public static final String TOPIC_LABORATORY = "careflow.laboratory";
    public static final String TOPIC_DLT = "careflow.events.dlt";

    public static final String GROUP_NOTIFICATIONS = "careflow-notification-group";
    public static final String GROUP_BILLING = "careflow-billing-group";

    @Bean
    @ConditionalOnProperty(name = "careflow.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic appointmentsTopic() {
        return TopicBuilder.name(TOPIC_APPOINTMENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "careflow.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic billingTopic() {
        return TopicBuilder.name(TOPIC_BILLING)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "careflow.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic laboratoryTopic() {
        return TopicBuilder.name(TOPIC_LABORATORY)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "careflow.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public NewTopic dltTopic() {
        return TopicBuilder.name(TOPIC_DLT)
                .partitions(3)
                .replicas(1)
                .build();
    }

    /**
     * Resilient error handler for Kafka consumers (§51, §76 Scenario 8):
     * Retries failed consumer executions up to 2 times with a 1-second fixed backoff.
     * If retries are exhausted (e.g. poison message), routes the message to the Dead Letter Topic (DLT).
     */
    @Bean
    @ConditionalOnProperty(name = "careflow.kafka.enabled", havingValue = "true", matchIfMissing = true)
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> {
                    log.error("Kafka consumer exhausted retries for topic='{}', partition={}, offset={}. Routing to DLT='{}'",
                            record.topic(), record.partition(), record.offset(), TOPIC_DLT, exception);
                    return new TopicPartition(TOPIC_DLT, record.partition());
                }
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));
        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }
}
