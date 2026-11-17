package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaState;
import com.bedwars.arena.Team;
import com.bedwars.arena.spawner.Spawner;
import com.bedwars.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.Iterator;

public class BlockListener implements Listener {

    private final BedwarsPlugin plugin;

    public BlockListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
            event.setCancelled(true);
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(player);

        // Outside-game visitor check: if placing block inside an arena map without edit mode
        if (arena == null) {
            Arena mapArena = findArenaAtLocation(event.getBlockPlaced().getLocation());
            if (mapArena != null && !mapArena.isEditMode()) {
                event.setCancelled(true);
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                MessageUtil.sendMessage(player, "§c✖ This map is protected! Use §e/bw edit " + mapArena.getName() + " §cto enable editing.");
                return;
            }
            return;
        }

        // If builder edit mode is ON, allow modifying the map freely!
        if (arena.isEditMode()) {
            return;
        }

        // Waiting or spectator
        if (arena.getState() != ArenaState.IN_GAME || arena.getSpectators().contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlockPlaced();

        // Map boundary check: prevent placing outside pos1/pos2 region
        if (!arena.isInRegion(block.getLocation())) {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "§c✖ You cannot place blocks outside the arena boundary!");
            return;
        }

        // Spawner proximity check: cannot place blocks near item generators
        Location placedLoc = block.getLocation();
        for (Spawner sp : arena.getSpawners()) {
            if (sp.getLocation() != null && sp.getLocation().getWorld().equals(placedLoc.getWorld())) {
                if (sp.getLocation().distance(placedLoc) <= 2.25) {
                    event.setCancelled(true);
                    MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    MessageUtil.sendMessage(player, "§c✖ You cannot place blocks near item generators!");
                    return;
                }
            }
        }
        for (Team t : arena.getTeams().values()) {
            if (t.getBaseSpawnerLocation() != null && t.getBaseSpawnerLocation().getWorld().equals(placedLoc.getWorld())) {
                if (t.getBaseSpawnerLocation().distance(placedLoc) <= 2.25) {
                    event.setCancelled(true);
                    MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    MessageUtil.sendMessage(player, "§c✖ You cannot place blocks near item generators!");
                    return;
                }
            }
        }

        // Base spawn & NPC proximity check: cannot place blocks around spawn and NPCs
        for (Team t : arena.getTeams().values()) {
            if (t.getSpawnLocation() != null && t.getSpawnLocation().getWorld().equals(placedLoc.getWorld())) {
                if (t.getSpawnLocation().distance(placedLoc) <= 3.5) {
                    event.setCancelled(true);
                    MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    MessageUtil.sendMessage(player, "§c✖ You cannot place blocks near team spawn point!");
                    return;
                }
            }
            if (t.getShopNpcLocation() != null && t.getShopNpcLocation().getWorld().equals(placedLoc.getWorld())) {
                if (t.getShopNpcLocation().distance(placedLoc) <= 3.0) {
                    event.setCancelled(true);
                    MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    MessageUtil.sendMessage(player, "§c✖ You cannot place blocks near shop NPC!");
                    return;
                }
            }
            if (t.getUpgradeNpcLocation() != null && t.getUpgradeNpcLocation().getWorld().equals(placedLoc.getWorld())) {
                if (t.getUpgradeNpcLocation().distance(placedLoc) <= 3.0) {
                    event.setCancelled(true);
                    MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    MessageUtil.sendMessage(player, "§c✖ You cannot place blocks near upgrade NPC!");
                    return;
                }
            }
        }
        for (Location npcLoc : arena.getShopNpcLocations()) {
            if (npcLoc.getWorld() != null && npcLoc.getWorld().equals(placedLoc.getWorld()) && npcLoc.distance(placedLoc) <= 3.0) {
                event.setCancelled(true);
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                MessageUtil.sendMessage(player, "§c✖ You cannot place blocks near shop NPC!");
                return;
            }
        }
        for (Location npcLoc : arena.getUpgradeNpcLocations()) {
            if (npcLoc.getWorld() != null && npcLoc.getWorld().equals(placedLoc.getWorld()) && npcLoc.distance(placedLoc) <= 3.0) {
                event.setCancelled(true);
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                MessageUtil.sendMessage(player, "§c✖ You cannot place blocks near upgrade NPC!");
                return;
            }
        }

        // TNT Auto-Prime with 2.0s fuse
        if (block.getType() == Material.TNT) {
            block.setType(Material.AIR);
            Location spawnLoc = block.getLocation().add(0.5, 0.2, 0.5);
            TNTPrimed tnt = spawnLoc.getWorld().spawn(spawnLoc, TNTPrimed.class);
            tnt.setFuseTicks(40); // 2.0 seconds
            spawnLoc.getWorld().playSound(spawnLoc, Sound.ENTITY_TNT_PRIMED, 1.2f, 1f);
            return;
        }

        // Instant Sweet Berry Bush growth when placed on dirt/grass
        if (block.getType() == Material.SWEET_BERRY_BUSH) {
            if (block.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
                ageable.setAge(ageable.getMaximumAge());
                block.setBlockData(ageable, false);
            }
        }

