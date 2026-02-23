package com.safety.platform.repository;

import com.safety.platform.domain.EventAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EventAuditLogRepository extends JpaRepository<EventAuditLog, Long> {
    
    // 명세서: Page<ModificationAttempt> getModificationAttempts(PageRequest)
    // attempted_at 기준으로 정렬
    Page<EventAuditLog> findAllByOrderByAttemptedAtDesc(Pageable pageable);

    // 날짜별 조회
    Page<EventAuditLog> findByAttemptedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    long countByOperation(String operation);

    Optional<EventAuditLog> findTopByOrderByAttemptedAtDesc();
}
