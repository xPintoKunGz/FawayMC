package com.combatplus.listener;

import com.combatplus.CombatPlus;
import com.combatplus.config.ConfigManager;
import com.combatplus.service.SwordService;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class SwordBlockListener implements Listener {

    private final CombatPlus plugin;
    private final ConfigManager configManager;
    private final SwordService swordService;

    public SwordBlockListener(CombatPlus plugin, ConfigManager configManager, SwordService swordService) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.swordService = swordService;
    }

    private boolean isPlayerAllowed(Player player) {
        if (!configManager.isEnabled()) {
            return false;
        }
        if (configManager.isWorldDisabled(player.getWorld().getName())) {
            return false;
        }
        if (configManager.isRequirePermission() && !player.hasPermission("combatplus.block")) {
            return false;
        }
        return plugin.isPlayerBlockingEnabled(player);
    }

    // Creative mode: when taking, dragging, or placing a sword from Creative inventory tabs
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreativeInventory(InventoryCreativeEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (!isPlayerAllowed(player)) return;

            ItemStack cursor = event.getCursor();
            if (swordService.isSword(cursor)) {
                swordService.applySwordBlocking(cursor);
                event.setCursor(cursor);
            }

            ItemStack current = event.getCurrentItem();
            if (swordService.isSword(current)) {
                swordService.applySwordBlocking(current);
                event.setCurrentItem(current);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!isPlayerAllowed(player)) return;

        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        if (swordService.isSword(item) && !swordService.isBlockingSword(item)) {
            if (swordService.applySwordBlocking(item)) {
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!isPlayerAllowed(player)) return;

        ItemStack main = event.getMainHandItem();
        if (swordService.isSword(main) && !swordService.isBlockingSword(main)) {
            swordService.applySwordBlocking(main);
        }

        ItemStack off = event.getOffHandItem();
        if (swordService.isSword(off) && !swordService.isBlockingSword(off)) {
            swordService.applySwordBlocking(off);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!isPlayerAllowed(player)) return;

        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            boolean updated = false;
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            if (swordService.isSword(mainHand) && !swordService.isBlockingSword(mainHand)) {
                if (swordService.applySwordBlocking(mainHand)) {
                    updated = true;
                }
            }

            ItemStack offHand = player.getInventory().getItemInOffHand();
            if (swordService.isSword(offHand) && !swordService.isBlockingSword(offHand)) {
                if (swordService.applySwordBlocking(offHand)) {
                    updated = true;
                }
            }

            if (updated) {
                player.updateInventory();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        ItemStack item = event.getItem().getItemStack();
        if (swordService.isSword(item) && !swordService.isBlockingSword(item)) {
            swordService.applySwordBlocking(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (swordService.isSword(item) && !swordService.isBlockingSword(item)) {
            swordService.applySwordBlocking(item);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getInventory().getResult() != null) {
            ItemStack result = event.getInventory().getResult();
            if (swordService.isSword(result) && !swordService.isBlockingSword(result)) {
                swordService.applySwordBlocking(result);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        ItemStack result = event.getCurrentItem();
        if (swordService.isSword(result) && !swordService.isBlockingSword(result)) {
            swordService.applySwordBlocking(result);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            if (!isPlayerAllowed(player)) return;

            ItemStack current = event.getCurrentItem();
            if (swordService.isSword(current) && !swordService.isBlockingSword(current)) {
                swordService.applySwordBlocking(current);
            }

            ItemStack cursor = event.getCursor();
            if (swordService.isSword(cursor) && !swordService.isBlockingSword(cursor)) {
                swordService.applySwordBlocking(cursor);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (!isPlayerAllowed(player)) return;
            swordService.updatePlayerSwords(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player) {
            if (!isPlayerAllowed(player)) return;
            swordService.updatePlayerSwords(player);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (isPlayerAllowed(player)) {
            swordService.updatePlayerSwords(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLootGenerate(LootGenerateEvent event) {
        for (ItemStack item : event.getLoot()) {
            if (swordService.isSword(item) && !swordService.isBlockingSword(item)) {
                swordService.applySwordBlocking(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        for (ItemStack item : event.getDrops()) {
            if (swordService.isSword(item) && !swordService.isBlockingSword(item)) {
                swordService.applySwordBlocking(item);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        // Swords should never be consumed as food
        if (swordService.isSword(event.getItem())) {
            event.setCancelled(true);
        }
    }

    private boolean isPlayerActivelyBlocking(Player player) {
        if (player.isBlocking()) {
            ItemStack active = player.getActiveItem();
            return swordService.isSword(active);
        }
        if (player.hasActiveItem()) {
            ItemStack active = player.getActiveItem();
            return swordService.isSword(active);
        }
        return false;
    }

    private boolean isAttackInFront(Player player, Location sourceLocation) {
        if (sourceLocation == null) return true;
        Vector playerDir = player.getLocation().getDirection().setY(0).normalize();
        Vector toSource = sourceLocation.toVector().subtract(player.getLocation().toVector()).setY(0).normalize();
        if (toSource.lengthSquared() == 0) return true;

        double angle = Math.toDegrees(playerDir.angle(toSource));
        return angle <= (configManager.getHorizontalBlockingAngle() / 2.0);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        if (!isPlayerAllowed(victim) || !isPlayerActivelyBlocking(victim)) {
            return;
        }

        Entity damager = event.getDamager();
        Location sourceLoc = damager.getLocation();

        // Direction check
        if (!isAttackInFront(victim, sourceLoc)) {
            return;
        }

        // Projectile deflection
        if (damager instanceof Projectile projectile && configManager.isDeflectProjectiles()) {
            Vector vel = projectile.getVelocity().multiply(-0.5);
            projectile.setVelocity(vel);

            playBlockEffects(victim);
            applyDurabilityDamage(victim);

            event.setCancelled(true);
            return;
        }

        // Melee attack: apply custom 1.8.9 damage reduction (default 50%)
        double reductionPercent = configManager.getDamageReductionPercent();
        double factor = Math.max(0.0, 1.0 - (reductionPercent / 100.0));
        event.setDamage(event.getDamage() * factor);

        playBlockEffects(victim);
        applyDurabilityDamage(victim);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onKnockback(EntityKnockbackEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!isPlayerAllowed(player) || !isPlayerActivelyBlocking(player)) {
            return;
        }

        double reduction = configManager.getKnockbackReductionPercent();
        if (reduction > 0) {
            double multiplier = Math.max(0.0, 1.0 - (reduction / 100.0));
            event.setKnockback(event.getKnockback().multiply(multiplier));
        }
    }

    private void playBlockEffects(Player player) {
        if (configManager.isPlayParticles()) {
            Vector dir = player.getLocation().getDirection().setY(0).normalize();
            Location particleLoc = player.getEyeLocation().add(dir.multiply(0.6));
            player.getWorld().spawnParticle(Particle.CRIT, particleLoc, 10, 0.2, 0.2, 0.2, 0.1);
        }

        String soundName = configManager.getBlockSound();
        try {
            net.kyori.adventure.key.Key key = net.kyori.adventure.key.Key.key(
                    soundName.contains(":") ? soundName : "minecraft:" + soundName
            );
            net.kyori.adventure.sound.Sound advSound = net.kyori.adventure.sound.Sound.sound(
                    key,
                    net.kyori.adventure.sound.Sound.Source.PLAYER,
                    1.0f,
                    1.0f
            );
            player.playSound(advSound);
        } catch (Exception ignored) {
            player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0f, 1.0f);
        }
    }

    private void applyDurabilityDamage(Player player) {
        int damage = configManager.getDurabilityDamage();
        if (damage <= 0) return;

        EquipmentSlot slot = player.getActiveItemHand();
        if (slot == null) slot = EquipmentSlot.HAND;

        ItemStack item = player.getInventory().getItem(slot);
        if (swordService.isSword(item)) {
            player.damageItemStack(slot, damage);
        }
    }
}
