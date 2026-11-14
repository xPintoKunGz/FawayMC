package org.kcm.bloomery.fawaymc.friend.manager;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.kcm.bloomery.fawaymc.friend.FriendResult;
import org.kcm.bloomery.fawaymc.friend.Global;
import org.kcm.bloomery.fawaymc.friend.data.BlockData;
import org.kcm.bloomery.fawaymc.friend.data.FriendData;
import org.kcm.bloomery.fawaymc.friend.data.FriendDataManager;
import org.kcm.bloomery.fawaymc.friend.data.FriendRequest;
import org.kcm.bloomery.fawaymc.friend.data.PlayerSettings;
import org.kcm.bloomery.fawaymc.friend.network.NetworkManager;

public class FriendManager {
   private final Plugin plugin;
   private final FriendDataManager dataManager;
   private final FriendCache cache;

   public FriendManager(Plugin plugin, FriendDataManager dataManager, FriendCache cache) {
      this.plugin = plugin;
      this.dataManager = dataManager;
      this.cache = cache;
   }

   public FriendCache getCache() {
      return this.cache;
   }

   public FriendDataManager getDataManager() {
      return this.dataManager;
   }

   public int getMaxFriends() {
      return this.plugin.getConfig().getInt("limits.max_friends", 50);
   }

   private NetworkManager net() {
      Plugin var2 = this.plugin;
      if (var2 instanceof Global g) {
         return g.getNetworkManager();
      } else {
         return null;
      }
   }

   private Sound resolveSound(String name) {
      if (name != null && !name.isEmpty()) {
         try {
            String normalized = name.trim().toLowerCase();
            Sound s = (Sound)Registry.SOUNDS.get(NamespacedKey.minecraft(normalized));
            if (s != null) {
               return s;
            } else {
               s = (Sound)Registry.SOUNDS.get(NamespacedKey.minecraft(normalized.replace('_', '.')));
               return s;
            }
         } catch (Throwable var4) {
            return null;
         }
      } else {
         return null;
      }
   }

   private String msg(String key) {
      String raw = this.plugin.getConfig().getString("messages." + key, "&cMissing: " + key);
      String prefix = this.plugin.getConfig().getString("messages.prefix", "");
      return ChatColor.translateAlternateColorCodes('&', prefix + raw);
   }

