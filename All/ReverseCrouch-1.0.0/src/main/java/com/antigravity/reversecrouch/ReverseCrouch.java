package com.antigravity.reversecrouch;

import com.antigravity.reversecrouch.listener.BridgingListener;
import com.antigravity.reversecrouch.listener.SneakSpeedListener;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ReverseCrouch extends JavaPlugin implements CommandExecutor, TabCompleter {

    private static ReverseCrouch instance;
    private BridgingListener bridgingListener;
    private SneakSpeedListener sneakSpeedListener;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        bridgingListener = new BridgingListener(this);
        sneakSpeedListener = new SneakSpeedListener(this);

        getServer().getPluginManager().registerEvents(bridgingListener, this);
        getServer().getPluginManager().registerEvents(sneakSpeedListener, this);

        // Apply attribute speed to currently online players
        sneakSpeedListener.applyToAllOnline();

        if (getCommand("reversecrouch") != null) {
            getCommand("reversecrouch").setExecutor(this);
            getCommand("reversecrouch").setTabCompleter(this);
        }

        getLogger().info("ReverseCrouch v1.0.0 enabled! 1.8.9-style bridging is now active.");
    }

    @Override
    public void onDisable() {
        if (sneakSpeedListener != null) {
            sneakSpeedListener.resetAllOnline();
        }
        getLogger().info("ReverseCrouch disabled.");
    }

    public static ReverseCrouch getInstance() {
        return instance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("reversecrouch.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
                return true;
            }
            reloadConfig();
            sneakSpeedListener.applyToAllOnline();
            sender.sendMessage(ChatColor.GREEN + "[ReverseCrouch] Configuration reloaded successfully!");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "=== ReverseCrouch v1.0.0 ===");
        sender.sendMessage(ChatColor.YELLOW + "1.8.9-Style bridging & crouch mechanics are active (Always On).");
        if (sender.hasPermission("reversecrouch.admin")) {
            sender.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/reversecrouch reload" + ChatColor.GRAY + " to reload config.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("reversecrouch.admin")) {
            List<String> completions = new ArrayList<>();
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
            return completions;
        }
        return Collections.emptyList();
    }
}

