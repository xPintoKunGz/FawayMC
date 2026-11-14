package org.kcm.bloomery.fawaymc.luckyblock;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class SpinManager implements Listener {
   private final Global plugin;
   private final Random random = new Random();
   private final Map<UUID, SpinData> activeSpins = new HashMap<>();
   private final Map<UUID, Location> chestOrigins = new HashMap<>();
   private final Map<UUID, Long> chestOriginTimestamps = new HashMap<>();
   private static final long CHEST_ORIGIN_TTL_MS = 60000L;
   private final Set<String> debugLogged = new HashSet<>();

   public SpinManager(Global plugin) {
      this.plugin = plugin;
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public boolean isSpinning(Player player) {
      return this.activeSpins.containsKey(player.getUniqueId());
   }

   public void stop() {
      for (SpinData spin : new ArrayList<>(this.activeSpins.values())) {
         this.cleanupSpin(spin);
      }

      this.activeSpins.clear();
   }

   public void setChestOrigin(Player player, Location chestBlockLoc) {
      if (player != null && chestBlockLoc != null) {
         if (chestBlockLoc.getWorld() != null) {
            Location centered = chestBlockLoc.clone();
            centered.setX(Math.floor(chestBlockLoc.getX()) + (double)0.5F);
            centered.setY(Math.floor(chestBlockLoc.getY()) + (double)0.5F);
            centered.setZ(Math.floor(chestBlockLoc.getZ()) + (double)0.5F);
            centered.setYaw(0.0F);
            centered.setPitch(0.0F);
            this.chestOrigins.put(player.getUniqueId(), centered);
            this.chestOriginTimestamps.put(player.getUniqueId(), System.currentTimeMillis());
         }
      }
   }

   private Location consumeChestOrigin(Player player) {
      Location origin = this.chestOrigins.remove(player.getUniqueId());
      Long ts = this.chestOriginTimestamps.remove(player.getUniqueId());
      if (origin != null && ts != null) {
         if (System.currentTimeMillis() - ts > 60000L) {
            return null;
         } else {
            return origin.getWorld() != player.getWorld() ? null : origin;
         }
      } else {
         return null;
      }
   }

   private Location findNearbyVaultChest(Player player) {
      try {
         HologramManager holo = this.plugin.getHologramManager();
         if (holo == null) {
            return null;
         } else {
            List<Location> chests = holo.getChestLocations();
            if (chests != null && !chests.isEmpty()) {
               double maxDist = this.plugin.getConfig().getDouble("opening.physical.vault_chest_search_radius", (double)15.0F);
               double maxDistSq = maxDist * maxDist;
               Location playerLoc = player.getLocation();
               Location nearest = null;
               double bestDistSq = Double.MAX_VALUE;

               for (Location chest : chests) {
                  if (chest.getWorld() == player.getWorld()) {
                     double distSq = chest.distanceSquared(playerLoc);
                     if (distSq < bestDistSq && distSq <= maxDistSq) {
                        bestDistSq = distSq;
                        nearest = chest;
                     }
                  }
               }

               if (nearest == null) {
                  return null;
               } else {
                  Location centered = nearest.clone();
                  centered.setX(Math.floor(nearest.getX()) + (double)0.5F);
                  centered.setY(Math.floor(nearest.getY()) + (double)0.5F);
                  centered.setZ(Math.floor(nearest.getZ()) + (double)0.5F);
                  centered.setYaw(0.0F);
                  centered.setPitch(0.0F);
                  return centered;
               }
            } else {
               return null;
            }
         }
      } catch (Exception e) {
         this.plugin.getLogger().warning("[SpinManager] findNearbyVaultChest failed: " + e.getMessage());
         return null;
      }
   }

   public void openSpin(Player player, String boxTypeId) {
      this.openSpin(player, boxTypeId, 1);
   }

   public void openSpin(Player player, String boxTypeId, int amount) {
      if (amount < 1) {
         amount = 1;
      }

      if (this.activeSpins.containsKey(player.getUniqueId())) {
         player.sendMessage(String.valueOf(ChatColor.RED) + "You are already opening a box!");
      } else {
         ConfigurationSection boxCfg = this.plugin.getConfig().getConfigurationSection("boxes." + boxTypeId);
         if (boxCfg != null) {
            String lootTable = boxCfg.getString("loot_table", "common");
            List<String> rewards = this.plugin.getConfig().getStringList("loot_tables." + lootTable + ".rewards");
            if (rewards.isEmpty()) {
               player.sendMessage(String.valueOf(ChatColor.RED) + "This box has no loot configured!");
            } else {
               int available = this.plugin.getData().getBoxCount(player.getUniqueId(), boxTypeId);
               if (available < amount) {
                  String boxName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', boxCfg.getString("display_name", boxTypeId)));
                  player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lLUCKYBLOCK &8» &cYou need &e" + amount + " boxes &cbut only have &e" + available + " &cof " + boxName + "!"));
                  player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
               } else if (!this.plugin.getData().removeBox(player.getUniqueId(), boxTypeId, amount)) {
                  String boxName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', boxCfg.getString("display_name", boxTypeId)));
                  player.sendMessage(this.plugin.msg("no_boxes").replace("{type}", boxName));
               } else {
                  this.triggerBoxOpenAdvancements(player, amount);
                  List<String> picked = new ArrayList<>();

                  for (int i = 0; i < amount; ++i) {
                     picked.add(this.pickWeighted(rewards));
                  }

                  String finalReward = picked.get(0);
                  double distance = this.plugin.getConfig().getDouble("opening.physical.box_distance", (double)2.0F);
                  double height = this.plugin.getConfig().getDouble("opening.physical.box_height", (double)2.5F);
                  float boxScale = (float)this.plugin.getConfig().getDouble("opening.physical.box_scale", 0.6);
                  float itemScale = (float)this.plugin.getConfig().getDouble("opening.physical.item_scale", 0.4);
                  Location chestOrigin = this.consumeChestOrigin(player);
                  if (chestOrigin == null) {
                     chestOrigin = this.findNearbyVaultChest(player);
                  }

                  Location boxLoc;
                  Location boxStartLoc;
                  if (chestOrigin != null) {
                     double boxH = this.plugin.getConfig().getDouble("opening.physical.box_height_above_chest", (double)2.0F);
                     boxLoc = chestOrigin.clone().add((double)0.0F, boxH, (double)0.0F);
                     boxStartLoc = chestOrigin.clone().add((double)0.0F, (double)0.5F, (double)0.0F);
                  } else {
                     Location playerLoc = player.getLocation();
                     Vector dir = playerLoc.getDirection().setY(0);
                     if (dir.lengthSquared() < 1.0E-4) {
                        dir = new Vector(0, 0, 1);
                     }

                     dir.normalize();
                     double targetX = playerLoc.getX() + dir.getX() * distance;
                     double targetZ = playerLoc.getZ() + dir.getZ() * distance;
                     int bx = (int)Math.floor(targetX);
                     int bz = (int)Math.floor(targetZ);
                     double centerX = (double)bx + (double)0.5F;
                     double centerZ = (double)bz + (double)0.5F;
                     World w = player.getWorld();

                     double boxY;
                     try {
                        int groundY = w.getHighestBlockYAt(bx, bz);
                        if (Math.abs(groundY - playerLoc.getBlockY()) <= 8) {
                           boxY = (double)groundY + (double)1.0F + height;
                        } else {
                           boxY = playerLoc.getY() + height;
                        }
                     } catch (Exception var39) {
                        boxY = playerLoc.getY() + height;
                     }

                     boxY = Math.max(boxY, playerLoc.getY() + (double)2.0F);
                     boxLoc = new Location(w, centerX, boxY, centerZ, 0.0F, 0.0F);
                     boxStartLoc = boxLoc.clone().add((double)0.0F, (double)-1.5F, (double)0.0F);
                  }

                  SpinData spin = new SpinData(boxTypeId, finalReward, rewards, boxLoc);
                  spin.boxScale = boxScale;
                  spin.itemScale = itemScale;
                  spin.chestOrigin = chestOrigin;
                  spin.boxStartLoc = boxStartLoc;
                  spin.amount = amount;
                  spin.pickedRewards.addAll(picked);
                  this.activeSpins.put(player.getUniqueId(), spin);
                  World world = player.getWorld();
                  if (chestOrigin != null) {
                     this.playChestOpenSounds(world, chestOrigin);

                     try {
                        ChestPacketAnimator anim = this.plugin.getChestPacketAnimator();
                        if (anim != null && anim.isAvailable()) {
                           Material chestMat = chestOrigin.getBlock().getType();
                           anim.open(chestOrigin, chestMat);
                        }
                     } catch (Exception e) {
                        this.plugin.getLogger().warning("[SpinManager] Chest animation open failed: " + e.getMessage());
                     }
                  }

                  Material boxMat = Material.matchMaterial(boxCfg.getString("material", "CHEST"));
                  if (boxMat == null || !boxMat.isItem()) {
                     boxMat = Material.CHEST;
                  }

                  ItemStack boxItem = new ItemStack(boxMat);

                  try {
                     spin.boxDisplay = world.spawn(boxStartLoc, ItemDisplay.class, (d) -> {
                        d.setItemStack(boxItem);
                        d.setBillboard(Billboard.FIXED);
                        d.setShadowRadius(0.3F * boxScale);
                        d.setShadowStrength(1.0F);
                        d.setInterpolationDuration(0);
                        d.setInterpolationDelay(0);
                        d.setRotation(0.0F, 0.0F);
                        d.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(0.01F, 0.01F, 0.01F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
                     });
                  } catch (Exception e) {
                     this.plugin.getLogger().warning("[SpinManager] Failed to spawn box display: " + e.getMessage());
                     this.cleanupSpin(spin);
                     this.activeSpins.remove(player.getUniqueId());
                     return;
                  }

                  if (this.plugin.getConfig().getBoolean("opening.physical.box_hologram.enabled", true)) {
                     double holoH = this.plugin.getConfig().getDouble("opening.physical.box_hologram.height", 0.9);
                     String boxDisplayName = boxCfg.getString("display_name", boxTypeId);
                     String fmt = this.plugin.getConfig().getString("opening.physical.box_hologram.text", "&e&l✦ &f{box_name} &e&l✦");
                     String holoText = fmt.replace("{box_name}", boxDisplayName);
                     if (amount > 1) {
                        holoText = holoText + ChatColor.translateAlternateColorCodes('&', " &7(&e" + amount + "x&7)");
                     }

                     try {
                        spin.boxHoloHeight = holoH;
                        spin.boxHologram = this.createHologram(world, boxStartLoc.clone().add((double)0.0F, holoH, (double)0.0F), holoText, 0.01F);
                     } catch (Exception var36) {
                     }
                  }

                  try {
                     world.playSound(boxLoc, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
                  } catch (Exception var35) {
                  }

                  this.startSpin(player, spin);
               }
            }
         }
      }
   }

   public void openGuiSpin(Player player, String boxTypeId) {
      this.openGuiSpin(player, boxTypeId, 1);
   }

   public void openGuiSpin(Player player, String boxTypeId, int amount) {
      if (amount < 1) {
         amount = 1;
      }

      if (this.activeSpins.containsKey(player.getUniqueId())) {
         player.sendMessage(String.valueOf(ChatColor.RED) + "You are already opening a box!");
      } else {
         ConfigurationSection boxCfg = this.plugin.getConfig().getConfigurationSection("boxes." + boxTypeId);
         if (boxCfg != null) {
            String lootTable = boxCfg.getString("loot_table", "common");
            List<String> rewards = this.plugin.getConfig().getStringList("loot_tables." + lootTable + ".rewards");
            if (rewards.isEmpty()) {
               player.sendMessage(String.valueOf(ChatColor.RED) + "This box has no loot configured!");
            } else {
               int available = this.plugin.getData().getBoxCount(player.getUniqueId(), boxTypeId);
               if (available < amount) {
                  String boxName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', boxCfg.getString("display_name", boxTypeId)));
                  player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lLUCKYBLOCK &8» &cYou need &e" + amount + " boxes &cbut only have &e" + available + " &cof " + boxName + "!"));
               } else if (!this.plugin.getData().removeBox(player.getUniqueId(), boxTypeId, amount)) {
                  String boxName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', boxCfg.getString("display_name", boxTypeId)));
                  player.sendMessage(this.plugin.msg("no_boxes").replace("{type}", boxName));
               } else {
                  this.triggerBoxOpenAdvancements(player, amount);
                  if (amount > 1) {
                     List<String> picked = new ArrayList<>();

                     for (int i = 0; i < amount; ++i) {
                        picked.add(this.pickWeighted(rewards));
                     }

                     this.openMultiGuiSpin(player, boxTypeId, picked, rewards);
                  } else {
                     String finalReward = this.pickWeighted(rewards);
                     String title = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("opening.gui_title", "&8&l✦ &e&lOpening... &8&l✦"));
                     int size = this.plugin.getConfig().getInt("opening.gui_size", 27);
                     if (size < 9) {
                        size = 9;
                     }

                     if (size > 54) {
                        size = 54;
                     }

                     if (size % 9 != 0) {
                        size = 27;
                     }

                     SpinGuiHolder holder = new SpinGuiHolder();
                     Inventory inv = Bukkit.createInventory(holder, size, title);
                     holder.setInventory(inv);
                     Material borderMat = Material.matchMaterial(this.plugin.getConfig().getString("opening.border_material", "BLACK_STAINED_GLASS_PANE"));
                     if (borderMat == null) {
                        borderMat = Material.BLACK_STAINED_GLASS_PANE;
                     }

                     ItemStack border = new ItemStack(borderMat);
                     ItemMeta bm = border.getItemMeta();
                     if (bm != null) {
                        bm.setDisplayName(" ");
                        border.setItemMeta(bm);
                     }

                     for (int i = 0; i < size; ++i) {
                        inv.setItem(i, border);
                     }

                     List<Integer> rawSlots = this.plugin.getConfig().getIntegerList("opening.spin_slots");
                     if (rawSlots.isEmpty()) {
                        rawSlots = List.of(10, 11, 12, 13, 14, 15, 16);
                     }

                     List<Integer> validSlots = new ArrayList<>();

                     for (int s : rawSlots) {
                        if (s >= 0 && s < size) {
                           validSlots.add(s);
                        }
                     }

                     if (validSlots.isEmpty()) {
                        validSlots.add(size / 2);
                     }

                     for (int s : validSlots) {
                        inv.setItem(s, null);
                     }

                     // ✅ FIX: ใช้ final int เพื่อให้ lambda capture ได้
                     // เดิม: int resultSlot = ...; if (cond) resultSlot = ...; → ไม่ effectively final
                     // ใหม่: คำนวณใน final int ตั้งแต่ต้น → capture ได้
                     int resultSlotCfg = this.plugin.getConfig().getInt("opening.result_slot", 13);
                     final int resultSlot = (resultSlotCfg < 0 || resultSlotCfg >= size)
                             ? validSlots.get(validSlots.size() / 2)
                             : resultSlotCfg;

                     for (int s : validSlots) {
                        String randomId = rewards.get(this.random.nextInt(rewards.size()));
                        inv.setItem(s, this.createRewardItem(randomId));
                     }

                     player.openInventory(inv);

                     try {
                        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
                     } catch (Exception var30) {
                     }

                     SpinData spin = new SpinData(boxTypeId, finalReward, rewards, player.getLocation());
                     this.activeSpins.put(player.getUniqueId(), spin);
                     int duration = Math.max(1, this.plugin.getConfig().getInt("opening.spin_duration_ticks", 120));
                     int update = Math.max(1, this.plugin.getConfig().getInt("opening.spin_update_ticks", 2));
                     int rewardDelay = Math.max(1, this.plugin.getConfig().getInt("opening.reward_delay_ticks", 40));
                     List<Integer> SLOTS = new ArrayList<>(validSlots);
                     int[] tick = new int[]{0};
                     spin.taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                        if (!player.isOnline()) {
                           if (spin.taskId != -1) {
                              try {
                                 Bukkit.getScheduler().cancelTask(spin.taskId);
                              } catch (Exception var16) {
                              }

                              spin.taskId = -1;
                           }

                           if (!spin.rewardGiven) {
                              spin.rewardGiven = true;

                              try {
                                 this.giveReward(player, spin.boxTypeId, spin.finalReward);
                              } catch (Exception var15) {
                              }
                           }

                           if (!spin.stateCleaned) {
                              spin.stateCleaned = true;
                              this.activeSpins.remove(player.getUniqueId());
                           }

                        } else {
                           tick[0] += update;
                           if (tick[0] >= duration) {
                              if (spin.taskId != -1) {
                                 try {
                                    Bukkit.getScheduler().cancelTask(spin.taskId);
                                 } catch (Exception var18) {
                                 }

                                 spin.taskId = -1;
                              }

                              for (int s : SLOTS) {
                                 inv.setItem(s, null);
                              }

                              if (resultSlot >= 0 && resultSlot < inv.getSize()) {
                                 inv.setItem(resultSlot, this.createRewardItem(finalReward));
                              }

                              try {
                                 player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                                 player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
                              } catch (Exception var17) {
                              }

                              Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                                 if (!spin.rewardGiven) {
                                    spin.rewardGiven = true;

                                    try {
                                       this.sendRewardNotification(player, spin.boxTypeId, spin.finalReward);
                                       this.giveReward(player, spin.boxTypeId, spin.finalReward);
                                    } catch (Exception var5) {
                                    }
                                 }

                                 if (!spin.stateCleaned) {
                                    spin.stateCleaned = true;
                                    this.activeSpins.remove(player.getUniqueId());
                                 }

                                 if (player.isOnline()) {
                                    try {
                                       player.closeInventory();
                                    } catch (Exception var4) {
                                    }
                                 }

                              }, (long)rewardDelay);
                           } else {
                              for (int i = 0; i < SLOTS.size() - 1; ++i) {
                                 inv.setItem(SLOTS.get(i), inv.getItem(SLOTS.get(i + 1)));
                              }

                              int lastSlot = SLOTS.get(SLOTS.size() - 1);
                              String randomId = rewards.get(this.random.nextInt(rewards.size()));
                              inv.setItem(lastSlot, this.createRewardItem(randomId));

                              try {
                                 player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3F, 1.8F);
                              } catch (Exception var19) {
                              }

                           }
                        }
                     }, 0L, (long)update).getTaskId();
                  }
               }
            }
         }
      }
   }

   private void openMultiGuiSpin(Player player, String boxTypeId, List<String> picked, List<String> pool) {
      int size = 54;
      SpinGuiHolder holder = new SpinGuiHolder();
      String titleRaw = this.plugin.getConfig().getString("opening.multi_title", "&8&l✦ &e&lOpening &f{amount}x &e&lBoxes &8&l✦");
      String title = ChatColor.translateAlternateColorCodes('&', titleRaw.replace("{amount}", String.valueOf(picked.size())));
      Inventory inv = Bukkit.createInventory(holder, size, title);
      holder.setInventory(inv);
      Material borderMat = Material.matchMaterial(this.plugin.getConfig().getString("opening.border_material", "BLACK_STAINED_GLASS_PANE"));
      if (borderMat == null) {
         borderMat = Material.BLACK_STAINED_GLASS_PANE;
      }

      ItemStack border = new ItemStack(borderMat);
      ItemMeta bm = border.getItemMeta();
      if (bm != null) {
         bm.setDisplayName(" ");
         border.setItemMeta(bm);
      }

      for (int i = 0; i < size; ++i) {
         inv.setItem(i, border);
      }

      List<Integer> avail = new ArrayList<>();

      for (int row = 1; row <= 4; ++row) {
         for (int col = 1; col <= 7; ++col) {
            avail.add(row * 9 + col);
         }
      }

      for (int s : avail) {
         String randomId = pool.get(this.random.nextInt(pool.size()));
         inv.setItem(s, this.createRewardItem(randomId));
      }

      player.openInventory(inv);

      try {
         player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
      } catch (Exception var24) {
      }

      SpinData spin = new SpinData(boxTypeId, picked.isEmpty() ? "unknown" : picked.get(0), pool, player.getLocation());
      spin.pickedRewards.addAll(picked);
      spin.amount = picked.size();
      this.activeSpins.put(player.getUniqueId(), spin);
      int duration = Math.max(1, this.plugin.getConfig().getInt("opening.multi_spin_duration_ticks", 80));
      int update = Math.max(1, this.plugin.getConfig().getInt("opening.spin_update_ticks", 2));
      int revealHold = Math.max(20, this.plugin.getConfig().getInt("opening.multi_reveal_hold_ticks", 80));
      List<Integer> SLOTS = new ArrayList<>(avail);
      List<String> PICKED = new ArrayList<>(picked);
      int[] tick = new int[]{0};
      spin.taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (!player.isOnline()) {
            if (spin.taskId != -1) {
               try {
                  Bukkit.getScheduler().cancelTask(spin.taskId);
               } catch (Exception var14) {
               }

               spin.taskId = -1;
            }

            if (!spin.rewardGiven) {
               spin.rewardGiven = true;

               try {
                  for (String rId : PICKED) {
                     this.giveReward(player, spin.boxTypeId, rId);
                  }
               } catch (Exception var19) {
               }
            }

            if (!spin.stateCleaned) {
               spin.stateCleaned = true;
               this.activeSpins.remove(player.getUniqueId());
            }

         } else {
            tick[0] += update;
            if (tick[0] < duration) {
               for (int i = 0; i < SLOTS.size() - 1; ++i) {
                  inv.setItem(SLOTS.get(i), inv.getItem(SLOTS.get(i + 1)));
               }

               int lastSlot = SLOTS.get(SLOTS.size() - 1);
               String randomId = pool.get(this.random.nextInt(pool.size()));
               inv.setItem(lastSlot, this.createRewardItem(randomId));

               try {
                  player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.3F, 1.8F);
               } catch (Exception var15) {
               }

            } else if (tick[0] == duration) {
               for (int s : SLOTS) {
                  inv.setItem(s, null);
               }

               int idx = 0;

               for (int s : SLOTS) {
                  if (idx >= PICKED.size()) {
                     break;
                  }

                  inv.setItem(s, this.createRewardItem(PICKED.get(idx)));
                  ++idx;
               }

               try {
                  player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                  player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
               } catch (Exception var16) {
               }

            } else {
               if (tick[0] >= duration + revealHold) {
                  if (spin.taskId != -1) {
                     try {
                        Bukkit.getScheduler().cancelTask(spin.taskId);
                     } catch (Exception var18) {
                     }

                     spin.taskId = -1;
                  }

                  if (!spin.rewardGiven) {
                     spin.rewardGiven = true;

                     try {
                        for (String rId : PICKED) {
                           this.giveReward(player, spin.boxTypeId, rId);
                        }

                        this.sendMultiRewardNotification(player, spin.boxTypeId, PICKED);
                     } catch (Exception var20) {
                     }
                  }

                  if (!spin.stateCleaned) {
                     spin.stateCleaned = true;
                     this.activeSpins.remove(player.getUniqueId());
                  }

                  if (player.isOnline()) {
                     try {
                        player.closeInventory();
                     } catch (Exception var17) {
                     }
                  }
               }

            }
         }
      }, 0L, (long)update).getTaskId();
   }

   private void sendMultiRewardNotification(Player player, String boxTypeId, List<String> rewards) {
      if (player != null && player.isOnline()) {
         String boxName = boxTypeId != null ? boxTypeId : "Box";

         try {
            String cfg = this.plugin.getConfig().getString("boxes." + boxTypeId + ".display_name");
            if (cfg != null && !cfg.isEmpty()) {
               boxName = cfg;
            }
         } catch (Exception var13) {
         }

         Map<String, Integer> counts = new LinkedHashMap<>();

         for (String r : rewards) {
            String dispName = this.resolveRewardDisplayName(r);
            String colored = ChatColor.translateAlternateColorCodes('&', dispName);
            counts.merge(colored, 1, Integer::sum);
         }

         try {
            String raw = this.plugin.msgRaw("box_opened_multi");
            if (raw == null || raw.isEmpty()) {
               raw = "&aOpened &e{amount}x {type}&a and got:";
            }

            String msg = raw.replace("{amount}", String.valueOf(rewards.size())).replace("{type}", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', boxName)));
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));

            for (Map.Entry<String, Integer> e : counts.entrySet()) {
               String var10002 = e.getKey();
               player.sendMessage(ChatColor.translateAlternateColorCodes('&', " &8▪ &f" + var10002 + " &7x&e" + e.getValue()));
            }
         } catch (Exception var14) {
         }

         try {
            LegacyComponentSerializer var10001 = LegacyComponentSerializer.legacyAmpersand();
            int var22 = rewards.size();
            player.sendActionBar(var10001.deserialize("&a&l+ " + var22 + " rewards &7(" + boxName + "&7)"));
         } catch (Exception var12) {
         }

         try {
            Component titleComp = ((TextComponent)Component.text("+" + rewards.size() + " REWARDS").color(NamedTextColor.LIGHT_PURPLE)).decoration(TextDecoration.BOLD, true);
            Component subComp = LegacyComponentSerializer.legacyAmpersand().deserialize(boxName);
            player.showTitle(Title.title(titleComp, subComp, Times.times(Duration.ofMillis(300L), Duration.ofMillis(1800L), Duration.ofMillis(500L))));
         } catch (Exception var11) {
         }

         try {
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0F, 1.0F);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.4F);
         } catch (Exception var10) {
         }

      }
   }

   private TextDisplay createHologram(World world, Location loc, String text, float scale) {
      try {
         return world.spawn(loc, TextDisplay.class, (td) -> {
            td.text(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
            td.setBillboard(Billboard.CENTER);
            td.setSeeThrough(false);
            td.setShadowed(true);
            td.setDefaultBackground(false);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setPersistent(false);
            td.setInvulnerable(true);
            td.setGravity(false);
            td.setViewRange(1.0F);
            td.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(scale, scale, scale), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
         });
      } catch (Exception var6) {
         return null;
      }
   }

   private void updateHoloText(TextDisplay td, String text) {
      if (td != null && td.isValid()) {
         try {
            td.text(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
         } catch (Exception var4) {
         }
      }

   }

   private void removeBoxHologram(SpinData spin) {
      if (spin.boxHologram != null) {
         try {
            if (spin.boxHologram.isValid()) {
               spin.boxHologram.remove();
            }
         } catch (Exception var3) {
         }

         spin.boxHologram = null;
      }

   }

   private void removeAllItemHolograms(SpinData spin) {
      for (TextDisplay h : spin.itemHolograms) {
         try {
            if (h != null && h.isValid()) {
               h.remove();
            }
         } catch (Exception var5) {
         }
      }

      spin.itemHolograms.clear();
   }

   private Sound getChestOpenSound(Material mat) {
      if (mat == Material.ENDER_CHEST) {
         return Sound.BLOCK_ENDER_CHEST_OPEN;
      } else if (mat == Material.BARREL) {
         return Sound.BLOCK_BARREL_OPEN;
      } else {
         return mat != null && mat.name().endsWith("_SHULKER_BOX") ? Sound.BLOCK_SHULKER_BOX_OPEN : Sound.BLOCK_CHEST_OPEN;
      }
   }

   private void playChestOpenSounds(World world, Location origin) {
      try {
         Material mat = origin.getBlock().getType();
         Sound openSound;
         if (mat == Material.ENDER_CHEST) {
            openSound = Sound.BLOCK_ENDER_CHEST_OPEN;
         } else if (mat == Material.BARREL) {
            openSound = Sound.BLOCK_BARREL_OPEN;
         } else if (mat.name().endsWith("_SHULKER_BOX")) {
            openSound = Sound.BLOCK_SHULKER_BOX_OPEN;
         } else {
            openSound = Sound.BLOCK_CHEST_OPEN;
         }

         world.playSound(origin, openSound, 1.0F, 0.8F);
         world.playSound(origin, Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 0.6F);
      } catch (Exception var5) {
      }

   }

   private Sound getChestCloseSound(Material mat) {
      if (mat == Material.ENDER_CHEST) {
         return Sound.BLOCK_ENDER_CHEST_CLOSE;
      } else if (mat == Material.BARREL) {
         return Sound.BLOCK_BARREL_CLOSE;
      } else {
         return mat != null && mat.name().endsWith("_SHULKER_BOX") ? Sound.BLOCK_SHULKER_BOX_CLOSE : Sound.BLOCK_CHEST_CLOSE;
      }
   }

   private void startSpin(Player player, SpinData spin) {
      int duration = Math.max(1, this.plugin.getConfig().getInt("opening.physical.spin_duration_ticks", 200));
      int flyDuration = Math.max(1, this.plugin.getConfig().getInt("opening.physical.fly_duration_ticks", 40));
      int freezePause = Math.max(1, this.plugin.getConfig().getInt("opening.physical.freeze_pause_ticks", 20));
      int moveCenter = Math.max(1, this.plugin.getConfig().getInt("opening.physical.move_to_center_ticks", 25));
      int centerPause = Math.max(1, this.plugin.getConfig().getInt("opening.physical.center_pause_ticks", 20));
      double radius = this.plugin.getConfig().getDouble("opening.physical.spin_radius", (double)1.5F);
      double heightOffset = this.plugin.getConfig().getDouble("opening.physical.spin_height_offset", 0.3);
      int itemCount = this.plugin.getConfig().getInt("opening.physical.item_count", 8);
      double rotationSpeed = this.plugin.getConfig().getDouble("opening.physical.rotation_speed", 0.08);
      float boxScale = spin.boxScale;
      float itemScale = spin.itemScale;
      int T_START_CENTER = duration + freezePause;
      int T_AT_CENTER = T_START_CENTER + moveCenter;
      int T_FLY_START = T_AT_CENTER + centerPause;
      int T_IMPACT = T_FLY_START + flyDuration;
      boolean itemHoloEnabled = this.plugin.getConfig().getBoolean("opening.physical.item_hologram.enabled", true);
      double itemHoloHeight = this.plugin.getConfig().getDouble("opening.physical.item_hologram.height", 0.55);
      float itemHoloScale = (float)this.plugin.getConfig().getDouble("opening.physical.item_hologram.scale", 0.8);
      World world = player.getWorld();

      for (int i = 0; i < itemCount; ++i) {
         String rewardId = spin.rewards.get(this.random.nextInt(spin.rewards.size()));
         ItemStack item = this.createRewardItem(rewardId);
         double angle = (Math.PI * 2D) / (double)itemCount * (double)i;
         Location targetLoc = spin.boxLocation.clone().add(Math.cos(angle) * radius, heightOffset, Math.sin(angle) * radius);
         Location spawnLoc = spin.boxLocation.clone().add((double)0.0F, 0.1, (double)0.0F);

         try {
            ItemDisplay display = world.spawn(spawnLoc, ItemDisplay.class, (d) -> {
               d.setItemStack(item);
               d.setBillboard(Billboard.CENTER);
               d.setShadowRadius(0.15F);
               d.setShadowStrength(0.5F);
               d.setInterpolationDuration(1);
               d.setInterpolationDelay(0);
               d.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(0.01F, 0.01F, 0.01F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
            });
            spin.itemDisplays.add(display);
            spin.itemTargetLocations.add(targetLoc);
            if (itemHoloEnabled) {
               String rName = this.plugin.getConfig().getString("rewards." + rewardId + ".display.name", rewardId);
               TextDisplay holo = this.createHologram(world, spawnLoc.clone().add((double)0.0F, itemHoloHeight, (double)0.0F), rName, 0.01F);
               spin.itemHolograms.add(holo);
            }
         } catch (Exception var39) {
         }
      }

      boolean orbitSoundEnabled = this.plugin.getConfig().getBoolean("opening.physical.orbit_sound.enabled", true);
      String orbitSoundName = this.plugin.getConfig().getString("opening.physical.orbit_sound.sound", "BLOCK_NOTE_BLOCK_CHIME");
      int orbitSoundInterval = Math.max(1, this.plugin.getConfig().getInt("opening.physical.orbit_sound.interval_ticks", 4));
      float orbitSoundVolume = (float)this.plugin.getConfig().getDouble("opening.physical.orbit_sound.volume", 0.4);
      float orbitSoundPitch = (float)this.plugin.getConfig().getDouble("opening.physical.orbit_sound.pitch", 1.6);
      boolean warnEnabled = this.plugin.getConfig().getBoolean("opening.physical.warn_sound.enabled", true);
      int warnStart = Math.max(1, this.plugin.getConfig().getInt("opening.physical.warn_sound.countdown_start_ticks", 12));
      int warnBeeps = Math.max(1, this.plugin.getConfig().getInt("opening.physical.warn_sound.beeps", 4));
      String warnSoundName = this.plugin.getConfig().getString("opening.physical.warn_sound.sound", "ENTITY_WARDEN_SONIC_CHARGE");
      Sound orbitSound = this.resolveSound(orbitSoundName, Sound.BLOCK_NOTE_BLOCK_CHIME);
      Sound warnSound = this.resolveSound(warnSoundName, Sound.ENTITY_WARDEN_SONIC_CHARGE);
      String winnerDisplayName = this.plugin.getConfig().getString("rewards." + spin.finalReward + ".display.name", spin.finalReward);
      spin.taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (!player.isOnline()) {
            if (!spin.stateCleaned) {
               spin.stateCleaned = true;
               this.activeSpins.remove(player.getUniqueId());
            }

            this.cleanupSpin(spin);
            this.giveAllRewardsFromSpin(player, spin);
         } else if (player.getWorld() != spin.boxLocation.getWorld()) {
            if (!spin.stateCleaned) {
               spin.stateCleaned = true;
               this.activeSpins.remove(player.getUniqueId());
            }

            this.cleanupSpin(spin);
            this.giveAllRewardsFromSpin(player, spin);
         } else {
            ++spin.tick;
            int t = spin.tick;
            World w = player.getWorld();
            if (spin.chestOrigin != null) {
               if (t == 1) {
                  try {
                     Location top = spin.chestOrigin.clone().add((double)0.0F, (double)0.5F, (double)0.0F);
                     w.spawnParticle(Particle.PORTAL, top, 15, 0.3, (double)0.5F, 0.3, (double)0.5F);
                     w.spawnParticle(Particle.CLOUD, top, 10, 0.3, 0.2, 0.3, 0.08);
                     w.spawnParticle(Particle.SOUL_FIRE_FLAME, top, 8, 0.2, 0.3, 0.2, 0.03);
                     w.playSound(spin.chestOrigin, this.getChestOpenSound(spin.chestOrigin.getBlock().getType()), 1.5F, 0.8F);
                  } catch (Exception var88) {
                  }
               }

               if (t <= duration && t % 4 == 0) {
                  try {
                     Location top = spin.chestOrigin.clone().add((double)0.0F, 0.8, (double)0.0F);
                     w.spawnParticle(Particle.PORTAL, top, 3, 0.2, 0.3, 0.2, 0.3);
                     w.spawnParticle(Particle.END_ROD, top, 2, 0.15, (double)0.25F, 0.15, 0.02);
                  } catch (Exception var87) {
                  }
               }

               if (t <= duration && t % 10 == 0) {
                  try {
                     w.spawnParticle(Particle.REVERSE_PORTAL, spin.chestOrigin, 2, 0.2, 0.3, 0.2, 0.05);
                     w.spawnParticle(Particle.SOUL_FIRE_FLAME, spin.chestOrigin.clone().add((double)0.0F, (double)0.5F, (double)0.0F), 2, 0.15, 0.15, 0.15, 0.02);
                  } catch (Exception var86) {
                  }
               }

               if (t <= duration && t % 20 == 0) {
                  try {
                     w.playSound(spin.chestOrigin, Sound.BLOCK_PORTAL_AMBIENT, 0.3F, 0.8F);
                  } catch (Exception var85) {
                  }
               }
            }

            if (t <= 20) {
               float prog = Math.min(1.0F, (float)t / 20.0F);
               float eased = 1.0F - (1.0F - prog) * (1.0F - prog) * (1.0F - prog);
               float scale = boxScale * Math.max(0.01F, eased);
               Location currentBoxLoc = spin.boxLocation;
               if (spin.boxStartLoc != null && spin.boxDisplay != null && spin.boxDisplay.isValid()) {
                  Location start = spin.boxStartLoc;
                  Location target = spin.boxLocation;
                  double cx = start.getX() + (target.getX() - start.getX()) * (double)eased;
                  double cy = start.getY() + (target.getY() - start.getY()) * (double)eased;
                  double cz = start.getZ() + (target.getZ() - start.getZ()) * (double)eased;
                  currentBoxLoc = new Location(start.getWorld(), cx, cy, cz);

                  try {
                     spin.boxDisplay.teleport(currentBoxLoc);
                  } catch (Exception var84) {
                  }
               }

               if (spin.boxDisplay != null && spin.boxDisplay.isValid()) {
                  try {
                     double shake = Math.sin((double)t * 1.2) * 0.02 * (double)(1.0F - prog);
                     spin.boxDisplay.setTransformation(new Transformation(new Vector3f((float)shake, 0.0F, (float)shake), new AxisAngle4f((float)((double)t * 0.3), 0.0F, 1.0F, 0.0F), new Vector3f(scale, scale, scale), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
                  } catch (Exception var83) {
                  }
               }

               if (spin.boxHologram != null && spin.boxHologram.isValid()) {
                  try {
                     if (spin.boxStartLoc != null) {
                        spin.boxHologram.teleport(currentBoxLoc.clone().add((double)0.0F, spin.boxHoloHeight, (double)0.0F));
                     }

                     float hs = Math.max(0.01F, eased);
                     spin.boxHologram.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(hs, hs, hs), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
                  } catch (Exception var82) {
                  }
               }
            }

            if (t > 20 && t <= 40) {
               float prog = Math.min(1.0F, (float)(t - 20) / 20.0F);
               float eased = 1.0F - (1.0F - prog) * (1.0F - prog) * (1.0F - prog);
               float scale = itemScale * Math.max(0.01F, eased);

               for (int i = 0; i < spin.itemDisplays.size(); ++i) {
                  ItemDisplay d = spin.itemDisplays.get(i);
                  if (d.isValid()) {
                     if (i < spin.itemTargetLocations.size()) {
                        Location target = spin.itemTargetLocations.get(i);
                        Location start = spin.boxLocation.clone().add((double)0.0F, 0.1, (double)0.0F);
                        double cx = start.getX() + (target.getX() - start.getX()) * (double)eased;
                        double cy = start.getY() + (target.getY() - start.getY()) * (double)eased;
                        double cz = start.getZ() + (target.getZ() - start.getZ()) * (double)eased;
                        cy += Math.sin((double)prog * Math.PI) * 0.6;
                        Location current = new Location(start.getWorld(), cx, cy, cz);

                        try {
                           d.teleport(current);
                        } catch (Exception var81) {
                        }

                        if (i < spin.itemHolograms.size()) {
                           TextDisplay h = spin.itemHolograms.get(i);
                           if (h != null && h.isValid()) {
                              try {
                                 h.teleport(current.clone().add((double)0.0F, itemHoloHeight, (double)0.0F));
                              } catch (Exception var80) {
                              }
                           }
                        }
                     } else {
                        Location current = d.getLocation();
                     }

                     try {
                        d.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(scale, scale, scale), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
                     } catch (Exception var79) {
                     }
                  }
               }

               if (itemHoloEnabled) {
                  float hScale = itemHoloScale * Math.max(0.01F, eased);

                  for (TextDisplay h : spin.itemHolograms) {
                     if (h != null && h.isValid()) {
                        try {
                           h.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(hScale, hScale, hScale), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
                        } catch (Exception var78) {
                        }
                     }
                  }
               }

               if (t == 21) {
                  try {
                     w.playSound(spin.boxLocation, Sound.BLOCK_CHEST_OPEN, 1.0F, 1.2F);
                  } catch (Exception var77) {
                  }
               }
            }

            if (t > 40 && t < duration) {
               double baseRotation = (double)(t - 41) * rotationSpeed;
               double sharedBob = Math.sin((double)t * 0.08) * 0.12;
               int n = spin.itemDisplays.size();

               for (int i = 0; i < n; ++i) {
                  ItemDisplay d = spin.itemDisplays.get(i);
                  if (d.isValid()) {
                     double angle = baseRotation + (Math.PI * 2D) / (double)n * (double)i;
                     Location newLoc = spin.boxLocation.clone().add(Math.cos(angle) * radius, heightOffset + sharedBob, Math.sin(angle) * radius);

                     try {
                        d.teleport(newLoc);
                     } catch (Exception var76) {
                     }

                     if (t % 3 == 0) {
                        w.spawnParticle(Particle.END_ROD, d.getLocation(), 1, (double)0.0F, (double)0.0F, (double)0.0F, (double)0.0F);
                     }

                     if (i < spin.itemHolograms.size()) {
                        TextDisplay h = spin.itemHolograms.get(i);
                        if (h != null && h.isValid()) {
                           try {
                              h.teleport(newLoc.clone().add((double)0.0F, itemHoloHeight, (double)0.0F));
                           } catch (Exception var75) {
                           }
                        }
                     }
                  }
               }

               if (orbitSoundEnabled && t % orbitSoundInterval == 0) {
                  try {
                     w.playSound(spin.boxLocation, orbitSound, orbitSoundVolume, orbitSoundPitch);
                  } catch (Exception var74) {
                  }
               }
            }

            if (t > 20 && t < duration && spin.boxDisplay != null && spin.boxDisplay.isValid()) {
               try {
                  float rotationMultiplier = 0.3F + (float)(t - 20) * 0.005F;
                  spin.boxDisplay.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f((float)t * rotationMultiplier, 0.0F, 1.0F, 0.0F), new Vector3f(boxScale, boxScale, boxScale), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
               } catch (Exception var73) {
               }
            }

            if (warnEnabled && t >= duration - warnStart && t < duration && warnBeeps > 0) {
               int interval = Math.max(1, warnStart / warnBeeps);
               int offset = t - (duration - warnStart);
               if (offset % interval == 0) {
                  int idx = offset / interval;
                  float pitch = 0.8F + (float)idx * 0.35F;

                  try {
                     player.playSound(player.getLocation(), warnSound, 1.0F, pitch);
                  } catch (Exception var72) {
                  }
               }
            }

            if (t == duration) {
               Location boxBreakLoc = spin.boxLocation.clone();

               try {
                  w.spawnParticle(Particle.EXPLOSION, boxBreakLoc, 1);
                  w.spawnParticle(Particle.CLOUD, boxBreakLoc, 25, (double)0.5F, (double)0.5F, (double)0.5F, 0.15);
                  w.spawnParticle(Particle.CRIT, boxBreakLoc, 40, 0.6, 0.6, 0.6, 0.4);
                  w.spawnParticle(Particle.ENCHANT, boxBreakLoc, 40, 0.7, 0.7, 0.7, (double)1.0F);
                  w.playSound(boxBreakLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 1.2F);
                  w.playSound(boxBreakLoc, Sound.BLOCK_WOOD_BREAK, 1.2F, 0.8F);
               } catch (Exception var71) {
               }

               this.removeBox(spin);
               this.removeBoxHologram(spin);

               for (int i = spin.itemDisplays.size() - 1; i >= 1; --i) {
                  ItemDisplay d = spin.itemDisplays.get(i);

                  try {
                     if (d.isValid()) {
                        d.remove();
                     }
                  } catch (Exception var70) {
                  }

                  spin.itemDisplays.remove(i);
               }

               for (int i = spin.itemHolograms.size() - 1; i >= 1; --i) {
                  TextDisplay h = spin.itemHolograms.get(i);

                  try {
                     if (h != null && h.isValid()) {
                        h.remove();
                     }
                  } catch (Exception var69) {
                  }

                  spin.itemHolograms.remove(i);
               }

               if (!spin.itemDisplays.isEmpty()) {
                  ItemDisplay winner = spin.itemDisplays.get(0);
                  if (winner.isValid()) {
                     try {
                        winner.setItemStack(this.createRewardItem(spin.finalReward));
                        winner.teleport(boxBreakLoc);
                     } catch (Exception var68) {
                     }

                     spin.centerMoveStart = boxBreakLoc.clone();
                  }
               }

               if (!spin.itemHolograms.isEmpty()) {
                  TextDisplay h = spin.itemHolograms.get(0);
                  String holoStr = winnerDisplayName;
                  if (spin.amount > 1) {
                     holoStr = "&f&l+&e&l" + spin.amount + " &f&lREWARDS";
                  }

                  this.updateHoloText(h, holoStr);
                  if (h != null && h.isValid()) {
                     try {
                        h.teleport(boxBreakLoc.clone().add((double)0.0F, itemHoloHeight, (double)0.0F));
                     } catch (Exception var67) {
                     }
                  }
               }

               try {
                  w.playSound(boxBreakLoc, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
                  w.playSound(boxBreakLoc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
                  w.spawnParticle(Particle.FLASH, boxBreakLoc, 1);
                  w.spawnParticle(Particle.TOTEM_OF_UNDYING, boxBreakLoc, 30, 0.8, 0.8, 0.8, 0.3);
                  w.spawnParticle(Particle.ELECTRIC_SPARK, boxBreakLoc, 30, 0.8, 0.8, 0.8, 0.1);
               } catch (Exception var66) {
               }
            }

            if (t > duration && t < T_START_CENTER && !spin.itemDisplays.isEmpty()) {
               ItemDisplay winner = spin.itemDisplays.get(0);
               if (winner.isValid()) {
                  Location wl = winner.getLocation();
                  w.spawnParticle(Particle.END_ROD, wl, 3, 0.2, 0.2, 0.2, (double)0.0F);
                  w.spawnParticle(Particle.FLAME, wl.clone().add((double)0.0F, -0.2, (double)0.0F), 1, 0.1, 0.1, 0.1, (double)0.0F);
                  w.spawnParticle(Particle.ELECTRIC_SPARK, wl, 2, 0.15, 0.15, 0.15, (double)0.0F);
               }
            }

            if (t >= T_START_CENTER && t < T_AT_CENTER) {
               float progress = (float)(t - T_START_CENTER) / (float)moveCenter;
               progress = Math.max(0.0F, Math.min(1.0F, progress));
               Location target = spin.boxLocation.clone().add((double)0.0F, heightOffset + (double)0.5F, (double)0.0F);
               if (!spin.itemDisplays.isEmpty() && spin.centerMoveStart != null) {
                  ItemDisplay winner = spin.itemDisplays.get(0);
                  Location start = spin.centerMoveStart;
                  double cx = start.getX() + (target.getX() - start.getX()) * (double)progress;
                  double cy = start.getY() + (target.getY() - start.getY()) * (double)progress;
                  double cz = start.getZ() + (target.getZ() - start.getZ()) * (double)progress;
                  Location current = new Location(start.getWorld(), cx, cy, cz);
                  if (winner.isValid()) {
                     try {
                        winner.teleport(current);
                     } catch (Exception var65) {
                     }
                  }

                  if (!spin.itemHolograms.isEmpty()) {
                     TextDisplay h = spin.itemHolograms.get(0);
                     if (h != null && h.isValid()) {
                        try {
                           h.teleport(current.clone().add((double)0.0F, itemHoloHeight, (double)0.0F));
                        } catch (Exception var64) {
                        }
                     }
                  }

                  w.spawnParticle(Particle.END_ROD, current, 4, 0.1, 0.1, 0.1, 0.02);
                  w.spawnParticle(Particle.ELECTRIC_SPARK, current, 3, 0.1, 0.1, 0.1, 0.05);
               }

               for (int i = spin.itemDisplays.size() - 1; i >= 1; --i) {
                  ItemDisplay d = spin.itemDisplays.get(i);
                  if (d.isValid()) {
                     float s = Math.max(0.01F, itemScale * (1.0F - progress));

                     try {
                        d.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(s, s, s), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
                     } catch (Exception var63) {
                     }

                     if ((double)progress > (double)0.5F) {
                        try {
                           d.remove();
                        } catch (Exception var62) {
                        }

                        spin.itemDisplays.remove(i);
                        if (i < spin.itemHolograms.size()) {
                           TextDisplay h = spin.itemHolograms.get(i);

                           try {
                              if (h != null && h.isValid()) {
                                 h.remove();
                              }
                           } catch (Exception var61) {
                           }

                           spin.itemHolograms.remove(i);
                        }
                     }
                  }
               }
            }

            if (t == T_AT_CENTER) {
               Location center = spin.boxLocation.clone().add((double)0.0F, heightOffset + (double)0.5F, (double)0.0F);
               if (!spin.itemDisplays.isEmpty()) {
                  ItemDisplay winner = spin.itemDisplays.get(0);
                  if (winner.isValid()) {
                     float fs = itemScale * 1.8F;

                     try {
                        winner.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(fs, fs, fs), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
                     } catch (Exception var60) {
                     }

                     try {
                        winner.teleport(center);
                     } catch (Exception var59) {
                     }
                  }

                  if (!spin.itemHolograms.isEmpty()) {
                     TextDisplay h = spin.itemHolograms.get(0);
                     if (h != null && h.isValid()) {
                        try {
                           h.teleport(center.clone().add((double)0.0F, itemHoloHeight * (double)1.5F, (double)0.0F));
                           float hs = itemHoloScale * 1.5F;
                           h.setTransformation(new Transformation(new Vector3f(0.0F, 0.0F, 0.0F), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F), new Vector3f(hs, hs, hs), new AxisAngle4f(0.0F, 0.0F, 1.0F, 0.0F)));
                        } catch (Exception var58) {
                        }
                     }
                  }
               }

               try {
                  w.playSound(center, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.4F);
                  w.playSound(center, Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.8F);
                  w.spawnParticle(Particle.FLASH, center, 2);
                  w.spawnParticle(Particle.TOTEM_OF_UNDYING, center, 40, 0.8, 0.8, 0.8, 0.4);
                  w.spawnParticle(Particle.ELECTRIC_SPARK, center, 50, (double)1.0F, (double)1.0F, (double)1.0F, 0.15);
                  w.spawnParticle(Particle.HEART, center, 8, (double)0.5F, (double)0.5F, (double)0.5F, 0.1);
               } catch (Exception var57) {
               }
            }

            if (t > T_AT_CENTER && t < T_FLY_START) {
               Location center = spin.boxLocation.clone().add((double)0.0F, heightOffset + (double)0.5F, (double)0.0F);
               if (t % 2 == 0) {
                  w.spawnParticle(Particle.END_ROD, center, 4, 0.4, 0.4, 0.4, 0.05);
                  w.spawnParticle(Particle.ENCHANT, center, 8, (double)0.5F, (double)0.5F, (double)0.5F, (double)0.5F);
               }
            }

            if (t >= T_FLY_START && t < T_IMPACT) {
               float progress = (float)(t - T_FLY_START) / (float)flyDuration;
               progress = Math.max(0.0F, Math.min(1.0F, progress));
               Location startLoc = spin.boxLocation.clone().add((double)0.0F, heightOffset + (double)0.5F, (double)0.0F);
               Location playerLoc = player.getEyeLocation().clone().add((double)0.0F, 0.2, (double)0.0F);
               double cx = startLoc.getX() + (playerLoc.getX() - startLoc.getX()) * (double)progress;
               double cy = startLoc.getY() + (playerLoc.getY() - startLoc.getY()) * (double)progress;
               double cz = startLoc.getZ() + (playerLoc.getZ() - startLoc.getZ()) * (double)progress;
               cy += Math.sin((double)progress * Math.PI) * (double)1.5F;
               Location current = new Location(player.getWorld(), cx, cy, cz, startLoc.getYaw(), startLoc.getPitch());
               if (!spin.itemDisplays.isEmpty()) {
                  ItemDisplay winner = spin.itemDisplays.get(0);
                  if (winner.isValid()) {
                     try {
                        winner.teleport(current);
                        winner.setRotation((float)t * 20.0F % 360.0F, 0.0F);
                     } catch (Exception var56) {
                     }
                  }
               }

               if (!spin.itemHolograms.isEmpty()) {
                  TextDisplay h = spin.itemHolograms.get(0);
                  if (h != null && h.isValid()) {
                     try {
                        h.teleport(current.clone().add((double)0.0F, itemHoloHeight * (double)1.5F, (double)0.0F));
                     } catch (Exception var55) {
                     }
                  }
               }

               w.spawnParticle(Particle.END_ROD, current, 3, 0.05, 0.05, 0.05, 0.02);
               w.spawnParticle(Particle.ELECTRIC_SPARK, current, 2, 0.1, 0.1, 0.1, 0.05);
               w.spawnParticle(Particle.ENCHANT, current, 5, 0.3, 0.3, 0.3, (double)0.5F);
            }

            if (t >= T_IMPACT) {
               if (spin.chestOrigin != null) {
                  try {
                     ChestPacketAnimator anim = this.plugin.getChestPacketAnimator();
                     if (anim != null) {
                        anim.close(spin.chestOrigin);
                     }
                  } catch (Exception var54) {
                  }

                  try {
                     Material mat = spin.chestOrigin.getBlock().getType();
                     w.playSound(spin.chestOrigin, this.getChestCloseSound(mat), 1.0F, 0.8F);
                  } catch (Exception var53) {
                  }
               }

               for (ItemDisplay d : spin.itemDisplays) {
                  try {
                     if (d.isValid()) {
                        d.remove();
                     }
                  } catch (Exception var52) {
                  }
               }

               spin.itemDisplays.clear();
               this.removeAllItemHolograms(spin);
               this.removeBox(spin);
               this.removeBoxHologram(spin);
               this.giveAllRewardsFromSpin(player, spin);

               try {
                  if (spin.amount > 1) {
                     this.spawnMultiImpactEffects(player, spin.pickedRewards);
                  } else {
                     this.spawnImpactEffects(player);
                  }
               } catch (Exception var51) {
               }

               if (!spin.stateCleaned) {
                  spin.stateCleaned = true;
                  this.activeSpins.remove(player.getUniqueId());
               }

               if (spin.taskId != -1) {
                  try {
                     Bukkit.getScheduler().cancelTask(spin.taskId);
                  } catch (Exception var50) {
                  }

                  spin.taskId = -1;
               }
            }

         }
      }, 0L, 1L).getTaskId();
   }

   private Sound resolveSound(String name, Sound fallback) {
      if (name != null && !name.isEmpty()) {
         try {
            String normalized = name.trim().toLowerCase();
            Sound s = Registry.SOUNDS.get(NamespacedKey.minecraft(normalized));
            if (s != null) {
               return s;
            } else {
               s = Registry.SOUNDS.get(NamespacedKey.minecraft(normalized.replace('_', '.')));
               return s != null ? s : fallback;
            }
         } catch (Exception var5) {
            return fallback;
         }
      } else {
         return fallback;
      }
   }

   private void removeBox(SpinData spin) {
      if (spin.boxDisplay != null) {
         try {
            spin.boxDisplay.remove();
         } catch (Exception var3) {
         }

         spin.boxDisplay = null;
      }
   }

   private void spawnImpactEffects(Player player) {
      Location loc = player.getLocation().clone().add((double)0.0F, (double)1.0F, (double)0.0F);
      World world = player.getWorld();

      try {
         Firework fw = world.spawn(loc, Firework.class);
         FireworkMeta fm = fw.getFireworkMeta();
         fm.addEffect(FireworkEffect.builder().withColor(new Color[]{Color.fromRGB(255, 100, 255), Color.AQUA, Color.YELLOW}).with(Type.BURST).trail(true).flicker(true).build());
         fm.setPower(1);
         fw.setFireworkMeta(fm);
      } catch (Exception var6) {
      }

      world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 60, (double)1.0F, (double)1.0F, (double)1.0F, (double)0.5F);
      world.spawnParticle(Particle.FLASH, loc, 3);
      world.spawnParticle(Particle.END_ROD, loc, 50, 1.2, 1.2, 1.2, 0.15);
      world.spawnParticle(Particle.HEART, loc, 12, 0.6, 0.6, 0.6, 0.1);
      world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 40, (double)1.0F, (double)1.0F, (double)1.0F, 0.2);
      world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
      world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 1.0F);
      world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0F, 1.2F);
      world.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.4F);
   }

   private void spawnMultiImpactEffects(Player player, List<String> rewards) {
      Location loc = player.getLocation().clone().add((double)0.0F, 1.2, (double)0.0F);
      World world = player.getWorld();
      int visualCount = Math.min(Math.max(rewards.size(), 1), 7);

      for (int i = 0; i < visualCount; ++i) {
         double angle = (Math.PI * 2D) / (double)visualCount * (double)i;
         Location spawnAt = loc.clone().add(Math.cos(angle) * 1.2, 0.3, Math.sin(angle) * 1.2);

         try {
            Firework fw = world.spawn(spawnAt, Firework.class);
            FireworkMeta fm = fw.getFireworkMeta();
            fm.addEffect(FireworkEffect.builder().withColor(new Color[]{Color.fromRGB(255, 100, 255), Color.AQUA, Color.YELLOW, Color.LIME}).with(Type.BALL_LARGE).trail(true).flicker(true).build());
            fm.setPower(1);
            fw.setFireworkMeta(fm);
         } catch (Exception var14) {
         }
      }

      try {
         world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 80, (double)1.5F, (double)1.5F, (double)1.5F, (double)0.5F);
         world.spawnParticle(Particle.FLASH, loc, 5);
         world.spawnParticle(Particle.FIREWORK, loc, 120, (double)1.5F, (double)1.5F, (double)1.5F, 0.4);
         world.spawnParticle(Particle.ELECTRIC_SPARK, loc, 60, 1.2, 1.2, 1.2, 0.2);
      } catch (Exception var13) {
      }

      try {
         world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
         world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.0F, 1.0F);
         world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0F, 1.2F);
         world.playSound(loc, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.4F);
         world.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6F, 1.4F);
      } catch (Exception var12) {
      }

   }

   private void cleanupSpin(SpinData spin) {
      if (spin.taskId != -1) {
         try {
            Bukkit.getScheduler().cancelTask(spin.taskId);
         } catch (Exception var7) {
         }

         spin.taskId = -1;
      }

      if (spin.chestOrigin != null) {
         try {
            ChestPacketAnimator anim = this.plugin.getChestPacketAnimator();
            if (anim != null) {
               anim.close(spin.chestOrigin);
            }
         } catch (Exception var6) {
         }
      }

      this.removeBox(spin);
      this.removeBoxHologram(spin);

      for (ItemDisplay d : spin.itemDisplays) {
         try {
            if (d.isValid()) {
               d.remove();
            }
         } catch (Exception var5) {
         }
      }

      spin.itemDisplays.clear();
      this.removeAllItemHolograms(spin);
   }

   @EventHandler
   public void onSpinGuiClick(InventoryClickEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof SpinGuiHolder) {
         event.setCancelled(true);
      }
   }

   @EventHandler
   public void onSpinGuiClose(InventoryCloseEvent event) {
      if (event.getPlayer() instanceof Player) {
         if (event.getView().getTopInventory().getHolder() instanceof SpinGuiHolder) {
            Player p = (Player)event.getPlayer();
            SpinData spin = this.activeSpins.get(p.getUniqueId());
            if (spin != null) {
               if (!spin.stateCleaned) {
                  Inventory inv = event.getInventory();
                  Bukkit.getScheduler().runTask(this.plugin, () -> {
                     if (p.isOnline()) {
                        SpinData cur = this.activeSpins.get(p.getUniqueId());
                        if (cur != null && !cur.stateCleaned) {
                           try {
                              p.openInventory(inv);
                           } catch (Exception var5) {
                           }

                        }
                     }
                  });
               }
            }
         }
      }
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Player p = event.getPlayer();
      this.chestOrigins.remove(p.getUniqueId());
      this.chestOriginTimestamps.remove(p.getUniqueId());
      SpinData spin = this.activeSpins.remove(p.getUniqueId());
      if (spin != null) {
         spin.stateCleaned = true;
         this.cleanupSpin(spin);
         this.giveAllRewardsFromSpin(p, spin);
      }
   }

   private String pickWeighted(List<String> ids) {
      int total = 0;
      Map<String, Integer> weights = new HashMap<>();

      for (String id : ids) {
         String rarity = this.plugin.getConfig().getString("rewards." + id + ".rarity", "common");
         int w = this.plugin.getConfig().getInt("rarities." + rarity.toLowerCase() + ".weight", 10);
         weights.put(id, w);
         total += w;
      }

      int roll = this.random.nextInt(Math.max(1, total));
      int cum = 0;

      for (Map.Entry<String, Integer> e : weights.entrySet()) {
         cum += e.getValue();
         if (roll < cum) {
            return e.getKey();
         }
      }

      return ids.get(0);
   }

   private String resolveRewardDisplayName(String rewardId) {
      if (rewardId != null && !rewardId.isEmpty()) {
         String path = "rewards." + rewardId;
         String name = this.plugin.getConfig().getString(path + ".display.name");
         if (name != null && !name.isEmpty()) {
            return name;
         } else {
            String cosmeticId = this.plugin.getConfig().getString(path + ".cosmetic_id");
            if (cosmeticId != null && !cosmeticId.isEmpty()) {
               String cosName = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".display_name");
               return cosName != null && !cosName.isEmpty() ? cosName : cosmeticId;
            } else {
               if (rewardId.startsWith("cosmetic_")) {
                  String s = rewardId.substring("cosmetic_".length());
                  if (!s.isEmpty()) {
                     String var10000 = s.substring(0, 1).toUpperCase();
                     return var10000 + s.substring(1);
                  }
               }

               return rewardId;
            }
         }
      } else {
         return "Reward";
      }
   }

   private String resolveRewardRarity(String rewardId) {
      String path = "rewards." + rewardId;
      String rarity = this.plugin.getConfig().getString(path + ".rarity");
      if (rarity != null && !rarity.isEmpty()) {
         return rarity;
      } else {
         String cosmeticId = this.plugin.getConfig().getString(path + ".cosmetic_id");
         return cosmeticId != null && !cosmeticId.isEmpty() ? this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".rarity", "common") : "common";
      }
   }

   private ItemStack createRewardItem(String rewardId) {
      String matName = this.plugin.getConfig().getString("rewards." + rewardId + ".display.material", "STONE");
      Material mat = Material.matchMaterial(matName);
      if (mat == null) {
         mat = Material.STONE;
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         String name = this.plugin.getConfig().getString("rewards." + rewardId + ".display.name", rewardId);
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
         String rarity = this.plugin.getConfig().getString("rewards." + rewardId + ".rarity", "common");
         String color = this.plugin.getConfig().getString("rarities." + rarity.toLowerCase() + ".color", "&7");
         String rName = this.plugin.getConfig().getString("rarities." + rarity.toLowerCase() + ".name", "Common");
         List<String> lore = new ArrayList<>();
         lore.add(ChatColor.translateAlternateColorCodes('&', "&7"));
         lore.add(ChatColor.translateAlternateColorCodes('&', "&7Rarity: " + color + rName));
         meta.setLore(lore);
         meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
         item.setItemMeta(meta);
         return item;
      }
   }

   private void sendRewardNotification(Player player, String boxTypeId, String rewardId) {
      if (player != null && player.isOnline()) {
         String boxName = boxTypeId != null ? boxTypeId : "Box";
         String displayName = rewardId != null ? rewardId : "Reward";

         try {
            String cfg = this.plugin.getConfig().getString("boxes." + boxTypeId + ".display_name");
            if (cfg != null && !cfg.isEmpty()) {
               boxName = cfg;
            }
         } catch (Exception var17) {
         }

         displayName = this.resolveRewardDisplayName(rewardId);
         String boxNameTranslated = ChatColor.translateAlternateColorCodes('&', boxName);
         String displayTranslated = ChatColor.translateAlternateColorCodes('&', displayName);
         String boxNameStripped = ChatColor.stripColor(boxNameTranslated) != null ? ChatColor.stripColor(boxNameTranslated) : boxName;

         try {
            String raw = this.plugin.msgRaw("box_opened");
            if (raw == null || raw.isEmpty()) {
               raw = "&aOpened 1x &e{type} &aand got: &e{reward}";
            }

            String msg = raw.replace("{type}", boxNameStripped != null ? boxNameStripped : boxName).replace("{reward}", displayTranslated != null ? displayTranslated : displayName);
            player.sendMessage(msg);
         } catch (Exception e) {
            this.plugin.getLogger().warning("[SpinManager] Chat notification error: " + e.getMessage());

            try {
               String var10001 = String.valueOf(ChatColor.GREEN);
               player.sendMessage(var10001 + "You got: " + String.valueOf(ChatColor.YELLOW) + rewardId);
            } catch (Exception var16) {
            }
         }

         try {
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize("&a&l+ &f" + displayTranslated));
         } catch (Exception var15) {
         }

         try {
            Component titleComp = ((TextComponent)Component.text("REWARD").color(NamedTextColor.LIGHT_PURPLE)).decoration(TextDecoration.BOLD, true);

            Component subComp;
            try {
               subComp = LegacyComponentSerializer.legacyAmpersand().deserialize(displayTranslated);
            } catch (Exception var13) {
               subComp = Component.text(displayName);
            }

            player.showTitle(Title.title(titleComp, subComp, Times.times(Duration.ofMillis(300L), Duration.ofMillis(1800L), Duration.ofMillis(500L))));
         } catch (Exception var14) {
         }

         try {
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 1.0F, 1.0F);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 1.4F);
         } catch (Exception var12) {
         }

      }
   }

   private void broadcastHighRarityCosmetic(Player player, String cosmeticId, String boxTypeId) {
      if (this.plugin.getConfig().getBoolean("broadcast.enabled", true)) {
         String rarity = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".rarity", "common").toLowerCase();
         if (this.plugin.getConfig().getBoolean("broadcast.rarities." + rarity, false)) {
            try {
               String cosmeticName = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".display_name", cosmeticId);
               String boxName = this.plugin.getConfig().getString("boxes." + boxTypeId + ".display_name", boxTypeId);
               String rarityColor = this.plugin.getConfig().getString("rarities." + rarity + ".color", "&7");
               String rarityName = this.plugin.getConfig().getString("rarities." + rarity + ".name", "Common");
               String format = this.plugin.getConfig().getString("broadcast.format", "&e{player} &ajust obtained {cosmetic} &afrom {box} &7({rarity}&7)&c!");
               String message = ChatColor.translateAlternateColorCodes('&', format.replace("{player}", player.getName()).replace("{cosmetic}", cosmeticName).replace("{box}", boxName).replace("{rarity}", rarityColor + rarityName));
               Bukkit.broadcastMessage(message);
               if (this.plugin.getConfig().getBoolean("broadcast.sound.enabled", true)) {
                  String soundName = this.plugin.getConfig().getString("broadcast.sound.name", "ENTITY_ENDER_DRAGON_GROWL");
                  float volume = (float)this.plugin.getConfig().getDouble("broadcast.sound.volume", (double)1.0F);
                  float pitch = (float)this.plugin.getConfig().getDouble("broadcast.sound.pitch", (double)1.0F);
                  Sound sound = this.resolveSound(soundName, Sound.ENTITY_ENDER_DRAGON_GROWL);

                  for (Player p : Bukkit.getOnlinePlayers()) {
                     p.playSound(p.getLocation(), sound, volume, pitch);
                  }
               }
            } catch (Exception e) {
               this.plugin.getLogger().warning("[SpinManager] Broadcast failed: " + e.getMessage());
            }

         }
      }
   }

   private void sendDuplicateCosmeticMessage(Player player, String cosmeticId, String boxTypeId) {
      if (player != null && player.isOnline()) {
         try {
            String cosmeticName = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".display_name", cosmeticId);
            String rarity = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".rarity", "common").toLowerCase();
            String rarityColor = this.plugin.getConfig().getString("rarities." + rarity + ".color", "&7");
            String rarityName = this.plugin.getConfig().getString("rarities." + rarity + ".name", "Common");
            CosmeticType type = CosmeticType.fromString(this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".type", "TRAIL"));
            String typeLabel;
            switch (type) {
               case TRAIL -> typeLabel = "Trail";
               case HAT -> typeLabel = "Hat";
               case GADGET -> typeLabel = "Gadget";
               default -> typeLabel = "Cosmetic";
            }

            String rawMsg = this.plugin.msgRaw("cosmetic_duplicate");
            if (rawMsg == null || rawMsg.isEmpty()) {
               rawMsg = "&e&l⚠ &eYou already own this &f{type}&e: &e{cosmetic} &7({rarity}&7)!";
            }

            String msg = rawMsg.replace("{cosmetic}", cosmeticName).replace("{rarity}", rarityColor + rarityName).replace("{type}", typeLabel);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         } catch (Exception e) {
            this.plugin.getLogger().warning("[SpinManager] Duplicate notification failed: " + e.getMessage());
         }

      }
   }

   private void giveReward(Player player, String boxTypeId, String rewardId) {
      try {
         String type = this.plugin.getConfig().getString("rewards." + rewardId + ".type", "COINS");
         switch (type.toUpperCase()) {
            // ✅ FIX: ห่อแต่ละ case ด้วย { ... } เพื่อแยก scope ของตัวแปร amount
            case "COINS": {
               int amount = this.plugin.getConfig().getInt("rewards." + rewardId + ".amount", 100);
               this.plugin.getData().addCoins(player.getUniqueId(), (long)amount);
               break;
            }
            case "COSMETIC": {
               String cid = this.plugin.getConfig().getString("rewards." + rewardId + ".cosmetic_id", "");
               if (!cid.isEmpty()) {
                  boolean alreadyOwned = false;

                  try {
                     alreadyOwned = this.plugin.getData().ownsCosmetic(player.getUniqueId(), cid);
                  } catch (Exception var10) {
                  }

                  if (alreadyOwned) {
                     this.sendDuplicateCosmeticMessage(player, cid, boxTypeId);
                  } else {
                     this.plugin.getData().giveCosmetic(player.getUniqueId(), cid);
                     this.broadcastHighRarityCosmetic(player, cid, boxTypeId);
                     this.triggerCosmeticAdvancements(player);
                  }
               }
               break;
            }
            case "BOX": {
               String boxId = this.plugin.getConfig().getString("rewards." + rewardId + ".box_id", "");
               int amount = this.plugin.getConfig().getInt("rewards." + rewardId + ".amount", 1);
               if (!boxId.isEmpty()) {
                  int added = this.plugin.getData().addBox(player.getUniqueId(), boxId, amount);
                  if (added < amount && player.isOnline()) {
                     player.sendMessage(String.valueOf(ChatColor.RED) + "⚠ Vault full! Some boxes were lost.");
                  }
               }
               break;
            }
         }
      } catch (Exception e) {
         this.plugin.getLogger().warning("[SpinManager] Failed to give reward '" + rewardId + "': " + e.getMessage());
      }

   }

   private void giveAllRewardsFromSpin(Player p, SpinData spin) {
      if (!spin.rewardGiven) {
         spin.rewardGiven = true;

         try {
            if (spin.amount > 1 && !spin.pickedRewards.isEmpty()) {
               for (String rId : spin.pickedRewards) {
                  this.giveReward(p, spin.boxTypeId, rId);
               }

               this.sendMultiRewardNotification(p, spin.boxTypeId, spin.pickedRewards);
            } else {
               this.sendRewardNotification(p, spin.boxTypeId, spin.finalReward);
               this.giveReward(p, spin.boxTypeId, spin.finalReward);
            }
         } catch (Exception var5) {
         }

      }
   }

   private Plugin getAdvancementPlugin() {
      PluginManager pm = Bukkit.getPluginManager();
      String[] names = new String[]{"FawayMC-Advancement", "FawayAdvancement", "Advancement", "advancement", "FawayMC_Advancement"};

      for (String n : names) {
         Plugin p = pm.getPlugin(n);
         if (p != null && p.isEnabled()) {
            return p;
         }
      }

      for (Plugin p : pm.getPlugins()) {
         if (p.isEnabled()) {
            String cls = p.getClass().getName();
            if (cls.startsWith("org.kcm.bloomery.fawaymc.advancement.")) {
               return p;
            }
         }
      }

      return null;
   }

   private boolean debugOnce(String key) {
      return this.debugLogged.add(key);
   }

   private void triggerAdvancement(Player player, String advId, int amount) {
      if (player != null && advId != null && amount > 0) {
         try {
            Plugin adv = this.getAdvancementPlugin();
            if (adv == null) {
               if (this.debugOnce("adv-not-found")) {
                  this.plugin.getLogger().warning("[AdvHook] Advancement plugin not found — hooks disabled");
               }

               return;
            }

            Object mgr = adv.getClass().getMethod("getAdvancementManager").invoke(adv);
            if (mgr == null) {
               if (this.debugOnce("adv-mgr-null")) {
                  this.plugin.getLogger().warning("[AdvHook] getAdvancementManager() returned null");
               }

               return;
            }

            mgr.getClass().getMethod("incrementProgress", Player.class, String.class, Integer.TYPE).invoke(mgr, player, advId, amount);
            this.plugin.getLogger().fine("[AdvHook] ✓ " + player.getName() + " → " + advId + " +" + amount);
         } catch (NoSuchMethodException e) {
            if (this.debugOnce("adv-method-" + advId)) {
               this.plugin.getLogger().warning("[AdvHook] Method not found for " + advId + ": " + e.getMessage());
            }
         } catch (Throwable t) {
            if (this.debugOnce("adv-error-" + advId)) {
               this.plugin.getLogger().warning("[AdvHook] Failed " + advId + ": " + t.getMessage());
            }
         }

      }
   }

   private void triggerBoxOpenAdvancements(Player player, int amount) {
      this.triggerAdvancement(player, "lucky_first", amount);
      this.triggerAdvancement(player, "lucky_50", amount);
      this.triggerAdvancement(player, "lucky_500", amount);
   }

   private void triggerCosmeticAdvancements(Player player) {
      this.triggerAdvancement(player, "cosmetic_first", 1);
      this.triggerAdvancement(player, "cosmetic_collector", 1);
   }

   public static class SpinGuiHolder implements InventoryHolder {
      private Inventory inventory;

      public void setInventory(Inventory inv) {
         this.inventory = inv;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   private static class SpinData {
      final String boxTypeId;
      final String finalReward;
      final List<String> rewards;
      final Location boxLocation;
      final List<String> pickedRewards = new ArrayList<>();
      int amount = 1;
      Location boxStartLoc = null;
      double boxHoloHeight = 0.9;
      final List<ItemDisplay> itemDisplays = new ArrayList<>();
      final List<TextDisplay> itemHolograms = new ArrayList<>();
      final List<Location> itemTargetLocations = new ArrayList<>();
      ItemDisplay boxDisplay;
      TextDisplay boxHologram;
      Location centerMoveStart = null;
      Location chestOrigin = null;
      int taskId = -1;
      boolean rewardGiven = false;
      boolean stateCleaned = false;
      int tick = 0;
      float boxScale = 0.6F;
      float itemScale = 0.5F;

      SpinData(String boxTypeId, String finalReward, List<String> rewards, Location boxLocation) {
         this.boxTypeId = boxTypeId;
         this.finalReward = finalReward;
         this.rewards = rewards;
         this.boxLocation = boxLocation;
      }
   }
}