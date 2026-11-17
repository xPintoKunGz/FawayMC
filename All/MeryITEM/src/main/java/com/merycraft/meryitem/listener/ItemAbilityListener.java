package com.merycraft.meryitem.listener;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.item.CustomItem;
import com.merycraft.meryitem.item.ItemAbility;
import com.merycraft.meryitem.util.ColorUtil;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles RPG active abilities, commands, potion effects, and player cooldowns.
 * Fully compatible with all item types (Swords, Axes, Bows, Crossbows, Tools, Custom Items).
 */
public class ItemAbilityListener implements Listener {

    private final MeryItemPlugin plugin;
    private final Map<UUID, Map<ItemAbility, Long>> cooldownMap = new HashMap<>();

    public ItemAbilityListener(MeryItemPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        CustomItem customItem = plugin.getItemManager().getFromItemStack(item);
        if (customItem == null) return;

        Action action = event.getAction();
        boolean isRight = (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK);
        boolean isLeft = (action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK);

        if (!isRight && !isLeft) return;

        // 1. Process Potion Effects on Click
        if (!customItem.getPotionEffects().isEmpty()) {
            String potTrigger = customItem.getPotionTrigger();
            if ((potTrigger.equalsIgnoreCase("RIGHT") && isRight) || (potTrigger.equalsIgnoreCase("LEFT") && isLeft)) {
                for (Map.Entry<PotionEffectType, Integer> entry : customItem.getPotionEffects().entrySet()) {
                    PotionEffectType pet = entry.getKey();
                    int lvl = entry.getValue();
                    if (pet != null && lvl > 0) {
                        int amp = Math.min(250, Math.max(0, lvl - 1));
                        player.addPotionEffect(new PotionEffect(pet, 199980, amp, true, true, true));
                    }
                }
                player.playSound(player.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 0.8f, 1.4f);
                player.sendMessage(ColorUtil.color("&#34D399✦ Activated potion effects for 9999s!"));
            }
        }

        // 2. Process Click Commands
        if (!customItem.getClickCommands().isEmpty()) {
            String clickType = customItem.getCommandClickType();
            if ((clickType.equalsIgnoreCase("LEFT") && isLeft) || (clickType.equalsIgnoreCase("RIGHT") && isRight)) {
                executeAbility(player, customItem, ItemAbility.RUN_COMMAND);
            }
        }

        // 3. Process Active Abilities
        for (ItemAbility ability : customItem.getAbilities()) {
            if (ability == ItemAbility.RUN_COMMAND) continue;
            if ("Passive".equalsIgnoreCase(ability.getTrigger())) continue;

            // Check if ability matches click
            boolean match = false;
            if ("Right-Click".equalsIgnoreCase(ability.getTrigger()) && isRight) {
                match = true;
            } else if ("Left-Click".equalsIgnoreCase(ability.getTrigger()) && isLeft) {
                match = true;
            }

            if (match) {
                executeAbility(player, customItem, ability);
                // For items like bows, don't cancel right-click so shooting still works unless requested
                if (!item.getType().name().contains("BOW")) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private void executeAbility(Player player, CustomItem item, ItemAbility ability) {
        UUID uuid = player.getUniqueId();
        long now = System.currentTimeMillis();

        cooldownMap.putIfAbsent(uuid, new HashMap<>());
        Map<ItemAbility, Long> playerCooldowns = cooldownMap.get(uuid);

        long lastUsed = playerCooldowns.getOrDefault(ability, 0L);
        long cdMillis = ability.getCooldownSeconds() * 1000L;

        if (now - lastUsed < cdMillis) {
            double remaining = (cdMillis - (now - lastUsed)) / 1000.0;
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent(ColorUtil.color("&#FB7185⌛ " + ability.getDisplayName() + " on cooldown: " + String.format("%.1f", remaining) + "s")));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.6f, 0.5f);
            return;
        }

        playerCooldowns.put(ability, now);

        // Execute Ability Logic
        switch (ability) {
            case CELESTIAL_SLASH: {
                Vector dir = player.getLocation().getDirection().normalize().multiply(1.5).setY(0.2);
                player.setVelocity(dir);

                Location loc = player.getLocation().add(0, 1.0, 0);
                player.getWorld().spawnParticle(Particle.SWEEP_ATTACK, loc, 3, 0.5, 0.2, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.END_ROD, loc, 15, 0.6, 0.3, 0.6, 0.05);
                player.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, 1.4f);

                for (Entity entity : player.getNearbyEntities(4, 2, 4)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        ((LivingEntity) entity).damage(14.0, player);
                        entity.setVelocity(dir.clone().multiply(0.5).setY(0.4));
                    }
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ColorUtil.color("&#F472B6✦ CELESTIAL SLASH!")));
                break;
            }

