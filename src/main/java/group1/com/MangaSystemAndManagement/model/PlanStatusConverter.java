package group1.com.MangaSystemAndManagement.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Maps {@link PlanStatus} ↔ DB string. Legacy rows holding {@code IN_PROGRESS},
 * {@code PAUSED}, {@code CANCELLED} are migrated to the closest v2.1 state by
 * the SQL migration {@code V2026_07_28__migrate_plan_status_to_v21.sql}.
 */
@Converter(autoApply = true)
public class PlanStatusConverter implements AttributeConverter<PlanStatus, String> {

    @Override
    public String convertToDatabaseColumn(PlanStatus status) {
        return status == null ? null : status.name();
    }

    @Override
    public PlanStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return PlanStatus.DRAFT;
        }
        // Legacy values mapping (BA V3 era)
        return switch (dbData.toUpperCase()) {
            case "PAUSED" -> PlanStatus.EXTENDED;
            case "CANCELLED" -> PlanStatus.COMPLETED;
            case "IN_PROGRESS" -> PlanStatus.ACTIVE;
            default -> {
                try {
                    yield PlanStatus.valueOf(dbData.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    yield PlanStatus.DRAFT;
                }
            }
        };
    }
}