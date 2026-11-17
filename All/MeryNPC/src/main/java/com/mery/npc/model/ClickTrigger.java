package com.mery.npc.model;

public enum ClickTrigger {
    ANY,
    RIGHT_CLICK,
    LEFT_CLICK,
    SHIFT_RIGHT_CLICK,
    SHIFT_LEFT_CLICK;

    public static ClickTrigger fromString(String name) {
        try {
            return ClickTrigger.valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ANY;
        }
    }
}

