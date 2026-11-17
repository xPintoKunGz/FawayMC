package com.mery.npc.model;

public enum CommandType {
    CONSOLE,
    PLAYER,
    OP,
    SERVER,
    MESSAGE,
    ACTIONBAR,
    TITLE;

    public static CommandType fromString(String name) {
        try {
            return CommandType.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CONSOLE;
        }
    }
}

