package org.kcm.bloomery.fawaymc.friend.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.plugin.Plugin;

public class FriendDataManager {
   private final Plugin plugin;
   private Connection connection;

   public FriendDataManager(Plugin plugin) {
      this.plugin = plugin;
   }

   public boolean connect() {
      try {
         File dbFile = new File(this.plugin.getDataFolder(), "friends.db");
         if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
         }

         this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
         this.connection.setAutoCommit(true);
         this.createTables();
         return true;
      } catch (SQLException e) {
         this.plugin.getLogger().severe("[Friends] DB connection failed: " + e.getMessage());
         return false;
      }
   }

   private void createTables() throws SQLException {
      Statement st = this.connection.createStatement();

      try {
         st.executeUpdate("CREATE TABLE IF NOT EXISTS friends (id INTEGER PRIMARY KEY AUTOINCREMENT,player_uuid VARCHAR(36) NOT NULL,friend_uuid VARCHAR(36) NOT NULL,created_at BIGINT NOT NULL,is_favorite BOOLEAN DEFAULT 0,UNIQUE(player_uuid, friend_uuid))");
         st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_player ON friends(player_uuid)");
         st.executeUpdate("CREATE TABLE IF NOT EXISTS friend_requests (id INTEGER PRIMARY KEY AUTOINCREMENT,sender_uuid VARCHAR(36) NOT NULL,target_uuid VARCHAR(36) NOT NULL,created_at BIGINT NOT NULL,UNIQUE(sender_uuid, target_uuid))");
         st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_target ON friend_requests(target_uuid)");
         st.executeUpdate("CREATE TABLE IF NOT EXISTS player_settings (player_uuid VARCHAR(36) PRIMARY KEY,accept_requests BOOLEAN DEFAULT 1,notify_online BOOLEAN DEFAULT 1)");
         st.executeUpdate("CREATE TABLE IF NOT EXISTS player_cache (player_uuid VARCHAR(36) PRIMARY KEY,player_name VARCHAR(16) NOT NULL,last_seen BIGINT NOT NULL)");
         st.executeUpdate("CREATE TABLE IF NOT EXISTS friend_blocks (id INTEGER PRIMARY KEY AUTOINCREMENT,player_uuid VARCHAR(36) NOT NULL,blocked_uuid VARCHAR(36) NOT NULL,created_at BIGINT NOT NULL,UNIQUE(player_uuid, blocked_uuid))");
         st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_blocker ON friend_blocks(player_uuid)");

         try {
            st.executeUpdate("ALTER TABLE friends ADD COLUMN nickname VARCHAR(32) DEFAULT NULL");
         } catch (SQLException var5) {
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_name ON player_cache(player_name)");
         }
      } catch (Throwable var6) {
         if (st != null) {
            try {
               st.close();
            } catch (Throwable var4) {
               var6.addSuppressed(var4);
            }
         }

         throw var6;
      }

      if (st != null) {
         st.close();
      }

   }

   public void disconnect() {
      if (this.connection != null) {
         try {
            this.connection.close();
         } catch (SQLException var2) {
         }

         this.connection = null;
      }

   }

   public void cachePlayer(UUID uuid, String name) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("INSERT OR REPLACE INTO player_cache(player_uuid, player_name, last_seen) VALUES (?, ?, ?)");

         try {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
         } catch (Throwable var7) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException e) {
         this.plugin.getLogger().warning("[Friends] cachePlayer: " + e.getMessage());
      }

   }

   public UUID findPlayerUuid(String name) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT player_uuid FROM player_cache WHERE LOWER(player_name) = LOWER(?) LIMIT 1");

         UUID var4;
         label80: {
            try {
               ps.setString(1, name);
               ResultSet rs = ps.executeQuery();

               label74: {
                  try {
                     if (!rs.next()) {
                        break label74;
                     }

                     var4 = UUID.fromString(rs.getString("player_uuid"));
                  } catch (Throwable var8) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label80;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var9) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var6) {
                     var9.addSuppressed(var6);
                  }
               }

               throw var9;
            }

            if (ps != null) {
               ps.close();
            }

            return null;
         }

         if (ps != null) {
            ps.close();
         }

         return var4;
      } catch (SQLException var10) {
         return null;
      }
   }

   public String findPlayerName(UUID uuid) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT player_name FROM player_cache WHERE player_uuid = ? LIMIT 1");

         String var4;
         label80: {
            try {
               ps.setString(1, uuid.toString());
               ResultSet rs = ps.executeQuery();

               label74: {
                  try {
                     if (!rs.next()) {
                        break label74;
                     }

                     var4 = rs.getString("player_name");
                  } catch (Throwable var8) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label80;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var9) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var6) {
                     var9.addSuppressed(var6);
                  }
               }

               throw var9;
            }

            if (ps != null) {
               ps.close();
            }

            return null;
         }

         if (ps != null) {
            ps.close();
         }

         return var4;
      } catch (SQLException var10) {
         return null;
      }
   }

   public List<FriendData> getFriends(UUID uuid) {
      List<FriendData> out = new ArrayList();

      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM friends WHERE player_uuid = ?");

         try {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            try {
               while(rs.next()) {
                  out.add(new FriendData(UUID.fromString(rs.getString("player_uuid")), UUID.fromString(rs.getString("friend_uuid")), rs.getLong("created_at"), rs.getBoolean("is_favorite"), rs.getString("nickname")));
               }
            } catch (Throwable var9) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var10) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException var11) {
      }

      return out;
   }

   public boolean isFriend(UUID a, UUID b) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT 1 FROM friends WHERE player_uuid = ? AND friend_uuid = ? LIMIT 1");

         boolean var5;
         try {
            ps.setString(1, a.toString());
            ps.setString(2, b.toString());
            ResultSet rs = ps.executeQuery();

            try {
               var5 = rs.next();
            } catch (Throwable var9) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var10) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (ps != null) {
            ps.close();
         }

         return var5;
      } catch (SQLException var11) {
         return false;
      }
   }

   public void addFriendPair(UUID a, UUID b) {
      long now = System.currentTimeMillis();

      try {
         this.connection.setAutoCommit(false);
         PreparedStatement ps = this.connection.prepareStatement("INSERT OR IGNORE INTO friends(player_uuid, friend_uuid, created_at) VALUES (?, ?, ?)");

         try {
            ps.setString(1, a.toString());
            ps.setString(2, b.toString());
            ps.setLong(3, now);
            ps.executeUpdate();
            ps.setString(1, b.toString());
            ps.setString(2, a.toString());
            ps.setLong(3, now);
            ps.executeUpdate();
         } catch (Throwable var21) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var20) {
                  var21.addSuppressed(var20);
               }
            }

            throw var21;
         }

         if (ps != null) {
            ps.close();
         }

         this.connection.commit();
      } catch (SQLException var22) {
         try {
            this.connection.rollback();
         } catch (SQLException var19) {
         }
      } finally {
         try {
            this.connection.setAutoCommit(true);
         } catch (SQLException var18) {
         }

      }

   }

   public void removeFriendPair(UUID a, UUID b) {
      try {
         this.connection.setAutoCommit(false);
         PreparedStatement ps = this.connection.prepareStatement("DELETE FROM friends WHERE (player_uuid = ? AND friend_uuid = ?) OR (player_uuid = ? AND friend_uuid = ?)");

         try {
            ps.setString(1, a.toString());
            ps.setString(2, b.toString());
            ps.setString(3, b.toString());
            ps.setString(4, a.toString());
            ps.executeUpdate();
         } catch (Throwable var19) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var18) {
                  var19.addSuppressed(var18);
               }
            }

            throw var19;
         }

         if (ps != null) {
            ps.close();
         }

         this.connection.commit();
      } catch (SQLException var20) {
         try {
            this.connection.rollback();
         } catch (SQLException var17) {
         }
      } finally {
         try {
            this.connection.setAutoCommit(true);
         } catch (SQLException var16) {
         }

      }

   }

   public int countFriends(UUID uuid) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT COUNT(*) FROM friends WHERE player_uuid = ?");

         int var4;
         label80: {
            try {
               ps.setString(1, uuid.toString());
               ResultSet rs = ps.executeQuery();

               label74: {
                  try {
                     if (!rs.next()) {
                        break label74;
                     }

                     var4 = rs.getInt(1);
                  } catch (Throwable var8) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label80;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var9) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var6) {
                     var9.addSuppressed(var6);
                  }
               }

               throw var9;
            }

            if (ps != null) {
               ps.close();
            }

            return 0;
         }

         if (ps != null) {
            ps.close();
         }

         return var4;
      } catch (SQLException var10) {
         return 0;
      }
   }

   public List<FriendRequest> getPendingRequests(UUID target) {
      List<FriendRequest> out = new ArrayList();

      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM friend_requests WHERE target_uuid = ?");

         try {
            ps.setString(1, target.toString());
            ResultSet rs = ps.executeQuery();

            try {
               while(rs.next()) {
                  out.add(new FriendRequest(UUID.fromString(rs.getString("sender_uuid")), UUID.fromString(rs.getString("target_uuid")), rs.getLong("created_at")));
               }
            } catch (Throwable var9) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var10) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException var11) {
      }

      return out;
   }

   public boolean hasRequest(UUID sender, UUID target) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT 1 FROM friend_requests WHERE sender_uuid = ? AND target_uuid = ? LIMIT 1");

         boolean var5;
         try {
            ps.setString(1, sender.toString());
            ps.setString(2, target.toString());
            ResultSet rs = ps.executeQuery();

            try {
               var5 = rs.next();
            } catch (Throwable var9) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var10) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (ps != null) {
            ps.close();
         }

         return var5;
      } catch (SQLException var11) {
         return false;
      }
   }

   public void addRequest(UUID sender, UUID target) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("INSERT OR REPLACE INTO friend_requests(sender_uuid, target_uuid, created_at) VALUES (?, ?, ?)");

         try {
            ps.setString(1, sender.toString());
            ps.setString(2, target.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
         } catch (Throwable var7) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException var8) {
      }

   }

   public void removeRequest(UUID sender, UUID target) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("DELETE FROM friend_requests WHERE sender_uuid = ? AND target_uuid = ?");

         try {
            ps.setString(1, sender.toString());
            ps.setString(2, target.toString());
            ps.executeUpdate();
         } catch (Throwable var7) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException var8) {
      }

   }

   public PlayerSettings getSettings(UUID uuid) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM player_settings WHERE player_uuid = ?");

         PlayerSettings var4;
         label80: {
            try {
               ps.setString(1, uuid.toString());
               ResultSet rs = ps.executeQuery();

               label74: {
                  try {
                     if (!rs.next()) {
                        break label74;
                     }

                     var4 = new PlayerSettings(uuid, rs.getBoolean("accept_requests"), rs.getBoolean("notify_online"));
                  } catch (Throwable var8) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label80;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var9) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var6) {
                     var9.addSuppressed(var6);
                  }
               }

               throw var9;
            }

            if (ps != null) {
               ps.close();
            }

            return new PlayerSettings(uuid, true, true);
         }

         if (ps != null) {
            ps.close();
         }

         return var4;
      } catch (SQLException var10) {
         return new PlayerSettings(uuid, true, true);
      }
   }

   public void saveSettings(PlayerSettings s) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("INSERT OR REPLACE INTO player_settings(player_uuid, accept_requests, notify_online) VALUES (?, ?, ?)");

         try {
            ps.setString(1, s.getPlayerUuid().toString());
            ps.setBoolean(2, s.isAcceptRequests());
            ps.setBoolean(3, s.isNotifyOnline());
            ps.executeUpdate();
         } catch (Throwable var6) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var5) {
                  var6.addSuppressed(var5);
               }
            }

            throw var6;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException var7) {
      }

   }

   public boolean isBlocked(UUID player, UUID target) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT 1 FROM friend_blocks WHERE player_uuid = ? AND blocked_uuid = ? LIMIT 1");

         boolean var5;
         try {
            ps.setString(1, player.toString());
            ps.setString(2, target.toString());
            ResultSet rs = ps.executeQuery();

            try {
               var5 = rs.next();
            } catch (Throwable var9) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var10) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (ps != null) {
            ps.close();
         }

         return var5;
      } catch (SQLException var11) {
         return false;
      }
   }

   public void addBlock(UUID player, UUID target) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("INSERT OR IGNORE INTO friend_blocks(player_uuid, blocked_uuid, created_at) VALUES (?, ?, ?)");

         try {
            ps.setString(1, player.toString());
            ps.setString(2, target.toString());
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
         } catch (Throwable var7) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException var8) {
      }

   }

   public void removeBlock(UUID player, UUID target) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("DELETE FROM friend_blocks WHERE player_uuid = ? AND blocked_uuid = ?");

         try {
            ps.setString(1, player.toString());
            ps.setString(2, target.toString());
            ps.executeUpdate();
         } catch (Throwable var7) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var6) {
                  var7.addSuppressed(var6);
               }
            }

            throw var7;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException var8) {
      }

   }

   public List<BlockData> getBlocks(UUID uuid) {
      List<BlockData> out = new ArrayList();

      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM friend_blocks WHERE player_uuid = ?");

         try {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            try {
               while(rs.next()) {
                  out.add(new BlockData(UUID.fromString(rs.getString("player_uuid")), UUID.fromString(rs.getString("blocked_uuid")), rs.getLong("created_at")));
               }
            } catch (Throwable var9) {
               if (rs != null) {
                  try {
                     rs.close();
                  } catch (Throwable var8) {
                     var9.addSuppressed(var8);
                  }
               }

               throw var9;
            }

            if (rs != null) {
               rs.close();
            }
         } catch (Throwable var10) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var7) {
                  var10.addSuppressed(var7);
               }
            }

            throw var10;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException var11) {
      }

      return out;
   }

   public void setNickname(UUID player, UUID friend, String nickname) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("UPDATE friends SET nickname = ? WHERE player_uuid = ? AND friend_uuid = ?");

         try {
            if (nickname != null && !nickname.isEmpty()) {
               ps.setString(1, nickname);
            } else {
               ps.setNull(1, 12);
            }

            ps.setString(2, player.toString());
            ps.setString(3, friend.toString());
            ps.executeUpdate();
         } catch (Throwable var8) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var7) {
                  var8.addSuppressed(var7);
               }
            }

            throw var8;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException var9) {
      }

   }

   public long getLastSeen(UUID uuid) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT last_seen FROM player_cache WHERE player_uuid = ? LIMIT 1");

         long var4;
         label80: {
            try {
               ps.setString(1, uuid.toString());
               ResultSet rs = ps.executeQuery();

               label74: {
                  try {
                     if (!rs.next()) {
                        break label74;
                     }

                     var4 = rs.getLong("last_seen");
                  } catch (Throwable var8) {
                     if (rs != null) {
                        try {
                           rs.close();
                        } catch (Throwable var7) {
                           var8.addSuppressed(var7);
                        }
                     }

                     throw var8;
                  }

                  if (rs != null) {
                     rs.close();
                  }
                  break label80;
               }

               if (rs != null) {
                  rs.close();
               }
            } catch (Throwable var9) {
               if (ps != null) {
                  try {
                     ps.close();
                  } catch (Throwable var6) {
                     var9.addSuppressed(var6);
                  }
               }

               throw var9;
            }

            if (ps != null) {
               ps.close();
            }

            return 0L;
         }

         if (ps != null) {
            ps.close();
         }

         return var4;
      } catch (SQLException var10) {
         return 0L;
      }
   }
}
