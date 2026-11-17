package com.bedwars.arena.spawner;

import com.bedwars.BedwarsPlugin;
import com.bedwars.integration.HoloMeryHook;
import com.bedwars.util.MessageUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Spawner {

    private final SpawnerType type;
    private final Location location;
    private final com.bedwars.arena.Team team;
    private int tier = 1;
    private int interval;
    private int countdown;
    private int dropAmount;
    private final String holoId;

    private ArmorStand nameStand;
    private ArmorStand timerStand;
    private ArmorStand rotatingBlockStand;
    private BukkitTask spinTask;

    public Spawner(SpawnerType type, Location location) {
        this(type, location, null);
    }

    public Spawner(SpawnerType type, Location location, com.bedwars.arena.Team team) {
        this.type = type;
        this.location = location;
        this.team = team;
        this.interval = type.getDefaultInterval();
        if (type == SpawnerType.IRON) {
            this.interval = BedwarsPlugin.getInstance().getConfig().getInt("spawners.iron-interval", 1);
            this.dropAmount = BedwarsPlugin.getInstance().getConfig().getInt("spawners.iron-drop-amount", 2);
        } else if (type == SpawnerType.GOLD) {
            this.interval = BedwarsPlugin.getInstance().getConfig().getInt("spawners.gold-interval", 60);
            this.dropAmount = BedwarsPlugin.getInstance().getConfig().getInt("spawners.gold-drop-amount", 1);
        } else {
            this.dropAmount = 1;
        }
        this.countdown = this.interval;
        this.holoId = "bw_sp_" + type.name().toLowerCase() + "_" +
                (location != null ? (location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ()) : "0_0_0") +
                (team != null ? "_" + team.getColor().name() : "");
    }

    public SpawnerType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public com.bedwars.arena.Team getTeam() {
        return team;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
        if (this.countdown > interval) {
            this.countdown = interval;
        }
    }

    public int getDropAmount() {
        return dropAmount;
    }

    public void setDropAmount(int dropAmount) {
        this.dropAmount = dropAmount;
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = tier;
        if (type == SpawnerType.DIAMOND) {
            this.interval = tier == 1 ? 30 : (tier == 2 ? 20 : 12);
        } else if (type == SpawnerType.EMERALD) {
            this.interval = tier == 1 ? 60 : (tier == 2 ? 45 : 30);
        }
        updateHologram();
    }

    public void initHologram() {
        if (location == null || location.getWorld() == null) return;
        if (type == SpawnerType.IRON || type == SpawnerType.GOLD) return; // Keep iron/gold spawner clean without hologram
        destroyHologram();

        double cx = location.getBlockX() + 0.5;
        double cz = location.getBlockZ() + 0.5;
        double baseY = location.getY();

        // Hologram +3.0 blocks higher (elevated for great visibility)
        Location holoLoc = new Location(location.getWorld(), cx, baseY + 6.0, cz);

        String title = type.getTitle() + " §7(Tier " + tier + ")";
        String timerLine = "§eSpawns in §c" + countdown + " §eseconds";

        List<String> lines = new ArrayList<>();
        lines.add(MessageUtil.color(title));
        lines.add(MessageUtil.color(timerLine));

        // Try HoloMery first
        boolean holoMeryUsed = HoloMeryHook.createOrUpdateHologram(holoId, holoLoc, lines);

        // Fallback to ArmorStand if HoloMery is not present
        if (!holoMeryUsed) {
            nameStand = location.getWorld().spawn(holoLoc, ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setCustomName(MessageUtil.color(title));
                stand.setCustomNameVisible(true);
                stand.setMarker(true);
            });

            Location timerLoc = holoLoc.clone().subtract(0, 0.28, 0);
            timerStand = location.getWorld().spawn(timerLoc, ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setCustomName(MessageUtil.color(timerLine));
                stand.setCustomNameVisible(true);
                stand.setMarker(true);
            });
        }

        // Spawn rotating Diamond / Emerald block +3.0 blocks higher under the hologram
        if (type == SpawnerType.DIAMOND || type == SpawnerType.EMERALD) {
            Location blockLoc = new Location(location.getWorld(), cx, baseY + 3.65, cz);
            Material blockMat = (type == SpawnerType.DIAMOND) ? Material.DIAMOND_BLOCK : Material.EMERALD_BLOCK;
            rotatingBlockStand = location.getWorld().spawn(blockLoc, ArmorStand.class, stand -> {
                stand.setVisible(false);
                stand.setGravity(false);
                stand.setMarker(true);
                stand.getEquipment().setHelmet(new ItemStack(blockMat));
                stand.addScoreboardTag("bw_spawner_block");
            });

            spinTask = new BukkitRunnable() {
                float yaw = 0f;
                @Override
                public void run() {
                    if (rotatingBlockStand == null || !rotatingBlockStand.isValid()) {
                        cancel();
                        return;
                    }
                    yaw = (yaw + 7.5f) % 360.0f;
                    Location cur = rotatingBlockStand.getLocation();
                    cur.setYaw(yaw);
                    rotatingBlockStand.teleport(cur);
                }
            }.runTaskTimer(BedwarsPlugin.getInstance(), 2L, 2L);
        }
    }

    public void tick() {
        countdown--;
        if (countdown <= 0) {
            countdown = interval;
            spawnItem();
        }
        updateHologram();
    }

    private void updateHologram() {
        if (type == SpawnerType.IRON || type == SpawnerType.GOLD) return;

        String title = type.getTitle() + " §7(Tier " + tier + ")";
        String timerLine = "§eSpawns in §c" + countdown + " §eseconds";

        if (HoloMeryHook.isAvailable()) {
            HoloMeryHook.updateLine(holoId, 0, MessageUtil.color(title));
            HoloMeryHook.updateLine(holoId, 1, MessageUtil.color(timerLine));
        }

        if (timerStand != null && timerStand.isValid()) {
            timerStand.setCustomName(MessageUtil.color(timerLine));
        }
        if (nameStand != null && nameStand.isValid()) {
            nameStand.setCustomName(MessageUtil.color(title));
        }
    }

    public void spawnItem() {
        if (location == null || location.getWorld() == null) return;

        // Anti-lag: check item cap within 2.5 blocks
        Collection<Entity> nearby = location.getWorld().getNearbyEntities(location, 2.5, 2.5, 2.5);
        int currentCount = 0;
        int ironCap = BedwarsPlugin.getInstance().getConfig().getInt("spawners.iron-max-cap", 24);
        int goldCap = BedwarsPlugin.getInstance().getConfig().getInt("spawners.gold-max-cap", 8);
        int maxCap = (type == SpawnerType.IRON) ? ironCap : (type == SpawnerType.GOLD ? goldCap : (type == SpawnerType.DIAMOND ? 6 : 4));

        for (Entity e : nearby) {
            if (e instanceof Item item && item.getItemStack().getType() == type.getMaterial()) {
                currentCount += item.getItemStack().getAmount();
            }
        }

        if (currentCount >= maxCap) return;

        int toDrop = (dropAmount > 0) ? dropAmount : 1;
        toDrop = Math.min(toDrop, maxCap - currentCount);

        for (int i = 0; i < toDrop; i++) {
            if (location == null || location.getWorld() == null) return;
            // Drop items together tightly in a neat cluster/group at the center of the spawner pad
            double cx = location.getBlockX() + 0.5;
            double cz = location.getBlockZ() + 0.5;
            double cy = location.getY() + 0.25;
            Location dropLoc = new Location(location.getWorld(), cx, cy, cz);

            Item dropped = location.getWorld().dropItem(dropLoc, new ItemStack(type.getMaterial(), 1));
            dropped.setVelocity(new Vector(0, 0.04, 0)); // No scattering: drops together in a neat group
            dropped.addScoreboardTag("bw_no_merge");
            dropped.addScoreboardTag("bw_base_drop");
        }
    }

    public void destroyHologram() {
        if (type == SpawnerType.IRON || type == SpawnerType.GOLD) return;
        HoloMeryHook.deleteHologram(holoId);

        if (spinTask != null) {
            spinTask.cancel();
            spinTask = null;
        }
        if (rotatingBlockStand != null) {
            rotatingBlockStand.remove();
            rotatingBlockStand = null;
        }
        if (nameStand != null) {
            nameStand.remove();
            nameStand = null;
        }
        if (timerStand != null) {
            timerStand.remove();
            timerStand = null;
        }
    }
}
