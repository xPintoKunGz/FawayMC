package org.kcm.bloomery.fawaymc.friend.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.manager.FriendManager;

public class NetworkManager {
   private static final String CHANNEL = "fawayfriend:events";
   private static final String KEY_LOC = "fawayfriend:locations";
   private static final String KEY_PLAYERS = "fawayfriend:players";
   private static final String KEY_PENDING_TP = "fawayfriend:pending_tp";
   private final Global plugin;
   private final String serverName;
   private RedisBridge redis;
   private boolean enabled;
   private int heartbeatTaskId = -1;

   public NetworkManager(Global plugin) {
      this.plugin = plugin;
      this.serverName = plugin.getConfig().getString("network.server_name", "server-1");
   }

   public boolean enable() {
      if (!this.plugin.getConfig().getBoolean("network.enabled", false)) {
         this.plugin.getLogger().info("[Network] Cross-server disabled in config");
         return false;
      } else {
         try {
            Class.forName("redis.clients.jedis.JedisPool");
         } catch (ClassNotFoundException var9) {
            this.plugin.getLogger().warning("[Network] Jedis library not found — network disabled");
            return false;
         }

         String host = this.plugin.getConfig().getString("network.redis.host", "localhost");
         int port = this.plugin.getConfig().getInt("network.redis.port", 6379);
         String password = this.plugin.getConfig().getString("network.redis.password", "");
         int db = this.plugin.getConfig().getInt("network.redis.database", 0);
         int timeout = this.plugin.getConfig().getInt("network.redis.timeout", 2000);

         try {
            this.redis = new RedisBridge(host, port, password, db, timeout, this::handleMessage);
            if (!this.redis.ping()) {
               this.plugin.getLogger().warning("[Network] Redis ping failed");
               this.redis.close();
               this.redis = null;
               return false;
            } else {
               this.redis.startSubscriber("fawayfriend:events");
               this.plugin.getLogger().info("[Network] ✓ Redis connected (" + host + ":" + port + ")");
               if (this.plugin.getConfig().getBoolean("network.bungee.enabled", true)) {
                  try {
                     this.plugin.getServer().getMessenger().registerOutgoingPluginChannel(this.plugin, "BungeeCord");
                     this.plugin.getLogger().info("[Network] ✓ BungeeCord channel registered");
                  } catch (Throwable var7) {
                  }
               }

               this.startHeartbeat();
               this.enabled = true;
               return true;
            }
         } catch (Throwable e) {
            this.plugin.getLogger().warning("[Network] Redis init failed: " + e.getMessage());
            return false;
         }
      }
   }

   public void disable() {
      this.enabled = false;
      if (this.heartbeatTaskId != -1) {
         try {
            Bukkit.getScheduler().cancelTask(this.heartbeatTaskId);
         } catch (Throwable var5) {
         }

         this.heartbeatTaskId = -1;
      }

      if (this.redis != null) {
         for(Player p : Bukkit.getOnlinePlayers()) {
            try {
               this.redis.hdel("fawayfriend:locations", p.getUniqueId().toString());
               this.redis.hdel("fawayfriend:players", p.getUniqueId().toString());
            } catch (Throwable var4) {
            }
         }

         this.redis.close();
         this.redis = null;
      }

   }

