package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.ColorUtil;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BloomeryXCommand implements CommandExecutor, TabCompleter {

    private final BloomeryX plugin;

    public BloomeryXCommand(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelpMenu(sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bloomeryx.admin")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }

            plugin.reloadPlugin();
            MessageUtil.sendConfigMessage(sender, "config-reloaded");
            if (sender instanceof Player) {
                MessageUtil.playSuccess((Player) sender);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("info") || args[0].equalsIgnoreCase("version")) {
            sender.sendMessage(ColorUtil.colorize("&#FF758C▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            sender.sendMessage(ColorUtil.colorize("  &#FF7EB3&lBloomeryX &7- &fModern Server Essentials"));
            sender.sendMessage(ColorUtil.colorize("  &7Version: &#B2EBF2" + plugin.getDescription().getVersion()));
            sender.sendMessage(ColorUtil.colorize("  &7LuckPerms Integration: " + (plugin.getLuckPermsHook().isAvailable() ? "&#70E000Connected &a✔" : "&#FF758CNot Hooked &c✕")));
            sender.sendMessage(ColorUtil.colorize("  &7PlaceholderAPI: " + (plugin.getPlaceholderHook().isPapiAvailable() ? "&#70E000Hooked &a✔" : "&#FF758CNot Found &c✕")));
            sender.sendMessage(ColorUtil.colorize("  &7Developer: &#FFB4A2BloomeryX Team"));
            sender.sendMessage(ColorUtil.colorize("&#FF758C▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
            return true;
        }

        MessageUtil.sendConfigMessage(sender, "unknown-command");
        return true;
    }

    private void sendHelpMenu(CommandSender sender) {
        sender.sendMessage(ColorUtil.colorize("&#FF758C▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬ [ &#FF7EB3&lBloomeryX Commands &#FF758C] ▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Gamemode: &f/gm <0-3/s,c,a,sp> &7| &f/gms&7, &f/gmc&7, &f/gma&7, &f/gmsp"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Time & Weather: &f/day&7, &f/night&7, &f/sun&7, &f/rain&7, &f/storm"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Teleportation: &f/tpa&7, &f/tpaccept&7, &f/tpdeny&7, &f/back"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Homes & Warps: &f/home&7, &f/sethome&7, &f/delhome&7, &f/warp&7, &f/setwarp"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ AFK Zone: &f/afk [reason]&7, &f/setafk&7, &f/delafk"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Freeze & Control: &f/freeze <player|world>&7, &f/unfreeze"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Jukebox Music: &f/music &8(Custom .nbs music player)"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Utilities: &f/fly&7, &f/heal&7, &f/feed&7, &f/god&7, &f/kill&7, &f/smite&7, &f/vanish"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Workstations: &f/craft&7, &f/ec&7, &f/anvil&7, &f/smithing&7, &f/loom"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Mention & Player: &f/nick <name|off>&7, &f@PlayerName &8(Mention alerts)"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Punish & Ban: &f/ban&7, &f/unban&7, &f/ban-ip&7, &f/unban-ip"));
        sender.sendMessage(ColorUtil.colorize("&#FF7EB3★ Admin: &f/broadcast <msg>&7, &f/invsee&7, &f/bx reload"));
        sender.sendMessage(ColorUtil.colorize("&#FF758C▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>();
            for (String sub : Arrays.asList("help", "info", "reload")) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    list.add(sub);
                }
            }
            return list;
        }
        return new ArrayList<>();
    }
}

