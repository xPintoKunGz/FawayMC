package com.imagery.gui;

import com.imagery.ImageryPlugin;
import com.imagery.model.ImageMetadata;
import com.imagery.model.PlacedImage;
import com.imagery.util.ColorUtil;
import com.imagery.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Event listener handling user interactions across all Imagery Style 26.3 GUIs.
 */
public class GuiListener implements Listener {

    private final ImageryPlugin plugin;

    public GuiListener(ImageryPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof ImageryHolder imageryHolder)) {
            return;
        }

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int rawSlot = event.getRawSlot();
        if (rawSlot < 0 || rawSlot >= event.getInventory().getSize()) {
            return;
        }

        ClickType clickType = event.getClick();

        switch (imageryHolder.getType()) {
            case MAIN_HUB -> handleMainHub(player, rawSlot);
            case GALLERY -> handleGallery(player, imageryHolder, rawSlot, clickType);
            case IMAGE_CONFIG -> handleImageConfig(player, imageryHolder, rawSlot);
            case PLACED_MANAGER -> handlePlacedManager(player, imageryHolder, rawSlot, clickType);
            case RESIZE_EDITOR -> handleResizeEditor(player, imageryHolder, rawSlot);
            case ALBUM_MANAGER -> handleAlbumManager(player, imageryHolder, rawSlot, clickType);
            case ALBUM_VIEW -> handleAlbumView(player, imageryHolder, rawSlot);
            case COMMAND_MANAGER -> handleCommandManager(player, imageryHolder, rawSlot, clickType);
        }
    }

    private void handleMainHub(Player player, int slot) {
        switch (slot) {
            case 19 -> GalleryGui.open(plugin, player, 0);
            case 21 -> AlbumManagerGui.open(plugin, player, 0);
            case 23 -> PlacedImagesGui.open(plugin, player, 0);
            case 25 -> {
                plugin.getImageFileManager().scanImages();
                plugin.getAlbumManager().loadAlbums();
                SoundUtil.playSuccess(player);
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aสแกนโฟลเดอร์รูปภาพและอัลบั้มสำเร็จ! &7(พบ &e" +
                        plugin.getImageFileManager().getAllMetadata().size() + " &7ไฟล์, &b" +
                        plugin.getAlbumManager().getAllAlbums().size() + " &7อัลบั้ม)"));
                MainHubGui.open(plugin, player);
            }
        }
    }

    private void handleGallery(Player player, ImageryHolder holder, int slot, ClickType clickType) {
        int page = holder.getPage();

        if (slot == 36) {
            MainHubGui.open(plugin, player);
            return;
        }
        if (slot == 38 && page > 0) {
            GalleryGui.open(plugin, player, page - 1);
            return;
        }
        if (slot == 40) {
            plugin.getImageFileManager().scanImages();
            SoundUtil.playSuccess(player);
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aรีเฟรชคลังรูปภาพเรียบร้อยแล้ว"));
            GalleryGui.open(plugin, player, page);
            return;
        }
        if (slot == 42) {
            GalleryGui.open(plugin, player, page + 1);
            return;
        }

        // Check if an image slot was clicked
        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int relativeIndex = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                relativeIndex = i;
                break;
            }
        }

        if (relativeIndex == -1) return;

        List<ImageMetadata> all = new ArrayList<>(plugin.getImageFileManager().getAllMetadata());
        int index = (page * 21) + relativeIndex;
        if (index >= all.size()) return;

        ImageMetadata meta = all.get(index);
        int[] suggested = meta.getSuggestedBlocks(6);

        if (clickType.isRightClick()) {
            // Quick place
            RayTraceResult ray = player.rayTraceBlocks(6.0);
            if (ray == null || ray.getHitBlock() == null || ray.getHitBlockFace() == null) {
                SoundUtil.playError(player);
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cกรุณาหันหน้ามองไปที่ผนัง/บล็อกในระยะ 6 บล็อก เพื่อวางภาพ"));
                return;
            }

            Block target = ray.getHitBlock();
            BlockFace face = ray.getHitBlockFace();
            PlacedImage placed = plugin.getPlacedImageManager().placeImage(
                    player, target, face, meta, suggested[0], suggested[1], true, false
            );

            if (placed != null) {
                player.closeInventory();
                SoundUtil.playPlace(player);
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aวางรูปภาพ &e" + meta.getFileName() + " &aสำเร็จ! &7(รหัส: &b" + placed.getId() + "&7)"));
                player.sendMessage(ColorUtil.color("  &#00f5a0📍 พิกัด: &f[X: " + placed.getOriginX() + ", Y: " + placed.getOriginY() + ", Z: " + placed.getOriginZ() + "]"));
            }
        } else {
            // Open full config GUI
            ImageConfigContext ctx = new ImageConfigContext(meta, suggested[0], suggested[1]);
            ImageConfigGui.open(plugin, player, ctx);
        }
    }

    private void handleImageConfig(Player player, ImageryHolder holder, int slot) {
        ImageConfigContext ctx = holder.getContext();
        if (ctx == null) return;

        switch (slot) {
            case 13 -> {
                ctx.toggleLockRatio();
                SoundUtil.playClick(player);
                ImageConfigGui.open(plugin, player, ctx);
            }
            case 19 -> {
                ctx.setWidth(ctx.getWidth() - 1);
                SoundUtil.playClick(player);
                ImageConfigGui.open(plugin, player, ctx);
            }
            case 21 -> {
                ctx.setWidth(ctx.getWidth() + 1);
                SoundUtil.playClick(player);
                ImageConfigGui.open(plugin, player, ctx);
            }
            case 23 -> {
                ctx.setHeight(ctx.getHeight() - 1);
                SoundUtil.playClick(player);
                ImageConfigGui.open(plugin, player, ctx);
            }
            case 25 -> {
                ctx.setHeight(ctx.getHeight() + 1);
                SoundUtil.playClick(player);
                ImageConfigGui.open(plugin, player, ctx);
            }
            case 29 -> {
                ctx.toggleInvisible();
                SoundUtil.playClick(player);
                ImageConfigGui.open(plugin, player, ctx);
            }
            case 33 -> {
                ctx.toggleGlowing();
                SoundUtil.playClick(player);
                ImageConfigGui.open(plugin, player, ctx);
            }
            case 36 -> GalleryGui.open(plugin, player, 0);
            case 31 -> {
                // Place on target wall
                RayTraceResult ray = player.rayTraceBlocks(6.0);
                if (ray == null || ray.getHitBlock() == null || ray.getHitBlockFace() == null) {
                    SoundUtil.playError(player);
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cกรุณาหันหน้ามองไปที่ผนัง/บล็อกในระยะ 6 บล็อก เพื่อวางภาพ"));
                    return;
                }

                Block target = ray.getHitBlock();
                BlockFace face = ray.getHitBlockFace();
                PlacedImage placed = plugin.getPlacedImageManager().placeImage(
                        player, target, face, ctx.getMetadata(), ctx.getWidth(), ctx.getHeight(), ctx.isInvisible(), ctx.isGlowing()
                );

                if (placed != null) {
                    player.closeInventory();
                    SoundUtil.playPlace(player);
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aวางรูปภาพ &e" + ctx.getMetadata().getFileName() +
                            " &aสำเร็จ! &7(ขนาด: &#00f2fe" + ctx.getWidth() + "x" + ctx.getHeight() + " บล็อก&7)"));
                    player.sendMessage(ColorUtil.color("  &#00f5a0📍 พิกัด: &f[X: " + placed.getOriginX() +
                            ", Y: " + placed.getOriginY() + ", Z: " + placed.getOriginZ() + "]"));
                }
            }
        }
    }

    private void handlePlacedManager(Player player, ImageryHolder holder, int slot, ClickType clickType) {
        int page = holder.getPage();

        if (slot == 36) {
            MainHubGui.open(plugin, player);
            return;
        }
        if (slot == 38 && page > 0) {
            PlacedImagesGui.open(plugin, player, page - 1);
            return;
        }
        if (slot == 42) {
            PlacedImagesGui.open(plugin, player, page + 1);
            return;
        }

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int relativeIndex = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                relativeIndex = i;
                break;
            }
        }

        if (relativeIndex == -1) return;

        List<PlacedImage> all = new ArrayList<>(plugin.getPlacedImageManager().getAllPlacedImages());
        int index = (page * 21) + relativeIndex;
        if (index >= all.size()) return;

        PlacedImage placed = all.get(index);

        if (clickType == ClickType.DROP) {
            // Delete with Q key
            plugin.getPlacedImageManager().removeImage(placed);
            SoundUtil.playDelete(player);
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cลบรูปภาพ &#ffb199" + placed.getId() + " &cออกจากโลกเรียบร้อยแล้ว"));
            PlacedImagesGui.open(plugin, player, page);
            return;
        }

        if (clickType.isShiftClick()) {
            if (clickType.isRightClick()) {
                // Open Command & Slideshow Manager
                CommandManagerGui.open(plugin, player, placed);
            } else {
                // Open live resize editor
                ResizeEditorGui.open(plugin, player, new ResizeEditorGui.ResizeContext(placed));
            }
        } else {
            if (clickType.isRightClick()) {
                // Highlight with glowing particles
                player.closeInventory();
                plugin.getParticleHighlightManager().highlightImage(player, placed, plugin.getConfigManager().getHighlightDurationSeconds());
                SoundUtil.playHighlight(player);
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aเปิดลำแสงระบุตำแหน่งรูปภาพ &e" + placed.getId() + " &aเป็นเวลา 12 วินาที!"));
            } else {
                // Teleport
                Location viewLoc = placed.getViewLocation();
                if (viewLoc != null) {
                    player.teleport(viewLoc);
                    SoundUtil.playTeleport(player);
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aวาร์ปมายังตำแหน่งของรูปภาพ &e" + placed.getId() + " &aแล้ว"));
                } else {
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบโลกที่รูปภาพนี้ตั้งอยู่"));
                }
            }
        }
    }

    private void handleResizeEditor(Player player, ImageryHolder holder, int slot) {
        ResizeEditorGui.ResizeContext ctx = holder.getContext();
        if (ctx == null) return;

        switch (slot) {
            case 19 -> {
                ctx.setTargetWidth(ctx.getTargetWidth() - 1);
                SoundUtil.playClick(player);
                ResizeEditorGui.open(plugin, player, ctx);
            }
            case 21 -> {
                ctx.setTargetWidth(ctx.getTargetWidth() + 1);
                SoundUtil.playClick(player);
                ResizeEditorGui.open(plugin, player, ctx);
            }
            case 23 -> {
                ctx.setTargetHeight(ctx.getTargetHeight() - 1);
                SoundUtil.playClick(player);
                ResizeEditorGui.open(plugin, player, ctx);
            }
            case 25 -> {
                ctx.setTargetHeight(ctx.getTargetHeight() + 1);
                SoundUtil.playClick(player);
                ResizeEditorGui.open(plugin, player, ctx);
            }
            case 29 -> {
                ctx.toggleInvisible();
                SoundUtil.playClick(player);
                ResizeEditorGui.open(plugin, player, ctx);
            }
            case 33 -> {
                ctx.toggleGlowing();
                SoundUtil.playClick(player);
                ResizeEditorGui.open(plugin, player, ctx);
            }
            case 36 -> PlacedImagesGui.open(plugin, player, 0);
            case 31 -> {
                PlacedImage placed = ctx.getPlacedImage();
                placed.setInvisibleFrame(ctx.isInvisible());
                placed.setGlowing(ctx.isGlowing());

                boolean success = plugin.getPlacedImageManager().resizeImage(placed, ctx.getTargetWidth(), ctx.getTargetHeight());
                if (success) {
                    player.closeInventory();
                    SoundUtil.playSuccess(player);
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aปรับขนาดรูปภาพ &e" + placed.getId() +
                            " &aเป็น &b" + ctx.getTargetWidth() + "x" + ctx.getTargetHeight() + " &aบล็อก เรียบร้อยแล้ว!"));
                } else {
                    SoundUtil.playError(player);
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cเกิดข้อผิดพลาดในการปรับขนาด"));
                }
            }
        }
    }

    private void handleAlbumManager(Player player, ImageryHolder holder, int slot, ClickType clickType) {
        int page = holder.getPage();

        if (slot == 36) {
            MainHubGui.open(plugin, player);
            return;
        }
        if (slot == 38 && page > 0) {
            AlbumManagerGui.open(plugin, player, page - 1);
            return;
        }
        if (slot == 40) {
            plugin.getAlbumManager().loadAlbums();
            SoundUtil.playSuccess(player);
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aรีเฟรชข้อมูลอัลบั้มเรียบร้อยแล้ว"));
            AlbumManagerGui.open(plugin, player, page);
            return;
        }
        if (slot == 42) {
            AlbumManagerGui.open(plugin, player, page + 1);
            return;
        }

        int[] slots = {
                10, 11, 12, 13, 14, 15, 16,
                19, 20, 21, 22, 23, 24, 25,
                28, 29, 30, 31, 32, 33, 34
        };

        int relativeIndex = -1;
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == slot) {
                relativeIndex = i;
                break;
            }
        }
        if (relativeIndex == -1) return;

        List<com.imagery.model.ImageAlbum> all = new ArrayList<>(plugin.getAlbumManager().getAllAlbums());
        int index = (page * 21) + relativeIndex;
        if (index >= all.size()) return;

        com.imagery.model.ImageAlbum album = all.get(index);

        if (clickType.isRightClick()) {
            // Quick place album
            placeAlbumOnWall(player, album);
        } else {
            // View album pages
            AlbumViewGui.open(plugin, player, album);
        }
    }

    private void handleAlbumView(Player player, ImageryHolder holder, int slot) {
        com.imagery.model.ImageAlbum album = holder.getContext();
        if (album == null) return;

        if (slot == 36) {
            AlbumManagerGui.open(plugin, player, 0);
            return;
        }

        if (slot == 31) {
            placeAlbumOnWall(player, album);
        }
    }

    private void placeAlbumOnWall(Player player, com.imagery.model.ImageAlbum album) {
        if (album.getPages().isEmpty()) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cอัลบั้มนี้ไม่มีหน้าภาพ"));
            return;
        }

        RayTraceResult ray = player.rayTraceBlocks(6.0);
        if (ray == null || ray.getHitBlock() == null || ray.getHitBlockFace() == null) {
            SoundUtil.playError(player);
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cกรุณาหันหน้ามองไปที่ผนัง/บล็อกในระยะ 6 บล็อก เพื่อวางอัลบั้ม"));
            return;
        }

        Block target = ray.getHitBlock();
        BlockFace face = ray.getHitBlockFace();

        PlacedImage placed = plugin.getPlacedImageManager().placeAlbum(
                player, target, face, album, 3, 2, true, false
        );

        if (placed != null) {
            player.closeInventory();
            SoundUtil.playPlace(player);
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aวางอัลบั้ม &e" + album.getTitle() +
                    " &aสำเร็จ! &7(รหัส: &b" + placed.getId() + "&7, มีทั้งหมด &e" + album.getPages().size() + " &7หน้า)"));
            player.sendMessage(ColorUtil.color("  &#00f2fe📖 ผู้เล่นสามารถคลิกขวาที่ภาพเพื่อเปลี่ยนหน้า 1 > 2 > 3... ได้ทันที!"));
        }
    }

    private void handleCommandManager(Player player, ImageryHolder holder, int slot, ClickType clickType) {
        PlacedImage placed = holder.getContext();
        if (placed == null) return;

        switch (slot) {
            case 19 -> {
                placed.setClickToNextPage(!placed.isClickToNextPage());
                plugin.getPlacedImageManager().savePlacedImages();
                SoundUtil.playClick(player);
                CommandManagerGui.open(plugin, player, placed);
            }
            case 21 -> {
                int sec = placed.getAutoRotateSeconds();
                int nextSec = switch (sec) {
                    case 0 -> 3;
                    case 3 -> 5;
                    case 5 -> 10;
                    case 10 -> 30;
                    default -> 0;
                };
                placed.setAutoRotateSeconds(nextSec);
                plugin.getPlacedImageManager().savePlacedImages();
                SoundUtil.playClick(player);
                CommandManagerGui.open(plugin, player, placed);
            }
            case 23 -> {
                placed.getClickCommands().clear();
                plugin.getPlacedImageManager().savePlacedImages();
                SoundUtil.playDelete(player);
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aล้างคำสั่งทั้งหมดในภาพเรียบร้อยแล้ว"));
                CommandManagerGui.open(plugin, player, placed);
            }
            case 25 -> {
                String cmd;
                if (clickType.isShiftClick()) {
                    cmd = "warp shop";
                } else if (clickType.isRightClick()) {
                    cmd = "menu";
                } else {
                    cmd = "spawn";
                }
                placed.addClickCommand(new com.imagery.model.ClickCommand(com.imagery.model.ClickCommand.Type.PLAYER, cmd));
                plugin.getPlacedImageManager().savePlacedImages();
                SoundUtil.playSuccess(player);
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aเพิ่มคำสั่ง &e/" + cmd + " &aลงในรูปภาพแล้ว!"));
                CommandManagerGui.open(plugin, player, placed);
            }
            case 31 -> {
                boolean ok = plugin.getPlacedImageManager().nextPage(placed);
                if (ok) {
                    SoundUtil.playPlace(player);
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aเปลี่ยนไปหน้า &e" + (placed.getCurrentPage() + 1) + "/" + placed.getPages().size()));
                }
                CommandManagerGui.open(plugin, player, placed);
            }
            case 36 -> PlacedImagesGui.open(plugin, player, 0);
        }
    }
}

