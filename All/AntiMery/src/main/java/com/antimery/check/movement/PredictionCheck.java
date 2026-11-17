package com.antimery.check.movement;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prediction Anti-Cheat Engine (V2 - Hypixel Style)
 * 1. แก้ไขปัญหาระบบกระโดดพุ่งไปด้านหน้า (Sprint-Jump Momentum Compensation)
 *    คำนวณแรงเฉื่อยของการกระโดดพุ่งไปข้างหน้า ไม่ให้เกิด False Positive อีกต่อไป
 * 2. ทำนายและดักจับการบินใน GameMode 0 (Survival):
 *    หากผู้เล่นอยู่ในโหมดเอาชีวิตรอด แล้วลอยตัว/บินในอากาศต่อเนื่องเกิน 20 วินาที
 *    โดยไม่มีสิทธิ์บิน ไม่มีคำสั่ง และไม่ได้สัมผัสบล็อก จะสั่งเตะหรือแบนทันที!
 * 3. ส่งข้อมูลการคาดการณ์ (Ban Trajectory Prediction) เข้าสู่ Atlas
 */
public class PredictionCheck extends Check {

    private final Map<UUID, Double> lastHorizontalDeltas = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastMoveTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Long> airborneStartTimestamps = new ConcurrentHashMap<>();

    public PredictionCheck(AntiMery plugin) {
        super(plugin, CheckType.PREDICTION, "prediction");
    }

    public void handleQuit(Player player) {
        UUID uuid = player.getUniqueId();
        lastHorizontalDeltas.remove(uuid);
        lastMoveTimestamps.remove(uuid);
        airborneStartTimestamps.remove(uuid);
    }

