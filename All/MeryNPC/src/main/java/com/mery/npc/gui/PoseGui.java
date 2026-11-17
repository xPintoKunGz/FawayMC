package com.mery.npc.gui;

import com.mery.npc.model.NPCData;
import com.mery.npc.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class PoseGui {

    public static void open(Player player, NPCData data) {
        GuiHolder holder = new GuiHolder(data, "POSE");
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#00F2FE🎭 &#4FACFEPose & Stance &8| &#00F2FE#" + data.getId()));

        ItemStack border = MainNPCGui.createItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack borderCyan = MainNPCGui.createItem(Material.CYAN_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, borderCyan);
            } else {
                inv.setItem(i, border);
            }
        }

        String cur = data.getPose() != null ? data.getPose().getPresetName() : "STANDING";

        inv.setItem(10, MainNPCGui.createItem(Material.SHIELD,
                "&#00F2FE🛡 ท่าเตรียมพร้อมรบ (GUARD)",
                "&7ท่าตั้งการ์ดพร้อมสู้ ถือดาบและยกโล่เตรียมพร้อม",
                cur.equals("GUARD") ? "&#43E97B✔ กำลังใช้งาน" : "&#A8EDEA▸ คลิกเพื่อเลือก"));

        inv.setItem(11, MainNPCGui.createItem(Material.DIAMOND_SWORD,
                "&#00F2FE⚔ ท่าโจมตี (ATTACK)",
                "&7ท่ายกดาบฟันเฉียง พร้อมพุ่งโจมตีศัตรู",
                cur.equals("ATTACK") ? "&#43E97B✔ กำลังใช้งาน" : "&#A8EDEA▸ คลิกเพื่อเลือก"));

        inv.setItem(12, MainNPCGui.createItem(Material.IRON_DOOR,
                "&#00F2FE🛡 ท่าป้องกันเต็มรูปแบบ (DEFEND)",
                "&7ท่ายกโล่ขึ้นบังด้านหน้าเต็มตัวเพื่อป้องกันการโจมตี",
                cur.equals("DEFEND") ? "&#43E97B✔ กำลังใช้งาน" : "&#A8EDEA▸ คลิกเพื่อเลือก"));

        inv.setItem(13, MainNPCGui.createItem(Material.GOLDEN_HELMET,
                "&#00F2FE🎖 ท่าวัดทยาหัตถ์ (SALUTE)",
                "&7ท่ายกมือขวาขึ้นแตะหน้าผากเพื่อทำความเคารพ",
                cur.equals("SALUTE") ? "&#43E97B✔ กำลังใช้งาน" : "&#A8EDEA▸ คลิกเพื่อเลือก"));

        inv.setItem(14, MainNPCGui.createItem(Material.STICK,
                "&#00F2FE👉 ท่าชี้นิ้วบอกทาง (POINTING)",
                "&7ท่ายื่นแขนขวาชี้ตรงไปข้างหน้า เหมาะกับ NPC นำทาง/เควส",
                cur.equals("POINTING") ? "&#43E97B✔ กำลังใช้งาน" : "&#A8EDEA▸ คลิกเพื่อเลือก"));

        inv.setItem(15, MainNPCGui.createItem(Material.FIREWORK_ROCKET,
                "&#00F2FE🎉 ท่าดีใจ / ไชโย (CHEERING)",
                "&7ท่ายกแขนทั้งสองข้างขึ้นสูงด้วยความยินดี ฉลองชัยชนะ",
                cur.equals("CHEERING") ? "&#43E97B✔ กำลังใช้งาน" : "&#A8EDEA▸ คลิกเพื่อเลือก"));

        inv.setItem(16, MainNPCGui.createItem(Material.ROTTEN_FLESH,
                "&#00F2FE🧟 ท่าซอมบี้ (ZOMBIE)",
                "&7ท่ายื่นแขนทั้งสองข้างไปข้างหน้าตรงๆ สไตล์ซอมบี้",
                cur.equals("ZOMBIE") ? "&#43E97B✔ กำลังใช้งาน" : "&#A8EDEA▸ คลิกเพื่อเลือก"));

        inv.setItem(22, MainNPCGui.createItem(Material.OAK_STAIRS,
                "&#00F2FE🪑 ท่านั่ง (SITTING)",
                "&7ท่านั่งยอง/นั่งพักผ่อน",
                cur.equals("SITTING") ? "&#43E97B✔ กำลังใช้งาน" : "&#A8EDEA▸ คลิกเพื่อเลือก"));

        inv.setItem(31, MainNPCGui.createItem(Material.ARMOR_STAND,
                "&#00F2FE🧍 ท่ายืนปกติ (STANDING)",
                "&7ท่ายืนตรงตามธรรมชาติแบบมาตรฐาน",
                cur.equals("STANDING") ? "&#43E97B✔ กำลังใช้งาน" : "&#A8EDEA▸ คลิกเพื่อเลือก"));

        // Back button
        inv.setItem(45, MainNPCGui.createItem(Material.ARROW, "&#FF6584« &#cย้อนกลับ"));

        player.openInventory(inv);
    }
}

