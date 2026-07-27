package group1.com.MangaSystemAndManagement.model;

import java.util.Locale;
import java.util.Optional;

public enum SystemRoleName {
    ADMIN,
    MANAGER,
    TANTOU_EDITOR,
    EDITORIAL_BOARD_MEMBER,
    LEADER_BOARD,
    MANGAKA,
    ASSISTANT;

    public static SystemRoleName from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("System role is required");
        }

        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TANTOR" -> TANTOU_EDITOR;
            case "EDITOR" -> EDITORIAL_BOARD_MEMBER;
            case "LEADER" -> LEADER_BOARD;
            default -> {
                try {
                    yield valueOf(normalized);
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("Unknown system role: " + value, exception);
                }
            }
        };
    }

    public boolean matches(String value) {
        return tryFrom(value).filter(role -> role == this).isPresent();
    }

    public static Optional<SystemRoleName> tryFrom(String value) {
        try {
            return Optional.of(from(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
