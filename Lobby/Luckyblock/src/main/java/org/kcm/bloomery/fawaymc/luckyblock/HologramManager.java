package org.kcm.bloomery.fawaymc.luckyblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.inventory.EquipmentSlot;

public class HologramManager implements Listener {
   private final Global plugin;
   private final List<TextDisplay> holograms = new ArrayList();
   private final List<Location> chestLocations = new ArrayList();
   private final Map<Location, ChestState> chestStates = new HashMap();
   private final List<Integer> animationTaskIds = new ArrayList();
   private final Random random = new Random();
   private int globalTick = 0;

   public HologramManager(Global plugin) {
      this.plugin = plugin;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void start() {
      this.spawnHolograms();
      this.startVaultAnimation();
   }

   public void stop() {
      this.stopVaultAnimation();

      for(TextDisplay td : this.holograms) {
         if (td != null && !td.isDead()) {
            td.remove();
         }
      }

      this.holograms.clear();
   }

   public void reload() {
      this.stop();
      this.start();
   }

   public void spawnHolograms() {
      for(TextDisplay td : this.holograms) {
         if (td != null && !td.isDead()) {
            td.remove();
         }
      }

      this.holograms.clear();
      this.chestLocations.clear();
      if (this.plugin.getConfig().getBoolean("vault.hologram.enabled", true)) {
         List<String> lines = this.plugin.getConfig().getStringList("vault.hologram.lines");
         if (!lines.isEmpty()) {
            double heightOffset = this.plugin.getConfig().getDouble("vault.hologram.height_offset", 1.8);
            double spacing = this.plugin.getConfig().getDouble("vault.hologram.line_spacing", 0.28);
            List<Map<?, ?>> locList = this.plugin.getConfig().getMapList("vault.chest_locations");
            this.plugin.getLogger().info("[Hologram] Loading " + locList.size() + " chest location(s)");
            int success = 0;

            for(Map<?, ?> map : locList) {
               try {
                  String worldName = String.valueOf(map.get("world"));
                  World world = Bukkit.getWorld(worldName);
                  if (world == null) {
                     this.plugin.getLogger().warning("[Hologram] World not loaded: '" + worldName + "'");
                  } else {
                     double x = this.toDouble(map.get("x"));
                     double y = this.toDouble(map.get("y"));
                     double z = this.toDouble(map.get("z"));
                     double cx = Math.floor(x) + (double)0.5F;
                     double cz = Math.floor(z) + (double)0.5F;
                     Location base = new Location(world, cx, y, cz);
                     this.chestLocations.add(base);

                     for(int i = 0; i < lines.size(); ++i) {
                        String line = (String)lines.get(i);
                        if (line == null) {
                           line = "";
                        }

                        String colored = ChatColor.translateAlternateColorCodes('&', line);
                        if (!colored.trim().isEmpty()) {
                           Location lineLoc = base.clone().add((double)0.0F, heightOffset - (double)i * spacing, (double)0.0F);
                           TextDisplay td = (TextDisplay)world.spawn(lineLoc, TextDisplay.class, (display) -> {
                              display.text(LegacyComponentSerializer.legacySection().deserialize(colored));
                              display.setBillboard(Billboard.CENTER);
                              display.setSeeThrough(false);
                              display.setShadowed(true);
                              display.setDefaultBackground(false);
                              display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                              display.setPersistent(true);
                              display.setInvulnerable(true);
                              display.setGravity(false);
                              display.setViewRange(1.0F);
                           });
                           this.holograms.add(td);
                           ++success;
                        }
                     }
                  }
               } catch (Exception e) {
                  this.plugin.getLogger().warning("[Hologram] Bad entry: " + e.getMessage());
               }
            }

            this.plugin.getLogger().info("[Hologram] ✓ Spawned " + success + " display(s) at " + this.chestLocations.size() + " chest(s)");
         }
      }
   }

   private double toDouble(Object o) {
      if (o == null) {
         return (double)0.0F;
      } else if (o instanceof Number) {
         return ((Number)o).doubleValue();
      } else {
         try {
            return Double.parseDouble(String.valueOf(o));
         } catch (Exception var3) {
            return (double)0.0F;
         }
      }
   }

   private void startVaultAnimation() {
      this.stopVaultAnimation();
      if (this.plugin.getConfig().getBoolean("vault.animation.enabled", true)) {
         if (this.chestLocations.isEmpty()) {
            Bukkit.getScheduler().runTaskLater(this.plugin, this::startVaultAnimation, 20L);
         } else {
            int taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
               ++this.globalTick;

               for(Location chest : this.chestLocations) {
                  this.tickChest(chest);
               }

            }, 0L, 1L).getTaskId();
            this.animationTaskIds.add(taskId);
            this.plugin.getLogger().info("[Hologram] Animation started for " + this.chestLocations.size() + " chest(s) (per-player particles)");
         }
      }
   }

   private void tickChest(Location chest) {
      if (chest.getWorld() != null) {
         double radius = this.plugin.getConfig().getDouble("vault.animation.check_radius", (double)10.0F);
         double radiusSq = radius * radius;
         Map<String, List<Player>> tierGroups = new HashMap();

         for(Player p : chest.getWorld().getPlayers()) {
            if (!(p.getLocation().distanceSquared(chest) > radiusSq)) {
               long boxes = (long)this.plugin.getData().getTotalBoxCount(p.getUniqueId());
               String tier = this.determineTier(boxes);
               ((List)tierGroups.computeIfAbsent(tier, (k) -> new ArrayList())).add(p);
            }
         }

         if (!tierGroups.isEmpty()) {
            String maxTier = null;
            int maxRank = -1;

            for(String tier : tierGroups.keySet()) {
               int rank = this.getTierRank(tier);
               if (rank > maxRank) {
                  maxRank = rank;
                  maxTier = tier;
               }
            }

            ChestState state = (ChestState)this.chestStates.computeIfAbsent(chest, ChestState::new);
            if (maxTier != null && !maxTier.equals(state.currentTier)) {
               state.currentTier = maxTier;
            }

            for(Map.Entry<String, List<Player>> entry : tierGroups.entrySet()) {
               String tier = (String)entry.getKey();
               List<Player> players = (List)entry.getValue();
               ConfigurationSection tierSec = this.plugin.getConfig().getConfigurationSection("vault.animation.tiers." + tier);
               if (tierSec != null) {
                  List<Map<?, ?>> layers = tierSec.getMapList("layers");
                  if (!layers.isEmpty()) {
                     for(Map<?, ?> layer : layers) {
                        try {
                           int interval = ((Number)layer.get("interval_ticks")).intValue();
                           if (interval < 1) {
                              interval = 1;
                           }

                           if (this.globalTick % interval == 0) {
                              String type = String.valueOf(layer.get("type")).toUpperCase();
                              String particleName = String.valueOf(layer.get("particle"));
                              int count = ((Number)layer.get("count")).intValue();
                              double lRadius = ((Number)layer.get("radius")).doubleValue();
                              double height = ((Number)layer.get("height")).doubleValue();
                              String colorHex = layer.containsKey("color") ? String.valueOf(layer.get("color")) : null;
                              float size = layer.containsKey("size") ? ((Number)layer.get("size")).floatValue() : 1.0F;
                              Particle particle = this.resolveParticle(particleName);
                              if (particle != null) {
                                 Location center = chest.clone().add((double)0.0F, (double)0.5F, (double)0.0F);

                                 for(Player p : players) {
                                    this.spawnPatternForPlayer(p, center, type, particle, count, lRadius, height, colorHex, size);
                                 }
                              }
                           }
                        } catch (Exception e) {
                           this.plugin.getLogger().warning("[Hologram] Layer error: " + e.getMessage());
                        }
                     }
                  }
               }
            }

         }
      }
   }

   private int getTierRank(String tier) {
      ConfigurationSection tiers = this.plugin.getConfig().getConfigurationSection("vault.animation.tiers");
      if (tiers == null) {
         return 0;
      } else {
         int rank = 0;

         for(String key : tiers.getKeys(false)) {
            long min = tiers.getLong(key + ".min_boxes", 0L);
            rank = (int)Math.max((long)rank, min);
            if (key.equals(tier)) {
               return rank;
            }
         }

         return 0;
      }
   }

   private String determineTier(long boxes) {
      ConfigurationSection tiers = this.plugin.getConfig().getConfigurationSection("vault.animation.tiers");
      if (tiers == null) {
         return "default";
      } else {
         for(String tierKey : tiers.getKeys(false)) {
            long min = tiers.getLong(tierKey + ".min_boxes", 0L);
            long max = tiers.getLong(tierKey + ".max_boxes", Long.MAX_VALUE);
            if (boxes >= min && boxes <= max) {
               return tierKey;
            }
         }

         return "default";
      }
   }

   private void stopVaultAnimation() {
      for(int id : this.animationTaskIds) {
         try {
            Bukkit.getScheduler().cancelTask(id);
         } catch (Exception var4) {
         }
      }

      this.animationTaskIds.clear();
      this.chestStates.clear();
      this.globalTick = 0;
   }

   private void spawnPatternForPlayer(Player p, Location center, String type, Particle particle, int count, double radius, double height, String colorHex, float size) {
      try {
         switch (type) {
            case "RING" -> this.spawnRing(p, center, particle, count, radius, height, colorHex, size);
            case "TORNADO" -> this.spawnTornado(p, center, particle, count, radius, height, colorHex, size);
            case "WAVE" -> this.spawnWave(p, center, particle, count, radius, height, colorHex, size);
            case "SPIRAL" -> this.spawnSpiral(p, center, particle, count, radius, height, colorHex, size);
            case "HELIX" -> this.spawnHelix(p, center, particle, count, radius, height, colorHex, size);
            case "BURST" -> this.spawnBurst(p, center, particle, count, radius, height, colorHex, size);
            case "BEAM" -> this.spawnBeam(p, center, particle, count, radius, height, colorHex, size);
            case "PILLAR" -> this.spawnPillar(p, center, particle, count, radius, height, colorHex, size);
            case "ORBIT" -> this.spawnOrbit(p, center, particle, count, radius, height, colorHex, size);
            case "FOUNTAIN" -> this.spawnFountain(p, center, particle, count, radius, height, colorHex, size);
            default -> this.spawnRing(p, center, particle, count, radius, height, colorHex, size);
         }
      } catch (Exception e) {
         this.plugin.getLogger().warning("[Hologram] Pattern error (" + type + "): " + e.getMessage());
      }

   }

   private void spawnP(Player player, Location loc, Particle particle, String colorHex, float size) {
      try {
         if (particle == Particle.DUST && colorHex != null && !colorHex.isEmpty()) {
            Color color = this.parseColor(colorHex);
            Particle.DustOptions dust = new Particle.DustOptions(color, size);
            player.spawnParticle(particle, loc, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F, dust);
         } else {
            player.spawnParticle(particle, loc, 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
         }
      } catch (Exception var8) {
      }

   }

   private Color parseColor(String hex) {
      try {
         String clean = hex.startsWith("#") ? hex.substring(1) : hex;
         int r = Integer.parseInt(clean.substring(0, 2), 16);
         int g = Integer.parseInt(clean.substring(2, 4), 16);
         int b = Integer.parseInt(clean.substring(4, 6), 16);
         return Color.fromRGB(r, g, b);
      } catch (Exception var6) {
         return Color.WHITE;
      }
   }

   private void spawnRing(Player p, Location c, Particle pt, int count, double r, double h, String colorHex, float size) {
      double time = (double)System.currentTimeMillis() / (double)1500.0F;

      for(int i = 0; i < count; ++i) {
         double angle = time + (Math.PI * 2D) / (double)count * (double)i;
         this.spawnP(p, c.clone().add(Math.cos(angle) * r, h, Math.sin(angle) * r), pt, colorHex, size);
      }

   }

   private void spawnTornado(Player p, Location c, Particle pt, int count, double maxR, double h, String colorHex, float size) {
      double time = (double)System.currentTimeMillis() / (double)200.0F;

      for(int i = 0; i < count; ++i) {
         double progress = (double)i / (double)count;
         double curR = maxR * progress;
         double angle = progress * Math.PI * (double)4.0F + time;
         this.spawnP(p, c.clone().add(Math.cos(angle) * curR, h * progress, Math.sin(angle) * curR), pt, colorHex, size);
      }

   }

   private void spawnWave(Player p, Location c, Particle pt, int count, double r, double h, String colorHex, float size) {
      double time = (double)System.currentTimeMillis() / (double)500.0F;

      for(int i = 0; i < count; ++i) {
         double progress = (double)i / (double)count;
         double angle = progress * Math.PI * (double)2.0F;
         double waveR = r * ((double)0.5F + (double)0.5F * Math.sin(progress * Math.PI * (double)4.0F + time));
         this.spawnP(p, c.clone().add(Math.cos(angle) * waveR, h, Math.sin(angle) * waveR), pt, colorHex, size);
      }

   }

   private void spawnSpiral(Player p, Location c, Particle pt, int count, double r, double h, String colorHex, float size) {
      double startAngle = (double)System.currentTimeMillis() / (double)200.0F;

      for(int i = 0; i < count; ++i) {
         double progress = (double)i / (double)count;
         double angle = startAngle + progress * Math.PI * (double)4.0F;
         this.spawnP(p, c.clone().add(Math.cos(angle) * r, h * progress, Math.sin(angle) * r), pt, colorHex, size);
      }

   }

   private void spawnHelix(Player p, Location c, Particle pt, int count, double r, double h, String colorHex, float size) {
      double startAngle = (double)System.currentTimeMillis() / (double)200.0F;
      int half = Math.max(1, count / 2);

      for(int i = 0; i < half; ++i) {
         double progress = (double)i / (double)half;
         double angle = startAngle + progress * Math.PI * (double)4.0F;
         double y = h * progress;
         this.spawnP(p, c.clone().add(Math.cos(angle) * r, y, Math.sin(angle) * r), pt, colorHex, size);
         this.spawnP(p, c.clone().add(Math.cos(angle + Math.PI) * r, y, Math.sin(angle + Math.PI) * r), pt, colorHex, size);
      }

   }

   private void spawnBurst(Player p, Location c, Particle pt, int count, double r, double h, String colorHex, float size) {
      for(int i = 0; i < count; ++i) {
         double theta = this.random.nextDouble() * Math.PI * (double)2.0F;
         double phi = Math.acos((double)2.0F * this.random.nextDouble() - (double)1.0F);
         double x = r * Math.sin(phi) * Math.cos(theta);
         double y = r * Math.cos(phi);
         double z = r * Math.sin(phi) * Math.sin(theta);
         this.spawnP(p, c.clone().add(x, y + h, z), pt, colorHex, size);
      }

   }

   private void spawnBeam(Player p, Location c, Particle pt, int count, double r, double h, String colorHex, float size) {
      for(int i = 0; i < count; ++i) {
         this.spawnP(p, c.clone().add((double)0.0F, (double)i / (double)count * h, (double)0.0F), pt, colorHex, size);
      }

   }

   private void spawnPillar(Player p, Location c, Particle pt, int count, double r, double h, String colorHex, float size) {
      for(int i = 0; i < count; ++i) {
         double angle = (Math.PI * 2D) / (double)count * (double)i;
         this.spawnP(p, c.clone().add(Math.cos(angle) * r, this.random.nextDouble() * h, Math.sin(angle) * r), pt, colorHex, size);
      }

   }

   private void spawnOrbit(Player p, Location c, Particle pt, int count, double r, double h, String colorHex, float size) {
      double time = (double)System.currentTimeMillis() / (double)150.0F;
      double tilt = Math.sin(time * (double)0.5F) * 0.6;

      for(int i = 0; i < count; ++i) {
         double angle = time + (Math.PI * 2D) / (double)count * (double)i;
         this.spawnP(p, c.clone().add(Math.cos(angle) * r, h + Math.sin(angle) * tilt, Math.sin(angle) * r), pt, colorHex, size);
      }

   }

   private void spawnFountain(Player p, Location c, Particle pt, int count, double r, double h, String colorHex, float size) {
      for(int i = 0; i < count; ++i) {
         double angle = this.random.nextDouble() * Math.PI * (double)2.0F;
         double y = this.random.nextDouble() * h;
         double spread = y / h * r;
         this.spawnP(p, c.clone().add(Math.cos(angle) * spread, y, Math.sin(angle) * spread), pt, colorHex, size);
      }

   }

   private Particle resolveParticle(String name) {
      if (name != null && !name.isBlank()) {
         String normalized = name.trim().toLowerCase();
         Particle p = (Particle)Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(normalized));
         return p != null ? p : (Particle)Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(normalized.replace('_', '.')));
      } else {
         return null;
      }
   }

   @EventHandler
   public void onWorldLoad(WorldLoadEvent event) {
      if (this.chestLocations.isEmpty()) {
         Bukkit.getScheduler().runTaskLater(this.plugin, this::reload, 20L);
      }

   }

   @EventHandler
   public void onInteract(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
         if (event.getClickedBlock() != null) {
            if (event.getHand() == EquipmentSlot.HAND) {
               Player p = event.getPlayer();
               Location blockLoc = event.getClickedBlock().getLocation();

               for(Location loc : this.chestLocations) {
                  if (blockLoc.getWorld().equals(loc.getWorld()) && blockLoc.getBlockX() == (int)Math.floor(loc.getX()) && blockLoc.getBlockY() == (int)Math.floor(loc.getY()) && blockLoc.getBlockZ() == (int)Math.floor(loc.getZ())) {
                     event.setCancelled(true);
                     SpinManager spin = this.plugin.getSpinManager();
                     if (spin != null && spin.isSpinning(p)) {
                        p.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lLUCKYBOX &8» &cPlease wait for the current animation to finish!"));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                        return;
                     }

                     try {
                        if (spin != null) {
                           spin.setChestOrigin(p, event.getClickedBlock().getLocation());
                        }
                     } catch (Exception e) {
                        this.plugin.getLogger().warning("[Hologram] setChestOrigin failed: " + e.getMessage());
                     }

                     this.plugin.getMenuManager().openVault(p, false);
                     return;
                  }
               }

            }
         }
      }
   }

   public void addChestAt(Location loc) {
      List<Map<?, ?>> list = new ArrayList(this.plugin.getConfig().getMapList("vault.chest_locations"));
      Map<String, Object> map = new HashMap();
      map.put("world", loc.getWorld().getName());
      map.put("x", (double)loc.getBlockX() + (double)0.5F);
      map.put("y", (double)loc.getBlockY());
      map.put("z", (double)loc.getBlockZ() + (double)0.5F);
      list.add(map);
      this.plugin.getConfig().set("vault.chest_locations", list);
      this.plugin.saveConfig();
      this.reload();
   }

   public boolean removeChestAt(Location loc) {
      List<Map<?, ?>> list = new ArrayList(this.plugin.getConfig().getMapList("vault.chest_locations"));
      boolean removed = false;
      Iterator<Map<?, ?>> it = list.iterator();

      while(it.hasNext()) {
         Map<?, ?> m = (Map)it.next();

         try {
            String w = String.valueOf(m.get("world"));
            int x = (int)this.toDouble(m.get("x"));
            int y = (int)this.toDouble(m.get("y"));
            int z = (int)this.toDouble(m.get("z"));
            if (w.equals(loc.getWorld().getName()) && x == loc.getBlockX() && y == loc.getBlockY() && z == loc.getBlockZ()) {
               it.remove();
               removed = true;
            }
         } catch (Exception var10) {
         }
      }

      if (removed) {
         this.plugin.getConfig().set("vault.chest_locations", list);
         this.plugin.saveConfig();
         this.reload();
      }

      return removed;
   }

   public List<Location> getChestLocations() {
      return new ArrayList(this.chestLocations);
   }

   private static class ChestState {
      final Location chest;
      String currentTier = "";

      ChestState(Location chest) {
         this.chest = chest;
      }
   }
}
