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

public class TeamBaseSetupGUI {

    public static final String TITLE_PREFIX = "§8✦ §6§lBase Setup §8» ";

    public static void open(Player player, Arena arena, TeamColor teamColor) {
        Team team = arena.getTeam(teamColor);
        if (team == null) {
            team = new Team(teamColor);
            arena.addTeam(team);
        }

        String title = TITLE_PREFIX + teamColor.getChatColor() + "Team " + teamColor.getDisplayName() + " §8[" + arena.getName() + "]";
        Inventory inv = Bukkit.createInventory(null, 54, title);

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        // Header info
        inv.setItem(4, new ItemBuilder(teamColor.getWoolMaterial())
                .name(teamColor.getChatColor() + "§l✦ Setup Base: Team " + teamColor.getDisplayName() + " ✦")
                .lore(
                        "§7Configure all base coordinates and NPCs for this team",
                        "§7to ensure complete arena readiness."
                ).build());

        // 1. Team Spawn Location
        boolean hasSpawn = team.getSpawnLocation() != null;
        inv.setItem(11, new ItemBuilder(Material.LEATHER_BOOTS)
                .color(teamColor.getArmorColor())
                .name("§e§l1. Team Spawn Location")
                .lore(
                        "§7Stand at the location where this team will spawn/respawn",
                        "§7and click here to save coordinates!",
                        "",
                        hasSpawn ? "§a✔ Configured: §f" + team.getSpawnLocation().getBlockX() + ", " + team.getSpawnLocation().getBlockY() + ", " + team.getSpawnLocation().getBlockZ()
                                : "§c✖ Not set (Click to set to current position)",
                        "",
                        "§a▶ Left-Click: §fSave current location",
                        hasSpawn ? "§b▶ Right-Click: §fTeleport here" : ""
                ).build());

        // 2. Team Bed Location
        boolean hasBed = team.getBedLocation() != null;
        inv.setItem(13, new ItemBuilder(teamColor.getBedMaterial())
                .name("§c§l2. Team Bed Location")
                .lore(
                        "§7Stand on or look directly at this team's bed",
                        "§7and click here to save the bed coordinates!",
                        "",
                        hasBed ? "§a✔ Configured: §f" + team.getBedLocation().getBlockX() + ", " + team.getBedLocation().getBlockY() + ", " + team.getBedLocation().getBlockZ()
                                : "§c✖ Not set (Click to set to current position)",
                        "",
                        "§a▶ Left-Click: §fSave bed at current position",
                        hasBed ? "§b▶ Right-Click: §fTeleport here" : ""
                ).build());

        // 3. Base Spawner Location (Iron & Gold)
        boolean hasBaseSp = team.getBaseSpawnerLocation() != null;
        inv.setItem(15, new ItemBuilder(Material.GOLD_INGOT)
                .name("§6§l3. Base Generator Spawner")
                .lore(
                        "§7Stand where the Iron & Gold generator should drop items",
                        "§7and click here to save coordinates!",
                        "",
                        hasBaseSp ? "§a✔ Configured: §f" + team.getBaseSpawnerLocation().getBlockX() + ", " + team.getBaseSpawnerLocation().getBlockY() + ", " + team.getBaseSpawnerLocation().getBlockZ()
                                : "§c✖ Not set (Click to set to current position)",
                        "",
                        "§a▶ Left-Click: §fSave base spawner location",
                        hasBaseSp ? "§b▶ Right-Click: §fTeleport here" : ""
                ).build());

        // 4. Team Shop NPC
        boolean hasShop = team.getShopNpcLocation() != null;
        inv.setItem(29, new ItemBuilder(Material.VILLAGER_SPAWN_EGG)
                .name("§e§l4. Item Shop NPC")
                .lore(
                        "§7Stand where the Item Shop NPC should stand",
                        "§7Click here to spawn and record the shop NPC!",
                        "",
                        hasShop ? "§a✔ NPC Spawned: §f" + team.getShopNpcLocation().getBlockX() + ", " + team.getShopNpcLocation().getBlockY() + ", " + team.getShopNpcLocation().getBlockZ()
                                : "§c✖ Not placed (Click to place here)",
                        "",
                        "§a▶ Left-Click: §fPlace Shop NPC here",
                        hasShop ? "§b▶ Right-Click: §fTeleport here" : ""
                ).build());

        // 5. Team Upgrade NPC
        boolean hasUp = team.getUpgradeNpcLocation() != null;
        inv.setItem(33, new ItemBuilder(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                .name("§b§l5. Team Upgrades NPC")
                .lore(
                        "§7Stand where the Team Upgrades NPC should stand",
                        "§7Click here to spawn and record the upgrade NPC!",
                        "",
                        hasUp ? "§a✔ NPC Spawned: §f" + team.getUpgradeNpcLocation().getBlockX() + ", " + team.getUpgradeNpcLocation().getBlockY() + ", " + team.getUpgradeNpcLocation().getBlockZ()
                                : "§c✖ Not placed (Click to place here)",
                        "",
                        "§a▶ Left-Click: §fPlace Upgrade NPC here",
                        hasUp ? "§b▶ Right-Click: §fTeleport here" : ""
                ).build());

        // 6. Delete Team
        inv.setItem(40, new ItemBuilder(Material.LAVA_BUCKET)
                .name("§c§lRemove Team from Arena")
                .lore("§7Click to delete this team from the arena")
                .build());

        // Back button
        inv.setItem(49, new ItemBuilder(Material.ARROW)
                .name("§c§lBack to Arena Setup")
                .build());

        player.openInventory(inv);
        MessageUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
