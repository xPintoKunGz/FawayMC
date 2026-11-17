package com.merycraft.meryitem.command;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.effect.ItemEffect;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemAbility;
import com.merycraft.meryitem.item.ItemRarity;
import com.merycraft.meryitem.item.ItemStat;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MeryItemTabCompleter implements TabCompleter {

    private final MeryItemPlugin plugin;
    private static final List<String> SUBCOMMANDS = Arrays.asList(
            "browse", "create", "folder", "get", "give", "name", "desc", "enchant", "potion", "command", "effect", "rarity", "stat", "ability", "upgrade", "save", "list", "stats", "reload", "help"
    );

    public MeryItemTabCompleter(MeryItemPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], SUBCOMMANDS, completions);
            Collections.sort(completions);
            return completions;
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            if (sub.equals("get")) {
                List<String> itemIds = new ArrayList<>();
                for (CustomItem item : plugin.getItemManager().getAllItems()) {
                    itemIds.add(item.getId());
                }
                StringUtil.copyPartialMatches(args[1], itemIds, completions);
                Collections.sort(completions);
                return completions;
            } else if (sub.equals("give")) {
                List<String> playerNames = new ArrayList<>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    playerNames.add(player.getName());
                }
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
                Collections.sort(completions);
                return completions;
            } else if (sub.equals("enchant")) {
                List<String> enchList = new ArrayList<>();
                for (Enchantment e : Enchantment.values()) {
                    enchList.add(e.getKey().getKey().toLowerCase());
                }
                StringUtil.copyPartialMatches(args[1], enchList, completions);
                Collections.sort(completions);
                return completions;
            } else if (sub.equals("potion")) {
                List<String> potList = new ArrayList<>(Arrays.asList("trigger"));
                for (PotionEffectType pet : PotionEffectType.values()) {
                    if (pet != null) potList.add(pet.getName().toLowerCase());
                }
                StringUtil.copyPartialMatches(args[1], potList, completions);
                Collections.sort(completions);
                return completions;
            } else if (sub.equals("command") || sub.equals("cmd")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("add", "click", "clear"), completions);
                return completions;
            } else if (sub.equals("effect") || sub.equals("aura")) {
                List<String> effects = new ArrayList<>();
                for (ItemEffect eff : ItemEffect.values()) {
                    effects.add(eff.name().toLowerCase());
                }
                StringUtil.copyPartialMatches(args[1], effects, completions);
                Collections.sort(completions);
                return completions;
            } else if (sub.equals("desc") || sub.equals("lore")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("add", "edit", "remove", "clear"), completions);
                return completions;
            } else if (sub.equals("name") || sub.equals("title")) {
                StringUtil.copyPartialMatches(args[1], Collections.singletonList("clear"), completions);
                return completions;
            } else if (sub.equals("rarity")) {
                List<String> rarities = new ArrayList<>(Arrays.asList("toggle"));
                for (ItemRarity r : ItemRarity.values()) {
                    rarities.add(r.name().toLowerCase());
                }
                StringUtil.copyPartialMatches(args[1], rarities, completions);
                return completions;
            } else if (sub.equals("folder")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("list", "create", "set"), completions);
                return completions;
            } else if (sub.equals("create")) {
                return Collections.singletonList("<item_id>");
            } else if (sub.equals("stat")) {
                StringUtil.copyPartialMatches(args[1], Collections.singletonList("set"), completions);
                return completions;
            } else if (sub.equals("ability")) {
                StringUtil.copyPartialMatches(args[1], Collections.singletonList("add"), completions);
                return completions;
            } else if (sub.equals("upgrade") || sub.equals("smith")) {
                StringUtil.copyPartialMatches(args[1], Arrays.asList("create", "reset"), completions);
                return completions;
            }
        }

        if (args.length == 3) {
            if (sub.equals("folder") && args[1].equalsIgnoreCase("set")) {
                StringUtil.copyPartialMatches(args[2], plugin.getItemManager().getAllFolders(), completions);
                Collections.sort(completions);
                return completions;
            } else if (sub.equals("create")) {
                List<String> mats = Arrays.asList("DIAMOND_SWORD", "NETHERITE_SWORD", "BOW", "CROSSBOW", "DIAMOND_AXE", "NETHERITE_AXE", "SHIELD", "NETHERITE_CHESTPLATE", "FEATHER", "STICK", "BOOK");
                StringUtil.copyPartialMatches(args[2], mats, completions);
                Collections.sort(completions);
                return completions;
            } else if (sub.equals("give")) {
                List<String> itemIds = new ArrayList<>();
                for (CustomItem item : plugin.getItemManager().getAllItems()) {
                    itemIds.add(item.getId());
                }
                StringUtil.copyPartialMatches(args[2], itemIds, completions);
                Collections.sort(completions);
                return completions;
            } else if (sub.equals("get")) {
                return Arrays.asList("1", "16", "32", "64");
            } else if (sub.equals("enchant")) {
                return Arrays.asList("1", "5", "10", "100", "255");
            } else if (sub.equals("potion")) {
                if (args[1].equalsIgnoreCase("trigger")) {
                    return Arrays.asList("hold", "right", "left");
                }
                return Arrays.asList("1", "2", "5", "50", "100", "250");
            } else if (sub.equals("command") && args[1].equalsIgnoreCase("click")) {
                return Arrays.asList("left", "right");
            } else if (sub.equals("stat") && args[1].equalsIgnoreCase("set")) {
                List<String> stats = new ArrayList<>();
                for (ItemStat st : ItemStat.values()) {
                    stats.add(st.name().toLowerCase());
                }
                StringUtil.copyPartialMatches(args[2], stats, completions);
                Collections.sort(completions);
                return completions;
            } else if (sub.equals("ability") && args[1].equalsIgnoreCase("add")) {
                List<String> abs = new ArrayList<>();
                for (ItemAbility ab : ItemAbility.values()) {
                    abs.add(ab.name().toLowerCase());
                }
                StringUtil.copyPartialMatches(args[2], abs, completions);
                Collections.sort(completions);
                return completions;
            }
        }

        if (args.length == 4) {
            if (sub.equals("give")) {
                return Arrays.asList("1", "16", "32", "64");
            } else if (sub.equals("stat") && args[1].equalsIgnoreCase("set")) {
                return Arrays.asList("10", "50", "100", "500", "1000");
            }
        }

        return Collections.emptyList();
    }
}
