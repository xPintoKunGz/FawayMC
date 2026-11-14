package org.kcm.bloomery.fawaymc.friend.listener;

import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.data.FriendData;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class PlayerListener implements Listener {
   private final Global plugin;
   private final FriendManager manager;

   public PlayerListener(Global plugin) {
      this.plugin = plugin;
      this.manager = plugin.getFriendManager();
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();
      if (this.plugin.getNetworkManager() != null) {
         this.plugin.getNetworkManager().publishOnline(player);
         this.plugin.getNetworkManager().checkPendingTeleport(player);
      }

      this.manager.loadPlayerAsync(uuid, player.getName(), () -> {
         if (player.isOnline()) {
            int pendingCount = this.manager.getPendingCache(uuid).size();
            if (pendingCount > 0) {
               player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&b&lFRIENDS &8» &fYou have &e" + pendingCount + " &fpending requests! &7(/friend requests)"));
            }

         }
      });
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      Player player = event.getPlayer();
      UUID uuid = player.getUniqueId();

      for(FriendData fd : this.manager.getFriendsCache(uuid)) {
         UUID fid = fd.getFriendUuid();
         Player fp = Bukkit.getPlayer(fid);
         if (fp != null && fp.isOnline() && this.manager.getSettingsCache(fid).isNotifyOnline()) {
            String m = this.plugin.getConfig().getString("notify.friend_offline.format", "&c&l✘ &f{friend} &cis now offline");
            fp.sendMessage(ChatColor.translateAlternateColorCodes('&', m.replace("{friend}", player.getName())));
         }
      }

      if (this.plugin.getNetworkManager() != null) {
         this.plugin.getNetworkManager().publishOffline(uuid);
      }

      this.manager.getCache().clear(uuid);
   }
}
