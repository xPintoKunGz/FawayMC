package org.kcm.bloomery.fawaymc.luckyblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class LuckyBlockMenuManager implements Listener {
   private final Global plugin;
   private final NamespacedKey vaultBoxKey;
   private final NamespacedKey vaultActionKey;
   private final NamespacedKey quantityKey;
   private final Map<UUID, Boolean> openedFromCommand = new HashMap();
   private final Map<UUID, Integer> selectedQuantity = new HashMap();
   private static final Set<String> GUI_HOLDER_NAMES = Set.of("VaultHolder", "ShopHolder", "SpinGuiHolder", "CosmeticMenuHolder");

   public LuckyBlockMenuManager(Global plugin) {
      this.plugin = plugin;
      this.vaultBoxKey = new NamespacedKey(plugin, "vault_box");
      this.vaultActionKey = new NamespacedKey(plugin, "vault_action");
      this.quantityKey = new NamespacedKey(plugin, "vault_quantity");
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void openVault(Player player, boolean fromCommand) {
      this.openedFromCommand.put(player.getUniqueId(), fromCommand);
      this.openVaultInternal(player);
   }

   public void openVault(Player player) {
      this.openVault(player, false);
   }

   private void openVaultInternal(Player player) {
      VaultHolder holder = new VaultHolder();
      String title = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("menu.title", "&8Lucky Block Vault"));
      int size = this.plugin.getConfig().getInt("menu.size", 45);
      Inventory inv = Bukkit.createInventory(holder, size, title);
      holder.setInventory(inv);
      String borderMatName = this.plugin.getConfig().getString("menu.border.material", "BLACK_CONCRETE");
      Material borderMat = Material.matchMaterial(borderMatName);
      if (borderMat == null) {
         borderMat = Material.BLACK_CONCRETE;
      }

      ItemStack border = new ItemStack(borderMat);
      ItemMeta bm = border.getItemMeta();
      if (bm != null) {
         bm.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("menu.border.name", "&8&l█")));
         bm.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         border.setItemMeta(bm);
      }

      for(int i = 0; i < size; ++i) {
         inv.setItem(i, border);
      }

      int totalBoxes = this.plugin.getData().getTotalBoxCount(player.getUniqueId());
      long maxBoxes = this.plugin.getConfig().getLong("vault.max_boxes", 1000L);
      Map<String, Integer> counts = this.plugin.getData().getAllBoxCounts(player.getUniqueId());
      ConfigurationSection boxesSec = this.plugin.getConfig().getConfigurationSection("boxes");
      if (boxesSec != null) {
         for(String id : boxesSec.getKeys(false)) {
            int slot = boxesSec.getInt(id + ".menu_slot", -1);
            if (slot >= 0 && slot < size) {
               int count = (Integer)counts.getOrDefault(id, 0);
               inv.setItem(slot, this.createBoxIcon(player, id, count, totalBoxes, maxBoxes));
            }
         }
      }

      int[] qtySlots = new int[]{28, 30, 32, 34};
      int[] qtyValues = new int[]{1, 5, 10, 25};
      int currentQty = (Integer)this.selectedQuantity.getOrDefault(player.getUniqueId(), 1);

      for(int i = 0; i < qtyValues.length; ++i) {
         if (qtySlots[i] < size) {
            inv.setItem(qtySlots[i], this.createQuantityButton(qtyValues[i], currentQty));
         }
      }

      int infoSlot = this.plugin.getConfig().getInt("menu.info_slot", 40);
      inv.setItem(infoSlot, this.createInfoItem(player, totalBoxes, maxBoxes));
      int shopSlot = this.plugin.getConfig().getInt("shop.shortcut.vault_slot", 41);
      if (shopSlot >= 0 && shopSlot < size && shopSlot != infoSlot) {
         inv.setItem(shopSlot, this.createShopShortcut());
      }

      player.openInventory(inv);
      player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0F, 1.0F);
   }

   private ItemStack createQuantityButton(int amount, int selected) {
      Material mat;
      String color;
      String tier;
      if (amount == 1) {
         mat = Material.LIME_DYE;
         color = "&a";
         tier = "Single";
      } else if (amount == 5) {
         mat = Material.YELLOW_DYE;
         color = "&e";
         tier = "Multi";
      } else if (amount == 10) {
         mat = Material.ORANGE_DYE;
         color = "&6";
         tier = "Multi";
      } else {
         mat = Material.RED_DYE;
         color = "&c";
         tier = "Multi";
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         String prefix = amount == selected ? "&l" : "";
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', color + prefix + "✦ " + amount + "x Roll"));
         List<String> lore = new ArrayList();
         lore.add(ChatColor.translateAlternateColorCodes('&', "&7Open &e" + amount + " &7boxes at once"));
         lore.add(ChatColor.translateAlternateColorCodes('&', "&7Tier: &f" + tier));
         lore.add("");
         if (amount == selected) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&a&l► SELECTED"));
         } else {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to select"));
         }

         meta.setLore(lore);
         if (amount == selected) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
         }

         meta.getPersistentDataContainer().set(this.quantityKey, PersistentDataType.INTEGER, amount);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
         return item;
      }
   }

   private ItemStack createInfoItem(Player player, int totalBoxes, long maxBoxes) {
      Material mat = Material.matchMaterial(this.plugin.getConfig().getString("menu.info_material", "BOOK"));
      if (mat == null) {
         mat = Material.BOOK;
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("menu.info_name", "&e&lℹ Vault Info")));
         List<String> lore = new ArrayList();
         double ratio = maxBoxes > 0L ? (double)totalBoxes / (double)maxBoxes : (double)0.0F;
         String barColor = ratio >= 0.9 ? "&c" : (ratio >= 0.7 ? "&e" : "&a");
         String bar = this.buildProgressBar(ratio, 20, barColor);
         lore.add(ChatColor.translateAlternateColorCodes('&', "&7Storage:"));
         lore.add(ChatColor.translateAlternateColorCodes('&', " " + bar + " " + barColor + totalBoxes + "&7/&f" + maxBoxes));
         lore.add("");
         List<String> configLore = this.plugin.getConfig().getStringList("menu.info_lore");
         if (!configLore.isEmpty()) {
            for(String line : configLore) {
               lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }

            lore.add("");
         }

         lore.add(ChatColor.translateAlternateColorCodes('&', "&d&lHOW TO USE?"));
         lore.add(ChatColor.translateAlternateColorCodes('&', " &7• &fSelect quantity above"));
         lore.add(ChatColor.translateAlternateColorCodes('&', " &7• &fClick a box to open"));
         lore.add(ChatColor.translateAlternateColorCodes('&', " &7• &fBoxes drop random loot"));
         lore.add(ChatColor.translateAlternateColorCodes('&', " &7• &fHigher tier = better loot"));
         lore.add("");
         if ((long)totalBoxes >= maxBoxes) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&c⚠ &lVAULT FULL!"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&cOpen boxes to free space."));
         } else if (ratio >= 0.9) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&6⚠ &lAlmost full!"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Space left: &e" + (maxBoxes - (long)totalBoxes)));
         }

         meta.setLore(lore);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
         return item;
      }
   }

   private String buildProgressBar(double ratio, int length, String color) {
      ratio = Math.max((double)0.0F, Math.min((double)1.0F, ratio));
      int filled = (int)Math.round(ratio * (double)length);
      StringBuilder sb = new StringBuilder();
      sb.append(color);

      for(int i = 0; i < filled; ++i) {
         sb.append("█");
      }

      sb.append("&8");

      for(int i = filled; i < length; ++i) {
         sb.append("▒");
      }

      return sb.toString();
   }

   private ItemStack createBoxIcon(Player player, String id, int count, int totalBoxes, long maxBoxes) {
      ConfigurationSection cfg = this.plugin.getConfig().getConfigurationSection("boxes." + id);
      if (cfg == null) {
         return new ItemStack(Material.CHEST);
      } else {
         String matName = cfg.getString("material", "CHEST");
         Material mat = Material.matchMaterial(matName);
         if (mat == null) {
            mat = Material.CHEST;
         }

         ItemStack item = new ItemStack(mat);
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return item;
         } else {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', cfg.getString("display_name", id)));
            String rarityStr = cfg.getString("rarity", "common");
            String rarityColor = this.plugin.getConfig().getString("rarities." + rarityStr.toLowerCase() + ".color", "&7");
            String rarityName = this.plugin.getConfig().getString("rarities." + rarityStr.toLowerCase() + ".name", "Common");
            List<String> lore = new ArrayList();
            lore.add(ChatColor.translateAlternateColorCodes('&', " &7Rarity: " + rarityColor + rarityName));
            int selectedQty = (Integer)this.selectedQuantity.getOrDefault(player.getUniqueId(), 1);
            if (count > 0) {
               lore.add(ChatColor.translateAlternateColorCodes('&', " &fCurrent boxes: &a" + count + "x " + cfg.getString("display_name", id)));
               lore.add("");
               if (count < selectedQty) {
                  lore.add(ChatColor.translateAlternateColorCodes('&', " &c✘ Need " + selectedQty + " but only have " + count));
               } else {
                  lore.add(ChatColor.translateAlternateColorCodes('&', " &aOpen " + selectedQty + "x &7(" + (count - selectedQty) + " remaining)"));
                  lore.add(ChatColor.translateAlternateColorCodes('&', " &a&l► CLICK TO OPEN"));
               }
            } else {
               lore.add(ChatColor.translateAlternateColorCodes('&', " &fCurrent boxes: &c0"));
               lore.add("");
               lore.add(ChatColor.translateAlternateColorCodes('&', " &c✘ You don't have this box"));
               lore.add(ChatColor.translateAlternateColorCodes('&', " &7Buy it from the &dShop&7!"));
            }

            meta.setLore(lore);
            if (cfg.getBoolean("glow", false) || count > 0) {
               meta.addEnchant(Enchantment.UNBREAKING, 1, true);
               meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }

            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            meta.getPersistentDataContainer().set(this.vaultBoxKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
            return item;
         }
      }
   }

   private ItemStack createShopShortcut() {
      ItemStack item = new ItemStack(Material.EMERALD);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&l✦ COSMETIC SHOP"));
         List<String> lore = new ArrayList();
         lore.add(ChatColor.translateAlternateColorCodes('&', "&7Purchase cosmetics using"));
         lore.add(ChatColor.translateAlternateColorCodes('&', "&6coins &7earned from boxes."));
         lore.add("");
         lore.add(ChatColor.translateAlternateColorCodes('&', "&7Available tiers:"));
         lore.add(ChatColor.translateAlternateColorCodes('&', " &8▪ &9Rare"));
         lore.add(ChatColor.translateAlternateColorCodes('&', " &8▪ &5Epic"));
         lore.add("");
         lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to open!"));
         meta.setLore(lore);
         meta.getPersistentDataContainer().set(this.vaultActionKey, PersistentDataType.STRING, "shop");
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
      }

      return item;
   }

   @EventHandler
   public void onVaultClose(InventoryCloseEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof VaultHolder) {
         if (event.getPlayer() instanceof Player) {
            UUID uuid = event.getPlayer().getUniqueId();
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               Player p = Bukkit.getPlayer(uuid);
               if (p == null) {
                  this.openedFromCommand.remove(uuid);
               } else {
                  Inventory top = p.getOpenInventory().getTopInventory();
                  if (top != null && top.getHolder() != null) {
                     String holderName = top.getHolder().getClass().getSimpleName();
                     if (!GUI_HOLDER_NAMES.contains(holderName)) {
                        this.openedFromCommand.remove(uuid);
                     }
                  } else {
                     this.openedFromCommand.remove(uuid);
                  }
               }
            }, 2L);
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onClick(InventoryClickEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof VaultHolder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            if (event.getClickedInventory() != null && event.getClickedInventory() == event.getView().getTopInventory()) {
               ItemStack clicked = event.getCurrentItem();
               if (clicked != null && clicked.hasItemMeta()) {
                  Inventory inv = event.getView().getTopInventory();
                  Integer qty = (Integer)clicked.getItemMeta().getPersistentDataContainer().get(this.quantityKey, PersistentDataType.INTEGER);
                  if (qty != null) {
                     this.selectedQuantity.put(player.getUniqueId(), qty);
                     player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
                     int[] qtySlots = new int[]{28, 30, 32, 34};
                     int[] qtyValues = new int[]{1, 5, 10, 25};

                     for(int i = 0; i < qtyValues.length; ++i) {
                        if (qtySlots[i] < inv.getSize()) {
                           inv.setItem(qtySlots[i], this.createQuantityButton(qtyValues[i], qty));
                        }
                     }

                     int totalBoxes = this.plugin.getData().getTotalBoxCount(player.getUniqueId());
                     long maxBoxes = this.plugin.getConfig().getLong("vault.max_boxes", 1000L);
                     Map<String, Integer> counts = this.plugin.getData().getAllBoxCounts(player.getUniqueId());
                     ConfigurationSection boxesSec = this.plugin.getConfig().getConfigurationSection("boxes");
                     if (boxesSec != null) {
                        for(String id : boxesSec.getKeys(false)) {
                           int slot = boxesSec.getInt(id + ".menu_slot", -1);
                           if (slot >= 0 && slot < inv.getSize()) {
                              int count = (Integer)counts.getOrDefault(id, 0);
                              inv.setItem(slot, this.createBoxIcon(player, id, count, totalBoxes, maxBoxes));
                           }
                        }
                     }

                  } else {
                     String action = (String)clicked.getItemMeta().getPersistentDataContainer().get(this.vaultActionKey, PersistentDataType.STRING);
                     if ("shop".equals(action)) {
                        player.closeInventory();
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                           if (player.isOnline()) {
                              this.plugin.getShopManager().openShop(player, ShopManager.ShopOrigin.VAULT);
                           }

                        }, 2L);
                     } else {
                        String boxId = (String)clicked.getItemMeta().getPersistentDataContainer().get(this.vaultBoxKey, PersistentDataType.STRING);
                        if (boxId != null) {
                           int count = this.plugin.getData().getBoxCount(player.getUniqueId(), boxId);
                           if (count <= 0) {
                              String displayName = clicked.getItemMeta().getDisplayName();
                              player.sendMessage(this.plugin.msg("no_boxes").replace("{type}", ChatColor.stripColor(displayName)));
                              player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                           } else {
                              int selectedQty = (Integer)this.selectedQuantity.getOrDefault(player.getUniqueId(), 1);
                              if (count < selectedQty) {
                                 String boxName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("boxes." + boxId + ".display_name", boxId)));
                                 player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&c&lLUCKYBLOCK &8» &cYou need &e" + selectedQty + " boxes &cbut only have &e" + count + " &cof " + boxName + "!"));
                                 player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                              } else {
                                 Boolean flag = (Boolean)this.openedFromCommand.remove(player.getUniqueId());
                                 boolean useGui = flag != null && flag;
                                 player.closeInventory();
                                 if (useGui) {
                                    this.plugin.getSpinManager().openGuiSpin(player, boxId, selectedQty);
                                 } else {
                                    this.plugin.getSpinManager().openSpin(player, boxId, selectedQty);
                                 }

                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   public static class VaultHolder implements InventoryHolder {
      private Inventory inventory;

      public void setInventory(Inventory inv) {
         this.inventory = inv;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }
}
