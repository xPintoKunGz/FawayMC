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

public class MainNPCGui {

    public static void open(Player player, NPCData data) {
        GuiHolder holder = new GuiHolder(data, "MAIN");
        Inventory inv = Bukkit.createInventory(holder, 54, ColorUtil.colorize("&#FF6CAB✦ &#7366FFMeryNPC Studio &8| &#FFD166#" + data.getId()));

        // Royal Amethyst & Magenta Glass Frame
        ItemStack borderAccent = createItem(Material.MAGENTA_STAINED_GLASS_PANE, " ");
        ItemStack borderDark = createItem(Material.BLACK_STAINED_GLASS_PANE, " ");

        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                inv.setItem(i, borderAccent);
            } else {
                inv.setItem(i, borderDark);
            }
        }

        String displayTitle = data.getName().isEmpty() ? "&7(ไร้ชื่อบนหัว)" : data.getName();

        // Slot 4: Persona Identity Card (Click to Rename via Chat!)
        inv.setItem(4, createItem(Material.END_CRYSTAL,
                "&#FF6CAB✦ &#FFD166หุ่นจำลอง: &f" + displayTitle + " &8(&#FF6CAB#" + data.getId() + "&8)",
                "&7โลกพิกัด: &f" + data.getWorldName() + " &7(" + (int) data.getX() + ", " + (int) data.getY() + ", " + (int) data.getZ() + ")",
                "&7รูปลักษณ์: &#FF6CAB" + data.getType() + " &8| &7ขนาด: &#FFD166" + data.getScale() + "x",
                "&7สกิน: &#43E97B" + (data.getSkinName().isEmpty() ? "Steve" : data.getSkinName()),
                "&7ออร่าพาร์ติเคิล: &#7366FF" + data.getParticleAura(),
                "&7ระบบโต้ตอบ: &#FFD166" + data.getCommands().size() + " Actions",
                "",
                "&#FF6CAB[คลิกซ้าย] &fพิมพ์เปลี่ยนชื่อใหม่ในแชท",
                "&#FF5858[คลิกขวา] &7ซ่อน/ลบชื่อบนหัวออกทันที"));

        // Slot 19: Wardrobe & Gear
        inv.setItem(19, createItem(Material.NETHERITE_SWORD,
                "&#FF6CAB⚔ &#FFD166คลังอาวุธ & เครื่องแต่งกาย",
                "&7สวมใส่อาวุธมือหลัก, โล่มือรอง, และชุดเกราะ",
                "&7หรือคัดลอกชุดจากตัวคุณใส่หุ่นได้ในคลิกเดียว",
                "",
                "&#43E97B▸ คลิกเพื่อเปิดคลังแต่งกาย"));

        // Slot 20: Stance & Motion
        inv.setItem(20, createItem(Material.ARMOR_STAND,
                "&#FF6CAB🎭 &#FFD166ท่วงท่า & ลีลา (Stance)",
                "&7ปรับท่าทางของหุ่น (ยืน, ย่อป้องกัน, นั่ง ฯลฯ)",
                "&7ท่าปัจจุบัน: &#FFD166" + data.getPose().getPresetName(),
                "",
                "&#43E97B▸ คลิกเพื่อเลือกท่วงท่า"));

        // Slot 21: Skin & Identity
        inv.setItem(21, createItem(Material.PLAYER_HEAD,
                "&#FF6CAB🎨 &#FFD166โมเดลสกิน 3 มิติ (3D Skin & URL)",
                "&7เปลี่ยนสกินผู้เล่น 3 มิติจากชื่อผู้เล่น หรือลิงก์ URL (.png)",
                "&7สกินปัจจุบัน: &#43E97B" + (data.getSkinName().isEmpty() ? "Steve" : data.getSkinName()),
                "",
                "&#FF6CAB[คลิกซ้าย] &fพิมพ์ชื่อสกิน หรือวางลิงก์ URL สกินในแชท",
                "&#FFD166[คลิกขวา] &eใช้สกินของคุณเองทันที",
                "&#A8EDEA[Shift+คลิก] &7รีเซ็ตเป็นสกิน Steve"));

        // Slot 22: Floating Banner (Hologram Studio - Line by Line Editor)
        inv.setItem(22, createItem(Material.END_ROD,
                "&#FF6CAB✨ &#FFD166ป้ายข้อความลอยฟ้า (Hologram Studio)",
                "&7จัดการและแก้ไขข้อความลอยเหนือหัวหุ่นทีละบรรทัด",
                "&7รองรับ PlaceholderAPI อัปเดตแบบ Real-Time!",
                "&7จำนวนบรรทัดเสริม: &#43E97B" + data.getHologramLines().size() + " บรรทัด",
                "",
                "&#43E97B[คลิกซ้าย] &aเปิดหน้าต่างแก้ไข Hologram ทีละบรรทัด",
                "&#FF6CAB[คลิกขวา] &fพิมพ์เพิ่มบรรทัดใหม่ด่วนในแชท",
                "&#FF5858[Shift+คลิก] &cล้างข้อความเสริมทั้งหมด"));

        // Slot 23: Action & Trigger Studio
        inv.setItem(23, createItem(Material.AMETHYST_CLUSTER,
                "&#FF6CAB⚡ &#FFD166ระบบโต้ตอบอัจฉริยะ (Action Studio)",
                "&7กำหนดเหตุการณ์เมื่อผู้เล่นคลิกที่หุ่นจำลอง",
                "&7รองรับ Console, Player, OP, ย้ายเซิร์ฟ, ข้อความ, Title",
                "&7รายการที่ตั้งไว้: &#43E97B" + data.getCommands().size() + " Actions",
                "",
                "&#43E97B▸ คลิกเพื่อเปิด Action Studio"));

        // Slot 24: Particle Aura Ring
        inv.setItem(24, createItem(Material.BLAZE_POWDER,
                "&#FF6CAB🔮 &#FFD166วงแหวนพาร์ติเคิล (Particle Aura)",
                "&7สร้างเอฟเฟกต์ออร่าหมุนรอบตัวหุ่นจำลอง",
                "&7ออร่าปัจจุบัน: &#43E97B" + data.getParticleAura(),
                "&8(NONE, FLAME, SOUL, ENCHANT, CHERRY, END, HEART, TOTEM)",
                "",
                "&#43E97B▸ คลิกเพื่อสลับรูปแบบออร่าทันที"));

        // Slot 25: Body Scale & Glowing
        String glowStr = data.isGlowing() ? "&#43E97Bเปิดเรืองแสง (ON)" : "&#FF5858ปิดเรืองแสง (OFF)";
        inv.setItem(25, createItem(Material.GLOW_INK_SAC,
                "&#FF6CAB🌟 &#FFD166ขนาดร่างกาย & แสงออร่า (Scale & Glow)",
                "&7ปรับขนาดตัวหุ่นจำลองและขอบเรืองแสง",
                "&7ขนาดปัจจุบัน: &#FFD166" + data.getScale() + "x",
                "&7ขอบเรืองแสง: " + glowStr,
                "",
                "&#FF6CAB[คลิกซ้าย] &fสลับขนาดตัว (0.7x / 1.0x / 1.3x / 1.6x / 2.0x)",
                "&#FFD166[คลิกขวา] &eสลับเปิด/ปิดขอบเรืองแสง"));

        // Slot 29: Entity Archetype
        inv.setItem(29, createItem(Material.TOTEM_OF_UNDYING,
                "&#FF6CAB🧬 &#FFD166เปลี่ยนเผ่าพันธุ์หุ่น (Archetype)",
                "&7สลับรูปร่างระหว่างผู้เล่น 3 มิติ และสิ่งมีชีวิตอื่น",
                "&7เผ่าพันธุ์ปัจจุบัน: &#43E97B" + data.getType(),
                "",
                "&#43E97B▸ คลิกเพื่อสลับเผ่าพันธุ์ (PLAYER / VILLAGER / ALLAY / GOLEM)"));

        // Slot 31: Auto-Gaze (Look at Player)
        String lookStatus = data.isLookAtPlayer() ? "&#43E97B✔ เปิดใช้งาน (ON)" : "&#FF5858✖ ปิดใช้งาน (OFF)";
        inv.setItem(31, createItem(Material.ENDER_EYE,
                "&#FF6CAB👁 &#FFD166ระบบสายตามองตาม (Auto-Gaze)",
                "&7หุ่นจำลองจะหันศีรษะและลำตัวมองผู้เล่นที่อยู่ใกล้",
                "&7สถานะ: " + lookStatus,
                "",
                "&#43E97B▸ คลิกเพื่อสลับเปิด/ปิด"));

        // Slot 33: Warp, Summon & Clone
        inv.setItem(33, createItem(Material.RECOVERY_COMPASS,
                "&#FF6CAB📍 &#FFD166เคลื่อนย้าย & คัดลอกหุ่น",
                "&7จัดการตำแหน่งพิกัดหรือโคลนหุ่นตัวใหม่",
                "",
                "&#FF6CAB[คลิกซ้าย] &fวาร์ปไปหาหุ่นตัวนี้",
                "&#FFD166[คลิกขวา] &eดึงหุ่นตัวนี้มายืนจุดที่คุณอยู่",
                "&#43E97B[Shift+คลิก] &aโคลนหุ่นตัวนี้เพิ่มอีก 1 ตัว"));

        // Slot 40: Close
        inv.setItem(40, createItem(Material.BARRIER, "&#FF5858✖ &cปิดหน้าต่าง MeryNPC Studio"));

        // Slot 44: Delete NPC
        inv.setItem(44, createItem(Material.LAVA_BUCKET,
                "&#FF5858🗑 &cลบทิ้งถาวร",
                "&7ลบหุ่นจำลองตัวนี้ออกจากเซิร์ฟเวอร์",
                "",
                "&#FF5858▸ กด Shift + คลิก เพื่อยืนยันการลบ"));

        player.openInventory(inv);
    }

    public static ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize(name));
            if (lore != null && lore.length > 0) {
                List<String> list = new ArrayList<>();
                for (String l : lore) {
                    list.add(ColorUtil.colorize(l));
                }
                meta.setLore(list);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