   private void startHeartbeat() {
      int interval = this.plugin.getConfig().getInt("network.sync.player_location_interval", 5);
      this.heartbeatTaskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (this.redis != null) {
            for(Player p : Bukkit.getOnlinePlayers()) {
               this.updateLocation(p);
            }

         }
      }, 20L, (long)interval * 20L).getTaskId();
   }

   private void updateLocation(Player p) {
      if (this.redis != null) {
         try {
            Location loc = p.getLocation();
            String var10000 = this.serverName;
            String data = var10000 + "|" + loc.getWorld().getName() + "|" + loc.getX() + "|" + loc.getY() + "|" + loc.getZ() + "|" + loc.getYaw() + "|" + loc.getPitch();
            String uuid = p.getUniqueId().toString();
            this.redis.hset("fawayfriend:locations", uuid, data);
            this.redis.hset("fawayfriend:players", uuid, p.getName());
         } catch (Throwable var5) {
         }

      }
   }

   public void publishOnline(Player player) {
      if (this.enabled && this.redis != null) {
         this.updateLocation(player);
         RedisBridge var10000 = this.redis;
         String var10002 = String.valueOf(player.getUniqueId());
         var10000.publish("fawayfriend:events", "online|" + var10002 + "|" + player.getName() + "|" + this.serverName);
      }
   }

   public void publishOffline(UUID uuid) {
      if (this.enabled && this.redis != null) {
         this.redis.hdel("fawayfriend:locations", uuid.toString());
         this.redis.hdel("fawayfriend:players", uuid.toString());
         this.redis.publish("fawayfriend:events", "offline|" + String.valueOf(uuid));
      }
   }

   public void publishFriendRequest(UUID sender, UUID target) {
      if (this.enabled && this.redis != null) {
         RedisBridge var10000 = this.redis;
         String var10002 = String.valueOf(sender);
         var10000.publish("fawayfriend:events", "request|" + var10002 + "|" + String.valueOf(target));
      }
   }

   public void publishFriendAccepted(UUID a, UUID b) {
      if (this.enabled && this.redis != null) {
         RedisBridge var10000 = this.redis;
         String var10002 = String.valueOf(a);
         var10000.publish("fawayfriend:events", "accepted|" + var10002 + "|" + String.valueOf(b));
      }
   }

   public String getServerOf(UUID uuid) {
      if (this.enabled && this.redis != null) {
         String data = this.redis.hget("fawayfriend:locations", uuid.toString());
         if (data == null) {
            return null;
         } else {
            int idx = data.indexOf(124);
            return idx == -1 ? null : data.substring(0, idx);
         }
      } else {
         return null;
      }
   }

   public boolean isOnlineOnNetwork(UUID uuid) {
      return this.getServerOf(uuid) != null;
   }

   public void jumpToPlayer(Player player, UUID targetUuid) {
      if (!this.enabled) {
         player.sendMessage(this.color("&cCross-server is disabled!"));
      } else {
         String targetServer = this.getServerOf(targetUuid);
         if (targetServer == null) {
            player.sendMessage(this.color("&c&lFRIENDS &8» &cPlayer is not online on the network!"));
         } else if (targetServer.equals(this.serverName)) {
            Player target = Bukkit.getPlayer(targetUuid);
            if (target != null && target.isOnline()) {
               player.teleport(target.getLocation());
               player.sendMessage(this.color("&b&lFRIENDS &8» &aTeleported to &f" + target.getName()));
            } else {
               player.sendMessage(this.color("&c&lFRIENDS &8» &cPlayer went offline!"));
            }
         } else {
            if (this.redis != null) {
               int ttl = this.plugin.getConfig().getInt("network.sync.pending_teleport_ttl", 60);
               String pendingKey = this.serverName;
               String var10000 = targetUuid.toString();
               String pendingData = var10000 + "|" + (System.currentTimeMillis() + (long)ttl * 1000L);
               this.redis.hset("fawayfriend:pending_tp", player.getUniqueId().toString(), pendingData);
               Logger var11 = this.plugin.getLogger();
               String var10001 = player.getName();
               var11.info("[Network] Pending teleport saved: " + var10001 + " → " + String.valueOf(targetUuid));
            }

            try {
               ByteArrayOutputStream b = new ByteArrayOutputStream();
               DataOutputStream out = new DataOutputStream(b);
               out.writeUTF("Connect");
               out.writeUTF(targetServer);
               player.sendPluginMessage(this.plugin, "BungeeCord", b.toByteArray());
               out.close();
               player.sendMessage(this.color("&b&lFRIENDS &8» &aConnecting to &f" + targetServer + "&a..."));
            } catch (Throwable e) {
               player.sendMessage(this.color("&c&lFRIENDS &8» &cTransfer failed!"));
               this.plugin.getLogger().warning("[Network] Transfer failed: " + e.getMessage());
            }

         }
      }
   }

   public void checkPendingTeleport(Player player) {
      if (this.enabled && this.redis != null) {
         String data = this.redis.hget("fawayfriend:pending_tp", player.getUniqueId().toString());
         if (data != null) {
            this.redis.hdel("fawayfriend:pending_tp", player.getUniqueId().toString());

            try {
               String[] parts = data.split("\\|");
               if (parts.length < 2) {
                  return;
               }

               UUID targetUuid = UUID.fromString(parts[0]);
               long expireAt = Long.parseLong(parts[1]);
               if (System.currentTimeMillis() > expireAt) {
                  this.plugin.getLogger().info("[Network] Pending teleport expired for " + player.getName());
                  return;
               }

               Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
                  if (player.isOnline()) {
                     Player target = Bukkit.getPlayer(targetUuid);
                     if (target != null && target.isOnline()) {
                        player.teleport(target.getLocation());
                        player.sendMessage(this.color("&b&lFRIENDS &8» &aTeleported to &f" + target.getName()));
                     } else {
                        player.sendMessage(this.color("&c&lFRIENDS &8» &cTarget went offline!"));
                     }
                  }
               }, 60L);
            } catch (Throwable e) {
               this.plugin.getLogger().warning("[Network] Pending teleport error: " + e.getMessage());
            }

         }
      }
   }

   public void publishDirectMessage(UUID senderUuid, UUID targetUuid, String senderName, String targetName, String message) {
      if (this.enabled && this.redis != null) {
         String safe = message.replace("\\", "\\\\").replace("|", "\\|");
         RedisBridge var10000 = this.redis;
         String var10002 = String.valueOf(senderUuid);
         var10000.publish("fawayfriend:events", "dm|" + var10002 + "|" + String.valueOf(targetUuid) + "|" + senderName + "|" + targetName + "|" + safe);
      }
   }

   private void handleMessage(String channel, String message) {
      String[] parts = message.split("\\|");
      if (parts.length != 0) {
         String type = parts[0];
         Bukkit.getScheduler().runTask(this.plugin, () -> {
            try {
               switch (type) {
                  case "online":
                     if (parts.length >= 4) {
                        this.handleFriendOnline(UUID.fromString(parts[1]), parts[2], parts[3]);
                     }
                     break;
                  case "offline":
                     if (parts.length >= 2) {
                        this.handleFriendOffline(UUID.fromString(parts[1]));
                     }
                     break;
                  case "request":
                     if (parts.length >= 3) {
                        this.handleCrossRequest(UUID.fromString(parts[1]), UUID.fromString(parts[2]));
                     }
                     break;
                  case "accepted":
                     if (parts.length >= 3) {
                        this.handleCrossAccepted(UUID.fromString(parts[1]), UUID.fromString(parts[2]));
                     }
                     break;
                  case "dm":
                     if (parts.length >= 6) {
                        this.handleCrossDM(parts[1], parts[2], parts[3], parts[4], parts[5].replace("\\|", "|").replace("\\\\", "\\"));
                     }
               }
            } catch (Throwable var5) {
            }

         });
      }
   }

   private void handleCrossDM(String senderUuidStr, String targetUuidStr, String senderName, String targetName, String message) {
      try {
         UUID targetUuid = UUID.fromString(targetUuidStr);
         Player target = Bukkit.getPlayer(targetUuid);
         if (target == null || !target.isOnline()) {
            return;
         }

         target.sendMessage(this.color("&d&lDM &8» &f" + senderName + " &7→ &fYou&7: &f" + message));

         try {
            target.playSound(target.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.5F);
         } catch (Throwable var9) {
         }
      } catch (Throwable var10) {
      }

   }

   private void handleFriendOnline(UUID uuid, String name, String server) {
      FriendManager manager = this.plugin.getFriendManager();

      for(Player p : Bukkit.getOnlinePlayers()) {
         if (!p.getUniqueId().equals(uuid) && manager.getDataManager().isFriend(p.getUniqueId(), uuid) && manager.getSettingsCache(p.getUniqueId()).isNotifyOnline()) {
            String m = this.plugin.getConfig().getString("notify.friend_online.format", "&a&l✔ &f{friend} &ais now online &7({server})");
            p.sendMessage(this.color(m.replace("{friend}", name).replace("{server}", server)));
         }
      }

   }

   private void handleFriendOffline(UUID uuid) {
      FriendManager manager = this.plugin.getFriendManager();
      String name = manager.getDataManager().findPlayerName(uuid);
      if (name != null) {
         for(Player p : Bukkit.getOnlinePlayers()) {
            if (manager.getDataManager().isFriend(p.getUniqueId(), uuid) && manager.getSettingsCache(p.getUniqueId()).isNotifyOnline()) {
               String m = this.plugin.getConfig().getString("notify.friend_offline.format", "&c&l✘ &f{friend} &cis now offline");
               p.sendMessage(this.color(m.replace("{friend}", name)));
            }
         }

      }
   }

   private void handleCrossRequest(UUID sender, UUID target) {
      Player targetP = Bukkit.getPlayer(target);
      if (targetP != null) {
         String senderName = this.plugin.getFriendManager().getDataManager().findPlayerName(sender);
         if (senderName == null) {
            senderName = "Unknown";
         }

         if (!senderName.equals("Unknown")) {
            this.plugin.getCache().setName(sender, senderName);
         }

         String prefix = this.plugin.getConfig().getString("messages.prefix", "");
         String m = this.plugin.getConfig().getString("messages.request_received", "&e&l\ud83d\udce9 &f{sender} &esent you a friend request!");
         targetP.sendMessage(this.color(prefix + m.replace("{sender}", senderName)));
      }
   }

   private void handleCrossAccepted(UUID a, UUID b) {
      Player pa = Bukkit.getPlayer(a);
      if (pa != null) {
         String nameB = this.plugin.getFriendManager().getDataManager().findPlayerName(b);
         if (nameB == null) {
            nameB = "Unknown";
         }

         String prefix = this.plugin.getConfig().getString("messages.prefix", "");
         pa.sendMessage(this.color(prefix + "&a&l✔ &f" + nameB + " &aaccepted your friend request!"));
      }
   }

   private String color(String s) {
      return ChatColor.translateAlternateColorCodes('&', s);
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public String getServerName() {
      return this.serverName;
   }
}
