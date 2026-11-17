package com.combatplus.service;

import com.combatplus.CombatPlus;
import com.combatplus.config.ConfigManager;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import net.kyori.adventure.key.Key;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SwordService {

    private final CombatPlus plugin;
    private final ConfigManager configManager;

    public SwordService(CombatPlus plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean isSword(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        return isSwordMaterial(item.getType());
    }

    public boolean isSwordMaterial(Material material) {
        if (material == null) return false;
        String name = material.name();
        return name.endsWith("_SWORD");
    }

    public boolean isBlockingSword(ItemStack item) {
        if (!isSword(item)) {
            return false;
        }
        if (!item.hasData(DataComponentTypes.CONSUMABLE)) {
            return false;
        }
        Consumable consumable = item.getData(DataComponentTypes.CONSUMABLE);
        return consumable != null && consumable.animation() == ItemUseAnimation.BLOCK;
    }

    public boolean applySwordBlocking(ItemStack item) {
        if (!isSword(item)) {
            return false;
        }

        // Already has block animation
        if (isBlockingSword(item)) {
            return false;
        }

        try {
            float factor = (float) Math.max(0.0, 1.0 - (configManager.getDamageReductionPercent() / 100.0));
            float angle = (float) configManager.getHorizontalBlockingAngle();
            float delay = (float) configManager.getBlockDelaySeconds();
            String soundStr = configManager.getBlockSound();
            Key soundKey = Key.key(soundStr.contains(":") ? soundStr : "minecraft:" + soundStr);

            DamageReduction reduction = DamageReduction.damageReduction()
                    .factor(factor)
                    .horizontalBlockingAngle(angle)
                    .build();

            BlocksAttacks blocksAttacks = BlocksAttacks.blocksAttacks()
                    .blockDelaySeconds(delay)
                    .disableCooldownScale(1.0f)
                    .blockSound(soundKey)
                    .addDamageReduction(reduction)
                    .build();

            Consumable consumable = Consumable.consumable()
                    .consumeSeconds(72000.0f)
                    .animation(ItemUseAnimation.BLOCK)
                    .hasConsumeParticles(false)
                    .build();

            item.setData(DataComponentTypes.CONSUMABLE, consumable);
            item.setData(DataComponentTypes.BLOCKS_ATTACKS, blocksAttacks);
            return true;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to apply sword blocking components: " + e.getMessage());
            return false;
        }
    }

    public void removeSwordBlocking(ItemStack item) {
        if (isSword(item) && isBlockingSword(item)) {
            item.resetData(DataComponentTypes.CONSUMABLE);
            item.resetData(DataComponentTypes.BLOCKS_ATTACKS);
        }
    }

    public ItemStack createBlockingSword(Material material) {
        if (!isSwordMaterial(material)) {
            material = Material.DIAMOND_SWORD;
        }
        ItemStack item = new ItemStack(material);
        applySwordBlocking(item);
        return item;
    }

    public boolean updatePlayerSwords(Player player) {
        if (player == null || !player.isOnline()) return false;
        boolean updated = false;
        for (ItemStack item : player.getInventory().getContents()) {
            if (isSword(item) && !isBlockingSword(item)) {
                if (applySwordBlocking(item)) {
                    updated = true;
                }
            }
        }
        if (updated) {
            player.updateInventory();
        }
        return updated;
    }
}
