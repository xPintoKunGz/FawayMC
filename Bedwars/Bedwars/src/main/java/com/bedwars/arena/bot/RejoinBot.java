package com.bedwars.arena.bot;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.Team;
import com.bedwars.arena.TeamColor;
import com.bedwars.arena.spawner.Spawner;
import com.bedwars.arena.spawner.SpawnerType;
import com.bedwars.util.MessageUtil;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Ultra-Smart AI Rejoin & Practice Bot:
 * - Appears identical to real Minecraft players (Player skin skull, team gear, no [Bot] tag)
 * - Recorded in Replay sessions as 100% full 3D Minecraft player avatars with skins
 * - Bridges over gaps towards middle and enemy islands with team wool
 * - Gathers Iron, Gold, Diamonds, and Emeralds from spawners
 * - Exchanges resources for team upgrades (Sharpness, Protection), swords, armor, and Golden Apples
 * - Actively engages both enemy players AND opposing enemy bots in combat
 * - Targets and breaks enemy beds
 * - Periodically sends realistic, human-like chat messages
 */
public class RejoinBot {

    public static final String[] BOT_SKIN_PRESETS = {
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2YxMTc3MzhjYjg1NWY5MWU0YzlmZTNlNWQ1Y2FkOWI0NTg5N2QxMWY5OTFjZTFmNDgwZmJlZTI4MWVhY2EifX19", // Steve classic
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNmI0ZjkxNGU3MDdjYjg3YjgzMmU4YmVkNzE1Y2I4MzI0YTM2Mzg1Y2QzZmFkZjc5ODZiMmQ2MjNlZGNmIn19fQ==", // Alex classic
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWIwMjMyMmFjZTM3M2QyZDI5YjM5YjA3NWM3NmU2ZDY1YjY3ZDVhN2QyNzkzYzgzN2YyYjBhMjQ4ODUzYWRjIn19fQ==", // Bedwars Pro
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMWUxYjEzZjU0N2UzZjc0Y2Y0YTVhYjgxODMzMDgwYmM5NWEyYjJhMmNjNDYzYzRmMTNmODVlODFkMzQ1ZGUifX19", // Shadow Knight
            "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmM1OTY1MDYzODAxMjdmZGM5OWFlZTRkNmJmZTRmYzNmZTg1N2ZjZDcxNDgyYjFiODQ3MmE0ZmFjMjYzZTUifX19"  // Cyber Ninja
    };

    private final Arena arena;
    private final UUID playerUuid;
    private final String playerName;
    private final Team team;
    private final String skinValue;
    private final String skinSignature;

    private Husk entity;
    private BukkitTask aiTask;
    private boolean active = false;
    private Entity lastDamager;

    // Inventory and progress
    private int ironCount = 0;
    private int goldCount = 0;
    private int diamondCount = 0;
    private int emeraldCount = 0;
    private int swordTier = 1; // 1=Wood, 2=Stone, 3=Iron, 4=Diamond
    private int armorTier = 1; // 1=Leather, 2=Iron, 3=Diamond
    private int woolCount = 64; // Replenishes automatically for bridging
    private boolean hasBow = false;
    private int arrowCount = 0;
    private long lastBowShotTime = 0;
    private boolean sneaking = false;
    private long lastElytraBoostTime = 0;

    private int respawnSeconds = 0;
    private BukkitTask respawnTask;

    // AI timing and chat
    private int ticksAlive = 0;
    private long lastChatTime = 0;
    private static final Random RNG = new Random();

    private static final String[] CHAT_CALLOUTS = {
            "rush mid",
            "watch their bed",
            "coming back to base",
            "need more iron",
            "careful behind you",
            "got emeralds",
            "rushing",
            "someone defend",
            "bed covered",
            "shooting from range",
            "gg",
            "on my way",
            "careful"
    };

    public RejoinBot(Arena arena, UUID botUuid, String botName, Team team, String customSkin) {
        this.arena = arena;
        this.playerUuid = botUuid;
        this.playerName = botName;
        this.team = team;
        this.skinValue = (customSkin != null && !customSkin.isEmpty())
                ? customSkin
                : BOT_SKIN_PRESETS[Math.abs(botUuid.hashCode()) % BOT_SKIN_PRESETS.length];
        this.skinSignature = "";
    }

    public RejoinBot(Arena arena, Player player, Team team) {
        this.arena = arena;
        this.playerUuid = player.getUniqueId();
        this.playerName = player.getName();
        this.team = team;

        String[] extracted = com.bedwars.replay.ReplayRecorder.extractPlayerSkin(player);
        if (extracted != null && extracted.length > 0 && extracted[0] != null) {
            this.skinValue = extracted[0];
            this.skinSignature = extracted.length > 1 ? extracted[1] : "";
        } else {
            this.skinValue = BOT_SKIN_PRESETS[Math.abs(player.getUniqueId().hashCode()) % BOT_SKIN_PRESETS.length];
            this.skinSignature = "";
        }
    }

