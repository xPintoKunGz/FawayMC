package com.merycraft.meryitem.listener;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemStat;
import com.merycraft.meryitem.util.AttributeHelper;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/**
 * Updates player attributes and applies held/equipped potion effects (Max Level
 * 250, Duration 9999s).
 */
public class EquipmentListener implements Listener {

    private final MeryItemPlugin plugin;
    // Track active potion effects applied to player UUID so we can clean them up
    // when items are unequipped
    private final Map<UUID, Set<PotionEffectType>> appliedPotionEffects = new HashMap<>();

    public EquipmentListener(MeryItemPlugin plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        // Run update task every 10 ticks (0.5s) to guarantee continuous active effects
        // while held
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updatePlayerStats(player);
            }
        }, 20L, 10L);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerStats(event.getPlayer()), 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        AttributeHelper.resetAttributes(event.getPlayer());
        appliedPotionEffects.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onHeld(PlayerItemHeldEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerStats(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onSwapHand(PlayerSwapHandItemsEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerStats(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerStats(event.getPlayer()), 1L);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayerStats(p), 1L);
        }
    }

    public void updatePlayerStats(Player player) {
        if (!player.isOnline())
            return;

        Map<ItemStat, Double> stats = plugin.getItemManager().getPlayerTotalStats(player);
        double bonusHp = stats.getOrDefault(ItemStat.MAX_HEALTH, 0.0);
        double bonusSpeed = stats.getOrDefault(ItemStat.MOVEMENT_SPEED, 0.0);

        AttributeHelper.setBonusHealth(player, bonusHp);
        AttributeHelper.setBonusSpeed(player, bonusSpeed);

        // Collect all desired potion effects from main hand and armor
        Map<PotionEffectType, Integer> desiredEffects = new HashMap<>();
        collectItemPotionEffects(player.getInventory().getItemInMainHand(), desiredEffects);
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            collectItemPotionEffects(armor, desiredEffects);
        }

        UUID uuid = player.getUniqueId();
        Set<PotionEffectType> previouslyApplied = appliedPotionEffects.computeIfAbsent(uuid, k -> new HashSet<>());

        // Remove potion effects that are no longer equipped/held
        Iterator<PotionEffectType> it = previouslyApplied.iterator();
        while (it.hasNext()) {
            PotionEffectType oldType = it.next();
            if (!desiredEffects.containsKey(oldType)) {
                player.removePotionEffect(oldType);
                it.remove();
            }
        }

        // Apply desired effects with 9999 seconds (199,980 ticks) and level up to 250
        for (Map.Entry<PotionEffectType, Integer> entry : desiredEffects.entrySet()) {
            PotionEffectType type = entry.getKey();
            int level = entry.getValue();
            if (type != null && level > 0) {
                int amp = Math.min(250, Math.max(0, level - 1));
                // 9999 seconds = 199980 ticks. ambient=true, particles=true, icon=true
                PotionEffect current = player.getPotionEffect(type);
                if (current == null || current.getAmplifier() != amp || current.getDuration() < 1000) {
                    player.addPotionEffect(new PotionEffect(type, 199980, amp, true, true, true));
                }
                previouslyApplied.add(type);
            }
        }
    }

    private void collectItemPotionEffects(ItemStack stack, Map<PotionEffectType, Integer> destination) {
        if (stack == null || !stack.hasItemMeta())
            return;

        ItemMeta meta = stack.getItemMeta();
        if (meta == null)
            return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        // Check trigger mode: Only apply automatically if trigger is "HOLD"
        String trigger = "HOLD";
        NamespacedKey potTriggerKey = new NamespacedKey(plugin, "mery_pot_trigger");
        if (pdc.has(potTriggerKey, PersistentDataType.STRING)) {
            trigger = pdc.get(potTriggerKey, PersistentDataType.STRING);
        }
        if (trigger != null && !trigger.equalsIgnoreCase("HOLD")) {
            return; // Configured for Click (Right/Left), do not grant continuously on hold!
        }
        for (NamespacedKey key : pdc.getKeys()) {
            if (key.getNamespace().equals(plugin.getName().toLowerCase()) && key.getKey().startsWith("merypot_")) {
                String effName = key.getKey().substring("merypot_".length());
                PotionEffectType type = PotionEffectType.getByName(effName.toUpperCase());
                if (type == null) {
                    type = PotionEffectType.getByKey(NamespacedKey.minecraft(effName.toLowerCase()));
                }
                if (type != null) {
                    Integer lvl = pdc.get(key, PersistentDataType.INTEGER);
                    if (lvl != null && lvl > 0) {
                        destination.merge(type, Math.min(250, lvl), Math::max);
                    }
                }
            }
        }

        // 2. Also check CustomItem from ItemManager if registered
        CustomItem cItem = plugin.getItemManager().getFromItemStack(stack);
        if (cItem != null) {
            for (Map.Entry<PotionEffectType, Integer> entry : cItem.getPotionEffects().entrySet()) {
                if (entry.getKey() != null && entry.getValue() > 0) {
                    destination.merge(entry.getKey(), Math.min(250, entry.getValue()), Math::max);
                }
            }
        }
    }
}
