package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.Team;
import com.bedwars.arena.TeamColor;
import com.bedwars.util.ItemBuilder;
import com.bedwars.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PracticeGUI {

    public static final String GUI_TITLE_PREFIX = "§8✦ §6§lBOT PRACTICE §8| §f";

    // Tracking selected teams per player
    private static final Map<UUID, Set<TeamColor>> selectedBotTeams = new ConcurrentHashMap<>();
    private static final Map<UUID, Arena> selectedArena = new ConcurrentHashMap<>();

    public static void open(Player player, Arena arena) {
        if (arena == null) return;

        selectedArena.put(player.getUniqueId(), arena);

        Team playerTeam = arena.getPlayerTeam(player.getUniqueId());
        List<Team> allTeams = new ArrayList<>(arena.getTeams().values());
        if (playerTeam == null && !allTeams.isEmpty()) {
            playerTeam = allTeams.get(0);
        }

        final Team pTeam = playerTeam;
        List<Team> enemyTeams = allTeams.stream().filter(t -> !t.equals(pTeam)).toList();

        // Initialize default selected teams: by default, select 1 enemy team if none selected
        Set<TeamColor> currentSelection = selectedBotTeams.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>());
        if (currentSelection.isEmpty() && !enemyTeams.isEmpty()) {
            currentSelection.add(enemyTeams.get(0).getColor());
        }

        Inventory inv = Bukkit.createInventory(null, 45, GUI_TITLE_PREFIX + arena.getDisplayName());

        // Fillers
        ItemStack blackGlass = new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE).name(" ").build();
        ItemStack grayGlass = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, blackGlass);
        for (int i = 36; i < 45; i++) inv.setItem(i, blackGlass);
        inv.setItem(9, grayGlass);
        inv.setItem(17, grayGlass);
        inv.setItem(18, grayGlass);
        inv.setItem(26, grayGlass);
        inv.setItem(27, grayGlass);
        inv.setItem(35, grayGlass);

        // Header (Slot 4)
        inv.setItem(4, new ItemBuilder(Material.BEACON)
                .name("§6§l✦ Arena: §e" + MessageUtil.color(arena.getDisplayName()) + " ✦")
                .lore(
                        "§7Configure AI bot opponents for practice training.",
                        "§7Opponent Teams: §e" + enemyTeams.size() + " Enemy Team(s)",
                        "§7Your Assigned Team: " + (pTeam != null ? pTeam.getColor().getChatColor() + pTeam.getColor().getDisplayName() : "§fNone"),
                        "",
                        "§b▶ Select preset below or toggle specific team wool!"
                ).build());

        // Quick Preset Buttons on Row 1 (Slots 10, 12, 14, 16)
        // Preset 1: 1 Enemy Team
        inv.setItem(10, new ItemBuilder(Material.IRON_SWORD)
                .name("§a§l1 Enemy Team (1v1 Practice)")
                .lore(
                        "§7Spawn 1 opposing AI bot team.",
                        "§7Great for 1v1 speedrun, rushing, and duel training!",
                        "",
                        "§e▶ Click to select 1 team"
                ).build());

        // Preset 2: 2 Enemy Teams
        if (enemyTeams.size() >= 2) {
            inv.setItem(12, new ItemBuilder(Material.GOLDEN_SWORD)
                    .name("§e§l2 Enemy Teams")
                    .lore(
                            "§7Spawn 2 opposing AI bot teams.",
                            "§7Practice defending against 2 sides simultaneously!",
                            "",
                            "§e▶ Click to select 2 teams"
                    ).build());
        }

        // Preset 3: 3 Enemy Teams
        if (enemyTeams.size() >= 3) {
            inv.setItem(14, new ItemBuilder(Material.DIAMOND_SWORD)
                    .name("§b§l3 Enemy Teams")
                    .lore(
                            "§7Spawn 3 opposing AI bot teams.",
                            "§7Intense multi-team training!",
                            "",
                            "§e▶ Click to select 3 teams"
                    ).build());
        }

        // Preset 4: All Enemy Teams
        inv.setItem(16, new ItemBuilder(Material.NETHERITE_SWORD)
                .name("§c§lALL Enemy Teams (" + enemyTeams.size() + " Teams)")
                .lore(
                        "§7Fill EVERY enemy team with AI bots!",
                        "§7Full real-match practice experience!",
                        "",
                        "§e▶ Click to select ALL teams"
                ).build());

        // Row 2: Team Wool Toggles (Slots 20 to 25 or centered)
        int woolSlot = 20;
        for (Team t : enemyTeams) {
            if (woolSlot > 25) break;
            boolean enabled = currentSelection.contains(t.getColor());
            ItemBuilder woolIb = new ItemBuilder(t.getColor().getWoolMaterial())
                    .name(t.getColor().getChatColor() + "● " + t.getColor().getDisplayName() + " Team")
                    .lore(
                            "§7AI Bot Opponent: " + (enabled ? "§a✔ ENABLED" : "§7✖ DISABLED"),
                            "",
                            enabled ? "§c▶ Click to Disable" : "§a▶ Click to Enable"
                    );
            if (enabled) {
                woolIb.enchant(Enchantment.LUCK_OF_THE_SEA, 1);
                woolIb.flags(ItemFlag.HIDE_ENCHANTS);
            }
            inv.setItem(woolSlot++, woolIb.build());
        }

        // Bottom Controls
        // Slot 36: Cancel / Close
        inv.setItem(36, new ItemBuilder(Material.BARRIER).name("§c§lCancel").build());

        // Slot 40: Start Practice Match
        int selectedCount = currentSelection.size();
        inv.setItem(40, new ItemBuilder(Material.EMERALD_BLOCK)
                .name("§a§l▶ START PRACTICE MATCH (" + selectedCount + " Bot Teams)")
                .lore(
                        "§7Click to start the match immediately!",
                        "§7Bots will bridge to mid, gather resources, buy gear,",
                        "§7break beds, and engage in realistic combat!",
                        "",
                        "§e▶ Click to play now!"
                ).build());

        player.openInventory(inv);
        MessageUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1.2f);
    }

    public static Arena getSelectedArena(UUID uuid) {
        return selectedArena.get(uuid);
    }

    public static Set<TeamColor> getSelectedBotTeams(UUID uuid) {
        return selectedBotTeams.computeIfAbsent(uuid, k -> new HashSet<>());
    }

    public static void clear(UUID uuid) {
        selectedArena.remove(uuid);
        selectedBotTeams.remove(uuid);
    }
}

