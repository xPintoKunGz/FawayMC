package com.bloomerymc.bloomeryx.commands;

import com.bloomerymc.bloomeryx.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

import java.util.ArrayList;
import java.util.List;

public class WorkbenchCommands implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            MessageUtil.sendConfigMessage(sender, "player-only");
            return true;
        }

        Player player = (Player) sender;
        String name = command.getName().toLowerCase();

        switch (name) {
            case "workbench":
            case "craft":
            case "wb":
                if (!player.hasPermission("bloomeryx.command.workbench")) {
                    MessageUtil.sendConfigMessage(sender, "no-permission");
                    return true;
                }
                player.openWorkbench(null, true);
                MessageUtil.playSuccess(player);
                return true;

            case "enderchest":
            case "ec":
                if (args.length > 0 && player.hasPermission("bloomeryx.command.enderchest.others")) {
                    Player target = Bukkit.getPlayer(args[0]);
                    if (target == null) {
                        MessageUtil.sendConfigMessage(player, "player-not-found", "{target}", args[0]);
                        return true;
                    }
                    player.openInventory(target.getEnderChest());
                } else {
                    if (!player.hasPermission("bloomeryx.command.enderchest")) {
                        MessageUtil.sendConfigMessage(sender, "no-permission");
                        return true;
                    }
                    player.openInventory(player.getEnderChest());
                }
                MessageUtil.playSuccess(player);
                return true;

            case "anvil":
                if (!player.hasPermission("bloomeryx.command.anvil")) {
                    MessageUtil.sendConfigMessage(sender, "no-permission");
                    return true;
                }
                player.openInventory(Bukkit.createInventory(player, InventoryType.ANVIL));
                MessageUtil.playSuccess(player);
                return true;

            case "smithing":
                if (!player.hasPermission("bloomeryx.command.smithing")) {
                    MessageUtil.sendConfigMessage(sender, "no-permission");
                    return true;
                }
                player.openInventory(Bukkit.createInventory(player, InventoryType.SMITHING));
                MessageUtil.playSuccess(player);
                return true;

            case "cartography":
                if (!player.hasPermission("bloomeryx.command.cartography")) {
                    MessageUtil.sendConfigMessage(sender, "no-permission");
                    return true;
                }
                player.openInventory(Bukkit.createInventory(player, InventoryType.CARTOGRAPHY));
                MessageUtil.playSuccess(player);
                return true;

            case "grindstone":
                if (!player.hasPermission("bloomeryx.command.grindstone")) {
                    MessageUtil.sendConfigMessage(sender, "no-permission");
                    return true;
                }
                player.openInventory(Bukkit.createInventory(player, InventoryType.GRINDSTONE));
                MessageUtil.playSuccess(player);
                return true;

            case "loom":
                if (!player.hasPermission("bloomeryx.command.loom")) {
                    MessageUtil.sendConfigMessage(sender, "no-permission");
                    return true;
                }
                player.openInventory(Bukkit.createInventory(player, InventoryType.LOOM));
                MessageUtil.playSuccess(player);
                return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if ((command.getName().equalsIgnoreCase("enderchest") || command.getName().equalsIgnoreCase("ec")) && args.length == 1) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase())) list.add(p.getName());
            }
        }
        return list;
    }
}

