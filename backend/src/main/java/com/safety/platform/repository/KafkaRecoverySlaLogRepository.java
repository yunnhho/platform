package com.safety.platform.repository;

import com.safety.platform.domain.KafkaRecoverySlaLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KafkaRecoverySlaLogRepository extends JpaRepository<KafkaRecoverySlaLog, Long> {

    long countByCompliant(boolean compliant);

    Optional<KafkaRecoverySlaLog> findTopByOrderByCheckedAtDesc();

    List<KafkaRecoverySlaLog> findTop20ByOrderByCheckedAtDesc();
}

