package com.safety.platform.scheduler;

import com.safety.platform.dto.StorageArchiveRunResponse;
import com.safety.platform.service.StorageTierService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "app.storage-tier.archive-enabled", havingValue = "true", matchIfMissing = true)
public class StorageTierScheduler {

    private final StorageTierService storageTierService;

    @Scheduled(cron = "${app.storage-tier.archive-cron:0 */10 * * * *}")
    public void archiveColdTier() {
        StorageArchiveRunResponse result = storageTierService.archiveColdEvents();
        if (result.getArchivedCount() > 0 || !result.isSuccess()) {
            log.info(
                "Storage tier archive run. success={}, archivedCount={}, message={}",
                result.isSuccess(),
                result.getArchivedCount(),
                result.getMessage()
            );
        }
    }
}

