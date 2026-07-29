package group1.com.MangaSystemAndManagement.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Technical Spec v2.1 §2.2 — Audit trail for every Plan end-date extension.
 * Decision Log 2026-07-28: introduced alongside the v2.1 refactor; not present
 * in BA V3 era.
 */
@Getter
@Setter
@Entity
@Table(name = "plan_extension_log")
public class PlanExtensionLog {

    public enum ReasonCode {
        CLIENT_CHANGE,
        RESOURCE_SHORTAGE,
        TECHNICAL_ISSUE,
        OTHER,
        DELAY_DEADLINE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private ProductionPlan productionPlan;

    @Column(name = "old_end_date", nullable = false)
    private LocalDate oldEndDate;

    @Column(name = "new_end_date", nullable = false)
    private LocalDate newEndDate;

    @Column(name = "reason_code", length = 50, nullable = false)
    private String reasonCode;

    @Column(name = "reason_note", columnDefinition = "nvarchar(max)")
    private String reasonNote;

    @Column(name = "extended_by", nullable = false)
    private Long extendedBy;

    @Column(name = "extended_at", nullable = false)
    private Instant extendedAt = Instant.now();
}