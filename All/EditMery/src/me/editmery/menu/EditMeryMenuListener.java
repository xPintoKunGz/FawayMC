package me.editmery.menu;

import me.editmery.ColorUtil;
import me.editmery.EditMeryManager;
import me.editmery.EditMeryPlugin;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class EditMeryMenuListener implements Listener {

    private final EditMeryPlugin plugin;
    private final EditMeryManager manager;

    public EditMeryMenuListener(EditMeryPlugin plugin, EditMeryManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory inv = event.getInventory();
        if (inv == null || !(inv.getHolder() instanceof EditMeryMenuHolder)) {
            return;
        }

        // Cancel all clicks in EditMery menus to prevent taking items
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        int rawSlot = event.getRawSlot();

        // Only handle clicks inside the top menu inventory
        if (rawSlot < 0 || rawSlot >= inv.getSize()) {
            return;
        }

        EditMeryMenuHolder holder = (EditMeryMenuHolder) inv.getHolder();
        MenuItem item = holder.getMenuItem(rawSlot);
        if (item == null) {
            return;
        }

        // Play feedback sound
        try {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
        } catch (Throwable ignored) {}

        // Custom action callback
        if (item.getAction() != null) {
            item.getAction().accept(player);
            return;
        }

        // Submenu navigation
        if (item.getNextMenu() != null) {
            EditMeryMenu.openMenu(player, item.getNextMenu());
            return;
        }

        // Command execution or suggestion
        if (item.getCommand() != null && !item.getCommand().isEmpty()) {
            // Auto-enable EditMery wand mode if currently disabled
            if (manager != null && !manager.isEnabled(player)) {
                manager.setEnabled(player, true);
                ColorUtil.sendMessage(player, "&8[&d&lEdit&b&lMery&8] &aเปิดโหมดใช้งาน EditMery ให้อัตโนมัติ!");
            }

            player.closeInventory();

            String cmd = item.getCommand().trim();
            if (item.isSuggestOnly()) {
                ColorUtil.sendSuggestCommandMessage(player,
                        "&8[&d&lEdit&b&lMery&8] &fคลิกเพื่อเตรียมใช้คำสั่ง: &b" + cmd + " &7(คลิกที่นี่)",
                        cmd,
                        "&7คลิกเพื่อใส่คำสั่ง &b" + cmd + " &7ลงในช่องแชท");
            } else {
                String execCmd = cmd.startsWith("/") ? cmd.substring(1) : cmd;
                player.performCommand(execCmd);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        Inventory inv = event.getInventory();
        if (inv != null && inv.getHolder() instanceof EditMeryMenuHolder) {
            event.setCancelled(true);
        }
    }
}
