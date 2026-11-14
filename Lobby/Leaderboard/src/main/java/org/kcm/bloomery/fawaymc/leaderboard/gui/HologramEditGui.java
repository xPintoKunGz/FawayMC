package org.kcm.bloomery.fawaymc.leaderboard.gui;

import java.util.Arrays;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
import org.kcm.bloomery.fawaymc.leaderboard.Global;
import org.kcm.bloomery.fawaymc.leaderboard.hologram.HologramConfig;

public class HologramEditGui implements Listener {
   private final Global plugin;

   public HologramEditGui(Global plugin) {
      this.plugin = plugin;
      Bukkit.getPluginManager().registerEvents(this, plugin);
   }

   public void open(Player p, HologramConfig cfg) {
      Holder holder = new Holder();
      holder.config = cfg;
      String title = this.color("&8&l✦ &6Edit Hologram &8» &f" + cfg.id);
      Inventory inv = Bukkit.createInventory(holder, 54, title);
      holder.setInventory(inv);
      ItemStack border = this.pane(Material.BLACK_STAINED_GLASS_PANE);

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
      }

      for(int i = 45; i < 54; ++i) {
         inv.setItem(i, border);
      }

      Material var10003 = Material.CLOCK;
      String[] var10005 = new String[2];
      Object[] var10009 = new Object[]{cfg.lineSpacing};
      var10005[0] = "&7Current: &f" + String.format("%.2f", var10009);
      var10005[1] = "&7L: &a+0.05 &7R: &c-0.05";
      inv.setItem(10, this.statItem(var10003, "&eLine Spacing", var10005));
      inv.setItem(11, this.statItem(Material.CHEST, "&eEntries", "&7Current: &f" + cfg.entries, "&7L: &a+1 &7R: &c-1"));
      inv.setItem(12, this.statItem(Material.NAME_TAG, "&eTotal Pixel Width", "&7Current: &f" + cfg.totalPixelWidth + "px", "&7L: &a+5 &7R: &c-5"));
      inv.setItem(13, this.toggleItem(Material.BARRIER, "&eHide Empty Positions", cfg.hideEmpty));
      inv.setItem(19, this.textItem(Material.WRITABLE_BOOK, "&eFormat Line", "&7Template: {rank} {name} {value} {dots}", " &f" + cfg.formatLine, "", "&eClick to edit"));
      inv.setItem(20, this.textItem(Material.BOOK, "&eHeader Lines", " &f" + cfg.header.size() + " line(s)", "&7(ใช้ || คั่น) | 'clear' เพื่อลบ", "&eClick to edit"));
      inv.setItem(21, this.textItem(Material.BOOK, "&eFooter Lines", " &f" + cfg.footer.size() + " line(s)", "&7(ใช้ || คั่น) | 'clear' เพื่อลบ", "&eClick to edit"));
      inv.setItem(28, this.actionItem(Material.COMPASS, "&aTeleport", "&7ไปยังตำแหน่ง Hologram"));
      inv.setItem(29, this.actionItem(Material.ENDER_PEARL, "&aMove Here", "&7ย้ายมาที่ตำแหน่งของคุณ"));
      inv.setItem(30, this.actionItem(Material.EMERALD, "&aRe-render", "&7อัปเดตทันที"));
      inv.setItem(31, this.actionItem(Material.LAVA_BUCKET, "&cDelete Hologram", "&7&c&lWARNING &7- Shift-click to confirm"));
      inv.setItem(49, this.actionItem(Material.BARRIER, "&cClose", "&7ปิดเมนู"));
      p.openInventory(inv);
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onClick(InventoryClickEvent event) {
      if (event.getInventory().getHolder() instanceof Holder) {
         event.setCancelled(true);
         if (event.getWhoClicked() instanceof Player) {
            Player p = (Player)event.getWhoClicked();
            Holder holder = (Holder)event.getInventory().getHolder();
            HologramConfig cfg = holder.config;
            if (cfg != null) {
               int slot = event.getRawSlot();
               if (slot >= 0 && slot < 54) {
                  int delta = 0;
                  if (event.isLeftClick()) {
                     delta = event.isShiftClick() ? 5 : 1;
                  } else if (event.isRightClick()) {
                     delta = event.isShiftClick() ? -5 : -1;
                  }

                  boolean rerender = false;
                  boolean save = false;
                  switch (slot) {
                     case 10:
                        if (delta != 0) {
                           double sign = event.isLeftClick() ? (double)1.0F : (double)-1.0F;
                           cfg.lineSpacing = Math.max(0.1, Math.min((double)2.0F, cfg.lineSpacing + 0.05 * sign));
                           rerender = true;
                        }
                        break;
                     case 11:
                        if (delta != 0) {
                           cfg.entries = Math.max(1, Math.min(50, cfg.entries + (event.isShiftClick() ? 5 : 1) * (event.isLeftClick() ? 1 : -1)));
                           rerender = true;
                        }
                        break;
                     case 12:
                        if (delta != 0) {
                           cfg.totalPixelWidth = Math.max(100, Math.min(500, cfg.totalPixelWidth + (event.isShiftClick() ? 10 : 5) * (event.isLeftClick() ? 1 : -1)));
                           rerender = true;
                        }
                        break;
                     case 13:
                        cfg.hideEmpty = !cfg.hideEmpty;
                        rerender = true;
                     case 14:
                     case 15:
                     case 16:
                     case 17:
                     case 18:
                     case 22:
                     case 23:
                     case 24:
                     case 25:
                     case 26:
                     case 27:
                     case 32:
                     case 33:
                     case 34:
                     case 35:
                     case 36:
                     case 37:
                     case 38:
                     case 39:
                     case 40:
                     case 41:
                     case 42:
                     case 43:
                     case 44:
                     case 45:
                     case 46:
                     case 47:
                     case 48:
                     default:
                        break;
                     case 19:
                        this.plugin.getChatInput().request(p, "Enter new FORMAT line:", (str) -> {
                           cfg.formatLine = str;
                           this.plugin.getHologramManager().save(cfg);
                           this.plugin.getHologramManager().render(cfg.id);
                           this.open(p, cfg);
                        });
                        return;
                     case 20:
                        this.plugin.getChatInput().request(p, "Enter HEADER ('||' to separate, 'clear' to remove):", (str) -> {
                           cfg.header.clear();
                           if (!str.equalsIgnoreCase("clear")) {
                              for(String l : str.split("\\|\\|")) {
                                 cfg.header.add(l.trim());
                              }
                           }

                           this.plugin.getHologramManager().save(cfg);
                           this.plugin.getHologramManager().render(cfg.id);
                           this.open(p, cfg);
                        });
                        return;
                     case 21:
                        this.plugin.getChatInput().request(p, "Enter FOOTER ('||' to separate, 'clear' to remove):", (str) -> {
                           cfg.footer.clear();
                           if (!str.equalsIgnoreCase("clear")) {
                              for(String l : str.split("\\|\\|")) {
                                 cfg.footer.add(l.trim());
                              }
                           }

                           this.plugin.getHologramManager().save(cfg);
                           this.plugin.getHologramManager().render(cfg.id);
                           this.open(p, cfg);
                        });
                        return;
                     case 28:
                        if (cfg.location != null) {
                           p.teleport(cfg.location.clone().add((double)0.0F, (double)1.0F, (double)0.0F));
                        }

                        p.closeInventory();
                        return;
                     case 29:
                        cfg.location = p.getLocation().clone();
                        rerender = true;
                        save = true;
                        break;
                     case 30:
                        rerender = true;
                        break;
                     case 31:
                        if (!event.isShiftClick()) {
                           p.sendMessage("§cShift-click to confirm!");
                           return;
                        }

                        this.plugin.getHologramManager().delete(cfg.id);
                        p.closeInventory();
                        return;
                     case 49:
                        p.closeInventory();
                        return;
                  }

                  if (rerender) {
                     this.plugin.getHologramManager().save(cfg);
                     this.plugin.getHologramManager().render(cfg.id);
                     this.open(p, cfg);
                  } else if (save) {
                     this.plugin.getHologramManager().save(cfg);
                  }

               }
            }
         }
      }
   }

   private ItemStack statItem(Material mat, String name, String... lore) {
      ItemStack i = new ItemStack(mat);
      ItemMeta m = i.getItemMeta();
      if (m != null) {
         m.setDisplayName(this.color(name));
         m.setLore(Arrays.asList(lore));
         m.addItemFlags(ItemFlag.values());
         i.setItemMeta(m);
      }

      return i;
   }

   private ItemStack toggleItem(Material mat, String name, boolean on) {
      ItemStack i = new ItemStack(on ? Material.LIME_DYE : Material.GRAY_DYE);
      ItemMeta m = i.getItemMeta();
      if (m != null) {
         m.setDisplayName(this.color(name));
         m.setLore(Arrays.asList(this.color(on ? "&a&l✔ ON" : "&c&l✘ OFF"), "", this.color("&eClick to toggle")));
         i.setItemMeta(m);
      }

      return i;
   }

   private ItemStack textItem(Material mat, String name, String... lore) {
      return this.statItem(mat, name, lore);
   }

   private ItemStack actionItem(Material mat, String name, String... lore) {
      return this.statItem(mat, name, lore);
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
      public HologramConfig config;

      public void setInventory(Inventory inv) {
         this.inventory = inv;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }
}
