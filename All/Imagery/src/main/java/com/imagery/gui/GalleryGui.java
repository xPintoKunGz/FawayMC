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

import java.util.ArrayList;
import java.util.List;

/**
 * Image Gallery GUI displaying available files from plugins/Imagery/images/.
 */
public class GalleryGui {

    private static final int PAGE_SIZE = 21;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static void open(ImageryPlugin plugin, Player player, int page) {
        List<ImageMetadata> all = new ArrayList<>(plugin.getImageFileManager().getAllMetadata());
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = ColorUtil.color("&#00f2feคลังรูปภาพ &#7f8c8d| &#ffffffหน้า " + (page + 1) + "/" + totalPages);
        ImageryHolder holder = new ImageryHolder(ImageryHolder.GuiType.GALLERY);
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        GuiUtil.fillBorders(inv);

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, all.size());

        for (int i = startIndex; i < endIndex; i++) {
            ImageMetadata meta = all.get(i);
            int slot = SLOTS[i - startIndex];

            int[] suggested = meta.getSuggestedBlocks(6);
            ItemStack item = GuiUtil.createItem(
                    Material.MAP,
                    "&#00f2fe❖ &#ffffff" + meta.getFileName(),
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    "&#a18cd1📐 ความละเอียด: &f" + meta.getPixelWidth() + "x" + meta.getPixelHeight() + " พิกเซล",
                    "&#a18cd1💾 ขนาดไฟล์: &f" + meta.getFormattedSize(),
                    "&#00f5a0⚡ ขนาดแนะนำ: &f" + suggested[0] + "x" + suggested[1] + " บล็อก",
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    "&#00f2fe[คลิกซ้าย] &fปรับแต่งขนาด & วางรูป",
                    "&#00f5a0[คลิกขวา] &fวางทันทีตามขนาดแนะนำ"
            );
            inv.setItem(slot, item);
        }

        // Empty state notice
        if (all.isEmpty()) {
            ItemStack emptyItem = GuiUtil.createItem(
                    Material.BARRIER,
                    "&#ff0844✖ ไม่พบไฟล์รูปภาพในโฟลเดอร์",
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    "&fโฟลเดอร์: &#f6d365plugins/Imagery/images/",
                    "&fรองรับไฟล์: &e.png, .jpg, .jpeg, .bmp, .gif",
                    "&fใส่ไฟล์แล้วกดปุ่ม &#00f5a0รีโหลด &#fได้ทันที",
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━"
            );
            inv.setItem(22, emptyItem);
        }

        // Navigation buttons
        // Back to Main Hub (Slot 36)
        inv.setItem(36, GuiUtil.createItem(
                Material.ARROW,
                "&#f6d365◀ กลับสู่เมนูหลัก",
                "&7คลิกเพื่อย้อนกลับไปหน้าหลัก"
        ));

        // Previous Page (Slot 38)
        if (page > 0) {
            inv.setItem(38, GuiUtil.createItem(
                    Material.SPECTRAL_ARROW,
                    "&#00f2fe◀ หน้าก่อนหน้า",
                    "&7ไปยังหน้า " + page
            ));
        }

        // Reload Button (Slot 40)
        inv.setItem(40, GuiUtil.createItem(
                Material.SUNFLOWER,
                "&#f6d365🔄 สแกนไฟล์ใหม่",
                "&7รีเฟรชโฟลเดอร์รูปภาพ"
        ));

        // Next Page (Slot 42)
        if (page < totalPages - 1) {
            inv.setItem(42, GuiUtil.createItem(
                    Material.SPECTRAL_ARROW,
                    "&#00f2feหน้าถัดไป ▶",
                    "&7ไปยังหน้า " + (page + 2)
            ));
        }

        player.openInventory(inv);
        SoundUtil.playClick(player);
    }
}

