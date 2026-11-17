package com.mery.npc.listener;

import com.mery.npc.MeryNPC;
import com.mery.npc.model.NPCData;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class WorldListener implements Listener {

    private final MeryNPC plugin;

    public WorldListener(MeryNPC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        World loadedWorld = event.getWorld();
        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            if (data.isInWorld(loadedWorld)) {
                data.setLocation(new Location(
                        loadedWorld,
                        data.getX(),
                        data.getY(),
                        data.getZ(),
                        data.getYaw(),
                        data.getPitch()
                ));
                plugin.getNpcManager().spawnNPC(data);
            }
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        Chunk chunk = event.getChunk();
        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            if (!data.isInWorld(chunk.getWorld())) continue;
            Location loc = data.getLocation();
            if (loc != null && loc.getWorld() != null) {
                if ((loc.getBlockX() >> 4) == chunk.getX() && (loc.getBlockZ() >> 4) == chunk.getZ()) {
                    Entity existing = data.getEntityUuid() != null ? Bukkit.getEntity(data.getEntityUuid()) : null;
                    if (existing == null || !existing.isValid()) {
                        plugin.getNpcManager().spawnNPC(data);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            if (!data.isInWorld(chunk.getWorld())) continue;
            Location loc = data.getLocation();
            if (loc != null && loc.getWorld() != null) {
                if ((loc.getBlockX() >> 4) == chunk.getX() && (loc.getBlockZ() >> 4) == chunk.getZ()) {
                    plugin.getNpcManager().despawnNPC(data);
                }
            }
        }
    }
}
