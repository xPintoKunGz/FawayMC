package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.BloomeryX;
import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlayerUtilityCommands implements CommandExecutor, TabCompleter {

    private final BloomeryX plugin;

    public PlayerUtilityCommands(BloomeryX plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();

        switch (name) {
            case "fly":
                return handleFly(sender, args);
            case "heal":
                return handleHeal(sender, args);
            case "feed":
                return handleFeed(sender, args);
            case "god":
            case "godmode":
                return handleGod(sender, args);
            case "speed":
                return handleSpeed(sender, args);
            case "vanish":
            case "v":
                return handleVanish(sender, args);
            case "hat":
                return handleHat(sender);
            case "clear":
            case "ci":
            case "clearinventory":
                return handleClear(sender, args);
            case "suicide":
                return handleSuicide(sender);
            case "ping":
                return handlePing(sender, args);
            case "near":
                return handleNear(sender, args);
            case "repair":
            case "fix":
                return handleRepair(sender, args);
            case "kill":
                return handleKill(sender, args);
            case "smite":
            case "lightning":
            case "strike":
                return handleSmite(sender, args);
            default:
                return false;
        }
    }

    private boolean handleFly(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("bloomeryx.command.fly.others")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("bloomeryx.command.fly")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
        }

        boolean newState = !target.getAllowFlight();
        target.setAllowFlight(newState);
        if (!newState) {
            target.setFlying(false);
        }

        if (target.equals(sender)) {
            MessageUtil.sendConfigMessage(target, newState ? "fly-enabled" : "fly-disabled");
        } else {
            MessageUtil.sendConfigMessage(target, newState ? "fly-enabled" : "fly-disabled");
            MessageUtil.sendConfigMessage(sender, "fly-other", "{target}", target.getName(), "{status}", newState ? "&aON" : "&cOFF");
        }
        MessageUtil.playSuccess(target);
        return true;
    }

    private boolean handleHeal(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("bloomeryx.command.heal.others")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("bloomeryx.command.heal")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
        }

        AttributeInstance maxHealthAttr = target.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double maxHealth = maxHealthAttr != null ? maxHealthAttr.getValue() : 20.0;
        target.setHealth(maxHealth);
        target.setFoodLevel(20);
        target.setSaturation(20.0f);
        target.setFireTicks(0);
        for (PotionEffect effect : target.getActivePotionEffects()) {
            target.removePotionEffect(effect.getType());
        }

        if (target.equals(sender)) {
            MessageUtil.sendConfigMessage(target, "heal-self");
        } else {
            MessageUtil.sendConfigMessage(target, "heal-self");
            MessageUtil.sendConfigMessage(sender, "heal-other", "{target}", target.getName());
        }
        MessageUtil.playSuccess(target);
        return true;
    }

    private boolean handleFeed(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("bloomeryx.command.feed.others")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("bloomeryx.command.feed")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
        }

        target.setFoodLevel(20);
        target.setSaturation(20.0f);

        if (target.equals(sender)) {
            MessageUtil.sendConfigMessage(target, "feed-self");
        } else {
            MessageUtil.sendConfigMessage(target, "feed-self");
            MessageUtil.sendConfigMessage(sender, "feed-other", "{target}", target.getName());
        }
        MessageUtil.playSuccess(target);
        return true;
    }

    private boolean handleGod(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("bloomeryx.command.god.others")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("bloomeryx.command.god")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
        }

        boolean current = plugin.getPlayerDataManager().isGod(target.getUniqueId());
        plugin.getPlayerDataManager().setGod(target.getUniqueId(), !current);

        MessageUtil.sendConfigMessage(target, !current ? "god-enabled" : "god-disabled");
        if (!target.equals(sender)) {
            MessageUtil.sendMessage(sender, "&aSet god mode for &f" + target.getName() + " &ato " + (!current ? "&aENABLED" : "&cDISABLED"));
        }
        MessageUtil.playSuccess(target);
        return true;
    }

    private boolean handleSpeed(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.speed")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.sendMessage(sender, "&#FF4B4B✕ &cUsage: &f/speed <1-10>");
            return true;
        }

        try {
            float speed = Float.parseFloat(args[0]);
            if (speed < 1 || speed > 10) {
                MessageUtil.sendConfigMessage(sender, "speed-invalid");
                return true;
            }

            float finalSpeed = speed / 10.0f;
            if (player.isFlying()) {
                player.setFlySpeed(finalSpeed);
                MessageUtil.sendConfigMessage(player, "speed-set", "{type}", "Fly", "{speed}", String.valueOf((int) speed));
            } else {
                player.setWalkSpeed(finalSpeed);
                MessageUtil.sendConfigMessage(player, "speed-set", "{type}", "Walk", "{speed}", String.valueOf((int) speed));
            }
            MessageUtil.playSuccess(player);
        } catch (NumberFormatException e) {
            MessageUtil.sendConfigMessage(sender, "speed-invalid");
        }
        return true;
    }

    private boolean handleVanish(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.vanish")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        boolean vanished = plugin.getPlayerDataManager().isVanished(player.getUniqueId());
        boolean newState = !vanished;
        plugin.getPlayerDataManager().setVanished(player.getUniqueId(), newState);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (newState) {
                if (!online.hasPermission("bloomeryx.vanish.see")) {
                    online.hidePlayer(plugin, player);
                }
            } else {
                online.showPlayer(plugin, player);
            }
        }

        MessageUtil.sendConfigMessage(player, newState ? "vanish-enabled" : "vanish-disabled");
        MessageUtil.playSuccess(player);
        return true;
    }

    private boolean handleHat(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.hat")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() == Material.AIR) {
            MessageUtil.sendConfigMessage(player, "hat-empty");
            MessageUtil.playError(player);
            return true;
        }

        ItemStack helmet = player.getInventory().getHelmet();
        player.getInventory().setHelmet(hand.clone());
        player.getInventory().setItemInMainHand(helmet);

        MessageUtil.sendConfigMessage(player, "hat-success");
        MessageUtil.playSuccess(player);
        return true;
    }

    private boolean handleClear(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            if (!sender.hasPermission("bloomeryx.command.clear.others")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            target = (Player) sender;
            if (!target.hasPermission("bloomeryx.command.clear")) {
                MessageUtil.sendConfigMessage(sender, "no-permission");
                return true;
            }
        }

        target.getInventory().clear();
        if (target.equals(sender)) {
            MessageUtil.sendConfigMessage(target, "clear-self");
        } else {
            MessageUtil.sendConfigMessage(target, "clear-self");
            MessageUtil.sendConfigMessage(sender, "clear-other", "{target}", target.getName());
        }
        MessageUtil.playSuccess(target);
        return true;
    }

    private boolean handleSuicide(CommandSender sender) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.suicide")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }
        player.setHealth(0.0);
        MessageUtil.sendMessage(player, "&#FF758CYou took your own life.");
        return true;
    }

    private boolean handlePing(CommandSender sender, String[] args) {
        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            target = (Player) sender;
        }

        int ping = target.getPing();
        MessageUtil.sendConfigMessage(sender, "ping", "{target}", target.getName(), "{ping}", String.valueOf(ping));
        return true;
    }

    private boolean handleNear(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.near")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        int radius = 100;
        if (args.length > 0) {
            try {
                radius = Math.min(500, Math.max(10, Integer.parseInt(args[0])));
            } catch (NumberFormatException ignored) {}
        }

        MessageUtil.sendConfigMessage(player, "near-header", "{radius}", String.valueOf(radius));
        boolean found = false;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (entity instanceof Player && !entity.equals(player)) {
                Player other = (Player) entity;
                if (!plugin.getPlayerDataManager().isVanished(other.getUniqueId()) || player.hasPermission("bloomeryx.vanish.see")) {
                    double dist = Math.round(player.getLocation().distance(other.getLocation()) * 10.0) / 10.0;
                    MessageUtil.sendConfigMessage(player, "near-entry", "{target}", other.getName(), "{distance}", String.valueOf(dist));
                    found = true;
                }
            }
        }

        if (!found) {
            MessageUtil.sendConfigMessage(player, "near-none");
        }
        return true;
    }

    private boolean handleRepair(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("bloomeryx.command.repair")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        boolean all = args.length > 0 && args[0].equalsIgnoreCase("all") && player.hasPermission("bloomeryx.command.repair.all");

        if (all) {
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (repairItem(item)) count++;
            }
            if (count > 0) {
                MessageUtil.sendConfigMessage(player, "repair-success");
                MessageUtil.playSuccess(player);
            } else {
                MessageUtil.sendConfigMessage(player, "repair-failed");
                MessageUtil.playError(player);
            }
            return true;
        }

        ItemStack hand = player.getInventory().getItemInMainHand();
        if (repairItem(hand)) {
            MessageUtil.sendConfigMessage(player, "repair-success");
            MessageUtil.playSuccess(player);
        } else {
            MessageUtil.sendConfigMessage(player, "repair-failed");
            MessageUtil.playError(player);
        }
        return true;
    }

    private boolean handleKill(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.kill")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        Player target;
        if (args.length > 0) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
                return true;
            }
        } else {
            if (!(sender instanceof Player)) {
                MessageUtil.sendConfigMessage(sender, "player-only");
                return true;
            }
            target = (Player) sender;
        }

        target.setHealth(0.0);
        if (target.equals(sender)) {
            MessageUtil.sendMessage(target, "&#FF4B4BYou killed yourself.");
        } else {
            MessageUtil.sendMessage(sender, "&#70E000✔ &aKilled player &f" + target.getName() + " &asuccessfully.");
            MessageUtil.sendMessage(target, "&#FF4B4BYou were killed by &f" + sender.getName());
        }
        return true;
    }

    private boolean handleSmite(CommandSender sender, String[] args) {
        if (!sender.hasPermission("bloomeryx.command.smite")) {
            MessageUtil.sendConfigMessage(sender, "no-permission");
            return true;
        }

        if (args.length > 0) {
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                MessageUtil.sendConfigMessage(sender, "player-not-found", "{target}", args[0]);
                return true;
            }
            target.getWorld().strikeLightning(target.getLocation());
            MessageUtil.sendMessage(sender, "&#70E000✔ &aSmote &f" + target.getName() + " &awith lightning!");
            if (sender instanceof Player) MessageUtil.playSuccess((Player) sender);
            return true;
        }

        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        org.bukkit.block.Block targetBlock = player.getTargetBlockExact(100);
        org.bukkit.Location strikeLoc = targetBlock != null ? targetBlock.getLocation() : player.getLocation();
        player.getWorld().strikeLightning(strikeLoc);
        MessageUtil.sendMessage(player, "&#70E000✔ &aStruck target location with lightning!");
        MessageUtil.playSuccess(player);
        return true;
    }

    private boolean repairItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof Damageable) {
            Damageable damageable = (Damageable) meta;
            if (damageable.hasDamage()) {
                damageable.setDamage(0);
                item.setItemMeta(meta);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        String name = command.getName().toLowerCase();
        if (Arrays.asList("fly", "heal", "feed", "god", "clear", "ping", "kill", "smite").contains(name) && args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) list.add(p.getName());
            }
        } else if (name.equals("repair") && args.length == 1) {
            for (String s : Arrays.asList("hand", "all")) {
                if (s.startsWith(args[0].toLowerCase())) list.add(s);
            }
        }
        return list;
    }
}

