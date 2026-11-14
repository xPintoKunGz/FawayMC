package org.kcm.bloomery.fawaymc.advancement.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.kcm.bloomery.fawaymc.advancement.Global;
import org.kcm.bloomery.fawaymc.advancement.data.Advancement;
import org.kcm.bloomery.fawaymc.advancement.data.PlayerProgress;

public class AdvancementCategoryGui {
   public static final NamespacedKey CATEGORY_KEY = new NamespacedKey("fawayadvancement", "category_id");
   private final Global plugin;

   public AdvancementCategoryGui(Global plugin) {
      this.plugin = plugin;
   }

   public void open(Player player) {
      Holder holder = new Holder();
      String title = this.color(this.plugin.getConfig().getString("display.title", "&8&l✦ &6&lADVANCEMENTS &8&l✦"));
      Inventory inv = Bukkit.createInventory(holder, 54, title);
      holder.setInventory(inv);
      ItemStack border = this.pane(Material.BLACK_STAINED_GLASS_PANE);
      ItemStack filler = this.pane(Material.GRAY_STAINED_GLASS_PANE);

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
      }

      for(int i = 45; i < 54; ++i) {
         inv.setItem(i, border);
      }

      for(int i = 9; i < 45; ++i) {
         inv.setItem(i, filler);
      }

      ConfigurationSection cats = this.plugin.getConfig().getConfigurationSection("categories");
      if (cats != null) {
         for(String id : cats.getKeys(false)) {
            int slot = cats.getInt(id + ".slot", -1);
            if (slot >= 0 && slot < 54) {
               inv.setItem(slot, this.buildCategoryItem(player, id, cats.getConfigurationSection(id)));
            }
         }
      }

      inv.setItem(49, this.buildSummary(player));

      try {
         player.setItemOnCursor((ItemStack)null);
      } catch (Exception var11) {
      }

      player.openInventory(inv);
   }

   private ItemStack buildCategoryItem(Player player, String id, ConfigurationSection cfg) {
      if (cfg == null) {
         return new ItemStack(Material.STONE);
      } else {
         Material mat = Material.matchMaterial(cfg.getString("icon", "STONE"));
         if (mat == null) {
            mat = Material.STONE;
         }

         String name = this.color(cfg.getString("name", id));
         String desc = this.color(cfg.getString("description", ""));
         List<Advancement> advs = this.plugin.getAdvancementManager().getByCategory(id);
         PlayerProgress prog = this.plugin.getDataManager().get(player.getUniqueId());
         int total = advs.size();
         int unlocked = 0;

         for(Advancement a : advs) {
            if (prog.isUnlocked(a.getId())) {
               ++unlocked;
            }
         }

         int percent = total > 0 ? (int)Math.round((double)unlocked * (double)100.0F / (double)total) : 0;
         ItemStack item = new ItemStack(mat);
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return item;
         } else {
            meta.setDisplayName(name);
            List<String> lore = new ArrayList();
            lore.add(desc);
            lore.add("");
            lore.add(this.color("&7Progress: &e" + unlocked + "&7/&f" + total + " &7(" + percent + "%)"));
            lore.add(this.color("&7Status: " + (unlocked == total && total > 0 ? "&a&l✔ COMPLETE" : "&6In Progress")));
            lore.add("");
            lore.add(this.color("&eClick to view!"));
            meta.setLore(lore);
            if (unlocked == total && total > 0) {
               meta.addEnchant(Enchantment.UNBREAKING, 1, true);
               meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            }

            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            meta.getPersistentDataContainer().set(CATEGORY_KEY, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
            return item;
         }
      }
   }

   private ItemStack buildSummary(Player player) {
      ItemStack item = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta)item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         meta.setOwningPlayer(player);
         meta.setDisplayName(this.color("&6&l✦ &e&l" + player.getName() + " &6&l✦"));
         PlayerProgress prog = this.plugin.getDataManager().get(player.getUniqueId());
         int total = this.plugin.getAdvancementManager().getAdvancementCount();
         int unlocked = prog.getUnlocked().size();
         int percent = total > 0 ? (int)Math.round((double)unlocked * (double)100.0F / (double)total) : 0;
         List<String> lore = new ArrayList();
         lore.add(this.color("&7Advancements: &e" + unlocked + "&7/&f" + total));
         lore.add(this.color("&7Completion: &e" + percent + "%"));
         lore.add("");
         lore.add(this.color("&7Playtime: &e" + prog.getPlaytimeMinutes() + "m"));
         meta.setLore(lore);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
         return item;
      }
   }

   private ItemStack pane(Material mat) {
      ItemStack i = new ItemStack(mat);
      ItemMeta m = i.getItemMeta();
      if (m != null) {
         m.setDisplayName(" ");
         i.setItemMeta(m);
      }

      return i;
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }

   public static class Holder implements InventoryHolder {
      private Inventory inventory;

      public void setInventory(Inventory inv) {
         this.inventory = inv;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }
}
