package com.safety.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safety.platform.config.StorageTierProperties;
import com.safety.platform.domain.EventArchiveManifest;
import com.safety.platform.domain.SafetyEventLog;
import com.safety.platform.domain.StorageTier;
import com.safety.platform.dto.StorageArchiveRunResponse;
import com.safety.platform.dto.StorageTierSummaryResponse;
import com.safety.platform.repository.EventArchiveManifestRepository;
import com.safety.platform.repository.SafetyEventLogRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageTierService {

    private final SafetyEventLogRepository eventRepository;
    private final EventArchiveManifestRepository archiveManifestRepository;
    private final StorageTierProperties storageTierProperties;
    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;

    @Value("${minio.bucket:safety-bucket}")
    private String bucketName;

    private final AtomicBoolean bucketPrepared = new AtomicBoolean(false);

    @Transactional(readOnly = true)
    public StorageTierSummaryResponse getSummary() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime hotBoundary = now.minusHours(storageTierProperties.getHotHours());
        LocalDateTime coldBoundary = now.minusDays(storageTierProperties.getWarmDays());

        long hotCount = eventRepository.countByOccurredAtGreaterThanEqual(hotBoundary);
        long warmCount = eventRepository.countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(coldBoundary, hotBoundary);
        long coldCount = eventRepository.countByOccurredAtLessThan(coldBoundary);
        long archivedColdCount = archiveManifestRepository.countByTier(StorageTier.COLD);

        return StorageTierSummaryResponse.builder()
            .hotCount(hotCount)
            .warmCount(warmCount)
            .coldCount(coldCount)
            .archivedColdCount(archivedColdCount)
            .hotBoundary(hotBoundary)
            .coldBoundary(coldBoundary)
            .build();
    }

    @Transactional
    public StorageArchiveRunResponse archiveColdEvents() {
        LocalDateTime now = LocalDateTime.now();
        if (!storageTierProperties.isEnabled()) {
            return StorageArchiveRunResponse.builder()
                .success(false)
                .archivedCount(0)
                .message("Storage tiering is disabled.")
                .executedAt(now)
                .build();
        }
        if (!storageTierProperties.isArchiveEnabled()) {
            return StorageArchiveRunResponse.builder()
                .success(false)
                .archivedCount(0)
                .message("Archive task is disabled.")
                .executedAt(now)
                .build();
        }

        LocalDateTime coldBoundary = now.minusDays(storageTierProperties.getWarmDays());
        List<SafetyEventLog> candidates = eventRepository.findColdEventsWithoutArchive(
            coldBoundary,
            PageRequest.of(0, Math.max(1, storageTierProperties.getBatchSize()))
        );
        if (candidates.isEmpty()) {
            return StorageArchiveRunResponse.builder()
                .success(true)
                .archivedCount(0)
                .message("No unarchived cold events found.")
                .executedAt(now)
                .build();
        }

        String objectKey = buildObjectKey(now, candidates.size());
        try {
            ensureBucket();
            byte[] archiveBody = buildArchiveBody(candidates);
            try (ByteArrayInputStream stream = new ByteArrayInputStream(archiveBody)) {
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .contentType("application/x-ndjson")
                        .stream(stream, archiveBody.length, -1)
                        .build()
                );
            }

            List<EventArchiveManifest> manifests = candidates.stream()
                .map(event -> {
                    EventArchiveManifest manifest = new EventArchiveManifest();
                    manifest.setEventId(event.getEventId());
                    manifest.setTier(StorageTier.COLD);
                    manifest.setObjectKey(objectKey);
                    return manifest;
                })
                .toList();
            archiveManifestRepository.saveAll(manifests);

            return StorageArchiveRunResponse.builder()
                .success(true)
                .archivedCount(candidates.size())
                .objectKey(objectKey)
                .message("Archived cold events to object storage.")
                .executedAt(now)
                .build();
        } catch (Exception e) {
            log.error("Cold event archival failed", e);
            return StorageArchiveRunResponse.builder()
                .success(false)
                .archivedCount(0)
                .objectKey(objectKey)
                .message("Cold event archival failed: " + e.getMessage())
                .executedAt(now)
                .build();
        }
    }

    private byte[] buildArchiveBody(List<SafetyEventLog> events) {
        StringBuilder builder = new StringBuilder();
        for (SafetyEventLog event : events) {
            Map<String, Object> line = new LinkedHashMap<>();
            line.put("eventId", event.getEventId());
            line.put("aggregateId", event.getAggregateId());
            line.put("eventType", event.getEventType());
            line.put("occurredAt", event.getOccurredAt());
            line.put("sourceSystem", event.getSourceSystem());
            line.put("previousHash", event.getPreviousHash());
            line.put("currentHash", event.getCurrentHash());
            line.put("payload", event.getPayload());
            try {
                builder.append(objectMapper.writeValueAsString(line)).append('\n');
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize archive line", e);
            }
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String buildObjectKey(LocalDateTime now, int count) {
        DateTimeFormatter prefixFormat = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        DateTimeFormatter nameFormat = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
        return "cold/" + now.format(prefixFormat) + "/events-" + now.format(nameFormat) + "-" + count + ".jsonl";
    }

    private synchronized void ensureBucket() throws Exception {
        if (bucketPrepared.get()) {
            return;
        }
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
        bucketPrepared.set(true);
    }
}

