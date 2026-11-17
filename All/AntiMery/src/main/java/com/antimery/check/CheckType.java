package com.antimery.check;

public enum CheckType {
    // 1. Watchdog Checks (Combat & Statistical Heuristics)
    KILLAURA("KillAura (ตีรอบทิศ/ออโต้ล็อค)"),
    REACH("Reach (ตีไกล/ระยะเกิน)"),
    AUTOCLICKER("AutoClicker (คลิกไวเกินกำหนด)"),
    CRITICALS("Criticals (คริติคอลปลอม)"),

    // 2. NoCheatPlus (NCP Physics & World Engine)
    FLY("Fly (บิน/ลอยตัว)"),
    SPEED("Speed (วิ่งไว/เคลื่อนที่ผิดปกติ)"),
    NOFALL("NoFall (ตกไม่เจ็บ)"),
    JESUS("Jesus (เดินบนน้ำ)"),
    HIGHJUMP("HighJump (กระโดดสูง/ก้าวสูง)"),
    FASTCLIMB("FastClimb (ปีนไว)"),
    FASTBREAK("FastBreak (ขุดบล็อกไว/Nuker)"),
    FASTPLACE("FastPlace (วางบล็อกรัว)"),
    FASTUSE("FastUse (กินไอเทม/ยิงธนูไว)"),

    // 3. Prediction Anti-Cheat Engine (Motion Simulation & Vector Mismatch)
    PREDICTION("Prediction (ทำนายเวกเตอร์/จับการเคลื่อนไหวผิดธรรมชาติ)"),

    // 4. Atlas Assessment System
    ATLAS("Atlas (ดัชนีความน่าสงสัยรวม/ความมั่นใจ)"),

    // Chat Filter
    CHAT_FILTER("ChatFilter (คำหยาบ TH/EN)");

    private final String displayName;

    CheckType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
