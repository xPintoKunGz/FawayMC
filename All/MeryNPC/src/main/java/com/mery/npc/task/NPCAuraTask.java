package com.mery.npc.task;

import com.mery.npc.MeryNPC;
import com.mery.npc.model.NPCData;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class NPCAuraTask extends BukkitRunnable {

    private final MeryNPC plugin;
    private double angle = 0;

    public NPCAuraTask(MeryNPC plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        angle += Math.PI / 8.0;
        if (angle > Math.PI * 2.0) {
            angle -= Math.PI * 2.0;
        }

        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            String aura = data.getParticleAura();
            if (aura == null || aura.equalsIgnoreCase("NONE") || data.getLocation() == null) continue;
            World world = data.getLocation().getWorld();
            if (world == null || world.getPlayers().isEmpty()) continue;

            Location base = data.getLocation();
            double scale = data.getScale();
            double radius = 0.75 * scale;
            double x1 = Math.cos(angle) * radius;
            double z1 = Math.sin(angle) * radius;
            double x2 = Math.cos(angle + Math.PI) * radius;
            double z2 = Math.sin(angle + Math.PI) * radius;

            try {
                switch (aura.toUpperCase()) {
                    case "FLAME":
                        world.spawnParticle(Particle.FLAME, base.clone().add(x1, 0.15, z1), 1, 0, 0, 0, 0);
                        world.spawnParticle(Particle.FLAME, base.clone().add(x2, 0.15, z2), 1, 0, 0, 0, 0);
                        break;
                    case "SOUL":
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, base.clone().add(x1, 0.2, z1), 1, 0, 0, 0, 0);
                        world.spawnParticle(Particle.SOUL_FIRE_FLAME, base.clone().add(x2, 0.2, z2), 1, 0, 0, 0, 0);
                        break;
                    case "ENCHANT":
                        world.spawnParticle(Particle.ENCHANT, base.clone().add(0, 1.2 * scale, 0), 5, 0.4, 0.5, 0.4, 0.5);
                        break;
                    case "CHERRY":
                        world.spawnParticle(Particle.CHERRY_LEAVES, base.clone().add(x1, 1.8 * scale, z1), 1, 0.1, 0.1, 0.1, 0);
                        break;
                    case "END":
                        world.spawnParticle(Particle.END_ROD, base.clone().add(x1, 1.0 * scale + Math.sin(angle) * 0.4, z1), 1, 0, 0, 0, 0);
                        break;
                    case "HEART":
                        if (((int) (angle * 10)) % 4 == 0) {
                            world.spawnParticle(Particle.HEART, base.clone().add(x1 * 0.6, 1.9 * scale, z1 * 0.6), 1, 0, 0, 0, 0);
                        }
                        break;
                    case "TOTEM":
                        world.spawnParticle(Particle.TOTEM_OF_UNDYING, base.clone().add(x1, 0.5 * scale, z1), 1, 0, 0, 0, 0.02);
                        world.spawnParticle(Particle.TOTEM_OF_UNDYING, base.clone().add(x2, 0.5 * scale, z2), 1, 0, 0, 0, 0.02);
                        break;
                }
            } catch (Throwable ignored) {}
        }
    }
}

