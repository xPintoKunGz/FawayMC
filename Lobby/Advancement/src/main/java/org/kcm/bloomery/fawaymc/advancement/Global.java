package org.kcm.bloomery.fawaymc.advancement;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.kcm.bloomery.fawaymc.advancement.command.AdvancementCommand;
import org.kcm.bloomery.fawaymc.advancement.data.AdvancementDataManager;
import org.kcm.bloomery.fawaymc.advancement.listener.AdvancementGuiListener;
import org.kcm.bloomery.fawaymc.advancement.listener.PlayerListener;
import org.kcm.bloomery.fawaymc.advancement.manager.AdvancementManager;
import org.kcm.bloomery.fawaymc.advancement.manager.NotificationManager;
import org.kcm.bloomery.fawaymc.advancement.manager.TriggerDispatcher;
import org.kcm.bloomery.fawaymc.advancement.placeholder.AdvancementExpansion;

public final class Global extends JavaPlugin {
   private static Global instance;
   private AdvancementDataManager dataManager;
   private AdvancementManager advancementManager;
   private TriggerDispatcher triggerDispatcher;
   private NotificationManager notificationManager;

   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      this.dataManager = new AdvancementDataManager(this);
      this.advancementManager = new AdvancementManager(this);
      this.notificationManager = new NotificationManager(this);
      this.triggerDispatcher = new TriggerDispatcher(this);
      this.advancementManager.loadFromConfig();
      this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
      this.getServer().getPluginManager().registerEvents(this.triggerDispatcher, this);
      this.getServer().getPluginManager().registerEvents(new AdvancementGuiListener(this), this);
      AdvancementCommand cmd = new AdvancementCommand(this);
      if (this.getCommand("advancement") != null) {
         this.getCommand("advancement").setExecutor(cmd);
      }

      if (this.getCommand("advancementadmin") != null) {
         this.getCommand("advancementadmin").setExecutor(cmd);
         this.getCommand("advancementadmin").setTabCompleter(cmd);
      }

      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         try {
            (new AdvancementExpansion(this)).register();
            this.getLogger().info("[Global] PlaceholderAPI expansion registered!");
         } catch (Exception e) {
            this.getLogger().warning("[Global] PAPI failed: " + e.getMessage());
         }
      }

      this.dataManager.startAutoSave();
      this.getLogger().info("[Global] enabled! Loaded " + this.advancementManager.getAdvancementCount() + " advancements.");
   }

   public void onDisable() {
      if (this.dataManager != null) {
         this.dataManager.saveAll();
      }

      this.getLogger().info("[Global] disabled.");
   }

   public static Global getInstance() {
      return instance;
   }

   public AdvancementDataManager getDataManager() {
      return this.dataManager;
   }

   public AdvancementManager getAdvancementManager() {
      return this.advancementManager;
   }

   public TriggerDispatcher getTriggerDispatcher() {
      return this.triggerDispatcher;
   }

   public NotificationManager getNotificationManager() {
      return this.notificationManager;
   }
}
