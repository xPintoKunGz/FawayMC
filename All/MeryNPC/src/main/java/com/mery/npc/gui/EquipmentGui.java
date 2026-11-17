package com.mery.npc.gui;

import com.mery.npc.model.NPCData;
import com.mery.npc.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EquipmentGui {

    public static void open(Player player, NPCData data) {
        GuiHolder holder = new GuiHolder(data, "EQUIPMENT");
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#00F2FE⚔ &#4FACFEEquipment &8| &#00F2FE#" + data.getId()));

        ItemStack border = MainNPCGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack borderCyan = MainNPCGui.createItem(Material.CYAN_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, borderCyan);
            } else {
                inv.setItem(i, border);
            }
        }

        Map<String, ItemStack> equip = data.getEquipment();

        // Armor column
        inv.setItem(10, getDisplayItem(equip.get("HELMET"), Material.LEATHER_HELMET, "&#00F2FEหมวก (Helmet)", "HELMET"));
        inv.setItem(19, getDisplayItem(equip.get("CHESTPLATE"), Material.LEATHER_CHESTPLATE, "&#00F2FEเสื้อเกราะ (Chestplate)", "CHESTPLATE"));
        inv.setItem(28, getDisplayItem(equip.get("LEGGINGS"), Material.LEATHER_LEGGINGS, "&#00F2FEกางเกง (Leggings)", "LEGGINGS"));
        inv.setItem(37, getDisplayItem(equip.get("BOOTS"), Material.LEATHER_BOOTS, "&#00F2FEรองเท้า (Boots)", "BOOTS"));

        // Weapon / Hands
        inv.setItem(23, getDisplayItem(equip.get("MAIN_HAND"), Material.DIAMOND_SWORD, "&#00F2FEมือหลัก - ดาบ/อาวุธ (Main Hand)", "MAIN_HAND"));
        inv.setItem(25, getDisplayItem(equip.get("OFF_HAND"), Material.SHIELD, "&#00F2FEมือรอง - โล่ (Off Hand)", "OFF_HAND"));

        // Copy button
        inv.setItem(49, MainNPCGui.createItem(Material.NETHERITE_CHESTPLATE,
                "&#43E97B✔ &#aคัดลอกชุดเกราะ & อาวุธจากตัวคุณ",
                "&7คัดลอกอาวุธ ดาบ โล่ และชุดเกราะที่คุณใส่อยู่",
                "&7มาสวมใส่ให้ NPC ตัวนี้ทั้งหมดในคลิกเดียว!",
                "",
                "&#43E97B▸ คลิกเพื่อคัดลอกทันที"));

        // Clear button
        inv.setItem(51, MainNPCGui.createItem(Material.CAULDRON,
                "&#FF6584✖ &#cถอดชุดเกราะและอาวุธทั้งหมด",
                "&7ล้างอุปกรณ์ทั้งหมดของ NPC ให้ว่างเปล่า",
                "",
                "&#FF6584▸ คลิกเพื่อถอดทั้งหมด"));

        // Back button
        inv.setItem(45, MainNPCGui.createItem(Material.ARROW, "&#FF6584« &#cย้อนกลับ"));

        player.openInventory(inv);
    }

    private static ItemStack getDisplayItem(ItemStack current, Material fallbackIcon, String slotName, String slotKey) {
        if (current != null && current.getType() != Material.AIR) {
            ItemStack clone = current.clone();
            ItemMeta meta = clone.getItemMeta();
            if (meta != null) {
                List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
                lore.add("");
                lore.add(ColorUtil.colorize("&8[" + slotName + "&8]"));
                lore.add(ColorUtil.colorize("&#FF6584▸ คลิกซ้าย เพื่อใส่ไอเทมจาก Cursor"));
                lore.add(ColorUtil.colorize("&#FF5858▸ คลิกขวา เพื่อถอดไอเทมออก"));
                meta.setLore(lore);
                clone.setItemMeta(meta);
            }
            return clone;
        } else {
            return MainNPCGui.createItem(fallbackIcon,
                    slotName + " &8(&7ว่าง&8)",
                    "&7ยังไม่มีไอเทมสวมใส่ในช่องนี้",
                    "",
                    "&#43E97B▸ คลิก เพื่อนำไอเทมใน Cursor มาสวมใส่");
        }
    }
}

