package com.safety.platform.repository;

import com.safety.platform.domain.SafetyEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SafetyEventLogRepository extends JpaRepository<SafetyEventLog, UUID> {
    
    // Phase 4: Hash Chaining 검증에 사용
    List<SafetyEventLog> findByAggregateIdOrderByOccurredAtAsc(String aggregateId);

    // 최근 이벤트 조회 (Mock API polling 결과 확인용)
    List<SafetyEventLog> findTop10ByOrderByOccurredAtDesc();
}
