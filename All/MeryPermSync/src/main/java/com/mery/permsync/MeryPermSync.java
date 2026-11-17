package com.mery.permsync;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.regex.Pattern;

public class MeryPermSync extends JavaPlugin implements CommandExecutor, PluginMessageListener {

    private static final String CHANNEL = "mery:permsync";
    private String masterPathStr = "/var/www/krachabmitr.nahost.in.th/storage/app/minecraft-servers/fawaymc-4AATL/plugins/LuckPerms/yaml-storage";
    private String fallbackPathStr = "../fawaymc-4AATL/plugins/LuckPerms/yaml-storage";
    private boolean autoConfigureYaml = true;
    private boolean autoLink = true;

    @Override
    public void onLoad() {
        getLogger().info("[MeryPermSync] Checking LuckPerms configuration and link...");
        loadPluginConfig();

        if (autoConfigureYaml) {
            fixLuckPermsConfig();
        }

        if (autoLink) {
            setupSymbolicLink();
        }
    }

    @Override
    public void onEnable() {
        // Register channel
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);

        if (getCommand("permsync") != null) {
            getCommand("permsync").setExecutor(this);
        }

        getLogger().info("[MeryPermSync] Enabled successfully! Running in sync with Bungee master.");
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this);
    }

    private void loadPluginConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getDataFolder().mkdirs();
            try (InputStream in = getResource("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                getLogger().warning("Could not create default config.yml: " + e.getMessage());
            }
        }

        if (configFile.exists()) {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            masterPathStr = config.getString("master-storage-path", masterPathStr);
            fallbackPathStr = config.getString("fallback-relative-path", fallbackPathStr);
            autoConfigureYaml = config.getBoolean("auto-configure-yaml", true);
            autoLink = config.getBoolean("auto-link", true);
        }
    }

    private void fixLuckPermsConfig() {
        File lpDir = new File("plugins/LuckPerms");
        if (!lpDir.exists()) {
            lpDir.mkdirs();
        }

        File lpConfig = new File(lpDir, "config.yml");
        if (lpConfig.exists()) {
            try {
                String content = Files.readString(lpConfig.toPath(), StandardCharsets.UTF_8);
                Pattern pattern = Pattern.compile("(?m)^(\\s*storage-method:\\s*)(?!yaml\\b).*$");
                if (pattern.matcher(content).find()) {
                    String updated = pattern.matcher(content).replaceAll("$1yaml");
                    Files.writeString(lpConfig.toPath(), updated, StandardCharsets.UTF_8);
                    getLogger().info("[MeryPermSync] Automatically updated LuckPerms config.yml to storage-method: yaml");
                }
            } catch (Exception e) {
                getLogger().warning("[MeryPermSync] Failed to inspect/update LuckPerms config: " + e.getMessage());
            }
        }
    }

    private void setupSymbolicLink() {
        Path masterPath = Paths.get(masterPathStr);
        if (!Files.exists(masterPath)) {
            Path fallback = Paths.get(fallbackPathStr);
            if (Files.exists(fallback)) {
                masterPath = fallback;
            }
        }

        if (!Files.exists(masterPath)) {
            getLogger().warning("[MeryPermSync] Master yaml-storage path not found: " + masterPath.toAbsolutePath());
            getLogger().warning("[MeryPermSync] Please ensure the master Bungee LuckPerms has run at least once!");
            return;
        }

        Path localTarget = Paths.get("plugins/LuckPerms/yaml-storage");

        try {
            if (Files.isSymbolicLink(localTarget)) {
                Path existingTarget = Files.readSymbolicLink(localTarget);
                if (existingTarget.equals(masterPath) || existingTarget.equals(masterPath.toAbsolutePath())) {
                    getLogger().info("[MeryPermSync] Symbolic link already active: " + localTarget + " -> " + masterPath);
                    return;
                } else {
                    Files.delete(localTarget);
                }
            } else if (Files.exists(localTarget)) {
                // Delete existing normal directory if empty or replace with link
                deleteDirectory(localTarget);
            }

            // Create parent if missing
            if (!Files.exists(localTarget.getParent())) {
                Files.createDirectories(localTarget.getParent());
            }

            Files.createSymbolicLink(localTarget.toAbsolutePath(), masterPath.toAbsolutePath());
            getLogger().info("[MeryPermSync] Successfully created symbolic link: " + localTarget.toAbsolutePath() + " -> " + masterPath.toAbsolutePath());
        } catch (Exception e) {
            getLogger().severe("[MeryPermSync] Error while creating symbolic link: " + e.getMessage());
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("merypermsync.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to run this command.");
            return true;
        }

        setupSymbolicLink();
        fixLuckPermsConfig();

        sender.sendMessage(ChatColor.YELLOW + "[MeryPermSync] Reloading LuckPerms nodes from shared storage...");
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp reloadnodes");
        sender.sendMessage(ChatColor.GREEN + "[MeryPermSync] Successfully synchronized permissions with Bungee!");
        return true;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) return;

        String msg = new String(message, StandardCharsets.UTF_8);
        if ("reload".equalsIgnoreCase(msg)) {
            getLogger().info("[MeryPermSync] Received sync signal from BungeeCord. Reloading LuckPerms...");
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp reloadnodes");
        }
    }
}
