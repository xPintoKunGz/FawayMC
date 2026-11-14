package org.kcm.bloomery.fawaymc.level;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.kcm.bloomery.fawaymc.level.command.LevelCommand;
import org.kcm.bloomery.fawaymc.level.data.LevelDataManager;
import org.kcm.bloomery.fawaymc.level.listener.PlayerListener;
import org.kcm.bloomery.fawaymc.level.manager.ExpBarManager;
import org.kcm.bloomery.fawaymc.level.manager.LevelManager;
import org.kcm.bloomery.fawaymc.level.manager.TablistManager;
import org.kcm.bloomery.fawaymc.level.placehdoler.LevelExpansion;

public final class Global extends JavaPlugin {
   private static Global instance;
   private LevelDataManager dataManager;
   private LevelManager levelManager;
   private TablistManager tablistManager;
   private ExpBarManager expBarManager;

   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      this.dataManager = new LevelDataManager(this);
      this.levelManager = new LevelManager(this);
      this.tablistManager = new TablistManager(this);
      this.expBarManager = new ExpBarManager(this);
      this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
      LevelCommand cmd = new LevelCommand(this);
      if (this.getCommand("level") != null) {
         this.getCommand("level").setExecutor(cmd);
      }

      if (this.getCommand("leveladmin") != null) {
         this.getCommand("leveladmin").setExecutor(cmd);
         this.getCommand("leveladmin").setTabCompleter(cmd);
      }

      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         try {
            (new LevelExpansion(this)).register();
            this.getLogger().info("[FawayLevel] PlaceholderAPI expansion registered!");
         } catch (Exception e) {
            this.getLogger().warning("[FawayLevel] PAPI register failed: " + e.getMessage());
         }
      }

      this.tablistManager.start();
      this.expBarManager.start();
      this.dataManager.startAutoSave();
      this.getLogger().info("[FawayLevel] enabled!");
   }

   public void onDisable() {
      if (this.dataManager != null) {
         this.dataManager.saveAll();
      }

      this.getLogger().info("[FawayLevel] disabled.");
   }

   public static Global getInstance() {
      return instance;
   }

   public LevelDataManager getDataManager() {
      return this.dataManager;
   }

   public LevelManager getLevelManager() {
      return this.levelManager;
   }

   public TablistManager getTablistManager() {
      return this.tablistManager;
   }

   public ExpBarManager getExpBarManager() {
      return this.expBarManager;
   }
}