            case LIGHTNING_STRIKE: {
                RayTraceResult ray = player.getWorld().rayTrace(
                        player.getEyeLocation(),
                        player.getLocation().getDirection(),
                        40,
                        FluidCollisionMode.NEVER,
                        true,
                        0.5,
                        e -> !e.equals(player)
                );

                Location targetLoc = (ray != null && ray.getHitPosition() != null)
                        ? ray.getHitPosition().toLocation(player.getWorld())
                        : player.getLocation().add(player.getLocation().getDirection().multiply(20));

                player.getWorld().strikeLightningEffect(targetLoc);
                player.playSound(targetLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);

                for (Entity entity : targetLoc.getWorld().getNearbyEntities(targetLoc, 4, 3, 4)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        ((LivingEntity) entity).damage(16.0, player);
                        entity.setFireTicks(80);
                    }
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ColorUtil.color("&#FDE047⚡ THUNDER STRIKE!")));
                break;
            }

            case HEAL_BURST: {
                double newHealth = Math.min(player.getMaxHealth(), player.getHealth() + 6.0);
                player.setHealth(newHealth);
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 80, 0));

                Location loc = player.getLocation().add(0, 1.0, 0);
                player.getWorld().spawnParticle(Particle.HEART, loc, 10, 0.5, 0.5, 0.5, 0.1);
                player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 25, 0.5, 0.5, 0.5, 0.1);
                player.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ColorUtil.color("&#34D399❤ DIVINE RESTORATION (+6 HP)")));
                break;
            }

            case SHADOW_STEP: {
                Vector dir = player.getLocation().getDirection().setY(0).normalize().multiply(8.0);
                Location dest = player.getLocation().add(dir);
                if (dest.getBlock().isPassable() && dest.clone().add(0, 1, 0).getBlock().isPassable()) {
                    player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 25, 0.3, 0.5, 0.3, 0.1);
                    player.teleport(dest);
                    player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                    player.getWorld().spawnParticle(Particle.LARGE_SMOKE, dest.add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.05);

                    // Blind surrounding enemies for 3 seconds
                    for (Entity entity : player.getNearbyEntities(4, 2, 4)) {
                        if (entity instanceof LivingEntity && !entity.equals(player)) {
                            ((LivingEntity) entity).addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
                        }
                    }
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ColorUtil.color("&#C084FC✦ SHADOW STEP!")));
                } else {
                    player.sendMessage(ColorUtil.color("&#FB7185Cannot teleport into solid blocks!"));
                }
                break;
            }

            case TELEPORT: {
                RayTraceResult ray = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getLocation().getDirection(), 50, FluidCollisionMode.NEVER, true);
                Location dest;
                if (ray != null && ray.getHitPosition() != null) {
                    dest = ray.getHitPosition().toLocation(player.getWorld()).add(0, 1.0, 0);
                } else {
                    dest = player.getLocation().add(player.getLocation().getDirection().multiply(25));
                }
                dest.setDirection(player.getLocation().getDirection());

                player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation().add(0, 1, 0), 30, 0.5, 0.8, 0.5, 0.2);
                player.teleport(dest);
                player.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.2f);
                player.getWorld().spawnParticle(Particle.REVERSE_PORTAL, dest.clone().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.1);

                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ColorUtil.color("&#38BDF8✦ WARP BLINK!")));
                break;
            }

            case PULL: {
                RayTraceResult ray = player.getWorld().rayTrace(
                        player.getEyeLocation(),
                        player.getLocation().getDirection(),
                        35,
                        FluidCollisionMode.NEVER,
                        true,
                        0.8,
                        e -> !e.equals(player)
                );

                if (ray != null && ray.getHitEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) ray.getHitEntity();
                    Vector pullVec = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.8).setY(0.4);
                    target.setVelocity(pullVec);
                    player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.4f);
                    player.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.1);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ColorUtil.color("&#FBBF24✦ GRAPPLE HOOK! Pulled " + target.getName())));
                } else if (ray != null && ray.getHitBlock() != null) {
                    Vector pullSelf = ray.getHitBlock().getLocation().toVector().subtract(player.getLocation().toVector()).normalize().multiply(1.8).setY(0.5);
                    player.setVelocity(pullSelf);
                    player.playSound(player.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent(ColorUtil.color("&#FBBF24✦ GRAPPLE LEAP!")));
                } else {
                    player.sendMessage(ColorUtil.color("&#FB7185No target in range to grapple!"));
                }
                break;
            }

            case FIRE_BURST: {
                Location loc = player.getLocation();
                player.getWorld().spawnParticle(Particle.FLAME, loc.clone().add(0, 0.5, 0), 45, 1.5, 0.3, 1.5, 0.05);
                player.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.9f);

                for (Entity entity : player.getNearbyEntities(5, 2, 5)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        ((LivingEntity) entity).damage(10.0, player);
                        entity.setFireTicks(100);
                        Vector push = entity.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(0.8).setY(0.3);
                        entity.setVelocity(push);
                    }
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ColorUtil.color("&#FB923C🔥 IGNITE NOVA!")));
                break;
            }

            case FROST_NOVA: {
                Location loc = player.getLocation();
                player.getWorld().spawnParticle(Particle.SNOWFLAKE, loc.clone().add(0, 0.5, 0), 60, 2.0, 0.5, 2.0, 0.05);
                player.playSound(loc, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.5f);

                for (Entity entity : player.getNearbyEntities(5, 2, 5)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        LivingEntity le = (LivingEntity) entity;
                        le.damage(12.0, player);
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 3));
                        le.setFreezeTicks(140);
                    }
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ColorUtil.color("&#67E8F9❄ FROST NOVA!")));
                break;
            }

            case METEOR_STRIKE: {
                Vector dir = player.getLocation().getDirection().normalize();
                Location spawnLoc = player.getEyeLocation().add(dir.clone().multiply(1.5));
                Fireball fireball = player.getWorld().spawn(spawnLoc, Fireball.class);
                fireball.setShooter(player);
                fireball.setDirection(dir.multiply(1.5));
                fireball.setYield(2.0f);
                fireball.setIsIncendiary(true);
                player.playSound(player.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.9f);
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ColorUtil.color("&#EF4444☄ METEOR STRIKE!")));
                break;
            }

            case WIND_PULSE: {
                Location loc = player.getLocation().add(0, 1.0, 0);
                Vector dir = player.getLocation().getDirection().normalize();
                player.getWorld().spawnParticle(Particle.SONIC_BOOM, loc.clone().add(dir.clone().multiply(2)), 1);
                player.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, 1.0f, 1.3f);

                for (Entity entity : player.getNearbyEntities(6, 3, 6)) {
                    if (entity instanceof LivingEntity && !entity.equals(player)) {
                        ((LivingEntity) entity).damage(10.0, player);
                        Vector push = entity.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.6).setY(0.4);
                        entity.setVelocity(push);
                    }
                }
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ColorUtil.color("&#38BDF8💨 SONIC SHOCKWAVE!")));
                break;
            }

            case RUN_COMMAND: {
                for (String cmd : item.getClickCommands()) {
                    String parsed = cmd.replace("%player%", player.getName()).replace("%world%", player.getWorld().getName());
                    if (parsed.toLowerCase().startsWith("console:")) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed.substring(8).trim());
                    } else {
                        player.performCommand(parsed);
                    }
                }
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.7f, 1.4f);
                break;
            }

            default:
                break;
        }
    }
}
