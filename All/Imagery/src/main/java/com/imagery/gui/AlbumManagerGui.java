package com.imagery.gui;

import com.imagery.ImageryPlugin;
import com.imagery.model.ImageAlbum;
import com.imagery.model.ImageMetadata;
import com.imagery.model.PlacedImage;
import com.imagery.util.ColorUtil;
import com.imagery.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Album Manager GUI to view, place, and manage multi-page album collections.
 */
public class AlbumManagerGui {

    private static final int PAGE_SIZE = 21;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static void open(ImageryPlugin plugin, Player player, int page) {
        List<ImageAlbum> all = new ArrayList<>(plugin.getAlbumManager().getAllAlbums());
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = ColorUtil.color("&#00f2feจัดการอัลบั้มภาพ &#7f8c8d| &#ffffffหน้า " + (page + 1) + "/" + totalPages);
        ImageryHolder holder = new ImageryHolder(ImageryHolder.GuiType.ALBUM_MANAGER);
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        GuiUtil.fillBorders(inv);

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, all.size());

        for (int i = startIndex; i < endIndex; i++) {
            ImageAlbum album = all.get(i);
            int slot = SLOTS[i - startIndex];

            List<String> lore = new ArrayList<>();
            lore.add("&7━━━━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("&#00f5a0❖ ข้อมูลอัลบั้ม:");
            lore.add("  &#a18cd1รหัส: &f" + album.getId());
            lore.add("  &#a18cd1จำนวนหน้า: &f" + album.getPages().size() + " หน้า");
            lore.add("  &#f6d365สไลด์โชว์: &f" + (album.getAutoRotateSeconds() > 0 ? "ทุกๆ " + album.getAutoRotateSeconds() + " วินาที" : "คลิกเพื่อเปลี่ยนหน้า"));
            lore.add("&7━━━━━━━━━━━━━━━━━━━━━━━━━━");
            lore.add("&#00f2fe[คลิกซ้าย] &fดูหน้าภาพทั้งหมดในอัลบั้ม");
            lore.add("&#00f5a0[คลิกขวา] &fวางอัลบั้มลงบนผนังตรงหน้า");

            ItemStack item = GuiUtil.createItem(
                    Material.WRITABLE_BOOK,
                    "&#00f2fe📚 &#4facfe&l" + album.getTitle(),
                    lore
            );
            inv.setItem(slot, item);
        }

        if (all.isEmpty()) {
            ItemStack emptyItem = GuiUtil.createItem(
                    Material.BARRIER,
                    "&#ff0844✖ ไม่พบอัลบั้มภาพ",
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    "&fสร้างโฟลเดอร์ใน &#f6d365plugins/Imagery/images/albums/<ชื่อ>/",
                    "&fแล้วใส่รูปภาพลงไป รูปทั้งหมดจะกลายเป็นหน้าอัลบั้มทันที!",
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━"
            );
            inv.setItem(22, emptyItem);
        }

        // Back to Main Hub (Slot 36)
        inv.setItem(36, GuiUtil.createItem(
                Material.ARROW,
                "&#f6d365◀ กลับสู่เมนูหลัก",
                "&7ย้อนกลับ"
        ));

        // Reload Albums (Slot 40)
        inv.setItem(40, GuiUtil.createItem(
                Material.SUNFLOWER,
                "&#f6d365🔄 สแกนอัลบั้มใหม่",
                "&7รีเฟรชโฟลเดอร์ albums/"
        ));

        if (page > 0) {
            inv.setItem(38, GuiUtil.createItem(Material.SPECTRAL_ARROW, "&#00f2fe◀ หน้าก่อนหน้า", "&7ไปยังหน้า " + page));
        }

        if (page < totalPages - 1) {
            inv.setItem(42, GuiUtil.createItem(Material.SPECTRAL_ARROW, "&#00f2feหน้าถัดไป ▶", "&7ไปยังหน้า " + (page + 2)));
        }

        player.openInventory(inv);
        SoundUtil.playClick(player);
    }
}

