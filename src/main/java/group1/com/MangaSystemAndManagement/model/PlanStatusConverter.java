package group1.com.MangaSystemAndManagement.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts {@link PlanStatus} to/from its DB string representation.
 *
 * <p>Decision Log 2026-07-27 §AI-10 follow-up: protects against legacy DB rows
 * that somehow stored 'PLANNING' (not a valid enum constant). Any unrecognized
 * value is treated as {@link PlanStatus#IN_PROGRESS}.
 *
 * <p>To permanently fix: run migration
 * {@code V2026_07_27__fix_legacy_plan_status_planning.sql}.
 */
@Converter(autoApply = true)
public class PlanStatusConverter implements AttributeConverter<PlanStatus, String> {

    @Override
    public String convertToDatabaseColumn(PlanStatus status) {
        if (status == null) {
            return null;
        }
        return status.name();
    }

    @Override
    public PlanStatus convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return PlanStatus.IN_PROGRESS;
        }
        for (PlanStatus status : PlanStatus.values()) {
            if (status.name().equals(dbData)) {
                return status;
            }
        }
        // Legacy/unknown value — treat as IN_PROGRESS rather than throwing.
        return PlanStatus.IN_PROGRESS;
    }
}
