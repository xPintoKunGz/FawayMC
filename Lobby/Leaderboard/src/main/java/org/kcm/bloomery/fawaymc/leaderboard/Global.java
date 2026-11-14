package org.kcm.bloomery.fawaymc.leaderboard;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.kcm.bloomery.fawaymc.leaderboard.command.HologramCommand;
import org.kcm.bloomery.fawaymc.leaderboard.command.LeaderboardCommand;
import org.kcm.bloomery.fawaymc.leaderboard.gui.HologramEditGui;
import org.kcm.bloomery.fawaymc.leaderboard.hologram.ChatInputManager;
import org.kcm.bloomery.fawaymc.leaderboard.listener.PlayerListener;
import org.kcm.bloomery.fawaymc.leaderboard.manager.HologramManager;
import org.kcm.bloomery.fawaymc.leaderboard.manager.LeaderboardManager;
import org.kcm.bloomery.fawaymc.leaderboard.placeholder.LeaderboardExpansion;

public final class Global extends JavaPlugin {
   private static Global instance;
   private LeaderboardManager leaderboardManager;
   private HologramManager hologramManager;
   private ChatInputManager chatInput;
   private HologramEditGui hologramEditGui;

   public void onEnable() {
      instance = this;
      this.saveDefaultConfig();
      this.chatInput = new ChatInputManager(this);
      this.getServer().getPluginManager().registerEvents(this.chatInput, this);
      this.hologramManager = new HologramManager(this);
      this.hologramManager.startRealtimeTask();
      this.hologramEditGui = new HologramEditGui(this);
      HologramCommand holoCmd = new HologramCommand(this);
      if (this.getCommand("lbholo") != null) {
         this.getCommand("lbholo").setExecutor(holoCmd);
         this.getCommand("lbholo").setTabCompleter(holoCmd);
      }

      this.leaderboardManager = new LeaderboardManager(this);
      this.leaderboardManager.loadCache();
      this.leaderboardManager.registerProviders();
      this.leaderboardManager.importFromSources();
      this.leaderboardManager.startRefreshTask();
      this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
      LeaderboardCommand cmd = new LeaderboardCommand(this);
      if (this.getCommand("leaderboard") != null) {
         this.getCommand("leaderboard").setExecutor(cmd);
         this.getCommand("leaderboard").setTabCompleter(cmd);
      }

      if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
         try {
            (new LeaderboardExpansion(this)).register();
            this.getLogger().info("[FawayLeaderboard] PlaceholderAPI expansion registered!");
         } catch (Exception e) {
            this.getLogger().warning("[FawayLeaderboard] PAPI register failed: " + e.getMessage());
         }
      }

      int saveMin = this.getConfig().getInt("settings.cache_save_minutes", 5);
      if (saveMin > 0) {
         long ticks = (long)saveMin * 60L * 20L;
         Bukkit.getScheduler().runTaskTimer(this, () -> this.leaderboardManager.saveCache(), ticks, ticks);
      }

      this.getLogger().info("[FawayLeaderboard] enabled! Providers: " + String.valueOf(this.leaderboardManager.getRegisteredTypes()));
   }

   public void onDisable() {
      if (this.leaderboardManager != null) {
         this.leaderboardManager.stop();
      }

      if (this.hologramManager != null) {
         this.hologramManager.stop();
      }

      this.getLogger().info("[FawayLeaderboard] disabled.");
   }

   public static Global getInstance() {
      return instance;
   }

   public LeaderboardManager getLeaderboardManager() {
      return this.leaderboardManager;
   }

   public HologramManager getHologramManager() {
      return this.hologramManager;
   }

   public ChatInputManager getChatInput() {
      return this.chatInput;
   }

   public HologramEditGui getHologramEditGui() {
      return this.hologramEditGui;
   }
}
