package com.mery.npc.listener;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.mery.npc.MeryNPC;
import com.mery.npc.command.MeryNPCCommand;
import com.mery.npc.gui.MainNPCGui;
import com.mery.npc.model.ClickTrigger;
import com.mery.npc.model.CommandType;
import com.mery.npc.model.NPCCommand;
import com.mery.npc.model.NPCData;
import com.mery.npc.util.ColorUtil;
import com.mery.npc.util.PlaceholderUtil;
import com.mery.npc.util.SoundUtil;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NPCInteractListener implements Listener {

    private static NPCInteractListener instance;
    private final MeryNPC plugin;
    // Cooldown map: (playerUUID + "_" + npcId + "_" + cmdIndex) -> expiry timestamp millis
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();

    public NPCInteractListener(MeryNPC plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static void executeForNPC(Player player, NPCData data, boolean isRightClick) {
        if (instance != null) {
            instance.handleInteract(player, data, isRightClick);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        NPCData data = plugin.getNpcManager().getNPCByEntity(event.getRightClicked());
        if (data != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        NPCData data = plugin.getNpcManager().getNPCByEntity(event.getRightClicked());
        if (data != null) {
            event.setCancelled(true);
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) {
                handleInteract(event.getPlayer(), data, true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();
            NPCData data = plugin.getNpcManager().getNPCByEntity(event.getEntity());
            if (data != null) {
                event.setCancelled(true);
                handleInteract(player, data, false);
            }
        }
    }

    private void handleInteract(Player player, NPCData data, boolean isRightClick) {
        if (data == null) return;

        // Stick Target Selector for Admins: Left-click or Right-click an NPC with a Stick to select/target it!
        ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (heldItem != null && heldItem.getType() == Material.STICK && player.hasPermission("merynpc.admin")) {
            MeryNPCCommand.setSelectedNpc(player.getUniqueId(), data.getId());
            String displayTitle = (data.getName() == null || data.getName().trim().isEmpty())
                    ? "(ไร้ชื่อ)"
                    : PlaceholderUtil.format(data.getName(), player, data);
            SoundUtil.playSuccess(player);

            if (player.isSneaking() && isRightClick) {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#43E97Bเลือกหุ่นจำลอง &#FFD166#" + data.getId() + " &f" + displayTitle + " &#43E97Bและเปิดหน้าต่าง Studio แล้ว!"));
                MainNPCGui.open(player, data);
            } else {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#43E97Bเลือกเป้าหมายหุ่นจำลอง &#FFD166#" + data.getId() + " &f(" + displayTitle + "&f) &#43E97Bเรียบร้อยแล้ว! &7(Shift+คลิกขวาด้วย Stick เพื่อเปิด GUI)"));
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(
                        ColorUtil.colorize("&#FF6CAB✦ &aเลือกหุ่น &#FFD166#" + data.getId() + " &f" + displayTitle + " &8| &7โลก: &e" + data.getWorldName())));
            }
            return;
        }

        boolean isSneaking = player.isSneaking();
        ClickTrigger currentTrigger;
        if (isRightClick) {
            currentTrigger = isSneaking ? ClickTrigger.SHIFT_RIGHT_CLICK : ClickTrigger.RIGHT_CLICK;
        } else {
            currentTrigger = isSneaking ? ClickTrigger.SHIFT_LEFT_CLICK : ClickTrigger.LEFT_CLICK;
        }

        if (data.getCommands() == null || data.getCommands().isEmpty()) return;

        // Separate regular commands and random (-r) commands (CommandNPC Reloaded v0.96 behavior)
        java.util.List<NPCCommand> toExecute = new java.util.ArrayList<>();
        java.util.List<NPCCommand> randomPool = new java.util.ArrayList<>();

        for (NPCCommand cmd : data.getCommands()) {
            if (!matchesTrigger(cmd.getClickTrigger(), currentTrigger, isRightClick)) {
                continue;
            }
            if (cmd.isRandom()) {
                randomPool.add(cmd);
            } else {
                toExecute.add(cmd);
            }
        }

        if (!randomPool.isEmpty()) {
            NPCCommand picked = randomPool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(randomPool.size()));
            toExecute.add(picked);
        }

        int cmdIndex = 0;
        for (NPCCommand cmd : toExecute) {
            cmdIndex++;

            // Check Permission
            if (cmd.getPermission() != null && !cmd.getPermission().isEmpty() && !"noPerm".equalsIgnoreCase(cmd.getPermission())) {
                if (!player.hasPermission(cmd.getPermission())) {
                    if (!cmd.isIgnorePermMsg()) {
                        if (cmd.getNoPermissionMessage() != null && !cmd.getNoPermissionMessage().isEmpty()) {
                            player.sendMessage(ColorUtil.colorize(cmd.getNoPermissionMessage()));
                        } else {
                            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                        }
                    }
                    SoundUtil.playError(player);
                    continue;
                }
            }

            // Check Cooldown
            String cooldownKey = player.getUniqueId().toString() + "_" + data.getId() + "_" + cmdIndex;
            long now = System.currentTimeMillis();
            if (cmd.getCooldown() > 0 && !player.hasPermission("merynpc.bypass.cooldown")) {
                Long expire = cooldowns.get(cooldownKey);
                if (expire != null && now < expire) {
                    double remaining = Math.round((expire - now) / 100.0) / 10.0;
                    if (cmd.getCooldownMessage() != null && !cmd.getCooldownMessage().isEmpty()) {
                        player.sendMessage(ColorUtil.colorize(
                                cmd.getCooldownMessage()
                                        .replace("%time%", String.valueOf(remaining))
                                        .replace("%s", String.valueOf((int) Math.ceil(remaining)))
                        ));
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("cmd-cooldown", "%time%", String.valueOf(remaining)));
                    }
                    SoundUtil.playError(player);
                    continue;
                }
                cooldowns.put(cooldownKey, now + (long) (cmd.getCooldown() * 1000));
            }

            // Check Vault Economy Cost
            if (cmd.getCost() > 0 && plugin.getEconomy() != null) {
                Economy econ = plugin.getEconomy();
                if (!econ.has(player, cmd.getCost())) {
                    if (!cmd.isIgnoreMoneyMsg()) {
                        player.sendMessage(plugin.getConfigManager().getMessage("cmd-no-money", "%cost%", String.valueOf(cmd.getCost())));
                    }
                    SoundUtil.playError(player);
                    continue;
                }
                econ.withdrawPlayer(player, cmd.getCost());
            }

            // Parse placeholders (both MeryNPC & CommandNPC Reloaded v0.96 placeholders)
            String rawCmd = processPlaceholders(player, data, cmd.getCommand());

            if (rawCmd.startsWith("/")) {
                rawCmd = rawCmd.substring(1);
            }

            final String finalCmd = rawCmd;
            final CommandType type = cmd.getType();

            Runnable task = () -> executeCommand(player, type, finalCmd);

            if (cmd.getDelay() > 0) {
                Bukkit.getScheduler().runTaskLater(plugin, task, cmd.getDelay());
            } else {
                task.run();
            }

            SoundUtil.playClick(player);
        }
    }

    private boolean matchesTrigger(ClickTrigger cmdTrigger, ClickTrigger currentTrigger, boolean isRightClick) {
        if (cmdTrigger == null || cmdTrigger == ClickTrigger.ANY) return true;
        if (cmdTrigger == currentTrigger) return true;
        if (cmdTrigger == ClickTrigger.RIGHT_CLICK && isRightClick) return true;
        if (cmdTrigger == ClickTrigger.LEFT_CLICK && !isRightClick) return true;
        return false;
    }

    private String processPlaceholders(Player player, NPCData data, String input) {
        if (input == null) return "";
        String res = input
                .replace("%player%", player.getName())
                .replace("%name%", player.getName())
                .replace("%name", player.getName())
                .replace("<player>", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%uuid", player.getUniqueId().toString())
                .replace("%npc_id%", String.valueOf(data.getId()))
                .replace("%npc_name%", ColorUtil.stripColor(data.getName()))
                .replace("%world%", player.getWorld().getName())
                .replace("%world", player.getWorld().getName())
                .replace("%x%", String.valueOf(player.getLocation().getBlockX()))
                .replace("%y%", String.valueOf(player.getLocation().getBlockY()))
                .replace("%z%", String.valueOf(player.getLocation().getBlockZ()))
                .replace("%x", String.valueOf(player.getLocation().getBlockX()))
                .replace("%y", String.valueOf(player.getLocation().getBlockY()))
                .replace("%z", String.valueOf(player.getLocation().getBlockZ()));

        // Support PlaceholderAPI (%...% and {...}) if installed
        if (com.mery.npc.util.PlaceholderUtil.isPlaceholderAPIEnabled()) {
            try {
                Class<?> papi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
                java.lang.reflect.Method m = papi.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
                res = (String) m.invoke(null, player, res);
                java.lang.reflect.Method mb = papi.getMethod("setBracketPlaceholders", org.bukkit.OfflinePlayer.class, String.class);
                res = (String) mb.invoke(null, player, res);
            } catch (Throwable ignored) {}
        }
        return res;
    }

    private void executeCommand(Player player, CommandType type, String command) {
        String trimmed = command != null ? command.trim() : "";
        if (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1).trim();
        }

        // Automatically intercept BungeeCord / Velocity proxy commands (/server, /send, /connect, /bungee)
        // even if the admin added them under CONSOLE, PLAYER, or OP mode!
        if (type == CommandType.CONSOLE || type == CommandType.PLAYER || type == CommandType.OP) {
            if (tryExecuteBungeeProxyCommand(player, trimmed, false)) {
                return;
            }
        }

        switch (type) {
            case CONSOLE:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), trimmed);
                break;

            case PLAYER:
                player.performCommand(trimmed);
                break;

            case OP:
                boolean wasOp = player.isOp();
                try {
                    player.setOp(true);
                    player.performCommand(trimmed);
                } finally {
                    player.setOp(wasOp);
                }
                break;

            case SERVER:
                tryExecuteBungeeProxyCommand(player, trimmed, true);
                break;

            case MESSAGE:
                player.sendMessage(ColorUtil.colorize(command));
                break;

            case ACTIONBAR:
                player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(ColorUtil.colorize(command)));
                break;

            case TITLE:
                String[] parts = command.split(";", 2);
                String title = ColorUtil.colorize(parts[0]);
                String subtitle = parts.length > 1 ? ColorUtil.colorize(parts[1]) : "";
                player.sendTitle(title, subtitle, 10, 50, 15);
                break;
        }
    }

    /**
     * Handles BungeeCord / Velocity server transfers:
     * - "/server <serverName>" or "server <serverName>"
     * - "/connect <serverName>" or "/bungee <serverName>"
     * - "/send <player|all|current> <serverName>"
     * - Direct "<serverName>" when forceServerMode is true (CommandType.SERVER)
     */
    private boolean tryExecuteBungeeProxyCommand(Player player, String rawCmd, boolean forceServerMode) {
        if (rawCmd == null || rawCmd.isEmpty()) return false;
        String[] tokens = rawCmd.split("\\s+");
        String first = tokens[0].toLowerCase();

        // Case 1: /server <serverName> | /connect <serverName> | /bungee <serverName>
        if ((first.equals("server") || first.equals("connect") || first.equals("bungee") || first.equals("btp")) && tokens.length >= 2) {
            sendPlayerToBungeeServer(player, player.getName(), tokens[1]);
            return true;
        }

        // Case 2: /send <targetPlayer|all|current> <serverName>
        if (first.equals("send") && tokens.length >= 3) {
            String targetArg = tokens[1];
            String targetServer = tokens[2];
            if (targetArg.equalsIgnoreCase("all") || targetArg.equals("*")) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    sendPlayerToBungeeServer(online, online.getName(), targetServer);
                }
            } else if (targetArg.equalsIgnoreCase("current")) {
                for (Player inWorld : player.getWorld().getPlayers()) {
                    sendPlayerToBungeeServer(inWorld, inWorld.getName(), targetServer);
                }
            } else {
                sendPlayerToBungeeServer(player, targetArg, targetServer);
            }
            return true;
        }

        // Case 3: CommandType.SERVER with just "<serverName>" (e.g., "lobby", "survival", or "play.domain.com:25565")
        if (forceServerMode) {
            sendPlayerToBungeeServer(player, player.getName(), tokens[0]);
            return true;
        }

        return false;
    }

    private void sendPlayerToBungeeServer(Player carrier, String targetPlayerName, String serverName) {
        if (serverName == null || serverName.isEmpty()) return;
        try {
            // Check if direct host:port transfer (Paper 1.20.5+ / 26.3 transfer packet)
            if (serverName.contains(":") && !serverName.startsWith(":")) {
                String[] hp = serverName.split(":", 2);
                try {
                    int port = Integer.parseInt(hp[1]);
                    Player targetOnline = Bukkit.getPlayerExact(targetPlayerName);
                    if (targetOnline != null) {
                        targetOnline.transfer(hp[0], port);
                        return;
                    }
                } catch (NumberFormatException ignored) {}
            }

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            if (targetPlayerName == null || targetPlayerName.equalsIgnoreCase(carrier.getName())) {
                out.writeUTF("Connect");
                out.writeUTF(serverName.trim());
            } else {
                out.writeUTF("ConnectOther");
                out.writeUTF(targetPlayerName.trim());
                out.writeUTF(serverName.trim());
            }
            carrier.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        } catch (Exception e) {
            carrier.sendMessage(ColorUtil.colorize("&#FF5858✖ ไม่สามารถเชื่อมต่อเซิร์ฟเวอร์ BungeeCord: &f" + serverName));
        }
    }
}
