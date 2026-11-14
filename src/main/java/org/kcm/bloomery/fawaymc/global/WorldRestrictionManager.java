package org.kcm.bloomery.fawaymc.global;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WorldRestrictionManager implements Listener, CommandExecutor, TabCompleter {

    private final JavaPlugin plugin;
    private FileConfiguration restrictionConfig;
    private File restrictionFile;

    public WorldRestrictionManager(JavaPlugin plugin) {
        this.plugin = plugin;
        createRestrictionConfig();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        if (plugin.getCommand("worldrestriction") != null) {
            plugin.getCommand("worldrestriction").setExecutor(this);
            plugin.getCommand("worldrestriction").setTabCompleter(this);
        }
    }

    public void createRestrictionConfig() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        restrictionFile = new File(plugin.getDataFolder(), "worldrestriction.yml");
        if (!restrictionFile.exists()) {
            try {
                if (restrictionFile.createNewFile()) {
                    restrictionConfig = YamlConfiguration.loadConfiguration(restrictionFile);

                    restrictionConfig.set("restrictions.disable_nether", true);
                    restrictionConfig.set("restrictions.disable_end", true);
                    restrictionConfig.set("restrictions.disable_monsters", true);
                    restrictionConfig.set("restrictions.disable_all_mobs", true); // เพิ่มตัวเลือกปิดม็อบทั้งหมด

                    List<String> allowedWorlds = new ArrayList<>();
                    allowedWorlds.add("world_survival");
                    restrictionConfig.set("restrictions.allowed_spawn_worlds", allowedWorlds);

                    restrictionConfig.set("messages.nether_disabled", "&cระบบมิติ Nether ถูกปิดใช้งานในขณะนี้!");
                    restrictionConfig.set("messages.end_disabled", "&cระบบมิติ The End ถูกปิดใช้งานในขณะนี้!");
                    restrictionConfig.set("messages.no_permission", "&cคุณไม่มีสิทธิ์ใช้งานคำสั่งนี้!");
                    restrictionConfig.set("messages.reload", "&aรีโหลดไฟล์ worldrestriction.yml เรียบร้อยแล้ว!");
                    restrictionConfig.set("messages.usage", "&eการใช้งาน: /worldrestriction reload");

                    restrictionConfig.save(restrictionFile);
                }
            } catch (IOException e) {
                plugin.getLogger().severe("ไม่สามารถสร้างไฟล์ worldrestriction.yml ได้: " + e.getMessage());
            }
        }
        restrictionConfig = YamlConfiguration.loadConfiguration(restrictionFile);
    }

    public void reloadRestrictionConfig() {
        if (restrictionFile == null) {
            restrictionFile = new File(plugin.getDataFolder(), "worldrestriction.yml");
        }
        if (!restrictionFile.exists()) {
            createRestrictionConfig();
        } else {
            restrictionConfig = YamlConfiguration.loadConfiguration(restrictionFile);
        }
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Player player = event.getPlayer();
        World.Environment targetEnv = event.getTo().getWorld().getEnvironment();

        if (targetEnv == World.Environment.NETHER && restrictionConfig.getBoolean("restrictions.disable_nether", true)) {
            event.setCancelled(true);
            String msg = restrictionConfig.getString("messages.nether_disabled", "&cระบบมิติ Nether ถูกปิดใช้งานในขณะนี้!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        } else if (targetEnv == World.Environment.THE_END && restrictionConfig.getBoolean("restrictions.disable_end", true)) {
            event.setCancelled(true);
            String msg = restrictionConfig.getString("messages.end_disabled", "&cระบบมิติ The End ถูกปิดใช้งานในขณะนี้!");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            if (event.getTo() != null && event.getTo().getWorld() != null) {
                World.Environment targetEnv = event.getTo().getWorld().getEnvironment();
                if (targetEnv == World.Environment.NETHER && restrictionConfig.getBoolean("restrictions.disable_nether", true)) {
                    event.setCancelled(true);
                    String msg = restrictionConfig.getString("messages.nether_disabled", "&cระบบมิติ Nether ถูกปิดใช้งานในขณะนี้!");
                    event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                } else if (targetEnv == World.Environment.THE_END && restrictionConfig.getBoolean("restrictions.disable_end", true)) {
                    event.setCancelled(true);
                    String msg = restrictionConfig.getString("messages.end_disabled", "&cระบบมิติ The End ถูกปิดใช้งานในขณะนี้!");
                    event.getPlayer().sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
                }
            }
        }
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        String worldName = event.getLocation().getWorld().getName();
        List<String> allowedWorlds = restrictionConfig.getStringList("restrictions.allowed_spawn_worlds");

        // ถ้าโลกนี้อยู่ในรายชื่อที่อนุญาตให้เกิดได้ตามปกติ ให้ข้ามไป
        if (allowedWorlds.contains(worldName)) {
            return;
        }

        boolean disableMonsters = restrictionConfig.getBoolean("restrictions.disable_monsters", true);
        boolean disableAllMobs = restrictionConfig.getBoolean("restrictions.disable_all_mobs", true);

        // หากปิดการเกิดม็อบทั้งหมด (รวมสัตว์และมอนสเตอร์)
        if (disableAllMobs) {
            event.setCancelled(true);
            return;
        }

        // หากปิดเฉพาะมอนสเตอร์
        if (disableMonsters && event.getEntity() instanceof Monster) {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("global.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', restrictionConfig.getString("messages.no_permission", "&cคุณไม่มีสิทธิ์ใช้งานคำสั่งนี้!")));
            return true;
        }

        if (args.length == 0 || !args[0].equalsIgnoreCase("reload")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', restrictionConfig.getString("messages.usage", "&eการใช้งาน: /worldrestriction reload")));
            return true;
        }

        reloadRestrictionConfig();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', restrictionConfig.getString("messages.reload", "&aรีโหลดไฟล์ worldrestriction.yml เรียบร้อยแล้ว!")));
        if (sender instanceof Player) {
            ((Player) sender).playSound(((Player) sender).getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender.hasPermission("global.admin") && args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                completions.add("reload");
            }
        }
        return completions;
    }
}