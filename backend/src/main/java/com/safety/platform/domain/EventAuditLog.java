package com.safety.platform.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Immutable;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_audit_log")
@Getter
@Setter
@NoArgsConstructor
@Immutable // JPA Level: Read-Only
public class EventAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long auditId;

    @Column(nullable = false, length = 20)
    private String operation;

    @Column(nullable = false)
    private UUID eventId;

    @Column(length = 100)
    private String attemptedBy;

    @Column(nullable = false)
    private LocalDateTime attemptedAt;

    @Column(columnDefinition = "text")
    private String deniedReason;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String originalData;

    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String attemptedData;

    @Column(columnDefinition = "inet") // PostgreSQL inet type
    private String clientIp;

    @Column(length = 200)
    private String clientApplication;
}
