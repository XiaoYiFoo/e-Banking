package com.ebanking.service;

import com.ebanking.domain.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaTransactionConsumer {

    private final TransactionService transactionService;

    @KafkaListener(
            topics = "${app.kafka.topic.transactions}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeTransaction(
            @Payload Transaction transaction,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            log.debug("Received transaction from Kafka - Topic: {}, Partition: {}, Offset: {}, Transaction ID: {}",
                    topic, partition, offset, transaction.getId());
            // Save to DB
            transactionService.processTransaction(transaction);
            acknowledgment.acknowledge();
            log.debug("Successfully processed transaction: {}", transaction.getId());
        } catch (Exception e) {
            log.error("Error processing transaction from Kafka - Transaction ID: {}, Error: {}",
                    transaction.getId(), e.getMessage(), e);
            acknowledgment.acknowledge();
        }
    }
}