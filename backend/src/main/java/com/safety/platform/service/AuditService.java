package com.safety.platform.service;

import com.safety.platform.domain.EventAuditLog;
import com.safety.platform.repository.EventAuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditService {

    private final EventAuditLogRepository auditLogRepository;

    public Page<EventAuditLog> getModificationAttempts(Pageable pageable) {
        return auditLogRepository.findAllByOrderByAttemptedAtDesc(pageable);
    }
}
