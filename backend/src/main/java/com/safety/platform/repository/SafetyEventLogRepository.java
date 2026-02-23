package com.safety.platform.repository;

import com.safety.platform.domain.SafetyEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SafetyEventLogRepository extends JpaRepository<SafetyEventLog, UUID> {
    
    // Phase 4: Hash Chaining 검증에 사용
    List<SafetyEventLog> findByAggregateIdOrderByOccurredAtAsc(String aggregateId);

    // 최근 이벤트 조회 (Mock API polling 결과 확인용)
    List<SafetyEventLog> findTop10ByOrderByOccurredAtDesc();

    Optional<SafetyEventLog> findTopByAggregateIdOrderByOccurredAtDesc(String aggregateId);

    @Query("select distinct e.aggregateId from SafetyEventLog e")
    List<String> findDistinctAggregateIds();

    long countByOccurredAtGreaterThanEqual(LocalDateTime hotBoundary);

    long countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(LocalDateTime warmBoundary, LocalDateTime hotBoundary);

    long countByOccurredAtLessThan(LocalDateTime coldBoundary);

    @Query(
        value = """
            SELECT e.*
            FROM safety_event_log e
            WHERE e.occurred_at < :coldBoundary
              AND NOT EXISTS (
                SELECT 1
                FROM event_archive_manifest m
                WHERE m.event_id = e.event_id
              )
            ORDER BY e.occurred_at ASC
            """,
        nativeQuery = true
    )
    List<SafetyEventLog> findColdEventsWithoutArchive(@Param("coldBoundary") LocalDateTime coldBoundary, Pageable pageable);
}
