package org.kcm.bloomery.fawaymc.bungee.manager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.kcm.bloomery.fawaymc.bungee.Global;

public class MotdManager {
   private final Global plugin;
   private final Random random = new Random();
   private boolean randomize;
   private int maxPlayers;
   private String versionName;
   private List<String[]> motds = new ArrayList();

   public MotdManager(Global plugin) {
      this.plugin = plugin;
      this.loadConfig();
   }

   public void loadConfig() {
      try {
         if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdir();
         }

         File file = new File(this.plugin.getDataFolder(), "config.yml");
         if (!file.exists()) {
            InputStream in = this.plugin.getResourceAsStream("config.yml");

            try {
               Files.copy(in, file.toPath(), new CopyOption[0]);
            } catch (Throwable var10) {
               if (in != null) {
                  try {
                     in.close();
                  } catch (Throwable var9) {
                     var10.addSuppressed(var9);
                  }
               }

               throw var10;
            }

            if (in != null) {
               in.close();
            }
         }

         Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
         this.randomize = config.getBoolean("motd.randomize", true);
         this.maxPlayers = config.getInt("settings.max_players", 2024);
         this.versionName = config.getString("settings.version_name", "&aFawayMC");
         this.motds.clear();
         List<?> list = config.getList("motd.list");
         if (list != null) {
            for(Object obj : list) {
               if (obj instanceof Map) {
                  Map<?, ?> map = (Map)obj;
                  String line1 = (String)map.get("line1");
                  String line2 = (String)map.get("line2");
                  if (line1 != null && line2 != null) {
                     this.motds.add(new String[]{line1, line2});
                  }
               }
            }
         }

         this.plugin.getLogger().info("[MOTD] Loaded " + this.motds.size() + " MOTDs successfully.");
      } catch (IOException e) {
         this.plugin.getLogger().severe("Could not load config.yml: " + e.getMessage());
      }

   }

   public String[] getMotd() {
      if (this.motds.isEmpty()) {
         return new String[]{"&cFawayMC", "&7Config is empty!"};
      } else {
         String[] selected;
         if (this.randomize) {
            selected = (String[])this.motds.get(this.random.nextInt(this.motds.size()));
         } else {
            selected = (String[])this.motds.get(0);
         }

         return new String[]{ChatColor.translateAlternateColorCodes('&', selected[0]), ChatColor.translateAlternateColorCodes('&', selected[1])};
      }
   }

   public int getMaxPlayers() {
      return this.maxPlayers;
   }

   public String getVersionName() {
      return ChatColor.translateAlternateColorCodes('&', this.versionName);
   }
}
