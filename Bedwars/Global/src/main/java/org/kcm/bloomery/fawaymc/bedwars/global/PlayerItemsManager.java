package org.kcm.bloomery.fawaymc.bedwars.global;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

public class PlayerItemsManager {
   private final Global plugin;
   private final ItemsConfigManager itemsConfig;
   private final NamespacedKey cosmeticKey;
   private final Set<UUID> hiddenPlayers = new HashSet();

   public PlayerItemsManager(Global plugin, ItemsConfigManager itemsConfig) {
      this.plugin = plugin;
      this.itemsConfig = itemsConfig;
      this.cosmeticKey = new NamespacedKey(plugin, "cosmetic_item");
   }

   private FileConfiguration cfg() {
      return this.itemsConfig.getConfig();
   }

   public boolean isSelectorItem(ItemStack item) {
      if (!this.cfg().getBoolean("gui.open_item.enabled", true)) {
         return false;
      } else if (item != null && item.hasItemMeta()) {
         String expectedName = ChatColor.translateAlternateColorCodes('&', this.cfg().getString("gui.open_item.name", ""));
         return item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equals(expectedName);
      } else {
         return false;
      }
   }

   public boolean isProfileItem(ItemStack item) {
      if (!this.cfg().getBoolean("profile.item.enabled", true)) {
         return false;
      } else if (item != null && item.hasItemMeta()) {
         String expectedName = ChatColor.translateAlternateColorCodes('&', this.cfg().getString("profile.item.name", ""));
         return item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equals(expectedName);
      } else {
         return false;
      }
   }

   public boolean isVisibilityItem(ItemStack item) {
      if (!this.cfg().getBoolean("visibility.item.enabled", true)) {
         return false;
      } else if (item != null && item.hasItemMeta()) {
         String shownName = ChatColor.translateAlternateColorCodes('&', this.cfg().getString("visibility.item.shown_name", ""));
         String hiddenName = ChatColor.translateAlternateColorCodes('&', this.cfg().getString("visibility.item.hidden_name", ""));
         if (!item.getItemMeta().hasDisplayName()) {
            return false;
         } else {
            String displayName = item.getItemMeta().getDisplayName();
            return displayName.equals(shownName) || displayName.equals(hiddenName);
         }
      } else {
         return false;
      }
   }

   public boolean isCosmeticsItem(ItemStack item) {
      return item != null && item.hasItemMeta() ? item.getItemMeta().getPersistentDataContainer().has(this.cosmeticKey, PersistentDataType.BYTE) : false;
   }

   public boolean isAnySpecialItem(ItemStack item) {
      return this.isSelectorItem(item) || this.isProfileItem(item) || this.isVisibilityItem(item) || this.isCosmeticsItem(item);
   }

   public void givePlayerItems(Player player) {
      if (this.cfg().getBoolean("gui.open_item.enabled", true)) {
         int slot = this.cfg().getInt("gui.open_item.slot", 4);
         String matName = this.cfg().getString("gui.open_item.material", "NETHER_STAR");
         String name = ChatColor.translateAlternateColorCodes('&', this.cfg().getString("gui.open_item.name", "&bServer Selector"));
         List<String> rawLore = this.cfg().getStringList("gui.open_item.lore");
         List<String> lore = new ArrayList();

         for(String l : rawLore) {
            lore.add(ChatColor.translateAlternateColorCodes('&', l));
         }

         Material material = Material.matchMaterial(matName);
         if (material == null) {
            material = Material.NETHER_STAR;
         }

         ItemStack item = new ItemStack(material);
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            item.setItemMeta(meta);
         }

         player.getInventory().setItem(slot, item);
      }

      if (this.cfg().getBoolean("profile.item.enabled", true)) {
         int slot = this.cfg().getInt("profile.item.slot", 0);
         String name = ChatColor.translateAlternateColorCodes('&', this.cfg().getString("profile.item.name", "&eMy Profile"));
         List<String> rawLore = this.cfg().getStringList("profile.item.lore");
         List<String> lore = new ArrayList();

         for(String l : rawLore) {
            lore.add(ChatColor.translateAlternateColorCodes('&', l));
         }

         ItemStack head = new ItemStack(Material.PLAYER_HEAD);
         SkullMeta meta = (SkullMeta)head.getItemMeta();
         if (meta != null) {
            meta.setOwningPlayer(player);
            meta.setDisplayName(name);
            meta.setLore(lore);
            head.setItemMeta(meta);
         }

         player.getInventory().setItem(slot, head);
      }

