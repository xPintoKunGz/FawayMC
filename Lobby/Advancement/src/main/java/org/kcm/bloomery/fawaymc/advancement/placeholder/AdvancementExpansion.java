package org.kcm.bloomery.fawaymc.advancement.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.kcm.bloomery.fawaymc.advancement.Global;
import org.kcm.bloomery.fawaymc.advancement.data.Advancement;
import org.kcm.bloomery.fawaymc.advancement.data.PlayerProgress;

import java.util.List;

public class AdvancementExpansion extends PlaceholderExpansion {

   private final Global plugin;

   public AdvancementExpansion(Global plugin) {
      this.plugin = plugin;
   }

   @Override public String getIdentifier() { return "advancement"; }
   @Override public String getAuthor()     { return "KCM Bloomery"; }
   @Override public String getVersion()    { return plugin.getDescription().getVersion(); }
   @Override public boolean persist()      { return true; }

   @Override
   public String onRequest(OfflinePlayer offlinePlayer, String params) {
      if (offlinePlayer == null) return "";
      PlayerProgress prog = plugin.getDataManager().get(offlinePlayer.getUniqueId());
      if (prog == null) return "";

      int total = plugin.getAdvancementManager().getAdvancementCount();
      int unlocked = prog.getUnlocked().size();
      int percent = total > 0 ? (int) Math.round(unlocked * 100.0 / total) : 0;

      // ★ FIX: ประกาศตัวแปร lower ให้ถูกต้อง
      String lower = params.toLowerCase();

      // ═══════════════════════════════════════════════════════
      // Global placeholders
      // ═══════════════════════════════════════════════════════
      switch (lower) {
         case "unlocked":          return String.valueOf(unlocked);
         case "total":             return String.valueOf(total);
         case "percent":           return String.valueOf(percent);
         case "playtime":          return String.valueOf(prog.getPlaytimeMinutes());

         // ★ NEW: formatted variants
         case "unlocked_format":   return unlocked + "/" + total;
         case "unlocked_percent":  return unlocked + "/" + total + " (" + percent + "%)";

         // ★ NEW: progress bar (default 10 ช่อง)
         case "bar":
         case "progress_bar":      return buildBar(unlocked, total, 10);

         // ★ NEW: bar 20 ช่อง
         case "bar_20":            return buildBar(unlocked, total, 20);

         // ★ NEW: bar 5 ช่อง
         case "bar_5":             return buildBar(unlocked, total, 5);
      }

      // ═══════════════════════════════════════════════════════
      // Per-advancement placeholders
      // ═══════════════════════════════════════════════════════
      if (lower.startsWith("has_")) {
         return String.valueOf(prog.isUnlocked(params.substring(4)));
      }
      if (lower.startsWith("progress_")) {
         return String.valueOf(prog.getProgress(params.substring(9)));
      }
      if (lower.startsWith("required_")) {
         Advancement adv = plugin.getAdvancementManager().get(params.substring(9));
         return adv != null ? String.valueOf(adv.getRequired()) : "0";
      }

      // ═══════════════════════════════════════════════════════
      // ★ NEW: Per-category placeholders
      // ═══════════════════════════════════════════════════════
      if (lower.startsWith("unlocked_")) {
         String category = params.substring(9);
         return String.valueOf(countUnlockedInCategory(prog, category));
      }
      if (lower.startsWith("total_")) {
         String category = params.substring(6);
         return String.valueOf(countTotalInCategory(category));
      }
      if (lower.startsWith("percent_")) {
         String category = params.substring(8);
         int catTotal = countTotalInCategory(category);
         int catUnlocked = countUnlockedInCategory(prog, category);
         int catPercent = catTotal > 0
                 ? (int) Math.round(catUnlocked * 100.0 / catTotal) : 0;
         return String.valueOf(catPercent);
      }
      if (lower.startsWith("format_")) {
         String category = params.substring(7);
         int catTotal = countTotalInCategory(category);
         int catUnlocked = countUnlockedInCategory(prog, category);
         return catUnlocked + "/" + catTotal;
      }

      return "";
   }

   // ═══════════════════════════════════════════════════════════
   // Helpers
   // ═══════════════════════════════════════════════════════════

   /**
    * ★ สร้าง progress bar แบบข้อความ
    */
   private String buildBar(int unlocked, int total, int length) {
      int filled;
      if (total <= 0) {
         filled = 0;
      } else {
         filled = (int) Math.round((double) unlocked / total * length);
         filled = Math.max(0, Math.min(length, filled));
      }

      String filledChar = plugin.getConfig().getString(
              "placeholder.bar_filled", "&a█");
      String emptyChar = plugin.getConfig().getString(
              "placeholder.bar_empty", "&7░");

      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < filled; i++) sb.append(filledChar);
      for (int i = filled; i < length; i++) sb.append(emptyChar);
      return sb.toString();
   }

   /**
    * ★ นับ advancement ที่ unlock ใน category
    */
   private int countUnlockedInCategory(PlayerProgress prog, String category) {
      int count = 0;
      for (Advancement adv : plugin.getAdvancementManager().getByCategory(category)) {
         if (prog.isUnlocked(adv.getId())) count++;
      }
      return count;
   }

   /**
    * ★ นับ advancement ทั้งหมดใน category
    */
   private int countTotalInCategory(String category) {
      return plugin.getAdvancementManager().getByCategory(category).size();
   }
}