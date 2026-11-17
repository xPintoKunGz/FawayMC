package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.Team;
import com.bedwars.arena.TeamColor;
import com.bedwars.arena.spawner.Spawner;
import com.bedwars.arena.spawner.SpawnerType;
import com.bedwars.gui.ArenaAdminGUI;
import com.bedwars.gui.CenterSpawnerGUI;
import com.bedwars.gui.SetupWizardGUI;
import com.bedwars.gui.TeamBaseSetupGUI;
import com.bedwars.util.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SetupListener implements Listener {

    private final BedwarsPlugin plugin;
    public static final Map<UUID, String> renamingMap = new HashMap<>();

    public SetupListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onAdminGuiClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player))
            return;
        String title = event.getView().getTitle();

        // 1. Admin Map Manager GUI
        if (title.equals(ArenaAdminGUI.ADMIN_TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR)
                return;

            if (clicked.getType() == Material.NETHER_STAR) {
                player.closeInventory();
                MessageUtil.sendMessage(player,
                        "§eRun: §a/bw setup <mapName> §eto create and setup a new arena!");
                return;
            }

            if (clicked.getItemMeta() != null && clicked.getItemMeta().hasLore()) {
                String arenaName = null;
                for (String line : clicked.getItemMeta().getLore()) {
                    if (line.contains("Arena ID: §f") || line.contains("ID: §f")) {
                        arenaName = ChatColor.stripColor(line).replace("Arena ID: ", "").replace("ID: ", "").trim();
                        break;
                    }
                }

                if (arenaName != null) {
                    Arena arena = plugin.getArenaManager().getArena(arenaName);
                    if (arena == null)
                        return;

                    if (event.isShiftClick() && event.isRightClick()) {
                        arena.setEnabled(!arena.isEnabled());
                        plugin.getArenaManager().saveArenas();
                        MessageUtil.sendMessage(player, "§eArena status for " + arena.getName() + ": "
                                + (arena.isEnabled() ? "§aEnabled" : "§cDisabled"));
                        ArenaAdminGUI.open(player);
                    } else if (event.isRightClick()) {
                        if (arena.getLobbyLocation() != null) {
                            player.teleport(arena.getLobbyLocation());
                            MessageUtil.sendMessage(player,
                                    "§aTeleported to lobby of §e" + arena.getName() + "§a!");
                        } else {
                            MessageUtil.sendMessage(player, "§cThis arena does not have a lobby set yet!");
                        }
                        player.closeInventory();
                    } else {
                        SetupWizardGUI.open(player, arena);
                    }
                }
            }
            return;
        }

        // 2. Setup Wizard GUI (Master Hub)
        if (title.startsWith(SetupWizardGUI.SETUP_TITLE_PREFIX)) {
            event.setCancelled(true);
            String arenaName = title.replace(SetupWizardGUI.SETUP_TITLE_PREFIX, "").trim();
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena == null)
                return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR)
                return;

            int slot = event.getRawSlot();
            Location pLoc = player.getLocation();

            // Rename Display Name
            if (slot == 4) {
                player.closeInventory();
                renamingMap.put(player.getUniqueId(), arena.getName());
                MessageUtil.sendMessage(player,
                        "§e§l[BEDWARS] §fPlease type the §anew display name §ffor this map in chat (color codes like &b&lAqua supported):");
                MessageUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.5f);
                return;
            }

            // 1. Lobby
            if (slot == 10) {
                arena.setLobbyLocation(pLoc);
                MessageUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                MessageUtil.sendMessage(player, "§a✔ Lobby spawn set to your current location!");
                SetupWizardGUI.open(player, arena);
                return;
            }

            // 2. Spectator
            if (slot == 12) {
                arena.setSpectatorLocation(pLoc);
                MessageUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                MessageUtil.sendMessage(player, "§a✔ Spectator spawn set to your current location!");
                SetupWizardGUI.open(player, arena);
                return;
            }

            // 3. Game Mode & Team Size
            if (slot == 14) {
                if (event.isRightClick()) {
                    int next = (arena.getPlayersPerTeam() % 8) + 1;
                    arena.setPlayersPerTeam(next);
                    arena.setMaxPlayers(arena.getTeams().size() * next);
                } else {
                    String[] modes = { "Solo", "Doubles", "3v3v3v3", "4v4v4v4", "8v8", "Mega" };
                    int curIdx = 0;
                    for (int i = 0; i < modes.length; i++) {
                        if (modes[i].equalsIgnoreCase(arena.getMode())) {
                            curIdx = i;
                            break;
                        }
                    }
                    String nextMode = modes[(curIdx + 1) % modes.length];
                    arena.setMode(nextMode);
                    if (nextMode.equals("Solo"))
                        arena.setPlayersPerTeam(1);
                    else if (nextMode.equals("Doubles"))
                        arena.setPlayersPerTeam(2);
                    else if (nextMode.equals("3v3v3v3"))
                        arena.setPlayersPerTeam(3);
                    else if (nextMode.equals("4v4v4v4"))
                        arena.setPlayersPerTeam(4);
                    else if (nextMode.equals("8v8"))
                        arena.setPlayersPerTeam(8);
                    arena.setMaxPlayers(Math.max(2, arena.getTeams().size() * arena.getPlayersPerTeam()));
                }
                MessageUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1.4f);
                SetupWizardGUI.open(player, arena);
                return;
            }

            // 4. Center Spawners
            if (slot == 16) {
                CenterSpawnerGUI.open(player, arena);
                return;
            }

            // 5. 16 Teams Base Cards (Slots 19-26, and 28-35)
            TeamColor[] allColors = TeamColor.values();
            for (int i = 0; i < 16; i++) {
                int teamSlot = (i < 8) ? (19 + i) : (28 + (i - 8));
                if (slot == teamSlot) {
                    TeamBaseSetupGUI.open(player, arena, allColors[i]);
                    return;
                }
            }

            // Pos 1 Boundary
            if (slot == 46) {
                arena.setPos1(pLoc.getBlock().getLocation());
                MessageUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                MessageUtil.sendMessage(player, "§a✔ Saved Map Boundary Pos 1 at: §e" + pLoc.getBlockX() + ", " + pLoc.getBlockY() + ", " + pLoc.getBlockZ() + " §afor arena §e" + arena.getName() + "§a!");
                SetupWizardGUI.open(player, arena);
                return;
            }

            // Pos 2 Boundary
            if (slot == 47) {
                arena.setPos2(pLoc.getBlock().getLocation());
                MessageUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                MessageUtil.sendMessage(player, "§a✔ Saved Map Boundary Pos 2 at: §e" + pLoc.getBlockX() + ", " + pLoc.getBlockY() + ", " + pLoc.getBlockZ() + " §afor arena §e" + arena.getName() + "§a!");
                SetupWizardGUI.open(player, arena);
                return;
            }

            // Save & Activate
            if (slot == 50) {
                plugin.getArenaManager().saveArenas();
                MessageUtil.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                MessageUtil.sendMessage(player, "§a§l✔ Configuration for arena " + arena.getName() + " saved successfully!");
                player.closeInventory();
                return;
            }

            // Back to Admin GUI
            if (slot == 49) {
                ArenaAdminGUI.open(player);
                return;
            }
        }

        // 3. Team Base Setup GUI (Dedicated per team)
        if (title.startsWith(TeamBaseSetupGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR)
                return;

            // Extract arena name from title
            int bracketStart = title.lastIndexOf('[');
            int bracketEnd = title.lastIndexOf(']');
            if (bracketStart == -1 || bracketEnd == -1)
                return;
            String arenaName = title.substring(bracketStart + 1, bracketEnd);
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena == null)
                return;

            int slot = event.getRawSlot();
            Location pLoc = player.getLocation();

            // Find current team from Header icon at slot 4
            ItemStack header = event.getInventory().getItem(4);
            if (header == null)
                return;
            TeamColor curColor = null;
            for (TeamColor tc : TeamColor.values()) {
                if (header.getType() == tc.getWoolMaterial()) {
                    curColor = tc;
                    break;
                }
            }
            if (curColor == null)
                return;
            Team team = arena.getTeam(curColor);
            if (team == null) {
                team = new Team(curColor);
                arena.addTeam(team);
            }

            if (event.isRightClick()) {
                Location target = null;
                if (slot == 11) target = team.getSpawnLocation();
                else if (slot == 13) target = team.getBedLocation();
                else if (slot == 15) target = team.getBaseSpawnerLocation();
                else if (slot == 29) target = team.getShopNpcLocation();
                else if (slot == 33) target = team.getUpgradeNpcLocation();

                if (target != null) {
                    player.teleport(target.clone().add(0.5, 0.1, 0.5));
                    MessageUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    MessageUtil.sendMessage(player, "§a✔ Teleported to configured location!");
                    player.closeInventory();
                    return;
                }
            }

            // 1. Team Spawn
            if (slot == 11) {
                team.setSpawnLocation(pLoc);
                MessageUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                MessageUtil.sendMessage(player,
                        "§a✔ Spawn location for team " + curColor.getDisplayName() + " saved!");
                TeamBaseSetupGUI.open(player, arena, curColor);
                return;
            }

            // 2. Team Bed (Point foot of the bed towards player's facing direction)
            if (slot == 13) {
                org.bukkit.block.Block bedBlock = pLoc.getBlock();
                Location bedLoc = bedBlock.getLocation();
                bedLoc.setYaw(player.getLocation().getYaw());
                team.setBedLocation(bedLoc);
                Arena.placeFullBed(bedBlock, curColor.getBedMaterial(), player.getLocation().getYaw());
                MessageUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                MessageUtil.sendMessage(player,
                        "§a✔ 2-block bed placed pointing " + Arena.getFacingFromYaw(player.getLocation().getYaw()) + " for team " + curColor.getDisplayName() + "!");
                TeamBaseSetupGUI.open(player, arena, curColor);
                return;
            }

            // 3. Base Spawner (Iron & Gold)
            if (slot == 15) {
                team.setBaseSpawnerLocation(pLoc);
                MessageUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
                MessageUtil.sendMessage(player,
                        "§a✔ Base spawner (Iron & Gold) saved for team " + curColor.getDisplayName() + "!");
                TeamBaseSetupGUI.open(player, arena, curColor);
                return;
            }

            // 4. Team Shop NPC - Spawns immediately so admin sees it!
            if (slot == 29) {
                team.setShopNpcLocation(pLoc);
                Arena.spawnShopVillager(pLoc, team);
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_YES, 1f, 1f);
                MessageUtil.sendMessage(player, "§a✔ Shop NPC created for team " + curColor.getDisplayName() + "!");
                TeamBaseSetupGUI.open(player, arena, curColor);
                return;
            }

            // 5. Team Upgrade NPC - Spawns immediately so admin sees it!
            if (slot == 33) {
                team.setUpgradeNpcLocation(pLoc);
                Arena.spawnUpgradeVillager(pLoc, team);
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_YES, 1f, 1f);
                MessageUtil.sendMessage(player, "§a✔ Upgrade NPC created for team " + curColor.getDisplayName() + "!");
                TeamBaseSetupGUI.open(player, arena, curColor);
                return;
            }

            // 6. Delete Team
            if (slot == 40) {
                arena.getTeams().remove(curColor);
                MessageUtil.playSound(player, Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                MessageUtil.sendMessage(player, "§cRemoved team " + curColor.getDisplayName() + " from the arena!");
                SetupWizardGUI.open(player, arena);
                return;
            }

            // Back to main Setup
            if (slot == 49) {
                SetupWizardGUI.open(player, arena);
                return;
            }
        }

        // 4. Center Spawner GUI (Diamond & Emerald)
        if (title.startsWith(CenterSpawnerGUI.TITLE_PREFIX)) {
            event.setCancelled(true);
            String arenaName = title.replace(CenterSpawnerGUI.TITLE_PREFIX, "").trim();
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena == null)
                return;

            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR)
                return;

            int slot = event.getRawSlot();
            Location pLoc = player.getLocation();

            // Diamond Spawner
            if (slot == 11) {
                Spawner sp = new Spawner(SpawnerType.DIAMOND, pLoc);
                arena.addSpawner(sp);
                sp.initHologram();
                plugin.getArenaManager().saveArenas();
                MessageUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1f, 1.4f);
                MessageUtil.sendMessage(player, "§b✔ Added Diamond Spawner with hologram at your location!");
                CenterSpawnerGUI.open(player, arena);
                return;
            }

            // Emerald Spawner
            if (slot == 15) {
                Spawner sp = new Spawner(SpawnerType.EMERALD, pLoc);
                arena.addSpawner(sp);
                sp.initHologram();
                plugin.getArenaManager().saveArenas();
                MessageUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1f, 1.4f);
                MessageUtil.sendMessage(player, "§a✔ Added Emerald Spawner with hologram at your location!");
                CenterSpawnerGUI.open(player, arena);
                return;
            }

            // Individual Spawner Cards (Slots 18 to 35)
            if (slot >= 18 && slot <= 35) {
                List<Spawner> centerSpawners = new ArrayList<>();
                for (Spawner s : arena.getSpawners()) {
                    if (s.getType() == SpawnerType.DIAMOND || s.getType() == SpawnerType.EMERALD) {
                        centerSpawners.add(s);
                    }
                }
                int idx = slot - 18;
                if (idx < centerSpawners.size()) {
                    Spawner targetSp = centerSpawners.get(idx);
                    if (event.isRightClick()) {
                        // Delete Spawner
                        targetSp.destroyHologram();
                        arena.getSpawners().remove(targetSp);
                        plugin.getArenaManager().saveArenas();
                        MessageUtil.playSound(player, Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                        MessageUtil.sendMessage(player, "§c✖ Deleted spawner at " + targetSp.getLocation().getBlockX() + ", " + targetSp.getLocation().getBlockY() + ", " + targetSp.getLocation().getBlockZ());
                        CenterSpawnerGUI.open(player, arena);
                        return;
                    } else if (event.isShiftClick() || event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
                        // Relocate Spawner to current position
                        targetSp.destroyHologram();
                        arena.getSpawners().remove(targetSp);
                        Spawner relocated = new Spawner(targetSp.getType(), player.getLocation());
                        arena.addSpawner(relocated);
                        relocated.initHologram();
                        plugin.getArenaManager().saveArenas();
                        MessageUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1f, 1.4f);
                        MessageUtil.sendMessage(player, "§a✔ Relocated spawner to your current location!");
                        CenterSpawnerGUI.open(player, arena);
                        return;
                    } else {
                        // Left-Click: Teleport to spawner
                        player.teleport(targetSp.getLocation().clone().add(0.5, 1, 0.5));
                        MessageUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                        MessageUtil.sendMessage(player, "§a✔ Teleported to spawner at " + targetSp.getLocation().getBlockX() + ", " + targetSp.getLocation().getBlockY() + ", " + targetSp.getLocation().getBlockZ());
                        player.closeInventory();
                        return;
                    }
                }
            }

            // Clear Center Spawners
            if (slot == 40) {
                for (Spawner s : arena.getSpawners()) {
                    s.destroyHologram();
                }
                arena.getSpawners().clear();
                plugin.getArenaManager().saveArenas();
                MessageUtil.playSound(player, Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                MessageUtil.sendMessage(player, "§cCleared all center spawners!");
                CenterSpawnerGUI.open(player, arena);
                return;
            }

            // Back
            if (slot == 49) {
                SetupWizardGUI.open(player, arena);
            }
        }
    }
}
