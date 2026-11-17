package org.kcm.bloomery.fawaymc.daily.manager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.kcm.bloomery.fawaymc.daily.data.DailyDataManager;
import org.kcm.bloomery.fawaymc.daily.data.PlayerStreak;

import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class DailyManager {

   public enum ClaimResult {
      SUCCESS,
      ALREADY_CLAIMED,
      NO_REWARD,
      ERROR
   }

   private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;

   private final Plugin plugin;
   private final DailyDataManager dataManager;
   private final DailyCache cache;

   // ✅ ตัวแปรสำหรับป้องกันการแจ้งเตือนซ้ำในรอบการเข้าสู่เซิร์ฟเวอร์เดียว
   private final Set<UUID> remindedThisSession = ConcurrentHashMap.newKeySet();

   public DailyManager(Plugin plugin, DailyDataManager dataManager, DailyCache cache) {
      this.plugin = plugin;
      this.dataManager = dataManager;
      this.cache = cache;
   }

   public DailyCache getCache() { return cache; }
   public DailyDataManager getDataManager() { return dataManager; }

   // ========== TIME (24h rolling cooldown) ==========

   private static final long ONE_DAY_MS = 24L * 60 * 60 * 1000;
   private static final long TWO_DAYS_MS = 48L * 60 * 60 * 1000;

   public long getTimeSinceLastClaim(PlayerStreak streak) {
      if (streak.getLastClaimTime() <= 0) return Long.MAX_VALUE;
      return System.currentTimeMillis() - streak.getLastClaimTime();
   }

   public long getTimeUntilNextClaim(PlayerStreak streak) {
      if (streak.getLastClaimTime() <= 0) return 0;
      long next = streak.getLastClaimTime() + ONE_DAY_MS;
      long remaining = next - System.currentTimeMillis();
      return Math.max(0, remaining);
   }

   public boolean canClaim(PlayerStreak streak) {
      if (streak.getLastClaimTime() <= 0) return true;
      return getTimeSinceLastClaim(streak) >= ONE_DAY_MS;
   }

   public boolean isStreakBroken(PlayerStreak streak) {
      if (streak.getLastClaimTime() <= 0) return false;
      return getTimeSinceLastClaim(streak) >= TWO_DAYS_MS;
   }

   public int getNextDay(PlayerStreak streak) {
      int cycleLength = plugin.getConfig().getInt("settings.cycle_length", 7);

      if (streak.getLastClaimTime() <= 0) return 1;
      if (!canClaim(streak)) return Math.max(1, streak.getStreakDays());

      if (isStreakBroken(streak)) return 1;

      int next = streak.getStreakDays() + 1;
      return next > cycleLength ? 1 : next;
   }

   public String formatCountdown(long millis) {
      if (millis <= 0) return "Ready!";
      long totalSec = millis / 1000;
      long hours = totalSec / 3600;
      long minutes = (totalSec % 3600) / 60;
      long seconds = totalSec % 60;

      if (hours > 0) {
         return String.format("%dh %dm %ds", hours, minutes, seconds);
      } else if (minutes > 0) {
         return String.format("%dm %ds", minutes, seconds);
      }
      return String.format("%ds", seconds);
   }

   // ═══════════════════════════════════════════════════════════
   // ROBUST LUCKYBLOCK FINDER
   // ═══════════════════════════════════════════════════════════
   private Plugin getLuckyblock() {
      org.bukkit.plugin.PluginManager pm = Bukkit.getPluginManager();

      String[] names = {
              "FawayMC-Luckyblock",
              "Luckyblock", "LuckyBlock", "luckyblock",
              "FawayMC_Luckyblock", "FawayMCLuckyblock"
      };
      for (String name : names) {
         Plugin p = pm.getPlugin(name);
         if (p != null && p.isEnabled()) return p;
      }

      for (Plugin p : pm.getPlugins()) {
         if (!p.isEnabled()) continue;
         String cls = p.getClass().getName();
         if (cls.startsWith("org.kcm.bloomery.fawaymc.luckyblock.")) {
            return p;
         }
      }
      return null;
   }

   public long getPlayerCoins(UUID uuid) {
      try {
         Plugin lb = getLuckyblock();
         if (lb == null || !lb.isEnabled()) return 0;

         Object dataMgr = lb.getClass().getMethod("getData").invoke(lb);
         if (dataMgr == null) return 0;

         java.lang.reflect.Method m = findMethod(dataMgr, "getCoins", UUID.class);
         if (m == null) return 0;

         try {
            Object r = m.invoke(dataMgr, uuid);
            if (r instanceof Number) return ((Number) r).longValue();
         } catch (Throwable ignored) {}
      } catch (Throwable ignored) {}
      return 0;
   }

   private boolean hasRewards(String rank) {
      return plugin.getConfig().getConfigurationSection("rewards." + rank) != null;
   }

   // ========== REWARDS ==========

   public ConfigurationSection getRewards(String rank, int day) {
      ConfigurationSection sec = plugin.getConfig()
              .getConfigurationSection("rewards." + rank + ".day_" + day);
      if (sec == null) {
         sec = plugin.getConfig()
                 .getConfigurationSection("rewards.default.day_" + day);
      }
      return sec;
   }

   // ========== RANK ==========

   public String getPlayerRank(Player player) {
      String placeholder = plugin.getConfig().getString("ranks.papi_placeholder",
              "%luckperms_prefix%");
      try {
         if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Class<?> clazz = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Object result = clazz.getMethod("setPlaceholders", Player.class, String.class)
                    .invoke(null, player, placeholder);
            if (result instanceof String) {
               String r = ((String) result).toLowerCase().trim();
               if (!r.isEmpty() && hasRewards(r)) return r;
            }
         }
      } catch (Throwable ignored) {}

      return plugin.getConfig().getString("ranks.default_rank", "default");
   }

   // ═══════════════════════════════════════════════════════════
   // CLAIM
   // ═══════════════════════════════════════════════════════════
   public void claimAsync(Player player, Consumer<ClaimResult> callback) {
      final UUID uuid = player.getUniqueId();

      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
         PlayerStreak cachedStreak = cache.get(uuid);
         if (cachedStreak == null || cachedStreak.getUuid() == null) {
            cachedStreak = dataManager.loadStreak(uuid);
            cache.set(uuid, cachedStreak);
         }

         final PlayerStreak streak = cachedStreak;

         if (!canClaim(streak)) {
            complete(callback, ClaimResult.ALREADY_CLAIMED);
            return;
         }

         final int nextDay = getNextDay(streak);
         final String rank = getPlayerRank(player);
         final ConfigurationSection rewardCfg = getRewards(rank, nextDay);

         if (rewardCfg == null) {
            complete(callback, ClaimResult.NO_REWARD);
            return;
         }

         Bukkit.getScheduler().runTask(plugin, () -> {
            boolean rewardsOk;
            try {
               rewardsOk = giveRewards(player, rewardCfg);
            } catch (Throwable t) {
               plugin.getLogger().severe("[Daily] giveRewards threw: " + t);
               t.printStackTrace();
               rewardsOk = false;
            }

            if (!rewardsOk) {
               player.sendMessage(ChatColor.RED + "✘ Reward failed! Please contact admin.");
               complete(callback, ClaimResult.ERROR);
               return;
            }

            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
               streak.setStreakDays(nextDay);
               streak.setLastClaimTime(System.currentTimeMillis());
               streak.setLastClaimDate(java.time.LocalDate.now().format(DATE_FMT));
               streak.setTotalClaims(streak.getTotalClaims() + 1);
               dataManager.saveStreak(streak);
               dataManager.logClaim(uuid, streak.getLastClaimDate(), nextDay, "day_" + nextDay);

               Bukkit.getScheduler().runTask(plugin, () -> {
                  sendClaimMessage(player, nextDay, rewardCfg);
                  complete(callback, ClaimResult.SUCCESS);
               });
            });
         });
      });
   }

   // ═══════════════════════════════════════════════════════════
   // REWARD GIVING
   // ═══════════════════════════════════════════════════════════

   private boolean giveRewards(Player player, ConfigurationSection cfg) {
      Plugin lb = getLuckyblock();

      if (lb == null || !lb.isEnabled()) {
         plugin.getLogger().warning("[Daily] Luckyblock not available");
         player.sendMessage(ChatColor.RED + "✘ Luckyblock not available. Contact admin.");
         return false;
      }

      int coins = cfg.getInt("coins", 0);
      ConfigurationSection boxes = cfg.getConfigurationSection("boxes");

      if (coins > 0 && !giveCoins(player, coins)) {
         plugin.getLogger().warning("[Daily] giveCoins FAILED for " + player.getName());
         return false;
      }

      if (boxes != null) {
         for (String boxId : boxes.getKeys(false)) {
            int amount = boxes.getInt(boxId, 0);
            if (amount <= 0) continue;

            if (!giveBox(player, boxId, amount)) {
               plugin.getLogger().warning("[Daily] giveBox(" + boxId + ") FAILED for " + player.getName());
               return false;
            }
         }
      }

      return true;
   }

   private boolean giveCoins(Player player, int amount) {
      try {
         Plugin lb = getLuckyblock();
         if (lb == null || !lb.isEnabled()) return false;

         Object dataMgr = lb.getClass().getMethod("getData").invoke(lb);
         if (dataMgr == null) {
            plugin.getLogger().warning("[Daily] getData() null");
            return false;
         }

         java.lang.reflect.Method m = findMethod(dataMgr, "addCoins", UUID.class, null);

         if (m == null) {
            plugin.getLogger().severe("[Daily] ❌ addCoins(UUID, int|long) NOT FOUND in " + dataMgr.getClass().getName());
            logAvailableMethods(dataMgr);
            return false;
         }

         Class<?>[] types = m.getParameterTypes();
         Object arg = (types[1] == long.class) ? (Object) (long) amount : (Object) amount;

         try {
            Object result = m.invoke(dataMgr, player.getUniqueId(), arg);
            plugin.getLogger().info("[Daily] ✓ Coins +" + amount + " → " + player.getName()
                    + " (returned " + (result != null ? result.toString() : "void") + ")");
            return true;
         } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            plugin.getLogger().severe("[Daily] ❌ addCoins threw: " + (cause != null ? cause.toString() : e.toString()));
            if (cause != null) cause.printStackTrace();
            return false;
         }
      } catch (Throwable t) {
         plugin.getLogger().severe("[Daily] giveCoins fatal: " + t);
         t.printStackTrace();
         return false;
      }
   }

   private boolean giveBox(Player player, String boxId, int amount) {
      try {
         Plugin lb = getLuckyblock();
         if (lb == null || !lb.isEnabled()) return false;

         Object dataMgr = lb.getClass().getMethod("getData").invoke(lb);
         if (dataMgr == null) return false;

         java.lang.reflect.Method m = findMethod(dataMgr, "addBox", UUID.class, String.class, null);

         if (m == null) {
            plugin.getLogger().severe("[Daily] ❌ addBox(UUID, String, int|long) NOT FOUND");
            logAvailableMethods(dataMgr);
            return false;
         }

         Class<?>[] types = m.getParameterTypes();
         Object arg3 = (types[2] == long.class) ? (Object) (long) amount : (Object) amount;

         try {
            Object result = m.invoke(dataMgr, player.getUniqueId(), boxId, arg3);
            int added = (result instanceof Number) ? ((Number) result).intValue() : amount;

            plugin.getLogger().info("[Daily] ✓ Box " + boxId + " x" + amount + " → " + player.getName()
                    + " (added " + added + ")");

            if (added < amount) {
               player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                       "&c⚠ Vault full! " + (amount - added) + "x " + boxId + " lost."));
            }

            return true;
         } catch (java.lang.reflect.InvocationTargetException e) {
            Throwable cause = e.getCause();
            plugin.getLogger().severe("[Daily] ❌ addBox(" + boxId + ") threw: " + (cause != null ? cause.toString() : e.toString()));
            if (cause != null) cause.printStackTrace();
            return false;
         }
      } catch (Throwable t) {
         plugin.getLogger().severe("[Daily] giveBox fatal: " + t);
         return false;
      }
   }

   // ═══════════════════════════════════════════════════════════
   // REFLECTION HELPERS
   // ═══════════════════════════════════════════════════════════

   private java.lang.reflect.Method findMethod(Object target, String name, Class<?>... types) {
      Class<?> clazz = target.getClass();
      while (clazz != null) {
         for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() != types.length) continue;

            Class<?>[] actual = m.getParameterTypes();
            boolean match = true;
            for (int i = 0; i < types.length; i++) {
               if (types[i] == null) {
                  if (!isNumeric(actual[i])) { match = false; break; }
               } else if (!types[i].isAssignableFrom(actual[i]) && actual[i] != types[i]) {
                  match = false;
                  break;
               }
            }
            if (!match) continue;

            try { m.setAccessible(true); } catch (Throwable ignored) {}
            return m;
         }
         clazz = clazz.getSuperclass();
      }
      return null;
   }

   private boolean isNumeric(Class<?> c) {
      return c == int.class || c == long.class || c == Integer.class || c == Long.class;
   }

   private void logAvailableMethods(Object target) {
      plugin.getLogger().warning("[Daily] Available methods in " + target.getClass().getName() + ":");
      for (java.lang.reflect.Method m : target.getClass().getDeclaredMethods()) {
         String n = m.getName().toLowerCase();
         if (n.contains("coin") || n.contains("box") || n.contains("add")) {
            plugin.getLogger().warning("[Daily]   - " + m.getName() + java.util.Arrays.toString(m.getParameterTypes()));
         }
      }
   }

   // ========== MESSAGES ==========

   private void sendClaimMessage(Player player, int day, ConfigurationSection cfg) {
      String display = cfg.getString("display_name", "Day " + day);
      String prefix = plugin.getConfig().getString("messages.prefix", "");
      String format = plugin.getConfig().getString("messages.claimed",
              "&a&l✔ &aYou claimed &e{reward} &afor &eDay {day}&a!");
      String msg = format
              .replace("{day}", String.valueOf(day))
              .replace("{reward}", ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', display)));
      player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + msg));

      int cycleLength = plugin.getConfig().getInt("settings.cycle_length", 7);
      if (day >= cycleLength) {
         String milestone = plugin.getConfig().getString("messages.milestone_reached",
                 "&d&l🎉 &fYou completed Day {max}! &7Streak reset for new cycle.");
         player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + milestone.replace("{max}", String.valueOf(cycleLength))));
      }
   }

   private void complete(Consumer<ClaimResult> callback, ClaimResult result) {
      if (callback != null) {
         Bukkit.getScheduler().runTask(plugin, () -> callback.accept(result));
      }
   }

   // ========== LOADER & REMINDER ==========

   public void loadPlayerAsync(UUID uuid, String name) {
      cache.setName(uuid, name);
      Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
         PlayerStreak streak = dataManager.loadStreak(uuid);
         cache.set(uuid, streak);
      });
   }

   /**
    * ✅ เมธอดใหม่สำหรับแจ้งเตือนด้วย Clickable Text (สไตล์ Hypixel)
    */
   public void sendReminder(Player player) {
      UUID uuid = player.getUniqueId();

      // 1. ถ้าแจ้งเตือนไปแล้วในเซสชันนี้ ให้ข้ามไปเลย
      if (remindedThisSession.contains(uuid)) return;

      // 2. ดึงข้อมูลว่ามีรางวัลให้กดรับหรือไม่
      PlayerStreak streak = cache.get(uuid);
      if (streak == null || !canClaim(streak)) return;

      // 3. ดึงข้อความจาก config
      String titleRaw = plugin.getConfig().getString("messages.reminder.title", "&e&l★ &f&lDAILY REWARDS &e&l★");
      String descRaw = plugin.getConfig().getString("messages.reminder.desc", "&fYour daily reward is ready to be claimed!");
      String btnText = plugin.getConfig().getString("messages.reminder.button", "&a&l[ CLICK HERE TO CLAIM ]");
      String hoverText = plugin.getConfig().getString("messages.reminder.hover", "&7Click to open the Daily Rewards GUI");

      // 4. แปลงเป็น Component
      Component titleComp = LegacyComponentSerializer.legacyAmpersand().deserialize(titleRaw);
      Component descComp = LegacyComponentSerializer.legacyAmpersand().deserialize(descRaw);

      // 5. สร้างปุ่มกด (คลิกแล้วรันคำสั่ง /daily เพื่อเปิด GUI เท่านั้น)
      Component buttonComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(btnText)
              .clickEvent(ClickEvent.runCommand("/daily"))
              .hoverEvent(HoverEvent.showText(LegacyComponentSerializer.legacyAmpersand().deserialize(hoverText)));
      player.sendMessage(Component.empty());
      player.sendMessage(descComp);
      player.sendMessage(buttonComponent);
      player.sendMessage(Component.empty());

      // 7. เล่นเสียงแจ้งเตือน
      try {
         player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
      } catch (Throwable ignored) {}

      // 8. บันทึกว่าแจ้งเตือนแล้ว
      remindedThisSession.add(uuid);
   }

   /**
    * ✅ ล้างประวัติการแจ้งเตือนเมื่อผู้เล่นออกจากเซิร์ฟเวอร์
    */
   public void clearSession(UUID uuid) {
      remindedThisSession.remove(uuid);
   }
}