package com.holomery.listener;

import com.holomery.manager.HologramManager;
import com.holomery.model.Hologram;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;

/**
 * Handles world/chunk loading and interactive in-game hologram clicking for page turning.
 */
public class HologramListener implements Listener {

    private final HologramManager manager;

    public HologramListener(HologramManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        manager.onChunkLoad(event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkUnload(ChunkUnloadEvent event) {
        manager.onChunkUnload(event.getWorld(), event.getChunk().getX(), event.getChunk().getZ());
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        manager.cleanOrphanEntities(event.getWorld());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        handleHoloInteraction(event.getRightClicked(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        handleHoloInteraction(event.getRightClicked(), event.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            if (handleHoloInteraction(event.getEntity(), player)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean handleHoloInteraction(Entity entity, Player player) {
        if (entity.hasMetadata("HoloMery-Id")) {
            String holoId = entity.getMetadata("HoloMery-Id").get(0).asString();
            Hologram holo = manager.getHologram(holoId);
            if (holo != null) {
                java.util.List<String> actions = holo.getClickActions();
                if (!actions.isEmpty()) {
                    for (String action : actions) {
                        com.holomery.action.ActionExecutor.execute(player, holo, action);
                    }
                    return true;
                }

                if (holo.isClickPageSwitch() && holo.getPageCount() > 1) {
                    holo.nextPage(player);
                    return true;
                }
            }
        }
        return false;
    }
}
