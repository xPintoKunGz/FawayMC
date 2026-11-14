package org.kcm.bloomery.fawaymc.luckyblock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class ShopManager implements Listener {
   private final Global plugin;
   private final NamespacedKey shopItemKey;
   private final NamespacedKey shopActionKey;
   private final Map<UUID, ShopOrigin> origins = new HashMap();
   private static final List<String> BLOCKED_RARITIES = List.of("legendary", "mythic");
   private static final String AUTO_PREFIX = "auto_";

   private static long getDefaultPrice(String rarity) {
      long var10000;
      switch (rarity.toLowerCase()) {
         case "common" -> var10000 = 1500L;
         case "rare" -> var10000 = 4000L;
         case "epic" -> var10000 = 8000L;
         default -> var10000 = 5000L;
      }

      return var10000;
   }

   public ShopManager(Global plugin) {
      this.plugin = plugin;
      this.shopItemKey = new NamespacedKey(plugin, "shop_item");
      this.shopActionKey = new NamespacedKey(plugin, "shop_action");
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   private List<String> collectShopItemIds() {
      List<String> itemIds = new ArrayList();
      Set<String> addedCosmeticIds = new HashSet();
      ConfigurationSection itemsSec = this.plugin.getConfig().getConfigurationSection("shop.items");
      if (itemsSec != null) {
         for(String id : itemsSec.getKeys(false)) {
            String cosmeticId = this.plugin.getConfig().getString("shop.items." + id + ".cosmetic_id", "");
            if (!cosmeticId.isEmpty()) {
               String rarity = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".rarity", "common").toLowerCase();
               if (BLOCKED_RARITIES.contains(rarity)) {
                  this.plugin.getLogger().info("[Shop] Skipping " + id + " (rarity: " + rarity + " — blocked)");
               } else {
                  itemIds.add(id);
                  addedCosmeticIds.add(cosmeticId);
               }
            }
         }
      }

      ConfigurationSection cosmeticsSec = this.plugin.getConfig().getConfigurationSection("cosmetics");
      if (cosmeticsSec != null) {
         for(String cosmeticId : cosmeticsSec.getKeys(false)) {
            if (!addedCosmeticIds.contains(cosmeticId)) {
               String rarity = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".rarity", "common").toLowerCase();
               if (!BLOCKED_RARITIES.contains(rarity)) {
                  itemIds.add("auto_" + cosmeticId);
                  this.plugin.getLogger().info("[Shop] Auto-added: " + cosmeticId + " (" + rarity + ")");
               }
            }
         }
      }

      return itemIds;
   }

   public void openShop(Player player, ShopOrigin origin) {
      if (!this.plugin.getConfig().getBoolean("shop.enabled", true)) {
         player.sendMessage(this.plugin.msg("shop.disabled"));
      } else {
         this.origins.put(player.getUniqueId(), origin);
         List<String> itemIds = this.collectShopItemIds();
         if (itemIds.isEmpty()) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "Shop has no items available!");
         } else {
            int size = 54;
            ShopHolder holder = new ShopHolder();
            String title = ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("shop.title", "&8&l✦ &d&lCOSMETIC SHOP &8&l✦"));
            Inventory inv = Bukkit.createInventory(holder, size, title);
            holder.setInventory(inv);
            ItemStack border = this.createBorder();

            for(int i = 0; i < 9; ++i) {
               inv.setItem(i, border);
            }

            for(int i = 45; i < 54; ++i) {
               inv.setItem(i, border);
            }

            List<Integer> availableSlots = new ArrayList();

            for(int slot = 9; slot <= 44; ++slot) {
               availableSlots.add(slot);
            }

            long balance = this.plugin.getData().getCoins(player.getUniqueId());
            int itemCount = itemIds.size();
            if (itemCount > availableSlots.size()) {
               this.plugin.getLogger().warning("[Shop] ⚠ มี " + itemCount + " items แต่มีแค่ " + availableSlots.size() + " slots — แสดงแค่ " + availableSlots.size() + " ตัวแรก");
            }

            int index = 0;

            for(String id : itemIds) {
               if (index >= availableSlots.size()) {
                  break;
               }

               int slot = (Integer)availableSlots.get(index);
               inv.setItem(slot, this.createShopItem(player, id, balance));
               ++index;
            }

            int lastRow = 45;
            inv.setItem(lastRow + 4, this.createInfoItem(balance));
            if (origin != ShopManager.ShopOrigin.NONE) {
               inv.setItem(lastRow + 8, this.createBackButton(origin));
            }

            player.openInventory(inv);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
         }
      }
   }

   private ItemStack createBorder() {
      Material mat = Material.matchMaterial(this.plugin.getConfig().getString("shop.border.material", "BLACK_STAINED_GLASS_PANE"));
      if (mat == null) {
         mat = Material.BLACK_STAINED_GLASS_PANE;
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(" ");
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createShopItem(Player player, String shopItemId, long balance) {
      boolean isAuto = shopItemId.startsWith("auto_");
      String cosmeticId;
      String name;
      long price;
      Material mat;
      if (isAuto) {
         cosmeticId = shopItemId.substring("auto_".length());
         String cosPath = "cosmetics." + cosmeticId;
         name = this.plugin.getConfig().getString(cosPath + ".display_name", cosmeticId);
         String matName = this.plugin.getConfig().getString(cosPath + ".material", "STONE");
         mat = Material.matchMaterial(matName);
         if (mat == null) {
            mat = Material.STONE;
         }

         String rarity = this.plugin.getConfig().getString(cosPath + ".rarity", "common").toLowerCase();
         price = getDefaultPrice(rarity);
      } else {
         String path = "shop.items." + shopItemId;
         cosmeticId = this.plugin.getConfig().getString(path + ".cosmetic_id", "");
         name = this.plugin.getConfig().getString(path + ".name", shopItemId);
         String matName = this.plugin.getConfig().getString(path + ".material", "STONE");
         mat = Material.matchMaterial(matName);
         if (mat == null) {
            mat = Material.STONE;
         }

         price = this.plugin.getConfig().getLong(path + ".price", 0L);
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         String rarityStr = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".rarity", "common").toLowerCase();
         String rarityColor = this.plugin.getConfig().getString("rarities." + rarityStr + ".color", "&7");
         String rarityName = this.plugin.getConfig().getString("rarities." + rarityStr + ".name", "Common");
         String typeStr = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".type", "TRAIL");
         boolean owned = !cosmeticId.isEmpty() && this.plugin.getData().ownsCosmetic(player.getUniqueId(), cosmeticId);
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
         List<String> lore = new ArrayList();
         lore.add(ChatColor.translateAlternateColorCodes('&', "&7Type: &f" + typeStr));
         lore.add(ChatColor.translateAlternateColorCodes('&', "&7Rarity: " + rarityColor + rarityName));
         lore.add("");
         if (owned) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&a&l✔ ALREADY OWNED"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7You already unlocked this."));
         } else {
            String var10002 = this.formatNumber(price);
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Price: &6" + var10002 + " coins"));
            var10002 = this.formatNumber(balance);
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Balance: &e" + var10002 + " coins"));
            lore.add("");
            if (balance >= price) {
               lore.add(ChatColor.translateAlternateColorCodes('&', "&eClick to purchase!"));
            } else {
               lore.add(ChatColor.translateAlternateColorCodes('&', "&cNot enough coins!"));
               var10002 = this.formatNumber(price - balance);
               lore.add(ChatColor.translateAlternateColorCodes('&', "&7Need &e" + var10002 + " more"));
            }
         }

         meta.setLore(lore);
         if (owned) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
         }

         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         meta.getPersistentDataContainer().set(this.shopItemKey, PersistentDataType.STRING, shopItemId);
         item.setItemMeta(meta);
         return item;
      }
   }

   private ItemStack createInfoItem(long balance) {
      Material mat = Material.matchMaterial(this.plugin.getConfig().getString("shop.info.material", "BOOK"));
      if (mat == null) {
         mat = Material.BOOK;
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("shop.info.name", "&e&lℹ Shop Info")));
         List<String> rawLore = this.plugin.getConfig().getStringList("shop.info.lore");
         List<String> lore = new ArrayList();

         for(String line : rawLore) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line.replace("{balance}", this.formatNumber(balance))));
         }

         if (lore.isEmpty()) {
            String var10002 = this.formatNumber(balance);
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Balance: &6" + var10002 + " coins"));
         }

         meta.setLore(lore);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
         return item;
      }
   }

   private ItemStack createBackButton(ShopOrigin origin) {
      Material mat = Material.matchMaterial(this.plugin.getConfig().getString("shop.back.material", "ARROW"));
      if (mat == null) {
         mat = Material.ARROW;
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', this.plugin.getConfig().getString("shop.back.name", "&a← Back")));
         String var10000;
         switch (origin.ordinal()) {
            case 0 -> var10000 = "&dCosmetics";
            case 1 -> var10000 = "&6Lucky Vault";
            default -> var10000 = "&7previous menu";
         }

         String originName = var10000;
         List<String> rawLore = this.plugin.getConfig().getStringList("shop.back.lore");
         List<String> lore = new ArrayList();

         for(String line : rawLore) {
            lore.add(ChatColor.translateAlternateColorCodes('&', line.replace("{origin}", originName)));
         }

         if (lore.isEmpty()) {
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Return to " + originName));
         }

         meta.setLore(lore);
         meta.getPersistentDataContainer().set(this.shopActionKey, PersistentDataType.STRING, "back");
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
         return item;
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onClick(InventoryClickEvent event) {
      if (event.getView().getTopInventory().getHolder() instanceof ShopHolder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player) {
            Player player = (Player)event.getWhoClicked();
            if (event.getClickedInventory() != null && event.getClickedInventory() == event.getView().getTopInventory()) {
               ItemStack clicked = event.getCurrentItem();
               if (clicked != null && clicked.hasItemMeta()) {
                  String action = (String)clicked.getItemMeta().getPersistentDataContainer().get(this.shopActionKey, PersistentDataType.STRING);
                  if (action != null) {
                     this.handleSpecialAction(player, action);
                  } else {
                     String shopItemId = (String)clicked.getItemMeta().getPersistentDataContainer().get(this.shopItemKey, PersistentDataType.STRING);
                     if (shopItemId != null) {
                        this.purchase(player, shopItemId);
                        this.openShop(player, (ShopOrigin)this.origins.getOrDefault(player.getUniqueId(), ShopManager.ShopOrigin.NONE));
                     }
                  }
               }
            }
         }
      }
   }

   private void handleSpecialAction(Player player, String action) {
      if (action.equals("back")) {
         ShopOrigin origin = (ShopOrigin)this.origins.getOrDefault(player.getUniqueId(), ShopManager.ShopOrigin.NONE);
         player.closeInventory();
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (player.isOnline()) {
               switch (origin.ordinal()) {
                  case 0 -> this.plugin.getCosmeticManager().openMenu(player);
                  case 1 -> this.plugin.getMenuManager().openVault(player);
               }

            }
         }, 2L);
         player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
      }

   }

   private void purchase(Player player, String shopItemId) {
      boolean isAuto = shopItemId.startsWith("auto_");
      String cosmeticId;
      String configPath;
      long price;
      if (isAuto) {
         cosmeticId = shopItemId.substring("auto_".length());
         configPath = "cosmetics." + cosmeticId;
         if (!this.plugin.getConfig().contains(configPath)) {
            return;
         }

         String rarity = this.plugin.getConfig().getString(configPath + ".rarity", "common").toLowerCase();
         price = getDefaultPrice(rarity);
      } else {
         configPath = "shop.items." + shopItemId;
         if (!this.plugin.getConfig().contains(configPath)) {
            return;
         }

         cosmeticId = this.plugin.getConfig().getString(configPath + ".cosmetic_id", "");
         if (cosmeticId.isEmpty()) {
            return;
         }

         price = this.plugin.getConfig().getLong(configPath + ".price", 0L);
      }

      String rarity = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".rarity", "common").toLowerCase();
      if (BLOCKED_RARITIES.contains(rarity)) {
         player.sendMessage(String.valueOf(ChatColor.RED) + "This cosmetic is not available in the shop!");
         player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else if (this.plugin.getData().ownsCosmetic(player.getUniqueId(), cosmeticId)) {
         player.sendMessage(this.plugin.msg("shop.already_owned"));
         player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else {
         long balance = this.plugin.getData().getCoins(player.getUniqueId());
         if (balance < price) {
            player.sendMessage(this.plugin.msg("shop.no_coins").replace("{price}", this.formatNumber(price)).replace("{balance}", this.formatNumber(balance)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         } else {
            String boxId = "";
            int boxAmount = 0;
            if (!isAuto) {
               boxId = this.plugin.getConfig().getString(configPath + ".reward.box_id", "");
               boxAmount = this.plugin.getConfig().getInt(configPath + ".reward.amount", 1);
            }

            if (!boxId.isEmpty()) {
               long maxVault = this.plugin.getConfig().getLong("vault.max_boxes", 1000L);
               int currentBoxes = this.plugin.getData().getTotalBoxCount(player.getUniqueId());
               if ((long)(currentBoxes + boxAmount) > maxVault) {
                  player.sendMessage(String.valueOf(ChatColor.RED) + "✗ Vault full! (" + currentBoxes + "/" + maxVault + ")");
                  player.sendMessage(String.valueOf(ChatColor.GRAY) + "Open some boxes first to free up space.");
                  player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  return;
               }
            }

            this.plugin.getData().addCoins(player.getUniqueId(), -price);
            this.plugin.getData().giveCosmetic(player.getUniqueId(), cosmeticId);
            if (!boxId.isEmpty()) {
               int added = this.plugin.getData().addBox(player.getUniqueId(), boxId, boxAmount);
               if (added < boxAmount) {
                  this.plugin.getData().addCoins(player.getUniqueId(), price);
                  player.sendMessage(String.valueOf(ChatColor.RED) + "✗ Vault full! Transaction cancelled & refunded.");
                  player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  return;
               }
            }

            String displayName;
            if (isAuto) {
               displayName = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".display_name", cosmeticId);
            } else {
               displayName = this.plugin.getConfig().getString(configPath + ".name", shopItemId);
            }

            String coloredName = ChatColor.translateAlternateColorCodes('&', displayName);
            player.sendMessage(this.plugin.msg("shop.purchased").replace("{item}", coloredName).replace("{price}", this.formatNumber(price)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.5F);
         }
      }
   }

   private String formatNumber(long n) {
      if (n >= 1000000L) {
         return String.format("%.1fM", (double)n / (double)1000000.0F);
      } else {
         return n >= 1000L ? String.format("%.1fk", (double)n / (double)1000.0F) : String.valueOf(n);
      }
   }

   public static enum ShopOrigin {
      COSMETICS,
      VAULT,
      NONE;

      // $FF: synthetic method
      private static ShopOrigin[] $values() {
         return new ShopOrigin[]{COSMETICS, VAULT, NONE};
      }
   }

   public static class ShopHolder implements InventoryHolder {
      private Inventory inventory;

      public void setInventory(Inventory inv) {
         this.inventory = inv;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }
}
