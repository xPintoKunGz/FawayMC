package org.kcm.bloomery.fawaymc.luckyblock;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class Global extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {
   private File configFile;
   private FileConfiguration config;
   private PlayerDataManager dataManager;
   private LuckyBlockMenuManager menuManager;
   private SpinManager spinManager;
   private CosmeticManager cosmeticManager;
   private HologramManager hologramManager;
   private ShopManager shopManager;
   private ChestPacketAnimator chestPacketAnimator;

   public ShopManager getShopManager() {
      return this.shopManager;
   }

   private List<String> resolveCosmeticIds(String arg) {
      List<String> result = new ArrayList();
      ConfigurationSection cosmetics = this.getConfig().getConfigurationSection("cosmetics");
      if (cosmetics == null) {
         return result;
      } else {
         String key = arg.toLowerCase();
         if (key.equals("all")) {
            result.addAll(cosmetics.getKeys(false));
         } else if (!key.equals("trail") && !key.equals("trails")) {
            if (key.equals("hat") || key.equals("hats")) {
               for(String id : cosmetics.getKeys(false)) {
                  if ("HAT".equalsIgnoreCase(this.getConfig().getString("cosmetics." + id + ".type", "TRAIL"))) {
                     result.add(id);
                  }
               }
            } else if (key.equals("gadget") || key.equals("gadgets")) {
               for(String id : cosmetics.getKeys(false)) {
                  if ("GADGET".equalsIgnoreCase(this.getConfig().getString("cosmetics." + id + ".type", "TRAIL"))) {
                     result.add(id);
                  }
               }
            } else if (cosmetics.contains(arg)) {
               result.add(arg);
            }
         } else {
            for(String id : cosmetics.getKeys(false)) {
               if ("TRAIL".equalsIgnoreCase(this.getConfig().getString("cosmetics." + id + ".type", "TRAIL"))) {
                  result.add(id);
               }
            }
         }

         return result;
      }
   }

   public void onEnable() {
      this.createConfig();
      this.dataManager = new PlayerDataManager(this);
      this.menuManager = new LuckyBlockMenuManager(this);
      this.spinManager = new SpinManager(this);
      this.cosmeticManager = new CosmeticManager(this);
      this.shopManager = new ShopManager(this);
      this.hologramManager = new HologramManager(this);
      this.chestPacketAnimator = new ChestPacketAnimator(this);
      this.cosmeticManager.startTrailTask();
      this.hologramManager.start();
      if (this.getCommand("luckybox") != null) {
         this.getCommand("luckybox").setExecutor(this);
      }

      if (this.getCommand("luckyadmin") != null) {
         this.getCommand("luckyadmin").setExecutor(this);
         this.getCommand("luckyadmin").setTabCompleter(this);
      }

      if (this.getCommand("cosmetics") != null) {
         this.getCommand("cosmetics").setExecutor(this);
      }

      if (this.getCommand("cosmeticshop") != null) {
         this.getCommand("cosmeticshop").setExecutor(this);
      }

      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         try {
            (new LuckyblockExpansion(this)).register();
            this.getLogger().info("[Luckyblock] PlaceholderAPI expansion registered!");
         } catch (Exception e) {
            this.getLogger().warning("[Luckyblock] Failed to register PAPI: " + e.getMessage());
         }
      } else {
         this.getLogger().warning("[Luckyblock] PlaceholderAPI not found — placeholders disabled");
      }

      this.getLogger().info("[Luckyblock] enabled!");
   }

   public void onDisable() {
      if (this.hologramManager != null) {
         this.hologramManager.stop();
      }

      if (this.cosmeticManager != null) {
         this.cosmeticManager.stop();
      }

      if (this.spinManager != null) {
         this.spinManager.stop();
      }

      if (this.dataManager != null) {
         this.dataManager.save();
      }

      if (this.chestPacketAnimator != null) {
         this.chestPacketAnimator.closeAll();
      }

      this.getLogger().info("[Luckyblock] disabled.");
   }

   private void createConfig() {
      this.configFile = new File(this.getDataFolder(), "config.yml");
      if (!this.configFile.exists()) {
         this.configFile.getParentFile().mkdirs();
         this.saveResource("config.yml", false);
      }

      this.config = YamlConfiguration.loadConfiguration(this.configFile);

      try {
         InputStreamReader r = new InputStreamReader(this.getResource("config.yml"), StandardCharsets.UTF_8);

         try {
            this.config.setDefaults(YamlConfiguration.loadConfiguration(r));
         } catch (Throwable var5) {
            try {
               r.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         r.close();
      } catch (Exception var6) {
      }

   }

   public void reloadConfig() {
      this.config = YamlConfiguration.loadConfiguration(this.configFile);

      try {
         InputStreamReader r = new InputStreamReader(this.getResource("config.yml"), StandardCharsets.UTF_8);

         try {
            this.config.setDefaults(YamlConfiguration.loadConfiguration(r));
         } catch (Throwable var5) {
            try {
               r.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         r.close();
      } catch (Exception var6) {
      }

   }

   public void saveConfig() {
      try {
         this.config.save(this.configFile);
         this.getLogger().info("[Luckyblock] Config saved");
      } catch (Exception e) {
         this.getLogger().severe("[Luckyblock] Cannot save config.yml: " + e.getMessage());
      }

   }

   public FileConfiguration getConfig() {
      if (this.config == null) {
         this.reloadConfig();
      }

      return this.config;
   }

   public PlayerDataManager getData() {
      return this.dataManager;
   }

   public LuckyBlockMenuManager getMenuManager() {
      return this.menuManager;
   }

   public SpinManager getSpinManager() {
      return this.spinManager;
   }

   public CosmeticManager getCosmeticManager() {
      return this.cosmeticManager;
   }

   public HologramManager getHologramManager() {
      return this.hologramManager;
   }

   public ChestPacketAnimator getChestPacketAnimator() {
      return this.chestPacketAnimator;
   }

   public String msg(String key) {
      String raw = this.getConfig().getString("messages." + key, "&cMissing: " + key);
      String var10001 = this.getConfig().getString("messages.prefix", "");
      return ChatColor.translateAlternateColorCodes('&', var10001 + raw);
   }

   public String msgRaw(String key) {
      return ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("messages." + key, ""));
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player p = event.getPlayer();
      String hat = this.dataManager.getEquipped(p.getUniqueId(), CosmeticType.HAT);
      if (hat != null && !hat.isEmpty()) {
         this.cosmeticManager.applyHat(p, hat);
      }

   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      this.cosmeticManager.removeHat(event.getPlayer());
   }

   private boolean isVaultChestBlock(Material mat) {
      if (mat == null) {
         return false;
      } else if (mat == Material.CHEST) {
         return true;
      } else if (mat == Material.TRAPPED_CHEST) {
         return true;
      } else if (mat == Material.ENDER_CHEST) {
         return true;
      } else if (mat == Material.BARREL) {
         return true;
      } else {
         return mat.name().endsWith("_SHULKER_BOX");
      }
   }

   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (command.getName().equalsIgnoreCase("cosmetics")) {
         if (!(sender instanceof Player)) {
            return true;
         } else {
            Player p = (Player)sender;
            this.cosmeticManager.openMenu(p);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
            return true;
         }
      } else if (command.getName().equalsIgnoreCase("cosmeticshop")) {
         if (!(sender instanceof Player)) {
            return true;
         } else {
            Player p = (Player)sender;
            if (this.shopManager == null) {
               p.sendMessage(String.valueOf(ChatColor.RED) + "❌ ShopManager not initialized!");
               return true;
            } else {
               this.shopManager.openShop(p, ShopManager.ShopOrigin.NONE);
               return true;
            }
         }
      } else {
         String cmdName = command.getName().toLowerCase();
         if (!cmdName.equals("luckybox") && !cmdName.equals("lb") && !cmdName.equals("vault") && !cmdName.equals("luckyblocks")) {
            if (!command.getName().equalsIgnoreCase("luckyadmin")) {
               return false;
            } else if (!sender.hasPermission("luckyblock.admin")) {
               sender.sendMessage(this.msg("no_permission"));
               return true;
            } else if (args.length == 0) {
               sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /luckyadmin <give|givecosmetic|list|reload>");
               return true;
            } else {
               switch (args[0].toLowerCase()) {
                  case "reload":
                     this.reloadConfig();
                     this.hologramManager.reload();
                     sender.sendMessage(this.msg("reload_success"));
                     return true;
                  case "list":
                     ConfigurationSection boxes = this.getConfig().getConfigurationSection("boxes");
                     if (boxes == null) {
                        return true;
                     }

                     sender.sendMessage(String.valueOf(ChatColor.GOLD) + "=== Box Types ===");

                     for(String id : boxes.getKeys(false)) {
                        String var79 = String.valueOf(ChatColor.YELLOW);
                        sender.sendMessage(var79 + " • " + id + String.valueOf(ChatColor.GRAY) + " — " + ChatColor.translateAlternateColorCodes('&', boxes.getString(id + ".display_name", id)));
                     }

                     return true;
                  case "give":
                     if (args.length < 4) {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /luckyadmin give <player> <type> <amount>");
                        return true;
                     } else {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        } else {
                           String typeId = args[2].toLowerCase();
                           if (!this.getConfig().contains("boxes." + typeId)) {
                              sender.sendMessage(this.msg("box_type_not_found").replace("{type}", typeId));
                              return true;
                           } else {
                              int amount = Integer.parseInt(args[3]);
                              int added = this.dataManager.addBox(target.getUniqueId(), typeId, amount);
                              String boxName = ChatColor.translateAlternateColorCodes('&', this.getConfig().getString("boxes." + typeId + ".display_name", typeId));
                              if (added == 0) {
                                 long max = this.getConfig().getLong("vault.max_boxes", 1000L);
                                 String var78 = String.valueOf(ChatColor.RED);
                                 sender.sendMessage(var78 + "✗ " + target.getName() + "'s vault is full! (" + max + " boxes max)");
                                 return true;
                              }

                              if (added < amount) {
                                 long max = this.getConfig().getLong("vault.max_boxes", 1000L);
                                 sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "⚠ Vault capacity limited — added only " + added + "/" + amount + " (max: " + max + ")");
                              }

                              sender.sendMessage(this.msg("box_given").replace("{amount}", String.valueOf(added)).replace("{type}", boxName).replace("{player}", target.getName()));
                              target.sendMessage(this.msgRaw("box_given_to_you").replace("{amount}", String.valueOf(added)).replace("{type}", boxName));
                              target.playSound(target.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0F, 1.0F);
                              return true;
                           }
                        }
                     }
                  case "givecosmetic":
                     if (args.length < 3) {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /luckyadmin givecosmetic <player> <id>");
                        return true;
                     } else {
                        Player t = Bukkit.getPlayer(args[1]);
                        if (t == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        } else {
                           if (!this.getConfig().contains("cosmetics." + args[2])) {
                              sender.sendMessage(String.valueOf(ChatColor.RED) + "Cosmetic not found");
                              return true;
                           }

                           this.dataManager.giveCosmetic(t.getUniqueId(), args[2]);
                           String var77 = String.valueOf(ChatColor.GREEN);
                           sender.sendMessage(var77 + "Gave cosmetic " + args[2] + " to " + t.getName());
                           return true;
                        }
                     }
                  case "addcosmetic":
                     if (args.length < 3) {
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /lba addcosmetic <player> <id|all|trail|hat|gadget>");
                        return true;
                     } else {
                        Player t = Bukkit.getPlayer(args[1]);
                        if (t == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        } else {
                           List<String> ids = this.resolveCosmeticIds(args[2]);
                           if (ids.isEmpty()) {
                              String var76 = String.valueOf(ChatColor.RED);
                              sender.sendMessage(var76 + "✗ No cosmetics found for: " + args[2]);
                              return true;
                           }

                           int given = 0;

                           for(String cid : ids) {
                              if (!this.dataManager.ownsCosmetic(t.getUniqueId(), cid)) {
                                 this.dataManager.giveCosmetic(t.getUniqueId(), cid);
                                 ++given;
                              }
                           }

                           String var75 = String.valueOf(ChatColor.GREEN);
                           sender.sendMessage(var75 + "✓ Gave " + given + "/" + ids.size() + " cosmetic(s) to " + t.getName());
                           t.playSound(t.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                           return true;
                        }
                     }
                  case "addchest":
                     if (!(sender instanceof Player)) {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "Only in-game");
                        return true;
                     } else {
                        Player p = (Player)sender;
                        Block target = p.getTargetBlockExact(6);
                        if (target == null) {
                           sender.sendMessage(String.valueOf(ChatColor.RED) + "Look at a block (within 6 blocks)");
                           return true;
                        } else {
                           if (!this.isVaultChestBlock(target.getType())) {
                              sender.sendMessage(String.valueOf(ChatColor.RED) + "✗ Invalid block! Only chest-family allowed:");
                              sender.sendMessage(String.valueOf(ChatColor.GRAY) + "  • CHEST, TRAPPED_CHEST, ENDER_CHEST");
                              sender.sendMessage(String.valueOf(ChatColor.GRAY) + "  • BARREL, SHULKER_BOX (any color)");
                              String var74 = String.valueOf(ChatColor.GRAY);
                              sender.sendMessage(var74 + "  Found: " + target.getType().name());
                              p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                              return true;
                           }

                           this.hologramManager.addChestAt(target.getLocation());
                           String var73 = String.valueOf(ChatColor.GREEN);
                           sender.sendMessage(var73 + "✓ Added vault chest at " + target.getX() + ", " + target.getY() + ", " + target.getZ() + " (" + target.getType().name() + ")");
                           sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Right-click this block to open vault");
                           return true;
                        }
                     }
                  case "removechest":
                     if (!(sender instanceof Player)) {
                        sender.sendMessage(String.valueOf(ChatColor.RED) + "Only in-game");
                        return true;
                     } else {
                        Player p = (Player)sender;
                        Block target = p.getTargetBlockExact(6);
                        if (target == null) {
                           sender.sendMessage(String.valueOf(ChatColor.RED) + "Look at a block");
                           return true;
                        }

                        boolean removed = this.hologramManager.removeChestAt(target.getLocation());
                        sender.sendMessage(removed ? String.valueOf(ChatColor.GREEN) + "Removed vault chest" : String.valueOf(ChatColor.RED) + "This block is not a vault chest");
                        return true;
                     }
                  case "listchests":
                     List<Location> locs = this.hologramManager.getChestLocations();
                     String var71 = String.valueOf(ChatColor.GOLD);
                     sender.sendMessage(var71 + "=== Vault Chests (" + locs.size() + ") ===");

                     for(Location l : locs) {
                        String matName = l.getBlock().getType().name();
                        var71 = String.valueOf(ChatColor.YELLOW);
                        sender.sendMessage(var71 + " • " + l.getWorld().getName() + " @ " + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + String.valueOf(ChatColor.GRAY) + " (" + matName + ")");
                     }

                     return true;
                  case "addcoins":
                     if (args.length < 3) {
                        sender.sendMessage(this.msg("admin.usage_addcoins"));
                        return true;
                     } else {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        } else {
                           long amount;
                           try {
                              amount = Long.parseLong(args[2]);
                           } catch (NumberFormatException var19) {
                              sender.sendMessage(this.msg("admin.invalid_amount"));
                              return true;
                           }

                           if (amount < 0L) {
                              sender.sendMessage(this.msg("admin.amount_negative"));
                              return true;
                           }

                           long actual = this.dataManager.addCoins(target.getUniqueId(), amount);
                           long now = this.dataManager.getCoins(target.getUniqueId());
                           long max = this.getConfig().getLong("economy.max_coins", 100000L);
                           if (actual < amount) {
                              sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "⚠ Cap reached! Only added " + actual + " (max: " + max + ")");
                           }

                           sender.sendMessage(this.msg("admin.coins_added").replace("{amount}", String.valueOf(actual)).replace("{player}", target.getName()).replace("{balance}", String.valueOf(now)));
                           target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&a+&6" + actual + " &acoins! &7(Balance: &e" + now + "&7)"));
                           return true;
                        }
                     }
                  case "removecoins":
                     if (args.length < 3) {
                        sender.sendMessage(this.msg("admin.usage_removecoins"));
                        return true;
                     } else {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        } else {
                           long amount;
                           try {
                              amount = Long.parseLong(args[2]);
                           } catch (NumberFormatException var18) {
                              sender.sendMessage(this.msg("admin.invalid_amount"));
                              return true;
                           }

                           if (amount < 0L) {
                              sender.sendMessage(this.msg("admin.amount_negative"));
                              return true;
                           }

                           long actualRemoved = -this.dataManager.addCoins(target.getUniqueId(), -amount);
                           long now = this.dataManager.getCoins(target.getUniqueId());
                           sender.sendMessage(this.msg("admin.coins_removed").replace("{amount}", String.valueOf(actualRemoved)).replace("{player}", target.getName()).replace("{balance}", String.valueOf(now)));
                           target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c-&6" + actualRemoved + " &ccoins! &7(Balance: &e" + now + "&7)"));
                           return true;
                        }
                     }
                  case "setcoins":
                     if (args.length < 3) {
                        sender.sendMessage(this.msg("admin.usage_setcoins"));
                        return true;
                     } else {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        } else {
                           long amount;
                           try {
                              amount = Long.parseLong(args[2]);
                           } catch (NumberFormatException var17) {
                              sender.sendMessage(this.msg("admin.invalid_amount"));
                              return true;
                           }

                           long max = this.getConfig().getLong("economy.max_coins", 100000L);
                           long clampedAmount = Math.max(0L, Math.min(max, amount));
                           if (clampedAmount != amount) {
                              sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "⚠ Amount clamped to " + clampedAmount + " (max: " + max + ")");
                           }

                           this.dataManager.setCoins(target.getUniqueId(), clampedAmount);
                           sender.sendMessage(this.msg("admin.coins_set").replace("{player}", target.getName()).replace("{balance}", String.valueOf(clampedAmount)));
                           return true;
                        }
                     }
                  case "removebox":
                     if (args.length < 3) {
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /lba removebox <player> <type|all> [amount]");
                        return true;
                     } else {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        } else {
                           String typeArg = args[2].toLowerCase();
                           int amount = args.length >= 4 ? Integer.parseInt(args[3]) : 1;
                           if (typeArg.equals("all")) {
                              int removed = this.dataManager.removeFromAllTypes(target.getUniqueId(), amount);
                              sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Removed " + removed + " box(es) from all types of " + target.getName());
                           } else {
                              if (!this.getConfig().contains("boxes." + typeArg)) {
                                 sender.sendMessage(this.msg("box_type_not_found").replace("{type}", typeArg));
                                 return true;
                              }

                              boolean ok = this.dataManager.removeBox(target.getUniqueId(), typeArg, amount);
                              if (ok) {
                                 sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Removed " + amount + "x " + typeArg + " from " + target.getName());
                              } else {
                                 String var70 = String.valueOf(ChatColor.RED);
                                 sender.sendMessage(var70 + "✗ " + target.getName() + " doesn't have enough " + typeArg + " boxes!");
                              }
                           }

                           return true;
                        }
                     }
                  case "clearbox":
                     if (args.length < 3) {
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /lba clearbox <player> <type>");
                        return true;
                     } else {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        } else {
                           String typeArg = args[2].toLowerCase();
                           if (!this.getConfig().contains("boxes." + typeArg)) {
                              sender.sendMessage(this.msg("box_type_not_found").replace("{type}", typeArg));
                              return true;
                           }

                           int before = this.dataManager.getBoxCount(target.getUniqueId(), typeArg);
                           this.dataManager.clearBoxType(target.getUniqueId(), typeArg);
                           sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Cleared " + before + "x " + typeArg + " from " + target.getName());
                           return true;
                        }
                     }
                  case "clearall":
                     if (args.length < 2) {
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /lba clearall <player>");
                        return true;
                     } else {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        }

                        int total = this.dataManager.getTotalBoxCount(target.getUniqueId());
                        this.dataManager.clearAllBoxes(target.getUniqueId());
                        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Cleared ALL " + total + " box(es) from " + target.getName());
                        return true;
                     }
                  case "removecosmetic":
                     if (args.length < 3) {
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /lba removecosmetic <player> <cosmetic_id>");
                        return true;
                     } else {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        } else {
                           String cosmeticId = args[2].toLowerCase();
                           if (!this.getConfig().contains("cosmetics." + cosmeticId)) {
                              String var69 = String.valueOf(ChatColor.RED);
                              sender.sendMessage(var69 + "✗ Cosmetic not found: " + cosmeticId);
                              return true;
                           }

                           boolean removed = this.dataManager.removeCosmetic(target.getUniqueId(), cosmeticId);
                           if (removed) {
                              sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Removed cosmetic '" + cosmeticId + "' from " + target.getName());
                              target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lLUCKYBLOCK &8» &cCosmetic &e" + cosmeticId + " &chas been removed from your account!"));
                              target.playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 1.0F);
                           } else {
                              String var68 = String.valueOf(ChatColor.RED);
                              sender.sendMessage(var68 + "✗ " + target.getName() + " doesn't own cosmetic: " + cosmeticId);
                           }

                           return true;
                        }
                     }
                  case "clearallcosmetics":
                     if (args.length < 2) {
                        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /lba clearallcosmetics <player>");
                        return true;
                     } else {
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                           sender.sendMessage(this.msg("player_not_found"));
                           return true;
                        }

                        int count = this.dataManager.clearAllCosmetics(target.getUniqueId());
                        if (count > 0) {
                           sender.sendMessage(String.valueOf(ChatColor.GREEN) + "✓ Cleared ALL " + count + " cosmetic(s) from " + target.getName());
                           target.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lLUCKYBLOCK &8» &cAll your cosmetics have been &4removed&c!"));
                           target.playSound(target.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.5F);
                           if (this.getCosmeticManager() != null) {
                              this.getCosmeticManager().removeHat(target);
                              this.getCosmeticManager().updateGadgetItem(target);
                           }
                        } else {
                           String var10001 = String.valueOf(ChatColor.YELLOW);
                           sender.sendMessage(var10001 + "⚠ " + target.getName() + " doesn't own any cosmetics.");
                        }

                        return true;
                     }
                  default:
                     sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Usage: /luckyadmin <give|givecosmetic|addcosmetic|removecosmetic|list|reload|...>");
                     return true;
               }
            }
         } else if (!(sender instanceof Player)) {
            return true;
         } else {
            this.menuManager.openVault((Player)sender, true);
            return true;
         }
      }
   }

   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      List<String> out = new ArrayList();
      if (!command.getName().equalsIgnoreCase("luckyadmin")) {
         return out;
      } else if (!sender.hasPermission("luckyblock.admin")) {
         return out;
      } else if (args.length == 1) {
         String input = args[0].toLowerCase();

         for(String s : new String[]{"give", "givecosmetic", "list", "reload", "addchest", "removechest", "listchests", "addcoins", "removecoins", "setcoins", "removebox", "clearbox", "clearall", "removecosmetic", "clearallcosmetics", "addcosmetics"}) {
            if (s.startsWith(input)) {
               out.add(s);
            }
         }

         return out;
      } else if (args.length == 2) {
         String sub = args[0].toLowerCase();
         boolean needsPlayer = sub.equals("give") || sub.equals("givecosmetic") || sub.equals("addcoins") || sub.equals("removecoins") || sub.equals("setcoins") || sub.equals("removebox") || sub.equals("clearbox") || sub.equals("clearall") || sub.equals("removecosmetic") || sub.equals("clearallcosmetics");
         if (needsPlayer) {
            String input = args[1].toLowerCase();

            for(Player p : Bukkit.getOnlinePlayers()) {
               if (p.getName().toLowerCase().startsWith(input)) {
                  out.add(p.getName());
               }
            }
         }

         return out;
      } else if (args.length == 3) {
         String sub = args[0].toLowerCase();
         String input = args[2].toLowerCase();
         if (sub.equals("addcosmetic") || sub.equals("removecosmetic") || sub.equals("givecosmetic")) {
            ConfigurationSection cosmetics = this.getConfig().getConfigurationSection("cosmetics");
            if (cosmetics != null) {
               for(String id : cosmetics.getKeys(false)) {
                  if (id.startsWith(input)) {
                     out.add(id);
                  }
               }
            }

            for(String s : new String[]{"all", "trail", "hat", "gadget"}) {
               if (s.startsWith(input)) {
                  out.add(s);
               }
            }
         }

         if (!sub.equals("give") && !sub.equals("removebox") && !sub.equals("clearbox")) {
            if (!sub.equals("givecosmetic") && !sub.equals("removecosmetic")) {
               if (sub.equals("addcoins") || sub.equals("removecoins") || sub.equals("setcoins")) {
                  for(String s : new String[]{"100", "500", "1000", "5000", "10000", "50000", "100000"}) {
                     if (s.startsWith(input)) {
                        out.add(s);
                     }
                  }
               }
            } else {
               ConfigurationSection cosmetics = this.getConfig().getConfigurationSection("cosmetics");
               if (cosmetics != null) {
                  for(String id : cosmetics.getKeys(false)) {
                     if (id.startsWith(input)) {
                        out.add(id);
                     }
                  }
               }
            }
         } else {
            ConfigurationSection boxes = this.getConfig().getConfigurationSection("boxes");
            if (boxes != null) {
               for(String id : boxes.getKeys(false)) {
                  if (id.startsWith(input)) {
                     out.add(id);
                  }
               }
            }

            if (sub.equals("removebox") && "all".startsWith(input)) {
               out.add("all");
            }
         }

         return out;
      } else {
         if (args.length == 4) {
            String sub = args[0].toLowerCase();
            String input = args[3].toLowerCase();
            if (sub.equals("give") || sub.equals("removebox")) {
               for(String s : new String[]{"1", "2", "5", "10", "32"}) {
                  if (s.startsWith(input)) {
                     out.add(s);
                  }
               }
            }
         }

         return out;
      }
   }
}
