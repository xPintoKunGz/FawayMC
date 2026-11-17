package com.mery.npc.gui;

import com.mery.npc.model.NPCCommand;
import com.mery.npc.model.NPCData;
import com.mery.npc.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CommandListGui {

    public static void open(Player player, NPCData data) {
        GuiHolder holder = new GuiHolder(data, "COMMANDS");
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#FF6CAB⚡ &#7366FFAction Studio &8| &#FFD166#" + data.getId()));

        ItemStack border = MainNPCGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack borderAccent = MainNPCGui.createItem(Material.MAGENTA_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, borderAccent);
            } else {
                inv.setItem(i, border);
            }
        }

        List<NPCCommand> list = data.getCommands();
        int slot = 10;
        int index = 0;

        for (NPCCommand cmd : list) {
            if (slot > 43) break;
            if (slot % 9 == 8) slot += 2;

            Material icon;
            switch (cmd.getType()) {
                case PLAYER: icon = Material.DIAMOND_SWORD; break;
                case OP: icon = Material.NETHER_STAR; break;
                case SERVER: icon = Material.ENDER_PEARL; break;
                case MESSAGE: icon = Material.BOOK; break;
                case ACTIONBAR:
                case TITLE: icon = Material.AMETHYST_SHARD; break;
                default: icon = Material.RESPAWN_ANCHOR; break;
            }

            inv.setItem(slot, MainNPCGui.createItem(icon,
                    "&#FF6CABAction #" + (index + 1) + " &8» &f" + cmd.getCommand(),
                    "&7รูปแบบการรัน: &#FFD166" + cmd.getType().name(),
                    "&7เงื่อนไขการกด: &#43E97B" + cmd.getClickTrigger().name(),
                    "&7คูลดาวน์: &#A8EDEA" + cmd.getCooldown() + " วินาที",
                    "&7ค่าธรรมเนียม: &#43E97B$" + cmd.getCost(),
                    "&7สิทธิ์ที่ต้องใช้: &f" + (cmd.getPermission().isEmpty() ? "ไม่ต้องใช้" : cmd.getPermission()),
                    "",
                    "&#FF6CAB[คลิกซ้าย] &fสลับโหมด (CONSOLE / PLAYER / OP / SERVER / MSG)",
                    "&#FFD166[คลิกขวา] &eสลับปุ่มคลิก (ANY / RIGHT / LEFT / SHIFT)",
                    "&#A8EDEA[กด Q / โยน] &bสลับเวลาคูลดาวน์ (0s / 1s / 3s / 5s / 10s)",
                    "&#FF5858[Shift + คลิกขวา] &cลบ Action นี้"));

            slot++;
            index++;
        }

        // Interactive Add Action Button (Click to type in chat!)
        inv.setItem(49, MainNPCGui.createItem(Material.END_CRYSTAL,
                "&#43E97B✚ &aสร้าง Action ใหม่ (คลิกเลย!)",
                "&7คลิกที่ปุ่มนี้แล้วพิมพ์คำสั่งหรือข้อความลงในแชทได้ทันที",
                "&7ไม่ต้องจำโค้ดคำสั่งยาวๆ อีกต่อไป!",
                "",
                "&7ตัวแปรที่ใช้ได้ในข้อความ/คำสั่ง:",
                "  &#FFD166%player% &7= ชื่อผู้เล่นที่กด",
                "  &#FFD166%world% &7= ชื่อโลกปัจจุบัน",
                "  &#FFD166%x% %y% %z% &7= พิกัดของผู้เล่น",
                "",
                "&#43E97B▸ คลิกเพื่อพิมพ์เพิ่ม Action ใหม่"));

        // Clear all actions button
        inv.setItem(51, MainNPCGui.createItem(Material.LAVA_BUCKET,
                "&#FF5858🗑 &cล้าง Action ทั้งหมด",
                "&7ลบ Action ทั้งหมดของหุ่นตัวนี้",
                "",
                "&#FF5858▸ Shift + คลิก เพื่อล้างทั้งหมด"));

        // Back button
        inv.setItem(45, MainNPCGui.createItem(Material.ARROW, "&#FF6CAB« &fย้อนกลับหน้าหลัก Studio"));

        player.openInventory(inv);
    }
}
