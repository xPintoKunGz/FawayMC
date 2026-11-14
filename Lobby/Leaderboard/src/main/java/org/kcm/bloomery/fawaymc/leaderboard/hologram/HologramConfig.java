package org.kcm.bloomery.fawaymc.leaderboard.hologram;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class HologramConfig {
   public String id;
   public String type;
   public Location location;
   public boolean enabled = true;
   public int entries = 10;
   public double lineSpacing = 0.35;
   public boolean hideEmpty = false;
   public int totalPixelWidth = 240;
   public String dotsChar = ".";
   public String dotsColor = "&8";
   public String formatLine = "&e{rank_padded} &f{name} &8{dots} &e{value}";
   public List<String> header = new ArrayList();
   public List<String> footer = new ArrayList();
   public boolean showPersonalRank = true;
   public String personalFormat = "&7อันดับของคุณ: &e#{rank} &7- &f{value}";
   public String personalUnranked = "&7อันดับของคุณ: &cยังไม่มีอันดับ";

   public HologramConfig(String id, String type, Location location) {
      this.id = id;
      this.type = type;
      this.location = location;
      this.header.add("&e&l★ TOP " + type.toUpperCase() + " ★");
      this.header.add("&e&m─────────────────");
   }

   public void save(ConfigurationSection sec) {
      sec.set("type", this.type);
      sec.set("enabled", this.enabled);
      World w = this.location != null && this.location.getWorld() != null ? this.location.getWorld() : (World)Bukkit.getWorlds().get(0);
      sec.set("location.world", w.getName());
      sec.set("location.x", this.location != null ? this.location.getX() : (double)0.0F);
      sec.set("location.y", this.location != null ? this.location.getY() : (double)0.0F);
      sec.set("location.z", this.location != null ? this.location.getZ() : (double)0.0F);
      sec.set("location.yaw", this.location != null ? this.location.getYaw() : 0.0F);
      sec.set("layout.entries", this.entries);
      sec.set("layout.line_spacing", this.lineSpacing);
      sec.set("layout.hide_empty", this.hideEmpty);
      sec.set("format.line", this.formatLine);
      sec.set("format.pixel_width", this.totalPixelWidth);
      sec.set("format.dots_char", this.dotsChar);
      sec.set("format.dots_color", this.dotsColor);
      sec.set("format.show_personal", this.showPersonalRank);
      sec.set("format.personal_format", this.personalFormat);
      sec.set("format.personal_unranked", this.personalUnranked);
      sec.set("header", this.header);
      sec.set("footer", this.footer);
   }

   public static HologramConfig load(String id, ConfigurationSection sec) {
      String type = sec.getString("type", "advancement");
      String worldName = sec.getString("location.world", "world");
      World world = Bukkit.getWorld(worldName);
      if (world == null && !Bukkit.getWorlds().isEmpty()) {
         world = (World)Bukkit.getWorlds().get(0);
      }

      Location loc = new Location(world, sec.getDouble("location.x"), sec.getDouble("location.y"), sec.getDouble("location.z"), (float)sec.getDouble("location.yaw"), 0.0F);
      HologramConfig cfg = new HologramConfig(id, type, loc);
      cfg.enabled = sec.getBoolean("enabled", true);
      cfg.entries = sec.getInt("layout.entries", 10);
      cfg.lineSpacing = sec.getDouble("layout.line_spacing", 0.35);
      cfg.hideEmpty = sec.getBoolean("layout.hide_empty", false);
      cfg.formatLine = sec.getString("format.line", "&e{rank_padded} &f{name} &8{dots} &e{value}");
      cfg.totalPixelWidth = sec.getInt("format.pixel_width", 240);
      cfg.dotsChar = sec.getString("format.dots_char", ".");
      cfg.dotsColor = sec.getString("format.dots_color", "&8");
      cfg.showPersonalRank = sec.getBoolean("format.show_personal", true);
      cfg.personalFormat = sec.getString("format.personal_format", "&7อันดับของคุณ: &e#{rank} &7- &f{value}");
      cfg.personalUnranked = sec.getString("format.personal_unranked", "&7อันดับของคุณ: &cยังไม่มีอันดับ");
      cfg.header = new ArrayList(sec.getStringList("header"));
      cfg.footer = new ArrayList(sec.getStringList("footer"));
      if (cfg.header.isEmpty()) {
         cfg.header.add("&e&l★ TOP " + type.toUpperCase() + " ★");
         cfg.header.add("&e&m─────────────────");
      }

      return cfg;
   }
}
