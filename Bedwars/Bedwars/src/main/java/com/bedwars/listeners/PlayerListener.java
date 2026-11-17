package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaState;
import com.bedwars.arena.Team;
import com.bedwars.gui.ArenaSelectorGUI;
import com.bedwars.gui.ShopGUI;
import com.bedwars.gui.UpgradesGUI;
import com.bedwars.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final BedwarsPlugin plugin;
    private final Map<UUID, Long> fireballCooldowns = new HashMap<>();

    public PlayerListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    private static final java.util.Set<UUID> invisPlayers = new java.util.HashSet<>();

    public static boolean isPlayerInvis(UUID uuid) {
        return invisPlayers.contains(uuid);
    }

    public static void makePlayerFullyInvisible(Player player, Arena arena) {
        invisPlayers.add(player.getUniqueId());
        for (UUID u : arena.getPlayers()) {
            if (!u.equals(player.getUniqueId())) {
                Player other = Bukkit.getPlayer(u);
                if (other != null) {
                    other.hidePlayer(BedwarsPlugin.getInstance(), player);
                }
            }
        }
        MessageUtil.sendTitle(player, "§8[§7INVISIBILITY§8]", "§aCompletely hidden! (Breaks on attack or mining)", 10, 50, 10);
        player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 1f, 1.2f);
    }

    public static void revealInvisPlayer(Player player, String reason) {
        if (player == null) return;
        if (!invisPlayers.remove(player.getUniqueId())) return;
        player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        Arena arena = BedwarsPlugin.getInstance().getArenaManager().getArena(player);
        if (arena != null) {
            for (UUID u : arena.getPlayers()) {
                Player other = Bukkit.getPlayer(u);
                if (other != null) {
                    other.showPlayer(BedwarsPlugin.getInstance(), player);
                }
            }
            for (UUID u : arena.getSpectators()) {
                Player spec = Bukkit.getPlayer(u);
                if (spec != null) {
                    spec.showPlayer(BedwarsPlugin.getInstance(), player);
                }
            }
        }
        if (reason != null) {
            MessageUtil.sendMessage(player, "§c✦ Invisibility revealed because you " + reason + "!");
        }
        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1f, 1f);
        player.getWorld().spawnParticle(Particle.SMOKE, player.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
    }

    public static void revealAllInvisPlayers() {
        for (UUID u : new java.util.HashSet<>(invisPlayers)) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                revealInvisPlayer(p, null);
            }
        }
        invisPlayers.clear();
    }

    @EventHandler
    public void onPlayerItemConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena == null || arena.getState() != ArenaState.IN_GAME) return;

        if (item.getType() == Material.POTION && item.getItemMeta() instanceof org.bukkit.inventory.meta.PotionMeta pm) {
            if (pm.hasCustomEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY)
                    || (pm.getBasePotionType() != null && pm.getBasePotionType().name().contains("INVISIBILITY"))) {
                makePlayerFullyInvisible(player, arena);
            }
        }
    }

    @EventHandler
    public void onItemMerge(ItemMergeEvent event) {
        if (event.getEntity().getScoreboardTags().contains("bw_no_merge")
                || event.getTarget().getScoreboardTags().contains("bw_no_merge")) {
            event.setCancelled(true);
        }
    }

    // Base generator pickup throttle: prevents vacuuming stacks at once, items enter 1 by 1 ("ดรอปทีละชิ้น ไม่ใช่แบบเป็นสแตก")
    private final Map<UUID, Long> playerPickupBurstStart = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena == null || arena.getState() != ArenaState.IN_GAME) return;

        Item itemEntity = event.getItem();
        if (itemEntity.getScoreboardTags().contains("bw_base_drop") || itemEntity.getScoreboardTags().contains("bw_no_merge")) {
            ItemStack stack = itemEntity.getItemStack();
            int amount = stack.getAmount();

            // If an item entity stack has more than 1, split so strictly 1 item is absorbed at once
            if (amount > 1) {
                event.setCancelled(true);
                int take = 1;
                stack.setAmount(amount - take);
                itemEntity.setItemStack(stack);
                ItemStack given = new ItemStack(stack.getType(), take);
                player.getInventory().addItem(given);
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1.2f);
                return;
            }

            // Throttle: items enter 1 by 1 with a smooth 60ms pacing
            long now = System.currentTimeMillis();
            long lastPickup = playerPickupBurstStart.getOrDefault(player.getUniqueId(), 0L);
            if (now - lastPickup < 60) {
                event.setCancelled(true);
                return;
            }
            playerPickupBurstStart.put(player.getUniqueId(), now);
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArena(player);

        // Safety: If player is inside an arena, they must never be blocked by replay
        if (arena != null && plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
            plugin.getReplayManager().stopPlayback(player);
        }

        // Handle Replay controls
        if (plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
            event.setCancelled(true);
            int heldSlot = player.getInventory().getHeldItemSlot();
            ItemStack held = event.getItem();
            if (heldSlot == 8 || (held != null && held.getType() == Material.BARRIER)) {
                plugin.getReplayManager().stopPlayback(player);
                return;
            }

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                com.bedwars.replay.ReplaySession session = plugin.getReplayManager().getSession(player);
                if (session != null) {
                    switch (heldSlot) {
                        case 0 -> session.rewind10s();
                        case 1 -> session.previousEvent();
                        case 2 -> session.togglePause();
                        case 3 -> session.nextEvent();
                        case 4 -> session.forward10s();
                        case 5 -> session.cycleSpeed();
                        case 8 -> plugin.getReplayManager().stopPlayback(player);
                    }
                }
            }
            return;
        }

        ItemStack item = event.getItem();

        if (item == null) return;

        // Spectator Clock
        if (arena != null && arena.getSpectators().contains(player.getUniqueId())) {
            if (item.getType() == Material.CLOCK) {
                event.setCancelled(true);
                com.bedwars.gui.SpectatorGUI.open(player, arena);
                return;
            }
        }

        // Lobby Items
        if (arena != null && (arena.getState() == ArenaState.WAITING || arena.getState() == ArenaState.STARTING)) {
            if (item.getType() == Material.RED_BED) {
                event.setCancelled(true);
                arena.leave(player);
                return;
            }
            if (item.getType() == Material.COMPASS) {
                event.setCancelled(true);
                com.bedwars.gui.TeamSelectorGUI.open(player, arena);
                return;
            }
            if (item.getType() == Material.BLAZE_ROD) {
                event.setCancelled(true);
                com.bedwars.gui.PracticeGUI.open(player, arena);
                return;
            }
            if (item.getType() == Material.DIAMOND) {
                event.setCancelled(true);
                arena.cancelLobbyCountdown();
                arena.broadcast("§b§l[DIAMOND START] §e" + player.getName() + " §astarted the game immediately!");
                for (UUID u : arena.getPlayers()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null) {
                        MessageUtil.playSound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
                        MessageUtil.sendTitle(p, "§b§lGame Started!", "§eStarted by " + player.getName(), 5, 25, 5);
                    }
                }
                arena.startGame();
                return;
            }
        }

        // In-game mechanics
        if (arena != null && arena.getState() == ArenaState.IN_GAME) {
            // Tactical Defense Wall (Pop-up Ice Wall: 30s decay)
            if ((item.getType() == Material.PACKED_ICE || item.getType() == Material.CHEST) && item.getItemMeta() != null &&
                    (item.getItemMeta().getDisplayName().contains("Wall") || item.getItemMeta().getDisplayName().contains("Fortress") || item.getItemMeta().getDisplayName().contains("Barrier"))) {
                event.setCancelled(true);
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);

                org.bukkit.util.Vector dir = player.getLocation().getDirection().setY(0).normalize();
                org.bukkit.util.Vector right = new org.bukkit.util.Vector(-dir.getZ(), 0, dir.getX()).normalize();

                Location wallBase = player.getLocation().add(dir.clone().multiply(2)).getBlock().getLocation();
                java.util.List<Location> wallBlocks = new java.util.ArrayList<>();
                for (int w = -2; w <= 2; w++) { // 5 blocks wide
                    for (int y = 0; y <= 3; y++) { // 4 blocks high
                        Location bLoc = wallBase.clone().add(right.clone().multiply(w)).add(0, y, 0);
                        Block b = bLoc.getBlock();
                        if (b.getType() == Material.AIR && arena.isInRegion(bLoc)) {
                            b.setType(Material.PACKED_ICE); // Non-melting ice!
                            arena.addPlacedBlock(bLoc);
                            wallBlocks.add(bLoc);
                        }
                    }
                }
                player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_PLACE, 1f, 1f);
                MessageUtil.sendMessage(player, "§aTactical ice barrier wall deployed! (Dissolves in 30s)");

                // Disappear after 30 seconds!
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (arena.getState() != ArenaState.IN_GAME) return;
                        for (Location bLoc : wallBlocks) {
                            if (bLoc.getBlock().getType() == Material.PACKED_ICE) {
                                bLoc.getBlock().setType(Material.AIR, false);
                                arena.removePlacedBlock(bLoc);
                                bLoc.getWorld().spawnParticle(Particle.SNOWFLAKE, bLoc.clone().add(0.5, 0.5, 0.5), 6, 0.2, 0.2, 0.2, 0.05);
                            }
                        }
                    }
                }.runTaskLater(plugin, 600L); // 30 seconds
                return;
            }

            // Ravager Ally
            if (item.getType() == Material.RAVAGER_SPAWN_EGG) {
                event.setCancelled(true);
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);

                Ravager ravager = player.getWorld().spawn(player.getLocation(), Ravager.class);
                Team team = arena.getPlayerTeam(player.getUniqueId());
                String teamName = (team != null) ? team.getColor().getChatColor() + team.getColor().getDisplayName() : "§f";
                ravager.setCustomName(MessageUtil.color("§c§lSiege Ravager " + teamName));
                ravager.setCustomNameVisible(true);
                ravager.addScoreboardTag("bw_ally");
                if (team != null) {
                    ravager.addScoreboardTag("bw_team_" + team.getColor().name());
                }
                startAllyTargetingAI(ravager, arena, team);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_RAVAGER_ROAR, 1.2f, 1f);
                MessageUtil.sendMessage(player, "§aSummoned Siege Ravager! (Will actively attack enemies and protect teammates)");
                return;
            }

            // Hoglin Ally
            if (item.getType() == Material.HOGLIN_SPAWN_EGG) {
                event.setCancelled(true);
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);

                Hoglin hoglin = player.getWorld().spawn(player.getLocation(), Hoglin.class);
                hoglin.setImmuneToZombification(true);
                Team team = arena.getPlayerTeam(player.getUniqueId());
                String teamName = (team != null) ? team.getColor().getChatColor() + team.getColor().getDisplayName() : "§f";
                hoglin.setCustomName(MessageUtil.color("§6§lWar Hoglin " + teamName));
                hoglin.setCustomNameVisible(true);
                hoglin.addScoreboardTag("bw_ally");
                if (team != null) {
                    hoglin.addScoreboardTag("bw_team_" + team.getColor().name());
                }
                startAllyTargetingAI(hoglin, arena, team);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_HOGLIN_ANGRY, 1.2f, 1f);
                MessageUtil.sendMessage(player, "§aSummoned War Hoglin! (Will actively attack enemies and protect teammates)");
                return;
            }

            // Iron Golem Bed Defender
            if (item.getType() == Material.IRON_BLOCK && item.getItemMeta() != null &&
                    (item.getItemMeta().getDisplayName().contains("Golem") || item.getItemMeta().getDisplayName().contains("Defender"))) {
                event.setCancelled(true);
                if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
                else player.getInventory().setItemInMainHand(null);

                IronGolem golem = player.getWorld().spawn(player.getLocation(), IronGolem.class);
                Team team = arena.getPlayerTeam(player.getUniqueId());
                String teamName = (team != null) ? team.getColor().getChatColor() + team.getColor().getDisplayName() : "§f";
                golem.setCustomName(MessageUtil.color("§f§lBed Defender Golem " + teamName));
                golem.setCustomNameVisible(true);
                golem.addScoreboardTag("bw_golem");
                if (team != null) {
                    golem.addScoreboardTag("bw_team_" + team.getColor().name());
                }
                startAllyTargetingAI(golem, arena, team);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, 1f, 1f);
                MessageUtil.sendMessage(player, "§aSummoned Bed Defender Golem! (Will actively attack enemies and protect teammates)");
                return;
            }
            // Fireball
            if (item.getType() == Material.FIRE_CHARGE && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                event.setCancelled(true);

                long now = System.currentTimeMillis();
                long last = fireballCooldowns.getOrDefault(player.getUniqueId(), 0L);
                if (now - last < 500) return; // 0.5s cooldown
                fireballCooldowns.put(player.getUniqueId(), now);

                // Consume item
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    player.getInventory().setItemInMainHand(null);
                }

                Fireball fireball = player.launchProjectile(Fireball.class);
                fireball.setYield((float) plugin.getConfig().getDouble("game.fireball-yield", 2.2));
                fireball.setIsIncendiary(false);
                fireball.setVelocity(player.getLocation().getDirection().multiply(1.5));
                MessageUtil.playSound(player, Sound.ENTITY_GHAST_SHOOT, 1f, 1f);
                return;
            }

            // Bridge Egg
            if (item.getType() == Material.EGG && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)) {
                Team team = arena.getPlayerTeam(player.getUniqueId());
                Material wool = (team != null) ? team.getColor().getWoolMaterial() : Material.WHITE_WOOL;

                // Track the launched egg
                new BukkitRunnable() {
                    Egg egg = null;
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (egg == null) {
                            for (Entity e : player.getNearbyEntities(2, 2, 2)) {
                                if (e instanceof Egg launched) {
                                    egg = launched;
                                    break;
                                }
                            }
                        }

                        if (egg == null || !egg.isValid() || egg.isDead() || ticks++ > 40) {
                            cancel();
                            return;
                        }

                        Location loc = egg.getLocation().subtract(0, 1.2, 0);
                        if (loc.getBlock().getType() == Material.AIR) {
                            loc.getBlock().setType(wool);
                            arena.addPlacedBlock(loc.getBlock().getLocation());
                            loc.getWorld().playSound(loc, Sound.BLOCK_WOOL_PLACE, 0.7f, 1.2f);
                        }
                    }
                }.runTaskTimer(plugin, 1L, 1L);
            }
        }
    }

    @EventHandler
    public void onNPCInteract(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArena(player);

        if (arena == null || arena.getState() != ArenaState.IN_GAME) return;

        if (entity.getScoreboardTags().contains("bw_shop")) {
            event.setCancelled(true);
            ShopGUI.open(player, ShopGUI.ShopCategory.QUICK_BUY);
            return;
        } else if (entity.getScoreboardTags().contains("bw_upgrade")) {
            event.setCancelled(true);
            UpgradesGUI.open(player);
            return;
        }

        // Check MeryNPC entity
        if (com.bedwars.integration.MeryNPCHook.isAvailable()) {
            if (com.bedwars.integration.MeryNPCHook.isMeryNPCEntity(entity, "shop")) {
                event.setCancelled(true);
                ShopGUI.open(player, ShopGUI.ShopCategory.QUICK_BUY);
                return;
            }
            if (com.bedwars.integration.MeryNPCHook.isMeryNPCEntity(entity, "upgrade")) {
                event.setCancelled(true);
                UpgradesGUI.open(player);
                return;
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
            event.setCancelled(true);
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(player);

        // Outside-game protection: prevent outsiders from hitting in-game players or vice versa
        if (event instanceof EntityDamageByEntityEvent edbe) {
            Entity damagerEnt = edbe.getDamager();
            if (damagerEnt instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
                damagerEnt = shooter;
            }
            if (damagerEnt instanceof Player damagerPlayer) {
                Arena damagerArena = plugin.getArenaManager().getArena(damagerPlayer);
                // Outsider hitting in-game player
                if (arena != null && arena.getState() == ArenaState.IN_GAME && damagerArena != arena) {
                    event.setCancelled(true);
                    MessageUtil.sendMessage(damagerPlayer, "§c✖ You cannot attack players who are in a Bedwars match!");
                    return;
                }
                // In-game player hitting outsider
                if (arena == null && damagerArena != null && damagerArena.getState() == ArenaState.IN_GAME) {
                    event.setCancelled(true);
                    return;
                }
                // Friendly fire check between teammates
                if (arena != null && arena == damagerArena && arena.getState() == ArenaState.IN_GAME) {
                    Team t1 = arena.getPlayerTeam(player.getUniqueId());
                    Team t2 = arena.getPlayerTeam(damagerPlayer.getUniqueId());
                    if (t1 != null && t1.equals(t2)) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }

        if (arena == null) return;

        // Waiting lobby protection
        if (arena.getState() != ArenaState.IN_GAME) {
            event.setCancelled(true);
            return;
        }

        // Fireball damage reduction: capped to 1 full heart (2.0 HP)
        if (event instanceof EntityDamageByEntityEvent edbe) {
            Entity damager = edbe.getDamager();
            if (damager instanceof Fireball || (damager instanceof Projectile proj && proj.getType() == EntityType.FIREBALL)) {
                event.setDamage(2.0);
            }
            if (damager instanceof Player attacker) {
                revealInvisPlayer(attacker, "attacked a player");
            }
        }
        revealInvisPlayer(player, "took damage");

        // In-game death check
        if (player.getHealth() - event.getFinalDamage() <= 0) {
            event.setCancelled(true);
            Player killer = null;
            if (event instanceof EntityDamageByEntityEvent edbe) {
                if (edbe.getDamager() instanceof Player damager) {
                    killer = damager;
                } else if (edbe.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player shooter) {
                    killer = shooter;
                }
            }
            arena.handlePlayerDeath(player, killer);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            Arena arena = plugin.getArenaManager().getArena(player);
            if (arena != null && arena.getState() != ArenaState.IN_GAME) {
                event.setCancelled(true);
                player.setFoodLevel(20);
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
            event.setCancelled(true);
            return;
        }
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena == null) {
            // Outside-game visitor check: if dropping items inside an arena map
            for (Arena a : plugin.getArenaManager().getArenas()) {
                if (a.getWorld() != null && a.getWorld().equals(player.getWorld()) && a.isInRegion(player.getLocation())) {
                    if (!a.isEditMode()) {
                        event.setCancelled(true);
                        MessageUtil.sendMessage(player, "§c✖ You cannot drop items in this arena map!");
                        return;
                    }
                }
            }
            return;
        }

        if (arena.getState() != ArenaState.IN_GAME || arena.getSpectators().contains(player.getUniqueId()) || arena.isRespawning(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // In-game: prevent dropping protected items (swords, armor, tools, shears)
        ItemStack dropped = event.getItemDrop().getItemStack();
        if (isProtectedItem(dropped)) {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "§cYou cannot drop primary weapons, armor, or tools!");
            MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    @EventHandler
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Replay Browser Menu
        if (event.getView().getTitle().equals(com.bedwars.gui.ReplayGUI.GUI_TITLE)) {
            event.setCancelled(true);
            int slot = event.getRawSlot();
            if (slot == 49) {
                player.closeInventory();
                return;
            }
            ItemStack clicked = event.getCurrentItem();
            if (clicked != null && clicked.getType() == Material.FILLED_MAP) {
                java.util.List<com.bedwars.replay.ReplayData> list = com.bedwars.gui.ReplayGUI.getCachedReplays(player.getUniqueId());
                if (list != null && clicked.getItemMeta() != null && clicked.getItemMeta().getLore() != null) {
                    for (String line : clicked.getItemMeta().getLore()) {
                        if (line.contains("Match ID: ")) {
                            String idPrefix = line.replace("§8Match ID: §7", "").trim();
                            for (com.bedwars.replay.ReplayData r : list) {
                                if (r.getReplayId().startsWith(idPrefix)) {
                                    if (event.isRightClick()) {
                                        boolean deleted = plugin.getReplayManager().deleteReplay(r.getReplayId());
                                        if (deleted) {
                                            MessageUtil.sendMessage(player, "§a✔ Deleted replay: §e" + r.getReplayId());
                                            MessageUtil.playSound(player, Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                                            com.bedwars.gui.ReplayGUI.open(player);
                                        } else {
                                            MessageUtil.sendMessage(player, "§c✖ Failed to delete replay.");
                                        }
                                        return;
                                    }
                                    player.closeInventory();
                                    plugin.getReplayManager().startPlayback(player, r);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            return;
        }

        // Prevent moving items while in active replay playback
        if (plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
            event.setCancelled(true);
            return;
        }

        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena == null || arena.getState() != ArenaState.IN_GAME) return;

        // Prevent dropping via hotkey Q
        if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP || event.getClick() == org.bukkit.event.inventory.ClickType.CONTROL_DROP) {
            ItemStack current = event.getCurrentItem();
            if (isProtectedItem(current)) {
                event.setCancelled(true);
                MessageUtil.sendMessage(player, "§cYou cannot drop this item!");
                return;
            }
        }

        // Prevent clicking outside inventory with protected item on cursor
        if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.OUTSIDE) {
            ItemStack cursor = event.getCursor();
            if (isProtectedItem(cursor)) {
                event.setCancelled(true);
                MessageUtil.sendMessage(player, "§cYou cannot drop this item!");
                return;
            }
        }

        // Prevent unequipping armor
        if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR) {
            event.setCancelled(true);
        }
    }

    public static boolean isProtectedItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        String name = item.getType().name();
        return name.endsWith("_SWORD") ||
               name.endsWith("_HELMET") ||
               name.endsWith("_CHESTPLATE") ||
               name.endsWith("_LEGGINGS") ||
               name.endsWith("_BOOTS") ||
               name.endsWith("_PICKAXE") ||
               name.endsWith("_AXE") ||
               name.equals("SHEARS") ||
               name.equals("COMPASS") ||
               name.endsWith("_BED");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (SetupListener.renamingMap.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            String arenaName = SetupListener.renamingMap.remove(player.getUniqueId());
            Arena arena = plugin.getArenaManager().getArena(arenaName);
            if (arena != null) {
                String newName = event.getMessage().trim();
                arena.setDisplayName(newName);
                plugin.getArenaManager().saveArenas();
                MessageUtil.sendMessage(player, "§a✔ Changed display name of arena §e" + arena.getName() + " §ato " + MessageUtil.color(newName) + "§a!");
                MessageUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        com.bedwars.gui.SetupWizardGUI.open(player, arena);
                    }
                }.runTask(plugin);
            }
        }
    }

    private boolean isPlayerInActiveGame(String name) {
        if (name == null || name.isEmpty()) return false;
        Player target = Bukkit.getPlayer(name);
        if (target == null) return false;
        Arena a = plugin.getArenaManager().getArena(target);
        return a != null && a.getState() == ArenaState.IN_GAME;
    }

    private boolean isAnyPlayerInActiveGame() {
        for (Arena a : plugin.getArenaManager().getArenas()) {
            if (a.getState() == ArenaState.IN_GAME && !a.getPlayers().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // Anti-cheat: prevent giving items or granting OP from console while players are in-game
    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        String raw = event.getCommand().trim();
        if (raw.startsWith("/")) raw = raw.substring(1);
        String[] parts = raw.split("\\s+");
        if (parts.length == 0) return;

        String cmd = parts[0].toLowerCase();

        // 1. Block OP / DeOP commands from console targeting in-game players
        if (cmd.equals("op") || cmd.endsWith(":op") || cmd.equals("deop") || cmd.endsWith(":deop")) {
            if (parts.length > 1 && isPlayerInActiveGame(parts[1])) {
                event.setCancelled(true);
                event.getSender().sendMessage("§c✖ [Bedwars Anti-Cheat] Cannot OP/DeOP '" + parts[1] + "' while they are actively playing in a Bedwars match!");
                return;
            }
        }

        // 2. Block item spawning commands from console targeting in-game players
        if (cmd.equals("give") || cmd.endsWith(":give") || cmd.equals("item") || cmd.endsWith(":item") || cmd.equals("i")) {
            if (parts.length > 1) {
                String targetName = parts[1];
                if (targetName.startsWith("@")) {
                    if (isAnyPlayerInActiveGame()) {
                        event.setCancelled(true);
                        event.getSender().sendMessage("§c✖ [Bedwars Anti-Cheat] Cannot spawn items with target selectors while Bedwars matches are active!");
                        return;
                    }
                } else if (isPlayerInActiveGame(targetName)) {
                    event.setCancelled(true);
                    event.getSender().sendMessage("§c✖ [Bedwars Anti-Cheat] Cannot give items to '" + targetName + "' while they are actively playing in a Bedwars match!");
                    return;
                }
            }
        }
    }

    // Anti-cheat: prevent giving items, changing gamemode, or using OP commands during game
    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommandPreprocess(org.bukkit.event.player.PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String raw = event.getMessage().startsWith("/") ? event.getMessage().substring(1).trim() : event.getMessage().trim();
        String[] parts = raw.split("\\s+");
        if (parts.length == 0) return;

        String cmd = parts[0].toLowerCase();
        Arena playerArena = plugin.getArenaManager().getArena(player);

        // A. If the sender is currently in an active game: block all cheating commands
        if (playerArena != null && playerArena.getState() == ArenaState.IN_GAME) {
            if (cmd.equals("gamemode") || cmd.endsWith(":gamemode") || cmd.equals("gm")
                    || cmd.equals("op") || cmd.endsWith(":op") || cmd.equals("deop") || cmd.endsWith(":deop")
                    || cmd.equals("give") || cmd.endsWith(":give") || cmd.equals("item") || cmd.endsWith(":item") || cmd.equals("i")
                    || cmd.equals("fly") || cmd.equals("god") || cmd.equals("speed") || cmd.equals("clear") || cmd.endsWith(":clear")) {
                event.setCancelled(true);
                MessageUtil.sendMessage(player, "§c✖ Item spawning, administrative commands, and gamemode changes are strictly prohibited during Bedwars games!");
                return;
            }
        }

        // B. If ANY player tries to give items or OP to a player currently in a Bedwars game
        if (cmd.equals("op") || cmd.endsWith(":op") || cmd.equals("deop") || cmd.endsWith(":deop")) {
            if (parts.length > 1 && isPlayerInActiveGame(parts[1])) {
                event.setCancelled(true);
                MessageUtil.sendMessage(player, "§c✖ [Bedwars Anti-Cheat] Cannot OP/DeOP '" + parts[1] + "' while they are actively playing in a Bedwars match!");
                return;
            }
        }

        if (cmd.equals("give") || cmd.endsWith(":give") || cmd.equals("item") || cmd.endsWith(":item") || cmd.equals("i")) {
            if (parts.length > 1) {
                String targetName = parts[1];
                if (targetName.startsWith("@")) {
                    if (isAnyPlayerInActiveGame()) {
                        event.setCancelled(true);
                        MessageUtil.sendMessage(player, "§c✖ [Bedwars Anti-Cheat] Cannot spawn items with target selectors while Bedwars matches are active!");
                        return;
                    }
                } else if (isPlayerInActiveGame(targetName)) {
                    event.setCancelled(true);
                    MessageUtil.sendMessage(player, "§c✖ [Bedwars Anti-Cheat] Cannot give items to '" + targetName + "' while they are actively playing in a Bedwars match!");
                    return;
                }
            }
        }
    }

    @EventHandler(priority = org.bukkit.event.EventPriority.LOWEST)
    public void onGameModeChange(org.bukkit.event.player.PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena == null || arena.getState() != ArenaState.IN_GAME) return;

        // Spectators and respawning players must strictly remain in Spectator mode!
        if (arena.getSpectators().contains(player.getUniqueId()) || arena.isRespawning(player.getUniqueId())) {
            if (event.getNewGameMode() != org.bukkit.GameMode.SPECTATOR) {
                event.setCancelled(true);
            }
            return;
        }
        if (event.getNewGameMode() != org.bukkit.GameMode.SURVIVAL) {
            event.setCancelled(true);
            player.setGameMode(org.bukkit.GameMode.SURVIVAL);
        }
    }

    // 1.8.9 Snowball combat: deals 1.0 damage (0.5 heart) and applies knockback!
    @EventHandler(priority = org.bukkit.event.EventPriority.HIGH)
    public void onSnowballHit(org.bukkit.event.entity.ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof org.bukkit.entity.Snowball snowball)) return;
        if (!(snowball.getShooter() instanceof Player shooter)) return;
        if (!(event.getHitEntity() instanceof Player victim)) return;

        Arena arena = plugin.getArenaManager().getArena(victim);
        if (arena == null || arena.getState() != ArenaState.IN_GAME) return;

        Team shooterTeam = arena.getPlayerTeam(shooter.getUniqueId());
        Team victimTeam = arena.getPlayerTeam(victim.getUniqueId());
        if (shooterTeam != null && shooterTeam.equals(victimTeam)) {
            return; // Friendly fire disabled
        }

        // Deal 1.0 damage (0.5 heart) and apply 1.8.9 knockback!
        victim.damage(1.0, shooter);
        org.bukkit.util.Vector vel = snowball.getVelocity().normalize().multiply(0.48).setY(0.32);
        victim.setVelocity(vel);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.8f, 1.2f);
    }

    // Track placed liquid buckets for 100% clean rollback + protect maps against non-game pouring!
    @EventHandler
    public void onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Arena arena = plugin.getArenaManager().getArena(player);

        if (arena == null) {
            org.bukkit.block.Block targetBlock = event.getBlockClicked();
            for (Arena a : plugin.getArenaManager().getArenas()) {
                if (a.getWorld() != null && a.getWorld().equals(targetBlock.getWorld()) && a.isInRegion(targetBlock.getLocation())) {
                    if (!a.isEditMode()) {
                        event.setCancelled(true);
                        MessageUtil.sendMessage(player, "§c✖ This map is protected! Use §e/bw edit " + a.getName() + " §cto enable editing.");
                        return;
                    }
                }
            }
            return;
        }

        if (arena.getState() != ArenaState.IN_GAME && !arena.isEditMode()) {
            event.setCancelled(true);
            return;
        }

        org.bukkit.block.Block placedLiquid = event.getBlockClicked().getRelative(event.getBlockFace());
        if (!arena.isInRegion(placedLiquid.getLocation())) {
            event.setCancelled(true);
            MessageUtil.sendMessage(player, "§c✖ You cannot pour liquids outside the arena boundary!");
            return;
        }
        arena.addPlacedBlock(placedLiquid.getLocation());
    }

    // Iron Golem / Mob Allies targeting: cancel targeting teammates
    @EventHandler
    public void onEntityTarget(org.bukkit.event.entity.EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player targetPlayer)) return;
        Entity mob = event.getEntity();

        for (String tag : mob.getScoreboardTags()) {
            if (tag.startsWith("bw_team_")) {
                String colorName = tag.replace("bw_team_", "");
                Arena arena = plugin.getArenaManager().getArena(targetPlayer);
                if (arena != null) {
                    Team playerTeam = arena.getPlayerTeam(targetPlayer.getUniqueId());
                    if (playerTeam != null && playerTeam.getColor().name().equalsIgnoreCase(colorName)) {
                        event.setCancelled(true);
                        event.setTarget(null);
                        return;
                    }
                }
            }
        }
    }

    // Active AI for guardian allies: constantly acquires nearest enemy player + 2 minutes max lifespan
    private void startAllyTargetingAI(Mob mob, Arena arena, Team team) {
        String baseName = mob.getCustomName() != null ? mob.getCustomName() : "Ally";
        new BukkitRunnable() {
            int lifespanSeconds = 120; // 2 minutes max lifespan!

            @Override
            public void run() {
                if (!mob.isValid() || mob.isDead() || arena.getState() != ArenaState.IN_GAME) {
                    cancel();
                    return;
                }

                lifespanSeconds--;
                if (lifespanSeconds <= 0) {
                    mob.getWorld().spawnParticle(Particle.POOF, mob.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.05);
                    mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    mob.remove();
                    cancel();
                    return;
                }

                // Show remaining time in name tag
                mob.setCustomName(baseName + " §e(" + lifespanSeconds + "s)");

                Player currentTarget = (mob.getTarget() instanceof Player p) ? p : null;
                if (currentTarget != null && currentTarget.isOnline() && currentTarget.getGameMode() != GameMode.SPECTATOR) {
                    Team targetTeam = arena.getPlayerTeam(currentTarget.getUniqueId());
                    if (targetTeam != team && currentTarget.getLocation().distance(mob.getLocation()) <= 24.0) {
                        return; // Keep chasing current valid enemy
                    }
                }
                Player nearestEnemy = null;
                double nearestDist = 24.0;
                for (UUID u : arena.getPlayers()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p == null || !p.isOnline() || p.getGameMode() == GameMode.SPECTATOR) continue;
                    Team pTeam = arena.getPlayerTeam(u);
                    if (pTeam != null && pTeam == team) continue;
                    double dist = p.getLocation().distance(mob.getLocation());
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestEnemy = p;
                    }
                }
                if (nearestEnemy != null) {
                    mob.setTarget(nearestEnemy);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    // Protect Allies from teammates & protect teammates from allies
    @EventHandler
    public void onAllyDamage(EntityDamageByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity damager = event.getDamager();
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }

        // Case 1: Teammates cannot damage ally mobs (Golem, Ravager, Hoglin)
        if (damager instanceof Player playerDamager && (victim instanceof IronGolem || victim instanceof Ravager || victim instanceof Hoglin)) {
            Arena arena = plugin.getArenaManager().getArena(playerDamager);
            if (arena != null) {
                Team team = arena.getPlayerTeam(playerDamager.getUniqueId());
                if (team != null && victim.getScoreboardTags().contains("bw_team_" + team.getColor().name())) {
                    event.setCancelled(true);
                    MessageUtil.sendMessage(playerDamager, "§cYou cannot attack your team's guardian ally!");
                    return;
                }
            }
        }

        // Case 2: Ally mobs cannot damage teammates
        if (victim instanceof Player playerVictim && (damager instanceof IronGolem || damager instanceof Ravager || damager instanceof Hoglin)) {
            Arena arena = plugin.getArenaManager().getArena(playerVictim);
            if (arena != null) {
                Team team = arena.getPlayerTeam(playerVictim.getUniqueId());
                if (team != null && damager.getScoreboardTags().contains("bw_team_" + team.getColor().name())) {
                    event.setCancelled(true);
                    if (damager instanceof Mob m) m.setTarget(null);
                    return;
                }
            }
        }
    }

    // Cancel wild/natural monster spawns in arena worlds
    @EventHandler
    public void onCreatureSpawn(org.bukkit.event.entity.CreatureSpawnEvent event) {
        if (event.getSpawnReason() == org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason.CUSTOM) return;
        if (event.getEntity() instanceof Monster || event.getEntity() instanceof Flying || event.getEntity() instanceof Slime) {
            for (Arena arena : plugin.getArenaManager().getArenas()) {
                if (arena.getLobbyLocation() != null && arena.getLobbyLocation().getWorld().equals(event.getLocation().getWorld())) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // World chest rollback snapshot on open
    @EventHandler
    public void onChestOpen(org.bukkit.event.inventory.InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena == null || arena.getState() != ArenaState.IN_GAME) return;

        if (event.getInventory().getHolder() instanceof org.bukkit.block.Chest chest) {
            arena.recordChestSnapshot(chest.getLocation(), chest.getInventory().getContents());
        } else if (event.getInventory().getHolder() instanceof org.bukkit.block.DoubleChest doubleChest) {
            if (doubleChest.getLeftSide() instanceof org.bukkit.block.Chest left) {
                arena.recordChestSnapshot(left.getLocation(), left.getInventory().getContents());
            }
            if (doubleChest.getRightSide() instanceof org.bukkit.block.Chest right) {
                arena.recordChestSnapshot(right.getLocation(), right.getInventory().getContents());
            }
        }
    }

    // Accelerate Elytra durability loss so it breaks quickly
    @EventHandler
    public void onElytraDamage(org.bukkit.event.player.PlayerItemDamageEvent event) {
        if (event.getItem().getType() == Material.ELYTRA) {
            event.setDamage(event.getDamage() * 3);
        }
    }

    // When Elytra breaks, restore team chestplate immediately
    @EventHandler
    public void onElytraBreak(org.bukkit.event.player.PlayerItemBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack broken = event.getBrokenItem();
        if (broken.getType() == Material.ELYTRA) {
            Arena arena = plugin.getArenaManager().getArena(player);
            if (arena != null && arena.getState() == ArenaState.IN_GAME) {
                Team team = arena.getPlayerTeam(player.getUniqueId());
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    arena.updatePlayerArmor(player, team);
                    MessageUtil.sendMessage(player, "§c⚠ Your Elytra wings broke! Restored team armor.");
                }, 1L);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
            plugin.getReplayManager().stopPlayback(player);
        }
        Arena arena = plugin.getArenaManager().getArena(player);
        if (arena != null) {
            if (arena.getState() == ArenaState.IN_GAME) {
                arena.handleDisconnect(player);
            } else {
                arena.leave(player);
            }
        }
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Replay isolation: hide joining players from active replay viewers and vice-versa
        if (plugin.getReplayManager() != null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (plugin.getReplayManager().isWatchingReplay(online)) {
                    player.hidePlayer(plugin, online);
                    online.hidePlayer(plugin, player);
                }
            }
        }

        for (Arena arena : plugin.getArenaManager().getArenas()) {
            if (arena.getState() == ArenaState.IN_GAME && arena.getDisconnectedPlayers().containsKey(player.getUniqueId())) {
                org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (player.isOnline() && arena.getState() == ArenaState.IN_GAME && arena.getDisconnectedPlayers().containsKey(player.getUniqueId())) {
                        MessageUtil.sendMessage(player, "§6§m════════════════════════════════════════════════════");
                        MessageUtil.sendMessage(player, "§e§l✦ RECONNECT AVAILABLE! §7You have an active match in [§6" + arena.getName() + "§7]");
                        MessageUtil.sendMessage(player, "§a▶ Click or type §6/bw rejoin §ato reconnect to your team!");
                        MessageUtil.sendMessage(player, "§6§m════════════════════════════════════════════════════");
                        MessageUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
                    }
                }, 20L);
                break;
            }
        }
    }

    @EventHandler
    public void onBotDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) {
            damager = shooter;
        }

        boolean damagerIsBot = damager.getScoreboardTags().contains("bw_bot");
        boolean victimIsBot = victim.getScoreboardTags().contains("bw_bot");

        if (!damagerIsBot && !victimIsBot) return;

        String damagerTeam = null;
        String victimTeam = null;

        if (damagerIsBot) {
            for (String tag : damager.getScoreboardTags()) {
                if (tag.startsWith("bw_bot_")) damagerTeam = tag.substring(7);
            }
        } else if (damager instanceof Player p) {
            Arena a = plugin.getArenaManager().getArena(p);
            if (a != null) {
                Team t = a.getPlayerTeam(p.getUniqueId());
                if (t != null) damagerTeam = t.getColor().name();
            }
        }

        if (victimIsBot) {
            for (String tag : victim.getScoreboardTags()) {
                if (tag.startsWith("bw_bot_")) victimTeam = tag.substring(7);
            }
        } else if (victim instanceof Player p) {
            Arena a = plugin.getArenaManager().getArena(p);
            if (a != null) {
                Team t = a.getPlayerTeam(p.getUniqueId());
                if (t != null) victimTeam = t.getColor().name();
            }
        }

        // Prevent friendly fire between bots and same-team players
        if (damagerTeam != null && damagerTeam.equalsIgnoreCase(victimTeam)) {
            event.setCancelled(true);
            return;
        }

        // Shield blocking for bots holding shield
        if (victimIsBot && victim instanceof Husk botHusk) {
            ItemStack offHand = (botHusk.getEquipment() != null) ? botHusk.getEquipment().getItemInOffHand() : null;
            if (offHand != null && offHand.getType() == Material.SHIELD) {
                if (Math.random() < 0.45) {
                    event.setDamage(event.getDamage() * 0.15);
                    botHusk.getWorld().playSound(botHusk.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
                    botHusk.getWorld().spawnParticle(Particle.CRIT, botHusk.getLocation().add(0, 1.2, 0), 5);
                    if (damager instanceof LivingEntity livingDamager) {
                        Vector knockback = livingDamager.getLocation().toVector().subtract(botHusk.getLocation().toVector()).normalize().multiply(0.35).setY(0.18);
                        livingDamager.setVelocity(knockback);
                    }
                }
            }
        }

        // Track last damager on bot
        if (victimIsBot) {
            for (Arena a : plugin.getArenaManager().getArenas()) {
                if (a.getState() == ArenaState.IN_GAME) {
                    for (com.bedwars.arena.bot.RejoinBot bot : a.getActiveBots().values()) {
                        if (bot != null && bot.getEntity() != null && bot.getEntity().equals(victim)) {
                            bot.setLastDamager(damager);
                            break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onReplayPickup(org.bukkit.event.entity.EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onReplayEntityInteract(org.bukkit.event.player.PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (plugin.getReplayManager() != null && plugin.getReplayManager().isWatchingReplay(player)) {
            event.setCancelled(true);
        }
    }
}

