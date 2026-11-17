package com.mery.npc.gui;

import com.mery.npc.model.NPCData;
import com.mery.npc.util.ColorUtil;
import com.mery.npc.util.PlaceholderUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class HologramGui {

    public static void open(Player player, NPCData data) {
        GuiHolder holder = new GuiHolder(data, "HOLOGRAM");
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#FF6CAB✨ &#7366FFHologram Studio &8| &#FFD166#" + data.getId()));

        ItemStack border = MainNPCGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack borderAccent = MainNPCGui.createItem(Material.MAGENTA_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, borderAccent);
            } else {
                inv.setItem(i, border);
            }
        }

        // Slot 4: Main NPC Title (Line 0)
        String rawTitle = data.getName() != null ? data.getName() : "";
        String liveTitle = rawTitle.isEmpty() ? "&7(ซ่อนชื่อหลักไว้)" : PlaceholderUtil.format(rawTitle, player, data);
        inv.setItem(4, MainNPCGui.createItem(Material.NAME_TAG,
                "&#FF6CAB👑 &#FFD166บรรทัดชื่อหลัก (Title Line) &8» &f" + liveTitle,
                "&7โค้ดต้นฉบับ: &f" + (rawTitle.isEmpty() ? "(ว่างเปล่า)" : rawTitle.replace("§", "&")),
                "&7แสดงผลจริง (Real-Time): &f" + liveTitle,
                "",
                "&#FF6CAB[คลิกซ้าย] &fพิมพ์แก้ไขบรรทัดชื่อหลักในแชท",
                "&#FF5858[คลิกขวา] &7ซ่อน/ลบบรรทัดชื่อหลักออก"));

        List<String> lines = data.getHologramLines();
        int slot = 10;
        int index = 0;

        for (String rawLine : lines) {
            if (slot > 43) break;
            if (slot % 9 == 8) slot += 2;

            String safeRaw = rawLine != null ? rawLine : "";
            String livePreview = safeRaw.trim().isEmpty() ? "&8(บรรทัดว่างเว้นระยะ)" : PlaceholderUtil.format(safeRaw, player, data);

            inv.setItem(slot, MainNPCGui.createItem(Material.PAPER,
                    "&#FF6CAB✦ &#FFD166บรรทัดที่ #" + (index + 1) + " &8» &f" + livePreview,
                    "&7โค้ดต้นฉบับ: &e" + (safeRaw.isEmpty() ? "(บรรทัดว่าง)" : safeRaw.replace("§", "&")),
                    "&7แสดงผลจริง (Real-Time): &f" + livePreview,
                    "",
                    "&#FF6CAB[คลิกซ้าย] &fแก้ไขข้อความบรรทัดที่ #" + (index + 1) + " นี้",
                    "&#FFD166[คลิกขวา] &eแทรกบรรทัดใหม่ต่อท้ายบรรทัดนี้",
                    "&#43E97B[กดปุ่ม Q / โยน] &aเลื่อนบรรทัดนี้ขึ้นด้านบน ▲",
                    "&#A8EDEA[Shift + คลิกซ้าย] &bเลื่อนบรรทัดนี้ลงด้านล่าง ▼",
                    "&#FF5858[Shift + คลิกขวา] &cลบบรรทัดที่ #" + (index + 1) + " นี้ออก"));

            slot++;
            index++;
        }

        // Slot 48: Hologram Height Offset adjustment
        double offset = Math.round(data.getHologramOffset() * 100.0) / 100.0;
        inv.setItem(48, MainNPCGui.createItem(Material.FEATHER,
                "&#FF6CAB📏 &#FFD166ระดับความสูงป้าย (Height Offset)",
                "&7ปรับระดับความสูงของ Hologram เหนือหัวหุ่น",
                "&7ระดับปัจจุบัน: &#43E97B" + (offset >= 0 ? "+" : "") + offset + " บล็อก",
                "",
                "&#FF6CAB[คลิกซ้าย] &fยกป้ายสูงขึ้น (+0.1 บล็อก)",
                "&#FFD166[คลิกขวา] &eลดป้ายต่ำลง (-0.1 บล็อก)",
                "&#A8EDEA[Shift + คลิก] &7รีเซ็ตระดับความสูงเป็นค่าเริ่มต้น (0.25)"));

        // Slot 49: Add New Line Button
        inv.setItem(49, MainNPCGui.createItem(Material.END_CRYSTAL,
                "&#43E97B✚ &aเพิ่มบรรทัด Hologram ใหม่ (คลิกเลย!)",
                "&7คลิกเพื่อพิมพ์ข้อความบรรทัดใหม่ต่อท้ายในแชท",
                "&7รองรับสี &#FF6CAB& &#FFD166&#HEX &7และ &#43E97BPlaceholderAPI &7แบบ Real-Time!",
                "",
                "&7ตัวอย่างตัวแปรยอดนิยม:",
                "  &#FFD166%online% &7หรือ &#FFD166%server_online% &7= ออนไลน์เซิร์ฟนี้",
                "  &#FFD166%bungee_<ชื่อเซิร์ฟ>% &7= ออนไลน์เซิร์ฟ BungeeCord",
                "  &#FFD166%player% &7/ &#FFD166%vault_eco_balance_formatted%",
                "",
                "&#43E97B▸ คลิกเพื่อพิมพ์เพิ่มบรรทัดใหม่"));

        // Slot 51: Clear All Lines
        inv.setItem(51, MainNPCGui.createItem(Material.LAVA_BUCKET,
                "&#FF5858🗑 &cล้างบรรทัดเสริมทั้งหมด",
                "&7ลบข้อความบรรทัดเสริมทั้งหมดของหุ่นตัวนี้",
                "",
                "&#FF5858▸ กด Shift + คลิก เพื่อล้างทั้งหมด"));

        // Slot 45: Back button
        inv.setItem(45, MainNPCGui.createItem(Material.ARROW, "&#FF6CAB« &fย้อนกลับหน้าหลัก Studio"));

        player.openInventory(inv);
    }
}

