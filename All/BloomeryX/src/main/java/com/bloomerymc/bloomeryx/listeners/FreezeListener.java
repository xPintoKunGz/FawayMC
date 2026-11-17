package com.bloomerymc.bloomeryx.listeners;

import com.bloomerymc.bloomeryx.BloomeryX;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class FreezeListener implements Listener {

    private final BloomeryX plugin;

    public FreezeListener(BloomeryX plugin) {
        this.plugin = plugin;
    }

    // --- Player Freeze Controls ---

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (plugin.getFreezeManager().isPlayerFrozen(player.getUniqueId())) {
            Location frozenLoc = plugin.getFreezeManager().getFrozenLocation(player.getUniqueId());
            if (frozenLoc != null) {
                // Cancel translation movement
                event.setCancelled(true);
                // Snap player position & head view so camera rotation is completely frozen
                Location to = event.getTo();
                if (to != null && (event.getFrom().getYaw() != to.getYaw() || event.getFrom().getPitch() != to.getPitch())) {
                    player.teleport(frozenLoc);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.getFreezeManager().isPlayerFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        if (plugin.getFreezeManager().isPlayerFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getFreezeManager().isWorldFrozen() && !event.getPlayer().hasPermission("bloomeryx.bypass.freezeworld")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (plugin.getFreezeManager().isPlayerFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (plugin.getFreezeManager().isWorldFrozen() && !event.getPlayer().hasPermission("bloomeryx.bypass.freezeworld")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (plugin.getFreezeManager().isPlayerFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player) {
            Player p = (Player) event.getWhoClicked();
            if (plugin.getFreezeManager().isPlayerFrozen(p.getUniqueId())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (plugin.getFreezeManager().isPlayerFrozen(player.getUniqueId()) && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage("§cYou are frozen and cannot execute commands!");
        }
    }

    // --- World Freeze Controls ---

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (plugin.getFreezeManager().isWorldFrozen()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockFlow(BlockFromToEvent event) {
        if (plugin.getFreezeManager().isWorldFrozen()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRedstone(BlockRedstoneEvent event) {
        if (plugin.getFreezeManager().isWorldFrozen()) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (plugin.getFreezeManager().isWorldFrozen()) {
            if (!(event instanceof EntityDamageByEntityEvent)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (plugin.getFreezeManager().isWorldFrozen()) {
            if (event.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent event) {
        if (plugin.getFreezeManager().isWorldFrozen()) {
            event.setCancelled(true);
        }
    }
}