    public boolean check(Player player, Location from, Location to) {
        if (shouldIgnorePlayer(player)) {
            resetAirborne(player);
            return false;
        }

        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        // ============================================================== //
        // 1. ระบบทำนายการบิน GameMode 0 (Survival) นานเกิน 20 วินาที        //
        // ============================================================== //
        if (player.getGameMode() == GameMode.SURVIVAL) {
            boolean hasFlyPermission = player.getAllowFlight() || player.isOp() 
                    || player.hasPermission("antimery.bypass.fly")
                    || player.hasPermission("essentials.fly");

            if (!hasFlyPermission) {
                boolean isAirborne = isTrulyInAir(player, to);

                if (isAirborne) {
                    Long start = airborneStartTimestamps.putIfAbsent(uuid, now);
                    long airborneStart = (start != null) ? start : now;
                    double airborneSeconds = (now - airborneStart) / 1000.0;

                    // อัปเดตข้อมูลการทำนายการโดนแบนเข้า Atlas
                    if (plugin.getAtlasManager() != null) {
                        plugin.getAtlasManager().getProfile(player).updateAirborneTime(true, airborneSeconds);
                    }

                    // หากบิน/ลอยตัวใน GameMode 0 เกิน 20 วินาที -> เตะหรือแบนทันที!
                    if (airborneSeconds >= 20.0) {
                        String details = String.format("GameMode 0 บินในอากาศต่อเนื่องเกินกำหนด (Airborne: %.1f วินาที >= 20.0s)", airborneSeconds);
                        flag(player, details);
                        airborneStartTimestamps.remove(uuid);
                        return true;
                    }
                } else {
                    resetAirborne(player);
                }
            } else {
                resetAirborne(player);
            }
        } else {
            resetAirborne(player);
        }

        // ============================================================== //
        // 2. Client Motion Simulation พร้อมชดเชยการกระโดดพุ่งไปข้างหน้า     //
        // ============================================================== //
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double actualDelta = Math.sqrt(dx * dx + dz * dz);

        Double lastDelta = lastHorizontalDeltas.get(uuid);
        lastHorizontalDeltas.put(uuid, actualDelta);
        lastMoveTimestamps.put(uuid, now);

        if (lastDelta == null) {
            return false;
        }

        // ตรวจสอบว่าผู้เล่นกำลังกระโดดพุ่งไปข้างหน้าหรือไม่ (Sprint-Jump Arc)
        boolean isAscending = to.getY() > from.getY();
        boolean isSprinting = player.isSprinting();

        // คำนวณแรงเสียดทาน (Friction)
        double friction = player.isOnGround() ? 0.6 : 0.91;
        if (from.getBlock().getType().name().contains("ICE")) {
            friction = 0.98;
        } else if (from.getBlock().getType().name().contains("SOUL_SAND")) {
            friction = 0.4;
        }

        // ขีดจำกัดแรงเร่ง (Acceleration)
        double maxAcceleration = player.isOnGround() ? 0.13 : 0.026;
        if (isSprinting) {
            maxAcceleration *= 1.35;
        }

        // ชดเชยแรงส่งจากการกระโดดพุ่งไปข้างหน้า (Sprint-Jump Momentum Boost)
        // ใน Minecraft Vanilla ขณะกระโดดและวิ่งพุ่งไปข้างหน้า จะได้รับแรงส่งแนวราบเพิ่มขึ้น ~0.20 - 0.26 block/tick
        if (isAscending || !player.isOnGround()) {
            if (isSprinting) {
                maxAcceleration += 0.28;
            } else {
                maxAcceleration += 0.12;
            }
        }

        // ผลของ Potion Effect
        if (player.hasPotionEffect(PotionEffectType.SPEED)) {
            maxAcceleration *= 1.5;
        }
        if (player.hasPotionEffect(PotionEffectType.JUMP)) {
            maxAcceleration += 0.15;
        }

        double predictedDelta = (lastDelta * friction) + maxAcceleration;
        // ปรับเกณฑ์ความคลาดเคลื่อนให้อนุโลมการกระโดดได้อย่างสมบูรณ์ (0.35 ขณะกระโดด, 0.22 ขณะเดินปกติ)
        double maxOffset = isAscending ? 0.35 : 0.22;

        double error = actualDelta - predictedDelta;

        // บันทึกสถิติลง Atlas
        if (plugin.getAtlasManager() != null) {
            plugin.getAtlasManager().getProfile(player).setLastPredictionOffset(Math.max(0.0, error));
        }

        // หากการเคลื่อนที่เกินค่าทำนายอย่างรุนแรงผิดธรรมชาติ (เช่น พุ่งเกินความเร่งสูงสุดไปมาก)
        if (error > maxOffset && actualDelta > 0.65) {
            String details = String.format("Vector Mismatch: Actual=%.3f, Pred=%.3f (Offset=%.3f > %.3f)",
                    actualDelta, predictedDelta, error, maxOffset);
            flag(player, details);
            return true;
        }

        return false;
    }

    private void resetAirborne(Player player) {
        airborneStartTimestamps.remove(player.getUniqueId());
        if (plugin.getAtlasManager() != null) {
            plugin.getAtlasManager().getProfile(player).updateAirborneTime(false, 0.0);
        }
    }

    /**
     * ตรวจสอบว่าผู้เล่นลอยตัวอยู่ในอากาศจริงหรือไม่ (ไม่ใช่ปีนบันได, ใยแมงมุม, ว่ายน้ำ หรือยืนบนบล็อก)
     */
    private boolean isTrulyInAir(Player player, Location loc) {
        if (player.isOnGround()) return false;

        Location l = loc.clone();
        Block feet = l.getBlock();
        Block below = l.subtract(0, 0.5, 0).getBlock();

        // สัมผัสน้ำ ลาวา บันได หรือใยแมงมุม
        if (feet.isLiquid() || below.isLiquid()) return false;
        if (feet.getType() == Material.LADDER || feet.getType() == Material.VINE || feet.getType() == Material.SCAFFOLDING) return false;
        if (below.getType() == Material.LADDER || below.getType() == Material.VINE || below.getType() == Material.SCAFFOLDING) return false;
        if (feet.getType() == Material.COBWEB || below.getType() == Material.COBWEB) return false;

        // บล็อกด้านล่างเป็นของแข็งรองรับ
        if (below.getType().isSolid()) return false;

        return true;
    }
}
