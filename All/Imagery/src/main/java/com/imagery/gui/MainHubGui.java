package com.imagery.gui;

import com.imagery.ImageryPlugin;
import com.imagery.util.ColorUtil;
import com.imagery.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Main Hub GUI for the Imagery plugin.
 */
public class MainHubGui {

    public static void open(ImageryPlugin plugin, Player player) {
        String title = ColorUtil.color("&#00f2fe❖ &#4facfe&lImagery &#7f8c8d| &#ffffffศูนย์ควบคุมหลัก");
        ImageryHolder holder = new ImageryHolder(ImageryHolder.GuiType.MAIN_HUB);
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        GuiUtil.fillBorders(inv);

        int totalImages = plugin.getImageFileManager().getAllMetadata().size();
        int totalAlbums = plugin.getAlbumManager().getAllAlbums().size();
        int totalPlaced = plugin.getPlacedImageManager().getAllPlacedImages().size();

        // Slot 19: Gallery & Place
        ItemStack galleryItem = GuiUtil.createItem(
                Material.PAINTING,
                "&#00f2fe🖼 &#4facfe&lคลังรูปภาพเดี่ยว &7(Gallery)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1คำอธิบาย:",
                "  &fดูรายการไฟล์รูปภาพทั้งหมดในโฟลเดอร์",
                "  &fเลือกรูปเพื่อปรับแต่งขนาดและวางลงในโลก",
                " ",
                "&#00f5a0❖ จำนวนรูปพร้อมใช้งาน: &#ffffff" + totalImages + " ไฟล์",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#00f2fe▶ คลิกเพื่อเปิดคลังรูปภาพ"
        );
        inv.setItem(19, galleryItem);

        // Slot 21: Album Manager
        ItemStack albumItem = GuiUtil.createItem(
                Material.WRITABLE_BOOK,
                "&#00f5a0📚 &#38ef7d&lอัลบั้ม & เซ็ตภาพ &7(Albums)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1คำอธิบาย:",
                "  &fรวมกลุ่มรูปภาพเป็นเซ็ตหลายหน้า &7(หน้า 1, 2, 3...)",
                "  &fเมื่อวางในโลก ผู้เล่นสามารถคลิกเปลี่ยนหน้าได้!",
                "  &fรองรับระบบสไลด์โชว์หมุนเปลี่ยนภาพอัตโนมัติ",
                " ",
                "&#00f5a0❖ อัลบั้มพร้อมใช้งาน: &#ffffff" + totalAlbums + " อัลบั้ม",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#00f5a0▶ คลิกเพื่อเปิดจัดการอัลบั้ม"
        );
        inv.setItem(21, albumItem);

        // Slot 23: Placed Images Manager
        ItemStack managerItem = GuiUtil.createItem(
                Material.FILLED_MAP,
                "&#a18cd1📍 &#fbc2eb&lจัดการรูปภาพที่ตั้งอยู่ &7(Placed Manager)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1คำอธิบาย:",
                "  &fตรวจสอบรูปภาพทั้งหมดที่ตั้งอยู่ในโลก",
                "  &fดูพิกัด &7(X, Y, Z)&f, วาร์ปไปหารูป, หรือเปิดลำแสง",
                "  &fปรับขนาดใหม่, ติดตั้งคำสั่งในภาพ, และลบรูป",
                " ",
                "&#00f5a0❖ รูปที่ตั้งอยู่ในโลก: &#ffffff" + totalPlaced + " จุด",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1▶ คลิกเพื่อเปิดหน้าจัดการตำแหน่ง"
        );
        inv.setItem(23, managerItem);

        // Slot 25: Reload Folder
        ItemStack reloadItem = GuiUtil.createItem(
                Material.SUNFLOWER,
                "&#f6d365🔄 &#fda085&lโหลดไฟล์รูปภาพใหม่ &7(Reload)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1คำอธิบาย:",
                "  &fสแกนโฟลเดอร์ &eimages/ &fและ &ealbums/ &fใหม่",
                "  &fเมื่อใส่ไฟล์ภาพใหม่ ให้กดปุ่มนี้เพื่ออัปเดต",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#f6d365▶ คลิกเพื่อรีโหลดไฟล์"
        );
        inv.setItem(25, reloadItem);

        // Slot 40: Info & Theme indicator
        ItemStack infoItem = GuiUtil.createItem(
                Material.BEACON,
                "&#00f2fe⚡ &#4facfe&lImagery System 26.3",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&7เวอร์ชันระบบ: &#00f2fe26.3 Modern Engine",
                "&7การทำงาน: &#00f5a0Vanilla Maps & Click Actions",
                "&7ระบบคลิกเปลี่ยนหน้า: &#00f2feเปิดใช้งาน (Multi-Page / Slideshow)",
                "&7ระบบคำสั่งในภาพ: &#00f5a0เปิดใช้งาน (Player & Console)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );
        inv.setItem(40, infoItem);

        player.openInventory(inv);
        SoundUtil.playClick(player);
    }
}
