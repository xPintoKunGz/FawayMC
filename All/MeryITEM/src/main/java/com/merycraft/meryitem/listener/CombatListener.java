package com.merycraft.meryitem.listener;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemAbility;
import com.merycraft.meryitem.item.ItemStat;
import com.merycraft.meryitem.util.ColorUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;

/**
 * Handles RPG custom damage calculations, critical hits, life steal, and defense mitigation.
 * Supports melee attacks, bows/crossbows, and thrown projectiles.
 */
public class CombatListener implements Listener {

    private final MeryItemPlugin plugin;
    private final Random random = new Random();

    public CombatListener(MeryItemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntity();

        Player attacker = null;
        if (event.getDamager() instanceof Player) {
            attacker = (Player) event.getDamager();
        } else if (event.getDamager() instanceof Projectile) {
            Projectile proj = (Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                attacker = (Player) proj.getShooter();
            }
        }

        double extraDamage = 0.0;
        boolean isCrit = false;
        double lifeStealPercent = 0.0;

        // 1. Process Attacker Custom Stats & Abilities
        if (attacker != null) {
            Map<ItemStat, Double> stats = plugin.getItemManager().getPlayerTotalStats(attacker);

            // Bonus Attack Damage & Magic Damage
            double bonusDmg = stats.getOrDefault(ItemStat.ATTACK_DAMAGE, 0.0);
            double magicDmg = stats.getOrDefault(ItemStat.MAGIC_DAMAGE, 0.0);
            extraDamage += (bonusDmg + magicDmg);

            // Berserk Ability Check (Increases damage by +30% when below 50% HP)
            ItemStack mainHand = attacker.getInventory().getItemInMainHand();
            CustomItem weapon = plugin.getItemManager().getFromItemStack(mainHand);
            if (weapon != null && weapon.getAbilities().contains(ItemAbility.BERSERK)) {
                if (attacker.getHealth() <= (attacker.getMaxHealth() * 0.5)) {
                    extraDamage += (event.getDamage() + extraDamage) * 0.30;
                    attacker.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, attacker.getLocation().add(0, 1.5, 0), 3);
                }
            }

            // Critical Chance & Damage
            double critChance = stats.getOrDefault(ItemStat.CRIT_CHANCE, 5.0); // 5% base crit
            double critDmg = stats.getOrDefault(ItemStat.CRIT_DAMAGE, 50.0); // +50% base crit damage
            if (random.nextDouble() * 100.0 < critChance) {
                isCrit = true;
                double multiplier = 1.0 + (critDmg / 100.0);
                double currentTotal = event.getDamage() + extraDamage;
                extraDamage = (currentTotal * multiplier) - event.getDamage();

                // Crit FX
                Location loc = victim.getLocation().add(0, 1.0, 0);
                victim.getWorld().spawnParticle(Particle.CRIT, loc, 15, 0.3, 0.3, 0.3, 0.1);
                attacker.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_CRIT, 0.8f, 1.2f);
                attacker.spigot().sendMessage(ChatMessageType.ACTION_BAR, 
                        new TextComponent(ColorUtil.color("&#F43F5E✦ CRITICAL HIT! &#FDA4AF" + String.format("%.1f", event.getDamage() + extraDamage) + " DMG")));
            }

            // Life Steal
            lifeStealPercent = stats.getOrDefault(ItemStat.LIFE_STEAL, 0.0);
            if (weapon != null && weapon.getAbilities().contains(ItemAbility.VAMPIRIC_TOUCH)) {
                lifeStealPercent += 10.0;
            }
        }

        // Apply extra damage
        double totalDamage = Math.max(0.0, event.getDamage() + extraDamage);

        // 2. Process Victim Defense & Mitigation
        if (victim instanceof Player) {
            Player victimPlayer = (Player) victim;
            Map<ItemStat, Double> victimStats = plugin.getItemManager().getPlayerTotalStats(victimPlayer);
            double defense = victimStats.getOrDefault(ItemStat.DEFENSE, 0.0);

            if (defense > 0) {
                // Diminishing returns defense formula: damage = raw * (100 / (100 + defense))
                double factor = 100.0 / (100.0 + defense);
                totalDamage *= factor;
            }

            // Thorns Aura Check (Armor & Offhand)
            for (ItemStack armor : victimPlayer.getInventory().getArmorContents()) {
                CustomItem cArmor = plugin.getItemManager().getFromItemStack(armor);
                if (cArmor != null && cArmor.getAbilities().contains(ItemAbility.THORNS_AURA) && attacker != null) {
                    double reflect = totalDamage * 0.20;
                    attacker.damage(reflect);
                    attacker.getWorld().spawnParticle(Particle.CRIT, attacker.getLocation().add(0, 1, 0), 5);
                    break;
                }
            }
            ItemStack offHand = victimPlayer.getInventory().getItemInOffHand();
            CustomItem cOff = plugin.getItemManager().getFromItemStack(offHand);
            if (cOff != null && cOff.getAbilities().contains(ItemAbility.THORNS_AURA) && attacker != null) {
                double reflect = totalDamage * 0.20;
                attacker.damage(reflect);
                attacker.getWorld().spawnParticle(Particle.CRIT, attacker.getLocation().add(0, 1, 0), 5);
            }
        }

        event.setDamage(Math.max(0.5, totalDamage));

        // 3. Process Life Steal healing
        if (attacker != null && lifeStealPercent > 0 && totalDamage > 0) {
            double heal = (totalDamage * (lifeStealPercent / 100.0));
            double newHealth = Math.min(attacker.getMaxHealth(), attacker.getHealth() + heal);
            attacker.setHealth(newHealth);
            attacker.getWorld().spawnParticle(Particle.HEART, attacker.getLocation().add(0, 2.0, 0), 2, 0.2, 0.2, 0.2, 0.05);
        }
    }
}
