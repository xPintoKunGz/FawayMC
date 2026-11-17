package com.imagery.gui;

import com.imagery.ImageryPlugin;
import com.imagery.model.ClickCommand;
import com.imagery.model.PlacedImage;
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
 * GUI to manage click commands, multi-page flipping, and slideshow rotation settings.
 */
public class CommandManagerGui {

    public static void open(ImageryPlugin plugin, Player player, PlacedImage placed) {
        String title = ColorUtil.color("&#00f2feคำสั่ง & สไลด์ &#7f8c8d| &#ffffff" + placed.getId());
        ImageryHolder holder = new ImageryHolder(ImageryHolder.GuiType.COMMAND_MANAGER, placed);
        Inventory inv = Bukkit.createInventory(holder, 45, title);
        holder.setInventory(inv);

        GuiUtil.fillBorders(inv);

        // Header (Slot 4)
        int current = placed.getCurrentPage() + 1;
        int total = Math.max(1, placed.getPages().size());
        ItemStack header = GuiUtil.createItem(
                Material.COMMAND_BLOCK,
                "&#00f2fe⚡ &#4facfe&lตั้งค่าคำสั่ง & สไลด์โชว์",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#a18cd1รหัสรูปภาพ: &f" + placed.getId(),
                "&#a18cd1ไฟล์ปัจจุบัน: &f" + placed.getCurrentFileName(),
                "&#00f5a0จำนวนหน้าทั้งหมด: &f" + total + " หน้า (หน้าปัจจุบัน: " + current + ")",
                "&#f6d365คำสั่งที่ติดตั้งอยู่: &f" + placed.getClickCommands().size() + " คำสั่ง",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );
        inv.setItem(4, header);

        // Slot 19: Toggle Click to Next Page
        String nextStatus = placed.isClickToNextPage() ? "&#00f5a0✔ เปิด (คลิกขวาเพื่อเปลี่ยนหน้า)" : "&#ff0844✖ ปิด";
        inv.setItem(19, GuiUtil.createItem(
                Material.BOOK,
                "&#00f2fe📖 สลับหน้าเมื่อคลิก (Click to Next Page)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&7สถานะ: " + nextStatus,
                "&7เมื่อเปิด: ผู้เล่นคลิกขวาที่ภาพจะเปลี่ยนไปหน้า 2, 3, 4...",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#00f2fe▶ คลิกเพื่อสลับเปิด/ปิด"
        ));

        // Slot 21: Auto Slideshow Rotation
        int autoSec = placed.getAutoRotateSeconds();
        String autoStr = autoSec == 0 ? "&#ff0844✖ ปิดใช้งาน (เปลี่ยนด้วยการคลิกเท่านั้น)" : "&#00f5a0✔ หมุนทุกๆ " + autoSec + " วินาที";
        inv.setItem(21, GuiUtil.createItem(
                Material.CLOCK,
                "&#f6d365🔄 สไลด์โชว์อัตโนมัติ (Auto Slideshow)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&7สถานะ: " + autoStr,
                "&7ระบบจะเปลี่ยนหน้าภาพอัตโนมัติตามเวลาที่กำหนด",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#f6d365▶ คลิกเพื่อเปลี่ยนเวลา (ปิด > 3s > 5s > 10s > 30s)"
        ));

        // Slot 23: Display Current Commands
        List<String> cmdLore = new ArrayList<>();
        cmdLore.add("&7━━━━━━━━━━━━━━━━━━━━━━━━━━");
        if (placed.getClickCommands().isEmpty()) {
            cmdLore.add("&7(ยังไม่มีคำสั่งที่ติดตั้งในภาพนี้)");
        } else {
            for (int i = 0; i < placed.getClickCommands().size(); i++) {
                ClickCommand c = placed.getClickCommands().get(i);
                cmdLore.add("  &#00f5a0[" + (i + 1) + "] &f(" + c.getType().name() + ") &#f6d365/" + c.getCommand());
            }
        }
        cmdLore.add("&7━━━━━━━━━━━━━━━━━━━━━━━━━━");
        cmdLore.add("&#ff0844▶ คลิกเพื่อล้างคำสั่งทั้งหมด");
        inv.setItem(23, GuiUtil.createItem(Material.REPEATING_COMMAND_BLOCK, "&#a18cd1📜 รายการคำสั่งในภาพ", cmdLore));

        // Slot 25: Quick Add Preset Commands
        inv.setItem(25, GuiUtil.createItem(
                Material.NETHER_STAR,
                "&#00f5a0➕ เพิ่มคำสั่งคลิก (Add Command)",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&#00f2fe[คลิกซ้าย] &fเพิ่มคำสั่งผู้เล่น: &e/spawn",
                "&#00f5a0[คลิกขวา] &fเพิ่มคำสั่งผู้เล่น: &e/menu",
                "&#f6d365[Shift+คลิกซ้าย] &fเพิ่มคำสั่งผู้เล่น: &e/warp shop",
                "&7━━━━━━━━━━━━━━━━━━━━━━━━━━",
                "&7(สามารถพิมพ์ /imagery cmd add " + placed.getId() + " เพื่อใส่คำสั่งใดๆ ก็ได้)"
        ));

        // Slot 31: Test Flip to Next Page
        inv.setItem(31, GuiUtil.createItem(
                Material.SPECTRAL_ARROW,
                "&#00f2fe⏩ ทดสอบเปลี่ยนหน้า (Next Page Now)",
                "&7คลิกเพื่อทดสอบเปลี่ยนหน้าภาพในโลกเดี๋ยวนี้"
        ));

        // Back to Placed Manager (Slot 36)
        inv.setItem(36, GuiUtil.createItem(
                Material.ARROW,
                "&#f6d365◀ กลับสู่หน้ารายการ",
                "&7ย้อนกลับ"
        ));

        player.openInventory(inv);
        SoundUtil.playClick(player);
    }
}

