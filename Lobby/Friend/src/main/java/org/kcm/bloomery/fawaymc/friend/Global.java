package org.kcm.bloomery.fawaymc.friend;

import org.bukkit.plugin.java.JavaPlugin;
import org.kcm.bloomery.fawaymc.friend.command.FriendAdminCommand;
import org.kcm.bloomery.fawaymc.friend.command.FriendCommand;
import org.kcm.bloomery.fawaymc.friend.data.FriendDataManager;
import org.kcm.bloomery.fawaymc.friend.listener.FriendGuiListener;
import org.kcm.bloomery.fawaymc.friend.listener.PlayerListener;
import org.kcm.bloomery.fawaymc.friend.manager.FriendCache;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;
import org.kcm.bloomery.fawaymc.friend.network.NetworkManager;

public class Global extends JavaPlugin {
   private FriendDataManager dataManager;
   private FriendCache cache;
   private FriendManager friendManager;
   private NetworkManager networkManager;

   public void onEnable() {
      this.saveDefaultConfig();
      this.dataManager = new FriendDataManager(this);
      if (!this.dataManager.connect()) {
         this.getLogger().severe("[Friends] Failed to connect to database!");
         this.getServer().getPluginManager().disablePlugin(this);
      } else {
         this.cache = new FriendCache();
         this.networkManager = new NetworkManager(this);
         boolean netOk = this.networkManager.enable();
         this.getLogger().info("[Friends] Network sync: " + (netOk ? "ENABLED" : "DISABLED"));
         this.friendManager = new FriendManager(this, this.dataManager, this.cache);
         FriendCommand cmd = new FriendCommand(this);
         if (this.getCommand("friend") != null) {
            this.getCommand("friend").setExecutor(cmd);
            this.getCommand("friend").setTabCompleter(cmd);
         }

         FriendAdminCommand adminCmd = new FriendAdminCommand(this);
         if (this.getCommand("friendadmin") != null) {
            this.getCommand("friendadmin").setExecutor(adminCmd);
            this.getCommand("friendadmin").setTabCompleter(adminCmd);
         }

         this.getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
         this.getServer().getPluginManager().registerEvents(new FriendGuiListener(this), this);
         if (this.getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
               (new FriendPlaceholders(this)).register();
               this.getLogger().info("[Friends] PlaceholderAPI expansion registered!");
            } catch (Exception e) {
               this.getLogger().warning("[Friends] PAPI register failed: " + e.getMessage());
            }
         } else {
            this.getLogger().warning("[Friends] PlaceholderAPI not found — placeholders disabled");
         }

         this.getServer().getPluginManager().registerEvents(new FriendGuiListener(this), this);
         this.getLogger().info("[Friends] Enabled successfully!");
      }
   }

   public void onDisable() {
      if (this.networkManager != null) {
         this.networkManager.disable();
      }

      if (this.dataManager != null) {
         this.dataManager.disconnect();
      }

      this.getLogger().info("[Friends] Disabled.");
   }

   public FriendDataManager getDataManager() {
      return this.dataManager;
   }

   public FriendCache getCache() {
      return this.cache;
   }

   public FriendManager getFriendManager() {
      return this.friendManager;
   }

   public NetworkManager getNetworkManager() {
      return this.networkManager;
   }
}
