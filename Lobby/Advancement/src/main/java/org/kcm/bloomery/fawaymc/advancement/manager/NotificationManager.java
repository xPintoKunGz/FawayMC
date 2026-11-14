package org.kcm.bloomery.fawaymc.advancement.manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.kcm.bloomery.fawaymc.advancement.Global;
import org.kcm.bloomery.fawaymc.advancement.data.Advancement;
import org.kcm.bloomery.fawaymc.advancement.data.PlayerProgress;

public class NotificationManager {
   private final Global plugin;
   private final Map<UUID, Long> lastProgressMsg = new HashMap();
   private static final long PROGRESS_THROTTLE_MS = 1500L;

   public NotificationManager(Global plugin) {
      this.plugin = plugin;
   }

   public void notifyUnlock(Player player, Advancement adv) {
      if (player != null && player.isOnline() && adv != null) {
         String tierColor = this.plugin.getConfig().getString("tiers." + adv.getTier() + ".color", "&7");
         String tierName = this.plugin.getConfig().getString("tiers." + adv.getTier() + ".name", "Common");
         String chat = this.plugin.getConfig().getString("messages.unlocked", "&a&l✔ &aAdvancement Unlocked: &e{name}");
         player.sendMessage(this.color(chat.replace("{name}", adv.getName())));
         List<String> rewardLines = this.buildRewardChatLines(adv);
         if (!rewardLines.isEmpty()) {
            player.sendMessage(this.color("&7&oRewards:"));

            for(String line : rewardLines) {
               player.sendMessage(line);
            }
         }

         try {
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(tierColor + "&l✔ &f" + adv.getName() + " &7(" + tierColor + tierName + "&7)"));
         } catch (Exception var10) {
         }

         if (this.plugin.getConfig().getBoolean("display.toast_enabled", true)) {
            this.showAdvancementToast(player, adv);
         }

         if (this.plugin.getConfig().getBoolean("display.sound_on_unlock", true)) {
            this.playUnlockSound(player, adv.getTier());
         }

         if (adv.getTier().equalsIgnoreCase("legendary") || adv.getTier().equalsIgnoreCase("mythic")) {
            try {
               Firework fw = (Firework)player.getWorld().spawn(player.getLocation(), Firework.class);
               FireworkMeta fm = fw.getFireworkMeta();
               fm.addEffect(FireworkEffect.builder().withColor(new Color[]{Color.fromRGB(255, 215, 0), Color.PURPLE}).with(Type.BALL_LARGE).trail(true).flicker(true).build());
               fm.setPower(1);
               fw.setFireworkMeta(fm);
            } catch (Exception var9) {
            }
         }

      }
   }

   public void notifyProgress(Player player, Advancement adv, int current) {
      if (player != null && player.isOnline() && adv != null) {
         if (adv.getRequired() > 1) {
            int required = adv.getRequired();
            if (current < required) {
               long now = System.currentTimeMillis();
               Long last = (Long)this.lastProgressMsg.get(player.getUniqueId());
               if (last == null || now - last >= 1500L) {
                  this.lastProgressMsg.put(player.getUniqueId(), now);
                  int percent = (int)Math.round((double)current * (double)100.0F / (double)required);
                  boolean milestone = current == 1 || percent == 25 || percent == 50 || percent == 75;
                  if (milestone) {
                     try {
                        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize("&e" + adv.getName() + " &7- &f" + current + "&7/&f" + required + " &8(" + percent + "%)"));
                     } catch (Exception var11) {
                     }

                  }
               }
            }
         }
      }
   }

   public void notifyCategoryComplete(Player player, String categoryId) {
      if (player != null && player.isOnline()) {
         String catName = this.plugin.getConfig().getString("categories." + categoryId + ".name", categoryId);
         player.sendMessage(this.color("&6&l✦ &e&lCATEGORY COMPLETE! &6&l✦"));
         player.sendMessage(this.color("&7You unlocked every advancement in " + catName + " &7!"));

         try {
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize("&6&l✦ &eCATEGORY COMPLETE &6&l✦"));
         } catch (Exception var9) {
         }

         try {
            String title = "Category Complete!";
            String desc = ChatColor.stripColor(this.color(catName));
            String bg = this.plugin.getConfig().getString("display.toast.background", "minecraft:textures/block/stone.png");
            this.showCustomToast(player, title, desc, "minecraft:nether_star", "challenge", bg);
         } catch (Exception var8) {
         }

         try {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.4F);
         } catch (Exception var7) {
         }

      }
   }

   public void notifyMilestone(Player player, int percent) {
      if (player != null && player.isOnline()) {
         player.sendMessage(this.color("&6&l✦ &e" + percent + "% ADVANCEMENT COMPLETE! &6&l✦"));

         try {
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize("&6&l✦ &e" + percent + "% &6&l✦"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0F, 1.5F);
         } catch (Exception var4) {
         }

      }
   }

   public void broadcast(Player player, Advancement adv) {
      if (player != null && adv != null) {
         String msg = this.plugin.getConfig().getString("messages.unlocked_broadcast", "&6{player} &eunlocked &6{name}&e!");
         msg = msg.replace("{player}", player.getName()).replace("{name}", adv.getName());
         Bukkit.broadcastMessage(this.color(msg));

         try {
            for(Player p : Bukkit.getOnlinePlayers()) {
               p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6F, 1.2F);
            }
         } catch (Exception var6) {
         }

      }
   }

   private void showAdvancementToast(Player player, Advancement adv) {
      try {
         String title = this.stripColor(adv.getName());
         String description = this.stripColor(adv.getDescription());
         if (description.isEmpty()) {
            description = "Advancement unlocked!";
         }

         if (description.length() > 32) {
            description = description.substring(0, 32);
         }

         String iconKey = this.plugin.getConfig().getString("display.toast.icon_override", "");
         if (iconKey == null || iconKey.isEmpty()) {
            iconKey = adv.getIcon().getKey().toString();
         }

         if (!iconKey.contains(":")) {
            iconKey = "minecraft:" + iconKey;
         }

         String pattern = this.plugin.getConfig().getString("display.toast.pattern", "AUTO");
         String frame = this.resolveFrame(pattern, adv.getTier());
         String bg = this.plugin.getConfig().getString("display.toast.background", "minecraft:textures/block/stone.png");
         String soundIn = this.plugin.getConfig().getString("display.toast.sound_in", "UI_TOAST_IN");
         this.playToastSound(player, soundIn, 1.0F, 1.0F);
         this.showCustomToast(player, title, description, iconKey, frame, bg);
         if ("challenge".equals(frame)) {
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               String soundOut = this.plugin.getConfig().getString("display.toast.sound_out", "UI_TOAST_CHALLENGE_COMPLETE");
               this.playToastSound(player, soundOut, 1.0F, 1.0F);
            }, 8L);
         }
      } catch (Exception e) {
         this.plugin.getLogger().warning("[AdvToast] showAdvancementToast failed: " + e.getMessage());
      }

   }

   private String resolveFrame(String pattern, String tier) {
      if (pattern == null) {
         pattern = "AUTO";
      }

      switch (pattern.toUpperCase()) {
         case "TASK":
            return "task";
         case "GOAL":
            return "goal";
         case "CHALLENGE":
            return "challenge";
         case "AUTO":
         default:
            if (tier == null) {
               return "task";
            } else {
               switch (tier.toLowerCase()) {
                  case "legendary":
                  case "mythic":
                     return "challenge";
                  case "epic":
                  case "rare":
                     return "goal";
                  default:
                     return "task";
               }
            }
      }
   }

   private void playToastSound(Player player, String soundName, float volume, float pitch) {
      if (player != null && player.isOnline()) {
         if (soundName != null && !soundName.isEmpty()) {
            try {
               Sound sound = (Sound)Registry.SOUNDS.get(NamespacedKey.minecraft(soundName.toLowerCase()));
               if (sound == null) {
                  String normalized = soundName.toLowerCase().replace('_', '.');
                  sound = (Sound)Registry.SOUNDS.get(NamespacedKey.minecraft(normalized));
               }

               if (sound == null) {
                  return;
               }

               player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (Exception var7) {
            }

         }
      }
   }

   private void showCustomToast(Player player, String title, String description, String iconKey, String frame, String background) {
      try {
         if (iconKey == null || !iconKey.contains(":")) {
            iconKey = "minecraft:stone";
         }

         if (background == null || background.isEmpty()) {
            background = "minecraft:textures/block/stone.png";
         }

         String json = "{\"criteria\":{\"trigger\":{\"trigger\":\"minecraft:impossible\"}},\"display\":{\"icon\":{\"id\":\"" + iconKey + "\"},\"title\":{\"text\":\"" + this.escapeJson(title) + "\"},\"description\":{\"text\":\"" + this.escapeJson(description) + "\"},\"frame\":\"" + frame + "\",\"background\":\"" + background + "\",\"show_toast\":true,\"announce_to_chat\":false,\"hidden\":true}}";
         String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
         NamespacedKey key = new NamespacedKey(this.plugin, "toast_" + suffix);
         org.bukkit.advancement.Advancement bukkitAdv = Bukkit.getUnsafe().loadAdvancement(key, json);
         if (bukkitAdv == null) {
            return;
         }

         AdvancementProgress progress = player.getAdvancementProgress(bukkitAdv);

         for(String criteria : progress.getRemainingCriteria()) {
            progress.awardCriteria(criteria);
         }

         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            try {
               Bukkit.getUnsafe().removeAdvancement(key);
            } catch (Exception var2) {
            }

         }, 40L);
      } catch (Exception e) {
         this.plugin.getLogger().warning("[AdvToast] showCustomToast failed: " + e.getMessage());
      }

   }

   private String escapeJson(String s) {
      return s == null ? "" : s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
   }

   private String stripColor(String s) {
      return s == null ? "" : ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s));
   }

   private List<String> buildRewardChatLines(Advancement adv) {
      List<String> lines = new ArrayList();
      ConfigurationSection reward = adv.getRewardConfig();
      if (reward == null) {
         return lines;
      } else {
         long coins = reward.getLong("coins", 0L);
         long exp = reward.getLong("exp", 0L);
         if (coins > 0L) {
            lines.add(this.color(" &8▪ &6+" + coins + " Coins"));
         }

         if (exp > 0L) {
            lines.add(this.color(" &8▪ &e+" + exp + " EXP"));
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
            lines.add(this.color(" &8▪ " + boxColor + "+" + amount + "x " + boxName + " Box"));
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

   private void playUnlockSound(Player player, String tier) {
      try {
         Sound sound;
         float pitch;
         switch (tier.toLowerCase()) {
            case "legendary":
               sound = Sound.UI_TOAST_CHALLENGE_COMPLETE;
               pitch = 0.8F;
               break;
            case "mythic":
               sound = Sound.ENTITY_ENDER_DRAGON_GROWL;
               pitch = 1.2F;
               break;
            case "epic":
               sound = Sound.ENTITY_PLAYER_LEVELUP;
               pitch = 1.4F;
               break;
            case "rare":
               sound = Sound.BLOCK_NOTE_BLOCK_BELL;
               pitch = 1.6F;
               break;
            default:
               sound = Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
               pitch = 1.2F;
         }

         player.playSound(player.getLocation(), sound, 1.0F, pitch);
      } catch (Exception var7) {
      }

   }

   public void afterUnlock(Player player, Advancement adv) {
      if (player != null && player.isOnline()) {
         List<Advancement> allInCat = this.plugin.getAdvancementManager().getByCategory(adv.getCategory());
         PlayerProgress prog = this.plugin.getDataManager().get(player.getUniqueId());
         boolean categoryDone = !allInCat.isEmpty();

         for(Advancement a : allInCat) {
            if (!prog.isUnlocked(a.getId())) {
               categoryDone = false;
               break;
            }
         }

         if (categoryDone) {
            this.notifyCategoryComplete(player, adv.getCategory());
         }

         int total = this.plugin.getAdvancementManager().getAdvancementCount();
         int unlocked = prog.getUnlocked().size();
         if (total > 0) {
            int percent = (int)Math.round((double)unlocked * (double)100.0F / (double)total);
            if (percent == 25 || percent == 50 || percent == 75 || percent == 100) {
               this.notifyMilestone(player, percent);
            }
         }

      }
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
   }
}