    public String[] getSkinTexture() {
        return new String[]{skinValue, skinSignature};
    }

    public void setLastDamager(Entity damager) {
        this.lastDamager = damager;
    }

    public Entity getLastDamager() {
        return lastDamager;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void spawn() {
        if (arena.getState() != com.bedwars.arena.ArenaState.IN_GAME) return;
        World world = arena.getWorld();
        if (world == null) return;

        Location spawnLoc = (team != null && team.getSpawnLocation() != null)
                ? team.getSpawnLocation()
                : world.getSpawnLocation();

        this.entity = world.spawn(spawnLoc, Husk.class, h -> {
            h.setBaby(false);
            h.setSilent(true);
            h.setCanPickupItems(true);
            h.setPersistent(false);
            h.setRemoveWhenFarAway(false);
            // Identical to player: NO [Bot] prefix!
            h.setCustomName(team.getColor().getChatColor() + playerName);
            h.setCustomNameVisible(true);
            h.addScoreboardTag("bw_bot");
            h.addScoreboardTag("bw_bot_" + team.getColor().name());

            if (h.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
                h.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
            }
            h.setHealth(20.0);

            if (h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
                h.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.30);
            }
            if (h.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE) != null) {
                h.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(4.5);
            }
        });

        equipBot();
        this.active = true;
        startAI();
    }

    private void equipBot() {
        if (entity == null || !entity.isValid() || entity.getEquipment() == null) return;

        // 1. Player Skull Head (Looks like the real player with their exact skin!)
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) skull.getItemMeta();
        if (sm != null) {
            try {
                Field profileField = sm.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                com.mojang.authlib.GameProfile profile = new com.mojang.authlib.GameProfile(playerUuid, playerName);
                profile.getProperties().put("textures", new com.mojang.authlib.properties.Property("textures", skinValue));
                profileField.set(sm, profile);
            } catch (Throwable ignored) {
                try {
                    sm.setOwningPlayer(Bukkit.getOfflinePlayer(playerUuid));
                } catch (Exception ignored2) {}
            }
            skull.setItemMeta(sm);
        }
        entity.getEquipment().setHelmet(skull);
        entity.getEquipment().setHelmetDropChance(0f);

        // 2. Armor based on current tier (Leather, Iron, Diamond)
        Color dyeColor = getTeamDyeColor(team.getColor());
        if (armorTier >= 3) {
            entity.getEquipment().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            entity.getEquipment().setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            entity.getEquipment().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
        } else if (armorTier >= 2) {
            entity.getEquipment().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            entity.getEquipment().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            entity.getEquipment().setBoots(new ItemStack(Material.IRON_BOOTS));
        } else {
            ItemStack chest = new ItemStack(Material.LEATHER_CHESTPLATE);
            LeatherArmorMeta lam = (LeatherArmorMeta) chest.getItemMeta();
            if (lam != null) {
                lam.setColor(dyeColor);
                chest.setItemMeta(lam);
            }
            entity.getEquipment().setChestplate(chest);

            ItemStack leg = new ItemStack(Material.LEATHER_LEGGINGS);
            LeatherArmorMeta lam2 = (LeatherArmorMeta) leg.getItemMeta();
            if (lam2 != null) {
                lam2.setColor(dyeColor);
                leg.setItemMeta(lam2);
            }
            entity.getEquipment().setLeggings(leg);

            ItemStack boot = new ItemStack(Material.LEATHER_BOOTS);
            LeatherArmorMeta lam3 = (LeatherArmorMeta) boot.getItemMeta();
            if (lam3 != null) {
                lam3.setColor(dyeColor);
                boot.setItemMeta(lam3);
            }
            entity.getEquipment().setBoots(boot);
        }
        entity.getEquipment().setChestplateDropChance(0f);
        entity.getEquipment().setLeggingsDropChance(0f);
        entity.getEquipment().setBootsDropChance(0f);

        // 3. Sword in Main Hand
        Material swordMat = switch (swordTier) {
            case 2 -> Material.STONE_SWORD;
            case 3 -> Material.IRON_SWORD;
            case 4 -> Material.DIAMOND_SWORD;
            default -> Material.WOODEN_SWORD;
        };
        entity.getEquipment().setItemInMainHand(new ItemStack(swordMat));
        entity.getEquipment().setItemInMainHandDropChance(0f);

        // 4. Pro Bedwars Shield in Off-Hand!
        entity.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
        entity.getEquipment().setItemInOffHandDropChance(0f);
    }

    private Color getTeamDyeColor(TeamColor tc) {
        if (tc == null) return Color.WHITE;
        return switch (tc) {
            case RED -> Color.RED;
            case BLUE -> Color.BLUE;
            case GREEN -> Color.LIME;
            case YELLOW -> Color.YELLOW;
            case AQUA -> Color.AQUA;
            case WHITE -> Color.WHITE;
            case PINK -> Color.FUCHSIA;
            case GRAY -> Color.GRAY;
            default -> Color.WHITE;
        };
    }

    private void startAI() {
        if (aiTask != null) aiTask.cancel();

        aiTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!active || entity == null || !entity.isValid() || entity.isDead()) {
                    if (active && (entity == null || entity.isDead())) {
                        handleDeath();
                    }
                    return;
                }

                ticksAlive++;

                // 1. Pro Void Clutch: Save from falling into void using pearl/wind burst
                if (checkVoidClutch()) return;

                // 2. Vacuum nearby spawner items: Iron, Gold, Diamond, Emerald
                vacuumNearbyItems();

                // 3. Periodic natural chat callouts
                maybeSendChatMessage();

                // 4. Low HP Survival: Eat Golden Apple if available
                if (entity.getHealth() <= 12.0 && goldCount >= 3) {
                    goldCount -= 3;
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                    entity.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1200, 0));
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
                }

                // 5. Threat Response: Enemy approaching our team bed!
                Entity intruder = findEnemyNearOurBed(16.0);
                if (intruder != null) {
                    engageCombat(intruder);
                    return;
                }

                // 6. Base Fortification: Cover our team bed if exposed
                if (team != null && team.isBedAlive() && !isBedDefended()) {
                    performBedDefense();
                    return;
                }

                // 7. Combat: Engage nearby enemy player or bot within 22 blocks
                Entity nearestEnemy = findNearestEnemy(22.0);
                if (nearestEnemy != null) {
                    engageCombat(nearestEnemy);
                    return;
                }

                // 8. Shopping & Team Upgrades (Swords, Armor, Traps, Sharpness, Bow)
                if (shouldShopOrUpgrade()) {
                    Location shopLoc = team.getShopNpcLocation();
                    if (shopLoc != null) {
                        stepTowards(shopLoc, 1.15, true);
                        if (entity.getLocation().distance(shopLoc) <= 3.5) {
                            performShopExchange();
                            return;
                        }
                    }
                }

                // 9. Bed Hunt: Attack and break alive enemy beds
                Team targetBedTeam = findEnemyBedToAttack();
                if (targetBedTeam != null && targetBedTeam.getBedLocation() != null) {
                    Location bedLoc = targetBedTeam.getBedLocation();
                    stepTowards(bedLoc, 1.15, true);

                    if (entity.getLocation().distance(bedLoc) <= 3.8) {
                        breakTargetBed(targetBedTeam, bedLoc.getBlock());
                        return;
                    }
                    return;
                }

                // 10. Middle / Emerald / Diamond Spawner Gathering (Ninja Bridge to Mid!)
                Location targetSpawner = findValuableSpawner();
                if (targetSpawner != null) {
                    stepTowards(targetSpawner, 1.10, true);
                    return;
                }

                // 11. Default: Team Base Spawner Gathering
                Location baseSpawner = team.getBaseSpawnerLocation();
                if (baseSpawner != null) {
                    double dist = entity.getLocation().distance(baseSpawner);
                    if (dist > 2.0) {
                        stepTowards(baseSpawner, 1.0, false);
                    } else if (ticksAlive % 20 == 0) {
                        Location cur = entity.getLocation();
                        cur.setYaw((float) (Math.random() * 360.0));
                        entity.teleport(cur);
                    }
                }
            }
        }.runTaskTimer(BedwarsPlugin.getInstance(), 8L, 8L); // Runs every 0.4s
    }

    private void vacuumNearbyItems() {
        for (Entity e : entity.getNearbyEntities(2.5, 2.0, 2.5)) {
            if (e instanceof Item item && item.isValid() && !item.isDead()) {
                Material mat = item.getItemStack().getType();
                int amt = item.getItemStack().getAmount();
                if (mat == Material.IRON_INGOT) {
                    ironCount += amt;
                    item.remove();
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
                } else if (mat == Material.GOLD_INGOT) {
                    goldCount += amt;
                    item.remove();
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.4f);
                } else if (mat == Material.DIAMOND) {
                    diamondCount += amt;
                    item.remove();
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.6f);
                } else if (mat == Material.EMERALD) {
                    emeraldCount += amt;
                    item.remove();
                    entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.6f, 1.8f);
                }
            }
        }
    }

    private boolean checkVoidClutch() {
        if (entity == null || !entity.isValid()) return false;
        Location cur = entity.getLocation();
        Location baseSpawn = (team != null && team.getSpawnLocation() != null) ? team.getSpawnLocation() : cur;

        if (cur.getY() < baseSpawn.getY() - 3.2) {
            Location safeLoc = findNearestSolidGround(cur, 14.0);
            if (safeLoc == null) safeLoc = baseSpawn;

            entity.teleport(safeLoc.clone().add(0, 1.2, 0));
            entity.setVelocity(new Vector(0, 0.25, 0));
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
            entity.getWorld().spawnParticle(Particle.PORTAL, entity.getLocation(), 25);
            sendRealisticChat("clutched!");
            return true;
        }
        return false;
    }

    private Location findNearestSolidGround(Location from, double radius) {
        Location best = null;
        double bestDistSq = Double.MAX_VALUE;
        World w = from.getWorld();
        if (w == null) return null;

        int cx = from.getBlockX();
        int cy = from.getBlockY();
        int cz = from.getBlockZ();
        int r = (int) radius;

        for (int x = cx - r; x <= cx + r; x += 2) {
            for (int z = cz - r; z <= cz + r; z += 2) {
                for (int y = Math.max(0, cy - 2); y <= cy + 6; y++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType().isSolid() && !w.getBlockAt(x, y + 1, z).getType().isSolid()) {
                        double dSq = from.distanceSquared(b.getLocation());
                        if (dSq < bestDistSq) {
                            bestDistSq = dSq;
                            best = b.getLocation();
                        }
                    }
                }
            }
        }
        return best;
    }

    private boolean isBedDefended() {
        return getExposedBedDefSpots().isEmpty();
    }

    private List<Block> getExposedBedDefSpots() {
        List<Block> exposed = new ArrayList<>();
        if (team == null || team.getBedLocation() == null) return exposed;
        Block bedBlock = team.getBedLocation().getBlock();
        List<Block> bedParts = getBedBlockList(bedBlock);
        if (bedParts.isEmpty()) return exposed;

        for (Block part : bedParts) {
            for (Block surr : getSurroundingBlocks(part)) {
                if (surr.getType() == Material.AIR || !surr.getType().isSolid()) {
                    if (!exposed.contains(surr)) {
                        exposed.add(surr);
                    }
                }
            }
        }
        return exposed;
    }

    private List<Block> getBedBlockList(Block bedBlock) {
        List<Block> list = new ArrayList<>();
        if (bedBlock == null) return list;
        if (bedBlock.getBlockData() instanceof org.bukkit.block.data.type.Bed bedData) {
            list.add(bedBlock);
            org.bukkit.block.BlockFace facing = bedData.getFacing();
            Block otherHalf = (bedData.getPart() == org.bukkit.block.data.type.Bed.Part.HEAD)
                    ? bedBlock.getRelative(facing.getOppositeFace())
                    : bedBlock.getRelative(facing);
            if (otherHalf.getType().name().endsWith("_BED")) {
                list.add(otherHalf);
            }
        } else if (bedBlock.getType().name().endsWith("_BED")) {
            list.add(bedBlock);
        }
        return list;
    }

    private List<Block> getSurroundingBlocks(Block center) {
        List<Block> list = new ArrayList<>();
        list.add(center.getRelative(org.bukkit.block.BlockFace.UP));
        list.add(center.getRelative(org.bukkit.block.BlockFace.NORTH));
        list.add(center.getRelative(org.bukkit.block.BlockFace.SOUTH));
        list.add(center.getRelative(org.bukkit.block.BlockFace.EAST));
        list.add(center.getRelative(org.bukkit.block.BlockFace.WEST));
        return list;
    }

    private void performBedDefense() {
        List<Block> exposed = getExposedBedDefSpots();
        if (exposed.isEmpty() || entity == null || !entity.isValid()) return;

        Block targetSpot = exposed.get(0);
        Location spotLoc = targetSpot.getLocation().add(0.5, 0.5, 0.5);

        stepTowards(spotLoc, 1.15, true);

        if (entity.getLocation().distance(spotLoc) <= 3.8) {
            Material defMat = (ironCount >= 24) ? Material.END_STONE : (goldCount >= 4 ? Material.OAK_PLANKS : team.getColor().getWoolMaterial());
            if (defMat == Material.END_STONE && ironCount >= 24) ironCount -= 12;
            else if (defMat == Material.OAK_PLANKS && goldCount >= 4) goldCount -= 4;

            targetSpot.setType(defMat);
            arena.addPlacedBlock(targetSpot.getLocation());
            entity.swingMainHand();
            if (arena.getReplayRecorder() != null) {
                arena.getReplayRecorder().recordSwing(playerUuid);
            }

            Sound placeSound = (defMat == Material.END_STONE) ? Sound.BLOCK_STONE_PLACE : (defMat == Material.OAK_PLANKS ? Sound.BLOCK_WOOD_PLACE : Sound.BLOCK_WOOL_PLACE);
            targetSpot.getWorld().playSound(targetSpot.getLocation(), placeSound, 1f, 1f);
            targetSpot.getWorld().spawnParticle(Particle.BLOCK, targetSpot.getLocation().add(0.5, 0.5, 0.5), 8, targetSpot.getBlockData());

            if (isBedDefended() && RNG.nextBoolean()) {
                sendRealisticChat("bed defense completed!");
            }
        }
    }

    private Entity findEnemyNearOurBed(double radius) {
        if (team == null || team.getBedLocation() == null) return null;
        Location bedLoc = team.getBedLocation();
        double rSq = radius * radius;

        for (UUID u : arena.getPlayers()) {
            if (u.equals(playerUuid)) continue;
            Player p = Bukkit.getPlayer(u);
            if (p == null || !p.isOnline() || p.getGameMode() == GameMode.SPECTATOR) continue;
            Team pTeam = arena.getPlayerTeam(u);
            if (pTeam != null && pTeam.equals(team)) continue;

            if (p.getLocation().distanceSquared(bedLoc) <= rSq) {
                return p;
            }
        }

        for (Map.Entry<UUID, RejoinBot> entry : arena.getActiveBots().entrySet()) {
            if (entry.getKey().equals(playerUuid)) continue;
            RejoinBot otherBot = entry.getValue();
            if (otherBot == null || !otherBot.isActive()) continue;
            if (otherBot.team != null && otherBot.team.equals(team)) continue;
            Husk botEnt = otherBot.getEntity();
            if (botEnt != null && botEnt.isValid() && botEnt.getLocation().distanceSquared(bedLoc) <= rSq) {
                return botEnt;
            }
        }
        return null;
    }

    private void shootBow(Entity target) {
        if (entity == null || !entity.isValid() || target == null || !target.isValid()) return;

        entity.getEquipment().setItemInMainHand(new ItemStack(Material.BOW));
        entity.swingMainHand();
        if (arena.getReplayRecorder() != null) {
            arena.getReplayRecorder().recordSwing(playerUuid);
        }

        Location eyeLoc = entity.getEyeLocation();
        Vector dir = target.getLocation().add(0, 1.1, 0).toVector().subtract(eyeLoc.toVector());
        double dist = dir.length();
        dir.normalize();
        dir.add(new Vector(0, Math.min(0.24, dist * 0.015), 0));

        org.bukkit.entity.Arrow arrow = entity.getWorld().spawnArrow(eyeLoc.add(dir.clone().multiply(0.4)), dir, (float) Math.min(2.8, 1.3 + dist * 0.05), 1.2f);
        arrow.setShooter(entity);
        arrow.setDamage(5.5 + (team != null ? team.getSharpnessLevel() * 1.2 : 0.0));
        arrowCount--;

        entity.getWorld().playSound(eyeLoc, Sound.ENTITY_ARROW_SHOOT, 1f, 1.1f);

        Bukkit.getScheduler().runTaskLater(BedwarsPlugin.getInstance(), () -> {
            if (entity != null && entity.isValid()) {
                equipBot();
            }
        }, 12L);
    }

    private void engageCombat(Entity target) {
        if (target == null || !target.isValid()) return;

        double dist = entity.getLocation().distance(target.getLocation());

        // Long-Medium range: Snipe with Bow!
        if (hasBow && arrowCount > 0 && dist >= 7.0 && dist <= 24.0) {
            long now = System.currentTimeMillis();
            if (now - lastBowShotTime >= 1400L) {
                lastBowShotTime = now;
                shootBow(target);
                return;
            }
        }

        // Bridge and sprint towards enemy
        stepTowards(target.getLocation(), 1.35, true);

        if (dist <= 3.4) {
            entity.swingMainHand();
            if (arena.getReplayRecorder() != null) {
                arena.getReplayRecorder().recordSwing(playerUuid);
            }

            // Critical hit jump simulation
            if (entity.isOnGround() && RNG.nextBoolean()) {
                entity.setVelocity(entity.getVelocity().setY(0.36));
            }

            double dmg = (swordTier == 4) ? 7.5 : (swordTier == 3) ? 6.0 : (swordTier == 2) ? 5.0 : 4.0;
            if (team != null && team.getSharpnessLevel() > 0) dmg += 1.25;

            if (target instanceof Player p) {
                p.damage(dmg, entity);
                Vector kb = entity.getLocation().getDirection().setY(0).normalize().multiply(0.45).setY(0.26);
                p.setVelocity(kb);
            } else if (target instanceof Husk botHusk) {
                botHusk.damage(dmg, entity);
                Vector kb = entity.getLocation().getDirection().setY(0).normalize().multiply(0.45).setY(0.26);
                botHusk.setVelocity(kb);
            }

            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
            entity.getWorld().spawnParticle(Particle.SWEEP_ATTACK, entity.getLocation().add(0, 1, 0).add(entity.getLocation().getDirection().multiply(0.8)), 1);
        }
    }

    private void breakTargetBed(Team enemyTeam, Block bedBlock) {
        if (enemyTeam == null || !enemyTeam.isBedAlive() || bedBlock == null) return;

        // If bed is covered by defensive blocks, penetrate covering blocks first!
        Block covering = null;
        for (Block surr : getSurroundingBlocks(bedBlock)) {
            if (surr.getType().isSolid()) {
                covering = surr;
                break;
            }
        }

        if (covering != null && covering.getType() != Material.AIR) {
            entity.swingMainHand();
            if (arena.getReplayRecorder() != null) {
                arena.getReplayRecorder().recordSwing(playerUuid);
            }
            covering.getWorld().spawnParticle(Particle.BLOCK, covering.getLocation().add(0.5, 0.5, 0.5), 10, covering.getBlockData());
            covering.getWorld().playSound(covering.getLocation(), Sound.BLOCK_STONE_BREAK, 1f, 1f);
            covering.setType(Material.AIR);
            return;
        }

        // Direct bed break
        entity.swingMainHand();
        if (arena.getReplayRecorder() != null) {
            arena.getReplayRecorder().recordSwing(playerUuid);
        }

        bedBlock.getWorld().spawnParticle(Particle.BLOCK, bedBlock.getLocation().add(0.5, 0.5, 0.5), 12, bedBlock.getBlockData());
        bedBlock.getWorld().playSound(bedBlock.getLocation(), Sound.BLOCK_WOOD_BREAK, 1f, 1f);

        // Break the bed block cleanly!
        arena.breakBedByBot(playerName, team, enemyTeam, bedBlock);

        // Natural chat reaction after breaking bed
        if (RNG.nextBoolean()) {
            BedwarsPlugin.getInstance().getServer().getScheduler().runTaskLater(BedwarsPlugin.getInstance(), () -> {
                sendRealisticChat("bed destroyed!");
            }, 20L);
        }
    }

    private boolean shouldShopOrUpgrade() {
        return ironCount >= 10 || goldCount >= 4 || diamondCount >= 2 || emeraldCount >= 4;
    }

    private void performShopExchange() {
        if (entity == null || !entity.isValid()) return;

        // 1. Team Sharpness Upgrade with Diamonds
        if (diamondCount >= 4 && team != null && team.getSharpnessLevel() == 0) {
            diamondCount -= 4;
            team.setSharpnessLevel(1);
            arena.broadcast("§b✦ " + team.getColor().getChatColor() + playerName + " §7unlocked §bSharpened Swords §7for the team!");
            for (UUID u : team.getPlayers()) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) MessageUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            }
            return;
        }

        // 2. Team Protection Upgrade with Diamonds
        if (diamondCount >= 2 && team != null && team.getProtectionLevel() < 2) {
            diamondCount -= 2;
            team.setProtectionLevel(team.getProtectionLevel() + 1);
            arena.broadcast("§b✦ " + team.getColor().getChatColor() + playerName + " §7upgraded §bReinforced Armor §7to Tier " + team.getProtectionLevel() + "!");
            for (UUID u : team.getPlayers()) {
                Player p = Bukkit.getPlayer(u);
                if (p != null) MessageUtil.playSound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            }
            return;
        }

        // 3. Diamond Armor with Emeralds
        if (emeraldCount >= 6 && armorTier < 3) {
            emeraldCount -= 6;
            armorTier = 3;
            equipBot();
            entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_ARMOR_EQUIP_DIAMOND, 1f, 1f);
            return;
        }

        // 4. Iron Armor with Iron
        if (ironCount >= 12 && armorTier < 2) {
            ironCount -= 12;
            armorTier = 2;
            equipBot();
            entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
            return;
        }

        // 5. Sword Upgrades
        if (emeraldCount >= 4 && swordTier < 4) {
            emeraldCount -= 4;
            swordTier = 4; // Diamond Sword
            equipBot();
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
            return;
        }
        if (goldCount >= 7 && swordTier < 3) {
            goldCount -= 7;
            swordTier = 3; // Iron Sword
            equipBot();
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            return;
        }
        if (ironCount >= 10 && swordTier < 2) {
            ironCount -= 10;
            swordTier = 2; // Stone Sword
            equipBot();
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.0f);
            return;
        }

        // 6. Buy Bow & Arrows with Gold
        if (goldCount >= 12 && !hasBow) {
            goldCount -= 12;
            hasBow = true;
            arrowCount += 32;
            entity.getWorld().playSound(entity.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1.2f);
            return;
        }
        if (hasBow && arrowCount < 8 && goldCount >= 2) {
            goldCount -= 2;
            arrowCount += 16;
        }

        // 7. Golden Apples
        if (goldCount >= 3 && entity.getHealth() < 18.0) {
            goldCount -= 3;
            entity.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
            entity.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 1200, 0));
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_BURP, 1f, 1f);
        }
    }

    private Entity findNearestEnemy(double radius) {
        if (entity == null || !entity.isValid()) return null;
        Entity nearest = null;
        double nearestDistSq = radius * radius;

        // A. Search real enemy human players
        for (UUID u : arena.getPlayers()) {
            if (u.equals(playerUuid)) continue;
            Player p = Bukkit.getPlayer(u);
            if (p == null || !p.isOnline() || p.getGameMode() == GameMode.SPECTATOR) continue;

            Team pTeam = arena.getPlayerTeam(u);
            if (pTeam != null && pTeam.equals(team)) continue; // Teammate: skip!

            double distSq = entity.getLocation().distanceSquared(p.getLocation());
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = p;
            }
        }

        // B. Search opposing enemy bots!
        for (Map.Entry<UUID, RejoinBot> entry : arena.getActiveBots().entrySet()) {
            if (entry.getKey().equals(playerUuid)) continue;
            RejoinBot otherBot = entry.getValue();
            if (otherBot == null || !otherBot.isActive()) continue;
            if (otherBot.team != null && otherBot.team.equals(team)) continue; // Friendly bot: skip!

            Husk botEntity = otherBot.getEntity();
            if (botEntity == null || !botEntity.isValid()) continue;

            double distSq = entity.getLocation().distanceSquared(botEntity.getLocation());
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = botEntity;
            }
        }

        return nearest;
    }

    private Team findEnemyBedToAttack() {
        Team closestBedTeam = null;
        double closestDistSq = Double.MAX_VALUE;

        for (Team t : arena.getTeams().values()) {
            if (t.equals(team)) continue;
            if (!t.isBedAlive() || t.getBedLocation() == null) continue;

            double distSq = entity.getLocation().distanceSquared(t.getBedLocation());
            if (distSq < closestDistSq) {
                closestDistSq = distSq;
                closestBedTeam = t;
            }
        }
        return closestBedTeam;
    }

    private Location findValuableSpawner() {
        Location best = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Spawner sp : arena.getSpawners()) {
            if (sp.getType() == SpawnerType.EMERALD || sp.getType() == SpawnerType.DIAMOND) {
                double distSq = entity.getLocation().distanceSquared(sp.getLocation());
                if (distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = sp.getLocation();
                }
            }
        }
        return best;
    }

    /**
     * Moves bot towards target with Pro Ninja Bridging (shift-bridging over edges/void)
     * and Elytra/Wind charge jumps across gaps!
     */
    private void stepTowards(Location target, double speedMultiplier, boolean bridgeGaps) {
        if (entity == null || !entity.isValid() || target == null) return;

        Location curLoc = entity.getLocation();
        Vector dir = target.toVector().subtract(curLoc.toVector());
        dir.setY(0);

        if (dir.lengthSquared() < 0.09) {
            this.sneaking = false;
            return;
        }
        Vector unitDir = dir.normalize();

        // 1. Elytra / Wind Burst Jump across wide gaps to middle
        long now = System.currentTimeMillis();
        double distToTarget = curLoc.distance(target);
        if (distToTarget > 13.0 && now - lastElytraBoostTime > 14000L && RNG.nextDouble() < 0.25) {
            lastElytraBoostTime = now;
            entity.setVelocity(unitDir.clone().multiply(1.22).setY(0.48));
            entity.getWorld().playSound(curLoc, Sound.ITEM_ELYTRA_FLYING, 1f, 1.2f);
            entity.getWorld().spawnParticle(Particle.CLOUD, curLoc.add(0, 0.5, 0), 12, 0.2, 0.2, 0.2, 0.05);
            return;
        }

        // 2. Ninja Bridging Logic (Shift on edges, place wool, unsneak on solid blocks)
        if (bridgeGaps && woolCount > 0) {
            Location lookAhead = curLoc.clone().add(unitDir.clone().multiply(0.85));
            Location blockBelow = lookAhead.clone().subtract(0, 1.0, 0);

            if (blockBelow.getBlock().getType() == Material.AIR || !blockBelow.getBlock().getType().isSolid()) {
                this.sneaking = true; // Crouch / Shift at edge!

                Material woolMat = team.getColor().getWoolMaterial();
                blockBelow.getBlock().setType(woolMat);
                arena.addPlacedBlock(blockBelow);
                blockBelow.getWorld().playSound(blockBelow, Sound.BLOCK_WOOL_PLACE, 0.8f, 1.2f);
                blockBelow.getWorld().spawnParticle(Particle.BLOCK, blockBelow.clone().add(0.5, 0.5, 0.5), 6, blockBelow.getBlock().getBlockData());
                entity.swingMainHand();
                if (arena.getReplayRecorder() != null) {
                    arena.getReplayRecorder().recordSwing(playerUuid);
                }

                woolCount--;
                if (woolCount <= 0) woolCount = 64; // Auto-replenish wool for bot bridging
            } else {
                this.sneaking = false; // Fast un-sneak when on solid blocks!
            }

            // Jump over 1-block obstacles smoothly
            if (lookAhead.getBlock().getType().isSolid()) {
                Location headSpace = lookAhead.clone().add(0, 1.0, 0);
                if (!headSpace.getBlock().getType().isSolid()) {
                    entity.setVelocity(entity.getVelocity().setY(0.42));
                }
            }
        } else {
            this.sneaking = false;
        }

        // 3. Realistic velocity: slower when crouching, faster when sprinting
        double moveSpeed = sneaking ? 0.16 : (0.29 * speedMultiplier);
        Vector v = unitDir.multiply(moveSpeed);
        v.setY(entity.getVelocity().getY()); // preserve gravity
        entity.setVelocity(v);

        float yaw = (float) Math.toDegrees(Math.atan2(-unitDir.getX(), unitDir.getZ()));
        curLoc.setYaw(yaw);
        entity.teleport(curLoc);
    }

    private void maybeSendChatMessage() {
        long now = System.currentTimeMillis();
        // Speak every 30-50 seconds
        if (now - lastChatTime < 35000L) return;
        if (RNG.nextDouble() > 0.45) return;

        lastChatTime = now;
        String msg = CHAT_CALLOUTS[RNG.nextInt(CHAT_CALLOUTS.length)];
        sendRealisticChat(msg);
    }

    private void sendRealisticChat(String message) {
        String formatted = team.getColor().getChatColor() + playerName + "§f: " + message;
        arena.broadcast(formatted);
    }

    public void handleDeath() {
        if (!active) return;

        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }

        if (entity != null) {
            entity.remove();
            entity = null;
        }

        // Award killer credit if slain by a player
        if (lastDamager instanceof Player killer) {
            arena.addKill(killer.getUniqueId());
            if (!team.isBedAlive()) {
                arena.addFinalKill(killer.getUniqueId());
                arena.broadcast("§c[FINAL KILL] " + team.getColor().getChatColor() + playerName + " §7was eliminated by §f" + killer.getName() + "§7!");
                MessageUtil.playSound(killer, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                if (arena.getReplayRecorder() != null) {
                    arena.getReplayRecorder().recordEvent(arena.getGameSeconds(), "FINAL_KILL", killer.getName() + " final killed " + playerName);
                }
            } else {
                arena.broadcast(team.getColor().getChatColor() + playerName + " §7was killed by §f" + killer.getName() + "§7!");
                if (arena.getReplayRecorder() != null) {
                    arena.getReplayRecorder().recordEvent(arena.getGameSeconds(), "KILL", killer.getName() + " killed " + playerName);
                }
            }
        }

        // Bed alive -> respawn bot in 5 seconds
        if (team != null && team.isBedAlive()) {
            if (lastDamager == null) {
                arena.broadcast(team.getColor().getChatColor() + playerName + " §7died! §eRespawning in 5 seconds...");
            }
            respawnTask = new BukkitRunnable() {
                @Override
                public void run() {
                    if (active && team.isBedAlive()) {
                        spawn();
                    }
                }
            }.runTaskLater(BedwarsPlugin.getInstance(), 100L); // 5 seconds
        } else {
            if (lastDamager == null) {
                arena.broadcast(team.getColor().getChatColor() + playerName + " §7was final killed! (Bed destroyed)");
            }
            active = false;
        }

        lastDamager = null;
        arena.checkWinCondition();
    }

    public void despawn() {
        this.active = false;
        if (aiTask != null) {
            aiTask.cancel();
            aiTask = null;
        }
        if (respawnTask != null) {
            respawnTask.cancel();
            respawnTask = null;
        }
        if (entity != null && entity.isValid()) {
            Location loc = entity.getLocation();
            if (loc.getWorld() != null) {
                loc.getWorld().spawnParticle(Particle.CLOUD, loc.clone().add(0, 1, 0), 10, 0.2, 0.4, 0.2, 0.05);
                loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
            }
            entity.remove();
            entity = null;
        }
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public Team getTeam() {
        return team;
    }

    public Husk getEntity() {
        return entity;
    }

    public boolean isActive() {
        return active && entity != null && entity.isValid();
    }
}
