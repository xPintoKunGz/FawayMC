package com.imagery.gui;

import com.imagery.ImageryPlugin;
import com.imagery.model.PlacedImage;
import com.imagery.util.ColorUtil;
import com.imagery.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Live resize and property editor for an existing placed image in the world.
 */
public class ResizeEditorGui {

    public static class ResizeContext {
        private final PlacedImage placedImage;
        private int targetWidth;
        private int targetHeight;
        private boolean invisible;
        private boolean glowing;

        public ResizeContext(PlacedImage placedImage) {
            this.placedImage = placedImage;
            this.targetWidth = placedImage.getWidth();
            this.targetHeight = placedImage.getHeight();
            this.invisible = placedImage.isInvisibleFrame();
            this.glowing = placedImage.isGlowing();
        }

        public PlacedImage getPlacedImage() {
            return placedImage;
        }

        public int getTargetWidth() {
            return targetWidth;
        }

        public void setTargetWidth(int targetWidth) {
            this.targetWidth = Math.max(1, Math.min(24, targetWidth));
        }

        public int getTargetHeight() {
            return targetHeight;
        }

        public void setTargetHeight(int targetHeight) {
            this.targetHeight = Math.max(1, Math.min(24, targetHeight));
        }

        public boolean isInvisible() {
            return invisible;
        }

        public void toggleInvisible() {
            this.invisible = !this.invisible;
        }

        public boolean isGlowing() {
            return glowing;
        }

        public void toggleGlowing() {
            this.glowing = !this.glowing;
        }
    }

    public static void open(ImageryPlugin plugin, Player player, ResizeContext ctx) {
        PlacedImage img = ctx.getPlacedImage();
        String title = ColorUtil.color("&#00f2feปรับขนาดรูป &#7f8c8d| &#ffffff" + img.getId());

        ImageryHolder holder = new ImageryHolder(ImageryHolder.GuiType.RESIZE_EDITOR, ctx);
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        GuiUtil.fillBorders(inv);

        // Header (Slot 4)
        ItemStack header = GuiUtil.createItem(
                Material.FILLED_MAP,
                "&#00f2fe📍 &#4facfe&l" + img.getId() + " &7(&f" + img.getFileName() + "&7)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1พิกัด: &f" + img.getWorldName() + " [X: " + img.getOriginX() + ", Y: " + img.getOriginY() + ", Z: " + img.getOriginZ() + "]",
                "&#a18cd1ขนาดเดิม: &f" + img.getWidth() + "x" + img.getHeight() + " บล็อก",
                "&#00f5a0ขนาดใหม่ที่จะปรับ: &f" + ctx.getTargetWidth() + "x" + ctx.getTargetHeight() + " บล็อก",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );
        inv.setItem(4, header);

        // Width Controls: 19, 20, 21
        inv.setItem(19, GuiUtil.createItem(
                Material.RED_CONCRETE,
                "&#ff0844[-] ลดความกว้าง 1 บล็อก",
                "&7ความกว้าง: &f" + ctx.getTargetWidth() + " บล็อก"
        ));

        inv.setItem(20, GuiUtil.createItem(
                Material.CYAN_CONCRETE,
                "&#00f2feความกว้าง: &#ffffff" + ctx.getTargetWidth() + " บล็อก",
                "&7จำนวนบล็อกตามแนวนอน"
        ));

        inv.setItem(21, GuiUtil.createItem(
                Material.LIME_CONCRETE,
                "&#00f5a0[+] เพิ่มความกว้าง 1 บล็อก",
                "&7ความกว้าง: &f" + ctx.getTargetWidth() + " บล็อก"
        ));

        // Height Controls: 23, 24, 25
        inv.setItem(23, GuiUtil.createItem(
                Material.RED_CONCRETE,
                "&#ff0844[-] ลดความสูง 1 บล็อก",
                "&7ความสูง: &f" + ctx.getTargetHeight() + " บล็อก"
        ));

        inv.setItem(24, GuiUtil.createItem(
                Material.CYAN_CONCRETE,
                "&#00f2feความสูง: &#ffffff" + ctx.getTargetHeight() + " บล็อก",
                "&7จำนวนบล็อกตามแนวตั้ง"
        ));

        inv.setItem(25, GuiUtil.createItem(
                Material.LIME_CONCRETE,
                "&#00f5a0[+] เพิ่มความสูง 1 บล็อก",
                "&7ความสูงปัจจุบัน: &f" + ctx.getTargetHeight() + " บล็อก"
        ));

        // Invisible toggle (Slot 29)
        inv.setItem(29, GuiUtil.createItem(
                Material.ITEM_FRAME,
                "&#a18cd1👻 กรอบล่องหน",
                "&7สถานะ: " + (ctx.isInvisible() ? "&aล่องหน (ไร้ขอบ)" : "&eมีขอบไม้"),
                "&7คลิกเพื่อสลับ"
        ));

        // Glow toggle (Slot 33)
        inv.setItem(33, GuiUtil.createItem(
                Material.GLOW_INK_SAC,
                "&#f6d365💡 เรืองแสง",
                "&7สถานะ: " + (ctx.isGlowing() ? "&aเปิด" : "&7ปิด"),
                "&7คลิกเพื่อสลับ"
        ));

        // Confirm Live Apply (Slot 31)
        inv.setItem(31, GuiUtil.createItem(
                Material.EMERALD_BLOCK,
                "&#00f5a0✔ บันทึกและปรับขนาดทันที &7(Apply Changes)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&fระบบจะคำนวณและปรับเปลี่ยนขนาดรูปภาพในโลกจริงทันที",
                "&fขนาดใหม่: &#00f2fe" + ctx.getTargetWidth() + "x" + ctx.getTargetHeight() + " บล็อก",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#00f5a0▶ คลิกเพื่อปรับขนาด"
        ));

        // Back to Placed Manager (Slot 36)
        inv.setItem(36, GuiUtil.createItem(
                Material.ARROW,
                "&#f6d365◀ กลับสู่หน้ารายการ",
                "&7ยกเลิกและย้อนกลับ"
        ));

        player.openInventory(inv);
        SoundUtil.playClick(player);
    }
}

