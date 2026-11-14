package org.kcm.bloomery.fawaymc.leaderboard.hologram;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.TextDisplay.TextAlignment;
import org.kcm.bloomery.fawaymc.leaderboard.Global;
import org.kcm.bloomery.fawaymc.leaderboard.data.LeaderboardEntry;
import org.kcm.bloomery.fawaymc.leaderboard.util.FontUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LeaderboardHologram {

   private final Global plugin;
   private final HologramConfig config;
   private final List<TextDisplay> displays = new ArrayList<>();
   private final Map<UUID, TextDisplay> personalDisplays = new HashMap<>();
   private final Map<UUID, String> lastPersonalText = new HashMap<>();
   private final List<String> lastGlobalText = new ArrayList<>();

   private static final String[] RANK_ICONS = new String[]{
           "", "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩", "⑪", "⑫", "⑬", "⑭", "⑮"
   };

   public LeaderboardHologram(Global plugin, HologramConfig config) {
      this.plugin = plugin;
      this.config = config;
   }

   public HologramConfig getConfig() {
      return this.config;
   }

   public void update() {
      if (!this.config.enabled || this.config.location == null) return;

      List<String> newLines = new ArrayList<>();
      for (String line : this.config.header) {
         newLines.add(this.resolvePapi(line));
      }
      for (int i = 1; i <= this.config.entries; ++i) {
         LeaderboardEntry entry = this.plugin.getLeaderboardManager().getEntry(this.config.type, i);
         if (entry != null || !this.config.hideEmpty) {
            newLines.add(this.formatLine(i, entry));
         }
      }
      for (String line : this.config.footer) {
         newLines.add(this.resolvePapi(line));
      }

      if (this.displays.size() != newLines.size()) {
         this.render();
         return;
      }

      for (int i = 0; i < this.displays.size(); ++i) {
         String newText = newLines.get(i);
         if (!newText.equals(this.lastGlobalText.get(i))) {
            TextDisplay td = this.displays.get(i);
            if (td.isValid()) {
               td.text(LegacyComponentSerializer.legacyAmpersand().deserialize(newText));
            }
            this.lastGlobalText.set(i, newText);
         }
      }

      if (this.config.showPersonalRank) {
         for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld() == this.config.location.getWorld()
                    && p.getLocation().distanceSquared(this.config.location) <= 2500.0) {
               this.updatePersonalLine(p);
            } else {
               TextDisplay existing = this.personalDisplays.get(p.getUniqueId());
               if (existing != null) {
                  try {
                     p.hideEntity(this.plugin, existing);
                  } catch (Exception ignored) {}
               }
            }
         }
      }
   }

   private void updatePersonalLine(Player p) {
      int rank = this.plugin.getLeaderboardManager().getPlayerRank(p.getUniqueId(), this.config.type);
      long value = this.plugin.getLeaderboardManager().getPlayerValue(p.getUniqueId(), this.config.type);
      String rawText = rank > 0
              ? this.config.personalFormat
                .replace("{rank}", String.valueOf(rank))
                .replace("{value}", this.formatValue(value))
                .replace("{name}", p.getName())
              : this.config.personalUnranked;
      String text = ChatColor.translateAlternateColorCodes('&', rawText);

      TextDisplay td = this.personalDisplays.get(p.getUniqueId());

      if (td != null && td.isValid()) {
         String last = this.lastPersonalText.get(p.getUniqueId());
         if (!text.equals(last)) {
            if (td.isValid()) {
               td.text(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
            }
            this.lastPersonalText.put(p.getUniqueId(), text);
         }

         if (p.isOnline() && td.isValid() && !p.canSee(td)) {
            try {
               p.showEntity(this.plugin, td);
            } catch (Exception ignored) {}
         }
      } else {
         double yOffset = -(this.config.lineSpacing + 0.5);
         Location loc = this.config.location.clone().add(0.0, yOffset, 0.0);
         loc.setPitch(0.0F);
         loc.setYaw(0.0F);

         td = p.getWorld().spawn(loc, TextDisplay.class, d -> {
            d.setBillboard(Billboard.VERTICAL);
            d.setVisibleByDefault(false);
            d.setSeeThrough(false);
            d.setShadowed(true);
            d.setDefaultBackground(false);
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setPersistent(false);
            d.setInvulnerable(true);
            d.setGravity(false);
            d.setViewRange(1.0F);
            d.setAlignment(TextAlignment.CENTER);
            d.text(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
         });

         this.personalDisplays.put(p.getUniqueId(), td);
         this.lastPersonalText.put(p.getUniqueId(), text);

         // ★ FIX: สร้าง final variable ใหม่เพื่อให้ Lambda ใช้ได้
         final TextDisplay finalTd = td;
         Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
            if (p.isOnline() && finalTd.isValid()) {
               p.showEntity(this.plugin, finalTd);
            }
         }, 2L);
      }
   }

   public void render() {
      this.cleanup();
      if (!this.config.enabled || this.config.location == null) return;

      World world = this.config.location.getWorld();
      Location base = this.config.location.clone();
      base.setPitch(0.0F);
      base.setYaw(0.0F);

      List<String> lines = new ArrayList<>();
      for (String line : this.config.header) {
         lines.add(this.resolvePapi(line));
      }
      for (int i = 1; i <= this.config.entries; ++i) {
         LeaderboardEntry entry = this.plugin.getLeaderboardManager().getEntry(this.config.type, i);
         if (entry != null || !this.config.hideEmpty) {
            lines.add(this.formatLine(i, entry));
         }
      }
      for (String line : this.config.footer) {
         lines.add(this.resolvePapi(line));
      }

      this.lastGlobalText.clear();

      for (int i = 0; i < lines.size(); ++i) {
         double yOffset = (double) (lines.size() - 1 - i) * this.config.lineSpacing;
         Location rowLoc = new Location(world, base.getX(), base.getY() + yOffset, base.getZ(), 0.0F, 0.0F);
         this.spawnText(world, rowLoc, lines.get(i));
         this.lastGlobalText.add(lines.get(i));
      }

      for (TextDisplay d : this.personalDisplays.values()) {
         try {
            if (d.isValid()) d.remove();
         } catch (Exception ignored) {}
      }
      this.personalDisplays.clear();
      this.lastPersonalText.clear();
   }

   private String formatLine(int rank, LeaderboardEntry entry) {
      String rankStr = this.getRankIcon(rank);
      String nameRaw;
      String valueRaw;

      if (entry == null) {
         String emptyMsg = this.plugin.getConfig().getString(
                 "leaderboards." + this.config.type + ".empty", "&7No data yet");
         if (emptyMsg.contains("...")) {
            emptyMsg = "&7No data yet";
         }
         nameRaw = emptyMsg;
         valueRaw = "0";
      } else {
         nameRaw = entry.getPlayerName() != null ? entry.getPlayerName() : "Unknown";
         valueRaw = this.formatValue(entry.getValue());
      }

      int usedPixels = FontUtil.getStringWidth(rankStr) + 6
              + FontUtil.getStringWidth(nameRaw) + 6
              + FontUtil.getStringWidth(valueRaw);
      int remainingPixels = this.config.totalPixelWidth - usedPixels;
      int dotsCount = Math.max(2, remainingPixels / 3);

      StringBuilder dots = new StringBuilder();
      for (int i = 0; i < dotsCount; ++i) {
         dots.append(".");
      }

      String out = this.config.formatLine
              .replace("{rank}", rankStr)
              .replace("{rank_padded}", rankStr)
              .replace("{name}", nameRaw)
              .replace("{value}", valueRaw)
              .replace("{dots}", this.config.dotsColor + dots)
              .replace("{raw_name}", nameRaw)
              .replace("{raw_value}", valueRaw);

      return this.resolvePapi(ChatColor.translateAlternateColorCodes('&', out));
   }

   private void spawnText(World world, Location loc, String text) {
      try {
         TextDisplay display = world.spawn(loc, TextDisplay.class, td -> {
            td.text(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
            td.setAlignment(TextAlignment.CENTER);
            td.setBillboard(Billboard.VERTICAL);
            td.setSeeThrough(false);
            td.setShadowed(true);
            td.setDefaultBackground(false);
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            td.setPersistent(false);
            td.setInvulnerable(true);
            td.setGravity(false);
            td.setViewRange(1.0F);
            td.setRotation(0.0F, 0.0F);
         });
         this.displays.add(display);
      } catch (Exception e) {
         this.plugin.getLogger().warning("[Hologram] spawnText failed: " + e.getMessage());
      }
   }

   public void cleanup() {
      for (TextDisplay d : this.displays) {
         try {
            if (d.isValid()) d.remove();
         } catch (Exception ignored) {}
      }
      this.displays.clear();

      for (TextDisplay d : this.personalDisplays.values()) {
         try {
            if (d.isValid()) d.remove();
         } catch (Exception ignored) {}
      }
      this.personalDisplays.clear();
      this.lastPersonalText.clear();
      this.lastGlobalText.clear();
   }

   private String getRankIcon(int rank) {
      if (rank >= 1 && rank < RANK_ICONS.length) {
         return "&e" + RANK_ICONS[rank];
      }
      return "&e#" + rank;
   }

   private String formatValue(long v) {
      if (v >= 1_000_000L) {
         return String.format("%.1fM", (double) v / 1_000_000.0);
      } else if (v >= 1_000L) {
         return String.format("%.1fk", (double) v / 1_000.0);
      }
      return String.valueOf(v);
   }

   private String resolvePapi(String text) {
      if (text == null) return "";
      if (text.isEmpty()) return text;
      if (!text.contains("%")) return text;
      if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return text;
      try {
         return PlaceholderAPI.setPlaceholders(null, text);
      } catch (Throwable ignored) {
         return text;
      }
   }
}