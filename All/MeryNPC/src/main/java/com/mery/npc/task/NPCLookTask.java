package com.mery.npc.task;

import com.mery.npc.MeryNPC;
import com.mery.npc.model.NPCData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.UUID;

public class NPCLookTask extends BukkitRunnable {

    private final MeryNPC plugin;

    public NPCLookTask(MeryNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        double maxDist = plugin.getConfigManager().getLookDistance();
        double maxDistSq = maxDist * maxDist;

        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            if (!data.isLookAtPlayer() || data.getLocation() == null || data.getLocation().getWorld() == null) continue;

            Location npcLoc = data.getLocation();
            Player nearestPlayer = null;
            double nearestDistSq = maxDistSq;

            for (Player player : npcLoc.getWorld().getPlayers()) {
                if (!player.isValid() || player.isDead()) continue;
                double distSq = player.getLocation().distanceSquared(npcLoc);
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearestPlayer = player;
                }
            }

            if (nearestPlayer != null) {
                Location playerEye = nearestPlayer.getEyeLocation();
                Location npcEye = npcLoc.clone().add(0, 1.62, 0);

                Vector dir = playerEye.toVector().subtract(npcEye.toVector());
                double length = dir.length();
                if (length < 0.001) continue;

                dir.divide(new Vector(length, length, length));

                double dx = dir.getX();
                double dy = dir.getY();
                double dz = dir.getZ();

                // Safe clamping to avoid NaN
                double clampedDy = Math.max(-1.0, Math.min(1.0, dy));
                float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float pitch = (float) Math.toDegrees(-Math.asin(clampedDy));

                if (Float.isNaN(yaw) || Float.isNaN(pitch)) continue;

                // 1. If Real Player NPC (NMS)
                if (data.getServerPlayer() != null) {
                    for (UUID viewerId : data.getViewers()) {
                        Player viewer = Bukkit.getPlayer(viewerId);
                        if (viewer != null && viewer.isOnline()) {
                            plugin.getNmsPlayerSpawner().updateRotation(viewer, data.getServerPlayer(), yaw, pitch);
                        }
                    }
                }

                // 2. If Bukkit entity / ArmorStand
                if (data.getEntityUuid() != null) {
                    Entity entity = Bukkit.getEntity(data.getEntityUuid());
                    if (entity instanceof ArmorStand) {
                        ArmorStand stand = (ArmorStand) entity;
                        float curYaw = stand.getLocation().getYaw();
                        if (Math.abs(curYaw - yaw) > 1.5f) {
                            stand.setRotation(yaw, 0f);
                        }
                        EulerAngle currentHead = data.getPose() != null ? data.getPose().getHead() : EulerAngle.ZERO;
                        stand.setHeadPose(new EulerAngle(Math.toRadians(pitch), currentHead.getY(), currentHead.getZ()));
                    } else if (entity instanceof LivingEntity) {
                        entity.setRotation(yaw, pitch);
                    }
                }
            }
        }
    }
}
