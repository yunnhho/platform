package com.safety.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "safety_event_log")
@Getter
@Setter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SafetyEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID eventId;

    @Column(nullable = false, length = 100)
    private String aggregateId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private EventType eventType;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime occurredAt;

    @Column(columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(nullable = false, length = 50)
    private String sourceSystem;

    @Version
    private Long version;

    // ⭐ Phase 4: Cryptographic Protection (Placeholder)
    @Column(length = 64)
    private String previousHash;

    @Column(length = 64)
    private String currentHash;

    // 🔒 Level 1: Application Level Protection
    @PreUpdate
    protected void preventUpdate() {
        throw new IllegalStateException(
            "Event log cannot be modified. EventId: " + eventId
        );
    }

    // ⭐ Phase 4: Hash Chaining (PrePersist hook)
    // @PrePersist
    // protected void calculateHash() { ... }
}
