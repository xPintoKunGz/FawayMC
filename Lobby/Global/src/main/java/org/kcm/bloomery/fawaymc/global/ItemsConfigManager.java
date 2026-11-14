package org.kcm.bloomery.fawaymc.global;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemsConfigManager {
   private final JavaPlugin plugin;
   private File itemsFile;
   private FileConfiguration itemsConfig;

   public ItemsConfigManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.createConfig();
   }

   public void createConfig() {
      this.itemsFile = new File(this.plugin.getDataFolder(), "items.yml");
      if (!this.itemsFile.exists()) {
         this.plugin.saveResource("items.yml", false);
      }

      this.itemsConfig = YamlConfiguration.loadConfiguration(this.itemsFile);

      try {
         InputStreamReader reader = new InputStreamReader(this.plugin.getResource("items.yml"), StandardCharsets.UTF_8);

         try {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            this.itemsConfig.setDefaults(defConfig);
         } catch (Throwable var5) {
            try {
               reader.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         reader.close();
      } catch (Exception var6) {
      }

      Logger var10000 = this.plugin.getLogger();
      boolean var10001 = this.itemsConfig.getBoolean("gui.open_item.enabled", true);
      var10000.info("[Items] config loaded | gui=" + var10001 + " | profile=" + this.itemsConfig.getBoolean("profile.item.enabled", true) + " | visibility=" + this.itemsConfig.getBoolean("visibility.item.enabled", true));
   }

   public FileConfiguration getConfig() {
      return this.itemsConfig;
   }

   public void reload() {
      if (this.itemsFile == null) {
         this.itemsFile = new File(this.plugin.getDataFolder(), "items.yml");
      }

      this.itemsConfig = YamlConfiguration.loadConfiguration(this.itemsFile);

      try {
         InputStreamReader reader = new InputStreamReader(this.plugin.getResource("items.yml"), StandardCharsets.UTF_8);

         try {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(reader);
            this.itemsConfig.setDefaults(defConfig);
         } catch (Throwable var5) {
            try {
               reader.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }

            throw var5;
         }

         reader.close();
      } catch (Exception var6) {
      }

   }

   public void save() {
      if (this.itemsConfig != null && this.itemsFile != null) {
         try {
            this.itemsConfig.save(this.itemsFile);
         } catch (IOException e) {
            this.plugin.getLogger().severe("ไม่สามารถบันทึก items.yml ได้: " + e.getMessage());
         }

      }
   }
}
