package com.safety.platform.service;

import com.safety.platform.dto.KafkaLagResponse;
import com.safety.platform.dto.KafkaTopicLagResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class KafkaLagService {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    public KafkaLagResponse getLag(String consumerGroup) {
        Map<String, TopicLagAccumulator> byTopic = new HashMap<>();
        try (AdminClient adminClient = createAdminClient()) {
            Map<TopicPartition, OffsetAndMetadata> committed = adminClient
                .listConsumerGroupOffsets(consumerGroup)
                .partitionsToOffsetAndMetadata()
                .get(5, TimeUnit.SECONDS);

            if (committed == null || committed.isEmpty()) {
                return KafkaLagResponse.builder()
                    .consumerGroup(consumerGroup)
                    .totalLag(0)
                    .checkedAt(LocalDateTime.now())
                    .topics(List.of())
                    .build();
            }

            Map<TopicPartition, OffsetSpec> latestRequest = new HashMap<>();
            for (TopicPartition partition : committed.keySet()) {
                latestRequest.put(partition, OffsetSpec.latest());
            }

            Map<TopicPartition, ListOffsetsResult.ListOffsetsResultInfo> latest = adminClient
                .listOffsets(latestRequest)
                .all()
                .get(5, TimeUnit.SECONDS);

            for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : committed.entrySet()) {
                TopicPartition partition = entry.getKey();
                long committedOffset = entry.getValue().offset();
                long endOffset = latest.containsKey(partition) ? latest.get(partition).offset() : committedOffset;
                long lag = Math.max(0L, endOffset - committedOffset);

                TopicLagAccumulator acc = byTopic.computeIfAbsent(partition.topic(), k -> new TopicLagAccumulator());
                acc.partitionCount++;
                acc.committedOffset += committedOffset;
                acc.endOffset += endOffset;
                acc.lag += lag;
            }

            List<KafkaTopicLagResponse> topicLag = new ArrayList<>();
            long totalLag = 0L;
            for (Map.Entry<String, TopicLagAccumulator> entry : byTopic.entrySet()) {
                TopicLagAccumulator acc = entry.getValue();
                totalLag += acc.lag;
                topicLag.add(
                    KafkaTopicLagResponse.builder()
                        .topic(entry.getKey())
                        .partitionCount(acc.partitionCount)
                        .committedOffset(acc.committedOffset)
                        .endOffset(acc.endOffset)
                        .lag(acc.lag)
                        .build()
                );
            }
            topicLag.sort((a, b) -> Long.compare(b.getLag(), a.getLag()));

            return KafkaLagResponse.builder()
                .consumerGroup(consumerGroup)
                .totalLag(totalLag)
                .checkedAt(LocalDateTime.now())
                .topics(topicLag)
                .build();
        } catch (Exception e) {
            log.error("Failed to fetch kafka lag for group={}", consumerGroup, e);
            throw new IllegalStateException("Failed to fetch kafka lag: " + e.getMessage(), e);
        }
    }

    private AdminClient createAdminClient() {
        Map<String, Object> props = new HashMap<>();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");
        return AdminClient.create(props);
    }

    private static class TopicLagAccumulator {
        private int partitionCount;
        private long committedOffset;
        private long endOffset;
        private long lag;
    }
}

