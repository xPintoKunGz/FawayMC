package org.kcm.bloomery.fawaymc.luckyblock;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent.SlotType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
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
import org.bukkit.FireworkEffect.Type;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Bat;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

public class CosmeticManager implements Listener {
   private final Global plugin;
   private int trailTaskId = -1;
   private int cleanupTaskId = -1;
   private final Map<UUID, Long> gadgetCooldowns = new HashMap();
   private final Map<UUID, ItemStack> storedHelmets = new HashMap();
   private final Map<UUID, BossBar> cooldownBars = new HashMap();
   private final Map<UUID, Integer> cooldownBarTasks = new HashMap();
   private final Map<UUID, Long> equipMessageCooldowns = new HashMap();
   private final Map<UUID, Long> cooldownMessageCooldowns = new HashMap();
   private final Set<UUID> transitioning = ConcurrentHashMap.newKeySet();
   private final Map<Location, BlockData> paintedBlocks = new HashMap();
   private final Map<UUID, Integer> jetpackGlideTasks = new HashMap();
   private final NamespacedKey cosmeticIdKey;
   private final NamespacedKey specialActionKey;
   private final NamespacedKey cosmeticHatKey;
   private final NamespacedKey gadgetItemKey;
   private final NamespacedKey shopActionKey;
   private final NamespacedKey categoryKey;
   private final NamespacedKey comingSoonKey;
   private boolean settingHat = false;
   private static final Set<String> SUPPORTED_GADGET_ACTIONS = Set.of("FIREWORK", "SNOWBALL", "GRAPPLE", "BAT_BLASTER", "PAINTBALL", "TNT_LAUNCHER", "ENDER_PEARL", "JETPACK");

