package com.merycraft.meryitem.upgrade;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.ItemStat;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Manages item upgrade recipes and smithing enhancements.
 */
public class UpgradeManager {

    private final MeryItemPlugin plugin;
    private final Map<String, UpgradeRecipe> recipes = new LinkedHashMap<>();

    public UpgradeManager(MeryItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadRecipes() {
        recipes.clear();
        File file = new File(plugin.getDataFolder(), "upgrades.yml");
        if (!file.exists()) {
            createDefaultUpgrades(file);
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(key);
            if (sec == null) continue;

            String mat = sec.getString("material", "DIAMOND");
            int amount = sec.getInt("amount", 1);
            String statName = sec.getString("stat", "ATTACK_DAMAGE");
            ItemStat stat = ItemStat.fromString(statName);
            double bonus = sec.getDouble("bonus", 5.0);
            String cmd = sec.getString("attach-command", null);
            String desc = sec.getString("description", "Upgrade item power");

            UpgradeRecipe recipe = new UpgradeRecipe(key, mat, amount, stat, bonus, cmd, desc);
            recipes.put(key.toUpperCase(), recipe);
        }
    }

    private void createDefaultUpgrades(File file) {
        FileConfiguration config = new YamlConfiguration();
        
        // 1. Diamond Sharpening (Upgrade attack damage)
        config.set("DIAMOND_SHARPEN.material", "DIAMOND");
        config.set("DIAMOND_SHARPEN.amount", 4);
        config.set("DIAMOND_SHARPEN.stat", "ATTACK_DAMAGE");
        config.set("DIAMOND_SHARPEN.bonus", 10.0);
        config.set("DIAMOND_SHARPEN.description", "+10 Attack Damage (Consumes 4x Diamond)");

        // 2. Netherite Hardening (Upgrade defense)
        config.set("NETHERITE_PLATING.material", "NETHERITE_INGOT");
        config.set("NETHERITE_PLATING.amount", 1);
        config.set("NETHERITE_PLATING.stat", "DEFENSE");
        config.set("NETHERITE_PLATING.bonus", 15.0);
        config.set("NETHERITE_PLATING.description", "+15 Defense & Protection (Consumes 1x Netherite Ingot)");

        // 3. Heart of Emerald (Upgrade max health)
        config.set("EMERALD_VITALITY.material", "EMERALD");
        config.set("EMERALD_VITALITY.amount", 8);
        config.set("EMERALD_VITALITY.stat", "MAX_HEALTH");
        config.set("EMERALD_VITALITY.bonus", 4.0);
        config.set("EMERALD_VITALITY.description", "+4 Max Health (+2 Hearts) (Consumes 8x Emerald)");

        // 4. Swift Feather (Upgrade speed)
        config.set("SWIFT_FEATHER.material", "FEATHER");
        config.set("SWIFT_FEATHER.amount", 16);
        config.set("SWIFT_FEATHER.stat", "MOVEMENT_SPEED");
        config.set("SWIFT_FEATHER.bonus", 0.05);
        config.set("SWIFT_FEATHER.description", "+5% Movement Speed (Consumes 16x Feather)");

        // 5. Divine Star Smite (Attach Thunder command)
        config.set("NETHER_STAR_SMITE.material", "NETHER_STAR");
        config.set("NETHER_STAR_SMITE.amount", 1);
        config.set("NETHER_STAR_SMITE.stat", "CRITICAL_DAMAGE");
        config.set("NETHER_STAR_SMITE.bonus", 25.0);
        config.set("NETHER_STAR_SMITE.attach-command", "execute at %player% run summon lightning_bolt ~ ~ ~");
        config.set("NETHER_STAR_SMITE.description", "+25% Crit Dmg & attaches Lightning Command (Consumes 1x Nether Star)");

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save default upgrades.yml: " + e.getMessage());
        }
    }

    public void addRecipe(UpgradeRecipe recipe) {
        recipes.put(recipe.getId().toUpperCase(), recipe);
        saveToFile();
    }

    public void removeRecipe(String id) {
        recipes.remove(id.toUpperCase());
        saveToFile();
    }

    public void resetToDefaults() {
        recipes.clear();
        File file = new File(plugin.getDataFolder(), "upgrades.yml");
        if (file.exists()) {
            file.delete();
        }
        createDefaultUpgrades(file);
        loadRecipes();
    }

    private void saveToFile() {
        File file = new File(plugin.getDataFolder(), "upgrades.yml");
        FileConfiguration config = new YamlConfiguration();
        for (UpgradeRecipe r : recipes.values()) {
            config.set(r.getId() + ".material", r.getRequiredMaterialOrName());
            config.set(r.getId() + ".amount", r.getRequiredAmount());
            if (r.getTargetStat() != null) {
                config.set(r.getId() + ".stat", r.getTargetStat().name());
            }
            config.set(r.getId() + ".bonus", r.getStatBonus());
            if (r.getAttachCommand() != null) {
                config.set(r.getId() + ".attach-command", r.getAttachCommand());
            }
            config.set(r.getId() + ".description", r.getDescription());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save upgrades.yml: " + e.getMessage());
        }
    }

    public Collection<UpgradeRecipe> getAllRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public UpgradeRecipe getRecipe(String id) {
        return recipes.get(id.toUpperCase());
    }
}

