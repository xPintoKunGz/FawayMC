package com.bedwars.replay;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.TeamColor;
import com.bedwars.util.ItemBuilder;
import com.bedwars.util.MessageUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

/**
 * Hypixel-style Replay Session:
 * - Real 3D Minecraft Player models with full player skins (ReplayNPC)
 * - Smooth 20 FPS movement interpolation, sprinting, sneaking, jumping, and arm swings
 * - 100% isolated: packet-based (0 entities on server world), mutual player hiding, and client-side block display
 */
public class ReplaySession {

    private final BedwarsPlugin plugin;
    private final Player viewer;
    private final ReplayData data;

    private double currentTimeSeconds = 0.0;
    private boolean paused = false;
    private double speed = 1.0; // 0.5, 1.0, 2.0, 4.0

    private BukkitTask playbackTask;
    private final Map<String, ReplayNPC> avatars = new HashMap<>(); // UUID -> Packet Player NPC
    private final Map<Location, String> replayPlacedBlocks = new HashMap<>();
    private final Set<Integer> triggeredEventIndices = new HashSet<>();
    private int lastRenderedSec = -1;
    private Scoreboard replayScoreboard;

    public ReplaySession(BedwarsPlugin plugin, Player viewer, ReplayData data) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.data = data;
    }

    public void start() {
        World world = Bukkit.getWorld(data.getWorldName());
        if (world == null && !Bukkit.getWorlds().isEmpty()) {
            world = Bukkit.getWorlds().get(0);
        }

        // 1. Complete Isolation: Hide viewer from ALL players, and hide ALL players from viewer
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(viewer.getUniqueId())) {
                other.hidePlayer(plugin, viewer);
                viewer.hidePlayer(plugin, other);
            }
        }

        // 2. Spectator/Adventure properties
        viewer.setGameMode(GameMode.ADVENTURE);
        viewer.setAllowFlight(true);
        viewer.setFlying(true);
        viewer.setInvisible(true);
        viewer.setCollidable(false);
        try {
            viewer.setCanPickupItems(false);
        } catch (Throwable ignored) {}

        giveControls();

        // 3. Setup client-side replay scoreboard for team colors on player NPC nametags
        setupReplayScoreboard();

        // 4. Teleport to match arena
        if (world != null) {
            Arena arena = plugin.getArenaManager().getArena(data.getArenaName());
            Location targetLoc = (arena != null && arena.getSpectatorLocation() != null)
                    ? arena.getSpectatorLocation()
                    : world.getSpawnLocation().clone().add(0, 10, 0);
            viewer.teleport(targetLoc);
        }

        MessageUtil.sendTitle(viewer, "§6§lMATCH REPLAY", "§eWatching " + data.getArenaName() + " (Hypixel Player Style)", 10, 45, 10);
        MessageUtil.playSound(viewer, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);

        startTicker();
    }

    private void setupReplayScoreboard() {
        try {
            Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
            for (TeamColor tc : TeamColor.values()) {
                Team t = sb.registerNewTeam("rp_" + tc.name().toLowerCase());
                t.setColor(tc.getChatColor());
                t.setPrefix(tc.getChatColor() + "[" + tc.name() + "] ");
            }

            // Register all participant names to their team color
            for (Map.Entry<String, String> entry : data.getPlayerNames().entrySet()) {
                String uuidStr = entry.getKey();
                String name = entry.getValue();
                String teamColorStr = data.getPlayerTeams().getOrDefault(uuidStr, "WHITE");
                try {
                    TeamColor tc = TeamColor.valueOf(teamColorStr.toUpperCase());
                    Team t = sb.getTeam("rp_" + tc.name().toLowerCase());
                    if (t != null) {
                        t.addEntry(name);
                    }
                } catch (Exception ignored) {}
            }

            this.replayScoreboard = sb;
            viewer.setScoreboard(sb);
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[Bedwars Replay] Scoreboard setup: " + t.getMessage());
        }
    }

    private void giveControls() {
        viewer.getInventory().clear();
        viewer.getInventory().setItem(0, new ItemBuilder(Material.REDSTONE).name("§c⏪ Rewind 10s §7(Right Click)").build());
        viewer.getInventory().setItem(1, new ItemBuilder(Material.ARROW).name("§e◀ Previous Event §7(Right Click)").build());
        viewer.getInventory().setItem(2, new ItemBuilder(paused ? Material.CLOCK : Material.COMPASS)
                .name(paused ? "§a▶ Resume Playback §7(Right Click)" : "§6⏸ Pause Playback §7(Right Click)").build());
        viewer.getInventory().setItem(3, new ItemBuilder(Material.SPECTRAL_ARROW).name("§e▶ Next Event §7(Right Click)").build());
        viewer.getInventory().setItem(4, new ItemBuilder(Material.GLOWSTONE_DUST).name("§a⏩ Fast Forward 10s §7(Right Click)").build());
        viewer.getInventory().setItem(5, new ItemBuilder(Material.SUGAR).name("§b⚡ Speed: §e" + speed + "x §7(Right Click to Cycle)").build());
        viewer.getInventory().setItem(8, new ItemBuilder(Material.BARRIER).name("§4🚪 Exit Replay §7(Right Click)").build());
    }

    private void startTicker() {
        if (playbackTask != null) {
            playbackTask.cancel();
        }

        // Run at 20 FPS (every 1 tick) for silky-smooth motion and real player animations!
        playbackTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (stopped || !viewer.isOnline() || !plugin.getReplayManager().isWatchingReplay(viewer)) {
                    stop();
                    cancel();
                    return;
                }

                if (!paused) {
                    renderTick(currentTimeSeconds);
                    currentTimeSeconds += (0.05 * speed); // 1 tick = 0.05s
                    if (currentTimeSeconds >= data.getDurationSeconds()) {
                        currentTimeSeconds = data.getDurationSeconds();
                        paused = true;
                        giveControls();
                        MessageUtil.sendTitle(viewer, "§a§lREPLAY FINISHED", "§7End of recorded match", 10, 40, 10);
                    }
                }
                updateActionBar();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private void renderTick(double currentSec) {
        World world = viewer.getWorld();
        int sampleRate = data.getSampleRate() > 0 ? data.getSampleRate() : 5;

        double sampleFloat = currentSec * sampleRate;
        int indexA = (int) Math.floor(sampleFloat);
        int indexB = indexA + 1;
        double alpha = Math.max(0.0, Math.min(1.0, sampleFloat - indexA));

        List<ReplayData.PlayerSnapshot> frameA = data.getTimeline().get(indexA);
        List<ReplayData.PlayerSnapshot> frameB = data.getTimeline().get(indexB);
        if (frameA == null && frameB != null) frameA = frameB;
        if (frameB == null) frameB = frameA;

        // 1. Render and interpolate real player skin NPCs
        if (frameA != null) {
            Map<String, ReplayData.PlayerSnapshot> nextMap = new HashMap<>();
            if (frameB != null) {
                for (ReplayData.PlayerSnapshot b : frameB) {
                    nextMap.put(b.uuid, b);
                }
            }

            Set<String> activeUuids = new HashSet<>();
            for (ReplayData.PlayerSnapshot snapA : frameA) {
                activeUuids.add(snapA.uuid);
                ReplayData.PlayerSnapshot snapB = nextMap.getOrDefault(snapA.uuid, snapA);

                // Smooth position and rotation interpolation
                double x = lerp(snapA.x, snapB.x, alpha);
                double y = lerp(snapA.y, snapB.y, alpha);
                double z = lerp(snapA.z, snapB.z, alpha);
                float yaw = lerpAngle(snapA.yaw, snapB.yaw, (float) alpha);
                float pitch = (float) lerp(snapA.pitch, snapB.pitch, alpha);

                Location targetLoc = new Location(world, x, y, z, yaw, pitch);

                ReplayNPC npc = avatars.get(snapA.uuid);
                if (npc == null || !npc.isSpawned()) {
                    String[] skin = data.getPlayerSkins().get(snapA.uuid);
                    String skinVal = (skin != null && skin.length > 0) ? skin[0] : null;
                    String skinSig = (skin != null && skin.length > 1) ? skin[1] : null;
                    TeamColor tc = TeamColor.WHITE;
                    try {
                        tc = TeamColor.valueOf(snapA.teamColor.toUpperCase());
                    } catch (Exception ignored) {}

                    UUID pUuid;
                    try {
                        pUuid = UUID.fromString(snapA.uuid);
                    } catch (Exception e) {
                        pUuid = UUID.nameUUIDFromBytes(snapA.name.getBytes());
                    }

                    npc = new ReplayNPC(pUuid, snapA.name, tc, skinVal, skinSig);
                    npc.spawn(viewer, targetLoc);
                    avatars.put(snapA.uuid, npc);
                }

                // Smooth teleport, rotation, sneaking, sprinting
                npc.teleportAndRotate(viewer, targetLoc, pitch, snapA.sneaking, snapA.sprinting);

                // Swing arm if attacking
                if (snapA.swinging) {
                    npc.swingArm(viewer);
                    viewer.spawnParticle(Particle.SWEEP_ATTACK, targetLoc.clone().add(0, 1.0, 0).add(targetLoc.getDirection().multiply(0.8)), 1);
                }

                // Equip weapons & armor
                ItemStack mainHand = parseItem(snapA.heldItem);
                ItemStack offHand = parseItem(snapA.offHandItem);
                ItemStack helmet = parseArmor(snapA.helmet, snapA.teamColor);
                ItemStack chest = parseArmor(snapA.chestplate, snapA.teamColor);
                ItemStack legs = parseArmor(snapA.leggings, snapA.teamColor);
                ItemStack boots = parseArmor(snapA.boots, snapA.teamColor);
                npc.updateEquipment(viewer, mainHand, offHand, helmet, chest, legs, boots);
            }

            // Remove NPCs of players no longer active in frame
            avatars.entrySet().removeIf(entry -> {
                if (!activeUuids.contains(entry.getKey())) {
                    entry.getValue().destroy(viewer);
                    return true;
                }
                return false;
            });
        }

        // 2. Client-side Block Changes (Process only when second advances to avoid packet spam!)
        int secFloor = (int) currentSec;
        if (secFloor != lastRenderedSec) {
            if (lastRenderedSec == -1) {
                // First initialization: apply all blocks up to current second
                for (ReplayData.BlockChange bc : data.getBlockChanges()) {
                    if (bc.second <= secFloor) {
                        applyBlockChange(world, bc);
                    }
                }
            } else if (secFloor > lastRenderedSec) {
                // Forward progression: apply newly reached seconds
                for (ReplayData.BlockChange bc : data.getBlockChanges()) {
                    if (bc.second > lastRenderedSec && bc.second <= secFloor) {
                        applyBlockChange(world, bc);
                    }
                }
            } else {
                // Backward progression (rewind): revert blocks that happened after secFloor
                for (ReplayData.BlockChange bc : data.getBlockChanges()) {
                    if (bc.second > secFloor && bc.second <= lastRenderedSec) {
                        revertBlockChange(world, bc);
                    }
                }
            }
            lastRenderedSec = secFloor;
        }

        // 3. Trigger events (Kill, Bed Break, etc.)
        for (int i = 0; i < data.getEvents().size(); i++) {
            ReplayData.ReplayEvent ev = data.getEvents().get(i);
            if (ev.second == secFloor && !triggeredEventIndices.contains(i)) {
                triggeredEventIndices.add(i);
                if ("BED_BREAK".equals(ev.type)) {
                    MessageUtil.sendTitle(viewer, "§c§lBED DESTROYED!", ev.description, 5, 30, 5);
                    MessageUtil.playSound(viewer, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1f);
                } else if ("FINAL_KILL".equals(ev.type) || "KILL".equals(ev.type)) {
                    viewer.sendMessage("§e[Replay Event] " + ev.description);
                    MessageUtil.playSound(viewer, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.2f);
                }
            }
        }
    }

    private void applyBlockChange(World world, ReplayData.BlockChange bc) {
        Location bLoc = new Location(world, bc.x, bc.y, bc.z);
        if (bc.isPlaced) {
            try {
                viewer.sendBlockChange(bLoc, Bukkit.createBlockData(bc.blockData));
                replayPlacedBlocks.put(bLoc, bc.blockData);
            } catch (Exception ignored) {}
        } else {
            viewer.sendBlockChange(bLoc, Material.AIR.createBlockData());
            replayPlacedBlocks.remove(bLoc);
        }
    }

    private void revertBlockChange(World world, ReplayData.BlockChange bc) {
        Location bLoc = new Location(world, bc.x, bc.y, bc.z);
        // Reset client-side visual view to actual real world block
        viewer.sendBlockChange(bLoc, bLoc.getBlock().getBlockData());
        replayPlacedBlocks.remove(bLoc);
    }

    private ItemStack parseArmor(String armorName, String teamColorStr) {
        if (armorName == null || armorName.equals("AIR")) return new ItemStack(Material.AIR);
        try {
            Material mat = Material.valueOf(armorName);
            ItemStack item = new ItemStack(mat);
            if (mat == Material.LEATHER_HELMET || mat == Material.LEATHER_CHESTPLATE ||
                mat == Material.LEATHER_LEGGINGS || mat == Material.LEATHER_BOOTS) {
                LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
                if (meta != null) {
                    meta.setColor(getTeamDyeColor(teamColorStr));
                    item.setItemMeta(meta);
                }
            }
            return item;
        } catch (Exception e) {
            return new ItemStack(Material.AIR);
        }
    }

    private Color getTeamDyeColor(String teamColorStr) {
        if (teamColorStr == null) return Color.WHITE;
        try {
            TeamColor tc = TeamColor.valueOf(teamColorStr.toUpperCase());
            return switch (tc) {
                case RED -> Color.RED;
                case BLUE -> Color.BLUE;
                case GREEN -> Color.LIME;
                case YELLOW -> Color.YELLOW;
                case AQUA -> Color.AQUA;
                case WHITE -> Color.WHITE;
                case PINK -> Color.FUCHSIA;
                case GRAY -> Color.GRAY;
                default -> Color.WHITE;
            };
        } catch (Exception e) {
            return Color.WHITE;
        }
    }

    private double lerp(double a, double b, double f) {
        return a + f * (b - a);
    }

    private float lerpAngle(float from, float to, float f) {
        float diff = ((to - from + 180f) % 360f + 360f) % 360f - 180f;
        return from + diff * f;
    }

    private ItemStack parseItem(String matName) {
        if (matName == null || matName.equals("AIR")) return new ItemStack(Material.AIR);
        try {
            return new ItemStack(Material.valueOf(matName));
        } catch (Exception e) {
            return new ItemStack(Material.AIR);
        }
    }

    private void updateActionBar() {
        String stateIcon = paused ? "§c⏸ PAUSED" : "§a▶ PLAYING " + speed + "x";
        String timeStr = formatTime((int) currentTimeSeconds) + " / " + formatTime(data.getDurationSeconds());
        String msg = "§6§lREPLAY §8» §f[" + stateIcon + "§f] §e" + timeStr + " §8| §7Slot 2: Pause/Play §8| §7Slot 8: Exit";
        viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(msg));
    }

    private String formatTime(int secs) {
        int m = secs / 60;
        int s = secs % 60;
        return String.format("%02d:%02d", m, s);
    }

    public void togglePause() {
        this.paused = !this.paused;
        giveControls();
        MessageUtil.playSound(viewer, Sound.UI_BUTTON_CLICK, 1f, paused ? 0.8f : 1.2f);
    }

    public void rewind10s() {
        seek(Math.max(0, currentTimeSeconds - 10.0));
    }

    public void forward10s() {
        seek(Math.min(data.getDurationSeconds(), currentTimeSeconds + 10.0));
    }

    public void previousEvent() {
        int target = 0;
        int curSec = (int) currentTimeSeconds;
        for (ReplayData.ReplayEvent ev : data.getEvents()) {
            if (ev.second < curSec - 1 && ev.second > target) {
                target = ev.second;
            }
        }
        seek(target);
    }

    public void nextEvent() {
        int curSec = (int) currentTimeSeconds;
        for (ReplayData.ReplayEvent ev : data.getEvents()) {
            if (ev.second > curSec + 1) {
                seek(ev.second);
                return;
            }
        }
        forward10s();
    }

    public void cycleSpeed() {
        if (speed == 0.5) speed = 1.0;
        else if (speed == 1.0) speed = 2.0;
        else if (speed == 2.0) speed = 4.0;
        else speed = 0.5;

        giveControls();
        MessageUtil.playSound(viewer, Sound.UI_BUTTON_CLICK, 1f, (float) (speed * 0.5 + 0.5));
    }

    public void seek(double targetSecond) {
        targetSecond = Math.max(0.0, Math.min(data.getDurationSeconds(), targetSecond));
        World world = viewer.getWorld();

        int toSec = (int) targetSecond;

        // Reset client-side visual blocks to clean world state
        for (Location loc : replayPlacedBlocks.keySet()) {
            viewer.sendBlockChange(loc, loc.getBlock().getBlockData());
        }
        replayPlacedBlocks.clear();

        // Re-apply all block changes up to toSec
        for (ReplayData.BlockChange bc : data.getBlockChanges()) {
            if (bc.second <= toSec) {
                applyBlockChange(world, bc);
            }
        }

        triggeredEventIndices.removeIf(idx -> data.getEvents().get(idx).second > toSec);
        this.lastRenderedSec = toSec;
        this.currentTimeSeconds = targetSecond;

        renderTick(currentTimeSeconds);
        updateActionBar();
        MessageUtil.playSound(viewer, Sound.ENTITY_ENDERMAN_TELEPORT, 0.5f, 1.5f);
    }

    private boolean stopped = false;

    public void stop() {
        stop(false);
    }

    public void stop(boolean silent) {
        if (stopped) return;
        stopped = true;

        if (playbackTask != null) {
            playbackTask.cancel();
            playbackTask = null;
        }

        // 0. Remove from active sessions immediately
        if (plugin.getReplayManager() != null) {
            plugin.getReplayManager().removeSession(viewer.getUniqueId());
        }

        // Clear action bar immediately and queue resets to erase any client buffer
        if (viewer.isOnline()) {
            viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(" "));
            for (long delay : new long[]{1L, 2L, 5L, 10L}) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (viewer.isOnline() && (plugin.getReplayManager() == null || !plugin.getReplayManager().isWatchingReplay(viewer))) {
                        viewer.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(" "));
                    }
                }, delay);
            }
        }

        // 1. Destroy all packet player NPCs
        for (ReplayNPC npc : avatars.values()) {
            if (npc != null) {
                npc.destroy(viewer);
            }
        }
        avatars.clear();

        // 2. Reset client-side block visual changes back to normal world blocks
        for (Location loc : replayPlacedBlocks.keySet()) {
            if (viewer.isOnline()) {
                viewer.sendBlockChange(loc, loc.getBlock().getBlockData());
            }
        }
        replayPlacedBlocks.clear();
        triggeredEventIndices.clear();

        // 3. Restore scoreboard
        try {
            if (viewer.isOnline()) {
                viewer.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        } catch (Throwable ignored) {}

        // 4. Restore visibility with all online players
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (!other.getUniqueId().equals(viewer.getUniqueId())) {
                other.showPlayer(plugin, viewer);
                viewer.showPlayer(plugin, other);
            }
        }

        // 5. Restore player mode and teleport to main lobby
        if (viewer.isOnline()) {
            viewer.getInventory().clear();
            viewer.setGameMode(GameMode.SURVIVAL);
            viewer.setFlying(false);
            viewer.setAllowFlight(false);
            viewer.setInvisible(false);
            viewer.setCollidable(true);
            try {
                viewer.setCanPickupItems(true);
            } catch (Throwable ignored) {}

            if (plugin.getArenaManager().getArena(viewer) == null) {
                Location mainLobby = plugin.getMainLobby();
                if (mainLobby == null && !Bukkit.getWorlds().isEmpty()) {
                    mainLobby = Bukkit.getWorlds().get(0).getSpawnLocation();
                }
                if (mainLobby != null) {
                    viewer.teleport(mainLobby);
                }
            }
            if (!silent) {
                MessageUtil.sendMessage(viewer, "§a✔ Exited Replay Mode.");
                MessageUtil.playSound(viewer, Sound.ENTITY_ITEM_BREAK, 1f, 1f);
            }
        }
    }
}