   public void loadPlayerAsync(UUID uuid, String name, Runnable onDone) {
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         this.dataManager.cachePlayer(uuid, name);
         this.cache.setName(uuid, name);
         List<FriendData> friends = this.dataManager.getFriends(uuid);
         this.cache.setFriends(uuid, friends);

         for(FriendData fd : friends) {
            if (this.cache.getName(fd.getFriendUuid()) == null) {
               String fn = this.dataManager.findPlayerName(fd.getFriendUuid());
               if (fn != null) {
                  this.cache.setName(fd.getFriendUuid(), fn);
               }
            }
         }

         List<FriendRequest> pending = this.dataManager.getPendingRequests(uuid);
         this.cache.setPending(uuid, pending);

         for(FriendRequest req : pending) {
            if (this.cache.getName(req.getSenderUuid()) == null) {
               String sn = this.dataManager.findPlayerName(req.getSenderUuid());
               if (sn != null) {
                  this.cache.setName(req.getSenderUuid(), sn);
               }
            }
         }

         this.cache.setSettings(uuid, this.dataManager.getSettings(uuid));
         if (onDone != null) {
            Bukkit.getScheduler().runTask(this.plugin, onDone);
         }

      });
   }

   public List<FriendData> getFriendsCache(UUID uuid) {
      return this.cache.getFriends(uuid);
   }

   public List<FriendRequest> getPendingCache(UUID uuid) {
      return this.cache.getPending(uuid);
   }

   public PlayerSettings getSettingsCache(UUID uuid) {
      return this.cache.getSettings(uuid);
   }

   public void sendRequest(Player sender, String targetName, Consumer<FriendResult> callback) {
      UUID senderUuid = sender.getUniqueId();
      this.cache.setName(senderUuid, sender.getName());
      this.dataManager.cachePlayer(senderUuid, sender.getName());
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         UUID targetUuid = this.resolveTarget(targetName);
         if (targetUuid == null) {
            FriendResult result = FriendResult.failure(FriendResult.Status.PLAYER_NOT_FOUND, this.msg("player_not_found").replace("{player}", targetName), targetName);
            this.complete(callback, result);
         } else {
            String targetDisplay = this.resolveName(targetUuid, targetName);
            if (senderUuid.equals(targetUuid)) {
               FriendResult result = FriendResult.failure(FriendResult.Status.CANNOT_ADD_SELF, this.msg("cannot_add_self"), targetDisplay);
               this.complete(callback, result);
            } else if (this.dataManager.isFriend(senderUuid, targetUuid)) {
               FriendResult result = FriendResult.failure(FriendResult.Status.ALREADY_FRIENDS, this.msg("already_friends").replace("{player}", targetDisplay), targetDisplay);
               this.complete(callback, result);
            } else if (this.dataManager.isBlocked(targetUuid, senderUuid)) {
               FriendResult result = FriendResult.failure(FriendResult.Status.ERROR, this.msg("cannot_send_request").replace("{player}", targetDisplay), targetDisplay);
               this.complete(callback, result);
            } else if (this.dataManager.isBlocked(senderUuid, targetUuid)) {
               FriendResult result = FriendResult.failure(FriendResult.Status.ERROR, this.msg("you_blocked_target").replace("{player}", targetDisplay), targetDisplay);
               this.complete(callback, result);
            } else if (this.dataManager.hasRequest(senderUuid, targetUuid)) {
               FriendResult result = FriendResult.failure(FriendResult.Status.ALREADY_REQUESTED, this.msg("already_requested").replace("{player}", targetDisplay), targetDisplay);
               this.complete(callback, result);
            } else if (this.dataManager.countFriends(senderUuid) >= this.getMaxFriends()) {
               FriendResult result = FriendResult.failure(FriendResult.Status.MAX_FRIENDS_REACHED, this.msg("max_friends_reached").replace("{max}", String.valueOf(this.getMaxFriends())), targetDisplay);
               this.complete(callback, result);
            } else if (!this.dataManager.getSettings(senderUuid).isAcceptRequests()) {
               FriendResult result = FriendResult.failure(FriendResult.Status.REQUESTS_DISABLED, this.msg("requests_disabled"), targetDisplay);
               this.complete(callback, result);
            } else if (!this.dataManager.getSettings(targetUuid).isAcceptRequests()) {
               FriendResult result = FriendResult.failure(FriendResult.Status.TARGET_REQUESTS_DISABLED, this.msg("target_requests_disabled").replace("{player}", targetDisplay), targetDisplay);
               this.complete(callback, result);
            } else {
               int maxPending = this.plugin.getConfig().getInt("limits.max_pending_requests", 20);
               if (this.dataManager.getPendingRequests(targetUuid).size() >= maxPending) {
                  FriendResult result = FriendResult.failure(FriendResult.Status.MAX_PENDING_REACHED, this.msg("max_pending_reached").replace("{player}", targetDisplay), targetDisplay);
                  this.complete(callback, result);
               } else {
                  this.dataManager.addRequest(senderUuid, targetUuid);
                  this.cache.addPending(targetUuid, new FriendRequest(senderUuid, targetUuid, System.currentTimeMillis()));
                  String senderName = sender.getName();
                  this.cache.setName(senderUuid, senderName);
                  this.dataManager.cachePlayer(senderUuid, senderName);
                  FriendResult result = FriendResult.success(this.msg("request_sent").replace("{player}", targetDisplay), targetDisplay);
                  this.complete(callback, result);
                  Bukkit.getScheduler().runTask(this.plugin, () -> {
                     NetworkManager net = this.net();
                     if (net != null) {
                        net.publishFriendRequest(senderUuid, targetUuid);
                     }

                     Player target = Bukkit.getPlayer(targetUuid);
                     if (target != null && target.isOnline()) {
                        target.sendMessage(this.msg("request_received").replace("{sender}", sender.getName()));

                        try {
                           String s = this.plugin.getConfig().getString("notify.request_received.sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
                           Sound sound = this.resolveSound(s);
                           if (sound != null) {
                              target.playSound(target.getLocation(), sound, 1.0F, 1.0F);
                           }
                        } catch (Exception var8) {
                        }
                     }

                  });
               }
            }
         }
      });
   }

   public void acceptRequest(Player target, String senderName, Consumer<FriendResult> callback) {
      this.cache.setName(target.getUniqueId(), target.getName());
      UUID targetUuid = target.getUniqueId();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         UUID senderUuid = this.resolveTarget(senderName);
         if (senderUuid == null) {
            FriendResult result = FriendResult.failure(FriendResult.Status.PLAYER_NOT_FOUND, this.msg("player_not_found").replace("{player}", senderName), senderName);
            this.complete(callback, result);
         } else {
            String senderDisplay = this.resolveName(senderUuid, senderName);
            if (!this.dataManager.hasRequest(senderUuid, targetUuid)) {
               FriendResult result = FriendResult.failure(FriendResult.Status.NO_PENDING_REQUEST, this.msg("no_pending_request").replace("{player}", senderDisplay), senderDisplay);
               this.complete(callback, result);
            } else if (this.dataManager.countFriends(targetUuid) >= this.getMaxFriends()) {
               FriendResult result = FriendResult.failure(FriendResult.Status.MAX_FRIENDS_REACHED, this.msg("max_friends_reached").replace("{max}", String.valueOf(this.getMaxFriends())), senderDisplay);
               this.complete(callback, result);
            } else {
               this.dataManager.addFriendPair(senderUuid, targetUuid);
               this.dataManager.removeRequest(senderUuid, targetUuid);
               this.cache.removePending(targetUuid, senderUuid);
               long now = System.currentTimeMillis();
               this.cache.addFriend(targetUuid, new FriendData(targetUuid, senderUuid, now, false));
               this.cache.addFriend(senderUuid, new FriendData(senderUuid, targetUuid, now, false));
               FriendResult result = FriendResult.success(this.msg("friend_added").replace("{player}", senderDisplay), senderDisplay);
               this.complete(callback, result);
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  NetworkManager net = this.net();
                  if (net != null) {
                     net.publishFriendAccepted(targetUuid, senderUuid);
                  }

                  Player senderP = Bukkit.getPlayer(senderUuid);
                  if (senderP != null && senderP.isOnline()) {
                     senderP.sendMessage(this.msg("request_accepted").replace("{friend}", target.getName()));
                  }

               });
            }
         }
      });
   }

   public void denyRequest(Player target, String senderName, Consumer<FriendResult> callback) {
      UUID targetUuid = target.getUniqueId();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         UUID senderUuid = this.resolveTarget(senderName);
         if (senderUuid == null) {
            this.complete(callback, FriendResult.failure(FriendResult.Status.PLAYER_NOT_FOUND, this.msg("player_not_found").replace("{player}", senderName), senderName));
         } else {
            String senderDisplay = this.resolveName(senderUuid, senderName);
            if (!this.dataManager.hasRequest(senderUuid, targetUuid)) {
               this.complete(callback, FriendResult.failure(FriendResult.Status.NO_PENDING_REQUEST, this.msg("no_pending_request").replace("{player}", senderDisplay), senderDisplay));
            } else {
               this.dataManager.removeRequest(senderUuid, targetUuid);
               this.cache.removePending(targetUuid, senderUuid);
               this.complete(callback, FriendResult.success(this.msg("request_denied").replace("{player}", senderDisplay), senderDisplay));
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  Player senderP = Bukkit.getPlayer(senderUuid);
                  if (senderP != null && senderP.isOnline()) {
                     senderP.sendMessage(this.msg("request_rejected").replace("{player}", target.getName()));
                  }

               });
            }
         }
      });
   }

   public void acceptRequestByUuid(Player target, UUID senderUuid, Consumer<FriendResult> callback) {
      if (senderUuid == null) {
         this.complete(callback, FriendResult.failure(FriendResult.Status.PLAYER_NOT_FOUND, this.msg("player_not_found").replace("{player}", "?"), "?"));
      } else {
         this.cache.setName(target.getUniqueId(), target.getName());
         UUID targetUuid = target.getUniqueId();
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            String senderDisplay = this.resolveName(senderUuid, senderUuid.toString());
            if (!this.dataManager.hasRequest(senderUuid, targetUuid)) {
               FriendResult result = FriendResult.failure(FriendResult.Status.NO_PENDING_REQUEST, this.msg("no_pending_request").replace("{player}", senderDisplay), senderDisplay);
               this.complete(callback, result);
            } else if (this.dataManager.countFriends(targetUuid) >= this.getMaxFriends()) {
               FriendResult result = FriendResult.failure(FriendResult.Status.MAX_FRIENDS_REACHED, this.msg("max_friends_reached").replace("{max}", String.valueOf(this.getMaxFriends())), senderDisplay);
               this.complete(callback, result);
            } else {
               this.dataManager.addFriendPair(senderUuid, targetUuid);
               this.dataManager.removeRequest(senderUuid, targetUuid);
               this.cache.removePending(targetUuid, senderUuid);
               long now = System.currentTimeMillis();
               this.cache.addFriend(targetUuid, new FriendData(targetUuid, senderUuid, now, false));
               this.cache.addFriend(senderUuid, new FriendData(senderUuid, targetUuid, now, false));
               FriendResult result = FriendResult.success(this.msg("friend_added").replace("{player}", senderDisplay), senderDisplay);
               this.complete(callback, result);
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  NetworkManager net = this.net();
                  if (net != null) {
                     net.publishFriendAccepted(targetUuid, senderUuid);
                  }

                  Player senderP = Bukkit.getPlayer(senderUuid);
                  if (senderP != null && senderP.isOnline()) {
                     senderP.sendMessage(this.msg("request_accepted").replace("{friend}", target.getName()));
                  }

               });
            }
         });
      }
   }

   public void denyRequestByUuid(Player target, UUID senderUuid, Consumer<FriendResult> callback) {
      if (senderUuid == null) {
         this.complete(callback, FriendResult.failure(FriendResult.Status.PLAYER_NOT_FOUND, this.msg("player_not_found").replace("{player}", "?"), "?"));
      } else {
         UUID targetUuid = target.getUniqueId();
         Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
            String senderDisplay = this.resolveName(senderUuid, senderUuid.toString());
            if (!this.dataManager.hasRequest(senderUuid, targetUuid)) {
               this.complete(callback, FriendResult.failure(FriendResult.Status.NO_PENDING_REQUEST, this.msg("no_pending_request").replace("{player}", senderDisplay), senderDisplay));
            } else {
               this.dataManager.removeRequest(senderUuid, targetUuid);
               this.cache.removePending(targetUuid, senderUuid);
               this.complete(callback, FriendResult.success(this.msg("request_denied").replace("{player}", senderDisplay), senderDisplay));
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  Player senderP = Bukkit.getPlayer(senderUuid);
                  if (senderP != null && senderP.isOnline()) {
                     senderP.sendMessage(this.msg("request_rejected").replace("{player}", target.getName()));
                  }

               });
            }
         });
      }
   }

   public void removeFriend(Player player, String friendName, Consumer<FriendResult> callback) {
      UUID playerUuid = player.getUniqueId();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         UUID friendUuid = this.resolveTarget(friendName);
         if (friendUuid == null) {
            this.complete(callback, FriendResult.failure(FriendResult.Status.PLAYER_NOT_FOUND, this.msg("player_not_found").replace("{player}", friendName), friendName));
         } else {
            String friendDisplay = this.resolveName(friendUuid, friendName);
            if (!this.dataManager.isFriend(playerUuid, friendUuid)) {
               this.complete(callback, FriendResult.failure(FriendResult.Status.NOT_FRIENDS, this.msg("not_friends").replace("{player}", friendDisplay), friendDisplay));
            } else {
               this.dataManager.removeFriendPair(playerUuid, friendUuid);
               this.cache.removeFriend(playerUuid, friendUuid);
               this.cache.removeFriend(friendUuid, playerUuid);
               this.complete(callback, FriendResult.success(this.msg("friend_removed_self").replace("{player}", friendDisplay), friendDisplay));
               Bukkit.getScheduler().runTask(this.plugin, () -> {
                  Player friendP = Bukkit.getPlayer(friendUuid);
                  if (friendP != null && friendP.isOnline()) {
                     friendP.sendMessage(this.msg("friend_removed_other").replace("{player}", player.getName()));
                  }

               });
            }
         }
      });
   }

   public void blockPlayer(Player player, String targetName, Consumer<FriendResult> callback) {
      UUID playerUuid = player.getUniqueId();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         UUID targetUuid = this.resolveTarget(targetName);
         if (targetUuid == null) {
            this.complete(callback, FriendResult.failure(FriendResult.Status.PLAYER_NOT_FOUND, this.msg("player_not_found").replace("{player}", targetName), targetName));
         } else {
            String targetDisplay = this.resolveName(targetUuid, targetName);
            if (playerUuid.equals(targetUuid)) {
               this.complete(callback, FriendResult.failure(FriendResult.Status.CANNOT_ADD_SELF, this.msg("cannot_block_self"), targetDisplay));
            } else if (this.dataManager.isBlocked(playerUuid, targetUuid)) {
               this.complete(callback, FriendResult.failure(FriendResult.Status.ERROR, this.msg("already_blocked").replace("{player}", targetDisplay), targetDisplay));
            } else {
               if (this.dataManager.isFriend(playerUuid, targetUuid)) {
                  this.dataManager.removeFriendPair(playerUuid, targetUuid);
                  this.cache.removeFriend(playerUuid, targetUuid);
                  this.cache.removeFriend(targetUuid, playerUuid);
               }

               this.dataManager.removeRequest(playerUuid, targetUuid);
               this.dataManager.removeRequest(targetUuid, playerUuid);
               this.cache.removePending(playerUuid, targetUuid);
               this.cache.removePending(targetUuid, playerUuid);
               this.dataManager.addBlock(playerUuid, targetUuid);
               this.complete(callback, FriendResult.success(this.msg("blocked").replace("{player}", targetDisplay), targetDisplay));
            }
         }
      });
   }

   public void unblockPlayer(Player player, String targetName, Consumer<FriendResult> callback) {
      UUID playerUuid = player.getUniqueId();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         UUID targetUuid = this.resolveTarget(targetName);
         if (targetUuid == null) {
            this.complete(callback, FriendResult.failure(FriendResult.Status.PLAYER_NOT_FOUND, this.msg("player_not_found").replace("{player}", targetName), targetName));
         } else {
            String targetDisplay = this.resolveName(targetUuid, targetName);
            if (!this.dataManager.isBlocked(playerUuid, targetUuid)) {
               this.complete(callback, FriendResult.failure(FriendResult.Status.ERROR, this.msg("not_blocked").replace("{player}", targetDisplay), targetDisplay));
            } else {
               this.dataManager.removeBlock(playerUuid, targetUuid);
               this.complete(callback, FriendResult.success(this.msg("unblocked").replace("{player}", targetDisplay), targetDisplay));
            }
         }
      });
   }

   public List<BlockData> getBlockedList(UUID uuid) {
      return this.dataManager.getBlocks(uuid);
   }

   public void setNickname(Player player, String targetName, String nickname, Consumer<FriendResult> callback) {
      UUID playerUuid = player.getUniqueId();
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
         UUID targetUuid = this.resolveTarget(targetName);
         if (targetUuid == null) {
            this.complete(callback, FriendResult.failure(FriendResult.Status.PLAYER_NOT_FOUND, this.msg("player_not_found").replace("{player}", targetName), targetName));
         } else {
            String targetDisplay = this.resolveName(targetUuid, targetName);
            if (!this.dataManager.isFriend(playerUuid, targetUuid)) {
               this.complete(callback, FriendResult.failure(FriendResult.Status.NOT_FRIENDS, this.msg("not_friends").replace("{player}", targetDisplay), targetDisplay));
            } else {
               String clean = nickname != null && !nickname.equalsIgnoreCase("reset") && !nickname.isEmpty() ? nickname : null;
               if (clean != null && clean.length() > 32) {
                  clean = clean.substring(0, 32);
               }

               this.dataManager.setNickname(playerUuid, targetUuid, clean);

               for(FriendData fd : this.cache.getFriends(playerUuid)) {
                  if (fd.getFriendUuid().equals(targetUuid)) {
                     fd.setNickname(clean);
                     break;
                  }
               }

               String resultMsg = clean == null ? this.msg("nickname_reset").replace("{player}", targetDisplay) : this.msg("nickname_set").replace("{player}", targetDisplay).replace("{nickname}", clean);
               this.complete(callback, FriendResult.success(resultMsg, targetDisplay));
            }
         }
      });
   }

   private UUID resolveTarget(String input) {
      if (input != null && !input.isEmpty()) {
         try {
            return UUID.fromString(input);
         } catch (IllegalArgumentException var4) {
            Player online = Bukkit.getPlayerExact(input);
            if (online != null) {
               return online.getUniqueId();
            } else {
               UUID cached = this.cache.uuidFromName(input);
               return cached != null ? cached : this.dataManager.findPlayerUuid(input);
            }
         }
      } else {
         return null;
      }
   }

   private String resolveName(UUID uuid, String fallback) {
      if (uuid == null) {
         return fallback != null ? fallback : "Unknown";
      } else {
         String n = this.cache.getName(uuid);
         if (n != null) {
            return n;
         } else {
            n = this.dataManager.findPlayerName(uuid);
            if (n != null) {
               this.cache.setName(uuid, n);
               return n;
            } else {
               return fallback != null && !fallback.matches("[0-9a-fA-F-]{36}") ? fallback : "Unknown";
            }
         }
      }
   }

   public String resolveNamePublic(UUID uuid) {
      if (uuid == null) {
         return "Unknown";
      } else {
         String n = this.cache.getName(uuid);
         if (n != null) {
            return n;
         } else {
            n = this.dataManager.findPlayerName(uuid);
            if (n != null) {
               this.cache.setName(uuid, n);
               return n;
            } else {
               try {
                  Player online = Bukkit.getPlayer(uuid);
                  if (online != null && online.isOnline()) {
                     this.cache.setName(uuid, online.getName());

                     try {
                        this.dataManager.cachePlayer(uuid, online.getName());
                     } catch (Exception var7) {
                     }

                     return online.getName();
                  }
               } catch (Throwable var8) {
               }

               try {
                  OfflinePlayer op = Bukkit.getOfflinePlayer(uuid);
                  if (op.getName() != null) {
                     this.cache.setName(uuid, op.getName());

                     try {
                        this.dataManager.cachePlayer(uuid, op.getName());
                     } catch (Exception var5) {
                     }

                     return op.getName();
                  }
               } catch (Throwable var6) {
               }

               return "Unknown";
            }
         }
      }
   }

   private void complete(Consumer<FriendResult> callback, FriendResult result) {
      if (callback != null) {
         Bukkit.getScheduler().runTask(this.plugin, () -> callback.accept(result));
      }

   }
}
