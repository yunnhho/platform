package com.safety.platform.controller;

import com.safety.platform.dto.StorageArchiveRunResponse;
import com.safety.platform.dto.StorageTierSummaryResponse;
import com.safety.platform.service.StorageTierService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/storage/tiers")
@RequiredArgsConstructor
public class StorageTierController {

    private final StorageTierService storageTierService;

    @GetMapping("/summary")
    public StorageTierSummaryResponse getSummary() {
        return storageTierService.getSummary();
    }

    @PostMapping("/archive/run")
    public StorageArchiveRunResponse runArchive() {
        return storageTierService.archiveColdEvents();
    }
}

