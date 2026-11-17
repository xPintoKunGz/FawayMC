package me.editmery;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class EditMeryListener implements Listener {

    private final EditMeryPlugin plugin;
    private final EditMeryManager manager;

    private static final Set<String> WORLDEDIT_SINGLE_COMMANDS = new HashSet<>(Arrays.asList(
            "we", "worldedit", "fawe", "fastasyncworldedit",
            "brush", "tool", "mask", "gmask", "transform", "gtransform",
            "superpickaxe", "sp", "toggleplace",
            "fixwater", "fixlava", "drain", "snow", "thaw", "green", "extinguish",
            "removeabove", "removebelow", "removenear", "replacenear", "fill", "fillr",
            "clearclipboard", "watchdog", "snapshots", "delchunks", "listchunks", "chunkinfo",
            "unstuck", "ascend", "descend", "ceil", "ceiling", "thru", "jumpto", "up",
            "calc", "biomeinfo", "biomelist", "butcher"
    ));

    public EditMeryListener(EditMeryPlugin plugin, EditMeryManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (manager.isDisableOnJoin()) {
            manager.setEnabled(player, false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandSend(PlayerCommandSendEvent event) {
        Player player = event.getPlayer();
        // ถ้าผู้เล่นไม่ใช่ OP ไม่ต้องแสดงคำสั่ง editmery, em ใน Command Autocomplete บน Minecraft
        if (!player.isOp() && !player.hasPermission("editmery.use")) {
            event.getCommands().removeIf(cmd ->
                    cmd.equalsIgnoreCase("editmery") || cmd.equalsIgnoreCase("em")
                    || cmd.equalsIgnoreCase("emery") || cmd.equalsIgnoreCase("editm")
                    || cmd.toLowerCase(Locale.ROOT).startsWith("editmery:")
                    || cmd.toLowerCase(Locale.ROOT).startsWith("em:")
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        manager.removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        // If player already turned on EditMery, allow everything
        if (manager.isEnabled(player)) {
            return;
        }

        // If player has bypass permission and bypass is enabled
        if (manager.canBypass(player)) {
            return;
        }

        String raw = event.getMessage();
        if (raw == null || raw.isEmpty()) return;

        // Strip leading slash
        String cmd = raw.startsWith("/") ? raw.substring(1) : raw;
        cmd = cmd.trim();

        String[] parts = cmd.split("\\s+");
        String label = parts[0].toLowerCase(Locale.ROOT);

        // Do not block our own /editmery command
        if (isEditMeryCommand(label)) {
            return;
        }

        boolean isWorldEdit = false;

        // Check double-slash command (//set, //wand, //pos1, etc.)
        if (cmd.startsWith("/")) {
            isWorldEdit = true;
        } else if (label.startsWith("worldedit:") || label.startsWith("fawe:") || label.startsWith("fastasyncworldedit:")) {
            isWorldEdit = true;
        } else if (WORLDEDIT_SINGLE_COMMANDS.contains(label)) {
            isWorldEdit = true;
        }

        if (isWorldEdit) {
            event.setCancelled(true);

            // แจ้งเตือนในแชทเฉพาะเมื่อเป็นคำสั่ง WorldEdit
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            boolean holdingAxe = (mainHand != null && mainHand.getType() == Material.WOODEN_AXE);

            if (player.isOp() || player.hasPermission("editmery.use")) {
                if (holdingAxe) {
                    player.sendMessage(ColorUtil.colorize("&8(&#B388FF✦ &#FF80ABEditMery&8) &#94A3B8ขวาน WorldEdit ปิดอยู่ &8| &#F8FAFCพิมพ์ &#FFD166/em enable &#F8FAFCเพื่อเปิดใช้งาน"));
                } else {
                    player.sendMessage(ColorUtil.colorize("&8(&#B388FF✦ &#FF80ABEditMery&8) &#EF476F✖ &cโหมด EditMery ปิดอยู่ &8(พิมพ์ &#FFD166/em enable &8เพื่อเปิดใช้งาน)"));
                }
            } else {
                player.sendMessage(ColorUtil.colorize("&8(&#B388FF✦ &#FF80ABEditMery&8) &#EF476F✖ &cคำสั่งนี้สงวนสิทธิ์เฉพาะผู้ดูแลเซิร์ฟเวอร์ (OP) เท่านั้น"));
            }
        }
    }

    private boolean isEditMeryCommand(String label) {
        if (label.equals("editmery") || label.equals("em") || label.equals("emery") || label.equals("editm")) {
            return true;
        }
        if (label.startsWith("editmery:") || label.startsWith("em:")) {
            String sub = label.substring(label.indexOf(':') + 1);
            return sub.equals("editmery") || sub.equals("em") || sub.equals("emery") || sub.equals("editm");
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        // If player already turned on EditMery or has bypass, let WorldEdit handle wand normally
        if (manager.isEnabled(player) || manager.canBypass(player)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.WOODEN_AXE) {
            return;
        }

        // เมื่อโหมดปิดอยู่: สั่งไม่ให้ WorldEdit ใช้งานเป็นคฑา Wand
        // **ที่สำคัญคือไม่สั่ง event.setCancelled(true)** เพื่อให้ผู้เล่นสามารถตัดไม้ และทุบบล็อกได้ตามปกติ 100%!
        event.setUseItemInHand(Event.Result.DENY);

        // หากเป็นแอดมิน (OP) แล้วคลิกขวาในอากาศขณะถือขวานไม้ ให้แนะนำคำสั่งเปิดโหมด
        if (player.isOp() || player.hasPermission("editmery.use")) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR) {
                manager.sendAxeHeldHint(player);
            }
        }
    }
}
