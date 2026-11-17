package com.antimery.client;

public enum ClientType {
    PVP_CLIENT("PvP Client (ไคลเอนต์สำหรับ PvP)", true, false),
    VANILLA("Vanilla (มายคราฟดั้งเดิม/ธรรมดา)", false, false),
    MOD_LOADER("Mod Loader (ตัวลงม็อดทั่วไป เช่น Forge/Fabric)", false, false),
    HACK_CLIENT("Hack Client (โปรแกรมช่วยเล่น/แฮคไคลเอนต์)", false, true),
    UNKNOWN("Unknown (ไม่ทราบประเภท)", false, false);

    private final String displayName;
    private final boolean pvpClient;
    private final boolean hackClient;

    ClientType(String displayName, boolean pvpClient, boolean hackClient) {
        this.displayName = displayName;
        this.pvpClient = pvpClient;
        this.hackClient = hackClient;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isPvpClient() {
        return pvpClient;
    }

    public boolean isHackClient() {
        return hackClient;
    }
}

