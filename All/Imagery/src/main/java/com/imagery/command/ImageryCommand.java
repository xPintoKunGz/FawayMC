package com.imagery.command;

import com.imagery.ImageryPlugin;
import com.imagery.gui.GalleryGui;
import com.imagery.gui.MainHubGui;
import com.imagery.gui.PlacedImagesGui;
import com.imagery.model.ImageMetadata;
import com.imagery.model.PlacedImage;
import com.imagery.util.ColorUtil;
import com.imagery.util.SoundUtil;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.RayTraceResult;

/**
 * Command executor for /imagery and its aliases (/img, /image).
 */
public class ImageryCommand implements CommandExecutor {

    private final ImageryPlugin plugin;

    public ImageryCommand(ImageryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                plugin.getConfigManager().loadConfig();
                plugin.getImageFileManager().scanImages();
                sender.sendMessage("[Imagery] Configuration and images folder reloaded.");
                return true;
            }
            sender.sendMessage("[Imagery] This command can only be used by players in-game.");
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("gui") || args[0].equalsIgnoreCase("menu")) {
            MainHubGui.open(plugin, player);
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "gallery" -> {
                GalleryGui.open(plugin, player, 0);
                return true;
            }
            case "album", "albums" -> {
                if (args.length >= 3 && args[1].equalsIgnoreCase("place")) {
                    handlePlaceAlbum(player, args);
                    return true;
                }
                com.imagery.gui.AlbumManagerGui.open(plugin, player, 0);
                return true;
            }
            case "page" -> {
                handlePageJump(player, args);
                return true;
            }
            case "cmd", "command" -> {
                handleCommandSub(player, args);
                return true;
            }
            case "list" -> {
                PlacedImagesGui.open(plugin, player, 0);
                return true;
            }
            case "reload" -> {
                plugin.getConfigManager().loadConfig();
                plugin.getImageFileManager().scanImages();
                SoundUtil.playSuccess(player);
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aรีโหลดการตั้งค่าและสแกนรูปภาพสำเร็จ! &7(พบ &e" +
                        plugin.getImageFileManager().getAllMetadata().size() + " &7ไฟล์)"));
                return true;
            }
            case "place" -> {
                handlePlace(player, args);
                return true;
            }
            case "resize" -> {
                handleResize(player, args);
                return true;
            }
            case "tp", "teleport" -> {
                handleTeleport(player, args);
                return true;
            }
            case "highlight", "hl" -> {
                handleHighlight(player, args);
                return true;
            }
            case "delete", "remove" -> {
                handleDelete(player, args);
                return true;
            }
            case "help" -> {
                sendHelp(player);
                return true;
            }
            default -> {
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบคำสั่งที่ระบุ พิมพ์ &e/imagery help &cเพื่อดูวิธีใช้งาน"));
                return true;
            }
        }
    }

    private void handlePlace(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cการใช้งาน: &e/imagery place <ชื่อไฟล์> [ความกว้าง] [ความสูง]"));
            return;
        }

        String fileName = args[1];
        ImageMetadata meta = plugin.getImageFileManager().getMetadata(fileName);
        if (meta == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบไฟล์รูปภาพ &e" + fileName + " &cในโฟลเดอร์ images/"));
            return;
        }

        int width, height;
        if (args.length >= 4) {
            try {
                width = Integer.parseInt(args[2]);
                height = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cขนาดความกว้างและความสูงต้องเป็นตัวเลขจำนวนเต็ม"));
                return;
            }
        } else {
            int[] suggested = meta.getSuggestedBlocks(6);
            width = suggested[0];
            height = suggested[1];
        }

        RayTraceResult ray = player.rayTraceBlocks(6.0);
        if (ray == null || ray.getHitBlock() == null || ray.getHitBlockFace() == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cกรุณาหันหน้ามองไปที่ผนัง/บล็อกในระยะ 6 บล็อก เพื่อวางภาพ"));
            return;
        }

        Block target = ray.getHitBlock();
        BlockFace face = ray.getHitBlockFace();
        PlacedImage placed = plugin.getPlacedImageManager().placeImage(
                player, target, face, meta, width, height, true, false
        );

        if (placed != null) {
            SoundUtil.playPlace(player);
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aวางรูปภาพ &e" + meta.getFileName() + " &aสำเร็จ! &7(รหัส: &b" + placed.getId() + "&7)"));
            player.sendMessage(ColorUtil.color("  &#00f5a0📍 พิกัด: &f[X: " + placed.getOriginX() + ", Y: " + placed.getOriginY() + ", Z: " + placed.getOriginZ() + "]"));
        }
    }

    private void handleResize(Player player, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cการใช้งาน: &e/imagery resize <รหัสรูปภาพ> <ความกว้างใหม่> <ความสูงใหม่>"));
            return;
        }

        String id = args[1];
        PlacedImage placed = plugin.getPlacedImageManager().getPlacedImage(id);
        if (placed == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบรูปภาพรหัส &e" + id));
            return;
        }

        int width, height;
        try {
            width = Integer.parseInt(args[2]);
            height = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cขนาดความกว้างและความสูงต้องเป็นตัวเลข"));
            return;
        }

        boolean ok = plugin.getPlacedImageManager().resizeImage(placed, width, height);
        if (ok) {
            SoundUtil.playSuccess(player);
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aปรับขนาดรูปภาพ &e" + id + " &aเป็น &b" + width + "x" + height + " &aบล็อก เรียบร้อยแล้ว!"));
        } else {
            SoundUtil.playError(player);
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cเกิดข้อผิดพลาดในการปรับขนาด"));
        }
    }

    private void handleTeleport(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cการใช้งาน: &e/imagery tp <รหัสรูปภาพ>"));
            return;
        }

        PlacedImage placed = plugin.getPlacedImageManager().getPlacedImage(args[1]);
        if (placed == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบรูปภาพรหัส &e" + args[1]));
            return;
        }

        Location loc = placed.getViewLocation();
        if (loc != null) {
            player.teleport(loc);
            SoundUtil.playTeleport(player);
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aวาร์ปมายังตำแหน่งของรูปภาพ &e" + placed.getId() + " &aแล้ว"));
        }
    }

    private void handleHighlight(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cการใช้งาน: &e/imagery highlight <รหัสรูปภาพ>"));
            return;
        }

        PlacedImage placed = plugin.getPlacedImageManager().getPlacedImage(args[1]);
        if (placed == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบรูปภาพรหัส &e" + args[1]));
            return;
        }

        plugin.getParticleHighlightManager().highlightImage(player, placed, plugin.getConfigManager().getHighlightDurationSeconds());
        SoundUtil.playHighlight(player);
        player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aเปิดลำแสงระบุตำแหน่งรูปภาพ &e" + placed.getId() + " &aเป็นเวลา 12 วินาที!"));
    }

    private void handleDelete(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cการใช้งาน: &e/imagery delete <รหัสรูปภาพ>"));
            return;
        }

        PlacedImage placed = plugin.getPlacedImageManager().getPlacedImage(args[1]);
        if (placed == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบรูปภาพรหัส &e" + args[1]));
            return;
        }

        plugin.getPlacedImageManager().removeImage(placed);
        SoundUtil.playDelete(player);
        player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cลบรูปภาพ &e" + args[1] + " &cออกจากโลกเรียบร้อยแล้ว"));
    }

    private void handlePlaceAlbum(Player player, String[] args) {
        String albumId = args[2];
        com.imagery.model.ImageAlbum album = plugin.getAlbumManager().getAlbum(albumId);
        if (album == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบอัลบั้มรหัส &e" + albumId));
            return;
        }

        int width = 3, height = 2;
        if (args.length >= 5) {
            try {
                width = Integer.parseInt(args[3]);
                height = Integer.parseInt(args[4]);
            } catch (NumberFormatException ignored) {}
        }

        RayTraceResult ray = player.rayTraceBlocks(6.0);
        if (ray == null || ray.getHitBlock() == null || ray.getHitBlockFace() == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cกรุณาหันหน้ามองไปที่ผนัง/บล็อกในระยะ 6 บล็อก เพื่อวางอัลบั้ม"));
            return;
        }

        PlacedImage placed = plugin.getPlacedImageManager().placeAlbum(
                player, ray.getHitBlock(), ray.getHitBlockFace(), album, width, height, true, false
        );

        if (placed != null) {
            SoundUtil.playPlace(player);
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aวางอัลบั้ม &e" + album.getTitle() + " &aสำเร็จ! &7(รหัส: &b" + placed.getId() + "&7)"));
        }
    }

    private void handlePageJump(Player player, String[] args) {
        if (args.length < 3) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cการใช้งาน: &e/imagery page <รหัสรูปภาพ> <หมายเลขหน้า>"));
            return;
        }

        PlacedImage placed = plugin.getPlacedImageManager().getPlacedImage(args[1]);
        if (placed == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบรูปภาพรหัส &e" + args[1]));
            return;
        }

        try {
            int pageNum = Integer.parseInt(args[2]) - 1; // 1-based to 0-based
            boolean ok = plugin.getPlacedImageManager().setPage(placed, pageNum);
            if (ok) {
                SoundUtil.playPlace(player);
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aเปลี่ยนรูปภาพ &e" + placed.getId() + " &aไปที่หน้า &b" + (pageNum + 1) + "/" + placed.getPages().size()));
            } else {
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cหมายเลขหน้าไม่ถูกต้อง"));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cหมายเลขหน้าต้องเป็นตัวเลข"));
        }
    }

    private void handleCommandSub(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cการใช้งาน: &e/imagery cmd <add|remove|list> <รหัสรูปภาพ> [ค่า]"));
            return;
        }

        String action = args[1].toLowerCase();
        if (args.length < 3) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cกรุณาระบุรหัสรูปภาพ"));
            return;
        }

        PlacedImage placed = plugin.getPlacedImageManager().getPlacedImage(args[2]);
        if (placed == null) {
            player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบรูปภาพรหัส &e" + args[2]));
            return;
        }

        switch (action) {
            case "list" -> {
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aคำสั่งทั้งหมดในรูปภาพ &e" + placed.getId() + "&a:"));
                if (placed.getClickCommands().isEmpty()) {
                    player.sendMessage(ColorUtil.color("  &7(ไม่มีคำสั่งที่ติดตั้ง)"));
                } else {
                    for (int i = 0; i < placed.getClickCommands().size(); i++) {
                        com.imagery.model.ClickCommand cmd = placed.getClickCommands().get(i);
                        player.sendMessage(ColorUtil.color("  &#00f5a0[" + (i + 1) + "] &f(" + cmd.getType().name() + ") &#f6d365/" + cmd.getCommand()));
                    }
                }
            }
            case "add" -> {
                if (args.length < 5) {
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cการใช้งาน: &e/imagery cmd add <รหัส> <player|console|message> <คำสั่ง...>"));
                    return;
                }

                com.imagery.model.ClickCommand.Type type;
                try {
                    type = com.imagery.model.ClickCommand.Type.valueOf(args[3].toUpperCase());
                } catch (IllegalArgumentException e) {
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cชนิดคำสั่งต้องเป็น: &eplayer, console, หรือ message"));
                    return;
                }

                StringBuilder sb = new StringBuilder();
                for (int i = 4; i < args.length; i++) {
                    sb.append(args[i]).append(i == args.length - 1 ? "" : " ");
                }

                String cmdStr = sb.toString();
                placed.addClickCommand(new com.imagery.model.ClickCommand(type, cmdStr));
                plugin.getPlacedImageManager().savePlacedImages();
                SoundUtil.playSuccess(player);
                player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aเพิ่มคำสั่ง &e" + cmdStr + " &7(" + type.name() + ") &aเรียบร้อยแล้ว!"));
            }
            case "remove" -> {
                if (args.length < 4) {
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cการใช้งาน: &e/imagery cmd remove <รหัส> <ลำดับที่>"));
                    return;
                }

                try {
                    int index = Integer.parseInt(args[3]) - 1;
                    if (index >= 0 && index < placed.getClickCommands().size()) {
                        placed.removeClickCommand(index);
                        plugin.getPlacedImageManager().savePlacedImages();
                        SoundUtil.playDelete(player);
                        player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&aลบคำสั่งลำดับที่ &e" + (index + 1) + " &aเรียบร้อยแล้ว"));
                    } else {
                        player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cไม่พบลำดับคำสั่งที่ระบุ"));
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.PREFIX + ColorUtil.color("&cลำดับคำสั่งต้องเป็นตัวเลข"));
                }
            }
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.color("&#00f2fe━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        player.sendMessage(ColorUtil.color("&#00f2fe❖ &#4facfe&lImagery System 26.3 &7- คำสั่งทั้งหมด"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery &7- เปิดเมนู GUI ควบคุมหลัก"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery album &7- เปิดเมนูจัดการอัลบั้มภาพ & สไลด์โชว์"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery page <รหัส> <หน้า> &7- สลับไปหน้าที่ต้องการ"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery cmd add <รหัส> <player|console> <คำสั่ง> &7- เพิ่มคำสั่งเมื่อคลิกภาพ"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery cmd list <รหัส> &7- ดูคำสั่งทั้งหมดในภาพ"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery place <ไฟล์> [กว้าง] [สูง] &7- วางรูปภาพที่ผนัง"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery resize <รหัส> <กว้าง> <สูง> &7- ปรับขนาดรูปภาพ"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery tp <รหัส> &7- วาร์ปไปที่หน้าของรูปภาพ"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery highlight <รหัส> &7- ยิงลำแสงอนุภาคระบุตำแหน่ง"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery delete <รหัส> &7- ลบรูปภาพออกจากโลก"));
        player.sendMessage(ColorUtil.color("&#00f2fe/imagery reload &7- สแกนโฟลเดอร์ images/ และ albums/ ใหม่"));
        player.sendMessage(ColorUtil.color("&#00f2fe━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
    }
}

