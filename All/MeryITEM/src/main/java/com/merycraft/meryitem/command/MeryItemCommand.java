package com.merycraft.meryitem.command;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.effect.ItemEffect;
import com.merycraft.meryitem.gui.ItemBrowserGUI;
import com.merycraft.meryitem.gui.ItemEditorGUI;
import com.merycraft.meryitem.gui.StatsGUI;
import com.merycraft.meryitem.item.*;
import com.merycraft.meryitem.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Modern, clean, and intuitive command handler for MeryITEM (100% English).
 */
public class MeryItemCommand implements CommandExecutor {

    private final MeryItemPlugin plugin;

    public MeryItemCommand(MeryItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (label.equalsIgnoreCase("mupgrade") && sender instanceof Player) {
            new com.merycraft.meryitem.gui.UpgradeStationGUI(plugin, (Player) sender).open();
            return true;
        }

        // Default /mitem: Open In-Hand Item Editor GUI
        if (args.length == 0) {
            if (sender instanceof Player) {
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand first!"));
                    player.sendMessage(ColorUtil.color("&#94A3B8(Type &#F472B6/" + label + " browse &#94A3B8to view saved items catalog)"));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
                    return true;
                }
                new ItemEditorGUI(plugin, player).open();
            } else {
                sendHelp(sender, label);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "create": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185This command can only be used by players."));
                    return true;
                }
                Player player = (Player) sender;
                if (args.length == 1) {
                    ItemStack held = player.getInventory().getItemInMainHand();
                    org.bukkit.Material baseMat = (held != null && !held.getType().isAir()) ? held.getType() : org.bukkit.Material.DIAMOND_SWORD;
                    com.merycraft.meryitem.gui.ChatInputListener.startPrompt(player, "Type ID for new item (e.g. 'MY_SWORD'):", input -> {
                        String cleanId = input.trim().toUpperCase().replace(" ", "_");
                        if (cleanId.isEmpty()) cleanId = "ITEM_" + (System.currentTimeMillis() % 100000);
                        String dispName = (held != null && held.hasItemMeta() && held.getItemMeta().hasDisplayName())
                                ? held.getItemMeta().getDisplayName() : ("&#F472B6✦ " + cleanId);
                        CustomItem newItem = new CustomItem(cleanId, baseMat, dispName, ItemRarity.COMMON, inferType(baseMat));
                        plugin.getItemManager().saveCustomItem(newItem);
                        player.getInventory().setItemInMainHand(newItem.buildItemStack(1));
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                        player.sendMessage(ColorUtil.color("&#34D399✔ Created custom item &#CBD5E1" + cleanId + " &#34D399and opened editor!"));
                        new ItemEditorGUI(plugin, player).open();
                    });
                    return true;
                }

                String id = args[1].toUpperCase().replace(" ", "_");
                org.bukkit.Material mat = null;
                if (args.length >= 3) {
                    mat = org.bukkit.Material.matchMaterial(args[2].toUpperCase());
                }
                if (mat == null) {
                    ItemStack held = player.getInventory().getItemInMainHand();
                    mat = (held != null && !held.getType().isAir()) ? held.getType() : org.bukkit.Material.DIAMOND_SWORD;
                }

                String dispName;
                if (args.length >= 4) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 3; i < args.length; i++) {
                        sb.append(args[i]).append(" ");
                    }
                    dispName = sb.toString().trim();
                } else {
                    dispName = "&#F472B6✦ " + id;
                }

                CustomItem newItem = new CustomItem(id, mat, dispName, ItemRarity.COMMON, inferType(mat));
                plugin.getItemManager().saveCustomItem(newItem);
                player.getInventory().setItemInMainHand(newItem.buildItemStack(1));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                player.sendMessage(ColorUtil.color("&#34D399✔ Successfully created custom item &#CBD5E1" + id + "! Opening editor..."));
                new ItemEditorGUI(plugin, player).open();
                return true;
            }

            case "folder": {
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " folder <list/create/set> [name]"));
                    return true;
                }
                String action = args[1].toLowerCase();
                if (action.equals("list")) {
                    sender.sendMessage(ColorUtil.color("&#F472B6§lMeryITEM &#334155» &#CBD5E1Registered Folders:"));
                    for (String fName : plugin.getItemManager().getAllFolders()) {
                        int count = plugin.getItemManager().getItemsByFolder(fName).size();
                        sender.sendMessage(ColorUtil.color(" &#FDE047📁 " + fName + " &#8E9AAF(" + count + " items)"));
                    }
                    return true;
                } else if (action.equals("create")) {
                    if (args.length < 3) {
                        sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " folder create <name>"));
                        return true;
                    }
                    String folderName = args[2].trim().replace(" ", "_");
                    plugin.getItemManager().createFolder(folderName);
                    sender.sendMessage(ColorUtil.color("&#34D399✔ Created folder &#FDE047" + folderName + "&#34D399!"));
                    return true;
                } else if (action.equals("set")) {
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ColorUtil.color("&#FB7185This command can only be used by players."));
                        return true;
                    }
                    if (args.length < 3) {
                        sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " folder set <name>"));
                        return true;
                    }
                    Player player = (Player) sender;
                    ItemStack held = player.getInventory().getItemInMainHand();
                    if (held == null || held.getType().isAir()) {
                        player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand first."));
                        return true;
                    }
                    String folderName = args[2].trim().replace(" ", "_");
                    CustomItem cItem = getOrCreateHeldCustomItem(held);
                    cItem.setFolder(folderName);
                    plugin.getItemManager().createFolder(folderName);
                    plugin.getItemManager().saveCustomItem(cItem);
                    player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.8f, 1.4f);
                    player.sendMessage(ColorUtil.color("&#34D399✔ Assigned held item to folder: &#FDE047" + folderName));
                    return true;
                } else {
                    sender.sendMessage(ColorUtil.color("&#FB7185Unknown folder action. Use: list, create, set"));
                    return true;
                }
            }

            case "edit": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185This command can only be used by players."));
                    return true;
                }
                new ItemEditorGUI(plugin, (Player) sender).open();
                return true;
            }

            case "browse":
            case "catalog": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185This command can only be used by players."));
                    return true;
                }
                new ItemBrowserGUI(plugin, (Player) sender).open();
                return true;
            }

            case "get": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185This command can only be used by players."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " get <item_id> [amount]"));
                    return true;
                }
                Player player = (Player) sender;
                String id = args[1];
                CustomItem cItem = plugin.getItemManager().getItem(id);
                if (cItem == null) {
                    player.sendMessage(ColorUtil.color("&#FB7185Item with ID '&#F43F5E" + id + "&#FB7185' not found."));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.5f);
                    return true;
                }
                int amount = 1;
                if (args.length >= 3) {
                    try {
                        amount = Integer.parseInt(args[2]);
                    } catch (NumberFormatException ignored) {}
                }
                player.getInventory().addItem(cItem.buildItemStack(amount));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.3f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Received &#F472B6" + amount + "x " + cItem.getDisplayName()));
                return true;
            }

            case "give": {
                if (!sender.hasPermission("meryitem.admin")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185You do not have permission to use this command."));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " give <player> <item_id> [amount]"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Player '&#F43F5E" + args[1] + "&#FB7185' is not online."));
                    return true;
                }
                String id = args[2];
                CustomItem cItem = plugin.getItemManager().getItem(id);
                if (cItem == null) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Item with ID '&#F43F5E" + id + "&#FB7185' not found."));
                    return true;
                }
                int amount = 1;
                if (args.length >= 4) {
                    try {
                        amount = Integer.parseInt(args[3]);
                    } catch (NumberFormatException ignored) {}
                }
                target.getInventory().addItem(cItem.buildItemStack(amount));
                target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.7f, 1.3f);
                target.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1You were given &#F472B6" + amount + "x " + cItem.getDisplayName()));
                sender.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Gave &#F472B6" + amount + "x " + cItem.getDisplayName() + " &#CBD5E1to &#F472B6" + target.getName()));
                return true;
            }

            case "title":
            case "name": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Only players can use /" + label + " name."));
                    return true;
                }
                if (!sender.hasPermission("meryitem.admin")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185No permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " name <title text...> or /" + label + " name clear"));
                    return true;
                }
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand."));
                    return true;
                }

                CustomItem cItem = getOrCreateHeldCustomItem(held);
                if (args[1].equalsIgnoreCase("clear")) {
                    cItem.setDisplayName(null);
                    player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.2f);
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Reset item name to default."));
                    return true;
                }

                String title = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                cItem.setDisplayName(title);
                player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));

                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Updated title: " + title));
                return true;
            }

            case "desc":
            case "lore": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Only players can use this command."));
                    return true;
                }
                if (!sender.hasPermission("meryitem.admin")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185No permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " desc <add/remove/clear> [text/line#]"));
                    return true;
                }
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand."));
                    return true;
                }

                String action = args[1].toLowerCase();
                CustomItem cItem = getOrCreateHeldCustomItem(held);

                if (action.equals("clear")) {
                    cItem.setCustomLore(new ArrayList<>());
                    player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Cleared all description lines."));
                    return true;
                } else if (action.equals("edit") || action.equals("set")) {
                    if (args.length < 4) {
                        player.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " desc edit <line_number> <new_text...>"));
                        return true;
                    }
                    try {
                        int lineNum = Integer.parseInt(args[2]);
                        List<String> current = new ArrayList<>(cItem.getCustomLore());
                        if (lineNum >= 1 && lineNum <= current.size()) {
                            String newText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                            current.set(lineNum - 1, newText);
                            cItem.setCustomLore(current);
                            player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                            player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.4f);
                            player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Updated Line #" + lineNum + ": &#CBD5E1" + newText));
                        } else {
                            player.sendMessage(ColorUtil.color("&#FB7185Line number out of bounds (1 - " + current.size() + ")"));
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtil.color("&#FB7185Please enter a valid line number."));
                    }
                    return true;
                } else if (action.equals("remove") || action.equals("delete")) {
                    if (args.length < 3) {
                        player.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " desc remove <line_number>"));
                        return true;
                    }
                    try {
                        int lineNum = Integer.parseInt(args[2]);
                        List<String> current = new ArrayList<>(cItem.getCustomLore());
                        if (lineNum >= 1 && lineNum <= current.size()) {
                            String removed = current.remove(lineNum - 1);
                            cItem.setCustomLore(current);
                            player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.2f);
                            player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Removed line #" + lineNum + ": &#94A3B8" + removed));
                        } else {
                            player.sendMessage(ColorUtil.color("&#FB7185Line number out of bounds (1 - " + current.size() + ")"));
                        }
                    } catch (NumberFormatException e) {
                        player.sendMessage(ColorUtil.color("&#FB7185Please enter a valid line number."));
                    }
                    return true;
                } else if (action.equals("add")) {
                    if (args.length < 3) {
                        player.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " desc add <text...>"));
                        return true;
                    }
                    String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    List<String> current = new ArrayList<>(cItem.getCustomLore());
                    current.add(text);
                    cItem.setCustomLore(current);

                    player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                    player.playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.2f);
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Added description line: &#CBD5E1" + text));
                    return true;
                }
                return true;
            }

            case "enchant": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Only players can use /" + label + " enchant."));
                    return true;
                }
                if (!sender.hasPermission("meryitem.admin")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185No permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " enchant <enchantment> [level]"));
                    return true;
                }
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand."));
                    return true;
                }

                String enchName = args[1].toLowerCase();
                Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(enchName));
                if (ench == null) ench = Enchantment.getByName(args[1].toUpperCase());
                if (ench == null) {
                    player.sendMessage(ColorUtil.color("&#FB7185Invalid enchantment name."));
                    return true;
                }

                int level = 1;
                if (args.length >= 3) {
                    try {
                        level = Math.max(1, Math.min(255, Integer.parseInt(args[2])));
                    } catch (NumberFormatException ignored) {}
                }

                CustomItem cItem = getOrCreateHeldCustomItem(held);
                cItem.setEnchantment(ench, level);
                player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Set enchantment &#C084FC" + ench.getKey().getKey() + " &#CBD5E1to level &#F472B6" + level + " (Max 255)"));
                return true;
            }

            case "upgrade":
            case "smith": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185This command can only be used by players."));
                    return true;
                }
                Player player = (Player) sender;
                if (args.length >= 2 && args[1].equalsIgnoreCase("create")) {
                    new com.merycraft.meryitem.gui.UpgradeStationGUI(plugin, player).openRecipeCreationWizard();
                    return true;
                }
                new com.merycraft.meryitem.gui.UpgradeStationGUI(plugin, player).open();
                return true;
            }

            case "potion": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Only players can use /" + label + " potion."));
                    return true;
                }
                if (!sender.hasPermission("meryitem.admin")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185No permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " potion <effect_name/trigger> [level 1-250 / hold/right/left]"));
                    return true;
                }
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand."));
                    return true;
                }

                CustomItem cItem = getOrCreateHeldCustomItem(held);
                if (args[1].equalsIgnoreCase("trigger")) {
                    if (args.length < 3) {
                        player.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " potion trigger <hold/right/left>"));
                        return true;
                    }
                    String trig = args[2].toUpperCase();
                    cItem.setPotionTrigger(trig);
                    player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Set potion effect trigger to &#38BDF8[" + cItem.getPotionTrigger() + "]"));
                    return true;
                }

                org.bukkit.potion.PotionEffectType pet = org.bukkit.potion.PotionEffectType.getByName(args[1].toUpperCase());
                if (pet == null) pet = org.bukkit.potion.PotionEffectType.getByKey(NamespacedKey.minecraft(args[1].toLowerCase()));
                if (pet == null) {
                    player.sendMessage(ColorUtil.color("&#FB7185Unknown potion effect name."));
                    return true;
                }

                int level = 1;
                if (args.length >= 3) {
                    try {
                        level = Math.max(0, Math.min(250, Integer.parseInt(args[2])));
                    } catch (NumberFormatException ignored) {}
                }

                if (level <= 0) {
                    cItem.removePotionEffect(pet);
                    player.sendMessage(ColorUtil.color("&#FB7185Removed potion effect: " + pet.getName()));
                } else {
                    cItem.setPotionEffect(pet, level);
                    player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.4f);
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Set potion effect &#34D399" + pet.getName() + " &#CBD5E1to level &#F472B6" + level + " (Duration: 9999s, Max 250)"));
                }
                player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                return true;
            }

            case "command":
            case "cmd": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Only players can use /" + label + " command."));
                    return true;
                }
                if (!sender.hasPermission("meryitem.admin")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185No permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " command <add/click/clear> [command/right/left]"));
                    return true;
                }
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand."));
                    return true;
                }

                CustomItem cItem = getOrCreateHeldCustomItem(held);
                String action = args[1].toLowerCase();
                if (action.equals("clear")) {
                    cItem.clearClickCommands();
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Cleared all commands on this item."));
                } else if (action.equals("click") || action.equals("trigger")) {
                    if (args.length < 3) {
                        player.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " command click <left/right>"));
                        return true;
                    }
                    String type = args[2].equalsIgnoreCase("left") ? "LEFT" : "RIGHT";
                    cItem.setCommandClickType(type);
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Command trigger set to &#38BDF8[" + type + "-CLICK]"));
                } else if (action.equals("add")) {
                    if (args.length < 3) {
                        player.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " command add <command...>"));
                        return true;
                    }
                    String cmd = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    cItem.addClickCommand(cmd);
                    cItem.addAbility(ItemAbility.RUN_COMMAND);
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Attached command: &#CBD5E1" + cmd + " &#64748B(Trigger: " + cItem.getCommandClickType() + "-CLICK)"));
                }
                player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                return true;
            }

            case "effect":
            case "aura": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Only players can use /" + label + " effect."));
                    return true;
                }
                if (!sender.hasPermission("meryitem.admin")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185No permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " effect <sakura/flame/soul_flame/starlight/void/divine/crit/heart/none>"));
                    return true;
                }
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand."));
                    return true;
                }

                ItemEffect effect = ItemEffect.fromString(args[1]);
                CustomItem cItem = getOrCreateHeldCustomItem(held);
                cItem.setEffect(effect);

                player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Applied aura effect: " + effect.getHexColor() + effect.getDisplayName()));
                return true;
            }

            case "rarity": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Only players can use this command."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " rarity <tier/toggle>"));
                    return true;
                }
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand."));
                    return true;
                }
                CustomItem cItem = getOrCreateHeldCustomItem(held);
                if (args[1].equalsIgnoreCase("toggle")) {
                    cItem.setShowRarity(!cItem.isShowRarity());
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Rarity display: " + (cItem.isShowRarity() ? "&#34D399ENABLED" : "&#64748BDISABLED")));
                } else {
                    ItemRarity r = ItemRarity.fromString(args[1]);
                    cItem.setRarity(r);
                    cItem.setShowRarity(true);
                    player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Set rarity to: " + r.getFormattedName()));
                }
                player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                return true;
            }

            case "stat": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Only players can use /" + label + " stat."));
                    return true;
                }
                if (!sender.hasPermission("meryitem.admin")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185No permission."));
                    return true;
                }
                if (args.length < 4 || !args[1].equalsIgnoreCase("set")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " stat set <stat_name> <value>"));
                    return true;
                }
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand."));
                    return true;
                }

                ItemStat stat = ItemStat.fromString(args[2]);
                if (stat == null) {
                    player.sendMessage(ColorUtil.color("&#FB7185Invalid stat name."));
                    return true;
                }

                double val;
                try {
                    val = Double.parseDouble(args[3]);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please enter a valid numeric value."));
                    return true;
                }

                CustomItem cItem = getOrCreateHeldCustomItem(held);
                cItem.setStat(stat, val);

                player.getInventory().setItemInMainHand(cItem.buildItemStack(held.getAmount()));
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.2f);
                player.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#CBD5E1Set " + stat.getHexColor() + stat.getDisplayName() + " &#CBD5E1to &#F472B6" + val));
                return true;
            }

            case "save": {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Only players can use /" + label + " save."));
                    return true;
                }
                if (!sender.hasPermission("meryitem.admin")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185No permission."));
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(ColorUtil.color("&#FB7185Usage: /" + label + " save <new_id>"));
                    return true;
                }
                Player player = (Player) sender;
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held == null || held.getType().isAir()) {
                    player.sendMessage(ColorUtil.color("&#FB7185Please hold an item in your main hand to save."));
                    return true;
                }
                String newId = args[1].toUpperCase();
                CustomItem cItem = getOrCreateHeldCustomItem(held);
                CustomItem savedItem = new CustomItem(newId, held.getType(), cItem.getDisplayName(), cItem.getRarity(), cItem.getType());
                savedItem.setShowRarity(cItem.isShowRarity());
                savedItem.setShowAbilityLore(cItem.isShowAbilityLore());
                savedItem.setEffect(cItem.getEffect());
                savedItem.setCustomLore(cItem.getCustomLore());
                savedItem.setCommandClickType(cItem.getCommandClickType());
                savedItem.setClickCommands(cItem.getClickCommands());
                savedItem.setPotionTrigger(cItem.getPotionTrigger());
                for (var entry : cItem.getPotionEffects().entrySet()) savedItem.setPotionEffect(entry.getKey(), entry.getValue());
                for (var entry : cItem.getStats().entrySet()) savedItem.setStat(entry.getKey(), entry.getValue());
                for (var entry : cItem.getEnchantments().entrySet()) savedItem.setEnchantment(entry.getKey(), entry.getValue());
                for (var ab : cItem.getAbilities()) savedItem.addAbility(ab);

                plugin.getItemManager().saveCustomItem(savedItem);
                player.getInventory().setItemInMainHand(savedItem.buildItemStack(held.getAmount()));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
                player.sendMessage(ColorUtil.color("&#34D399✔ Saved item as " + newId + " into /mitem browse catalog!"));
                return true;
            }

            case "list": {
                Collection<CustomItem> items = plugin.getItemManager().getAllItems();
                sender.sendMessage(ColorUtil.color("&#F472B6§lMeryITEM &#334155» &#CBD5E1Registered Items (&#F472B6" + items.size() + "&#CBD5E1):"));
                if (items.isEmpty()) {
                    sender.sendMessage(ColorUtil.color("&#94A3B8(No items saved yet. Use /" + label + " to create and save items!)"));
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                int count = 0;
                for (CustomItem it : items) {
                    if (count > 0) sb.append("&#334155, ");
                    sb.append(it.getRarity().getPrimaryColor()).append(it.getId());
                    count++;
                }
                sender.sendMessage(ColorUtil.color(sb.toString()));
                return true;
            }

            case "stats": {
                if (sender instanceof Player) {
                    new StatsGUI(plugin, (Player) sender).open();
                } else {
                    sender.sendMessage(ColorUtil.color("&#FB7185Use within game as a player."));
                }
                return true;
            }

            case "reload": {
                if (!sender.hasPermission("meryitem.admin")) {
                    sender.sendMessage(ColorUtil.color("&#FB7185No permission."));
                    return true;
                }
                plugin.reloadAll();
                sender.sendMessage(ColorUtil.color("&#F472B6MeryITEM &#8E9AAF» &#34D399Configurations reloaded successfully!"));
                if (sender instanceof Player) {
                    ((Player) sender).playSound(((Player) sender).getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.7f, 1.4f);
                }
                return true;
            }

            case "help":
            default: {
                sendHelp(sender, label);
                return true;
            }
        }
    }

    private CustomItem getOrCreateHeldCustomItem(ItemStack held) {
        CustomItem cItem = plugin.getItemManager().getFromItemStack(held);
        if (cItem == null) {
            String autoId = "ITEM_" + (System.currentTimeMillis() % 100000);
            String name = held.hasItemMeta() && held.getItemMeta().hasDisplayName()
                    ? held.getItemMeta().getDisplayName()
                    : "&#F472B6✦ " + held.getType().name();
            ItemType inferredType = inferType(held.getType());
            cItem = new CustomItem(autoId, held.getType(), name, ItemRarity.COMMON, inferredType);
            cItem.setShowRarity(false);
        }
        return cItem;
    }

    private ItemType inferType(org.bukkit.Material mat) {
        String name = mat.name();
        if (name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_TRIDENT") || name.endsWith("_MACE")) return ItemType.SWORD;
        if (name.endsWith("_BOW") || name.endsWith("CROSSBOW")) return ItemType.BOW;
        if (name.endsWith("_HELMET")) return ItemType.HELMET;
        if (name.endsWith("_CHESTPLATE")) return ItemType.CHESTPLATE;
        if (name.endsWith("_LEGGINGS")) return ItemType.LEGGINGS;
        if (name.endsWith("_BOOTS")) return ItemType.BOOTS;
        if (name.endsWith("SHIELD")) return ItemType.SHIELD;
        if (name.contains("POTION") || name.endsWith("FOOD") || mat.isEdible()) return ItemType.CONSUMABLE;
        return ItemType.MISC;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ColorUtil.color("&#334155§m                                                  "));
        sender.sendMessage(ColorUtil.color("&#F472B6§lMeryITEM &#334155» &#FDA4AFModern In-Hand Item Editor"));
        sender.sendMessage(ColorUtil.color("&#334155§m                                                  "));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " &#8E9AAF- &#CBD5E1Open In-Hand Item Editor GUI"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " create <id> [mat] [name] &#8E9AAF- &#CBD5E1Create & edit new custom item"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " folder <list/create/set> &#8E9AAF- &#CBD5E1Manage custom item folders"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " browse &#8E9AAF- &#CBD5E1Open saved items catalog"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " name <title/clear> &#8E9AAF- &#CBD5E1Set or clear item title"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " desc <add/remove/clear> &#8E9AAF- &#CBD5E1Manage description lines"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " enchant <type> [lvl] &#8E9AAF- &#CBD5E1Add enchantment (Max 255)"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " potion <type/trigger> [lvl/mode] &#8E9AAF- &#CBD5E1Potion effect (Hold/Right/Left)"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " upgrade &#8E9AAF- &#CBD5E1Open weapon Upgrade & Smithing Station"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " command <add/click/clear> &#8E9AAF- &#CBD5E1Attach click command (Left/Right)"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " effect <type> &#8E9AAF- &#CBD5E1Set held weapon particle aura"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " rarity <tier/toggle> &#8E9AAF- &#CBD5E1Set or toggle rarity display"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " save <id> &#8E9AAF- &#CBD5E1Save held item into browse catalog"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " get <id> [amt] &#8E9AAF- &#CBD5E1Take item from catalog"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " give <p> <id> [amt] &#8E9AAF- &#CBD5E1Give custom item to a player"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " stats &#8E9AAF- &#CBD5E1Inspect active bonuses"));
        sender.sendMessage(ColorUtil.color(" &#F472B6/" + label + " reload &#8E9AAF- &#CBD5E1Reload configs and items"));
        sender.sendMessage(ColorUtil.color("&#334155§m                                                  "));
    }
}
