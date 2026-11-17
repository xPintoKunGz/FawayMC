package com.merycraft.meryitem.effect;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/**
 * Advanced MMOItems-grade Particle Engine (Helix, Vortex, Double Rings, Galaxy, Fireflies, Aura, Offset).
 * Emits particles for main-hand, off-hand, and equipped armor pieces with active effects.
 */
public class ParticleEffectTask extends BukkitRunnable {

    private final MeryItemPlugin plugin;
    private final NamespacedKey effectKey;
    private final NamespacedKey motionKey;
    private double step = 0;

    public ParticleEffectTask(MeryItemPlugin plugin) {
        this.plugin = plugin;
        this.effectKey = new NamespacedKey(plugin, "meryitem_effect");
        this.motionKey = new NamespacedKey(plugin, "meryitem_particle_motion");
    }

    public void start() {
        // Run every 2 ticks (10 times per second) for smooth fluid MMO particle math
        this.runTaskTimer(plugin, 20L, 2L);
    }

    @Override
    public void run() {
        step += 0.2;
        if (step > Math.PI * 4) step = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline() || player.isDead()) continue;

            // 1. Main Hand
            renderItemEffect(player, player.getInventory().getItemInMainHand(), true);

            // 2. Off Hand
            renderItemEffect(player, player.getInventory().getItemInOffHand(), false);

            // 3. Armor Pieces
            for (ItemStack armor : player.getInventory().getArmorContents()) {
                renderItemEffect(player, armor, false);
            }
        }
    }

    private void renderItemEffect(Player player, ItemStack stack, boolean isMainHand) {
        if (stack == null || !stack.hasItemMeta()) return;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return;

        ItemEffect effect = null;
        ParticleMotion motion = ParticleMotion.AURA;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(effectKey, PersistentDataType.STRING)) {
            String effectName = pdc.get(effectKey, PersistentDataType.STRING);
            effect = ItemEffect.fromString(effectName);
            if (pdc.has(motionKey, PersistentDataType.STRING)) {
                motion = ParticleMotion.fromString(pdc.get(motionKey, PersistentDataType.STRING));
            }
        }

        if ((effect == null || effect == ItemEffect.NONE) && plugin.getItemManager().isMeryItem(stack)) {
            CustomItem cItem = plugin.getItemManager().getFromItemStack(stack);
            if (cItem != null) {
                effect = cItem.getEffect();
                motion = cItem.getParticleMotion();
            }
        }

        if (effect == null || effect == ItemEffect.NONE || effect.getParticle() == null) {
            return;
        }

        Location playerLoc = player.getLocation();
        Location center = playerLoc.clone().add(0, 1.0, 0);

        // Render according to selected MMOItems motion model
        switch (motion) {
            case HELIX: {
                // Double spiraling helix upwards
                double radius = 0.85;
                double y1 = (step % 2.0);
                double y2 = ((step + 1.0) % 2.0);
                double angle1 = step * 3;
                double angle2 = (step * 3) + Math.PI;

                Location p1 = playerLoc.clone().add(Math.cos(angle1) * radius, y1, Math.sin(angle1) * radius);
                Location p2 = playerLoc.clone().add(Math.cos(angle2) * radius, y2, Math.sin(angle2) * radius);

                player.getWorld().spawnParticle(effect.getParticle(), p1, 1, 0, 0, 0, 0.01);
                player.getWorld().spawnParticle(effect.getParticle(), p2, 1, 0, 0, 0, 0.01);
                break;
            }

            case VORTEX: {
                // Expanding vortex cone around player
                for (int i = 0; i < 3; i++) {
                    double r = 0.4 + (i * 0.35);
                    double angle = (step * 2.5) + (i * 2.094);
                    double y = 0.5 + (i * 0.45);
                    Location p = playerLoc.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r);
                    player.getWorld().spawnParticle(effect.getParticle(), p, 1, 0, 0, 0, 0.01);
                }
                break;
            }

            case DOUBLE_RINGS: {
                // Intersecting planetary orbital rings
                double r = 1.0;
                double a = step * 2.0;
                Location r1 = center.clone().add(Math.cos(a) * r, Math.sin(a) * 0.5, Math.sin(a) * r);
                Location r2 = center.clone().add(-Math.cos(a) * r, -Math.sin(a) * 0.5, Math.sin(a) * r);
                player.getWorld().spawnParticle(effect.getParticle(), r1, 1, 0, 0, 0, 0.01);
                player.getWorld().spawnParticle(effect.getParticle(), r2, 1, 0, 0, 0, 0.01);
                break;
            }

            case GALAXY: {
                // 3-armed rotating spiral galaxy
                double r = 0.9;
                for (int arm = 0; arm < 3; arm++) {
                    double a = (step * 1.8) + (arm * ((2 * Math.PI) / 3));
                    Location p = center.clone().add(Math.cos(a) * r, Math.sin(step + arm) * 0.2, Math.sin(a) * r);
                    player.getWorld().spawnParticle(effect.getParticle(), p, 1, 0, 0, 0, 0.01);
                }
                break;
            }

            case FIREFLIES: {
                // Erratic floating lights
                for (int i = 0; i < 2; i++) {
                    double offsetX = Math.sin(step + (i * 3)) * 0.7;
                    double offsetY = Math.cos((step * 1.5) + i) * 0.4;
                    double offsetZ = Math.cos(step + (i * 2)) * 0.7;
                    Location p = center.clone().add(offsetX, offsetY, offsetZ);
                    player.getWorld().spawnParticle(effect.getParticle(), p, 1, 0, 0, 0, 0.01);
                }
                break;
            }

            case OFFSET: {
                // Directly attached to weapon hand / blade tip
                Vector dir = playerLoc.getDirection().setY(0).normalize();
                Vector right = new Vector(-dir.getZ(), 0, dir.getX()).normalize().multiply(isMainHand ? 0.45 : -0.45);
                Location weaponTip = center.clone().add(dir.multiply(0.75)).add(right).add(0, 0.2, 0);
                player.getWorld().spawnParticle(effect.getParticle(), weaponTip, 2, 0.05, 0.05, 0.05, 0.01);
                break;
            }

            case AURA:
            default: {
                // Smooth surrounding planetary orbital aura
                double radius = 0.85;
                double x = Math.cos(step * 2) * radius;
                double z = Math.sin(step * 2) * radius;
                Location orbit = center.clone().add(x, Math.sin(step * 3) * 0.35, z);
                player.getWorld().spawnParticle(effect.getParticle(), orbit, 1, 0, 0, 0, 0.01);

                Location orbitOpposite = center.clone().add(-x, -Math.sin(step * 3) * 0.35, -z);
                player.getWorld().spawnParticle(effect.getParticle(), orbitOpposite, 1, 0, 0, 0, 0.01);
                break;
            }
        }
    }
}
