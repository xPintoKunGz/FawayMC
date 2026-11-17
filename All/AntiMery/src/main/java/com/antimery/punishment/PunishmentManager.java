package com.antimery.punishment;

import com.antimery.AntiMery;
import com.antimery.check.CheckType;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PunishmentManager
 * ทำงานในโหมดเงียบ (Silent Operation) ตามที่ต้องการ:
 * - ไม่สแปมข้อความ Log หรือ Alert ขึ้นแชทของแอดมิน
 * - ไม่ส่งข้อความเตือนไปยังตัวคนทำผิดให้รู้ตัวล่วงหน้า
 * - ตรวจจับและเก็บแต้มความผิดอย่างเงียบๆ
 * - เมื่อแต้มความผิดเยอะแล้ว (ถึงขีดจำกัด) จะเตะหรือแบนทันที!
 */
public class PunishmentManager {

    private final AntiMery plugin;
    // Map<PlayerUUID, Map<CheckType, Integer>>
    private final Map<UUID, Map<CheckType, Integer>> violationLevels = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastViolationTime = new ConcurrentHashMap<>();

    public PunishmentManager(AntiMery plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    private void startCleanupTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            long resetMillis = plugin.getConfigManager().getViolationResetSeconds() * 1000L;
            for (Map.Entry<UUID, Long> entry : lastViolationTime.entrySet()) {
                if (now - entry.getValue() > resetMillis) {
                    violationLevels.remove(entry.getKey());
                    lastViolationTime.remove(entry.getKey());
                }
            }
        }, 20L * 30, 20L * 30); // check every 30 seconds
    }

    public int addViolation(Player player, CheckType checkType) {
        UUID uuid = player.getUniqueId();
        lastViolationTime.put(uuid, System.currentTimeMillis());
        Map<CheckType, Integer> map = violationLevels.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        return map.merge(checkType, 1, Integer::sum);
    }

    public int getViolation(Player player, CheckType checkType) {
        Map<CheckType, Integer> map = violationLevels.get(player.getUniqueId());
        if (map == null) return 0;
        return map.getOrDefault(checkType, 0);
    }

    public void resetViolations(UUID uuid) {
        violationLevels.remove(uuid);
        lastViolationTime.remove(uuid);
    }

    /**
     * Silent Alert: ไม่ส่งข้อความสแปมเข้าแชทแอดมินหรือผู้เล่นตามที่ผู้ใช้สั่ง
     * บันทึกเฉพาะ Console Log เมื่อเปิด debug เท่านั้น
     */
    public void alertStaff(Player player, CheckType checkType, int vl, int maxVl, String details) {
        boolean consoleDebug = plugin.getConfigManager().getConfig().getBoolean("general.console-debug", false);
        if (consoleDebug) {
            plugin.getLogger().info("[Silent Check] " + player.getName() + " flagged " + checkType.name()
                    + " (VL: " + vl + "/" + maxVl + "): " + (details != null ? details : ""));
        }
    }

    public void executePunishment(Player player, CheckType checkType, PunishmentType punishmentType, String reason) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) return;

            String prefix = plugin.getConfigManager().getPrefix();
            String kickMsg = plugin.getConfigManager().getKickMessage()
                    .replace("%prefix%", prefix)
                    .replace("%reason%", reason)
                    .replace("%check%", checkType.getDisplayName())
                    .replace("%player%", player.getName());

            // เมื่อเยอะแล้ว ดำเนินการลงโทษทันที (เตะหรือแบน)
            switch (punishmentType) {
                case KICK:
                case WARN: // เมื่อเลือก Silent และคะแนนเยอะแล้ว ให้เตะทันทีแทนการเตือน
                default:
                    resetViolations(player.getUniqueId());
                    player.kickPlayer(kickMsg);
                    plugin.getLogger().warning("[AntiMery] ทำการเตะ " + player.getName() + " เนื่องจากแต้มความผิดสะสมครบกำหนด: " + checkType.name() + " (" + reason + ")");
                    break;

                case BAN:
                    resetViolations(player.getUniqueId());
                    String banReason = plugin.getConfigManager().getBanCommand()
                            .replace("%player%", player.getName())
                            .replace("%check%", checkType.getDisplayName())
                            .replace("%reason%", reason);
                    Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), banReason, null, "AntiMery");
                    player.kickPlayer(banReason);
                    plugin.getLogger().warning("[AntiMery] ทำการแบน " + player.getName() + " เนื่องจากตรวจพบการโกง: " + checkType.name());
                    break;

                case COMMAND:
                    resetViolations(player.getUniqueId());
                    String cmd = plugin.getConfigManager().getCustomCommand()
                            .replace("%player%", player.getName())
                            .replace("%check%", checkType.getDisplayName())
                            .replace("%reason%", reason);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    plugin.getLogger().info("[AntiMery] รันคำสั่งลงโทษสำหรับ " + player.getName() + ": " + cmd);
                    break;

                case NOTIFY_ONLY:
                    break;
            }
        });
    }
}
