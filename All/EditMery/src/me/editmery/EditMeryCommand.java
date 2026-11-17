package me.editmery;

import me.editmery.menu.EditMeryMenu;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class EditMeryCommand implements CommandExecutor, TabCompleter {

    private final EditMeryPlugin plugin;
    private final EditMeryManager manager;

    private static final List<String> SUBCOMMANDS = Arrays.asList("menu", "gui", "enable", "disable", "toggle", "on", "off", "status", "reload", "help");

    public EditMeryCommand(EditMeryPlugin plugin, EditMeryManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                manager.loadConfig();
                plugin.applyWorldEditPrefix();
                sender.sendMessage(ColorUtil.colorize(manager.getMsgReloaded()));
                return true;
            }
            sender.sendMessage(ColorUtil.colorize("&cคำสั่งนี้ใช้ได้เฉพาะผู้เล่นในเซิร์ฟเวอร์เท่านั้น (ยกเว้น /editmery reload)"));
            return true;
        }

        Player player = (Player) sender;

        // เฉพาะผู้เล่นที่มี OP หรือมีสิทธิ์ editmery.use เท่านั้น
        if (!player.isOp() && !player.hasPermission("editmery.use")) {
            player.sendMessage(ColorUtil.colorize(manager.getMsgNoPermission()));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            boolean newState = manager.toggle(player);
            manager.sendToggleSuccess(player, newState);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu":
            case "gui": {
                EditMeryMenu.openMainMenu(player);
                break;
            }
            case "enable":
            case "on": {
                if (manager.isEnabled(player)) {
                    player.sendMessage(ColorUtil.colorize("&8(&#B388FF✦ &#FF80ABEditMery&8) &#FFD166ℹ โหมดขวาน WorldEdit ของคุณเปิดใช้งานอยู่แล้ว!"));
                } else {
                    manager.setEnabled(player, true);
                    manager.sendToggleSuccess(player, true);
                }
                break;
            }
            case "disable":
            case "off": {
                if (!manager.isEnabled(player)) {
                    player.sendMessage(ColorUtil.colorize("&8(&#B388FF✦ &#FF80ABEditMery&8) &#FFD166ℹ โหมดขวาน WorldEdit ของคุณปิดการใช้งานอยู่แล้ว!"));
                } else {
                    manager.setEnabled(player, false);
                    manager.sendToggleSuccess(player, false);
                }
                break;
            }
            case "status": {
                boolean state = manager.isEnabled(player);
                player.sendMessage(ColorUtil.colorize(manager.getMsgStatus(state)));
                break;
            }
            case "reload": {
                if (!player.isOp() && !player.hasPermission("editmery.reload")) {
                    player.sendMessage(ColorUtil.colorize(manager.getMsgNoPermission()));
                    return true;
                }
                manager.loadConfig();
                plugin.applyWorldEditPrefix();
                player.sendMessage(ColorUtil.colorize(manager.getMsgReloaded()));
                break;
            }
            case "help":
            default: {
                sendHelp(player);
                break;
            }
        }

        return true;
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorize("&8╭━━━ &#B388FF✦ &#C084FCE&#D080FAd&#E07CF8i&#F078F6t&#FF74F4M&#FF79DDe&#FF7EC6r&#FF83AFy &#FF83AF✦ &8[&7คำสั่งแอดมิน (OP)&8] ━━━━━━━━━━━━╮"));
        player.sendMessage(ColorUtil.colorize("&8│"));
        player.sendMessage(ColorUtil.colorize("&8│  &#FFD166/em menu     &8- &#B388FFเปิดเมนู GUI รวมคำสั่ง WorldEdit ครบวงจร"));
        player.sendMessage(ColorUtil.colorize("&8│  &#FFD166/em enable   &8- &#06D6A0เปิดใช้งานโหมดขวาน WorldEdit &8(Wand & คำสั่ง //)"));
        player.sendMessage(ColorUtil.colorize("&8│  &#FFD166/em disable  &8- &#EF476Fปิดการใช้งานโหมดขวาน &8(ขวานไม้ใช้ตัดไม้ทุบบล็อกได้ปกติ)"));
        player.sendMessage(ColorUtil.colorize("&8│  &#FFD166/em toggle   &8- &#F8FAFCสลับสถานะเปิด / ปิด การใช้งาน"));
        player.sendMessage(ColorUtil.colorize("&8│  &#FFD166/em status   &8- &#F8FAFCตรวจสอบสถานะปัจจุบัน"));
        if (player.isOp() || player.hasPermission("editmery.reload")) {
            player.sendMessage(ColorUtil.colorize("&8│  &#FFD166/em reload   &8- &#F8FAFCรีโหลดการตั้งค่าทั้งหมด"));
        }
        player.sendMessage(ColorUtil.colorize("&8│"));
        player.sendMessage(ColorUtil.colorize("&8╰━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━╯"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // ซ่อน Tab Complete ทั้งหมดถ้าไม่ใช่ OP
        if (sender instanceof Player) {
            Player p = (Player) sender;
            if (!p.isOp() && !p.hasPermission("editmery.use")) {
                return Collections.emptyList();
            }
        }

        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String current = args[0].toLowerCase(Locale.ROOT);
            for (String sub : SUBCOMMANDS) {
                if (sub.equalsIgnoreCase("reload") && !sender.isOp() && !sender.hasPermission("editmery.reload")) {
                    continue;
                }
                if (sub.startsWith(current)) {
                    completions.add(sub);
                }
            }
        }
        return completions;
    }
}
