package com.antimery.check.combat;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Watchdog KillAura & Aimbot Engine
 * 1. ตรวจสอบมุมองศาการมองเห็น (FOV Check)
 * 2. ตรวจสอบการสะบัดมุมกล้องล็อกเป้าใน 1 tick (Snap Detection)
 * 3. ตรวจสอบมุมก้มเงยผิดธรรมชาติ (Pitch Clamp: |pitch| > 90)
 * 4. ตรวจสอบการโจมตีเป้าหมายหลายตัวพร้อมกัน (Multi-Aura)
 */
public class KillAuraCheck extends Check {

    private final Map<UUID, Long> lastAttackTime = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> lastTargetId = new ConcurrentHashMap<>();
    private final Map<UUID, Float> lastYaws = new ConcurrentHashMap<>();
    private final Map<UUID, Float> lastPitches = new ConcurrentHashMap<>();

    public KillAuraCheck(AntiMery plugin) {
        super(plugin, CheckType.KILLAURA, "killaura");
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        lastAttackTime.remove(uuid);
        lastTargetId.remove(uuid);
        lastYaws.remove(uuid);
        lastPitches.remove(uuid);
    }

    public boolean check(Player player, Entity target) {
        if (shouldIgnorePlayer(player)) {
            return false;
        }

        UUID uuid = player.getUniqueId();
        Location eyeLoc = player.getEyeLocation();
        float currentYaw = eyeLoc.getYaw();
        float currentPitch = eyeLoc.getPitch();

        // 1. Pitch Clamp Check: Minecraft แท้ๆ Pitch จะอยู่ระหว่าง -90 ถึง +90 องศาเท่านั้น
        if (Math.abs(currentPitch) > 90.0f) {
            flag(player, String.format("Watchdog: มุมก้มเงยผิดธรรมชาติ (Pitch: %.2f)", currentPitch));
            return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-attack", true);
        }

        // 2. FOV Angle Check: ตรวจสอบมุมว่าผู้เล่นหันหน้าหาเป้าหมายจริงหรือไม่
        Vector eyeDir = eyeLoc.getDirection().normalize();
        Vector toTarget = target.getLocation().add(0, target.getHeight() / 2.0, 0)
                .subtract(eyeLoc).toVector().normalize();

        double angle = Math.toDegrees(eyeDir.angle(toTarget));
        double maxAngle = plugin.getConfigManager().getCheckDouble(configPath, "max-angle", 90.0);

        if (angle > maxAngle) {
            String details = String.format("โจมตีนอกระยะมุมมอง (Angle: %.1f > %.1f องศา)", angle, maxAngle);
            flag(player, details);
            return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-attack", true);
        }

        // 3. Snap Detection (การสะบัดเมาส์ล็อกเป้าทันทีใน 1 tick)
        Float prevYaw = lastYaws.get(uuid);
        Float prevPitch = lastPitches.get(uuid);
        if (prevYaw != null && prevPitch != null) {
            float deltaYaw = Math.abs(currentYaw - prevYaw);
            float deltaPitch = Math.abs(currentPitch - prevPitch);

            // หากสะบัด Yaw > 50 องศาใน 1 tick แล้วล็อกเข้าเป้าในมุมแคบมาก (< 5 องศา)
            if (deltaYaw > 50.0f && angle < 5.0) {
                flag(player, String.format("Watchdog Snap Aimbot: สะบัดมุมกล้องล็อกเป้าฉับพลัน (DeltaYaw: %.1f, Angle: %.1f)", deltaYaw, angle));
                lastYaws.put(uuid, currentYaw);
                lastPitches.put(uuid, currentPitch);
                return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-attack", true);
            }
        }
        lastYaws.put(uuid, currentYaw);
        lastPitches.put(uuid, currentPitch);

        // 4. Multi-Aura Check: โจมตีเป้าหมายคนละตัวในเวลาไม่กี่มิลลิวินาที
        Long lastTime = lastAttackTime.get(uuid);
        UUID lastTarget = lastTargetId.get(uuid);
        long now = System.currentTimeMillis();

        if (lastTime != null && lastTarget != null && !lastTarget.equals(target.getUniqueId())) {
            if (now - lastTime < 25) {
                flag(player, "Watchdog MultiAura: โจมตีเป้าหมายหลายตัวพร้อมกันในเสี้ยววินาที");
                lastAttackTime.put(uuid, now);
                lastTargetId.put(uuid, target.getUniqueId());
                return plugin.getConfigManager().getCheckBoolean(configPath, "cancel-attack", true);
            }
        }

        lastAttackTime.put(uuid, now);
        lastTargetId.put(uuid, target.getUniqueId());
        return false;
    }
}
