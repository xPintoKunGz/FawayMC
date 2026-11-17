package com.imagery.gui;

import com.imagery.ImageryPlugin;
import com.imagery.model.ImageAlbum;
import com.imagery.util.ColorUtil;
import com.imagery.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Views individual pages of a selected album and allows placement.
 */
public class AlbumViewGui {

    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    public static void open(ImageryPlugin plugin, Player player, ImageAlbum album) {
        String title = ColorUtil.color("&#00f2feอัลบั้ม &#7f8c8d| &#ffffff" + album.getTitle());
        ImageryHolder holder = new ImageryHolder(ImageryHolder.GuiType.ALBUM_VIEW, album);
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        GuiUtil.fillBorders(inv);

        // Header (Slot 4)
        ItemStack header = GuiUtil.createItem(
                Material.BOOKSHELF,
                "&#00f2fe📚 &#4facfe&l" + album.getTitle(),
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1รหัสอัลบั้ม: &f" + album.getId(),
                "&#00f5a0จำนวนหน้า: &f" + album.getPages().size() + " หน้า",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );
        inv.setItem(4, header);

        List<String> pages = album.getPages();
        for (int i = 0; i < Math.min(pages.size(), SLOTS.length); i++) {
            String pageFile = pages.get(i);
            String display = pageFile.contains("/") ? pageFile.substring(pageFile.lastIndexOf('/') + 1) : pageFile;

            ItemStack pageItem = GuiUtil.createItem(
                    Material.MAP,
                    "&#00f5a0หน้า " + (i + 1) + ": &#ffffff" + display,
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    "&7ไฟล์: &f" + pageFile,
                    "&7ลำดับที่: &e" + (i + 1) + " ในเซ็ตภาพ",
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━"
            );
            inv.setItem(SLOTS[i], pageItem);
        }

        // Place Album Button (Slot 31)
        inv.setItem(31, GuiUtil.createItem(
                Material.EMERALD_BLOCK,
                "&#00f5a0✔ วางอัลบั้มนี้ลงบนผนัง &7(Place Album)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&fวางเซ็ตภาพนี้ลงบนผนังตรงหน้าคุณ",
                "&fเมื่อผู้เล่นคลิกที่รูปภาพในโลก จะเปลี่ยนหน้า 1 > 2 > 3... ทันที!",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#00f5a0▶ คลิกเพื่อวางอัลบั้ม"
        ));

        // Back to Album List (Slot 36)
        inv.setItem(36, GuiUtil.createItem(
                Material.ARROW,
                "&#f6d365◀ กลับสู่หน้ารายการอัลบั้ม",
                "&7ย้อนกลับ"
        ));

        player.openInventory(inv);
        SoundUtil.playClick(player);
    }
}

