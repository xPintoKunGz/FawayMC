package org.kcm.bloomery.fawaymc.advancement.manager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.kcm.bloomery.fawaymc.advancement.Global;
import org.kcm.bloomery.fawaymc.advancement.data.Advancement;

public class RewardDispatcher {
   private final Global plugin;
   private final Set<String> debugLogged = new HashSet();

   public RewardDispatcher(Global plugin) {
      this.plugin = plugin;
   }

   public void dispatch(Player player, Advancement adv) {
      if (player != null && player.isOnline() && adv != null) {
         ConfigurationSection reward = adv.getRewardConfig();
         if (reward != null) {
            long coins = reward.getLong("coins", 0L);
            long exp = reward.getLong("exp", 0L);
            if (coins > 0L) {
               this.giveCoins(player, coins);
            }

            if (exp > 0L) {
               this.giveExp(player, exp);
            }

            String tier = adv.getTier().toLowerCase();
            String boxType = this.resolveVaultBoxForTier(tier);
            if (boxType != null) {
               ConfigurationSection vault = reward.getConfigurationSection("vault");
               int amount = vault != null ? vault.getInt("amount", 1) : 1;
               if (vault != null && vault.getString("box") != null) {
                  boxType = vault.getString("box");
               }

               this.giveVault(player, boxType, amount);
            }

         }
      }
   }

   private String resolveVaultBoxForTier(String tier) {
      switch (tier) {
         case "rare" -> {
            return "common";
         }
         case "epic" -> {
            return "rare";
         }
         default -> {
            return null;
         }
      }
   }

   private void giveCoins(Player player, long amount) {
      try {
         Plugin lb = this.getLuckyblock();
         if (lb == null || !lb.isEnabled()) {
            if (this.debugOnce("coin-plugin-null")) {
               this.plugin.getLogger().warning("[Reward] ❌ Luckyblock plugin not found or disabled!");
            }

            return;
         }

         Object dataMgr = lb.getClass().getMethod("getData").invoke(lb);
         if (dataMgr == null) {
            return;
         }

         Method m = this.findMethod(dataMgr, "addCoins", UUID.class, null);
         if (m == null) {
            m = this.findMethod(dataMgr, "addCoins", Player.class, null);
         }

         if (m == null) {
            if (this.debugOnce("coin-method-not-found")) {
               this.plugin.getLogger().severe("[Reward] ❌ addCoins method NOT FOUND in " + dataMgr.getClass().getName());
               this.logAvailableMethods(dataMgr, "coin");
            }

            return;
         }

         Class<?>[] types = m.getParameterTypes();
         Object arg1 = types[0] == UUID.class ? player.getUniqueId() : player;
         Object arg2 = types[1] != Long.TYPE && types[1] != Long.class ? (int)amount : amount;
         m.invoke(dataMgr, arg1, arg2);
         this.plugin.getLogger().info("[Reward] ✓ Successfully gave " + amount + " coins to " + player.getName());
      } catch (InvocationTargetException e) {
         Throwable cause = e.getCause();
         Logger var10000 = this.plugin.getLogger();
         String var10001 = cause != null ? cause.toString() : e.toString();
         var10000.severe("[Reward] ❌ addCoins threw an error: " + var10001);
         if (cause != null) {
            cause.printStackTrace();
         }
      } catch (Throwable e) {
         this.plugin.getLogger().warning("[Reward] giveCoins failed: " + e.getMessage());
      }

   }