        // Add to rollback cache
        arena.addPlacedBlock(block.getLocation());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
            event.setCancelled(true);
            return;
        }
        PlayerListener.revealInvisPlayer(player, "mined a block");
        Arena arena = plugin.getArenaManager().getArena(player);

        // Outside-game visitor check: if breaking block inside an arena map without edit mode
        if (arena == null) {
            Arena mapArena = findArenaAtLocation(event.getBlock().getLocation());
            if (mapArena != null && !mapArena.isEditMode()) {
                event.setCancelled(true);
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                MessageUtil.sendMessage(player, "§c✖ This map is protected! Use §e/bw edit " + mapArena.getName() + " §cto enable editing.");
                return;
            }
            return;
        }

        // If builder edit mode is ON, allow breaking anything freely!
        if (arena.isEditMode()) {
            return;
        }

        // Waiting or spectator
        if (arena.getState() != ArenaState.IN_GAME || arena.getSpectators().contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        Block block = event.getBlock();

        // Map boundary check: prevent breaking outside pos1/pos2 region
        if (!arena.isInRegion(block.getLocation())) {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "§c✖ You cannot break blocks outside the arena boundary!");
            return;
        }

        // Check if bed
        if (block.getType().name().endsWith("_BED")) {
            event.setCancelled(true);
            arena.breakBed(player, block);
            return;
        }

        // Only allow breaking player-placed blocks
        if (arena.isBlockPlaced(block.getLocation())) {
            arena.removePlacedBlock(block.getLocation());
        } else {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, plugin.getMessage("messages.cannot-break-map"));
        }
    }

    // Protect Map against TNT & Fireball explosions 100%!
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location loc = event.getLocation();
        Arena arena = findArenaAtLocation(loc);
        if (arena == null) return;

        // If game is NOT in progress (e.g. ENDING, GAME_OVER, WAITING), CANCEL explosion 100%!
        if (arena.getState() != ArenaState.IN_GAME) {
            event.setCancelled(true);
            event.blockList().clear();
            return;
        }

        // ONLY allow destroying blocks placed by players!
        // Never destroy map blocks, glass, beds, or obsidian!
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block b = iterator.next();
            String matName = b.getType().name();
            if (matName.endsWith("_BED") || matName.contains("GLASS") || matName.equals("OBSIDIAN")
                    || !arena.isInRegion(b.getLocation()) || !arena.isBlockPlaced(b.getLocation())) {
                iterator.remove(); // Protect map blocks and blocks outside boundary!
            } else {
                arena.removePlacedBlock(b.getLocation());
            }
        }
        event.setYield(0f); // Zero block entity drops

        // Fireball Jump & TNT Jump launch velocity!
        if (event.getEntity() instanceof org.bukkit.entity.Fireball || event.getEntity() instanceof org.bukkit.entity.TNTPrimed) {
            boolean isFireball = event.getEntity() instanceof org.bukkit.entity.Fireball;
            double radius = isFireball ? 4.5 : 5.5;
            for (org.bukkit.entity.Entity nearby : loc.getWorld().getNearbyEntities(loc, radius, radius, radius)) {
                if (nearby instanceof org.bukkit.entity.Player p && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) {
                    Location pLoc = p.getLocation();
                    org.bukkit.util.Vector push = pLoc.toVector().subtract(loc.toVector());
                    double dist = push.length();
                    if (dist < 0.1) {
                        push = pLoc.getDirection().setY(0.7);
                    } else {
                        push.normalize();
                        org.bukkit.util.Vector look = pLoc.getDirection();
                        push.setX(push.getX() * 0.65 + look.getX() * 0.75);
                        push.setZ(push.getZ() * 0.65 + look.getZ() * 0.75);
                        push.setY(Math.max(0.60, push.getY() * 0.8 + 0.45));
                    }
                    double mult = isFireball ? Math.max(0.95, 1.70 - (dist * 0.15)) : Math.max(0.85, 1.45 - (dist * 0.12));
                    org.bukkit.util.Vector finalVel = push.normalize().multiply(mult);
                    finalVel.setY(Math.min(1.15, Math.max(0.55, finalVel.getY() * 1.15)));
                    p.setVelocity(finalVel);
                    p.playSound(pLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 0.9f);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockExplode(BlockExplodeEvent event) {
        Location loc = event.getBlock().getLocation();
        Arena arena = findArenaAtLocation(loc);
        if (arena == null) return;

        // If game is NOT in progress, CANCEL explosion 100%!
        if (arena.getState() != ArenaState.IN_GAME) {
            event.setCancelled(true);
            event.blockList().clear();
            return;
        }

        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block b = iterator.next();
            String matName = b.getType().name();
            if (matName.endsWith("_BED") || matName.contains("GLASS") || matName.equals("OBSIDIAN")
                    || !arena.isInRegion(b.getLocation()) || !arena.isBlockPlaced(b.getLocation())) {
                iterator.remove();
            } else {
                arena.removePlacedBlock(b.getLocation());
            }
        }
        event.setYield(0f);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockFromTo(org.bukkit.event.block.BlockFromToEvent event) {
        Location toLoc = event.getToBlock().getLocation();
        Arena arena = findArenaAtLocation(toLoc);
        if (arena != null && arena.getState() == ArenaState.IN_GAME) {
            if (!arena.isInRegion(toLoc)) {
                event.setCancelled(true);
                return;
            }
            arena.addPlacedBlock(toLoc);
        }
    }

    private Arena findArenaAtLocation(Location loc) {
        if (loc == null || loc.getWorld() == null) return null;
        for (Arena a : plugin.getArenaManager().getArenas()) {
            if (a.getWorld() != null && a.getWorld().equals(loc.getWorld())) {
                if (a.isInRegion(loc)) {
                    return a;
                }
            }
            if (a.getLobbyLocation() != null && a.getLobbyLocation().getWorld().equals(loc.getWorld()) && a.isInRegion(loc)) {
                return a;
            }
            if (a.getSpectatorLocation() != null && a.getSpectatorLocation().getWorld().equals(loc.getWorld()) && a.isInRegion(loc)) {
                return a;
            }
        }
        return null;
    }
}
