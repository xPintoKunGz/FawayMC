package org.kcm.bloomery.fawaymc.daily;

import org.bukkit.plugin.java.JavaPlugin;
import org.kcm.bloomery.fawaymc.daily.command.DailyAdminCommand;
import org.kcm.bloomery.fawaymc.daily.command.DailyCommand;
import org.kcm.bloomery.fawaymc.daily.data.DailyDataManager;
import org.kcm.bloomery.fawaymc.daily.listener.DailyGuiListener;
import org.kcm.bloomery.fawaymc.daily.listener.PlayerListener;
import org.kcm.bloomery.fawaymc.daily.manager.DailyCache;
import org.kcm.bloomery.fawaymc.daily.manager.DailyManager;

public class Global extends JavaPlugin {
   private DailyDataManager dataManager;
   private DailyCache cache;
   private DailyManager dailyManager;
   private DailyGuiListener guiListener;

   public void onEnable() {
      this.saveDefaultConfig();
      this.dataManager = new DailyDataManager(this);
      if (!this.dataManager.connect()) {
         this.getLogger().severe("[Daily] Failed to connect to database!");
         this.getServer().getPluginManager().disablePlugin(this);
      } else {
         this.cache = new DailyCache();
         this.dailyManager = new DailyManager(this, this.dataManager, this.cache);
         DailyCommand cmd = new DailyCommand(this);
         if (this.getCommand("daily") != null) {
            this.getCommand("daily").setExecutor(cmd);
            this.getCommand("daily").setTabCompleter(cmd);
         }

         DailyAdminCommand adminCmd = new DailyAdminCommand(this);
         if (this.getCommand("dailyadmin") != null) {
            this.getCommand("dailyadmin").setExecutor(adminCmd);
            this.getCommand("dailyadmin").setTabCompleter(adminCmd);
         }

         this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
         this.getServer().getPluginManager().registerEvents(new DailyGuiListener(this), this);
         this.guiListener = new DailyGuiListener(this);
         this.getServer().getPluginManager().registerEvents(this.guiListener, this);
         this.getLogger().info("[Daily] ✓ Enabled!");
      }
   }

   public void onDisable() {
      if (this.guiListener != null) {
         this.guiListener.stop();
      }

      if (this.dataManager != null) {
         this.dataManager.disconnect();
      }

      this.getLogger().info("[Daily] Disabled.");
   }

   public DailyDataManager getDataManager() {
      return this.dataManager;
   }

   public DailyCache getCache() {
      return this.cache;
   }

   public DailyManager getDailyManager() {
      return this.dailyManager;
   }
}
