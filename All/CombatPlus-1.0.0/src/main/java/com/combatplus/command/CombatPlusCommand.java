package com.combatplus.command;

import com.combatplus.CombatPlus;
import com.combatplus.config.ConfigManager;
import com.combatplus.service.SwordService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CombatPlusCommand implements BasicCommand {

    private final CombatPlus plugin;
    private final ConfigManager configManager;
    private final SwordService swordService;

    private final List<String> swordNames = Arrays.asList(
            "NETHERITE_SWORD",
            "DIAMOND_SWORD",
            "IRON_SWORD",
            "GOLDEN_SWORD",
            "STONE_SWORD",
            "WOODEN_SWORD"
    );

    public CombatPlusCommand(CombatPlus plugin, ConfigManager configManager, SwordService swordService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.swordService = swordService;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();
        String label = "combatplus";

        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender, label);
            return;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("combatplus.admin")) {
                    sender.sendMessage(configManager.getPrefix() + configManager.getNoPermission());
                    return;
                }
                configManager.load();
                // Update all online players' swords
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (plugin.isPlayerBlockingEnabled(player)) {
                        swordService.updatePlayerSwords(player);
                    }
                }
                sender.sendMessage(configManager.getPrefix() + configManager.getReloadSuccess());
            }
            case "toggle" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(configManager.getPrefix() + ChatColor.RED + "Only players can use this command.");
                    return;
                }
                if (!player.hasPermission("combatplus.toggle")) {
                    player.sendMessage(configManager.getPrefix() + configManager.getNoPermission());
                    return;
                }

                boolean current = plugin.isPlayerBlockingEnabled(player);
                plugin.setPlayerBlockingEnabled(player, !current);

                if (!current) {
                    swordService.updatePlayerSwords(player);
                    player.sendMessage(configManager.getPrefix() + configManager.getToggleOn());
                } else {
                    player.sendMessage(configManager.getPrefix() + configManager.getToggleOff());
                }
            }
            case "give" -> {
                if (!sender.hasPermission("combatplus.admin")) {
                    sender.sendMessage(configManager.getPrefix() + configManager.getNoPermission());
                    return;
                }
                if (args.length < 2) {
                    sender.sendMessage(configManager.getPrefix() + ChatColor.RED + "Usage: /" + label + " give <player> [sword_type]");
                    return;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(configManager.getPrefix() + ChatColor.RED + "Player not found: " + args[1]);
                    return;
                }

                Material mat = Material.DIAMOND_SWORD;
                if (args.length >= 3) {
                    try {
                        mat = Material.valueOf(args[2].toUpperCase());
                        if (!swordService.isSwordMaterial(mat)) {
                            sender.sendMessage(configManager.getPrefix() + ChatColor.RED + args[2] + " is not a valid sword!");
                            return;
                        }
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(configManager.getPrefix() + ChatColor.RED + "Unknown sword type: " + args[2]);
                        return;
                    }
                }

                ItemStack sword = swordService.createBlockingSword(mat);
                target.getInventory().addItem(sword);

                String msg = configManager.getSwordGiven()
                        .replace("%type%", mat.name())
                        .replace("%player%", target.getName());
                sender.sendMessage(configManager.getPrefix() + msg);
            }
            case "info" -> {
                sender.sendMessage(ChatColor.GOLD + "========== [ CombatPlus v1.0.0 ] ==========");
                sender.sendMessage(ChatColor.GREEN + "Author: " + ChatColor.WHITE + "Made By FawayMC");
                sender.sendMessage(ChatColor.YELLOW + "Status: " + (configManager.isEnabled() ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
                sender.sendMessage(ChatColor.YELLOW + "Damage Reduction: " + ChatColor.AQUA + configManager.getDamageReductionPercent() + "%");
                sender.sendMessage(ChatColor.YELLOW + "Projectile Deflection: " + ChatColor.AQUA + configManager.isDeflectProjectiles());
                sender.sendMessage(ChatColor.YELLOW + "Knockback Reduction: " + ChatColor.AQUA + configManager.getKnockbackReductionPercent() + "%");
                sender.sendMessage(ChatColor.YELLOW + "Durability Loss: " + ChatColor.AQUA + configManager.getDurabilityDamage());
                sender.sendMessage(ChatColor.GOLD + "===========================================");
            }
            default -> sendHelp(sender, label);
        }
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ChatColor.GOLD + "========== [ CombatPlus - Made By FawayMC ] ==========");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " help " + ChatColor.GRAY + "- Show help menu");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " toggle " + ChatColor.GRAY + "- Toggle sword blocking on/off for yourself");
        sender.sendMessage(ChatColor.YELLOW + "/" + label + " info " + ChatColor.GRAY + "- View plugin settings & stats");
        if (sender.hasPermission("combatplus.admin")) {
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " reload " + ChatColor.GRAY + "- Reload config.yml");
            sender.sendMessage(ChatColor.YELLOW + "/" + label + " give <player> [sword_type] " + ChatColor.GRAY + "- Give an upgraded blocking sword");
        }
        sender.sendMessage(ChatColor.GOLD + "======================================================");
    }

    @Override
    public Collection<String> suggest(CommandSourceStack stack, String[] args) {
        CommandSender sender = stack.getSender();

        if (args.length <= 1) {
            List<String> subs = new ArrayList<>(Arrays.asList("help", "toggle", "info"));
            if (sender.hasPermission("combatplus.admin")) {
                subs.add("reload");
                subs.add("give");
            }
            String current = args.length == 1 ? args[0] : "";
            return filterMatches(subs, current);
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("combatplus.admin")) return Collections.emptyList();
            List<String> players = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            return filterMatches(players, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            if (!sender.hasPermission("combatplus.admin")) return Collections.emptyList();
            return filterMatches(swordNames, args[2]);
        }

        return Collections.emptyList();
    }

    private List<String> filterMatches(List<String> list, String query) {
        String lower = query.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