   public CosmeticManager(Global plugin) {
      this.plugin = plugin;
      this.cosmeticIdKey = new NamespacedKey(plugin, "cosmetic_id");
      this.specialActionKey = new NamespacedKey(plugin, "special_action");
      this.cosmeticHatKey = new NamespacedKey(plugin, "cosmetic_hat");
      this.gadgetItemKey = new NamespacedKey(plugin, "gadget_item");
      this.shopActionKey = new NamespacedKey(plugin, "shop_action");
      this.categoryKey = new NamespacedKey(plugin, "cosmetic_category");
      this.comingSoonKey = new NamespacedKey(plugin, "cosmetic_coming_soon");
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public void startTrailTask() {
      if (this.trailTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.trailTaskId);
      }

      this.trailTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         for(Player p : Bukkit.getOnlinePlayers()) {
            String trailId = this.plugin.getData().getEquipped(p.getUniqueId(), CosmeticType.TRAIL);
            if (trailId != null && !trailId.isEmpty() && this.plugin.getConfig().contains("cosmetics." + trailId)) {
               String particleName = this.plugin.getConfig().getString("cosmetics." + trailId + ".particle", "HEART");
               int count = this.plugin.getConfig().getInt("cosmetics." + trailId + ".particle_count", 3);
               double offset = this.plugin.getConfig().getDouble("cosmetics." + trailId + ".particle_offset", 0.3);
               Particle particle = this.resolveParticle(particleName);
               if (particle != null) {
                  Location loc = p.getLocation().clone().add((double)0.0F, 0.2, (double)0.0F);
                  p.getWorld().spawnParticle(particle, loc, count, offset, 0.1, offset, 0.02);
               }
            }
         }

      }, 0L, 2L).getTaskId();
      if (this.cleanupTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.cleanupTaskId);
      }

      this.cleanupTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         for(Player p : Bukkit.getOnlinePlayers()) {
            this.cleanupStrayHats(p);
         }

      }, 0L, 20L).getTaskId();
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> this.updateGadgetItem(player), 5L);
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      UUID uuid = event.getPlayer().getUniqueId();
      this.gadgetCooldowns.remove(uuid);
      this.cooldownMessageCooldowns.remove(uuid);
      this.equipMessageCooldowns.remove(uuid);
      this.removeCooldownBar(event.getPlayer());
   }

   public void stop() {
      if (this.trailTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.trailTaskId);
         this.trailTaskId = -1;
      }

      if (this.cleanupTaskId != -1) {
         Bukkit.getScheduler().cancelTask(this.cleanupTaskId);
         this.cleanupTaskId = -1;
      }

      for(Map.Entry<Location, BlockData> e : this.paintedBlocks.entrySet()) {
         try {
            ((Location)e.getKey()).getBlock().setBlockData((BlockData)e.getValue());
         } catch (Exception var5) {
         }
      }

      this.paintedBlocks.clear();

      for(Player p : Bukkit.getOnlinePlayers()) {
         this.removeHat(p);
         this.removeCooldownBar(p);
      }

      for(Integer tid : this.jetpackGlideTasks.values()) {
         try {
            Bukkit.getScheduler().cancelTask(tid);
         } catch (Exception var4) {
         }
      }

      this.jetpackGlideTasks.clear();
   }

   private Particle resolveParticle(String name) {
      if (name != null && !name.isBlank()) {
         try {
            return Particle.valueOf(name.trim().toUpperCase());
         } catch (Exception var4) {
            String normalized = name.trim().toLowerCase();
            Particle p = (Particle)Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(normalized));
            if (p != null) {
               return p;
            } else {
               p = (Particle)Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(normalized.replace('_', '.')));
               return p;
            }
         }
      } else {
         return null;
      }
   }

   private void renderComingSoonRow(Inventory inv) {
      List<Map<String, Object>> list = this.getComingSoonCategories();
      if (!list.isEmpty()) {
         int count = Math.min(list.size(), 5);
         int startSlot = 23 - count;

         for(int i = 0; i < count; ++i) {
            int slot = startSlot + i * 2;
            if (slot >= 18 && slot <= 26) {
               Map<String, Object> cat = (Map)list.get(i);
               String id = (String)cat.get("id");
               String name = (String)cat.get("name");
               String description = (String)cat.get("description");
               Material mat = Material.matchMaterial((String)cat.get("material"));
               if (mat == null) {
                  mat = Material.BARRIER;
               }

               inv.setItem(slot, this.createComingSoonButton(id, name, mat, description));
            }
         }

      }
   }

   private ItemStack createComingSoonButton(String id, String name, Material material, String description) {
      ItemStack item = new ItemStack(material);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         meta.setDisplayName(this.color(name));
         List<String> lore = new ArrayList();
         lore.add(this.color(description));
         lore.add("");
         lore.add(this.color("&c&l⏳ COMING SOON"));
         lore.add(this.color("&cThis category is not yet available."));
         lore.add("");
         lore.add(this.color("&8Stay tuned for future updates!"));
         meta.setLore(lore);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
         meta.getPersistentDataContainer().set(this.comingSoonKey, PersistentDataType.STRING, id);

         try {
            Enchantment unb = (Enchantment)Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
            if (unb != null) {
               meta.addEnchant(unb, 1, true);
            }
         } catch (Exception var9) {
         }

         item.setItemMeta(meta);
         return item;
      }
   }

   private List<Map<String, Object>> getComingSoonCategories() {
      List<Map<String, Object>> result = new ArrayList();
      ConfigurationSection sec = this.plugin.getConfig().getConfigurationSection("gui.coming_soon");
      if (sec != null) {
         for(String id : sec.getKeys(false)) {
            Map<String, Object> cat = new HashMap();
            cat.put("id", id);
            cat.put("name", sec.getString(id + ".name", "&7&l" + id.toUpperCase()));
            cat.put("material", sec.getString(id + ".material", "BARRIER"));
            cat.put("description", sec.getString(id + ".description", "&7Coming soon!"));
            result.add(cat);
         }
      }

      if (result.isEmpty()) {
         result.add(this.makeComingSoon("pets", "&a&l✦ &2&lPETS &a&l✦", "BONE", "&7Collect pets to follow you!"));
         result.add(this.makeComingSoon("wings", "&b&l✦ &3&lWINGS &b&l✦", "ELYTRA", "&7Cosmetic wings for your back!"));
         result.add(this.makeComingSoon("3d_heads", "&e&l✦ &6&l3D HEADS &e&l✦", "DRAGON_HEAD", "&7Wear 3D modeled heads!"));
         result.add(this.makeComingSoon("cloaks", "&d&l✦ &5&lCLOAKS &d&l✦", "RED_CARPET", "&7Cosmetic cloaks to show off!"));
      }

      return result;
   }

   private Map<String, Object> makeComingSoon(String id, String name, String material, String description) {
      Map<String, Object> cat = new HashMap();
      cat.put("id", id);
      cat.put("name", name);
      cat.put("material", material);
      cat.put("description", description);
      return cat;
   }

   public void openMenu(Player player) {
      CosmeticCategoryHolder holder = new CosmeticCategoryHolder();
      String title = this.color(this.plugin.getConfig().getString("gui.title", "&8&l✦ &d&lCOSMETICS &8&l✦"));
      Inventory inv = Bukkit.createInventory(holder, 54, title);
      holder.setInventory(inv);
      ItemStack border = this.createStatic(Material.BLACK_STAINED_GLASS_PANE, " ");

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
      }

      for(int i = 45; i < 54; ++i) {
         inv.setItem(i, border);
      }

      ItemStack filler = this.createStatic(Material.GRAY_STAINED_GLASS_PANE, " ");

      for(int i = 9; i < 45; ++i) {
         inv.setItem(i, filler);
      }

      ConfigurationSection cosmetics = this.plugin.getConfig().getConfigurationSection("cosmetics");
      List<String> trails = new ArrayList();
      List<String> hats = new ArrayList();
      List<String> gadgets = new ArrayList();
      if (cosmetics != null) {
         for(String id : cosmetics.getKeys(false)) {
            CosmeticType type = CosmeticType.fromString(this.plugin.getConfig().getString("cosmetics." + id + ".type", "TRAIL"));
            switch (type) {
               case TRAIL:
                  trails.add(id);
                  break;
               case HAT:
                  hats.add(id);
                  break;
               case GADGET:
                  gadgets.add(id);
            }
         }
      }

      inv.setItem(11, this.createCategoryButton(player, CosmeticType.TRAIL, trails));
      inv.setItem(13, this.createCategoryButton(player, CosmeticType.HAT, hats));
      inv.setItem(15, this.createCategoryButton(player, CosmeticType.GADGET, gadgets));
      this.renderComingSoonRow(inv);
      inv.setItem(45, this.createVaultShortcut());
      inv.setItem(47, this.createResetButton(player));
      inv.setItem(49, this.createEquippedSummary(player));
      inv.setItem(51, this.createShopShortcut());
      inv.setItem(53, this.createInfoButton());
      player.openInventory(inv);
   }

   public void openCategory(Player player, CosmeticType type) {
      CosmeticMenuHolder holder = new CosmeticMenuHolder();
      holder.setCategory(type);
      String catName = this.getCategoryName(type);
      String title = this.color("&8&l✦ " + catName + " &8&l✦");
      Inventory inv = Bukkit.createInventory(holder, 54, title);
      holder.setInventory(inv);
      ItemStack border = this.createStatic(Material.BLACK_STAINED_GLASS_PANE, " ");

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
      }

      for(int i = 45; i < 54; ++i) {
         inv.setItem(i, border);
      }

      ConfigurationSection cosmetics = this.plugin.getConfig().getConfigurationSection("cosmetics");
      List<String> items = new ArrayList();
      if (cosmetics != null) {
         for(String id : cosmetics.getKeys(false)) {
            CosmeticType t = CosmeticType.fromString(this.plugin.getConfig().getString("cosmetics." + id + ".type", "TRAIL"));
            if (t == type) {
               items.add(id);
            }
         }
      }

      items.sort(this.buildCosmeticSorter());
      int idx = 0;

      for(int slot = 9; slot <= 44 && idx < items.size(); ++slot) {
         inv.setItem(slot, this.createCosmeticItem(player, (String)items.get(idx)));
         ++idx;
      }

      if (idx < items.size()) {
         Logger var10000 = this.plugin.getLogger();
         int var10001 = items.size() - idx;
         var10000.warning("[Cosmetics] " + var10001 + " item(s) not displayed in " + type.name() + " (max 36)");
      }

      inv.setItem(45, this.createBackButton());
      inv.setItem(47, this.createResetButton(player));
      inv.setItem(49, this.createEquippedSummary(player));
      inv.setItem(51, this.createShopShortcut());
      inv.setItem(53, this.createInfoButton());
      player.openInventory(inv);
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }

   private String getCategoryName(CosmeticType type) {
      switch (type) {
         case TRAIL -> {
            return "&a&lTRAILS";
         }
         case HAT -> {
            return "&6&lHATS";
         }
         case GADGET -> {
            return "&d&lGADGETS";
         }
         default -> {
            return "&7&lUNKNOWN";
         }
      }
   }

   private ItemStack createStatic(Material mat, String name) {
      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(name);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
      }

      return item;
   }

   private boolean isContainerBlock(Material mat) {
      if (mat == null) {
         return true;
      } else {
         String n = mat.name();
         return n.endsWith("CHEST") || n.endsWith("_BOX") || n.contains("SHULKER") || n.equals("BARREL") || n.equals("HOPPER") || n.equals("FURNACE") || n.equals("BLAST_FURNACE") || n.equals("SMOKER") || n.equals("DROPPER") || n.equals("DISPENSER") || n.equals("BREWING_STAND") || n.equals("BEACON") || n.equals("SPAWNER") || n.equals("COMMAND_BLOCK");
      }
   }

   private ItemStack createCategoryButton(Player player, CosmeticType type, List<String> ids) {
      int total = ids.size();
      int owned = 0;

      for(String id : ids) {
         if (this.plugin.getData().ownsCosmetic(player.getUniqueId(), id)) {
            ++owned;
         }
      }

      double ratio = total > 0 ? (double)owned / (double)total : (double)0.0F;
      int percent = (int)Math.round(ratio * (double)100.0F);
      String pctColor = ratio >= 0.9 ? "&a" : (ratio >= (double)0.5F ? "&e" : "&c");
      Map<String, int[]> rarityStats = new HashMap();

      for(String id : ids) {
         String rarity = this.plugin.getConfig().getString("cosmetics." + id + ".rarity", "common").toLowerCase();
         int[] stats = (int[])rarityStats.computeIfAbsent(rarity, (k) -> new int[2]);
         int var10002 = stats[0]++;
         if (this.plugin.getData().ownsCosmetic(player.getUniqueId(), id)) {
            var10002 = stats[1]++;
         }
      }

      Material iconMat;
      String displayName;
      String description;
      String accentColor;
      switch (type) {
         case TRAIL:
            iconMat = Material.BLAZE_POWDER;
            displayName = "&a&l✦ &2&lTRAILS &a&l✦";
            description = "&7Particle effects that trail behind you.";
            accentColor = "&a";
            break;
         case HAT:
            iconMat = Material.DIAMOND_HELMET;
            displayName = "&6&l✦ &e&lHATS &6&l✦";
            description = "&7Cosmetic headwear to show off.";
            accentColor = "&6";
            break;
         case GADGET:
            iconMat = Material.FIREWORK_ROCKET;
            displayName = "&d&l✦ &5&lGADGETS &d&l✦";
            description = "&7Activatable items with cooldowns.";
            accentColor = "&d";
            break;
         default:
            return new ItemStack(Material.STONE);
      }

      ItemStack item = new ItemStack(iconMat);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         meta.setDisplayName(this.color(displayName));
         List<String> lore = new ArrayList();
         lore.add(this.color(description));
         lore.add("");
         lore.add(this.color("&aProgress:"));
         lore.add(this.color(" &f" + owned + "&7/&f" + total + " " + pctColor + "(" + percent + "%)"));
         lore.add("");
         if (!rarityStats.isEmpty()) {
            lore.add(this.color("&dRarity Breakdown:"));
            List<String> rarities = new ArrayList(rarityStats.keySet());
            rarities.sort(Comparator.comparingInt(this::getRarityOrder));

            for(String rarity : rarities) {
               int[] stats = (int[])rarityStats.get(rarity);
               int rTotal = stats[0];
               int rOwned = stats[1];
               int rPercent = rTotal > 0 ? (int)Math.round((double)rOwned / (double)rTotal * (double)100.0F) : 0;
               String rColor = this.plugin.getConfig().getString("rarities." + rarity + ".color", "&7");
               String rName = this.plugin.getConfig().getString("rarities." + rarity + ".name", "Common");
               String rPctColor = rPercent >= 100 ? "&a" : (rPercent > 0 ? "&e" : "&c");
               lore.add(this.color(" " + rColor + rName + "&7: &f" + rOwned + "&7/&f" + rTotal + " " + rPctColor + "(" + rPercent + "%)"));
            }

            lore.add("");
         }

         lore.add(this.color("&bCurrently Equipped:"));
         String equippedId = this.plugin.getData().getEquipped(player.getUniqueId(), type);
         if (equippedId != null && !equippedId.isEmpty()) {
            String eqName = this.plugin.getConfig().getString("cosmetics." + equippedId + ".display_name", equippedId);
            String stripped = ChatColor.stripColor(this.color(eqName));
            lore.add(this.color(" " + accentColor + "✔ &f" + stripped));
         } else {
            lore.add(this.color(" &c✘ &cNone"));
         }

         lore.add("");
         lore.add(this.color("&eClick to view!"));
         meta.setLore(lore);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
         meta.getPersistentDataContainer().set(this.categoryKey, PersistentDataType.STRING, type.name());
         if (total > 0 && owned == total) {
            try {
               Enchantment unb = (Enchantment)Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
               if (unb != null) {
                  meta.addEnchant(unb, 1, true);
               }
            } catch (Exception var28) {
            }
         }

         item.setItemMeta(meta);
         return item;
      }
   }

   private Comparator<String> buildCosmeticSorter() {
      return (a, b) -> {
         String rarityA = this.plugin.getConfig().getString("cosmetics." + a + ".rarity", "common");
         String rarityB = this.plugin.getConfig().getString("cosmetics." + b + ".rarity", "common");
         int cmp = Integer.compare(this.getRarityOrder(rarityA), this.getRarityOrder(rarityB));
         if (cmp != 0) {
            return cmp;
         } else {
            String nameA = ChatColor.stripColor(this.color(this.plugin.getConfig().getString("cosmetics." + a + ".display_name", a)));
            String nameB = ChatColor.stripColor(this.color(this.plugin.getConfig().getString("cosmetics." + b + ".display_name", b)));
            if (nameA == null) {
               nameA = a;
            }

            if (nameB == null) {
               nameB = b;
            }

            return nameA.compareToIgnoreCase(nameB);
         }
      };
   }

   private int getRarityOrder(String rarity) {
      if (rarity == null) {
         return 999;
      } else {
         switch (rarity.toLowerCase()) {
            case "common" -> {
               return 0;
            }
            case "uncommon" -> {
               return 1;
            }
            case "rare" -> {
               return 2;
            }
            case "epic" -> {
               return 3;
            }
            case "legendary" -> {
               return 4;
            }
            case "mythic" -> {
               return 5;
            }
            default -> {
               return 999;
            }
         }
      }
   }

   private ItemStack createShopShortcut() {
      ItemStack item = new ItemStack(Material.EMERALD);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(this.color("&d&l✦ COSMETIC SHOP"));
         List<String> lore = new ArrayList();
         lore.add(this.color("&7Purchase cosmetics using"));
         lore.add(this.color("&6coins &7earned from boxes."));
         lore.add("");
         lore.add(this.color("&7Available tiers:"));
         lore.add(this.color(" &8▪ &9Rare"));
         lore.add(this.color(" &8▪ &5Epic"));
         lore.add("");
         lore.add(this.color("&eClick to open!"));
         meta.setLore(lore);
         meta.getPersistentDataContainer().set(this.shopActionKey, PersistentDataType.STRING, "shop");
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createVaultShortcut() {
      ItemStack item = new ItemStack(Material.ENDER_CHEST);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(this.color("&e&lLucky Vault"));
         meta.setLore(Arrays.asList(this.color("&7Open your Lucky Block Vault"), this.color("&7to unlock more cosmetics."), "", this.color("&eClick to open!")));
         meta.getPersistentDataContainer().set(this.specialActionKey, PersistentDataType.STRING, "open_vault");
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createResetButton(Player player) {
      int equipped = 0;

      for(CosmeticType t : CosmeticType.values()) {
         if (this.plugin.getData().getEquipped(player.getUniqueId(), t) != null) {
            ++equipped;
         }
      }

      ItemStack item = new ItemStack(Material.BARRIER);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(this.color("&c&lReset All"));
         meta.setLore(Arrays.asList(this.color("&7Unequip all cosmetics."), "", this.color("&7Equipped: &f" + equipped + "/3"), "", this.color(equipped > 0 ? "&cClick to reset!" : "&7Nothing to reset")));
         meta.getPersistentDataContainer().set(this.specialActionKey, PersistentDataType.STRING, "reset_all");
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createEquippedSummary(Player player) {
      ItemStack item = new ItemStack(Material.PLAYER_HEAD);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         if (meta instanceof SkullMeta) {
            ((SkullMeta)meta).setOwningPlayer(player);
         }

         meta.setDisplayName(this.color("&6&l✦ &e&l" + player.getName() + " &6&l✦"));
         ConfigurationSection cosmetics = this.plugin.getConfig().getConfigurationSection("cosmetics");
         int totalOwned = 0;
         int totalAll = 0;
         int[] trailStats = new int[]{0, 0};
         int[] hatStats = new int[]{0, 0};
         int[] gadgetStats = new int[]{0, 0};
         if (cosmetics != null) {
            for(String id : cosmetics.getKeys(false)) {
               CosmeticType type = CosmeticType.fromString(this.plugin.getConfig().getString("cosmetics." + id + ".type", "TRAIL"));
               boolean owned = this.plugin.getData().ownsCosmetic(player.getUniqueId(), id);
               ++totalAll;
               if (owned) {
                  ++totalOwned;
               }

               switch (type) {
                  case TRAIL:
                     int var35 = trailStats[1]++;
                     if (owned) {
                        var35 = trailStats[0]++;
                     }
                     break;
                  case HAT:
                     int var33 = hatStats[1]++;
                     if (owned) {
                        var33 = hatStats[0]++;
                     }
                     break;
                  case GADGET:
                     int var10002 = gadgetStats[1]++;
                     if (owned) {
                        var10002 = gadgetStats[0]++;
                     }
               }
            }
         }

         int equippedCount = 0;

         for(CosmeticType t : CosmeticType.values()) {
            if (this.plugin.getData().getEquipped(player.getUniqueId(), t) != null) {
               ++equippedCount;
            }
         }

         double totalRatio = totalAll > 0 ? (double)totalOwned / (double)totalAll : (double)0.0F;
         int totalPercent = (int)Math.round(totalRatio * (double)100.0F);
         String totalColor = totalRatio >= 0.9 ? "&a" : (totalRatio >= (double)0.5F ? "&e" : "&c");
         List<String> lore = new ArrayList();
         lore.add(this.color("&bCollections:"));
         lore.add(this.color(" &8■ &fTotal: &e" + totalOwned + "&7/&f" + totalAll + " " + totalColor + "(" + totalPercent + "%)"));
         lore.add(this.color(" &8■ &aTrails:  &f" + trailStats[0] + "&7/&f" + trailStats[1]));
         lore.add(this.color(" &8■ &6Hats:    &f" + hatStats[0] + "&7/&f" + hatStats[1]));
         lore.add(this.color(" &8■ &dGadgets: &f" + gadgetStats[0] + "&7/&f" + gadgetStats[1]));
         lore.add("");
         lore.add(this.color("&dEquipped &7(" + equippedCount + "/3):"));

         for(CosmeticType t : CosmeticType.values()) {
            String eq = this.plugin.getData().getEquipped(player.getUniqueId(), t);
            String icon = eq != null ? "&a✔" : "&c✘";
            String var10000 = t.name().substring(0, 1);
            String label = var10000 + t.name().substring(1).toLowerCase();
            if (eq != null) {
               String displayName = this.plugin.getConfig().getString("cosmetics." + eq + ".display_name", eq);
               String stripped = ChatColor.stripColor(this.color(displayName));
               lore.add(this.color(" " + icon + " &7" + label + ": &f" + stripped));
            } else {
               lore.add(this.color(" " + icon + " &7" + label + ": &cNone"));
            }
         }

         meta.setLore(lore);
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
         return item;
      }
   }

   private ItemStack createInfoButton() {
      ItemStack item = new ItemStack(Material.BOOK);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(this.color("&e&lInfo"));
         meta.setLore(Arrays.asList(this.color("&7Left-click an item to"), this.color("&7equip or unequip it."), "", this.color("&bTrails &7- Particles"), this.color("&6Hats &7- Headwear"), this.color("&5Gadgets &7- Right-click to use")));
         meta.getPersistentDataContainer().set(this.specialActionKey, PersistentDataType.STRING, "info");
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createBackButton() {
      ItemStack item = new ItemStack(Material.ARROW);
      ItemMeta meta = item.getItemMeta();
      if (meta != null) {
         meta.setDisplayName(this.color("&e← &e&lBACK TO CATEGORIES"));
         List<String> lore = new ArrayList();
         lore.add(this.color("&7Return to the category menu."));
         meta.setLore(lore);
         meta.getPersistentDataContainer().set(this.specialActionKey, PersistentDataType.STRING, "back_to_categories");
         meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
         item.setItemMeta(meta);
      }

      return item;
   }

   private ItemStack createCosmeticItem(Player player, String id) {
      String path = "cosmetics." + id;
      String matName = this.plugin.getConfig().getString(path + ".material", "STONE");
      Material mat = Material.matchMaterial(matName);
      if (mat == null) {
         mat = Material.STONE;
      }

      ItemStack item = new ItemStack(mat);
      ItemMeta meta = item.getItemMeta();
      if (meta == null) {
         return item;
      } else {
         CosmeticType type = CosmeticType.fromString(this.plugin.getConfig().getString(path + ".type", "TRAIL"));
         String displayName = this.plugin.getConfig().getString(path + ".display_name", id);
         String rarityStr = this.plugin.getConfig().getString(path + ".rarity", "common");
         String rarityColor = this.plugin.getConfig().getString("rarities." + rarityStr.toLowerCase() + ".color", "&7");
         String rarityName = this.plugin.getConfig().getString("rarities." + rarityStr.toLowerCase() + ".name", "Common");
         boolean owned = this.plugin.getData().ownsCosmetic(player.getUniqueId(), id);
         boolean equipped = id.equals(this.plugin.getData().getEquipped(player.getUniqueId(), type));
         boolean usable = this.isCosmeticUsable(id);
         meta.setDisplayName(this.color(displayName));
         List<String> lore = new ArrayList();
         lore.add(this.color("&7Rarity: " + rarityColor + rarityName));
         if (type == CosmeticType.GADGET) {
            int cd = this.plugin.getConfig().getInt(path + ".cooldown_seconds", 5);
            lore.add(this.color("&7Cooldown: &f" + cd + "s"));
         }

         lore.add("");
         if (!usable) {
            lore.add(this.color("&c✘ Not available"));
         } else if (!owned) {
            lore.add(this.color("&c✘ Locked"));
            lore.add(this.color("&7Unlock from Lucky Blocks"));
         } else if (equipped) {
            lore.add(this.color("&a&l✔ EQUIPPED"));
            lore.add(this.color("&7Click to unequip"));
         } else {
            lore.add(this.color("&eClick to equip"));
         }

         meta.setLore(lore);
         if (equipped && owned && usable) {
            try {
               Enchantment unbreaking = (Enchantment)Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
               if (unbreaking != null) {
                  meta.addEnchant(unbreaking, 1, true);
               }

               meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ENCHANTS});
            } catch (Exception var18) {
            }
         }

         meta.getPersistentDataContainer().set(this.cosmeticIdKey, PersistentDataType.STRING, id);
         item.setItemMeta(meta);
         return item;
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onInventoryClick(InventoryClickEvent event) {
      InventoryHolder holder = event.getView().getTopInventory().getHolder();
      if (holder != null) {
         if (holder instanceof CosmeticCategoryHolder) {
            event.setCancelled(true);
            event.setResult(Result.DENY);
            if (event.getWhoClicked() instanceof Player) {
               Player player = (Player)event.getWhoClicked();
               if (event.getClickedInventory() == event.getView().getTopInventory()) {
                  if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
                     ItemStack clicked = event.getCurrentItem();
                     if (clicked != null && clicked.hasItemMeta()) {
                        String catName = (String)clicked.getItemMeta().getPersistentDataContainer().get(this.categoryKey, PersistentDataType.STRING);
                        if (catName != null) {
                           if (!this.transitioning.contains(player.getUniqueId())) {
                              this.transitioning.add(player.getUniqueId());

                              try {
                                 CosmeticType type = CosmeticType.valueOf(catName);
                                 Bukkit.getScheduler().runTask(this.plugin, () -> {
                                    try {
                                       if (player.isOnline()) {
                                          InventoryHolder cur = player.getOpenInventory().getTopInventory().getHolder();
                                          if (cur instanceof CosmeticCategoryHolder) {
                                             this.openCategory(player, type);
                                          }

                                          return;
                                       }
                                    } finally {
                                       this.transitioning.remove(player.getUniqueId());
                                    }

                                 });
                                 player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.4F);
                              } catch (Exception var9) {
                                 this.transitioning.remove(player.getUniqueId());
                              }

                           }
                        } else {
                           String shop = (String)clicked.getItemMeta().getPersistentDataContainer().get(this.shopActionKey, PersistentDataType.STRING);
                           if ("shop".equals(shop)) {
                              if (!this.transitioning.contains(player.getUniqueId())) {
                                 this.transitioning.add(player.getUniqueId());
                                 Bukkit.getScheduler().runTask(this.plugin, () -> {
                                    try {
                                       if (player.isOnline()) {
                                          InventoryHolder cur = player.getOpenInventory().getTopInventory().getHolder();
                                          if (cur instanceof CosmeticCategoryHolder) {
                                             this.plugin.getShopManager().openShop(player, ShopManager.ShopOrigin.COSMETICS);
                                          }

                                          return;
                                       }
                                    } finally {
                                       this.transitioning.remove(player.getUniqueId());
                                    }

                                 });
                              }
                           } else {
                              String special = (String)clicked.getItemMeta().getPersistentDataContainer().get(this.specialActionKey, PersistentDataType.STRING);
                              if (special != null) {
                                 this.handleSpecialAction(player, special);
                              }

                           }
                        }
                     }
                  }
               }
            }
         } else if (holder instanceof CosmeticMenuHolder) {
            event.setCancelled(true);
            event.setResult(Result.DENY);
            if (event.getWhoClicked() instanceof Player) {
               Player player = (Player)event.getWhoClicked();
               if (event.getClickedInventory() != null && event.getClickedInventory() == event.getView().getTopInventory()) {
                  if (event.getClick() == ClickType.LEFT || event.getClick() == ClickType.RIGHT) {
                     ItemStack clicked = event.getCurrentItem();
                     if (clicked != null && clicked.hasItemMeta()) {
                        String shop = (String)clicked.getItemMeta().getPersistentDataContainer().get(this.shopActionKey, PersistentDataType.STRING);
                        if ("shop".equals(shop)) {
                           CosmeticType cat = ((CosmeticMenuHolder)holder).getCategory();
                           Bukkit.getScheduler().runTask(this.plugin, () -> {
                              if (player.isOnline()) {
                                 InventoryHolder cur = player.getOpenInventory().getTopInventory().getHolder();
                                 if (cur instanceof CosmeticMenuHolder && ((CosmeticMenuHolder)cur).getCategory() == cat) {
                                    this.plugin.getShopManager().openShop(player, ShopManager.ShopOrigin.COSMETICS);
                                 }

                              }
                           });
                        } else if (!this.isGadgetItem(clicked)) {
                           String special = (String)clicked.getItemMeta().getPersistentDataContainer().get(this.specialActionKey, PersistentDataType.STRING);
                           if (special != null) {
                              this.handleSpecialAction(player, special);
                           } else {
                              String cosmeticId = (String)clicked.getItemMeta().getPersistentDataContainer().get(this.cosmeticIdKey, PersistentDataType.STRING);
                              if (cosmeticId != null && !cosmeticId.isEmpty()) {
                                 if (!this.isCosmeticUsable(cosmeticId)) {
                                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                                 } else if (!this.plugin.getData().ownsCosmetic(player.getUniqueId(), cosmeticId)) {
                                    player.sendMessage(this.plugin.msg("cosmetic_not_owned"));
                                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                                 } else {
                                    CosmeticType catType = ((CosmeticMenuHolder)holder).getCategory();
                                    this.toggleEquip(player, cosmeticId);
                                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                                       if (player.isOnline()) {
                                          Inventory top = player.getOpenInventory().getTopInventory();
                                          if (top != null) {
                                             InventoryHolder h = top.getHolder();
                                             if (h instanceof CosmeticMenuHolder) {
                                                CosmeticMenuHolder menuHolder = (CosmeticMenuHolder)h;
                                                if (menuHolder.getCategory() == catType) {
                                                   this.renderCategoryInPlace(player, top, catType);
                                                }
                                             }

                                          }
                                       }
                                    });
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

   private void renderCategoryInPlace(Player player, Inventory inv, CosmeticType type) {
      inv.clear();
      ItemStack border = this.createStatic(Material.BLACK_STAINED_GLASS_PANE, " ");

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
      }

      for(int i = 45; i < 54; ++i) {
         inv.setItem(i, border);
      }

      ConfigurationSection cosmetics = this.plugin.getConfig().getConfigurationSection("cosmetics");
      List<String> items = new ArrayList();
      if (cosmetics != null) {
         for(String id : cosmetics.getKeys(false)) {
            CosmeticType t = CosmeticType.fromString(this.plugin.getConfig().getString("cosmetics." + id + ".type", "TRAIL"));
            if (t == type) {
               items.add(id);
            }
         }
      }

      items.sort(this.buildCosmeticSorter());
      int idx = 0;

      for(int slot = 9; slot <= 44 && idx < items.size(); ++slot) {
         inv.setItem(slot, this.createCosmeticItem(player, (String)items.get(idx)));
         ++idx;
      }

      inv.setItem(45, this.createBackButton());
      inv.setItem(47, this.createResetButton(player));
      inv.setItem(49, this.createEquippedSummary(player));
      inv.setItem(51, this.createShopShortcut());
      inv.setItem(53, this.createInfoButton());
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onHelmetSlotClick(InventoryClickEvent event) {
      if (!(event.getView().getTopInventory().getHolder() instanceof CosmeticMenuHolder)) {
         if (!(event.getView().getTopInventory().getHolder() instanceof CosmeticCategoryHolder)) {
            if (event.getWhoClicked() instanceof Player) {
               Player player = (Player)event.getWhoClicked();
               ItemStack helmet = player.getInventory().getHelmet();
               if (this.isCosmeticHat(helmet)) {
                  ItemStack current = event.getCurrentItem();
                  boolean block = false;
                  if (event.getView().getTopInventory().getType() == InventoryType.CRAFTING && event.getRawSlot() == 5) {
                     block = true;
                  }

                  if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory()) && event.getSlot() == 39) {
                     block = true;
                  }

                  if (current != null && this.isCosmeticHat(current)) {
                     block = true;
                  }

                  if (current != null && this.isGadgetItem(current)) {
                     event.setCancelled(true);
                     event.setResult(Result.DENY);
                  } else if (block) {
                     event.setCancelled(true);
                     event.setResult(Result.DENY);
                     this.sendHatLockMessage(player);
                  } else {
                     if (event.isShiftClick() && current != null && (this.isHelmetPlaceable(current.getType()) || this.isCosmeticHat(current))) {
                        event.setCancelled(true);
                        event.setResult(Result.DENY);
                        this.sendHatLockMessage(player);
                     }

                  }
               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onHelmetDrag(InventoryDragEvent event) {
      if (!(event.getView().getTopInventory().getHolder() instanceof CosmeticMenuHolder)) {
         if (!(event.getView().getTopInventory().getHolder() instanceof CosmeticCategoryHolder)) {
            if (event.getWhoClicked() instanceof Player) {
               Player player = (Player)event.getWhoClicked();
               if (this.isCosmeticHat(player.getInventory().getHelmet())) {
                  if (event.getView().getTopInventory().getType() == InventoryType.CRAFTING && event.getRawSlots().contains(5)) {
                     event.setCancelled(true);
                     this.sendHatLockMessage(player);
                  }

               }
            }
         }
      }
   }

   private void sendHatLockMessage(Player player) {
      player.sendMessage(this.color("&c&lCOSMETICS &8» &cYour cosmetic hat is locked! Use &f/cosmetics &cto unequip."));
      player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
   }

   private boolean isHelmetPlaceable(Material mat) {
      if (mat == null) {
         return false;
      } else {
         String name = mat.name();
         return name.endsWith("_HELMET") || name.endsWith("_SKULL") || name.equals("PLAYER_HEAD") || name.equals("CARVED_PUMPKIN") || name.equals("TURTLE_HELMET");
      }
   }

   private void handleSpecialAction(Player player, String action) {
      switch (action) {
         case "open_vault":
            Bukkit.getScheduler().runTask(this.plugin, () -> {
               if (player.isOnline()) {
                  this.plugin.getMenuManager().openVault(player);
               }

            });
            player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1.0F, 1.0F);
            break;
         case "reset_all":
            this.resetAllCosmetics(player);
            break;
         case "info":
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
            break;
         case "back_to_categories":
            Bukkit.getScheduler().runTask(this.plugin, () -> {
               if (player.isOnline()) {
                  this.openMenu(player);
               }

            });
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.2F);
      }

   }

   private void resetAllCosmetics(Player player) {
      if (this.hasActiveGadgetCooldown(player)) {
         long remain = this.getRemainingGadgetCooldown(player);
         player.sendMessage(this.color("&c&lCOSMETICS &8» &cPlease wait &e" + remain + "s &cbefore resetting!"));
         player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
      } else {
         boolean anyEquipped = false;

         for(CosmeticType t : CosmeticType.values()) {
            if (this.plugin.getData().getEquipped(player.getUniqueId(), t) != null) {
               anyEquipped = true;
               break;
            }
         }

         if (!anyEquipped) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
         } else {
            for(CosmeticType t : CosmeticType.values()) {
               this.plugin.getData().setEquipped(player.getUniqueId(), t, (String)null);
            }

            this.removeHat(player);
            this.removeCooldownBar(player);
            this.gadgetCooldowns.remove(player.getUniqueId());
            this.updateGadgetItem(player);
            player.sendMessage(this.color("&a&lCOSMETICS &8» &aAll cosmetics reset."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0F, 0.5F);
            Bukkit.getScheduler().runTask(this.plugin, () -> {
               if (player.isOnline()) {
                  Inventory top = player.getOpenInventory().getTopInventory();
                  if (top != null) {
                     InventoryHolder h = top.getHolder();
                     if (h instanceof CosmeticMenuHolder) {
                        CosmeticType cat = ((CosmeticMenuHolder)h).getCategory();
                        if (cat != null) {
                           this.renderCategoryInPlace(player, top, cat);
                        }
                     } else if (h instanceof CosmeticCategoryHolder) {
                        this.renderOverviewInPlace(player, top);
                     }

                  }
               }
            });
         }
      }
   }

   private void renderOverviewInPlace(Player player, Inventory inv) {
      inv.clear();
      ItemStack border = this.createStatic(Material.BLACK_STAINED_GLASS_PANE, " ");

      for(int i = 0; i < 9; ++i) {
         inv.setItem(i, border);
      }

      for(int i = 45; i < 54; ++i) {
         inv.setItem(i, border);
      }

      ItemStack filler = this.createStatic(Material.GRAY_STAINED_GLASS_PANE, " ");

      for(int i = 9; i < 45; ++i) {
         inv.setItem(i, filler);
      }

      ConfigurationSection cosmetics = this.plugin.getConfig().getConfigurationSection("cosmetics");
      List<String> trails = new ArrayList();
      List<String> hats = new ArrayList();
      List<String> gadgets = new ArrayList();
      if (cosmetics != null) {
         for(String id : cosmetics.getKeys(false)) {
            CosmeticType type = CosmeticType.fromString(this.plugin.getConfig().getString("cosmetics." + id + ".type", "TRAIL"));
            switch (type) {
               case TRAIL:
                  trails.add(id);
                  break;
               case HAT:
                  hats.add(id);
                  break;
               case GADGET:
                  gadgets.add(id);
            }
         }
      }

      inv.setItem(11, this.createCategoryButton(player, CosmeticType.TRAIL, trails));
      inv.setItem(13, this.createCategoryButton(player, CosmeticType.HAT, hats));
      inv.setItem(15, this.createCategoryButton(player, CosmeticType.GADGET, gadgets));
      this.renderComingSoonRow(inv);
      inv.setItem(45, this.createVaultShortcut());
      inv.setItem(47, this.createResetButton(player));
      inv.setItem(49, this.createEquippedSummary(player));
      inv.setItem(51, this.createShopShortcut());
      inv.setItem(53, this.createInfoButton());
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onInventoryDrag(InventoryDragEvent event) {
      InventoryHolder h = event.getView().getTopInventory().getHolder();
      if (h instanceof CosmeticMenuHolder || h instanceof CosmeticCategoryHolder) {
         event.setCancelled(true);
      }

   }

   @EventHandler(
      priority = EventPriority.HIGHEST
   )
   public void onPlayerDeath(PlayerDeathEvent event) {
      Player player = event.getEntity();
      event.getDrops().removeIf((item) -> item != null && this.isGadgetItem(item));
      String gadgetId = this.plugin.getData().getEquipped(player.getUniqueId(), CosmeticType.GADGET);
      if (gadgetId != null && !gadgetId.isEmpty()) {
         ItemStack gadget = this.createGadgetItem(gadgetId);
         if (gadget != null) {
            event.getItemsToKeep().add(gadget);
         }

      }
   }

   @EventHandler(
      priority = EventPriority.MONITOR
   )
   public void onPlayerRespawn(PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (player.isOnline()) {
            this.updateGadgetItem(player);
         }

      }, 2L);
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onPlayerDropItem(PlayerDropItemEvent event) {
      ItemStack item = event.getItemDrop().getItemStack();
      if (item != null && item.hasItemMeta()) {
         if (item.getItemMeta().getPersistentDataContainer().has(this.cosmeticIdKey, PersistentDataType.STRING)) {
            event.setCancelled(true);
         } else if (this.isGadgetItem(item)) {
            event.setCancelled(true);
         } else {
            if (this.isCosmeticHat(item)) {
               event.setCancelled(true);
               this.sendHatLockMessage(event.getPlayer());
            }

         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onInventoryMoveItem(InventoryMoveItemEvent event) {
      InventoryHolder src = event.getSource().getHolder();
      InventoryHolder dst = event.getDestination().getHolder();
      if (!(src instanceof CosmeticMenuHolder) && !(src instanceof CosmeticCategoryHolder) && !(dst instanceof CosmeticMenuHolder) && !(dst instanceof CosmeticCategoryHolder)) {
         if (event.getItem() != null && this.isGadgetItem(event.getItem())) {
            event.setCancelled(true);
         }

      } else {
         event.setCancelled(true);
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onGadgetInventoryClick(InventoryClickEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         Player player = (Player)event.getWhoClicked();
         InventoryHolder holder = event.getView().getTopInventory().getHolder();
         if (!(holder instanceof CosmeticMenuHolder)) {
            if (!(holder instanceof CosmeticCategoryHolder)) {
               ItemStack current = event.getCurrentItem();
               ItemStack cursor = event.getCursor();
               int hotbarBtn = event.getHotbarButton();
               ItemStack hotbarSwap = hotbarBtn >= 0 && hotbarBtn <= 8 ? player.getInventory().getItem(hotbarBtn) : null;
               if (current != null && this.isGadgetItem(current) || cursor != null && this.isGadgetItem(cursor) || hotbarSwap != null && this.isGadgetItem(hotbarSwap)) {
                  event.setCancelled(true);
                  event.setResult(Result.DENY);
               }

            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onGadgetDragProtect(InventoryDragEvent event) {
      if (event.getWhoClicked() instanceof Player) {
         InventoryHolder holder = event.getView().getTopInventory().getHolder();
         if (!(holder instanceof CosmeticMenuHolder)) {
            if (!(holder instanceof CosmeticCategoryHolder)) {
               ItemStack dragged = event.getOldCursor();
               if (dragged != null && this.isGadgetItem(dragged)) {
                  event.setCancelled(true);
               } else {
                  for(int rawSlot : event.getRawSlots()) {
                     ItemStack slotItem = event.getView().getItem(rawSlot);
                     if (slotItem != null && this.isGadgetItem(slotItem)) {
                        event.setCancelled(true);
                        return;
                     }
                  }

               }
            }
         }
      }
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onGadgetSwapHands(PlayerSwapHandItemsEvent event) {
      ItemStack main = event.getMainHandItem();
      ItemStack off = event.getOffHandItem();
      if (main != null && this.isGadgetItem(main) || off != null && this.isGadgetItem(off)) {
         event.setCancelled(true);
      }

   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onArmorChange(PlayerArmorChangeEvent event) {
      if (!this.settingHat) {
         if (event.getSlotType() == SlotType.HEAD) {
            ItemStack oldItem = event.getOldItem();
            ItemStack newItem = event.getNewItem();
            if (this.isCosmeticHat(oldItem) && !this.isCosmeticHat(newItem)) {
               Player player = event.getPlayer();
               String currentHat = this.plugin.getData().getEquipped(player.getUniqueId(), CosmeticType.HAT);
               if (currentHat != null && !currentHat.isEmpty()) {
                  Bukkit.getScheduler().runTask(this.plugin, () -> {
                     if (player.isOnline()) {
                        String latest = this.plugin.getData().getEquipped(player.getUniqueId(), CosmeticType.HAT);
                        if (latest != null && !latest.isEmpty()) {
                           this.applyHatInternal(player, latest);
                           this.cleanupStrayHats(player);
                           player.updateInventory();
                        }
                     }
                  });
                  this.sendHatLockMessage(player);
               }
            }
         }
      }
   }

   private void cleanupStrayHats(Player player) {
      boolean changed = false;
      ItemStack[] contents = player.getInventory().getContents();

      for(int i = 0; i < contents.length; ++i) {
         if (i != 39) {
            ItemStack item = contents[i];
            if (item != null && this.isCosmeticHat(item)) {
               player.getInventory().setItem(i, (ItemStack)null);
               changed = true;
            }
         }
      }

      ItemStack cursor = player.getItemOnCursor();
      if (cursor != null && this.isCosmeticHat(cursor)) {
         player.setItemOnCursor((ItemStack)null);
         changed = true;
      }

      if (changed) {
         player.updateInventory();
      }

   }

   private boolean hasActiveGadgetCooldown(Player player) {
      UUID uuid = player.getUniqueId();
      Long lastUse = (Long)this.gadgetCooldowns.get(uuid);
      if (lastUse == null) {
         return false;
      } else {
         String gadgetId = this.plugin.getData().getEquipped(uuid, CosmeticType.GADGET);
         if (gadgetId != null && !gadgetId.isEmpty()) {
            int cd = this.plugin.getConfig().getInt("cosmetics." + gadgetId + ".cooldown_seconds", 5);
            if (cd <= 0) {
               return false;
            } else {
               return System.currentTimeMillis() - lastUse < (long)cd * 1000L;
            }
         } else {
            return false;
         }
      }
   }

   private long getRemainingGadgetCooldown(Player player) {
      UUID uuid = player.getUniqueId();
      Long lastUse = (Long)this.gadgetCooldowns.get(uuid);
      if (lastUse == null) {
         return 0L;
      } else {
         String gadgetId = this.plugin.getData().getEquipped(uuid, CosmeticType.GADGET);
         if (gadgetId != null && !gadgetId.isEmpty()) {
            int cd = this.plugin.getConfig().getInt("cosmetics." + gadgetId + ".cooldown_seconds", 5);
            if (cd <= 0) {
               return 0L;
            } else {
               long elapsed = System.currentTimeMillis() - lastUse;
               long remain = (long)cd * 1000L - elapsed;
               return remain > 0L ? remain / 1000L + 1L : 0L;
            }
         } else {
            return 0L;
         }
      }
   }

   private void toggleEquip(Player player, String id) {
      if (this.plugin.getConfig().contains("cosmetics." + id)) {
         if (this.isCosmeticUsable(id)) {
            CosmeticType type = CosmeticType.fromString(this.plugin.getConfig().getString("cosmetics." + id + ".type", "TRAIL"));
            if (type == CosmeticType.GADGET && this.hasActiveGadgetCooldown(player)) {
               long remain = this.getRemainingGadgetCooldown(player);
               player.sendMessage(this.color("&c&lCOSMETICS &8» &cPlease wait &e" + remain + "s &cbefore switching gadget!"));
               player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            } else {
               String current = this.plugin.getData().getEquipped(player.getUniqueId(), type);
               if (id.equals(current)) {
                  this.plugin.getData().setEquipped(player.getUniqueId(), type, (String)null);
                  if (type == CosmeticType.HAT) {
                     this.removeHat(player);
                  }

                  if (type == CosmeticType.GADGET) {
                     this.updateGadgetItem(player);
                  }

                  this.sendEquipMessage(player, id, false);
                  player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 0.8F);
               } else {
                  if (type == CosmeticType.GADGET) {
                     this.gadgetCooldowns.remove(player.getUniqueId());
                     this.removeCooldownBar(player);
                  }

                  if (type == CosmeticType.HAT) {
                     this.removeHat(player);
                     if (!this.applyHat(player, id)) {
                        return;
                     }
                  }

                  this.plugin.getData().setEquipped(player.getUniqueId(), type, id);
                  if (type == CosmeticType.GADGET) {
                     this.updateGadgetItem(player);
                  }

                  this.sendEquipMessage(player, id, true);
                  player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0F, 1.5F);
               }
            }
         }
      }
   }

   public boolean applyHat(Player p, String hatId) {
      if (!this.storedHelmets.containsKey(p.getUniqueId())) {
         this.storedHelmets.put(p.getUniqueId(), p.getInventory().getHelmet());
      }

      this.applyHatInternal(p, hatId);
      return p.getInventory().getHelmet() != null;
   }

   private void applyHatInternal(Player p, String hatId) {
      String matName = this.plugin.getConfig().getString("cosmetics." + hatId + ".material", "DIAMOND_BLOCK");
      Material mat = Material.matchMaterial(matName);
      if (mat != null && mat.isItem()) {
         ItemStack hat = new ItemStack(mat);
         ItemMeta meta = hat.getItemMeta();
         if (meta != null) {
            meta.setDisplayName(this.color(this.plugin.getConfig().getString("cosmetics." + hatId + ".display_name", hatId)));
            meta.getPersistentDataContainer().set(this.cosmeticHatKey, PersistentDataType.STRING, hatId);
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES});
            hat.setItemMeta(meta);
         }

         this.settingHat = true;

         try {
            p.getInventory().setHelmet(hat);
         } finally {
            this.settingHat = false;
         }

      }
   }

   public void removeHat(Player p) {
      ItemStack old = (ItemStack)this.storedHelmets.remove(p.getUniqueId());
      this.settingHat = true;

      try {
         if (old != null) {
            p.getInventory().setHelmet(old);
         } else {
            ItemStack current = p.getInventory().getHelmet();
            if (current != null && this.isCosmeticHat(current)) {
               p.getInventory().setHelmet((ItemStack)null);
            }
         }
      } finally {
         this.settingHat = false;
      }

   }

   private ItemStack createGadgetItem(String gadgetId) {
      String matName = this.plugin.getConfig().getString("cosmetics." + gadgetId + ".material", "FIREWORK_ROCKET");
      Material mat = Material.matchMaterial(matName);
      if (mat != null && mat.isItem()) {
         ItemStack item = new ItemStack(mat);
         ItemMeta meta = item.getItemMeta();
         if (meta == null) {
            return item;
         } else {
            String displayName = this.plugin.getConfig().getString("cosmetics." + gadgetId + ".display_name", gadgetId);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&l✦ " + displayName + " &7(Right Click)"));
            int cd = this.plugin.getConfig().getInt("cosmetics." + gadgetId + ".cooldown_seconds", 5);
            List<String> lore = new ArrayList();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Right-click to activate"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7Cooldown: &f" + cd + "s"));
            lore.add("");
            lore.add(ChatColor.translateAlternateColorCodes('&', "&8Gadget Cosmetic"));
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(this.gadgetItemKey, PersistentDataType.STRING, gadgetId);
            meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
            item.setItemMeta(meta);
            return item;
         }
      } else {
         return null;
      }
   }

   public void updateGadgetItem(Player player) {
      int slot = this.plugin.getConfig().getInt("gadget.hotbar_slot", 5);
      if (slot < 0 || slot > 8) {
         slot = 5;
      }

      ItemStack existing = player.getInventory().getItem(slot);
      if (existing != null && this.isGadgetItem(existing)) {
         player.getInventory().setItem(slot, (ItemStack)null);
      }

      String gadgetId = this.plugin.getData().getEquipped(player.getUniqueId(), CosmeticType.GADGET);
      if (gadgetId != null && !gadgetId.isEmpty()) {
         String matName = this.plugin.getConfig().getString("cosmetics." + gadgetId + ".material", "FIREWORK_ROCKET");
         Material mat = Material.matchMaterial(matName);
         if (mat != null && mat.isItem()) {
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
               String displayName = this.plugin.getConfig().getString("cosmetics." + gadgetId + ".display_name", gadgetId);
               meta.setDisplayName(this.color("&d&l✦ " + displayName + " &7(Right Click)"));
               int cd = this.plugin.getConfig().getInt("cosmetics." + gadgetId + ".cooldown_seconds", 5);
               List<String> lore = new ArrayList();
               lore.add(this.color("&7Right-click to activate"));
               lore.add(this.color("&7Cooldown: &f" + cd + "s"));
               lore.add("");
               lore.add(this.color("&8Gadget Cosmetic"));
               meta.setLore(lore);
               meta.getPersistentDataContainer().set(this.gadgetItemKey, PersistentDataType.STRING, gadgetId);
               meta.addItemFlags(new ItemFlag[]{ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS});
               item.setItemMeta(meta);
            }

            player.getInventory().setItem(slot, item);
            player.updateInventory();
         }
      } else {
         player.updateInventory();
      }
   }

   private boolean isGadgetItem(ItemStack item) {
      return item != null && item.hasItemMeta() ? item.getItemMeta().getPersistentDataContainer().has(this.gadgetItemKey, PersistentDataType.STRING) : false;
   }

   private void sendCooldownMessage(Player p, long remainSec) {
      UUID uuid = p.getUniqueId();
      long now = System.currentTimeMillis();
      long throttleMs = this.plugin.getConfig().getLong("gadget.message_cooldown_ms", 1000L);
      Long last = (Long)this.cooldownMessageCooldowns.get(uuid);
      if (last == null || now - last >= throttleMs) {
         this.cooldownMessageCooldowns.put(uuid, now);
         p.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize("&c&lGADGET &8» &cCooldown: &e" + remainSec + "s"));
      }
   }

   private void startCooldownBar(Player player, int cooldownSec) {
      if (this.plugin.getConfig().getBoolean("gadget.cooldown_bar.enabled", true)) {
         this.removeCooldownBar(player);
         String type = this.plugin.getConfig().getString("gadget.cooldown_bar.type", "BOSS_BAR");
         long endTime = System.currentTimeMillis() + (long)cooldownSec * 1000L;
         if (type.equalsIgnoreCase("BOSS_BAR")) {
            this.startBossBarCooldown(player, endTime, cooldownSec);
         } else {
            this.startActionBarCooldown(player, endTime, cooldownSec);
         }

      }
   }

   private void startBossBarCooldown(Player player, long endTime, int cooldownSec) {
      BarColor color;
      try {
         color = BarColor.valueOf(this.plugin.getConfig().getString("gadget.cooldown_bar.color", "RED").toUpperCase());
      } catch (Exception var11) {
         color = BarColor.RED;
      }

      BarStyle style;
      try {
         style = BarStyle.valueOf(this.plugin.getConfig().getString("gadget.cooldown_bar.style", "SOLID").toUpperCase());
      } catch (Exception var10) {
         style = BarStyle.SOLID;
      }

      BossBar bar = Bukkit.createBossBar("", color, style, new BarFlag[0]);
      bar.addPlayer(player);
      bar.setProgress((double)1.0F);
      this.cooldownBars.put(player.getUniqueId(), bar);
      String titleTemplate = this.plugin.getConfig().getString("gadget.cooldown_bar.title", "&c&l✦ &fGadget Cooldown &c&l✦ &7(&e{remaining}s&7)");
      int taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (!player.isOnline()) {
            this.removeCooldownBar(player);
         } else {
            long remaining = endTime - System.currentTimeMillis();
            if (remaining <= 0L) {
               this.removeCooldownBar(player);
               player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0F, 2.0F);
            } else {
               double progress = Math.max((double)0.0F, Math.min((double)1.0F, (double)remaining / ((double)cooldownSec * (double)1000.0F)));
               String title = this.color(titleTemplate.replace("{remaining}", String.format("%.1f", (double)remaining / (double)1000.0F)));
               bar.setTitle(title);
               bar.setProgress(progress);
            }
         }
      }, 0L, 1L).getTaskId();
      this.cooldownBarTasks.put(player.getUniqueId(), taskId);
   }

   private void startActionBarCooldown(Player player, long endTime, int cooldownSec) {
      int barLength = this.plugin.getConfig().getInt("gadget.cooldown_bar.action_bar.bar_length", 20);
      String filled = this.plugin.getConfig().getString("gadget.cooldown_bar.action_bar.filled_color", "&a");
      String empty = this.plugin.getConfig().getString("gadget.cooldown_bar.action_bar.empty_color", "&c");
      String filledChar = this.plugin.getConfig().getString("gadget.cooldown_bar.action_bar.filled_char", "|");
      String emptyChar = this.plugin.getConfig().getString("gadget.cooldown_bar.action_bar.empty_char", "|");
      String msgTemplate = this.plugin.getConfig().getString("gadget.cooldown_bar.action_bar.message", "{bar} &e{remaining}s");
      int taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (!player.isOnline()) {
            this.removeCooldownBar(player);
         } else {
            long remaining = endTime - System.currentTimeMillis();
            if (remaining <= 0L) {
               this.removeCooldownBar(player);
            } else {
               double ratio = Math.max((double)0.0F, Math.min((double)1.0F, (double)remaining / ((double)cooldownSec * (double)1000.0F)));
               int filledCount = (int)Math.round((double)barLength * ratio);
               StringBuilder bar = new StringBuilder();
               bar.append(filled);

               for(int i = 0; i < filledCount; ++i) {
                  bar.append(filledChar);
               }

               bar.append(empty);

               for(int i = filledCount; i < barLength; ++i) {
                  bar.append(emptyChar);
               }

               String msg = msgTemplate.replace("{bar}", bar.toString()).replace("{remaining}", String.format("%.1f", (double)remaining / (double)1000.0F));
               player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
            }
         }
      }, 0L, 1L).getTaskId();
      this.cooldownBarTasks.put(player.getUniqueId(), taskId);
   }

   private void removeCooldownBar(Player player) {
      Integer task = (Integer)this.cooldownBarTasks.remove(player.getUniqueId());
      if (task != null) {
         try {
            Bukkit.getScheduler().cancelTask(task);
         } catch (Exception var4) {
         }
      }

      BossBar bar = (BossBar)this.cooldownBars.remove(player.getUniqueId());
      if (bar != null) {
         bar.removeAll();
      }

   }

   private void sendEquipMessage(Player player, String cosmeticId, boolean equipped) {
      if (this.plugin.getConfig().getBoolean("gui.equip_message.enabled", true)) {
         UUID uuid = player.getUniqueId();
         long now = System.currentTimeMillis();
         long throttleMs = this.plugin.getConfig().getLong("gui.equip_message.throttle_ms", 500L);
         Long last = (Long)this.equipMessageCooldowns.get(uuid);
         if (last == null || now - last >= throttleMs) {
            this.equipMessageCooldowns.put(uuid, now);
            String displayName = this.plugin.getConfig().getString("cosmetics." + cosmeticId + ".display_name", cosmeticId);
            String format = equipped ? this.plugin.getConfig().getString("gui.equip_message.equip_format", "&a&l✔ &fEquipped &e{name}") : this.plugin.getConfig().getString("gui.equip_message.unequip_format", "&c&l✘ &fUnequipped &e{name}");
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(format.replace("{name}", displayName)));
         }
      }
   }

   private boolean isCosmeticHat(ItemStack item) {
      return item != null && item.hasItemMeta() ? item.getItemMeta().getPersistentDataContainer().has(this.cosmeticHatKey, PersistentDataType.STRING) : false;
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onGadgetItemClick(PlayerInteractEvent event) {
      if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
         Player p = event.getPlayer();
         ItemStack item = event.getItem();
         if (item != null && this.isGadgetItem(item)) {
            event.setCancelled(true);
            event.setUseItemInHand(Result.DENY);
            event.setUseInteractedBlock(Result.DENY);
            String gadgetId = (String)item.getItemMeta().getPersistentDataContainer().get(this.gadgetItemKey, PersistentDataType.STRING);
            if (gadgetId != null && !gadgetId.isEmpty()) {
               String equipped = this.plugin.getData().getEquipped(p.getUniqueId(), CosmeticType.GADGET);
               if (!gadgetId.equals(equipped)) {
                  this.updateGadgetItem(p);
               } else if (this.isCosmeticUsable(gadgetId)) {
                  String action = this.plugin.getConfig().getString("cosmetics." + gadgetId + ".action", "");
                  int cooldownSec = this.plugin.getConfig().getInt("cosmetics." + gadgetId + ".cooldown_seconds", 5);
                  if ("GRAPPLE".equalsIgnoreCase(action)) {
                     cooldownSec = 0;
                  }

                  long now = System.currentTimeMillis();
                  if (cooldownSec > 0) {
                     Long lastUse = (Long)this.gadgetCooldowns.get(p.getUniqueId());
                     if (lastUse != null && now - lastUse < (long)cooldownSec * 1000L) {
                        long remain = ((long)cooldownSec * 1000L - (now - lastUse)) / 1000L + 1L;
                        this.sendCooldownMessage(p, remain);
                        return;
                     }

                     this.gadgetCooldowns.put(p.getUniqueId(), now);
                     this.startCooldownBar(p, cooldownSec);
                  }

                  this.executeGadgetAction(p, action);
               }
            }
         }
      }
   }

   private void executeGadgetAction(Player p, String action) {
      if (action != null && !action.isEmpty()) {
         try {
            switch (action.toUpperCase()) {
               case "FIREWORK": {
                  Firework fw = (Firework)p.getWorld().spawn(p.getLocation(), Firework.class);
                  FireworkMeta fm = fw.getFireworkMeta();
                  fm.addEffect(FireworkEffect.builder().withColor(new Color[]{Color.RED, Color.ORANGE, Color.YELLOW}).with(Type.BALL_LARGE).trail(true).flicker(true).build());
                  fm.setPower(1);
                  fw.setFireworkMeta(fm);
                  p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
                  break;
               }
               case "SNOWBALL": {
                  for(int i = 0; i < 5; ++i) {
                     Snowball sb = (Snowball)p.launchProjectile(Snowball.class);
                     Vector v = p.getLocation().getDirection().multiply(1.8);
                     v.setX(v.getX() + (Math.random() - (double)0.5F) * (double)0.25F);
                     v.setY(v.getY() + (Math.random() - (double)0.5F) * 0.1);
                     v.setZ(v.getZ() + (Math.random() - (double)0.5F) * (double)0.25F);
                     sb.setVelocity(v);
                  }

                  p.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0F, 1.0F);
                  break;
               }
               case "GRAPPLE": {
                  Block target = p.getTargetBlockExact(30);
                  if (target != null) {
                     Location dest = target.getLocation().add((double)0.5F, (double)1.0F, (double)0.5F);
                     Vector dir = dest.toVector().subtract(p.getLocation().toVector());
                     double dist = dir.length();
                     if (dist > (double)1.5F) {
                        dir.normalize().multiply(Math.min((double)2.5F, dist * 0.2));
                        dir.setY(dir.getY() + 0.45);
                        p.setVelocity(dir);
                     }

                     p.getWorld().spawnParticle(Particle.END_ROD, p.getEyeLocation(), 15, 0.1, 0.1, 0.1, 0.02);
                     p.playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0F, 1.0F);
                  } else {
                     p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
                  }
                  break;
               }
               case "BAT_BLASTER": {
                  Location spawn = p.getLocation().add((double)0.0F, (double)1.0F, (double)0.0F);

                  for(int i = 0; i < 8; ++i) {
                     Location batLoc = spawn.clone().add((Math.random() - (double)0.5F) * (double)0.5F, (double)0.0F, (Math.random() - (double)0.5F) * (double)0.5F);

                     try {
                        Bat bat = (Bat)p.getWorld().spawn(batLoc, Bat.class, SpawnReason.CUSTOM, (b) -> {
                           b.setPersistent(false);
                           b.setSilent(true);
                           b.setInvulnerable(true);
                           b.setRemoveWhenFarAway(true);
                        });
                        if (bat != null) {
                           double dx = (Math.random() - (double)0.5F) * (double)1.5F;
                           double dy = Math.random() * 0.8 + 0.2;
                           double dz = (Math.random() - (double)0.5F) * (double)1.5F;
                           bat.setVelocity(new Vector(dx, dy, dz));
                           Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                              if (bat.isValid()) {
                                 bat.remove();
                              }

                           }, 100L);
                        } else {
                           p.getWorld().spawnParticle(Particle.SMOKE, batLoc, 5, 0.3, 0.3, 0.3, 0.05);
                        }
                     } catch (Exception var18) {
                        p.getWorld().spawnParticle(Particle.SMOKE, batLoc, 5, 0.3, 0.3, 0.3, 0.05);
                     }
                  }

                  p.playSound(p.getLocation(), Sound.ENTITY_BAT_AMBIENT, 1.0F, 1.0F);
                  break;
               }
               case "PAINTBALL": {
                  Block paintTarget = p.getTargetBlockExact(20);
                  if (paintTarget != null) {
                     Location loc = paintTarget.getLocation().add((double)0.5F, (double)0.5F, (double)0.5F);

                     try {
                        p.getWorld().spawnParticle(Particle.DUST, loc, 30, (double)0.5F, (double)0.5F, (double)0.5F, 0.01, new Particle.DustOptions(Color.fromRGB((int)(Math.random() * (double)255.0F), (int)(Math.random() * (double)255.0F), (int)(Math.random() * (double)255.0F)), 1.5F));
                     } catch (Exception var17) {
                     }

                     if (paintTarget.getType().isSolid() && !this.isContainerBlock(paintTarget.getType())) {
                        Location blockLoc = paintTarget.getLocation();
                        if (!this.paintedBlocks.containsKey(blockLoc)) {
                           this.paintedBlocks.put(blockLoc.clone(), paintTarget.getBlockData().clone());
                        }

                        Material[] paintColors = new Material[]{Material.RED_CONCRETE, Material.BLUE_CONCRETE, Material.GREEN_CONCRETE, Material.YELLOW_CONCRETE, Material.PURPLE_CONCRETE, Material.ORANGE_CONCRETE, Material.CYAN_CONCRETE, Material.MAGENTA_CONCRETE, Material.LIME_CONCRETE, Material.PINK_CONCRETE};
                        Material paint = paintColors[(new Random()).nextInt(paintColors.length)];
                        paintTarget.setType(paint, false);
                        Location bl = blockLoc.clone();
                        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                           BlockData original = (BlockData)this.paintedBlocks.remove(bl);
                           if (original != null) {
                              try {
                                 bl.getBlock().setBlockData(original);
                              } catch (Exception var4) {
                              }
                           }

                        }, 60L);
                     }
                  }

                  Snowball pb = (Snowball)p.launchProjectile(Snowball.class);
                  pb.setVelocity(p.getLocation().getDirection().multiply((double)2.5F));
                  p.playSound(p.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 1.0F, 1.5F);
                  break;
               }
               case "TNT_LAUNCHER": {
                  TNTPrimed tnt = (TNTPrimed)p.getWorld().spawn(p.getEyeLocation(), TNTPrimed.class);
                  tnt.setFuseTicks(40);
                  tnt.setYield(0.0F);
                  tnt.setVelocity(p.getLocation().getDirection().multiply(1.6));
                  p.playSound(p.getLocation(), Sound.ENTITY_TNT_PRIMED, 1.0F, 1.0F);
                  break;
               }
               case "ENDER_PEARL": {
                  final EnderPearl pearl = (EnderPearl)p.launchProjectile(EnderPearl.class);
                  pearl.setVelocity(p.getLocation().getDirection().multiply((double)2.0F));
                  p.playSound(p.getLocation(), Sound.ENTITY_ENDER_PEARL_THROW, 1.0F, 1.0F);
                  final UUID pearlOwner = p.getUniqueId();
                  int pearlTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, new Runnable() {
                     int ticks;

                     {
                        Objects.requireNonNull(CosmeticManager.this);
                        this.ticks = 0;
                     }

                     public void run() {
                        ++this.ticks;
                        Player rider = Bukkit.getPlayer(pearlOwner);
                        if (rider != null && rider.isOnline() && pearl.isValid() && this.ticks <= 120) {
                           try {
                              Location pearlLoc = pearl.getLocation();
                              pearlLoc.setYaw(rider.getLocation().getYaw());
                              pearlLoc.setPitch(rider.getLocation().getPitch());
                              rider.teleport(pearlLoc);
                              rider.setFallDistance(0.0F);
                           } catch (Exception var3) {
                           }

                        } else {
                           try {
                              Bukkit.getScheduler().cancelTask(Bukkit.getScheduler().scheduleSyncDelayedTask(CosmeticManager.this.plugin, () -> {
                              }));
                           } catch (Exception var4) {
                           }

                        }
                     }
                  }, 1L, 1L).getTaskId();
                  final int cancelId = pearlTaskId;
                  Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                     try {
                        Bukkit.getScheduler().cancelTask(cancelId);
                     } catch (Exception var2) {
                     }

                  }, 200L);
                  break;
               }
               case "JETPACK": {
                  Vector jetVec = p.getLocation().getDirection().setY(1.6);
                  p.setVelocity(jetVec);

                  for(int i = 0; i < 25; ++i) {
                     double angle = Math.random() * Math.PI * (double)2.0F;
                     double r = Math.random() * (double)0.5F;
                     double dx = Math.cos(angle) * r;
                     double dz = Math.sin(angle) * r;
                     p.getWorld().spawnParticle(Particle.FLAME, p.getLocation().add(dx, -0.3, dz), 1, (double)0.0F, (double)0.0F, (double)0.0F, 0.01);
                  }

                  p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
                  UUID jetUuid = p.getUniqueId();
                  Integer existing = (Integer)this.jetpackGlideTasks.remove(jetUuid);
                  if (existing != null) {
                     try {
                        Bukkit.getScheduler().cancelTask(existing);
                     } catch (Exception var16) {
                     }
                  }

                  int jetTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
                     Player player = Bukkit.getPlayer(jetUuid);
                     if (player != null && player.isOnline() && !player.isOnGround()) {
                        if (player.isSneaking()) {
                           Vector vel = player.getVelocity();
                           if (vel.getY() < -0.08) {
                              vel.setY(-0.08);
                              player.setVelocity(vel);
                              player.setFallDistance(0.0F);
                              player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 2, 0.2, 0.1, 0.2, 0.02);
                           }
                        }

                     } else {
                        Integer tid = (Integer)this.jetpackGlideTasks.remove(jetUuid);
                        if (tid != null) {
                           try {
                              Bukkit.getScheduler().cancelTask(tid);
                           } catch (Exception var5) {
                           }
                        }

                     }
                  }, 1L, 1L).getTaskId();
                  this.jetpackGlideTasks.put(jetUuid, jetTaskId);
                  break;
               }
            }
         } catch (Exception e) {
            this.plugin.getLogger().warning("[Cosmetics] Gadget action '" + action + "' failed: " + e.getMessage());
         }

      }
   }

   private boolean isCosmeticUsable(String id) {
      String path = "cosmetics." + id;
      if (!this.plugin.getConfig().contains(path)) {
         return false;
      } else {
         CosmeticType type = CosmeticType.fromString(this.plugin.getConfig().getString(path + ".type", "TRAIL"));
         switch (type) {
            case TRAIL:
               return this.resolveParticle(this.plugin.getConfig().getString(path + ".particle", "")) != null;
            case HAT:
               Material m = Material.matchMaterial(this.plugin.getConfig().getString(path + ".material", ""));
               return m != null && m.isItem();
            case GADGET:
               String action = this.plugin.getConfig().getString(path + ".action", "");
               return SUPPORTED_GADGET_ACTIONS.contains(action.toUpperCase());
            default:
               return false;
         }
      }
   }

   public static class CosmeticCategoryHolder implements InventoryHolder {
      private Inventory inventory;

      public void setInventory(Inventory inv) {
         this.inventory = inv;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }

   public static class CosmeticMenuHolder implements InventoryHolder {
      private Inventory inventory;
      private CosmeticType category;

      public void setInventory(Inventory inv) {
         this.inventory = inv;
      }

      public void setCategory(CosmeticType cat) {
         this.category = cat;
      }

      public CosmeticType getCategory() {
         return this.category;
      }

      public Inventory getInventory() {
         return this.inventory;
      }
   }
}
