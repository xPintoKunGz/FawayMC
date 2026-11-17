package com.mery.npc.listener;

import com.mery.npc.MeryNPC;
import com.mery.npc.manager.NPCManager;
import com.mery.npc.model.NPCData;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;

public class NPCProtectionListener implements Listener {

    private final MeryNPC plugin;

    public NPCProtectionListener(MeryNPC plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCreatureSpawnHighest(CreatureSpawnEvent event) {
        if (NPCManager.isSpawning || plugin.getNpcManager().getNPCByEntity(event.getEntity()) != null) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCreatureSpawnMonitor(CreatureSpawnEvent event) {
        if (NPCManager.isSpawning || plugin.getNpcManager().getNPCByEntity(event.getEntity()) != null) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onEntitySpawnHighest(EntitySpawnEvent event) {
        if (NPCManager.isSpawning || plugin.getNpcManager().getNPCByEntity(event.getEntity()) != null) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onEntitySpawnMonitor(EntitySpawnEvent event) {
        if (NPCManager.isSpawning || plugin.getNpcManager().getNPCByEntity(event.getEntity()) != null) {
            event.setCancelled(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        NPCData data = plugin.getNpcManager().getNPCByEntity(event.getRightClicked());
        if (data != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        NPCData data = plugin.getNpcManager().getNPCByEntity(event.getEntity());
        if (data != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityCombust(EntityCombustEvent event) {
        NPCData data = plugin.getNpcManager().getNPCByEntity(event.getEntity());
        if (data != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // Prevent NPC destruction in explosion
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        NPCData data = plugin.getNpcManager().getNPCByEntity(event.getEntity());
        if (data != null) {
            event.setCancelled(true);
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }
}

