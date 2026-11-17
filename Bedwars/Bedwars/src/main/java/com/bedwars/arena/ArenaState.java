package com.bedwars.arena;

public enum ArenaState {
    WAITING("§aWaiting", "§aWaiting..."),
    STARTING("§eStarting", "§eCountdown"),
    IN_GAME("§cIn Game", "§cPlaying"),
    ENDING("§6Game Over", "§7Resetting");

    private final String displayStatus;
    private final String shortStatus;

    ArenaState(String displayStatus, String shortStatus) {
        this.displayStatus = displayStatus;
        this.shortStatus = shortStatus;
    }

    public String getDisplayStatus() {
        return displayStatus;
    }

    public String getShortStatus() {
        return shortStatus;
    }
}

