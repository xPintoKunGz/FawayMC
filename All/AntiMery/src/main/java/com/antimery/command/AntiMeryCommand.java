package com.antimery.command;

import com.antimery.AntiMery;
import com.antimery.check.CheckType;
import com.antimery.punishment.PunishmentType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AntiMeryCommand implements CommandExecutor, TabCompleter {

    private final AntiMery plugin;
    private final List<String> subCommands = Arrays.asList("help", "reload", "stats", "check", "client", "info", "predict", "reset", "warn", "kick", "ban");

    public AntiMeryCommand(AntiMery plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("antimery.admin") && !sender.isOp()) {
            sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.RED + "คุณไม่มีสิทธิ์ใช้งานคำสั่งนี้! (ต้องการ antimery.admin)");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "reload":
                plugin.getConfigManager().loadConfig();
                plugin.getCheckManager().reload();
                sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.GREEN + "รีโหลดการตั้งค่า Config และระบบตรวจจับสำเร็จเรียบร้อย!");
                break;

            case "stats":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.YELLOW + "วิธีใช้: /" + label + " stats <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.RED + "ไม่พบผู้เล่น " + args[1]);
                    return true;
                }
                sender.sendMessage(ChatColor.DARK_GRAY + "==== " + ChatColor.RED + "AntiMery Stats: " + ChatColor.WHITE + target.getName() + ChatColor.DARK_GRAY + " ====");
                for (CheckType ct : CheckType.values()) {
                    int vl = plugin.getPunishmentManager().getViolation(target, ct);
                    if (vl > 0) {
                        sender.sendMessage(ChatColor.GRAY + "- " + ChatColor.YELLOW + ct.getDisplayName() + ": " + ChatColor.RED + vl + " VL");
                    }
                }
                break;

            case "client":
            case "check":
            case "info":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.YELLOW + "วิธีใช้: /" + label + " " + sub + " <player>");
                    return true;
                }
                Player targetPlayer = Bukkit.getPlayer(args[1]);
                if (targetPlayer == null) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.RED + "ไม่พบผู้เล่น " + args[1]);
                    return true;
                }
                com.antimery.client.ClientInfo clientInfo = plugin.getClientDetector().getClientInfo(targetPlayer);

                String ip = "Unknown";
                if (targetPlayer.getAddress() != null && targetPlayer.getAddress().getAddress() != null) {
                    ip = targetPlayer.getAddress().getAddress().getHostAddress();
                }

                int ping = targetPlayer.getPing();
                Location loc = targetPlayer.getLocation();
                String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "Unknown";
                String coords = String.format("X: %.1f, Y: %.1f, Z: %.1f (Yaw: %.1f, Pitch: %.1f)",
                        loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());

                sender.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------------------------");
                sender.sendMessage(ChatColor.RED + "AntiMery ข้อมูลผู้เล่น: " + ChatColor.YELLOW + targetPlayer.getName());
                sender.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------------------------");
                sender.sendMessage(ChatColor.GRAY + "ไคลเอนต์: " + ChatColor.WHITE + clientInfo.getCleanName() + " " +
                        (clientInfo.getType().isHackClient() ? ChatColor.RED : (clientInfo.getType().isPvpClient() ? ChatColor.GREEN : ChatColor.AQUA)) + "[" + clientInfo.getType().getDisplayName() + "]");
                sender.sendMessage(ChatColor.GRAY + "Raw Brand: " + ChatColor.DARK_GRAY + clientInfo.getRawBrand());
                sender.sendMessage(ChatColor.GRAY + "ที่อยู่ IP: " + ChatColor.YELLOW + ip + ChatColor.GRAY + " | ปิง (Ping): " + ChatColor.GREEN + ping + " ms");
                sender.sendMessage(ChatColor.GRAY + "โลก (World): " + ChatColor.WHITE + worldName);
                sender.sendMessage(ChatColor.GRAY + "ตำแหน่งพิกัด: " + ChatColor.WHITE + coords);
                sender.sendMessage(ChatColor.GRAY + "โหมดการเล่น: " + ChatColor.AQUA + targetPlayer.getGameMode().name() +
                        ChatColor.GRAY + " | เลือด: " + ChatColor.RED + String.format("%.1f", targetPlayer.getHealth()) +
                        ChatColor.GRAY + " | อาหาร: " + ChatColor.GOLD + targetPlayer.getFoodLevel());
                sender.sendMessage(ChatColor.GRAY + "สถานะการบิน: " + ChatColor.WHITE + "Flying: " + targetPlayer.isFlying() + ", AllowFlight: " + targetPlayer.getAllowFlight());

                // Atlas & Watchdog Diagnostics
                if (plugin.getAtlasManager() != null) {
                    com.antimery.atlas.AtlasProfile atlas = plugin.getAtlasManager().getProfile(targetPlayer);
                    double totalSuspicion = atlas.getTotalSuspicion();
                    int confidence = atlas.getConfidence();

                    ChatColor threatColor = totalSuspicion > 50 ? ChatColor.RED : (totalSuspicion > 20 ? ChatColor.YELLOW : ChatColor.GREEN);
                    sender.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------------------------");
                    sender.sendMessage(ChatColor.GOLD + "ระบบวิเคราะห์ระดับสูง (Hypixel-Inspired Systems):");
                    sender.sendMessage(ChatColor.GRAY + "• Atlas Threat Index: " + threatColor + String.format("%.1f", totalSuspicion) +
                            ChatColor.GRAY + " | Confidence: " + threatColor + confidence + "%");
                    sender.sendMessage(ChatColor.GRAY + "• Watchdog Suspicion: " + ChatColor.YELLOW + String.format("%.1f", atlas.getWatchdogSuspicion()) +
                            ChatColor.GRAY + " | Reach: " + ChatColor.WHITE + String.format("%.2f", atlas.getLastReachDistance()) + "b" +
                            ChatColor.GRAY + " | CPS Jitter: " + ChatColor.WHITE + String.format("%.2f", atlas.getLastCpsStandardDeviation()) + "ms");
                    sender.sendMessage(ChatColor.GRAY + "• NCP Physics Suspicion: " + ChatColor.YELLOW + String.format("%.1f", atlas.getNcpSuspicion()));
                    sender.sendMessage(ChatColor.GRAY + "• Prediction Vector Error: " + ChatColor.YELLOW + String.format("%.3f", atlas.getLastPredictionOffset()) +
                            ChatColor.GRAY + " (Suspicion: " + String.format("%.1f", atlas.getPredictionSuspicion()) + ")");

                    // Ban Prediction Engine
                    double maxSuspicion = plugin.getConfigManager().getConfig().getDouble("atlas.max-suspicion", 100.0);
                    int banProbability = atlas.getPredictedBanProbability(maxSuspicion);
                    String banStatus = atlas.getPredictedBanStatus(maxSuspicion);
                    double timeUntilBan = atlas.getPredictedTimeUntilBan();

                    sender.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------------------------");
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "ระบบทำนายการลงโทษ (Ban Prediction Engine):");
                    sender.sendMessage(ChatColor.GRAY + "• โอกาสถูกลงโทษ (Ban Probability): " +
                            (banProbability >= 75 ? ChatColor.RED : (banProbability >= 40 ? ChatColor.YELLOW : ChatColor.GREEN)) + banProbability + "%");
                    sender.sendMessage(ChatColor.GRAY + "• สถานะทำนาย: " + banStatus);
                    if (atlas.isCurrentlyAirborne()) {
                        sender.sendMessage(ChatColor.GRAY + "• บินใน GameMode 0: " + ChatColor.RED + String.format("%.1f", atlas.getCurrentAirborneSeconds()) + "s / 20.0s " +
                                ChatColor.YELLOW + "(คาดการณ์แบนในอีก ~" + String.format("%.1f", timeUntilBan) + " วินาที)");
                    } else {
                        sender.sendMessage(ChatColor.GRAY + "• บินใน GameMode 0: " + ChatColor.GREEN + "0.0s (แตะพื้นปกติ ปลอดภัย)");
                    }
                }

                // Show violations
                int totalVl = 0;
                StringBuilder vlDetails = new StringBuilder();
                for (CheckType ct : CheckType.values()) {
                    int vl = plugin.getPunishmentManager().getViolation(targetPlayer, ct);
                    if (vl > 0) {
                        totalVl += vl;
                        vlDetails.append("\n  ").append(ChatColor.GRAY).append("- ").append(ChatColor.YELLOW).append(ct.getDisplayName()).append(": ").append(ChatColor.RED).append(vl).append(" VL");
                    }
                }
                if (totalVl == 0) {
                    sender.sendMessage(ChatColor.GRAY + "คะแนนความผิด (VL): " + ChatColor.GREEN + "0 VL (สะอาด ปกติดี)");
                } else {
                    sender.sendMessage(ChatColor.GRAY + "คะแนนความผิดรวม: " + ChatColor.RED + totalVl + " VL" + vlDetails.toString());
                }
                sender.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------------------------");
                break;

            case "predict":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.YELLOW + "วิธีใช้: /" + label + " predict <player>");
                    return true;
                }
                Player pTarget = Bukkit.getPlayer(args[1]);
                if (pTarget == null) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.RED + "ไม่พบผู้เล่น " + args[1]);
                    return true;
                }
                if (plugin.getAtlasManager() != null) {
                    com.antimery.atlas.AtlasProfile pAtlas = plugin.getAtlasManager().getProfile(pTarget);
                    double mSuspicion = plugin.getConfigManager().getConfig().getDouble("atlas.max-suspicion", 100.0);
                    int bProb = pAtlas.getPredictedBanProbability(mSuspicion);
                    String bStat = pAtlas.getPredictedBanStatus(mSuspicion);
                    double tBan = pAtlas.getPredictedTimeUntilBan();

                    sender.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------------------------");
                    sender.sendMessage(ChatColor.LIGHT_PURPLE + "การทำนายการลงโทษผู้เล่น: " + ChatColor.YELLOW + pTarget.getName());
                    sender.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------------------------");
                    sender.sendMessage(ChatColor.GRAY + "โหมดปัจจุบัน: " + ChatColor.AQUA + pTarget.getGameMode().name());
                    sender.sendMessage(ChatColor.GRAY + "โอกาสถูกแบน/เตะ (Ban Probability): " +
                            (bProb >= 75 ? ChatColor.RED : (bProb >= 40 ? ChatColor.YELLOW : ChatColor.GREEN)) + bProb + "%");
                    sender.sendMessage(ChatColor.GRAY + "สถานะการประเมิน: " + bStat);
                    sender.sendMessage(ChatColor.GRAY + "ดัชนี Atlas Suspicion: " + ChatColor.YELLOW + String.format("%.1f / %.1f", pAtlas.getTotalSuspicion(), mSuspicion));
                    if (pAtlas.isCurrentlyAirborne()) {
                        sender.sendMessage(ChatColor.GRAY + "เวลาลอยตัวใน GameMode 0: " + ChatColor.RED + String.format("%.1f", pAtlas.getCurrentAirborneSeconds()) + " วินาที / 20.0s");
                        sender.sendMessage(ChatColor.RED + "⚠ คาดการณ์ว่าจะถูกเตะ/แบนในอีก: " + ChatColor.YELLOW + String.format("%.1f", tBan) + " วินาที (หากไม่ลงแตะพื้น)");
                    } else {
                        sender.sendMessage(ChatColor.GRAY + "เวลาลอยตัวใน GameMode 0: " + ChatColor.GREEN + "0.0 วินาที (อยู่บนพื้นปกติ)");
                    }
                    sender.sendMessage(ChatColor.DARK_GRAY + "§m--------------------------------------------------");
                }
                break;

            case "reset":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.YELLOW + "วิธีใช้: /" + label + " reset <player>");
                    return true;
                }
                Player targetReset = Bukkit.getPlayer(args[1]);
                if (targetReset == null) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.RED + "ไม่พบผู้เล่น " + args[1]);
                    return true;
                }
                plugin.getPunishmentManager().resetViolations(targetReset.getUniqueId());
                if (plugin.getAtlasManager() != null) {
                    plugin.getAtlasManager().getProfile(targetReset).reset();
                }
                sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.GREEN + "รีเซ็ตคะแนนความผิดและ Atlas Threat Score ของผู้เล่น " + targetReset.getName() + " เรียบร้อย!");
                break;

            case "warn":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.YELLOW + "วิธีใช้: /" + label + " warn <player> [เหตุผล]");
                    return true;
                }
                Player targetWarn = Bukkit.getPlayer(args[1]);
                if (targetWarn == null) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.RED + "ไม่พบผู้เล่น " + args[1]);
                    return true;
                }
                String reasonWarn = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "พฤติกรรมน่าสงสัย";
                plugin.getPunishmentManager().executePunishment(targetWarn, CheckType.FLY, PunishmentType.WARN, reasonWarn);
                sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.GREEN + "ส่งคำเตือนไปยัง " + targetWarn.getName() + " เรียบร้อย!");
                break;

            case "kick":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.YELLOW + "วิธีใช้: /" + label + " kick <player> [เหตุผล]");
                    return true;
                }
                Player targetKick = Bukkit.getPlayer(args[1]);
                if (targetKick == null) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.RED + "ไม่พบผู้เล่น " + args[1]);
                    return true;
                }
                String reasonKick = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "ตรวจพบโปรแกรมช่วยเล่น";
                plugin.getPunishmentManager().executePunishment(targetKick, CheckType.FLY, PunishmentType.KICK, reasonKick);
                sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.GREEN + "เตะผู้เล่น " + targetKick.getName() + " ออกจากเซิร์ฟเวอร์เรียบร้อย!");
                break;

            case "ban":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.YELLOW + "วิธีใช้: /" + label + " ban <player> [เหตุผล]");
                    return true;
                }
                Player targetBan = Bukkit.getPlayer(args[1]);
                if (targetBan == null) {
                    sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.RED + "ไม่พบผู้เล่น " + args[1]);
                    return true;
                }
                String reasonBan = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : "ตรวจพบโปรแกรมช่วยเล่น";
                plugin.getPunishmentManager().executePunishment(targetBan, CheckType.FLY, PunishmentType.BAN, reasonBan);
                sender.sendMessage(plugin.getConfigManager().getPrefix() + ChatColor.GREEN + "แบนผู้เล่น " + targetBan.getName() + " เรียบร้อย!");
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.DARK_GRAY + "================== " + ChatColor.RED + "AntiMery v26.3" + ChatColor.DARK_GRAY + " ==================");
        sender.sendMessage(ChatColor.YELLOW + "/antimery reload " + ChatColor.GRAY + "- รีโหลดคอนฟิกและฐานข้อมูลคำหยาบ");
        sender.sendMessage(ChatColor.YELLOW + "/antimery check <player> " + ChatColor.GRAY + "- ตรวจสอบข้อมูลผู้เล่นเต็มรูปแบบ (IP, พิกัด, โลก, ไคลเอนต์, ปิง)");
        sender.sendMessage(ChatColor.YELLOW + "/antimery predict <player> " + ChatColor.GRAY + "- ทำนายความเสี่ยงการโดนแบน และการบิน GameMode 0 (>20s)");
        sender.sendMessage(ChatColor.YELLOW + "/antimery stats <player> " + ChatColor.GRAY + "- ดูคะแนนความผิดของผู้เล่น");
        sender.sendMessage(ChatColor.YELLOW + "/antimery client <player> " + ChatColor.GRAY + "- เช็คไคลเอนต์/ม็อดของผู้เล่น (Lunar, LabyMod, ธรรมดา, แฮค)");
        sender.sendMessage(ChatColor.YELLOW + "/antimery reset <player> " + ChatColor.GRAY + "- ล้างประวัติความผิดของผู้เล่น");
        sender.sendMessage(ChatColor.YELLOW + "/antimery warn <player> [เหตุผล] " + ChatColor.GRAY + "- สั่งตักเตือนผู้เล่น");
        sender.sendMessage(ChatColor.YELLOW + "/antimery kick <player> [เหตุผล] " + ChatColor.GRAY + "- สั่งเตะผู้เล่น");
        sender.sendMessage(ChatColor.YELLOW + "/antimery ban <player> [เหตุผล] " + ChatColor.GRAY + "- สั่งแบนผู้เล่น");
        sender.sendMessage(ChatColor.DARK_GRAY + "===============================================");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return subCommands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}

