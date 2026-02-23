package com.safety.platform.repository;

import com.safety.platform.domain.EventArchiveManifest;
import com.safety.platform.domain.StorageTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface EventArchiveManifestRepository extends JpaRepository<EventArchiveManifest, UUID> {

    long countByTier(StorageTier tier);
}