      if (this.cfg().getBoolean("cosmetics_item.enabled", true)) {
         int slot = this.cfg().getInt("cosmetics_item.slot", 3);
         String matName = this.cfg().getString("cosmetics_item.material", "NAME_TAG");
         String name = ChatColor.translateAlternateColorCodes('&', this.cfg().getString("cosmetics_item.name", "&d&l✦ Cosmetics &7(Right Click)"));
         List<String> rawLore = this.cfg().getStringList("cosmetics_item.lore");
         List<String> lore = new ArrayList();

         for(String l : rawLore) {
            lore.add(ChatColor.translateAlternateColorCodes('&', l));
         }

         Material material = Material.matchMaterial(matName);
         if (material == null) {
            material = Material.NAME_TAG;
         }

         ItemStack item = new ItemStack(material);
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(this.cosmeticKey, PersistentDataType.BYTE, (byte)1);
            item.setItemMeta(meta);
         }

         player.getInventory().setItem(slot, item);
      }

      this.updateVisibilityItem(player);
   }

   public void updateVisibilityItem(Player player) {
      if (this.cfg().getBoolean("visibility.item.enabled", true)) {
         int slot = this.cfg().getInt("visibility.item.slot", 8);
         boolean hidden = this.hiddenPlayers.contains(player.getUniqueId());
         String matName = hidden ? this.cfg().getString("visibility.item.hidden_material", "GRAY_DYE") : this.cfg().getString("visibility.item.shown_material", "LIME_DYE");
         String name = hidden ? this.cfg().getString("visibility.item.hidden_name", "&cPlayers: Hidden") : this.cfg().getString("visibility.item.shown_name", "&aPlayers: Shown");
         List<String> rawLore = hidden ? this.cfg().getStringList("visibility.item.hidden_lore") : this.cfg().getStringList("visibility.item.shown_lore");
         List<String> lore = new ArrayList();

         for(String l : rawLore) {
            lore.add(ChatColor.translateAlternateColorCodes('&', l));
         }

         Material material = Material.matchMaterial(matName);
         if (material == null) {
            material = hidden ? Material.GRAY_DYE : Material.LIME_DYE;
         }

         ItemStack item = new ItemStack(material);
         ItemMeta meta = item.getItemMeta();
         if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(lore);
            item.setItemMeta(meta);
         }

         player.getInventory().setItem(slot, item);
      }
   }

   public void togglePlayerVisibility(Player player) {
      UUID uuid = player.getUniqueId();
      if (this.hiddenPlayers.contains(uuid)) {
         this.hiddenPlayers.remove(uuid);

         for(Player target : this.plugin.getServer().getOnlinePlayers()) {
            player.showPlayer(this.plugin, target);
         }

         player.sendMessage(ChatColor.translateAlternateColorCodes('&', this.cfg().getString("visibility.messages.shown", "&aแสดงผู้เล่นทั้งหมดแล้ว!")));
         player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
      } else {
         this.hiddenPlayers.add(uuid);

         for(Player target : this.plugin.getServer().getOnlinePlayers()) {
            player.hidePlayer(this.plugin, target);
         }

         player.sendMessage(ChatColor.translateAlternateColorCodes('&', this.cfg().getString("visibility.messages.hidden", "&cซ่อนผู้เล่นคนอื่นทั้งหมดแล้ว!")));
         player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 0.5F);
      }

      this.updateVisibilityItem(player);
   }

   public boolean isHidden(Player player) {
      return this.hiddenPlayers.contains(player.getUniqueId());
   }

   public void onJoin(Player player) {
      for(UUID hiddenUuid : this.hiddenPlayers) {
         Player hidingPlayer = this.plugin.getServer().getPlayer(hiddenUuid);
         if (hidingPlayer != null) {
            hidingPlayer.hidePlayer(this.plugin, player);
         }
      }

   }

   public void onQuit(Player player) {
      this.hiddenPlayers.remove(player.getUniqueId());
   }
}
