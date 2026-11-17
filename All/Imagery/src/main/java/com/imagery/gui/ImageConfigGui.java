package com.imagery.gui;

import com.imagery.ImageryPlugin;
import com.imagery.model.ImageMetadata;
import com.imagery.util.ColorUtil;
import com.imagery.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Interactive GUI to configure dimensions, ratios, frame styles, and placement for a selected image.
 */
public class ImageConfigGui {

    public static void open(ImageryPlugin plugin, Player player, ImageConfigContext context) {
        ImageMetadata meta = context.getMetadata();
        String title = ColorUtil.color("&#00f2feปรับแต่งรูป &#7f8c8d| &#ffffff" + meta.getFileName());

        ImageryHolder holder = new ImageryHolder(ImageryHolder.GuiType.IMAGE_CONFIG, context);
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        GuiUtil.fillBorders(inv);

        // Slot 4: Image Info Header
        ItemStack header = GuiUtil.createItem(
                Material.PAINTING,
                "&#00f2fe❖ &#ffffff" + meta.getFileName(),
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1ขนาดภาพต้นฉบับ: &f" + meta.getPixelWidth() + "x" + meta.getPixelHeight() + " พิกเซล",
                "&#a18cd1ขนาดไฟล์: &f" + meta.getFormattedSize(),
                "&#00f5a0ขนาดบล็อกปัจจุบัน: &f" + context.getWidth() + "x" + context.getHeight() + " บล็อก",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );
        inv.setItem(4, header);

        // Slot 13: Lock Aspect Ratio Toggle
        String lockStatus = context.isLockRatio() ? "&#00f5a0✔ เปิดใช้งาน (คำนวณสัดส่วนอัตโนมัติ)" : "&#ff0844✖ ปิด (ปรับอิสระ)";
        Material lockMat = context.isLockRatio() ? Material.LIME_DYE : Material.GRAY_DYE;
        ItemStack lockBtn = GuiUtil.createItem(
                lockMat,
                "&#f6d365🔒 ล็อคสัดส่วนภาพ (Aspect Ratio)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&7สถานะ: " + lockStatus,
                "&7เมื่อเปิด: ปรับความกว้างจะเปลี่ยนความสูงตามสัดส่วนภาพจริง",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#f6d365▶ คลิกเพื่อสลับเปิด/ปิด"
        );
        inv.setItem(13, lockBtn);

        // Width Controls: Slots 19, 20, 21
        inv.setItem(19, GuiUtil.createItem(
                Material.RED_CONCRETE,
                "&#ff0844[-] ลดความกว้าง 1 บล็อก",
                "&7ความกว้างปัจจุบัน: &f" + context.getWidth() + " บล็อก"
        ));

        inv.setItem(20, GuiUtil.createItem(
                Material.CYAN_CONCRETE,
                "&#00f2feความกว้าง (Width): &#ffffff" + context.getWidth() + " บล็อก",
                "&7จำนวนบล็อกตามแนวนอน"
        ));

        inv.setItem(21, GuiUtil.createItem(
                Material.LIME_CONCRETE,
                "&#00f5a0[+] เพิ่มความกว้าง 1 บล็อก",
                "&7ความกว้างปัจจุบัน: &f" + context.getWidth() + " บล็อก"
        ));

        // Height Controls: Slots 23, 24, 25
        inv.setItem(23, GuiUtil.createItem(
                Material.RED_CONCRETE,
                "&#ff0844[-] ลดความสูง 1 บล็อก",
                "&7ความสูงปัจจุบัน: &f" + context.getHeight() + " บล็อก"
        ));

        inv.setItem(24, GuiUtil.createItem(
                Material.CYAN_CONCRETE,
                "&#00f2feความสูง (Height): &#ffffff" + context.getHeight() + " บล็อก",
                "&7จำนวนบล็อกตามแนวตั้ง"
        ));

        inv.setItem(25, GuiUtil.createItem(
                Material.LIME_CONCRETE,
                "&#00f5a0[+] เพิ่มความสูง 1 บล็อก",
                "&7ความสูงปัจจุบัน: &f" + context.getHeight() + " บล็อก"
        ));

        // Invisible Frame Toggle (Slot 29)
        String invisStatus = context.isInvisible() ? "&#00f5a0✔ ล่องหน (ไร้ขอบไม้)" : "&#ffb199✖ มีขอบไม้ธรรมดา";
        inv.setItem(29, GuiUtil.createItem(
                Material.ITEM_FRAME,
                "&#a18cd1👻 รูปแบบกรอบรูป (Frame Style)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&7สถานะ: " + invisStatus,
                "&7ทำให้รูปภาพดูแนบเนียนกับผนังเหมือนโปสเตอร์จริง",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1▶ คลิกเพื่อสลับโหมด"
        ));

        // Glow Toggle (Slot 33)
        String glowStatus = context.isGlowing() ? "&#00f5a0✔ เปิดเรืองแสง" : "&#95a5a6✖ ปิด";
        inv.setItem(33, GuiUtil.createItem(
                Material.GLOW_INK_SAC,
                "&#f6d365💡 เรืองแสงในที่มืด (Glow Effect)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&7สถานะ: " + glowStatus,
                "&7ช่วยให้มองเห็นภาพได้สว่างแม้ในที่มืด",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#f6d365▶ คลิกเพื่อสลับโหมด"
        ));

        // Confirm & Place Button (Slot 31)
        inv.setItem(31, GuiUtil.createItem(
                Material.EMERALD_BLOCK,
                "&#00f5a0✔ ยืนยันและวางภาพลงบนผนัง &7(Place Image)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1สรุปการตั้งค่า:",
                "  &fขนาด: &#00f2fe" + context.getWidth() + "x" + context.getHeight() + " บล็อก",
                "  &fกรอบ: " + (context.isInvisible() ? "&aล่องหน (ไร้ขอบ)" : "&eมีขอบไม้"),
                "  &fเรืองแสง: " + (context.isGlowing() ? "&aเปิด" : "&7ปิด"),
                " ",
                "&eคำแนะนำ: หันหน้ามองไปที่ผนัง/บล็อกที่ต้องการวาง",
                "&eแล้วคลิกปุ่มนี้ รูปภาพจะถูกสร้างขึ้นทันที!",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#00f5a0▶ คลิกเพื่อวางรูปภาพเดี๋ยวนี้"
        ));

        // Back to Gallery (Slot 36)
        inv.setItem(36, GuiUtil.createItem(
                Material.ARROW,
                "&#f6d365◀ กลับสู่คลังรูปภาพ",
                "&7ย้อนกลับไปเลือกไฟล์อื่น"
        ));

        player.openInventory(inv);
        SoundUtil.playClick(player);
    }
}

