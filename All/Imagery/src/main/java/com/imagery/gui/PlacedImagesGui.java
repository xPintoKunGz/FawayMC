package com.imagery.gui;

import com.imagery.ImageryPlugin;
import com.imagery.model.PlacedImage;
import com.imagery.util.ColorUtil;
import com.imagery.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Placed Images Manager GUI showing live world coordinates, distances,
 * teleports, particle visualizers, live resize, and deletion controls.
 */
public class PlacedImagesGui {

    private static final int PAGE_SIZE = 21;
    private static final int[] SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    };

    public static void open(ImageryPlugin plugin, Player player, int page) {
        List<PlacedImage> all = new ArrayList<>(plugin.getPlacedImageManager().getAllPlacedImages());
        int totalPages = Math.max(1, (int) Math.ceil((double) all.size() / PAGE_SIZE));
        page = Math.max(0, Math.min(page, totalPages - 1));

        String title = ColorUtil.color("&#00f5a0ตำแหน่งรูปภาพในโลก &#7f8c8d| &#ffffffหน้า " + (page + 1) + "/" + totalPages);
        ImageryHolder holder = new ImageryHolder(ImageryHolder.GuiType.PLACED_MANAGER);
        holder.setPage(page);

        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        GuiUtil.fillBorders(inv);

        int startIndex = page * PAGE_SIZE;
        int endIndex = Math.min(startIndex + PAGE_SIZE, all.size());

        Location playerLoc = player.getLocation();

        for (int i = startIndex; i < endIndex; i++) {
            PlacedImage placed = all.get(i);
            int slot = SLOTS[i - startIndex];

            String distanceStr = "ต่างมิติ (Different World)";
            if (playerLoc.getWorld() != null && playerLoc.getWorld().getName().equals(placed.getWorldName())) {
                Location origin = placed.getOriginLocation();
                if (origin != null) {
                    double dist = playerLoc.distance(origin);
                    distanceStr = String.format("%.1f บล็อก", dist);
                }
            }

            int current = placed.getCurrentPage() + 1;
            int totalPagesCount = Math.max(1, placed.getPages().size());
            String albumInfo = placed.getAlbumId() != null ? " &#7f8c8d(อัลบั้ม: &e" + placed.getAlbumId() + "&7)" : "";

            ItemStack item = GuiUtil.createItem(
                    Material.FILLED_MAP,
                    "&#00f2fe📍 &#4facfe&l" + placed.getId() + " &7(&f" + placed.getCurrentFileName() + "&7)" + albumInfo,
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    "&#00f5a0❖ พิกัดตำแหน่งที่ตั้ง (Coordinates):",
                    "  &#a18cd1โลก: &f" + placed.getWorldName(),
                    "  &#00f2feX: &f" + placed.getOriginX() + " &7| &#00f2feY: &f" + placed.getOriginY() + " &7| &#00f2feZ: &f" + placed.getOriginZ(),
                    "  &#f6d365ทิศทาง: &f" + placed.getFacing().name() + " &7| &#a18cd1ระยะห่าง: &f" + distanceStr,
                    " ",
                    "&#00f5a0❖ ข้อมูลรูปภาพ & สไลด์:",
                    "  &#a18cd1ขนาด: &f" + placed.getWidth() + "x" + placed.getHeight() + " บล็อก (" + (placed.getWidth() * placed.getHeight()) + " เฟรม)",
                    "  &#a18cd1หน้าปัจจุบัน: &#00f2fe" + current + "/" + totalPagesCount + " หน้า",
                    "  &#a18cd1คำสั่งในภาพ: &#f6d365" + placed.getClickCommands().size() + " คำสั่ง",
                    "  &#a18cd1กรอบล่องหน: " + (placed.isInvisibleFrame() ? "&aเปิด (ไร้ขอบ)" : "&eปิด (มีขอบไม้)"),
                    "  &#a18cd1เรืองแสง: " + (placed.isGlowing() ? "&aเปิด" : "&7ปิด"),
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    "&#00f2fe[คลิกซ้าย] &fวาร์ปไปหารูปภาพ (Teleport)",
                    "&#00f5a0[คลิกขวา] &fเปิดลำแสงส่องตำแหน่ง (Highlight 12s)",
                    "&#f6d365[Shift + คลิกซ้าย] &fปรับขนาดรูปภาพใหม่ (Resize)",
                    "&#a18cd1[Shift + คลิกขวา] &fตั้งค่าคำสั่ง & สไลด์ (Commands & Slides)",
                    "&#ff0844[กดปุ่ม Q (Drop)] &fลบรูปภาพนี้ออกจากโลก (Delete)"
            );
            inv.setItem(slot, item);
        }

        if (all.isEmpty()) {
            ItemStack emptyItem = GuiUtil.createItem(
                    Material.BARRIER,
                    "&#ff0844✖ ยังไม่มีรูปภาพที่ถูกวางในโลก",
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                    "&fคุณยังไม่ได้วางรูปภาพใดๆ ลงในเซิร์ฟเวอร์",
                    "&fสามารถไปที่ &#00f2feคลังรูปภาพ &fเพื่อเลือกวางภาพแรกได้ทันที!",
                    "&7━━━━━━━━━━━━━━━━━━━━━━━━━━"
            );
            inv.setItem(22, emptyItem);
        }

        // Navigation
        inv.setItem(36, GuiUtil.createItem(
                Material.ARROW,
                "&#f6d365◀ กลับสู่เมนูหลัก",
                "&7ย้อนกลับไปหน้าหลัก"
        ));

        if (page > 0) {
            inv.setItem(38, GuiUtil.createItem(
                    Material.SPECTRAL_ARROW,
                    "&#00f2fe◀ หน้าก่อนหน้า",
                    "&7ไปยังหน้า " + page
            ));
        }

        inv.setItem(40, GuiUtil.createItem(
                Material.BEACON,
                "&#00f5a0✨ ระบบค้นหาพิกัด Style 26.3",
                "&7คลิกขวาที่รูปใดก็ได้เพื่อยิงลำแสงอนุภาค",
                "&7ช่วยให้มองเห็นตำแหน่งรูปภาพผ่านบล็อกได้ง่ายดาย!"
        ));

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

