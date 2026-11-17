package com.combatplus;

import com.combatplus.command.CombatPlusCommand;
import com.combatplus.config.ConfigManager;
import com.combatplus.listener.SwordBlockListener;
import com.combatplus.service.SwordService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CombatPlus extends JavaPlugin {

    private ConfigManager configManager;
    private SwordService swordService;
    private final Set<UUID> disabledPlayers = new HashSet<>();

    @Override
    public void onEnable() {
        // Load configuration
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        // Initialize service
        this.swordService = new SwordService(this, configManager);

        // Register listeners
        getServer().getPluginManager().registerEvents(
                new SwordBlockListener(this, configManager, swordService),
                this
        );

        // Register command using Paper's native modern command registration
        CombatPlusCommand cmdHandler = new CombatPlusCommand(this, configManager, swordService);
        registerCommand(
                "combatplus",
                "Main CombatPlus command",
                Arrays.asList("cp", "combat", "blocksword"),
                cmdHandler
        );

        // Apply to any online players immediately
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isPlayerBlockingEnabled(player)) {
                swordService.updatePlayerSwords(player);
            }
        }

        // Repeating inventory sync task: every 5 ticks (4 times per second)
        // Automatically converts any sword spawned into the player's hands or hotbar
        // (Creative tabs, /give commands, loot, chests, etc.)
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            if (!configManager.isEnabled()) return;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!isPlayerBlockingEnabled(player)) continue;
                if (configManager.isWorldDisabled(player.getWorld().getName())) continue;

                boolean updated = false;

                ItemStack main = player.getInventory().getItemInMainHand();
                if (swordService.isSword(main) && !swordService.isBlockingSword(main)) {
                    if (swordService.applySwordBlocking(main)) {
                        updated = true;
                    }
                }

                ItemStack off = player.getInventory().getItemInOffHand();
                if (swordService.isSword(off) && !swordService.isBlockingSword(off)) {
                    if (swordService.applySwordBlocking(off)) {
                        updated = true;
                    }
                }

                for (int i = 0; i < 9; i++) {
                    ItemStack item = player.getInventory().getItem(i);
                    if (swordService.isSword(item) && !swordService.isBlockingSword(item)) {
                        if (swordService.applySwordBlocking(item)) {
                            updated = true;
                        }
                    }
                }

                if (updated) {
                    player.updateInventory();
                }
            }
        }, 5L, 5L);

        getLogger().info("=========================================");
        getLogger().info(" CombatPlus v1.0.0 Enabled successfully!");
        getLogger().info(" Made By FawayMC");
        getLogger().info(" 1.8.9 Sword Blocking active for MC 26.3");
        getLogger().info("=========================================");
    }

    @Override
    public void onDisable() {
        getLogger().info("CombatPlus has been disabled.");
    }

    public boolean isPlayerBlockingEnabled(Player player) {
        if (player == null) return false;
        return !disabledPlayers.contains(player.getUniqueId());
    }

    public void setPlayerBlockingEnabled(Player player, boolean enabled) {
        if (player == null) return;
        if (enabled) {
            disabledPlayers.remove(player.getUniqueId());
        } else {
            disabledPlayers.add(player.getUniqueId());
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SwordService getSwordService() {
        return swordService;
    }
}