   private void giveExp(Player player, long amount) {
      if (player != null && player.isOnline() && amount > 0L) {
         try {
            Plugin fl = this.getLevelPlugin();
            if (fl == null) {
               if (this.debugOnce("exp-plugin-null")) {
                  this.plugin.getLogger().warning("[Reward] Level plugin not found — exp disabled");
               }

               return;
            }

            Object levelMgr = fl.getClass().getMethod("getLevelManager").invoke(fl);
            if (levelMgr == null) {
               return;
            }

            Method m = this.findMethod(levelMgr, "addExp", Player.class, null);
            if (m == null) {
               if (this.debugOnce("exp-method-not-found")) {
                  this.plugin.getLogger().warning("[Reward] addExp method not found");
               }

               return;
            }

            Class<?>[] types = m.getParameterTypes();
            Object arg2 = types[1] != Long.TYPE && types[1] != Long.class ? (int)amount : amount;
            m.invoke(levelMgr, player, arg2);
            this.plugin.getLogger().info("[Reward] ✓ Successfully gave " + amount + " exp to " + player.getName());
         } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            Logger var10000 = this.plugin.getLogger();
            String var10001 = cause != null ? cause.toString() : e.toString();
            var10000.severe("[Reward] ❌ addExp threw an error: " + var10001);
         } catch (Throwable e) {
            this.plugin.getLogger().warning("[Reward] giveExp failed: " + e.getMessage());
         }

      }
   }

   private void giveVault(Player player, String boxType, int amount) {
      try {
         Plugin lb = this.getLuckyblock();
         if (lb == null || !lb.isEnabled()) {
            return;
         }

         Object dataMgr = lb.getClass().getMethod("getData").invoke(lb);
         if (dataMgr == null) {
            return;
         }

         Method m = this.findMethod(dataMgr, "addBox", UUID.class, String.class, null);
         if (m == null) {
            m = this.findMethod(dataMgr, "addBox", Player.class, String.class, null);
         }

         if (m == null) {
            return;
         }

         Class<?>[] types = m.getParameterTypes();
         Object arg1 = types[0] == UUID.class ? player.getUniqueId() : player;
         Object arg3 = types[2] != Long.TYPE && types[2] != Long.class ? amount : (long)amount;
         m.invoke(dataMgr, arg1, boxType, arg3);
         this.plugin.getLogger().info("[Reward] ✓ Successfully gave " + amount + "x " + boxType + " box to " + player.getName());
      } catch (InvocationTargetException e) {
         Throwable cause = e.getCause();
         Logger var10000 = this.plugin.getLogger();
         String var10001 = cause != null ? cause.toString() : e.toString();
         var10000.severe("[Reward] ❌ addBox threw an error: " + var10001);
      } catch (Throwable e) {
         this.plugin.getLogger().warning("[Reward] giveVault failed: " + e.getMessage());
      }

   }

   private Plugin getLuckyblock() {
      PluginManager pm = Bukkit.getPluginManager();
      String[] names = new String[]{"FawayMC-Luckyblock", "Luckyblock", "LuckyBlock", "luckyblock", "FawayMC_Luckyblock", "FawayMCLuckyblock"};

      for(String name : names) {
         Plugin p = pm.getPlugin(name);
         if (p != null && p.isEnabled()) {
            return p;
         }
      }

      for(Plugin p : pm.getPlugins()) {
         if (p.isEnabled() && p.getClass().getName().startsWith("org.kcm.bloomery.fawaymc.luckyblock.")) {
            return p;
         }
      }

      return null;
   }

   private Plugin getLevelPlugin() {
      PluginManager pm = Bukkit.getPluginManager();
      String[] names = new String[]{"FawayLevel", "FawayMC-Level", "FawayMCLevel", "Level", "FawayMC_Level"};

      for(String n : names) {
         Plugin p = pm.getPlugin(n);
         if (p != null && p.isEnabled()) {
            return p;
         }
      }

      for(Plugin p : pm.getPlugins()) {
         if (p.isEnabled() && p.getClass().getName().startsWith("org.kcm.bloomery.fawaymc.level.")) {
            return p;
         }
      }

      return null;
   }

   private Method findMethod(Object target, String name, Class<?>... types) {
      for(Class<?> clazz = target.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
         for(Method m : clazz.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == types.length) {
               Class<?>[] actual = m.getParameterTypes();
               boolean match = true;

               for(int i = 0; i < types.length; ++i) {
                  if (types[i] == null) {
                     if (!this.isNumeric(actual[i])) {
                        match = false;
                        break;
                     }
                  } else if (!types[i].isAssignableFrom(actual[i]) && actual[i] != types[i]) {
                     match = false;
                     break;
                  }
               }

               if (match) {
                  try {
                     m.setAccessible(true);
                  } catch (Throwable var12) {
                  }

                  return m;
               }
            }
         }
      }

      return null;
   }

   private boolean isNumeric(Class<?> c) {
      return c == Integer.TYPE || c == Long.TYPE || c == Integer.class || c == Long.class || c == Double.TYPE || c == Float.TYPE || c == Number.class;
   }

   private void logAvailableMethods(Object target, String filter) {
      this.plugin.getLogger().warning("[Reward] Available methods in " + target.getClass().getName() + ":");

      for(Method m : target.getClass().getDeclaredMethods()) {
         String n = m.getName().toLowerCase();
         if (n.contains(filter) || n.contains("add")) {
            Logger var10000 = this.plugin.getLogger();
            String var10001 = m.getName();
            var10000.warning("[Reward]   - " + var10001 + Arrays.toString(m.getParameterTypes()));
         }
      }

   }

   private boolean debugOnce(String key) {
      return this.debugLogged.add(key);
   }
}
