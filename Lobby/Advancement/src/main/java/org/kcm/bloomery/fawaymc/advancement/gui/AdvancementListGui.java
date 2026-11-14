package org.kcm.bloomery.fawaymc.advancement.gui;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.kcm.bloomery.fawaymc.advancement.Global;
import org.kcm.bloomery.fawaymc.advancement.data.Advancement;
import org.kcm.bloomery.fawaymc.advancement.data.PlayerProgress;

public class AdvancementListGui {
   public static final NamespacedKey BACK_KEY = new NamespacedKey("advancement", "back_btn");
   private final Global plugin;

   public AdvancementListGui(Global plugin) {
      this.plugin = plugin;
   }

   public void open(Player player, String category) {
      Holder holder = new Holder();
      holder.setCategory(category);
      String catName = this.plugin.getConfig().getString("categories." + category + ".name", category);
      String title = this.color(this.plugin.getConfig().getString("display.category_title", "&8&l✦ &6{category} &8&l✦").replace("{category}", catName));
      Inventory inv = Bukkit.createInventory(holder, 54, title);
      holder.setInventory(inv);
      ItemStack border = this.pane(Material.BLACK_STAINED_GLASS_PANE);

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
      }

      for(int i = 45; i < 54; ++i) {
         inv.setItem(i, border);
      }

      List<Advancement> advs = this.plugin.getAdvancementManager().getByCategory(category);
      int idx = 0;

      for(int slot = 9; slot <= 44 && idx < advs.size(); ++slot) {
         inv.setItem(slot, this.buildAdvancementItem(player, (Advancement)advs.get(idx)));
         ++idx;
      }

      ItemStack back = new ItemStack(Material.ARROW);
      ItemMeta bm = back.getItemMeta();
      if (bm != null) {
         bm.setDisplayName(this.color("&e← Back"));
         bm.getPersistentDataContainer().set(BACK_KEY, PersistentDataType.STRING, "back");
         back.setItemMeta(bm);
      }

      inv.setItem(45, back);

      try {
         player.setItemOnCursor((ItemStack)null);
      } catch (Exception var13) {
      }

      player.openInventory(inv);
   }

   private ItemStack buildAdvancementItem(Player player, Advancement adv) {
      PlayerProgress prog = this.plugin.getDataManager().get(player.getUniqueId());
      boolean unlocked = prog.isUnlocked(adv.getId());
      int current = prog.getProgress(adv.getId());
      int required = adv.getRequired();
      int percent = required > 0 ? Math.min(100, current * 100 / required) : 0;
      ItemStack item = new ItemStack(adv.getIcon());
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         FileConfiguration var10000 = this.plugin.getConfig();
         String var10001 = adv.getTier();
         String tierColor = var10000.getString("tiers." + var10001 + ".color", "&7");
         var10000 = this.plugin.getConfig();
         var10001 = adv.getTier();
         String tierName = var10000.getString("tiers." + var10001 + ".name", "Common");
         String prefix = unlocked ? "&a&l✔ " : "&7&l\ud83d\udd12 ";
         meta.setDisplayName(this.color(prefix + adv.getName()));
         List<String> lore = new ArrayList();
         lore.add(this.color(adv.getDescription()));
         lore.add("");
         lore.add(this.color("&7Tier: " + tierColor + tierName));
         lore.add("");
         lore.add(this.color("&6&l\ud83d\udce6 REWARDS:"));
         List<String> rewardLines = this.buildRewardPreviewLines(adv);
         if (rewardLines.isEmpty()) {
            lore.add(this.color(" &8▪ &7No reward"));
         } else {
            lore.addAll(rewardLines);
         }

         lore.add("");
         if (unlocked) {
            lore.add(this.color("&a&l✔ UNLOCKED"));
         } else {
            lore.add(this.color("&7Progress: &e" + current + "&7/&f" + required + " &7(" + percent + "%)"));
         }

         meta.setLore(lore);
         if (unlocked) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
         }

         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
         return item;
      }
   }

   private List<String> buildRewardPreviewLines(Advancement adv) {
      List<String> lines = new ArrayList();
      ConfigurationSection reward = adv.getRewardConfig();
      if (reward == null) {
         return lines;
      } else {
         long coins = reward.getLong("coins", 0L);
         long exp = reward.getLong("exp", 0L);
         if (coins > 0L) {
            lines.add(this.color(" &8▪ &6" + coins + " Coins"));
         }

         if (exp > 0L) {
            lines.add(this.color(" &8▪ &e" + exp + " EXP"));
         }

         String tier = adv.getTier().toLowerCase();
         String boxType = null;
         int amount = 0;
         if (tier.equals("rare")) {
            boxType = "common";
            amount = 1;
         } else if (tier.equals("epic")) {
            boxType = "rare";
            amount = 1;
         }

         ConfigurationSection vault = reward.getConfigurationSection("vault");
         if (vault != null) {
            String customBox = vault.getString("box");
            if (customBox != null && !customBox.isEmpty()) {
               boxType = customBox;
            }

            amount = vault.getInt("amount", amount);
         }

         if (boxType != null && amount > 0) {
            String boxColor = this.getBoxColor(boxType);
            String var10000 = boxType.substring(0, 1).toUpperCase();
            String boxName = var10000 + boxType.substring(1).toLowerCase();
            lines.add(this.color(" &8▪ " + boxColor + amount + "x " + boxName + " Box"));
         }

         return lines;
      }
   }

   private String getBoxColor(String boxId) {
      switch (boxId.toLowerCase()) {
         case "common" -> {
            return "&7";
         }
         case "rare" -> {
            return "&9";
         }
         case "epic" -> {
            return "&5";
         }
         case "legendary" -> {
            return "&6";
         }
         case "mythic" -> {
            return "&c";
         }
         default -> {
            return "&e";
         }
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
      private String category;

      public void setInventory(Inventory inv) {
         this.inventory = inv;
      }

      public void setCategory(String cat) {
         this.category = cat;
      }

      public String getCategory() {
         return this.category;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }
}
