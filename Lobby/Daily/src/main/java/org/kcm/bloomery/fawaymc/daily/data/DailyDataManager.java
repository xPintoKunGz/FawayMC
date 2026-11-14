package org.kcm.bloomery.fawaymc.daily.data;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.bukkit.plugin.Plugin;

public class DailyDataManager {
   private final Plugin plugin;
   private Connection connection;

   public DailyDataManager(Plugin plugin) {
      this.plugin = plugin;
   }

   public boolean connect() {
      try {
         File dbFile = new File(this.plugin.getDataFolder(), "daily.db");
         if (!dbFile.getParentFile().exists()) {
            dbFile.getParentFile().mkdirs();
         }

         this.connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
         this.connection.setAutoCommit(true);
         this.createTables();
         return true;
      } catch (SQLException e) {
         this.plugin.getLogger().severe("[Daily] DB connection failed: " + e.getMessage());
         return false;
      }
   }

   private void createTables() throws SQLException {
      Statement st = this.connection.createStatement();

      try {
         st.executeUpdate("CREATE TABLE IF NOT EXISTS daily_streaks (player_uuid VARCHAR(36) PRIMARY KEY,streak_days INT DEFAULT 0,last_claim_date VARCHAR(10),last_claim_time BIGINT DEFAULT 0,total_claims INT DEFAULT 0)");
         st.executeUpdate("CREATE TABLE IF NOT EXISTS daily_claims_log (id INTEGER PRIMARY KEY AUTOINCREMENT,player_uuid VARCHAR(36) NOT NULL,claim_date VARCHAR(10) NOT NULL,streak_day INT NOT NULL,reward_id VARCHAR(32) NOT NULL,claimed_at BIGINT NOT NULL)");
         st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_daily_log_player ON daily_claims_log(player_uuid)");
      } catch (Throwable var5) {
         if (st != null) {
            try {
               st.close();
            } catch (Throwable var4) {
               var5.addSuppressed(var4);
            }
         }

         throw var5;
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

   public PlayerStreak loadStreak(UUID uuid) {
      PlayerStreak streak = new PlayerStreak(uuid);

      try {
         PreparedStatement ps = this.connection.prepareStatement("SELECT * FROM daily_streaks WHERE player_uuid = ?");

         try {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            try {
               if (rs.next()) {
                  streak.setStreakDays(rs.getInt("streak_days"));
                  streak.setLastClaimDate(rs.getString("last_claim_date"));
                  streak.setLastClaimTime(rs.getLong("last_claim_time"));
                  streak.setTotalClaims(rs.getInt("total_claims"));
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

      return streak;
   }

   public void saveStreak(PlayerStreak streak) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("INSERT OR REPLACE INTO daily_streaks (player_uuid, streak_days, last_claim_date, last_claim_time, total_claims) VALUES (?, ?, ?, ?, ?)");

         try {
            ps.setString(1, streak.getUuid().toString());
            ps.setInt(2, streak.getStreakDays());
            ps.setString(3, streak.getLastClaimDate());
            ps.setLong(4, streak.getLastClaimTime());
            ps.setInt(5, streak.getTotalClaims());
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
      } catch (SQLException e) {
         this.plugin.getLogger().warning("[Daily] saveStreak error: " + e.getMessage());
      }

   }

   public void logClaim(UUID uuid, String date, int day, String rewardId) {
      try {
         PreparedStatement ps = this.connection.prepareStatement("INSERT INTO daily_claims_log (player_uuid, claim_date, streak_day, reward_id, claimed_at) VALUES (?, ?, ?, ?, ?)");

         try {
            ps.setString(1, uuid.toString());
            ps.setString(2, date);
            ps.setInt(3, day);
            ps.setString(4, rewardId);
            ps.setLong(5, System.currentTimeMillis());
            ps.executeUpdate();
         } catch (Throwable var9) {
            if (ps != null) {
               try {
                  ps.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (ps != null) {
            ps.close();
         }
      } catch (SQLException var10) {
      }

   }
}
