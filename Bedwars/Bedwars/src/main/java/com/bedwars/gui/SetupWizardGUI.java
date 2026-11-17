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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SetupWizardGUI {

    public static final String SETUP_TITLE_PREFIX = "§8✦ §6§lArena Setup §8» §e";

    public static void open(Player player, Arena arena) {
        Inventory inv = Bukkit.createInventory(null, 54, SETUP_TITLE_PREFIX + arena.getName());

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        // Header info: Map ID & Display Name
        inv.setItem(4, new ItemBuilder(Material.NAME_TAG)
                .name("§6§l✦ Arena Info: " + MessageUtil.color(arena.getDisplayName()) + " ✦")
                .lore(
                        "§7Arena ID: §f" + arena.getName(),
                        "§7Display Name: §e" + MessageUtil.color(arena.getDisplayName()),
                        "§7Mode: §b" + arena.getMode() + " §7(" + arena.getPlayersPerTeam() + " players/team)",
                        "§7Configured Teams: §a" + arena.getTeams().size() + " §7/ 16 colors",
                        "",
                        "§e▶ Click to rename display name"
                ).build());

        // 1. Set Lobby
        boolean hasLobby = arena.getLobbyLocation() != null;
        inv.setItem(10, new ItemBuilder(Material.COMPASS)
                .name("§6§l1. Waiting Lobby Spawn")
                .lore(
                        "§7Stand at the waiting lobby location",
                        "§7and click here to save coordinates!",
                        "",
                        hasLobby ? "§a✔ Configured: §f" + arena.getLobbyLocation().getBlockX() + ", " + arena.getLobbyLocation().getBlockY() + ", " + arena.getLobbyLocation().getBlockZ()
                                : "§c✖ Not set (Click to set to current position)"
                ).build());

        // 2. Set Spectator Spawn
        boolean hasSpec = arena.getSpectatorLocation() != null;
        inv.setItem(12, new ItemBuilder(Material.ENDER_EYE)
                .name("§b§l2. Spectator Spawn")
                .lore(
                        "§7Stand at the aerial spectator spawn point",
                        "§7and click here to save coordinates!",
                        "",
                        hasSpec ? "§a✔ Configured: §f" + arena.getSpectatorLocation().getBlockX() + ", " + arena.getSpectatorLocation().getBlockY() + ", " + arena.getSpectatorLocation().getBlockZ()
                                : "§c✖ Not set (Click to set to current position)"
                ).build());

        // 3. Game Mode & Team Size
        inv.setItem(14, new ItemBuilder(Material.PAINTING)
                .name("§e§l3. Game Mode & Team Size")
                .lore(
                        "§7Current Mode: §b§l" + arena.getMode(),
                        "§7Players per team: §e" + arena.getPlayersPerTeam(),
                        "§7Max total players: §f" + arena.getMaxPlayers(),
                        "",
                        "§e▶ Left-Click: Cycle mode (Solo ➔ Doubles ➔ 3v3v3v3 ➔ 4v4v4v4 ➔ 8v8)",
                        "§b▶ Right-Click: Adjust players per team (1-8)"
                ).build());

        // 4. Center Spawners
        inv.setItem(16, new ItemBuilder(Material.BEACON)
                .name("§b§l4. Center Spawners (Diamond & Emerald)")
                .lore(
                        "§7Configure island spawners (Diamond & Emerald)",
                        "§7Current Spawners: §e" + arena.getSpawners().size() + " §7nodes",
                        "",
                        "§e▶ Click to configure center spawners!"
                ).build());

        // 5. 16 TEAMS BASE CARDS (Rows 2 & 3: slots 19 to 26, and 28 to 35)
        TeamColor[] allColors = TeamColor.values();
        for (int i = 0; i < 16; i++) {
            TeamColor tc = allColors[i];
            Team team = arena.getTeam(tc);

            boolean hasSpawn = (team != null && team.getSpawnLocation() != null);
            boolean hasBed = (team != null && team.getBedLocation() != null);
            boolean hasSp = (team != null && team.getBaseSpawnerLocation() != null);
            boolean hasShop = (team != null && team.getShopNpcLocation() != null);
            boolean hasUp = (team != null && team.getUpgradeNpcLocation() != null);

            int slot = (i < 8) ? (19 + i) : (28 + (i - 8));

            List<String> lore = new ArrayList<>();
            lore.add("§8────────────────────");
            lore.add((hasSpawn ? "§a✔ " : "§c✖ ") + "§7Team Spawn");
            lore.add((hasBed ? "§a✔ " : "§c✖ ") + "§7Bed Location");
            lore.add((hasSp ? "§a✔ " : "§c✖ ") + "§7Base Spawner");
            lore.add((hasShop ? "§a✔ " : "§c✖ ") + "§7Shop NPC");
            lore.add((hasUp ? "§a✔ " : "§c✖ ") + "§7Upgrade NPC");
            lore.add("§8────────────────────");
            lore.add("§e▶ Click to setup this team's base!");

            ItemStack woolItem = new ItemBuilder(tc.getWoolMaterial())
                    .name(tc.getChatColor() + "§l● Team " + tc.getDisplayName())
                    .lore(lore)
                    .build();

            inv.setItem(slot, woolItem);
        }

        // 6. Checklist Book
        boolean ready = BedwarsPlugin.getInstance().getArenaManager().isArenaReady(arena);
        List<String> checkLore = new ArrayList<>();
        checkLore.add("§8────────────────────");
        checkLore.add((hasLobby ? "§a✔ " : "§c✖ ") + "§7Lobby Spawn");
        checkLore.add((hasSpec ? "§a✔ " : "§c✖ ") + "§7Spectator Spawn");
        checkLore.add((arena.getTeams().size() >= 2 ? "§a✔ " : "§c✖ ") + "§7At least 2 teams (" + arena.getTeams().size() + " teams)");
        checkLore.add((!arena.getSpawners().isEmpty() ? "§a✔ " : "§c✖ ") + "§7Center Spawners (" + arena.getSpawners().size() + " nodes)");
        checkLore.add("§8────────────────────");
        checkLore.add(ready ? "§a§lStatus: 100% Ready!" : "§c§lStatus: Setup Incomplete");

        // Pos 1 Boundary
        boolean hasPos1 = arena.getPos1() != null;
        inv.setItem(46, new ItemBuilder(Material.STRUCTURE_BLOCK)
                .name("§e§lMap Border: Pos 1")
                .lore(
                        "§7Stand at corner 1 of the map boundary",
                        "§7and click here to save Pos 1 coordinates!",
                        "",
                        hasPos1 ? "§a✔ Configured: §f" + arena.getPos1().getBlockX() + ", " + arena.getPos1().getBlockY() + ", " + arena.getPos1().getBlockZ()
                                : "§c✖ Not set (Click to set to current location)"
                ).build());

        // Pos 2 Boundary
        boolean hasPos2 = arena.getPos2() != null;
        inv.setItem(47, new ItemBuilder(Material.JIGSAW)
                .name("§b§lMap Border: Pos 2")
                .lore(
                        "§7Stand at corner 2 of the map boundary",
                        "§7and click here to save Pos 2 coordinates!",
                        "",
                        hasPos2 ? "§a✔ Configured: §f" + arena.getPos2().getBlockX() + ", " + arena.getPos2().getBlockY() + ", " + arena.getPos2().getBlockZ()
                                : "§c✖ Not set (Click to set to current location)"
                ).build());

        inv.setItem(48, new ItemBuilder(Material.BOOK)
                .name("§e§lArena Readiness Checklist")
                .lore(checkLore)
                .build());

        // 7. Save & Activate
        inv.setItem(50, new ItemBuilder(ready ? Material.EMERALD_BLOCK : Material.REDSTONE_BLOCK)
                .name(ready ? "§a§lSave & Enable Arena" : "§c§lSave Configuration (Incomplete)")
                .lore(
                        "§7Click to save arena settings to disk",
                        "§e▶ Click to save now!"
                ).build());

        // Return to Map List
        inv.setItem(49, new ItemBuilder(Material.ARROW)
                .name("§c§lBack to Arenas List")
                .build());

        player.openInventory(inv);
        MessageUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
