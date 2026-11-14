package org.kcm.bloomery.fawaymc.global;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DoubleJumpManager implements Listener, CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private final PlayerSettingsManager settingsManager;
    private FileConfiguration jumpConfig;
    private File jumpFile;

    private final Map<UUID, Long> lastJumpTime = new HashMap<>();
    private int groundTaskId = -1;

    public DoubleJumpManager(JavaPlugin plugin, PlayerSettingsManager settingsManager) {
        this.plugin = plugin;
        this.settingsManager = settingsManager;
        createJumpConfig();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        if (plugin.getCommand("doublejump") != null) {
            plugin.getCommand("doublejump").setExecutor(this);
            plugin.getCommand("doublejump").setTabCompleter(this);
        }

        startGroundTask();
    }

    /**
     * ★ Task: เปิด allowFlight=true เฉพาะเมื่อ
     *   1. อยู่บนพื้นจริง (block check)
     *   2. cooldown หมดแล้ว (ไม่ใช่ just-jumped)
     * ★ นี่คือการ fix spam: ระหว่าง cooldown, allowFlight จะเป็น false → client ไม่ส่ง packet
     */
    private void startGroundTask() {
        if (groundTaskId != -1) {
            Bukkit.getScheduler().cancelTask(groundTaskId);
            groundTaskId = -1;
        }
        groundTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isVanillaFlightMode(p)) continue;

                if (!shouldHaveFlight(p)) {
                    if (p.getAllowFlight()) p.setAllowFlight(false);
                    continue;
                }

                UUID uuid = p.getUniqueId();

                // ★ เปิด allowFlight เฉพาะ: บนพื้น + cooldown หมดแล้ว
                if (isOnSolidGround(p) && !isInCooldown(uuid) && !p.getAllowFlight()) {
                    p.setAllowFlight(true);
                }
                // ★ ไม่ปิด allowFlight ใน task — handler จัดการเองหลัง jump
            }
        }, 0L, 1L).getTaskId();
    }

    public void stop() {
        if (groundTaskId != -1) {
            Bukkit.getScheduler().cancelTask(groundTaskId);
            groundTaskId = -1;
        }
        lastJumpTime.clear();
    }

    /**
     * ★ Block-based ground check แม่นยำกว่า isOnGround()
     */
    private boolean isOnSolidGround(Player player) {
        if (player.getVelocity().getY() > 0.1) {
            return false;
        }

        if (player.isOnGround()) {
            return true;
        }

        Location loc = player.getLocation();
        int blockY = (int) Math.floor(loc.getY() - 0.1);
        Block b = loc.getWorld().getBlockAt(loc.getBlockX(), blockY, loc.getBlockZ());
        return b.getType().isSolid() && !b.isPassable();
    }

    public void createJumpConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        jumpFile = new File(plugin.getDataFolder(), "doublejump.yml");
        if (!jumpFile.exists()) {
            try {
                if (jumpFile.createNewFile()) {
                    jumpConfig = YamlConfiguration.loadConfiguration(jumpFile);
                    jumpConfig.set("double_jump.enabled", true);
                    jumpConfig.set("double_jump.jump_power", 1.0);
                    jumpConfig.set("double_jump.jump_height", 0.5);
                    jumpConfig.set("double_jump.cooldown_seconds", 3);
                    jumpConfig.set("double_jump.sound.enabled", true);
                    jumpConfig.set("double_jump.sound.type", "ENTITY_ENDER_DRAGON_FLAP");
                    jumpConfig.set("double_jump.sound.volume", 1.0);
                    jumpConfig.set("double_jump.sound.pitch", 1.5);
                    jumpConfig.set("double_jump.particle.enabled", true);
                    jumpConfig.set("double_jump.particle.type", "CLOUD");
                    jumpConfig.set("double_jump.particle.count", 20);
                    jumpConfig.set("messages.no_permission", "&cคุณไม่มีสิทธิ์!");
                    jumpConfig.set("messages.usage", "&e/doublejump <on|off|reload>");
                    jumpConfig.set("messages.enabled", "&aเปิด Double Jump สำเร็จ!");
                    jumpConfig.set("messages.disabled", "&cปิด Double Jump สำเร็จ!");
                    jumpConfig.set("messages.reload", "&aรีโหลด doublejump.yml สำเร็จ!");
                    jumpConfig.save(jumpFile);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("ไม่สามารถสร้างไฟล์ doublejump.yml ได้: " + e.getMessage());
            }
        }
        jumpConfig = YamlConfiguration.loadConfiguration(jumpFile);
    }

    public void reloadJumpConfig() {
        if (jumpFile == null) {
            jumpFile = new File(plugin.getDataFolder(), "doublejump.yml");
        }
        if (!jumpFile.exists()) {
            createJumpConfig();
        } else {
            jumpConfig = YamlConfiguration.loadConfiguration(jumpFile);
        }
    }

    private boolean isInSpawnWorld(Player player) {
        try {
            FileConfiguration global = plugin.getConfig();
            String worldName = global.getString("spawn.world");
            if (worldName == null) return false;
            return player.getWorld().getName().equals(worldName);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isVanillaFlightMode(Player player) {
        GameMode gm = player.getGameMode();
        return gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR;
    }

    private boolean shouldHaveFlight(Player player) {
        if (isVanillaFlightMode(player)) return false;
        if (!jumpConfig.getBoolean("double_jump.enabled", true)) return false;
        if (!settingsManager.isDoubleJumpEnabled(player)) return false;
        return isInSpawnWorld(player);
    }

    private long getCooldownMs() {
        int sec = jumpConfig.getInt("double_jump.cooldown_seconds", 3);
        if (sec < 1) sec = 1;
        return sec * 1000L;
    }

    private long getRemainingMs(UUID uuid) {
        Long last = lastJumpTime.get(uuid);
        if (last == null) return 0;
        long elapsed = System.currentTimeMillis() - last;
        return Math.max(0, getCooldownMs() - elapsed);
    }

    private boolean isInCooldown(UUID uuid) {
        return getRemainingMs(uuid) > 0;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!shouldHaveFlight(player)) return;
        if (isOnSolidGround(player) && !isInCooldown(player.getUniqueId())) {
            player.setAllowFlight(true);
        }
        player.setFlying(false);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastJumpTime.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        GameMode newMode = event.getNewGameMode();

        if (newMode == GameMode.CREATIVE || newMode == GameMode.SPECTATOR) return;

        if (shouldHaveFlight(player) && isOnSolidGround(player) && !isInCooldown(player.getUniqueId())) {
            player.setAllowFlight(true);
        } else {
            player.setAllowFlight(false);
        }
        player.setFlying(false);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        lastJumpTime.remove(player.getUniqueId());

        if (isVanillaFlightMode(player)) return;

        player.setFlying(false);
        if (shouldHaveFlight(player) && isOnSolidGround(player)) {
            player.setAllowFlight(true);
        } else {
            player.setAllowFlight(false);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (isVanillaFlightMode(player)) return;

        // ห้ามบินจริง
        if (player.isFlying()) player.setFlying(false);
    }

    @EventHandler
    public void onPlayerToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        if (isVanillaFlightMode(player)) return;

        event.setCancelled(true);
        player.setFlying(false);

        // ★ Client ที่โกงส่ง packet แม้ allowFlight=false → block ทันที
        if (!player.getAllowFlight()) return;

        if (!shouldHaveFlight(player)) {
            player.setAllowFlight(false);
            return;
        }

        UUID uuid = player.getUniqueId();

        if (isOnSolidGround(player)) {
            return;
        }

        if (isInCooldown(uuid)) {
            player.setAllowFlight(false);
            // Cap velocity กัน client-side prediction stacking
            Vector v = player.getVelocity();
            if (v.getY() > 0.42) {
                player.setVelocity(new Vector(v.getX(), 0.42, v.getZ()));
            }
            return;
        }

        // ★★★ Legit double jump ★★★
        player.setAllowFlight(false);
        lastJumpTime.put(uuid, System.currentTimeMillis());

        double jumpPower = jumpConfig.getDouble("double_jump.jump_power", 1.0);
        double jumpHeight = jumpConfig.getDouble("double_jump.jump_height", 0.5);

        Vector dir = player.getLocation().getDirection();
        Vector horizontal = new Vector(dir.getX(), 0, dir.getZ());

        if (horizontal.lengthSquared() < 0.01) {
            float yaw = player.getLocation().getYaw();
            double yawRad = Math.toRadians(yaw);
            horizontal = new Vector(-Math.sin(yawRad), 0, Math.cos(yawRad));
        } else {
            horizontal.normalize();
        }

        final Vector finalVelocity = horizontal.multiply(jumpPower).setY(jumpHeight);
        player.setVelocity(finalVelocity);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isOnGround()) {
                player.setFlying(false);
                player.setVelocity(finalVelocity);
            }
        }, 1L);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && !player.isOnGround()) {
                player.setFlying(false);
                player.setVelocity(finalVelocity);
            }
        }, 2L);

        if (jumpConfig.getBoolean("double_jump.sound.enabled", true)) {
            String soundName = jumpConfig.getString("double_jump.sound.type", "ENTITY_ENDER_DRAGON_FLAP");
            float volume = (float) jumpConfig.getDouble("double_jump.sound.volume", 1.0);
            float pitch = (float) jumpConfig.getDouble("double_jump.sound.pitch", 1.5);
            Sound sound = resolveSound(soundName);
            if (sound != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }

        if (jumpConfig.getBoolean("double_jump.particle.enabled", true)) {
            String particleName = jumpConfig.getString("double_jump.particle.type", "CLOUD");
            int count = jumpConfig.getInt("double_jump.particle.count", 20);
            Particle particle = resolveParticle(particleName);
            if (particle != null) {
                Location loc = player.getLocation();
                player.getWorld().spawnParticle(particle, loc, count, 0.5, 0.1, 0.5, 0.05);
            }
        }
    }

    private Sound resolveSound(String rawName) {
        if (rawName == null || rawName.isBlank()) return Sound.ENTITY_ENDER_DRAGON_FLAP;
        String normalized = rawName.trim().toLowerCase().replace('_', '.');
        NamespacedKey key = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : NamespacedKey.minecraft(normalized);
        if (key != null) {
            Sound sound = Registry.SOUNDS.get(key);
            if (sound != null) return sound;
        }
        return Sound.ENTITY_ENDER_DRAGON_FLAP;
    }

    private Particle resolveParticle(String rawName) {
        if (rawName == null || rawName.isBlank()) return Particle.CLOUD;
        String normalized = rawName.trim().toLowerCase().replace('_', '.');
        NamespacedKey key = normalized.contains(":")
                ? NamespacedKey.fromString(normalized)
                : NamespacedKey.minecraft(normalized);
        if (key != null) {
            Particle p = Registry.PARTICLE_TYPE.get(key);
            if (p != null) return p;
        }
        return Particle.CLOUD;
    }

    private String getMsg(String path, String defaultMsg) {
        String msg = jumpConfig.getString("messages." + path, defaultMsg);
        return PlaceholderManager.setPlaceholders(null, msg);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("global.admin")) {
            sender.sendMessage(getMsg("no_permission", "&cคุณไม่มีสิทธิ์!"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(getMsg("usage", "&e/doublejump <on|off|reload>"));
            return true;
        }

        String sub = args[0].toLowerCase();

        if (sub.equals("on")) {
            jumpConfig.set("double_jump.enabled", true);
            saveJumpConfig();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (shouldHaveFlight(p) && isOnSolidGround(p) && !isInCooldown(p.getUniqueId())) {
                    p.setAllowFlight(true);
                    p.setFlying(false);
                }
            }
            sender.sendMessage(getMsg("enabled", "&aเปิด Double Jump สำเร็จ!"));
            return true;
        }

        if (sub.equals("off")) {
            jumpConfig.set("double_jump.enabled", false);
            saveJumpConfig();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isVanillaFlightMode(p)) continue;
                p.setAllowFlight(false);
                p.setFlying(false);
            }
            lastJumpTime.clear();
            sender.sendMessage(getMsg("disabled", "&cปิด Double Jump สำเร็จ!"));
            return true;
        }

        if (sub.equals("reload")) {
            reloadJumpConfig();
            sender.sendMessage(getMsg("reload", "&aรีโหลด doublejump.yml สำเร็จ!"));
            return true;
        }

        sender.sendMessage(getMsg("usage", "&e/doublejump <on|off|reload>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender.hasPermission("global.admin") && args.length == 1) {
            String input = args[0].toLowerCase();
            if ("on".startsWith(input)) completions.add("on");
            if ("off".startsWith(input)) completions.add("off");
            if ("reload".startsWith(input)) completions.add("reload");
        }
        return completions;
    }

    private void saveJumpConfig() {
        try {
            jumpConfig.save(jumpFile);
        } catch (IOException e) {
            plugin.getLogger().severe("ไม่สามารถบันทึกไฟล์ doublejump.yml ได้!");
        }
    }
}