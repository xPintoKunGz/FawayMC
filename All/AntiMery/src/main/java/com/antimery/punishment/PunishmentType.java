package com.antimery.punishment;

public enum PunishmentType {
    DEFAULT,
    WARN,
    KICK,
    BAN,
    COMMAND,
    NOTIFY_ONLY;

    public static PunishmentType fromString(String name) {
        if (name == null) return DEFAULT;
        try {
            return PunishmentType.valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return DEFAULT;
        }
    }
}

