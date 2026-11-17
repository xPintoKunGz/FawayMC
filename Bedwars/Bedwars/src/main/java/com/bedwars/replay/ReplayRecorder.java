package com.bedwars.replay;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReplayRecorder implements Listener {

    private final Arena arena;
    private final ReplayData data;
    private boolean recording = false;

    private BukkitTask sampleTask;
    private int sampleIndex = 0;
    private final Set<UUID> recentSwings = ConcurrentHashMap.newKeySet();

    public static final int SAMPLE_INTERVAL_TICKS = 4; // 4 ticks = 5 frames per second
    public static final int SAMPLES_PER_SECOND = 20 / SAMPLE_INTERVAL_TICKS;

    public ReplayRecorder(Arena arena) {
        this.arena = arena;
        String timeStr = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String replayId = "replay_" + arena.getName().toLowerCase() + "_" + timeStr;
        String worldName = (arena.getWorld() != null) ? arena.getWorld().getName() : "world";
        this.data = new ReplayData(replayId, arena.getName(), worldName, System.currentTimeMillis());
        this.data.setSampleRate(SAMPLES_PER_SECOND);
    }

    public void start() {
        this.recording = true;
        this.sampleIndex = 0;

        // Cache initial participant information
        for (UUID uuid : arena.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                Team team = arena.getPlayerTeam(uuid);
                String teamName = (team != null) ? team.getColor().name() : "WHITE";
                data.getPlayerNames().put(uuid.toString(), p.getName());
                data.getPlayerTeams().put(uuid.toString(), teamName);
                String[] skin = extractPlayerSkin(p);
                if (skin != null) {
                    data.getPlayerSkins().put(uuid.toString(), skin);
                }
            }
        }

        // Register arm swing listener
        Bukkit.getPluginManager().registerEvents(this, BedwarsPlugin.getInstance());

        // Fast high-fidelity snapshot recording task: runs every 4 ticks (5 FPS)
        sampleTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!recording) {
                    cancel();
                    return;
                }
                captureSample(sampleIndex++);
            }
        }.runTaskTimer(BedwarsPlugin.getInstance(), 0L, SAMPLE_INTERVAL_TICKS);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerAnimation(PlayerAnimationEvent event) {
        if (!recording) return;
        if (event.getAnimationType() == PlayerAnimationType.ARM_SWING) {
            Player p = event.getPlayer();
            if (arena.getPlayers().contains(p.getUniqueId())) {
                recentSwings.add(p.getUniqueId());
            }
        }
    }

    public void recordSwing(UUID uuid) {
        if (uuid != null) {
            recentSwings.add(uuid);
        }
    }

    public void registerBot(UUID botUuid, String botName, String teamColor, String skinValue) {
        if (botUuid == null) return;
        data.getPlayerNames().put(botUuid.toString(), botName);
        data.getPlayerTeams().put(botUuid.toString(), teamColor != null ? teamColor : "WHITE");
        if (skinValue != null && !skinValue.isEmpty()) {
            data.getPlayerSkins().put(botUuid.toString(), new String[]{skinValue, ""});
        }
    }

    private void captureSample(int frame) {
        List<ReplayData.PlayerSnapshot> snapshots = new ArrayList<>();

        // 1. Capture real human players
        for (UUID uuid : arena.getPlayers()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;

            Team team = arena.getPlayerTeam(uuid);
            String teamColor = (team != null) ? team.getColor().name() : "WHITE";

            ReplayData.PlayerSnapshot snap = new ReplayData.PlayerSnapshot();
            snap.uuid = uuid.toString();
            snap.name = p.getName();
            snap.teamColor = teamColor;

            if (!data.getPlayerSkins().containsKey(uuid.toString())) {
                String[] skin = extractPlayerSkin(p);
                if (skin != null) {
                    data.getPlayerSkins().put(uuid.toString(), skin);
                }
            }

            Location loc = p.getLocation();
            snap.x = Math.round(loc.getX() * 1000.0) / 1000.0;
            snap.y = Math.round(loc.getY() * 1000.0) / 1000.0;
            snap.z = Math.round(loc.getZ() * 1000.0) / 1000.0;
            snap.yaw = Math.round(loc.getYaw() * 10.0f) / 10.0f;
            snap.pitch = Math.round(loc.getPitch() * 10.0f) / 10.0f;
            snap.sneaking = p.isSneaking();
            snap.sprinting = p.isSprinting();
            snap.swinging = recentSwings.remove(uuid);
            snap.health = Math.round(p.getHealth() * 10.0) / 10.0;

            ItemStack mainHand = p.getInventory().getItemInMainHand();
            snap.heldItem = (mainHand != null && mainHand.getType() != Material.AIR) ? mainHand.getType().name() : "AIR";

            ItemStack offHand = p.getInventory().getItemInOffHand();
            snap.offHandItem = (offHand != null && offHand.getType() != Material.AIR) ? offHand.getType().name() : "AIR";

            ItemStack helm = p.getInventory().getHelmet();
            snap.helmet = (helm != null) ? helm.getType().name() : "AIR";
            ItemStack chest = p.getInventory().getChestplate();
            snap.chestplate = (chest != null) ? chest.getType().name() : "AIR";
            ItemStack leg = p.getInventory().getLeggings();
            snap.leggings = (leg != null) ? leg.getType().name() : "AIR";
            ItemStack boot = p.getInventory().getBoots();
            snap.boots = (boot != null) ? boot.getType().name() : "AIR";

            snapshots.add(snap);
        }

        // 2. Capture active AI Bots (Practice bots and Rejoin replacement bots)
        for (com.bedwars.arena.bot.RejoinBot bot : arena.getActiveBots().values()) {
            if (bot == null || !bot.isActive() || bot.getEntity() == null || !bot.getEntity().isValid()) continue;

            UUID botUuid = bot.getPlayerUuid();
            String uuidStr = botUuid.toString();
            String botName = bot.getPlayerName();
            Team team = bot.getTeam();
            String teamColor = (team != null) ? team.getColor().name() : "WHITE";

            if (!data.getPlayerNames().containsKey(uuidStr)) {
                data.getPlayerNames().put(uuidStr, botName);
                data.getPlayerTeams().put(uuidStr, teamColor);
                String[] skin = bot.getSkinTexture();
                if (skin != null && skin.length > 0 && skin[0] != null) {
                    data.getPlayerSkins().put(uuidStr, skin);
                }
            }

            ReplayData.PlayerSnapshot snap = new ReplayData.PlayerSnapshot();
            snap.uuid = uuidStr;
            snap.name = botName;
            snap.teamColor = teamColor;

            Location loc = bot.getEntity().getLocation();
            snap.x = Math.round(loc.getX() * 1000.0) / 1000.0;
            snap.y = Math.round(loc.getY() * 1000.0) / 1000.0;
            snap.z = Math.round(loc.getZ() * 1000.0) / 1000.0;
            snap.yaw = Math.round(loc.getYaw() * 10.0f) / 10.0f;
            snap.pitch = Math.round(loc.getPitch() * 10.0f) / 10.0f;
            snap.sneaking = bot.isSneaking();
            snap.sprinting = !bot.isSneaking();
            snap.swinging = recentSwings.remove(botUuid);
            snap.health = Math.round(bot.getEntity().getHealth() * 10.0) / 10.0;

            if (bot.getEntity().getEquipment() != null) {
                ItemStack mainHand = bot.getEntity().getEquipment().getItemInMainHand();
                snap.heldItem = (mainHand != null && mainHand.getType() != Material.AIR) ? mainHand.getType().name() : "AIR";

                ItemStack offHand = bot.getEntity().getEquipment().getItemInOffHand();
                snap.offHandItem = (offHand != null && offHand.getType() != Material.AIR) ? offHand.getType().name() : "AIR";

                ItemStack helm = bot.getEntity().getEquipment().getHelmet();
                snap.helmet = (helm != null) ? helm.getType().name() : "AIR";

                ItemStack chest = bot.getEntity().getEquipment().getChestplate();
                snap.chestplate = (chest != null) ? chest.getType().name() : "AIR";

                ItemStack leg = bot.getEntity().getEquipment().getLeggings();
                snap.leggings = (leg != null) ? leg.getType().name() : "AIR";

                ItemStack boot = bot.getEntity().getEquipment().getBoots();
                snap.boots = (boot != null) ? boot.getType().name() : "AIR";
            }

            snapshots.add(snap);
        }

        data.getTimeline().put(frame, snapshots);
    }

    public void recordTick(int second) {
        // Fallback for 1s ticks if needed
    }

    public void recordBlockChange(int second, Block block, boolean isPlaced) {
        if (!recording || block == null) return;
        recordBlockChange(second, block.getLocation(), block.getBlockData().getAsString(), isPlaced);
    }

    public void recordBlockChange(int second, Location loc, String blockData, boolean isPlaced) {
        if (!recording || loc == null || loc.getWorld() == null) return;

        ReplayData.BlockChange change = new ReplayData.BlockChange();
        change.second = second;
        change.x = loc.getBlockX();
        change.y = loc.getBlockY();
        change.z = loc.getBlockZ();
        change.world = loc.getWorld().getName();
        change.blockData = blockData != null ? blockData : "minecraft:air";
        change.material = (blockData != null && blockData.contains("[")) ? blockData.substring(0, blockData.indexOf("[")) : (blockData != null ? blockData : "AIR");
        change.isPlaced = isPlaced;

        data.getBlockChanges().add(change);
    }

    public void recordEvent(int second, String type, String desc) {
        recordEvent(second, type, desc, "WHITE");
    }

    public void recordEvent(int second, String type, String desc, String teamColor) {
        if (!recording) return;

        ReplayData.ReplayEvent ev = new ReplayData.ReplayEvent();
        ev.second = second;
        ev.type = type;
        ev.description = desc;
        ev.teamColor = teamColor;

        data.getEvents().add(ev);
    }

    public ReplayData finish(Team winner, int durationSeconds) {
        return finish((winner != null) ? winner.getColor().name() : "NONE", durationSeconds);
    }

    public ReplayData finish(String winnerName, int durationSeconds) {
        this.recording = false;
        if (sampleTask != null) {
            sampleTask.cancel();
            sampleTask = null;
        }
        HandlerList.unregisterAll(this);

        data.setDurationSeconds(durationSeconds);
        data.setWinnerTeam(winnerName != null ? winnerName : "NONE");

        // Copy stats
        for (UUID uuid : arena.getPlayers()) {
            data.getPlayerKills().put(uuid.toString(), arena.getKills(uuid));
            data.getPlayerFinalKills().put(uuid.toString(), arena.getFinalKills(uuid));
            data.getPlayerBedsBroken().put(uuid.toString(), arena.getBedsBroken(uuid));
        }

        return data;
    }

    public boolean isRecording() {
        return recording;
    }

    public static String[] extractPlayerSkin(Player player) {
        if (player == null) return null;
        try {
            // Paper / CraftPlayer getProfile reflection
            java.lang.reflect.Method getProfileMethod = player.getClass().getMethod("getProfile");
            Object gameProfile = getProfileMethod.invoke(player);
            java.lang.reflect.Method getPropertiesMethod = gameProfile.getClass().getMethod("getProperties");
            Object propertyMap = getPropertiesMethod.invoke(gameProfile);
            java.lang.reflect.Method getMethod = propertyMap.getClass().getMethod("get", Object.class);
            Collection<?> properties = (Collection<?>) getMethod.invoke(propertyMap, "textures");
            if (properties != null && !properties.isEmpty()) {
                Object prop = properties.iterator().next();
                java.lang.reflect.Method getValue = prop.getClass().getMethod("getValue");
                java.lang.reflect.Method getSig = prop.getClass().getMethod("getSignature");
                String val = (String) getValue.invoke(prop);
                String sig = (String) getSig.invoke(prop);
                if (val != null && !val.isEmpty()) {
                    return new String[]{val, sig != null ? sig : ""};
                }
            }
        } catch (Throwable ignored) {}

        try {
            // Paper playerProfile reflection fallback
            java.lang.reflect.Method getPlayerProfile = player.getClass().getMethod("getPlayerProfile");
            Object playerProfile = getPlayerProfile.invoke(player);
            java.lang.reflect.Method getProperties = playerProfile.getClass().getMethod("getProperties");
            Set<?> props = (Set<?>) getProperties.invoke(playerProfile);
            for (Object prop : props) {
                java.lang.reflect.Method getName = prop.getClass().getMethod("getName");
                if ("textures".equals(getName.invoke(prop))) {
                    java.lang.reflect.Method getValue = prop.getClass().getMethod("getValue");
                    java.lang.reflect.Method getSig = prop.getClass().getMethod("getSignature");
                    String val = (String) getValue.invoke(prop);
                    String sig = (String) getSig.invoke(prop);
                    if (val != null && !val.isEmpty()) {
                        return new String[]{val, sig != null ? sig : ""};
                    }
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }
}
