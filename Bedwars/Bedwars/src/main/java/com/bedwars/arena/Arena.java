package com.bedwars.arena;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.bot.RejoinBot;
import com.bedwars.arena.spawner.Spawner;
import com.bedwars.arena.spawner.SpawnerType;
import com.bedwars.replay.ReplayData;
import com.bedwars.replay.ReplayRecorder;
import com.bedwars.util.ItemBuilder;
import com.bedwars.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Arena {

    private final String name;
    private String displayName;
    private String mode = "Solo";
    private int minPlayers = 2;
    private int maxPlayers = 8;
    private int playersPerTeam = 1;
    private boolean enabled = true;

    private Location lobbyLocation;
    private Location spectatorLocation;

    private final Map<TeamColor, Team> teams = new LinkedHashMap<>();
    private final List<Spawner> spawners = new ArrayList<>();
    private final List<Location> shopNpcLocations = new ArrayList<>();
    private final List<Location> upgradeNpcLocations = new ArrayList<>();

    private final Set<UUID> players = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private final Map<UUID, Team> disconnectedPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, RejoinBot> activeBots = new ConcurrentHashMap<>();
    private ReplayRecorder replayRecorder;
    private final Set<Location> placedBlocks = new HashSet<>();
    private final Map<Location, org.bukkit.block.data.BlockData> originalBedBlockData = new HashMap<>();

    // In-game stats: UUID -> count
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> finalKills = new HashMap<>();
    private final Map<UUID, Integer> bedsBroken = new HashMap<>();

    private ArenaState state = ArenaState.WAITING;
    private int countdown = 15;
    private int gameSeconds = 0;
    private BukkitTask gameTask;
    private BukkitTask lobbyTask;
    private BukkitTask dragonDestructionTask;
    private String timeOfDay = "day";
    private final Map<Location, ItemStack[]> originalChestContents = new HashMap<>();
    private final Set<UUID> originalOps = new HashSet<>();
    private final Set<UUID> respawningPlayers = new HashSet<>();
    private boolean editMode = false;

    public boolean isEditMode() {
        return editMode;
    }

    public void setEditMode(boolean editMode) {
        this.editMode = editMode;
    }

    private Location pos1;
    private Location pos2;

    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public boolean isInRegion(Location loc) {
        if (loc == null || pos1 == null || pos2 == null) {
            return true; // Not configured, allow default
        }
        if (!loc.getWorld().equals(pos1.getWorld())) {
            return false;
        }
        double minX = Math.min(pos1.getX(), pos2.getX());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());

        return loc.getX() >= minX && loc.getX() <= maxX
                && loc.getY() >= minY && loc.getY() <= maxY
                && loc.getZ() >= minZ && loc.getZ() <= maxZ;
    }

    public org.bukkit.World getWorld() {
        if (pos1 != null && pos1.getWorld() != null) return pos1.getWorld();
        if (pos2 != null && pos2.getWorld() != null) return pos2.getWorld();
        if (spectatorLocation != null && spectatorLocation.getWorld() != null) return spectatorLocation.getWorld();
        if (lobbyLocation != null && lobbyLocation.getWorld() != null) return lobbyLocation.getWorld();
        for (Team t : teams.values()) {
            if (t.getSpawnLocation() != null && t.getSpawnLocation().getWorld() != null) return t.getSpawnLocation().getWorld();
            if (t.getBaseSpawnerLocation() != null && t.getBaseSpawnerLocation().getWorld() != null) return t.getBaseSpawnerLocation().getWorld();
        }
        for (Spawner s : spawners) {
            if (s.getLocation() != null && s.getLocation().getWorld() != null) return s.getLocation().getWorld();
        }
        return null;
    }

    public boolean isRespawning(UUID uuid) {
        return respawningPlayers.contains(uuid);
    }

    public Arena(String name) {
        this.name = name;
    }

    public String getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public void applyTimeOfDay() {
        if (lobbyLocation != null && lobbyLocation.getWorld() != null) {
            long time = switch (timeOfDay.toLowerCase()) {
                case "noon" -> 6000L;
                case "sunset" -> 12000L;
                case "night" -> 18000L;
                default -> 1000L;
            };
            lobbyLocation.getWorld().setTime(time);
        }
    }

    public void recordChestSnapshot(Location loc, ItemStack[] contents) {
        if (!originalChestContents.containsKey(loc) && contents != null) {
            ItemStack[] copy = new ItemStack[contents.length];
            for (int i = 0; i < contents.length; i++) {
                copy[i] = (contents[i] != null) ? contents[i].clone() : null;
            }
            originalChestContents.put(loc, copy);
        }
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return (displayName != null && !displayName.isEmpty()) ? displayName : name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getMinPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public int getPlayersPerTeam() {
        return playersPerTeam;
    }

    public void setPlayersPerTeam(int playersPerTeam) {
        this.playersPerTeam = playersPerTeam;
    }

    public Location getLobbyLocation() {
        return lobbyLocation;
    }

    public void setLobbyLocation(Location lobbyLocation) {
        this.lobbyLocation = lobbyLocation;
    }

    public Location getSpectatorLocation() {
        return spectatorLocation;
    }

    public void setSpectatorLocation(Location spectatorLocation) {
        this.spectatorLocation = spectatorLocation;
    }

    public Map<TeamColor, Team> getTeams() {
        return teams;
    }

    public Team getTeam(TeamColor color) {
        return teams.get(color);
    }

    public void addTeam(Team team) {
        teams.put(team.getColor(), team);
    }

    public List<Spawner> getSpawners() {
        return spawners;
    }

    public void addSpawner(Spawner spawner) {
        spawners.add(spawner);
    }

    public List<Location> getShopNpcLocations() {
        return shopNpcLocations;
    }

    public List<Location> getUpgradeNpcLocations() {
        return upgradeNpcLocations;
    }

    public Set<UUID> getPlayers() {
        return Collections.unmodifiableSet(players);
    }

    public Set<UUID> getSpectators() {
        return Collections.unmodifiableSet(spectators);
    }

    public ArenaState getState() {
        return state;
    }

    public void setState(ArenaState state) {
        this.state = state;
    }

    public int getCountdown() {
        return countdown;
    }

    public int getGameSeconds() {
        return gameSeconds;
    }

    public boolean isPlayerInGame(UUID uuid) {
        return players.contains(uuid) || spectators.contains(uuid);
    }

    public Team getPlayerTeam(UUID uuid) {
        for (Team team : teams.values()) {
            if (team.hasPlayer(uuid)) return team;
        }
        return null;
    }

    public int getKills(UUID uuid) {
        return kills.getOrDefault(uuid, 0);
    }

    public int getFinalKills(UUID uuid) {
        return finalKills.getOrDefault(uuid, 0);
    }

    public int getBedsBroken(UUID uuid) {
        return bedsBroken.getOrDefault(uuid, 0);
    }

    public void addKill(UUID uuid) {
        kills.put(uuid, getKills(uuid) + 1);
    }

    public void addFinalKill(UUID uuid) {
        finalKills.put(uuid, getFinalKills(uuid) + 1);
    }

    public void addBedBroken(UUID uuid) {
        bedsBroken.put(uuid, getBedsBroken(uuid) + 1);
    }

    public boolean isBlockPlaced(Location loc) {
        return placedBlocks.contains(loc);
    }

    public void addPlacedBlock(Location loc) {
        placedBlocks.add(loc);
        if (replayRecorder != null) {
            try {
                String bd = loc.getBlock().getBlockData().getAsString();
                replayRecorder.recordBlockChange(gameSeconds, loc, bd, true);
            } catch (Exception ignored) {}
        }
    }

    public void removePlacedBlock(Location loc) {
        placedBlocks.remove(loc);
        if (replayRecorder != null) {
            replayRecorder.recordBlockChange(gameSeconds, loc, "AIR", false);
        }
    }

    // Join Arena
    public boolean join(Player player) {
        if (state != ArenaState.WAITING && state != ArenaState.STARTING) {
            MessageUtil.sendMessage(player, BedwarsPlugin.getInstance().getMessage("messages.arena-running"));
            return false;
        }
        if (players.size() >= maxPlayers) {
            MessageUtil.sendMessage(player, BedwarsPlugin.getInstance().getMessage("messages.arena-full"));
            return false;
        }

        if (BedwarsPlugin.getInstance().getReplayManager() != null) {
            BedwarsPlugin.getInstance().getReplayManager().stopPlayback(player);
        }
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(" "));

        players.add(player.getUniqueId());
        player.setGameMode(GameMode.SURVIVAL);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.getInventory().clear();
        player.getInventory().setArmorContents(null);

        // Teleport to waiting lobby
        if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
        }

        // Give lobby items: Team selector, Bot practice setup, Diamond start, Leave bed
        player.getInventory().setItem(0, new ItemBuilder(Material.COMPASS).name("§a§lTeam Selector §7(Right Click)").build());
        player.getInventory().setItem(2, new ItemBuilder(Material.BLAZE_ROD).name("§6§lBot Practice Setup §7(Right Click)").build());
        player.getInventory().setItem(4, new ItemBuilder(Material.DIAMOND).name("§b§lStart Game Now §7(Right Click)").build());
        player.getInventory().setItem(8, new ItemBuilder(Material.RED_BED).name("§c§lLeave Game §7(Right Click)").build());

        String joinMsg = BedwarsPlugin.getInstance().getMessage("messages.player-join")
                .replace("{player}", player.getName())
                .replace("{current}", String.valueOf(players.size()))
                .replace("{max}", String.valueOf(maxPlayers));
        broadcast(joinMsg);

        MessageUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);

        // Check if can start
        if (players.size() >= minPlayers && state == ArenaState.WAITING) {
            startLobbyCountdown();
        }
        return true;
    }

    // Leave Arena
    public void leave(Player player) {
        players.remove(player.getUniqueId());
        spectators.remove(player.getUniqueId());
        respawningPlayers.remove(player.getUniqueId());

        Team team = getPlayerTeam(player.getUniqueId());
        if (team != null) {
            team.removePlayer(player.getUniqueId());
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.setGameMode(GameMode.SURVIVAL);

        playerPickaxeTiers.remove(player.getUniqueId());
        playerAxeTiers.remove(player.getUniqueId());
        playerHasShears.remove(player.getUniqueId());

        // Restore OP status if was temporarily stripped
        if (originalOps.contains(player.getUniqueId())) {
            player.setOp(true);
            originalOps.remove(player.getUniqueId());
        }

        // Teleport to main server lobby
        Location mainLobby = BedwarsPlugin.getInstance().getMainLobby();
        if (mainLobby != null) {
            player.teleport(mainLobby);
        } else if (lobbyLocation != null) {
            player.teleport(lobbyLocation);
        }

        // Reset player scoreboard to main and restore original tab name
        BedwarsPlugin.getInstance().getScoreboardManager().removeScoreboard(player);
        player.setPlayerListName(player.getName());
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, new net.md_5.bungee.api.chat.TextComponent(" "));
        if (player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED) != null) {
            player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED).setBaseValue(4.0);
        }

        String leaveMsg = BedwarsPlugin.getInstance().getMessage("messages.player-leave")
                .replace("{player}", player.getName())
                .replace("{current}", String.valueOf(players.size()))
                .replace("{max}", String.valueOf(maxPlayers));
        broadcast(leaveMsg);

        if (state == ArenaState.IN_GAME) {
            checkWinCondition();
        } else if (state == ArenaState.STARTING && players.size() < minPlayers) {
            cancelLobbyCountdown();
        }
    }

    public Map<UUID, Team> getDisconnectedPlayers() {
        return disconnectedPlayers;
    }

    public void handleDisconnect(Player player) {
        if (state != ArenaState.IN_GAME) {
            leave(player);
            return;
        }

        UUID uuid = player.getUniqueId();
        Team team = getPlayerTeam(uuid);
        if (team == null) {
            leave(player);
            return;
        }

        disconnectedPlayers.put(uuid, team);
        players.remove(uuid);
        respawningPlayers.remove(uuid);

        if (originalOps.contains(uuid)) {
            player.setOp(true);
            originalOps.remove(uuid);
        }

        if (replayRecorder != null) {
            replayRecorder.recordEvent(gameSeconds, "DISCONNECT", player.getName() + " disconnected.");
        }

        // Spawn AI Bot to take over playing in place of the disconnected player seamlessly without revealing it's a bot
        if (team.isBedAlive()) {
            try {
                RejoinBot bot = new RejoinBot(this, player, team);
                activeBots.put(uuid, bot);
                bot.spawn();
            } catch (Exception e) {
                BedwarsPlugin.getInstance().getLogger().warning("Failed to spawn replacement bot: " + e.getMessage());
            }
        }

        checkWinCondition();
    }

    public boolean handleRejoin(Player player) {
        if (state != ArenaState.IN_GAME) {
            MessageUtil.sendMessage(player, "§c✖ This match is no longer running!");
            return false;
        }

        UUID uuid = player.getUniqueId();

        // Despawn replacement AI Bot immediately when main player returns
        RejoinBot bot = activeBots.remove(uuid);
        if (bot != null) {
            bot.despawn();
        }

        Team team = disconnectedPlayers.remove(uuid);
        if (team == null) {
            team = getPlayerTeam(uuid);
            if (team == null) {
                MessageUtil.sendMessage(player, "§c✖ You do not have an active session in this arena!");
                return false;
            }
        }

        // 1. Bed broken condition -> Join directly as Spectator
        if (!team.isBedAlive()) {
            spectators.add(uuid);
            players.remove(uuid);
            player.setGameMode(GameMode.SPECTATOR);
            player.getInventory().clear();
            player.getInventory().setArmorContents(null);
            player.getInventory().setItem(0, new ItemBuilder(Material.CLOCK).name("§b§lTeleport to Player §7(Right Click)").build());
            player.getInventory().setItem(8, new ItemBuilder(Material.RED_BED).name("§c§lLeave Game §7(Right Click)").build());

            Location specLoc = (spectatorLocation != null)
                    ? spectatorLocation
                    : (team.getSpawnLocation() != null ? team.getSpawnLocation() : player.getWorld().getSpawnLocation());
            player.teleport(specLoc);

            BedwarsPlugin.getInstance().getScoreboardManager().updatePlayerScoreboard(player);

            MessageUtil.sendTitle(player, "§c§lBED DESTROYED", "§7Your bed was broken! Rejoined as Spectator.", 10, 60, 10);
            MessageUtil.sendMessage(player, "§c✖ Your bed was destroyed while disconnected. You are now spectating.");
            broadcast("§7" + player.getName() + " §erejoined as a §8Spectator §7(Bed broken).");

            if (replayRecorder != null) {
                replayRecorder.recordEvent(gameSeconds, "REJOIN_SPEC", player.getName() + " rejoined as spectator.");
            }
            return true;
        }

        // 2. Bed is alive -> Rejoin active: Enter as SPECTATOR first, run normal death respawn countdown, then spawn with default gear and team upgrades!
        players.add(uuid);
        spectators.remove(uuid);
        if (!team.hasPlayer(uuid)) {
            team.addPlayer(uuid);
        }

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        playerArmorTiers.put(uuid, ArmorTier.LEATHER);
        playerPickaxeTiers.remove(uuid);
        playerAxeTiers.remove(uuid);
        playerHasShears.remove(uuid);

        player.setGameMode(GameMode.SPECTATOR);
        player.setHealth(20.0);
        player.setFoodLevel(20);
        player.setFireTicks(0);

        Location specLoc = (spectatorLocation != null)
                ? spectatorLocation
                : (team.getSpawnLocation() != null ? team.getSpawnLocation().clone().add(0, 5, 0) : player.getWorld().getSpawnLocation());
        player.teleport(specLoc);

        player.setPlayerListName(team.getColor().getChatColor() + player.getName());
        BedwarsPlugin.getInstance().getScoreboardManager().updatePlayerScoreboard(player);

        MessageUtil.sendTitle(player, "§a§lRECONNECTED!", "§eRespawning in spectator mode...", 5, 30, 5);
        MessageUtil.sendMessage(player, "§a✔ Welcome back! You are respawning via countdown. Team upgrades remain active.");
        broadcast("§a✦ " + player.getName() + " §7reconnected to " + team.getColor().getChatColor() + team.getColor().getDisplayName() + " Team§7!");

        if (replayRecorder != null) {
            replayRecorder.recordEvent(gameSeconds, "REJOIN", player.getName() + " reconnected to the game.");
        }

        // Run normal respawn countdown like a normal death!
        startRespawnCountdown(player, team);
        return true;
    }

    private void startLobbyCountdown() {
        state = ArenaState.STARTING;
        countdown = BedwarsPlugin.getInstance().getConfig().getInt("game.lobby-countdown", 15);

        lobbyTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (players.size() < minPlayers) {
                    cancelLobbyCountdown();
                    cancel();
                    return;
                }

                for (UUID uuid : players) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null) {
                        p.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent("§6Starting in §e" + countdown + "s §8| §bDiamond: §fStart now"));
                    }
                }

                if (countdown == 10 || countdown == 5 || (countdown <= 3 && countdown > 0)) {
                    String msg = BedwarsPlugin.getInstance().getMessage("messages.countdown")
                            .replace("{time}", String.valueOf(countdown));
                    broadcast(msg);
                    for (UUID uuid : players) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            MessageUtil.playSound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                            MessageUtil.sendTitle(p, "§e§l" + countdown, "§7Game starting soon...", 0, 25, 5);
                        }
                    }
                }

                if (countdown <= 0) {
                    startGame();
                    cancel();
                    return;
                }
                countdown--;
            }
        }.runTaskTimer(BedwarsPlugin.getInstance(), 0L, 20L);
    }

    public void cancelLobbyCountdown() {
        if (lobbyTask != null) {
            lobbyTask.cancel();
            lobbyTask = null;
        }
        state = ArenaState.WAITING;
    }

    // Start Game
    public void startGame() {
        if (lobbyTask != null) {
            lobbyTask.cancel();
            lobbyTask = null;
        }
        state = ArenaState.IN_GAME;
        gameSeconds = 0;
        disconnectedPlayers.clear();

        // Start match replay recorder
        try {
            replayRecorder = new ReplayRecorder(this);
            replayRecorder.start();
        } catch (Exception e) {
            BedwarsPlugin.getInstance().getLogger().warning("Failed to start replay recorder: " + e.getMessage());
        }

        // Auto assign players without teams safely
        List<Team> teamList = new ArrayList<>(teams.values());
        if (!teamList.isEmpty()) {
            for (UUID uuid : players) {
                if (getPlayerTeam(uuid) == null) {
                    Team best = null;
                    for (Team t : teamList) {
                        if (best == null || t.getAlivePlayerCount(this) < best.getAlivePlayerCount(this)) {
                            best = t;
                        }
                    }
                    if (best != null) {
                        best.addPlayer(uuid);
                    }
                }
            }
        }

        applyTimeOfDay();

        // Deactivate beds for teams that have no players in this match
        for (Team t : teams.values()) {
            if (t.getPlayers().isEmpty()) {
                t.setBedAlive(false);
            }
        }

        // Anti-cheat: strip OP from any admins during match and force survival
        originalOps.clear();
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                if (p.isOp()) {
                    originalOps.add(uuid);
                    p.setOp(false);
                }
                p.setGameMode(GameMode.SURVIVAL);
            }
        }

        // Teleport to team spawns and equip
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            Team t = getPlayerTeam(uuid);
            p.getInventory().clear();
            if (p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED) != null) {
                p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED).setBaseValue(1024.0); // 1.8.9 PvP Spam click!
            }
            if (t != null) {
                p.setPlayerListName(t.getColor().getChatColor() + p.getName());
                if (t.getSpawnLocation() != null) {
                    p.teleport(t.getSpawnLocation());
                }
            }
            giveDefaultEquipment(p, t);
            MessageUtil.sendTitle(p, "§a§lGAME START!", "§fDefend your bed and destroy the enemy!", 10, 50, 10);
            MessageUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.2f);
        }

        // Cancel any existing game task to prevent duplicate task loops
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }

        // Clean all dropped items in arena world before match begins
        clearAllArenaDroppedItems();

        // Clean any leftover base spawners (Iron & Gold) before spawning new ones
        spawners.removeIf(s -> s.getType() == SpawnerType.IRON || s.getType() == SpawnerType.GOLD);

        // Spawn NPCs and Spawners
        spawnNPCs();

        Set<String> createdBaseLocs = new HashSet<>();
        for (Team t : teams.values()) {
            // ONLY create base generators for teams that actually have players in the match!
            if (t.getPlayers().isEmpty()) continue;

            if (t.getBaseSpawnerLocation() != null) {
                Location spLoc = t.getBaseSpawnerLocation();
                if (spLoc.getWorld() != null) {
                    if (!spLoc.getChunk().isLoaded()) {
                        spLoc.getChunk().load(true);
                    }
                    // Guarantee base spawner pad is 100% completely clear of any items at start
                    for (Entity e : spLoc.getWorld().getNearbyEntities(spLoc, 15, 15, 15)) {
                        if (e instanceof Item) {
                            e.remove();
                        }
                    }
                    // Prevent duplicate spawners at same block if multiple teams share coordinates
                    String locKey = spLoc.getWorld().getName() + "_" + spLoc.getBlockX() + "_" + spLoc.getBlockY() + "_" + spLoc.getBlockZ();
                    if (!createdBaseLocs.add(locKey)) {
                        continue;
                    }
                }
                Spawner ironSp = new Spawner(SpawnerType.IRON, spLoc, t);
                Spawner goldSp = new Spawner(SpawnerType.GOLD, spLoc, t);
                spawners.add(ironSp);
                spawners.add(goldSp);
                updateTeamSpawnerTier(t);
            }
        }
        for (Spawner spawner : spawners) {
            spawner.initHologram();
        }

        // Cache original full 2-block beds
        originalBedBlockData.clear();
        for (Team t : teams.values()) {
            if (t.getBedLocation() != null) {
                Block bedBlock = t.getBedLocation().getBlock();
                if (bedBlock.getBlockData() instanceof org.bukkit.block.data.type.Bed bedData) {
                    originalBedBlockData.put(bedBlock.getLocation(), bedBlock.getBlockData().clone());
                    Block otherHalf = (bedData.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD)
                            ? bedBlock.getRelative(bedData.getFacing().getOppositeFace())
                            : bedBlock.getRelative(bedData.getFacing());
                    if (otherHalf.getBlockData() instanceof org.bukkit.block.data.type.Bed) {
                        originalBedBlockData.put(otherHalf.getLocation(), otherHalf.getBlockData().clone());
                    }
                } else {
                    float bedYaw = t.getBedLocation().getYaw();
                    placeFullBed(bedBlock, t.getColor().getBedMaterial(), bedYaw);
                    if (bedBlock.getBlockData() instanceof org.bukkit.block.data.type.Bed bedData) {
                        originalBedBlockData.put(bedBlock.getLocation(), bedBlock.getBlockData().clone());
                        Block otherHalf = (bedData.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD)
                                ? bedBlock.getRelative(bedData.getFacing().getOppositeFace())
                                : bedBlock.getRelative(bedData.getFacing());
                        originalBedBlockData.put(otherHalf.getLocation(), otherHalf.getBlockData().clone());
                    }
                }
            }
        }

        broadcast(BedwarsPlugin.getInstance().getMessage("messages.game-started"));

        // In-game loop task
        gameTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (state != ArenaState.IN_GAME) {
                    cancel();
                    return;
                }
                gameSeconds++;

                // Record replay timeline frame
                if (replayRecorder != null) {
                    replayRecorder.recordTick(gameSeconds);
                }

                // Tick spawners
                for (Spawner spawner : spawners) {
                    spawner.tick();
                }

                // Team Base Traps, Magnet & Heal Pool checks
                tickTeamBases();

                // Check Game Events (e.g. Diamond II at 8m, Emerald II at 16m, Sudden death at 60m)
                handleGameEvents();
            }
        }.runTaskTimer(BedwarsPlugin.getInstance(), 20L, 20L);
    }

    public enum ArmorTier {
        LEATHER,
        CHAIN,
        IRON,
        DIAMOND,
        NETHERITE
    }

    private final Map<UUID, ArmorTier> playerArmorTiers = new HashMap<>();
    private final Map<UUID, Integer> playerPickaxeTiers = new HashMap<>();
    private final Map<UUID, Integer> playerAxeTiers = new HashMap<>();
    private final Set<UUID> playerHasShears = new HashSet<>();

    public ArmorTier getPlayerArmorTier(UUID uuid) {
        return playerArmorTiers.getOrDefault(uuid, ArmorTier.LEATHER);
    }

    public void setPlayerArmorTier(UUID uuid, ArmorTier tier) {
        playerArmorTiers.put(uuid, tier);
    }

    public int getPlayerPickaxeTier(UUID uuid) {
        return playerPickaxeTiers.getOrDefault(uuid, 0);
    }

    public void setPlayerPickaxeTier(UUID uuid, int tier) {
        playerPickaxeTiers.put(uuid, tier);
    }

    public int getPlayerAxeTier(UUID uuid) {
        return playerAxeTiers.getOrDefault(uuid, 0);
    }

    public void setPlayerAxeTier(UUID uuid, int tier) {
        playerAxeTiers.put(uuid, tier);
    }

    public boolean hasShears(UUID uuid) {
        return playerHasShears.contains(uuid);
    }

    public void setHasShears(UUID uuid, boolean has) {
        if (has) playerHasShears.add(uuid);
        else playerHasShears.remove(uuid);
    }

    public void updatePlayerArmor(Player player, Team team) {
        if (player == null) return;
        ArmorTier tier = getPlayerArmorTier(player.getUniqueId());
        int prot = (team != null) ? team.getProtectionLevel() : 0;
        Color color = (team != null) ? team.getColor().getArmorColor() : Color.WHITE;

        // Helmet (Leather with team color)
        ItemBuilder helm = new ItemBuilder(Material.LEATHER_HELMET).color(color).unbreakable(true);
        if (prot > 0) helm.enchant(org.bukkit.enchantments.Enchantment.PROTECTION, prot);
        player.getInventory().setHelmet(helm.build());

        // Chestplate (Leather with team color) - preserve Elytra if currently equipped!
        ItemStack curChest = player.getInventory().getChestplate();
        if (curChest == null || curChest.getType() != Material.ELYTRA) {
            ItemBuilder chest = new ItemBuilder(Material.LEATHER_CHESTPLATE).color(color).unbreakable(true);
            if (prot > 0) chest.enchant(org.bukkit.enchantments.Enchantment.PROTECTION, prot);
            player.getInventory().setChestplate(chest.build());
        }

        // Leggings & Boots based on unlocked tier
        Material legMat = switch (tier) {
            case CHAIN -> Material.CHAINMAIL_LEGGINGS;
            case IRON -> Material.IRON_LEGGINGS;
            case DIAMOND -> Material.DIAMOND_LEGGINGS;
            case NETHERITE -> Material.NETHERITE_LEGGINGS;
            default -> Material.LEATHER_LEGGINGS;
        };

        Material bootMat = switch (tier) {
            case CHAIN -> Material.CHAINMAIL_BOOTS;
            case IRON -> Material.IRON_BOOTS;
            case DIAMOND -> Material.DIAMOND_BOOTS;
            case NETHERITE -> Material.NETHERITE_BOOTS;
            default -> Material.LEATHER_BOOTS;
        };

        ItemBuilder leg = new ItemBuilder(legMat).unbreakable(true);
        if (legMat == Material.LEATHER_LEGGINGS) leg.color(color);
        if (prot > 0) leg.enchant(org.bukkit.enchantments.Enchantment.PROTECTION, prot);
        player.getInventory().setLeggings(leg.build());

        ItemBuilder boot = new ItemBuilder(bootMat).unbreakable(true);
        if (bootMat == Material.LEATHER_BOOTS) boot.color(color);
        if (prot > 0) boot.enchant(org.bukkit.enchantments.Enchantment.PROTECTION, prot);
        player.getInventory().setBoots(boot.build());
    }

    public void updateTeamArmor(Team team) {
        if (team == null) return;
        for (UUID uuid : team.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                updatePlayerArmor(p, team);
            }
        }
    }

    public void giveDefaultEquipment(Player player, Team team) {
        if (player == null) return;
        player.getInventory().clear();

        // Wooden Sword (PLAIN UNENCHANTED AT START!)
        ItemBuilder sword = new ItemBuilder(Material.WOODEN_SWORD).unbreakable(true);
        if (team != null && team.getSharpnessLevel() > 0) {
            sword.enchant(org.bukkit.enchantments.Enchantment.SHARPNESS, team.getSharpnessLevel());
        }
        player.getInventory().setItem(0, sword.build());

        // Equips plain armor with team color (PLAIN UNENCHANTED AT START!)
        updatePlayerArmor(player, team);

        // Restore permanent Shears if owned
        if (hasShears(player.getUniqueId())) {
            ItemStack shears = new ItemBuilder(Material.SHEARS).unbreakable(true).name("§a✂ Hyper Shears").build();
            player.getInventory().addItem(shears);
        }

        // Restore permanent Pickaxe if owned
        int pickTier = getPlayerPickaxeTier(player.getUniqueId());
        if (pickTier > 0) {
            Material pickMat = switch (pickTier) {
                case 1 -> Material.WOODEN_PICKAXE;
                case 2 -> Material.STONE_PICKAXE;
                case 3 -> Material.IRON_PICKAXE;
                default -> Material.DIAMOND_PICKAXE;
            };
            int eff = pickTier >= 4 ? 3 : (pickTier == 3 ? 2 : 1);
            ItemStack pick = new ItemBuilder(pickMat).unbreakable(true)
                    .name(pickTier == 4 ? "§b⛏ Cosmic Drill Pickaxe III" : (pickTier == 3 ? "§f⛏ Steel Drill Pickaxe II" : (pickTier == 2 ? "§a⛏ Stone Pickaxe I" : "§e⛏ Earth Breaker Pickaxe I")))
                    .enchant(org.bukkit.enchantments.Enchantment.EFFICIENCY, eff).build();
            player.getInventory().addItem(pick);
        }

        // Restore permanent Axe if owned
        int axeTier = getPlayerAxeTier(player.getUniqueId());
        if (axeTier > 0) {
            Material axeMat = switch (axeTier) {
                case 1 -> Material.WOODEN_AXE;
                case 2 -> Material.STONE_AXE;
                case 3 -> Material.IRON_AXE;
                default -> Material.DIAMOND_AXE;
            };
            int eff = axeTier >= 4 ? 3 : (axeTier == 3 ? 2 : 1);
            ItemStack axe = new ItemBuilder(axeMat).unbreakable(true)
                    .name(axeTier == 4 ? "§b🪓 Colossal Diamond Waraxe IV" : (axeTier == 3 ? "§6🪓 Berserker Battleaxe III" : (axeTier == 2 ? "§a🪓 Tactical Stone Axe II" : "§e🪓 Lumberjack Wooden Axe I")))
                    .enchant(org.bukkit.enchantments.Enchantment.EFFICIENCY, eff).build();
            player.getInventory().addItem(axe);
        }
    }

    private void handleGameEvents() {
        int d2 = BedwarsPlugin.getInstance().getConfig().getInt("game.diamond-2-time", 120);
        int e2 = BedwarsPlugin.getInstance().getConfig().getInt("game.emerald-2-time", 240);
        int d3 = BedwarsPlugin.getInstance().getConfig().getInt("game.diamond-3-time", 360);
        int e3 = BedwarsPlugin.getInstance().getConfig().getInt("game.emerald-3-time", 480);
        int bedDestroy = BedwarsPlugin.getInstance().getConfig().getInt("game.bed-destroy-time", 600);
        int gameDuration = BedwarsPlugin.getInstance().getConfig().getInt("game.game-duration", 1200);

        if (gameSeconds == d2) {
            setSpawnerTier(SpawnerType.DIAMOND, 2);
            broadcast("§b§l[UPGRADE] §fDiamond Generators upgraded to §bTier II§f!");
            broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else if (gameSeconds == e2) {
            setSpawnerTier(SpawnerType.EMERALD, 2);
            broadcast("§a§l[UPGRADE] §fEmerald Generators upgraded to §aTier II§f!");
            broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } else if (gameSeconds == d3) {
            setSpawnerTier(SpawnerType.DIAMOND, 3);
            broadcast("§b§l[UPGRADE] §fDiamond Generators upgraded to §bTier III (MAX)§f!");
            broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } else if (gameSeconds == e3) {
            setSpawnerTier(SpawnerType.EMERALD, 3);
            broadcast("§a§l[UPGRADE] §fEmerald Generators upgraded to §aTier III (MAX)§f!");
            broadcastSound(Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        } else if (gameSeconds == bedDestroy) {
            triggerSuddenDeath();
        } else if (gameSeconds >= gameDuration) {
            endGame(null);
        }
    }

    private void setSpawnerTier(SpawnerType type, int tier) {
        for (Spawner sp : spawners) {
            if (sp.getType() == type) {
                sp.setTier(tier);
            }
        }
    }

    public void updateTeamSpawnerTier(Team team) {
        if (team == null) return;
        int forge = team.getForgeLevel();

        FileConfiguration config = BedwarsPlugin.getInstance().getConfig();
        int baseIronInterval = config.getInt("spawners.iron-interval", 1);
        int baseIronAmount = config.getInt("spawners.iron-drop-amount", 2);
        int baseGoldInterval = config.getInt("spawners.gold-interval", 60);
        int baseGoldAmount = config.getInt("spawners.gold-drop-amount", 1);

        // Base generator rates loaded from config:
        // Iron: default 1s per 2 items (วิละ 2 ชิ้น)
        // Gold: default 60s per 1 item (1 นาที 1 ชิ้น ในเริ่มต้น)
        int ironInterval = baseIronInterval;
        int ironAmount = switch (forge) {
            case 1 -> baseIronAmount + 1;  // Tier 1 (+50%): 3 pieces per second
            case 2 -> baseIronAmount + 2;  // Tier 2 (+100%): 4 pieces per second
            case 3 -> baseIronAmount + 2;  // Tier 3: 4 pieces per second
            case 4 -> baseIronAmount + 3;  // Tier 4: 5 pieces per second
            default -> baseIronAmount;     // Tier 0: 2 pieces per second (วิละ 2 ชิ้น)
        };

        int goldInterval = switch (forge) {
            case 1 -> Math.max(10, (int)(baseGoldInterval * 0.75)); // Tier 1: 45s
            case 2 -> Math.max(8, (int)(baseGoldInterval * 0.50));  // Tier 2: 30s
            case 3 -> Math.max(6, (int)(baseGoldInterval * 0.35));  // Tier 3: 20s
            case 4 -> Math.max(4, (int)(baseGoldInterval * 0.25));  // Tier 4: 15s
            default -> baseGoldInterval;                            // Tier 0: 60s per item (1 นาที 1 ชิ้น)
        };
        int goldAmount = baseGoldAmount;

        for (Spawner s : spawners) {
            if (s.getTeam() == team) {
                if (s.getType() == SpawnerType.IRON) {
                    s.setInterval(ironInterval);
                    s.setDropAmount(ironAmount);
                } else if (s.getType() == SpawnerType.GOLD) {
                    s.setInterval(goldInterval);
                    s.setDropAmount(goldAmount);
                }
            }
        }

        // Tier 3 & 4: Spawn Diamonds at base (slowly like diamond generators)
        if (forge >= 3) {
            boolean hasDiamond = false;
            for (Spawner s : spawners) {
                if (s.getTeam() == team && s.getType() == SpawnerType.DIAMOND) {
                    hasDiamond = true;
                    s.setInterval(forge >= 4 ? 20 : 30);
                    break;
                }
            }
            if (!hasDiamond && team.getBaseSpawnerLocation() != null) {
                Spawner dia = new Spawner(SpawnerType.DIAMOND, team.getBaseSpawnerLocation(), team);
                dia.setInterval(forge >= 4 ? 20 : 30);
                spawners.add(dia);
                dia.spawnItem();
            }
        }

        // Tier 4 (Max): Spawn Emeralds at base (slowly like emerald generators)
        if (forge >= 4) {
            boolean hasEmerald = false;
            for (Spawner s : spawners) {
                if (s.getTeam() == team && s.getType() == SpawnerType.EMERALD) {
                    hasEmerald = true;
                    s.setInterval(40);
                    break;
                }
            }
            if (!hasEmerald && team.getBaseSpawnerLocation() != null) {
                Spawner em = new Spawner(SpawnerType.EMERALD, team.getBaseSpawnerLocation(), team);
                em.setInterval(40);
                spawners.add(em);
                em.spawnItem();
            }
        }
    }

    private void tickTeamBases() {
        for (Team team : teams.values()) {
            if (team.getSpawnLocation() == null) continue;
            Location baseLoc = team.getSpawnLocation();

            // Heart particle effects around team base spawn when Heal Pool upgrade is purchased
            if (team.hasHealPool() && baseLoc.getWorld() != null && gameSeconds % 2 == 0) {
                baseLoc.getWorld().spawnParticle(org.bukkit.Particle.HEART, baseLoc.clone().add(0, 1.2, 0), 4, 1.2, 0.5, 1.2, 0.05);
            }

            // Check players near base (within 16 blocks)
            for (UUID uuid : players) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || p.getGameMode() == GameMode.SPECTATOR) continue;

                double dist = p.getLocation().distance(baseLoc);
                if (dist <= 16) {
                    Team playerTeam = getPlayerTeam(uuid);
                    if (playerTeam == team) {
                        // Heal pool
                        if (team.hasHealPool()) {
                            if (p.getHealth() < 20.0) {
                                p.setHealth(Math.min(20.0, p.getHealth() + 0.5));
                            }
                            p.getWorld().spawnParticle(org.bukkit.Particle.HEART, p.getLocation().add(0, 1.2, 0), 2, 0.3, 0.4, 0.3, 0.02);
                        }
                        // Iron Skin
                        if (team.hasIronSkin()) {
                            p.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.RESISTANCE, 40, 0, false, false));
                        }
                        // Mineral Magnet (Nerfed: every 5s, 2.0 block radius, gentle pull towards player for minerals only)
                        if (team.hasMineralMagnet() && gameSeconds % 5 == 0) {
                            for (Entity e : p.getNearbyEntities(2.0, 2.0, 2.0)) {
                                if (e instanceof Item dropItem) {
                                    Material mat = dropItem.getItemStack().getType();
                                    if (mat == Material.IRON_INGOT || mat == Material.GOLD_INGOT
                                            || mat == Material.DIAMOND || mat == Material.EMERALD) {
                                        org.bukkit.util.Vector pull = p.getLocation().toVector().subtract(dropItem.getLocation().toVector());
                                        if (pull.lengthSquared() > 0.04) {
                                            pull.normalize().multiply(0.22);
                                            pull.setY(0.08);
                                            dropItem.setVelocity(pull);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Enemy detected! Trigger traps
                        if (!team.getActiveTraps().isEmpty()) {
                            String trap = team.getActiveTraps().remove(0);
                            triggerTrap(team, p, trap);
                        }
                    }
                }
            }
        }
    }

    private void triggerTrap(Team defendingTeam, Player enemy, String trapType) {
        for (UUID uuid : defendingTeam.getPlayers()) {
            Player defender = Bukkit.getPlayer(uuid);
            if (defender != null) {
                MessageUtil.sendTitle(defender, "§c§l[TRAP TRIGGERED!]", "§eAn enemy stepped on your base trap!", 5, 40, 5);
                MessageUtil.playSound(defender, Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, 2f);
                if ("COUNTER".equalsIgnoreCase(trapType)) {
                    defender.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 300, 1));
                    defender.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.JUMP_BOOST, 300, 1));
                    MessageUtil.sendMessage(defender, "§a⚡ Received Speed II & Jump Boost II from Counter-Assault Trap!");
                }
            }
        }
        MessageUtil.sendMessage(enemy, "§cYou triggered a trap belonging to " + defendingTeam.getColor().getDisplayName() + "!");
        if ("ALARM".equalsIgnoreCase(trapType)) {
            enemy.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
            enemy.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.GLOWING, 200, 0));
            MessageUtil.playSound(enemy, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 1f);
        } else if ("GRAVITY".equalsIgnoreCase(trapType)) {
            enemy.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS, 100, 2));
            enemy.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS, 100, 0));
            MessageUtil.playSound(enemy, Sound.ENTITY_WARDEN_DIG, 1f, 1f);
        } else if ("SABOTAGE".equalsIgnoreCase(trapType)) {
            enemy.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.MINING_FATIGUE, 200, 2));
            MessageUtil.playSound(enemy, Sound.BLOCK_ANVIL_LAND, 1f, 0.8f);
        }
    }

    public void handlePlayerDeath(Player victim, Player killer) {
        Team victimTeam = getPlayerTeam(victim.getUniqueId());
        boolean isFinal = (victimTeam == null || !victimTeam.isBedAlive());

        if (killer != null && killer != victim) {
            if (isFinal) {
                addFinalKill(killer.getUniqueId());
                String msg = BedwarsPlugin.getInstance().getMessage("messages.player-final-kill")
                        .replace("{victim}", victim.getName())
                        .replace("{killer}", killer.getName());
                broadcast(msg);
            } else {
                addKill(killer.getUniqueId());
                String msg = BedwarsPlugin.getInstance().getMessage("messages.player-kill")
                        .replace("{victim}", victim.getName())
                        .replace("{killer}", killer.getName());
                broadcast(msg);
            }
        } else {
            if (isFinal) {
                String msg = BedwarsPlugin.getInstance().getMessage("messages.player-final-died")
                        .replace("{player}", victim.getName());
                broadcast(msg);
            } else {
                String msg = BedwarsPlugin.getInstance().getMessage("messages.player-died")
                        .replace("{player}", victim.getName());
                broadcast(msg);
            }
        }

        if (replayRecorder != null) {
            String desc = (killer != null && killer != victim)
                    ? killer.getName() + (isFinal ? " FINAL KILLED " : " killed ") + victim.getName()
                    : victim.getName() + (isFinal ? " finally died." : " died.");
            replayRecorder.recordEvent(gameSeconds, isFinal ? "FINAL_KILL" : "KILL", desc);
        }

        // Drop resources and combat inventory items on the ground at death location
        Location deathLoc = victim.getLocation();
        if (deathLoc.getWorld() != null) {
            for (ItemStack item : victim.getInventory().getContents()) {
                if (item == null || item.getType() == Material.AIR) continue;
                Material mat = item.getType();
                if (mat == Material.WOODEN_SWORD || mat.name().endsWith("_HELMET")
                        || mat.name().endsWith("_CHESTPLATE") || mat.name().endsWith("_LEGGINGS") || mat.name().endsWith("_BOOTS")
                        || mat == Material.COMPASS || mat == Material.CLOCK) {
                    continue;
                }
                deathLoc.getWorld().dropItemNaturally(deathLoc, item.clone());
            }
        }

        victim.getInventory().clear();

        if (isFinal) {
            // Eliminated
            players.remove(victim.getUniqueId());
            spectators.add(victim.getUniqueId());
            victim.setGameMode(GameMode.SPECTATOR);
            victim.getInventory().setItem(0, new ItemBuilder(Material.CLOCK).name("§b§lTeleport to Player §7(Right Click)").build());
            if (spectatorLocation != null) {
                victim.teleport(spectatorLocation);
            }
            MessageUtil.sendTitle(victim, "§c§lYOU WERE ELIMINATED!", "§7Your bed was destroyed! (Right click clock to spectate)", 10, 60, 10);
            checkWinCondition();
        } else {
            // Respawn countdown
            victim.setGameMode(GameMode.SPECTATOR);
            if (spectatorLocation != null) {
                victim.teleport(spectatorLocation);
            }
            startRespawnCountdown(victim, victimTeam);
        }
    }

    private void startRespawnCountdown(Player player, Team team) {
        respawningPlayers.add(player.getUniqueId());
        new BukkitRunnable() {
            int countdown = BedwarsPlugin.getInstance().getConfig().getInt("game.respawn-time", 5);

            @Override
            public void run() {
                if (!player.isOnline() || state != ArenaState.IN_GAME) {
                    respawningPlayers.remove(player.getUniqueId());
                    cancel();
                    return;
                }

                if (countdown > 0) {
                    if (player.getGameMode() != GameMode.SPECTATOR) {
                        player.setGameMode(GameMode.SPECTATOR);
                    }
                    MessageUtil.sendTitle(player, "§c§lYOU DIED!", "§eRespawning in §c" + countdown + " §eseconds...", 0, 22, 0);
                    MessageUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    countdown--;
                } else {
                    respawningPlayers.remove(player.getUniqueId());
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setHealth(20.0);
                    player.setFoodLevel(20);
                    player.setSaturation(20.0f);
                    player.setFireTicks(0);
                    for (var eff : player.getActivePotionEffects()) {
                        player.removePotionEffect(eff.getType());
                    }
                    if (team != null && team.getSpawnLocation() != null) {
                        player.teleport(team.getSpawnLocation());
                    }
                    if (player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED) != null) {
                        player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED).setBaseValue(1024.0);
                    }
                    giveDefaultEquipment(player, team);
                    MessageUtil.sendTitle(player, "§a§lRESPAWNED!", "§fBack into the battle!", 5, 25, 5);
                    MessageUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                    cancel();
                }
            }
        }.runTaskTimer(BedwarsPlugin.getInstance(), 20L, 20L);
    }

    public void breakBed(Player breaker, Block bedBlock) {
        if (breaker == null || bedBlock == null) return;

        Team breakerTeam = getPlayerTeam(breaker.getUniqueId());
        if (breakerTeam == null) {
            for (Team t : teams.values()) {
                if (t.hasPlayer(breaker.getUniqueId()) || t.getPlayers().contains(breaker.getUniqueId())) {
                    breakerTeam = t;
                    break;
                }
            }
        }

        // 1. Strict Self-Bed Check A: Bed block within distance of breaker's configured bed location
        if (breakerTeam != null && breakerTeam.getBedLocation() != null) {
            Location bLoc = breakerTeam.getBedLocation();
            if (bLoc.getWorld() != null && bLoc.getWorld().equals(bedBlock.getWorld())) {
                if (bLoc.distance(bedBlock.getLocation()) <= 4.0) {
                    MessageUtil.sendMessage(breaker, BedwarsPlugin.getInstance().getMessage("messages.cannot-break-own-bed"));
                    bedBlock.getWorld().playSound(bedBlock.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
            }
        }

        // 2. Strict Self-Bed Check B: Breaker team bed material and near breaker base
        if (breakerTeam != null && breakerTeam.getColor().getBedMaterial() == bedBlock.getType()) {
            Location sLoc = breakerTeam.getSpawnLocation();
            if (sLoc != null && sLoc.getWorld() != null && sLoc.getWorld().equals(bedBlock.getWorld())) {
                if (sLoc.distance(bedBlock.getLocation()) < 28.0) {
                    MessageUtil.sendMessage(breaker, BedwarsPlugin.getInstance().getMessage("messages.cannot-break-own-bed"));
                    bedBlock.getWorld().playSound(bedBlock.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
            }
        }

        // 3. Resolve target team by closest configured bed location
        Team targetTeam = null;
        double minDistance = Double.MAX_VALUE;
        for (Team t : teams.values()) {
            if (t.getBedLocation() != null && t.getBedLocation().getWorld() != null && t.getBedLocation().getWorld().equals(bedBlock.getWorld())) {
                double dist = t.getBedLocation().distance(bedBlock.getLocation());
                if (dist < 4.2 && dist < minDistance) {
                    minDistance = dist;
                    targetTeam = t;
                }
            }
        }

        // 4. Fallback resolve target team by bed material near team base
        if (targetTeam == null) {
            for (Team t : teams.values()) {
                if (t.getColor().getBedMaterial() == bedBlock.getType()) {
                    Location sLoc = t.getSpawnLocation();
                    if (sLoc != null && sLoc.getWorld() != null && sLoc.getWorld().equals(bedBlock.getWorld())) {
                        if (sLoc.distance(bedBlock.getLocation()) < 28.0) {
                            targetTeam = t;
                            break;
                        }
                    }
                }
            }
        }

        if (targetTeam == null) {
            MessageUtil.sendMessage(breaker, "§c✖ This bed is not recognized as an enemy bed!");
            return;
        }

        // 5. Final validation: Target team must not be the breaker team!
        if (breakerTeam != null && breakerTeam.equals(targetTeam)) {
            MessageUtil.sendMessage(breaker, BedwarsPlugin.getInstance().getMessage("messages.cannot-break-own-bed"));
            bedBlock.getWorld().playSound(bedBlock.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (!targetTeam.isBedAlive()) {
            MessageUtil.sendMessage(breaker, "§c✖ This team's bed was already destroyed!");
            return;
        }

        targetTeam.setBedAlive(false);
        addBedBroken(breaker.getUniqueId());

        if (replayRecorder != null) {
            replayRecorder.recordEvent(gameSeconds, "BED_BREAK", breaker.getName() + " broke " + targetTeam.getColor().getDisplayName() + "'s bed!");
        }

        // Destroy both bed halves cleanly with ZERO drops
        if (bedBlock.getBlockData() instanceof org.bukkit.block.data.type.Bed bedData) {
            org.bukkit.block.BlockFace facing = bedData.getFacing();
            Block otherHalf = (bedData.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD)
                    ? bedBlock.getRelative(facing.getOppositeFace())
                    : bedBlock.getRelative(facing);
            if (otherHalf.getType().name().endsWith("_BED")) {
                otherHalf.setType(Material.AIR, false);
            }
        }
        bedBlock.setType(Material.AIR, false);

        // Bed destroy effect
        bedBlock.getWorld().spawnParticle(Particle.EXPLOSION, bedBlock.getLocation().add(0.5, 0.5, 0.5), 3);
        bedBlock.getWorld().playSound(bedBlock.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);

        String teamName = targetTeam.getColor().getChatColor() + "● " + targetTeam.getColor().getDisplayName() + " Team";
        String breakerName = (breakerTeam != null ? breakerTeam.getColor().getChatColor() : "§f") + breaker.getName();

        String bannerMsg = "\n§8§m                                                  §r\n"
                + " §c§lBED DESTRUCTION!\n"
                + " " + teamName + "§f's bed was destroyed by " + breakerName + "!\n"
                + "§8§m                                                  §r\n";
        broadcast(bannerMsg);

        // Send Titles to all players in the game
        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (targetTeam.hasPlayer(uuid)) {
                MessageUtil.sendTitle(p, "§c§lBED DESTROYED!", "§eYou will no longer respawn!", 5, 60, 10);
                MessageUtil.playSound(p, Sound.ENTITY_WITHER_DEATH, 1.2f, 1f);
            } else {
                MessageUtil.sendTitle(p, "§c§lBED DESTROYED!", teamName + " §7bed was destroyed", 5, 45, 10);
                MessageUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.1f);
            }
        }

        // Check if any team is eliminated immediately
        checkWinCondition();
    }

    public void breakBedByBot(String breakerName, Team breakerTeam, Team targetTeam, Block bedBlock) {
        if (targetTeam == null || !targetTeam.isBedAlive() || bedBlock == null) return;

        targetTeam.setBedAlive(false);

        if (replayRecorder != null) {
            replayRecorder.recordEvent(gameSeconds, "BED_BREAK", breakerName + " broke " + targetTeam.getColor().getDisplayName() + "'s bed!");
        }

        // Destroy both bed halves cleanly with ZERO drops
        if (bedBlock.getBlockData() instanceof org.bukkit.block.data.type.Bed bedData) {
            org.bukkit.block.BlockFace facing = bedData.getFacing();
            Block otherHalf = (bedData.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD)
                    ? bedBlock.getRelative(facing.getOppositeFace())
                    : bedBlock.getRelative(facing);
            if (otherHalf.getType().name().endsWith("_BED")) {
                otherHalf.setType(Material.AIR, false);
            }
        }
        bedBlock.setType(Material.AIR, false);

        bedBlock.getWorld().spawnParticle(Particle.EXPLOSION, bedBlock.getLocation().add(0.5, 0.5, 0.5), 3);
        bedBlock.getWorld().playSound(bedBlock.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0.8f);

        String teamName = targetTeam.getColor().getChatColor() + "● " + targetTeam.getColor().getDisplayName() + " Team";
        String breakerDisplay = (breakerTeam != null ? breakerTeam.getColor().getChatColor() : "§f") + breakerName;

        String bannerMsg = "\n§8§m                                                  §r\n"
                + " §c§lBED DESTRUCTION!\n"
                + " " + teamName + "§f's bed was destroyed by " + breakerDisplay + "!\n"
                + "§8§m                                                  §r\n";
        broadcast(bannerMsg);

        for (UUID uuid : players) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null) continue;
            if (targetTeam.hasPlayer(uuid)) {
                MessageUtil.sendTitle(p, "§c§lBED DESTROYED!", "§eYou will no longer respawn!", 5, 60, 10);
                MessageUtil.playSound(p, Sound.ENTITY_WITHER_DEATH, 1.2f, 1f);
            } else {
                MessageUtil.sendTitle(p, "§c§lBED DESTROYED!", teamName + " §7bed was destroyed", 5, 45, 10);
                MessageUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.1f);
            }
        }

        checkWinCondition();
    }

    public ReplayRecorder getReplayRecorder() {
        return replayRecorder;
    }

    public Map<UUID, com.bedwars.arena.bot.RejoinBot> getActiveBots() {
        return activeBots;
    }

    private static final String[] PRACTICE_BOT_NAMES = {
            "Alex_Pro", "ShadowNinja", "BedwarsGod", "DragonSlayer", "ViperStrike",
            "NeonGamer", "CyberKnight", "PhantomBlade", "StormRider", "FrostBite",
            "HyperSniper", "AlphaWolf", "TitanKing", "PixelHero", "VortexPvP"
    };

    public boolean startPracticeMatch(Player player, List<Team> botTeams) {
        if (state == ArenaState.IN_GAME) {
            MessageUtil.sendMessage(player, "§c✖ This arena is already running a match!");
            return false;
        }

        if (botTeams == null || botTeams.isEmpty()) {
            MessageUtil.sendMessage(player, "§c✖ Please select at least 1 enemy team with bots!");
            return false;
        }

        // 1. Join player if not already in arena
        if (!players.contains(player.getUniqueId())) {
            if (!join(player)) {
                return false;
            }
        }

        // 2. Assign player to a non-bot team
        Team playerTeam = getPlayerTeam(player.getUniqueId());
        if (playerTeam == null || botTeams.contains(playerTeam)) {
            for (Team t : teams.values()) {
                if (!botTeams.contains(t)) {
                    if (playerTeam != null) playerTeam.removePlayer(player.getUniqueId());
                    t.addPlayer(player.getUniqueId());
                    playerTeam = t;
                    break;
                }
            }
        }

        if (playerTeam == null) {
            MessageUtil.sendMessage(player, "§c✖ Could not find an available team for you!");
            return false;
        }

        // 3. Pre-register bot UUIDs in bot teams so startGame() initializes their base/spawners/beds
        int botIndex = 0;
        List<com.bedwars.arena.bot.RejoinBot> pendingBots = new ArrayList<>();
        for (Team botTeam : botTeams) {
            UUID botUuid = UUID.randomUUID();
            String bName = PRACTICE_BOT_NAMES[botIndex % PRACTICE_BOT_NAMES.length];
            String bSkin = com.bedwars.arena.bot.RejoinBot.BOT_SKIN_PRESETS[botIndex % com.bedwars.arena.bot.RejoinBot.BOT_SKIN_PRESETS.length];

            botTeam.addPlayer(botUuid);
            com.bedwars.arena.bot.RejoinBot bot = new com.bedwars.arena.bot.RejoinBot(this, botUuid, bName, botTeam, bSkin);
            pendingBots.add(bot);
            botIndex++;
        }

        // 4. Start match
        cancelLobbyCountdown();
        startGame();

        // 5. Spawn all the bots into the game and register them in ReplayRecorder
        for (com.bedwars.arena.bot.RejoinBot b : pendingBots) {
            b.getTeam().setBedAlive(true);
            activeBots.put(b.getPlayerUuid(), b);
            b.spawn();
            if (replayRecorder != null) {
                replayRecorder.registerBot(b.getPlayerUuid(), b.getPlayerName(), b.getTeam().getColor().name(), b.getSkinTexture()[0]);
            }
        }

        broadcast("\n§8§m                                                  §r");
        broadcast(" §b§l✦ BOT PRACTICE MATCH STARTED! ✦");
        broadcast(" §fOpposing AI Teams: §e" + botTeams.size() + " Team(s)");
        broadcast(" §7Defend your bed, bridge to mid, and eliminate enemy AI teams!");
        broadcast("§8§m                                                  §r\n");

        MessageUtil.sendTitle(player, "§b§lBOT PRACTICE!", "§eStarted vs " + botTeams.size() + " AI Team(s)", 10, 45, 10);
        MessageUtil.playSound(player, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        return true;
    }

    public void checkWinCondition() {
        if (state != ArenaState.IN_GAME) return;

        // Condition: "ถ้าหลุดกันหมดจะแพ้ทั้งคู่"
        // If all real human players have disconnected, both/all lose! Game ends in Draw immediately.
        boolean hasAnyOnlineHuman = false;
        for (UUID u : players) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) {
                hasAnyOnlineHuman = true;
                break;
            }
        }
        if (!hasAnyOnlineHuman) {
            broadcast("\n§c§l✖ ALL HUMAN PLAYERS DISCONNECTED!\n§eAll teams lose! Game ended in Draw.\n");
            endGame(null);
            return;
        }

        List<Team> remainingTeams = new ArrayList<>();
        for (Team team : teams.values()) {
            boolean hasLivingParticipant = false;
            for (UUID uuid : team.getPlayers()) {
                if (players.contains(uuid) || (team.isBedAlive() && disconnectedPlayers.containsKey(uuid))) {
                    hasLivingParticipant = true;
                    break;
                }
            }
            if (!hasLivingParticipant) {
                for (com.bedwars.arena.bot.RejoinBot bot : activeBots.values()) {
                    if (bot != null && bot.getTeam() != null && bot.getTeam().equals(team)) {
                        if (bot.isActive() || team.isBedAlive()) {
                            hasLivingParticipant = true;
                            break;
                        }
                    }
                }
            }
            if (hasLivingParticipant) {
                remainingTeams.add(team);
            }
        }

        if (remainingTeams.size() <= 1 && state == ArenaState.IN_GAME) {
            Team winner = remainingTeams.isEmpty() ? null : remainingTeams.get(0);
            endGame(winner);
        }
    }

    public void endGame(Team winner) {
        state = ArenaState.ENDING;
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        if (dragonDestructionTask != null) {
            dragonDestructionTask.cancel();
            dragonDestructionTask = null;
        }

        // Despawn any active AI replacement bots
        for (RejoinBot b : activeBots.values()) {
            if (b != null) b.despawn();
        }
        activeBots.clear();

        // Finish and save Replay Data asynchronously
        if (replayRecorder != null) {
            try {
                ReplayData data = replayRecorder.finish(winner, gameSeconds);
                BedwarsPlugin.getInstance().getReplayManager().saveReplayAsync(data);
            } catch (Exception e) {
                BedwarsPlugin.getInstance().getLogger().warning("Failed to save replay: " + e.getMessage());
            }
            replayRecorder = null;
        }

        // Calculate Kill MVP (Most total kills + final kills in this match)
        UUID mvpUuid = null;
        int maxKills = -1;
        int mvpRegularKills = 0;
        int mvpFinals = 0;

        Set<UUID> allParticipants = new HashSet<>(kills.keySet());
        allParticipants.addAll(finalKills.keySet());

        for (UUID u : allParticipants) {
            int reg = kills.getOrDefault(u, 0);
            int fin = finalKills.getOrDefault(u, 0);
            int total = reg + fin;
            if (total > maxKills) {
                maxKills = total;
                mvpUuid = u;
                mvpRegularKills = reg;
                mvpFinals = fin;
            }
        }

        String finalMvpName = "None";
        if (mvpUuid != null) {
            Player mvpPlayer = Bukkit.getPlayer(mvpUuid);
            if (mvpPlayer != null) {
                finalMvpName = mvpPlayer.getName();
            } else {
                finalMvpName = Bukkit.getOfflinePlayer(mvpUuid).getName();
            }
            if (finalMvpName == null) finalMvpName = "Player";
        }

        String winnerDisplay = (winner != null) ? winner.getColor().getChatColor() + "Team " + winner.getColor().getDisplayName() : "§eDraw";

        broadcast("§6§m════════════════════════════════════════════════════");
        broadcast("§f                 §6§l✦ BEDWARS VICTORY ✦");
        broadcast(" ");
        broadcast("§f           Winner: " + winnerDisplay);
        broadcast(" ");
        if (mvpUuid != null && maxKills > 0) {
            broadcast("§6             ★ §e§lKILL MVP OF THE MATCH §6★");
            broadcast("§f                 §b§l" + finalMvpName + " §7(§a" + mvpRegularKills + " §7Kills | §e" + mvpFinals + " §7Finals)");
        } else {
            broadcast("§6             ★ §e§lKILL MVP: §7None");
        }
        broadcast(" ");
        broadcast("§6§m════════════════════════════════════════════════════");

        // Despawn all holograms immediately when game ends!
        for (Spawner spawner : spawners) {
            spawner.destroyHologram();
        }
        com.bedwars.integration.HoloMeryHook.deleteAllBedwarsHolograms();

        // Remove dropped items, primed TNT, Fireballs, dragons, and projectiles so they cannot explode or litter after game ends!
        org.bukkit.World arenaWorld = getWorld();
        if (arenaWorld != null) {
            for (Entity e : arenaWorld.getEntities()) {
                if (e instanceof TNTPrimed || e instanceof org.bukkit.entity.Fireball || e instanceof org.bukkit.entity.SmallFireball
                        || e instanceof Projectile || e instanceof org.bukkit.entity.EnderDragon
                        || e.getScoreboardTags().contains("bw_dragon") || e.getScoreboardTags().contains("bw_spawner_block")
                        || e.getScoreboardTags().contains("bw_golem") || e.getScoreboardTags().contains("bw_ally")) {
                    e.remove();
                } else if (e instanceof Item) {
                    e.remove();
                }
            }
        }

        // Restore OP status to original admins
        for (UUID opUuid : originalOps) {
            Player p = Bukkit.getPlayer(opUuid);
            if (p != null) {
                p.setOp(true);
            }
        }
        originalOps.clear();
        com.bedwars.listeners.PlayerListener.revealAllInvisPlayers();

        final String finalWinnerTitle = (winner != null) ? "§6§lTeam " + winner.getColor().getDisplayName() + " WON!" : "§e§lGAME OVER!";
        final UUID capturedMvp = mvpUuid;
        final int capturedMaxKills = maxKills;
        final String capturedMvpName = finalMvpName;
        final int capturedRegKills = mvpRegularKills;
        final int capturedFinals = mvpFinals;

        // 15-second countdown to return to main lobby with fireworks & Kill MVP celebration!
        new BukkitRunnable() {
            int secondsLeft = 15;

            @Override
            public void run() {
                Set<UUID> all = new HashSet<>(players);
                all.addAll(spectators);

                if (secondsLeft > 0) {
                    for (UUID uuid : all) {
                        Player p = Bukkit.getPlayer(uuid);
                        if (p != null) {
                            if (secondsLeft >= 8 && capturedMvp != null && capturedMaxKills > 0) {
                                MessageUtil.sendTitle(p, "§6§l★ KILL MVP: §b§l" + capturedMvpName + " §6§l★", "§a" + capturedRegKills + " §7Kills | §e" + capturedFinals + " §7Finals §8(§c" + secondsLeft + "s§8)", 0, 25, 0);
                            } else {
                                MessageUtil.sendTitle(p, finalWinnerTitle, "§eReturning to lobby in §c" + secondsLeft + " §es...", 0, 25, 0);
                            }

                            if (secondsLeft % 2 == 0) {
                                spawnVictoryFireworks(p.getLocation());
                                MessageUtil.playSound(p, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.2f, 1f);
                                MessageUtil.playSound(p, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.2f, 1f);
                            }
                        }
                    }
                    secondsLeft--;
                } else {
                    reset();
                    cancel();
                }
            }
        }.runTaskTimer(BedwarsPlugin.getInstance(), 20L, 20L);
    }

    private void spawnVictoryFireworks(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        Firework fw = loc.getWorld().spawn(loc.clone().add(0, 1, 0), Firework.class);
        var meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder()
                .withColor(Color.YELLOW, Color.ORANGE, Color.RED, Color.LIME, Color.AQUA)
                .withFade(Color.WHITE)
                .with(FireworkEffect.Type.BALL_LARGE)
                .flicker(true)
                .trail(true)
                .build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);
        loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
    }

    public static BlockFace getFacingFromYaw(float yaw) {
        yaw = (yaw % 360 + 360) % 360;
        if (yaw >= 45 && yaw < 135) {
            return BlockFace.WEST;
        } else if (yaw >= 135 && yaw < 225) {
            return BlockFace.NORTH;
        } else if (yaw >= 225 && yaw < 315) {
            return BlockFace.EAST;
        } else {
            return BlockFace.SOUTH;
        }
    }

    public static void placeFullBed(Block headBlock, Material bedMaterial) {
        placeFullBed(headBlock, bedMaterial, 180f);
    }

    public static void placeFullBed(Block headBlock, Material bedMaterial, float yaw) {
        if (headBlock == null || bedMaterial == null) return;
        BlockFace lookDir = getFacingFromYaw(yaw);
        Block footBlock = headBlock.getRelative(lookDir);

        // Bed facing in Bukkit points from FOOT to HEAD
        BlockFace bedFacing = lookDir.getOppositeFace();

        // Place Foot
        footBlock.setType(bedMaterial, false);
        if (footBlock.getBlockData() instanceof org.bukkit.block.data.type.Bed footData) {
            footData.setPart(org.bukkit.block.data.type.Bed.Part.FOOT);
            footData.setFacing(bedFacing);
            footBlock.setBlockData(footData, false);
        }

        // Place Head
        headBlock.setType(bedMaterial, false);
        if (headBlock.getBlockData() instanceof org.bukkit.block.data.type.Bed headData) {
            headData.setPart(org.bukkit.block.data.type.Bed.Part.HEAD);
            headData.setFacing(bedFacing);
            headBlock.setBlockData(headData, false);
        }
    }

    public void reset() {
        // Restore OP status to original admins
        for (UUID opUuid : originalOps) {
            Player p = Bukkit.getPlayer(opUuid);
            if (p != null) {
                p.setOp(true);
            }
        }
        originalOps.clear();
        com.bedwars.listeners.PlayerListener.revealAllInvisPlayers();

        // Kick all remaining players to main lobby
        Location mainLobby = BedwarsPlugin.getInstance().getMainLobby();
        Location fallback = (mainLobby != null) ? mainLobby : (lobbyLocation != null ? lobbyLocation : Bukkit.getWorlds().get(0).getSpawnLocation());

        Set<UUID> all = new HashSet<>(players);
        all.addAll(spectators);

        for (UUID uuid : all) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                p.setGameMode(GameMode.SURVIVAL);
                p.setHealth(20.0);
                p.setFoodLevel(20);
                p.setPlayerListName(p.getName());
                if (p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED) != null) {
                    p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_ATTACK_SPEED).setBaseValue(4.0);
                }
                for (var effect : p.getActivePotionEffects()) {
                    p.removePotionEffect(effect.getType());
                }
                p.teleport(fallback);
                MessageUtil.sendMessage(p, "§a✔ Teleported back to lobby!");
                BedwarsPlugin.getInstance().getScoreboardManager().removeScoreboard(p);
            }
        }

        if (dragonDestructionTask != null) {
            dragonDestructionTask.cancel();
            dragonDestructionTask = null;
        }

        players.clear();
        spectators.clear();
        disconnectedPlayers.clear();
        for (RejoinBot b : activeBots.values()) {
            if (b != null) b.despawn();
        }
        activeBots.clear();
        replayRecorder = null;
        respawningPlayers.clear();
        playerArmorTiers.clear();
        playerPickaxeTiers.clear();
        playerAxeTiers.clear();
        playerHasShears.clear();
        kills.clear();
        finalKills.clear();
        bedsBroken.clear();

        // Rollback placed blocks & fluids completely
        for (Location loc : placedBlocks) {
            Block b = loc.getBlock();
            if (b.getType() != Material.AIR) {
                b.setType(Material.AIR, false);
            }
        }
        placedBlocks.clear();

        // Restore original full beds with zero half-bed glitch!
        for (Map.Entry<Location, org.bukkit.block.data.BlockData> entry : originalBedBlockData.entrySet()) {
            Block b = entry.getKey().getBlock();
            b.setType(entry.getValue().getMaterial(), false);
            b.setBlockData(entry.getValue(), false);
        }
        originalBedBlockData.clear();

        // Despawn holograms & clean entities
        for (Spawner spawner : spawners) {
            spawner.destroyHologram();
        }
        com.bedwars.integration.HoloMeryHook.deleteAllBedwarsHolograms();

        // Remove dynamic base spawners so they don't multiply on next match!
        spawners.removeIf(s -> s.getTeam() != null);

        // Restore all chest contents
        for (Map.Entry<Location, ItemStack[]> entry : originalChestContents.entrySet()) {
            if (entry.getKey().getBlock().getState() instanceof org.bukkit.block.Chest chest) {
                chest.getInventory().setContents(entry.getValue());
            }
        }
        originalChestContents.clear();

        // Clean dropped items, Iron Golems & custom entities in arena world
        org.bukkit.World resetWorld = getWorld();
        if (resetWorld != null) {
            for (Entity e : resetWorld.getEntities()) {
                if (e instanceof Item) {
                    e.remove();
                } else if ((e instanceof Villager && e.getScoreboardTags().contains("bw_npc"))
                        || e.getScoreboardTags().contains("bw_golem")
                        || e.getScoreboardTags().contains("bw_ally")
                        || e instanceof TNTPrimed
                        || e instanceof Projectile
                        || e instanceof org.bukkit.entity.EnderDragon
                        || e.getScoreboardTags().contains("bw_dragon")
                        || e.getScoreboardTags().contains("bw_spawner_block")) {
                    e.remove();
                }
            }
        }

        // Reset teams
        for (Team t : teams.values()) {
            t.reset();
        }

        state = ArenaState.WAITING;
        countdown = 15;
    }

    public void spawnNPCs() {
        for (Location loc : shopNpcLocations) {
            spawnShopVillager(loc, null);
        }

        for (Location loc : upgradeNpcLocations) {
            spawnUpgradeVillager(loc, null);
        }

        // Spawn per-team NPCs
        for (Team team : teams.values()) {
            if (team.getShopNpcLocation() != null) {
                spawnShopVillager(team.getShopNpcLocation(), team);
            }
            if (team.getUpgradeNpcLocation() != null) {
                spawnUpgradeVillager(team.getUpgradeNpcLocation(), team);
            }
        }
    }

    public static void spawnShopVillager(Location loc, Team team) {
        if (loc == null || loc.getWorld() == null) return;
        String teamName = (team != null) ? " §7(" + team.getColor().getChatColor() + team.getColor().getDisplayName() + "§7)" : "";
        loc.getWorld().spawn(loc, Villager.class, villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setProfession(Villager.Profession.NONE);
            villager.setVillagerType(Villager.Type.PLAINS);
            villager.setCustomName(MessageUtil.color("§e§l✦ ITEM SHOP" + teamName + " ✦ §7(Right Click)"));
            villager.setCustomNameVisible(true);
            villager.addScoreboardTag("bw_npc");
            villager.addScoreboardTag("bw_shop");
        });
    }

    public static void spawnUpgradeVillager(Location loc, Team team) {
        if (loc == null || loc.getWorld() == null) return;
        String teamName = (team != null) ? " §7(" + team.getColor().getChatColor() + team.getColor().getDisplayName() + "§7)" : "";
        loc.getWorld().spawn(loc, Villager.class, villager -> {
            villager.setAI(false);
            villager.setInvulnerable(true);
            villager.setSilent(true);
            villager.setProfession(Villager.Profession.NONE);
            villager.setVillagerType(Villager.Type.PLAINS);
            villager.setCustomName(MessageUtil.color("§b§l✦ TEAM UPGRADES" + teamName + " ✦ §7(Right Click)"));
            villager.setCustomNameVisible(true);
            villager.addScoreboardTag("bw_npc");
            villager.addScoreboardTag("bw_upgrade");
        });
    }

    public void clearAllArenaDroppedItems() {
        org.bukkit.World w = getWorld();
        if (w != null) {
            // Force load all base spawner and spawn chunks so entities in unloaded chunks are not missed!
            for (Team t : teams.values()) {
                if (t.getBaseSpawnerLocation() != null && t.getBaseSpawnerLocation().getWorld() != null) {
                    Location loc = t.getBaseSpawnerLocation();
                    if (!loc.getChunk().isLoaded()) {
                        loc.getChunk().load(true);
                    }
                    for (Entity e : loc.getWorld().getNearbyEntities(loc, 15, 15, 15)) {
                        if (e instanceof Item) {
                            e.remove();
                        }
                    }
                }
                if (t.getSpawnLocation() != null && t.getSpawnLocation().getWorld() != null) {
                    Location sloc = t.getSpawnLocation();
                    if (!sloc.getChunk().isLoaded()) {
                        sloc.getChunk().load(true);
                    }
                }
            }
            for (Entity e : w.getEntities()) {
                if (e instanceof Item) {
                    e.remove();
                }
            }
        }
    }

    public void triggerSuddenDeath() {
        if (state != ArenaState.IN_GAME) return;
        int bedDestroyTime = BedwarsPlugin.getInstance().getConfig().getInt("game.bed-destroy-time", 600);
        if (gameSeconds < bedDestroyTime) {
            gameSeconds = bedDestroyTime;
        }

        // 1. Physically destroy all beds in world and mark beds destroyed
        for (Team t : teams.values()) {
            t.setBedAlive(false);
            if (t.getBedLocation() != null && t.getBedLocation().getWorld() != null) {
                Block bedBlock = t.getBedLocation().getBlock();
                if (bedBlock.getBlockData() instanceof org.bukkit.block.data.type.Bed bedData) {
                    Block otherHalf = (bedData.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD)
                            ? bedBlock.getRelative(bedData.getFacing().getOppositeFace())
                            : bedBlock.getRelative(bedData.getFacing());
                    otherHalf.setType(Material.AIR, false);
                }
                bedBlock.setType(Material.AIR, false);

                Location bLoc = bedBlock.getLocation().add(0.5, 0.5, 0.5);
                bLoc.getWorld().spawnParticle(Particle.BLOCK, bLoc, 35, 0.4, 0.4, 0.4, 0.1, Material.RED_BED.createBlockData());
                bLoc.getWorld().spawnParticle(Particle.EXPLOSION, bLoc, 2, 0.2, 0.2, 0.2, 0.05);
                bLoc.getWorld().playSound(bLoc, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.8f);
            }
        }

        broadcast("§4§l☠ BED DESTRUCTION! §cAll beds have been destroyed!");
        for (UUID u : players) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                MessageUtil.sendTitle(p, "§4§lBED DESTRUCTION!", "§cAll beds destroyed! Dragons awakened!", 10, 60, 10);
                MessageUtil.playSound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.2f, 0.8f);
            }
        }

        // 2. Spawn Dragons (Team Dragon vs Wild Dragon)
        org.bukkit.World w = getWorld();
        if (w != null) {
            Location center = (pos1 != null && pos2 != null)
                    ? new Location(w, (pos1.getX() + pos2.getX()) / 2, Math.max(pos1.getY(), pos2.getY()) + 15, (pos1.getZ() + pos2.getZ()) / 2)
                    : (lobbyLocation != null ? lobbyLocation.clone().add(0, 20, 0) : w.getSpawnLocation().add(0, 30, 0));

            List<Team> upgradedTeams = new ArrayList<>();
            for (Team t : teams.values()) {
                if (t.hasDragonBuff()) {
                    upgradedTeams.add(t);
                }
            }

            if (!upgradedTeams.isEmpty()) {
                // Team Dragons for each upgraded team: protects own base, destroys enemy bases
                int offset = 0;
                for (Team t : upgradedTeams) {
                    offset += 8;
                    Location spawnLoc = center.clone().add(offset, 0, offset);
                    w.spawn(spawnLoc, org.bukkit.entity.EnderDragon.class, d -> {
                        d.setCustomName(MessageUtil.color(t.getColor().getChatColor() + "§l" + t.getColor().getDisplayName() + " Team Dragon"));
                        d.setCustomNameVisible(true);
                        d.addScoreboardTag("bw_dragon");
                        d.addScoreboardTag("bw_team_dragon");
                        d.addScoreboardTag("bw_team_" + t.getColor().name());
                        d.setPhase(org.bukkit.entity.EnderDragon.Phase.CIRCLING);
                    });
                    broadcast("§5§l[DRAGON] " + t.getColor().getChatColor() + "§lTeam " + t.getColor().getDisplayName() + "§f's Allied Dragon has entered the battlefield!");
                }

                // If only 1 team upgraded, also spawn 1 Wild Dragon for enemy / neutral destruction
                if (upgradedTeams.size() < 2) {
                    w.spawn(center.clone().add(-10, 0, -10), org.bukkit.entity.EnderDragon.class, d -> {
                        d.setCustomName(MessageUtil.color("§5§lWild Ender Dragon"));
                        d.setCustomNameVisible(true);
                        d.addScoreboardTag("bw_dragon");
                        d.addScoreboardTag("bw_wild_dragon");
                        d.setPhase(org.bukkit.entity.EnderDragon.Phase.CIRCLING);
                    });
                }
            } else {
                // NO team upgraded -> Spawn 2 Wild Ender Dragons that destroy ALL bases
                for (int i = 1; i <= 2; i++) {
                    int finalI = i;
                    Location spawnLoc = center.clone().add((i - 1) * 12 - 6, 0, (i - 1) * 12 - 6);
                    w.spawn(spawnLoc, org.bukkit.entity.EnderDragon.class, d -> {
                        d.setCustomName(MessageUtil.color("§5§lWild Ender Dragon " + (finalI == 1 ? "I" : "II")));
                        d.setCustomNameVisible(true);
                        d.addScoreboardTag("bw_dragon");
                        d.addScoreboardTag("bw_wild_dragon");
                        d.setPhase(org.bukkit.entity.EnderDragon.Phase.CIRCLING);
                    });
                }
                broadcast("§5§l[DRAGON] §cWild Ender Dragons have been unleashed to destroy all islands!");
            }

            // 3. Start active dragon destruction task
            startDragonDestructionTask();
        }
    }

    public void startDragonDestructionTask() {
        if (dragonDestructionTask != null && !dragonDestructionTask.isCancelled()) {
            dragonDestructionTask.cancel();
        }

        dragonDestructionTask = new BukkitRunnable() {
            int taskTick = 0;

            @Override
            public void run() {
                if (state != ArenaState.IN_GAME) {
                    cancel();
                    return;
                }
                taskTick++;
                World w = getWorld();
                if (w == null) return;

                List<org.bukkit.entity.EnderDragon> dragons = new ArrayList<>();
                for (Entity e : w.getEntities()) {
                    if (e instanceof org.bukkit.entity.EnderDragon d && d.isValid() && !d.isDead()) {
                        if (d.getScoreboardTags().contains("bw_dragon")) {
                            dragons.add(d);
                        }
                    }
                }

                if (dragons.isEmpty()) return;

                for (org.bukkit.entity.EnderDragon dragon : dragons) {
                    Set<String> tags = dragon.getScoreboardTags();
                    boolean isTeamDragon = tags.contains("bw_team_dragon");
                    String ownerTeamColor = null;
                    for (String tag : tags) {
                        if (tag.startsWith("bw_team_") && !tag.equals("bw_team_dragon")) {
                            ownerTeamColor = tag.substring("bw_team_".length());
                            break;
                        }
                    }

                    // Gather target base locations
                    List<Location> targetBases = new ArrayList<>();
                    for (Team t : teams.values()) {
                        if (isTeamDragon && ownerTeamColor != null && t.getColor().name().equalsIgnoreCase(ownerTeamColor)) {
                            // Friendly team base: PROTECTED! Never target or destroy own island!
                            continue;
                        }
                        Location targetLoc = t.getBedLocation() != null ? t.getBedLocation() : t.getSpawnLocation();
                        if (targetLoc != null) {
                            targetBases.add(targetLoc);
                        }
                    }

                    if (targetBases.isEmpty()) continue;

                    // 1. Prioritize finding the nearest enemy player to hunt!
                    Player targetPlayer = null;
                    double nearestDistSq = Double.MAX_VALUE;
                    Location dLoc = dragon.getLocation();

                    for (UUID u : players) {
                        Player p = Bukkit.getPlayer(u);
                        if (p == null || p.getGameMode() == GameMode.SPECTATOR) continue;
                        Team pTeam = getPlayerTeam(u);
                        if (isTeamDragon && ownerTeamColor != null && pTeam != null && pTeam.getColor().name().equalsIgnoreCase(ownerTeamColor)) {
                            continue; // Friendly team member: PROTECTED!
                        }
                        if (p.getWorld().equals(dLoc.getWorld())) {
                            double dist = p.getLocation().distanceSquared(dLoc);
                            if (dist < nearestDistSq) {
                                nearestDistSq = dist;
                                targetPlayer = p;
                            }
                        }
                    }

                    Location targetLocation = null;
                    if (targetPlayer != null && nearestDistSq <= 6400.0) { // within 80 blocks
                        targetLocation = targetPlayer.getLocation().add(0, 1.2, 0);
                        dragon.setTarget(targetPlayer);
                    } else if (!targetBases.isEmpty()) {
                        // Fallback to rotating island bases
                        int targetIndex = (taskTick / 3) % targetBases.size();
                        targetLocation = targetBases.get(targetIndex).clone().add(0, 4, 0);
                    }

                    if (targetLocation == null) continue;

                    // Flight steering towards target (Aggressive swoop!)
                    org.bukkit.util.Vector toTarget = targetLocation.toVector().subtract(dLoc.toVector());
                    double distSq = toTarget.lengthSquared();

                    if (distSq > 9.0) {
                        org.bukkit.util.Vector flyVec = toTarget.normalize().multiply(0.70);
                        flyVec.setY(Math.max(-0.45, Math.min(0.45, flyVec.getY())));
                        dragon.setVelocity(flyVec);
                    }

                    // 2. Destroy player-placed blocks within 6 blocks of dragon's current position
                    for (int x = -5; x <= 5; x++) {
                        for (int y = -3; y <= 3; y++) {
                            for (int z = -5; z <= 5; z++) {
                                Location check = dLoc.clone().add(x, y, z);
                                if (placedBlocks.contains(check)) {
                                    check.getBlock().setType(Material.AIR, false);
                                    placedBlocks.remove(check);
                                    w.spawnParticle(Particle.DRAGON_BREATH, check.clone().add(0.5, 0.5, 0.5), 2, 0.15, 0.15, 0.15, 0.02);
                                }
                            }
                        }
                    }

                    // 3. Player Attack & Island Bombardment
                    if (targetPlayer != null && dLoc.distance(targetPlayer.getLocation()) <= 10.0) {
                        Location pLoc = targetPlayer.getLocation();
                        w.spawnParticle(Particle.DRAGON_BREATH, pLoc.clone().add(0, 1, 0), 40, 1.5, 1.0, 1.5, 0.08);
                        w.spawnParticle(Particle.EXPLOSION, pLoc.clone().add(0, 0.5, 0), 3, 0.5, 0.5, 0.5, 0.05);
                        w.playSound(pLoc, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.5f, 0.8f);

                        // Deal heavy player damage & armor wear
                        targetPlayer.damage(4.0); // 2 full hearts
                        org.bukkit.util.Vector fling = pLoc.toVector().subtract(dLoc.toVector()).normalize().multiply(0.75);
                        fling.setY(0.45);
                        targetPlayer.setVelocity(fling);

                        // Damage armor durability
                        for (ItemStack armor : targetPlayer.getInventory().getArmorContents()) {
                            if (armor != null && armor.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg) {
                                dmg.setDamage(dmg.getDamage() + 10);
                                armor.setItemMeta(dmg);
                            }
                        }

                        // Destroy player-placed blocks around victim
                        List<Location> toRemove = new ArrayList<>();
                        for (Location placedLoc : placedBlocks) {
                            if (placedLoc.getWorld().equals(pLoc.getWorld()) && placedLoc.distanceSquared(pLoc) <= 36.0) {
                                placedLoc.getBlock().setType(Material.AIR, false);
                                toRemove.add(placedLoc);
                                w.spawnParticle(Particle.SMOKE, placedLoc.clone().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.02);
                                if (toRemove.size() >= 8) break;
                            }
                        }
                        placedBlocks.removeAll(toRemove);
                    } else if (!targetBases.isEmpty() && dLoc.distance(targetLocation) <= 25.0) {
                        w.spawnParticle(Particle.DRAGON_BREATH, targetLocation.clone().add(0, 1.5, 0), 25, 2.0, 1.0, 2.0, 0.05);
                        w.spawnParticle(Particle.EXPLOSION, targetLocation.clone().add(0, 1.0, 0), 2, 0.5, 0.5, 0.5, 0.02);
                        w.playSound(targetLocation, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1.0f, 0.9f);

                        List<Location> toRemove = new ArrayList<>();
                        for (Location placedLoc : placedBlocks) {
                            if (placedLoc.getWorld().equals(targetLocation.getWorld()) && placedLoc.distanceSquared(targetLocation) <= 64.0) {
                                placedLoc.getBlock().setType(Material.AIR, false);
                                toRemove.add(placedLoc);
                                w.spawnParticle(Particle.SMOKE, placedLoc.clone().add(0.5, 0.5, 0.5), 3, 0.2, 0.2, 0.2, 0.02);
                                if (toRemove.size() >= 8) break;
                            }
                        }
                        placedBlocks.removeAll(toRemove);
                    }
                }
            }
        }.runTaskTimer(BedwarsPlugin.getInstance(), 20L, 40L);
    }

    public void broadcastSound(Sound sound, float volume, float pitch) {
        Set<UUID> all = new HashSet<>(players);
        all.addAll(spectators);
        for (UUID uuid : all) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.playSound(p.getLocation(), sound, volume, pitch);
            }
        }
    }

    public void broadcast(String message) {
        Set<UUID> all = new HashSet<>(players);
        all.addAll(spectators);
        for (UUID uuid : all) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                MessageUtil.sendMessage(p, message);
            }
        }
    }
}
