package org.kcm.bloomery.fawaymc.bedwars.global;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class JoinEffectManager implements Listener {
   private final JavaPlugin plugin;
   private FileConfiguration joinConfig;
   private File joinFile;
   private final Map<UUID, Boolean> knownPlayers = new HashMap();

   public JoinEffectManager(JavaPlugin plugin) {
      this.plugin = plugin;
      this.createConfig();
      plugin.getServer().getPluginManager().registerEvents(this, plugin);
      Logger var10000 = plugin.getLogger();
      boolean var10001 = this.joinConfig.getBoolean("join_effects.enabled", true);
      var10000.info("[JoinEffect] config loaded | enabled=" + var10001 + " | effects=" + this.countEffects());
   }

   private int countEffects() {
      ConfigurationSection effects = this.joinConfig.getConfigurationSection("join_effects.effects");
      return effects == null ? 0 : effects.getKeys(false).size();
   }

   public void createConfig() {
      if (!this.plugin.getDataFolder().exists()) {
         this.plugin.getDataFolder().mkdirs();
      }

      this.joinFile = new File(this.plugin.getDataFolder(), "join_effects.yml");
      this.plugin.getLogger().info("[JoinEffect] ═══════════════════════════");
      this.plugin.getLogger().info("[JoinEffect] PATH: " + this.joinFile.getAbsolutePath());
      this.plugin.getLogger().info("[JoinEffect] EXISTS: " + this.joinFile.exists());
      if (!this.joinFile.exists() && this.plugin.getResource("join_effects.yml") != null) {
         try {
            this.plugin.saveResource("join_effects.yml", false);
            this.plugin.getLogger().info("[JoinEffect] ✓ copy จาก resources → plugins/Global/");
         } catch (Exception e) {
            this.plugin.getLogger().severe("[JoinEffect] copy ไม่ได้: " + e.getMessage());
         }
      }

      if (!this.joinFile.exists()) {
         this.plugin.getLogger().warning("[JoinEffect] ไม่มีไฟล์ทั้ง resources และ plugins/Global/ → สร้าง template");

         try {
            String template = "join:\n  message: \"&b✦ &a[+] %luckperms_prefix%{player} &eได้เข้าร่วมเซิร์ฟเวอร์!\"\nquit:\n  message: \"&b✦ &c[-] %luckperms_prefix%{player} &cออกจากเซิร์ฟเวอร์แล้ว\"\n\njoin_effects:\n  enabled: true\n  effects:\n    welcome_board:\n      enabled: true\n      trigger: \"JOIN\"\n      delay_ticks: 5\n      permission: \"\"\n      messages:\n        - \"\"\n        - \"&b&l✦ &e&lFAWAYMC NETWORK &b&l✦\"\n        - \"&fWelcome, &a&l{player}&f!\"\n        - \"\"\n      particle:\n        enabled: false\n      sound:\n        enabled: false\n";
            Files.write(this.joinFile.toPath(), template.getBytes(StandardCharsets.UTF_8), new OpenOption[0]);
         } catch (Exception e) {
            this.plugin.getLogger().severe("[JoinEffect] สร้างไม่ได้: " + e.getMessage());
            this.joinConfig = new YamlConfiguration();
            return;
         }
      }

      try {
         InputStreamReader reader = new InputStreamReader(new FileInputStream(this.joinFile), StandardCharsets.UTF_8);

         try {
            this.joinConfig = YamlConfiguration.loadConfiguration(reader);
         } catch (Throwable var7) {
            try {
               reader.close();
            } catch (Throwable var4) {
               var7.addSuppressed(var4);
            }

            throw var7;
         }

         reader.close();
      } catch (Exception e) {
         this.plugin.getLogger().severe("[JoinEffect] อ่านไม่ได้: " + e.getMessage());
         this.joinConfig = new YamlConfiguration();
         return;
      }

      if (!this.joinConfig.contains("join.message")) {
         this.plugin.getLogger().warning("[JoinEffect] ⚠ join.message ไม่มีในไฟล์ → เพิ่ม default");
         this.joinConfig.set("join.message", "&b✦ &a[+] %luckperms_prefix%{player} &eได้เข้าร่วมเซิร์ฟเวอร์!");

         try {
            this.joinConfig.save(this.joinFile);
         } catch (Exception var6) {
         }
      }

      if (!this.joinConfig.contains("quit.message")) {
         this.joinConfig.set("quit.message", "&b✦ &c[-] %luckperms_prefix%{player} &cออกจากเซิร์ฟเวอร์แล้ว");

         try {
            this.joinConfig.save(this.joinFile);
         } catch (Exception var5) {
         }
      }

      ConfigurationSection effects = this.joinConfig.getConfigurationSection("join_effects.effects");
      int count = effects == null ? 0 : effects.getKeys(false).size();
      this.plugin.getLogger().info("[JoinEffect] ✓ effects=" + count);
      this.plugin.getLogger().info("[JoinEffect] ✓ join.message: '" + this.joinConfig.getString("join.message", "") + "'");
      this.plugin.getLogger().info("[JoinEffect] ✓ quit.message: '" + this.joinConfig.getString("quit.message", "") + "'");
      this.plugin.getLogger().info("[JoinEffect] ═══════════════════════════");
   }

   public void reloadConfig() {
      if (this.joinFile == null) {
         this.joinFile = new File(this.plugin.getDataFolder(), "join_effects.yml");
      }

      this.plugin.getLogger().info("[JoinEffect] ═══ RELOAD ═══");
      this.plugin.getLogger().info("[JoinEffect] PATH : " + this.joinFile.getAbsolutePath());
      this.plugin.getLogger().info("[JoinEffect] SIZE : " + this.joinFile.length() + " bytes");
      if (!this.joinFile.exists()) {
         this.plugin.getLogger().warning("[JoinEffect] ❌ ไม่พบไฟล์");
      } else {
         try {
            InputStreamReader reader = new InputStreamReader(new FileInputStream(this.joinFile), StandardCharsets.UTF_8);

            try {
               this.joinConfig = YamlConfiguration.loadConfiguration(reader);
            } catch (Throwable var5) {
               try {
                  reader.close();
               } catch (Throwable var4) {
                  var5.addSuppressed(var4);
               }

               throw var5;
            }

            reader.close();
         } catch (Exception e) {
            this.plugin.getLogger().severe("[JoinEffect] reload ไม่ได้: " + e.getMessage());
            return;
         }

         this.logLoadedContent();
      }
   }

   private void logLoadedContent() {
      ConfigurationSection effects = this.joinConfig.getConfigurationSection("join_effects.effects");
      int count = effects == null ? 0 : effects.getKeys(false).size();
      this.plugin.getLogger().info("[JoinEffect] ✓ effects=" + count);
      if (effects != null) {
         for(String k : effects.getKeys(false)) {
            ConfigurationSection e = effects.getConfigurationSection(k);
            if (e != null) {
               List<String> msgs = e.getStringList("messages");
               String single = e.getString("message", "");
               String firstLine = "-";
               if (!msgs.isEmpty()) {
                  String f = (String)msgs.get(0);
                  firstLine = f != null && !f.isEmpty() ? f : "(empty)";
               } else if (!single.isEmpty()) {
                  firstLine = single;
               }

               this.plugin.getLogger().info("[JoinEffect]   • " + k + " | messages=" + msgs.size() + " | first=" + firstLine + " | trigger=" + e.getString("trigger", "?"));
            }
         }
      }

   }

   public String getJoinMessage() {
      String msg = this.joinConfig.getString("join.message", "");
      this.plugin.getLogger().info("[JoinEffect] getJoinMessage() → '" + msg + "'");
      return msg;
   }

   public String getQuitMessage() {
      String msg = this.joinConfig.getString("quit.message", "");
      this.plugin.getLogger().info("[JoinEffect] getQuitMessage() → '" + msg + "'");
      return msg;
   }

   private Sound resolveSound(String rawName) {
      if (rawName != null && !rawName.isBlank()) {
         String normalized = rawName.trim().toLowerCase();
         Sound s = (Sound)Registry.SOUNDS.get(NamespacedKey.minecraft(normalized));
         if (s != null) {
            return s;
         } else {
            s = (Sound)Registry.SOUNDS.get(NamespacedKey.minecraft(normalized.replace('_', '.')));
            if (s != null) {
               return s;
            } else {
               if (normalized.contains(":")) {
                  NamespacedKey custom = NamespacedKey.fromString(normalized);
                  if (custom != null) {
                     s = (Sound)Registry.SOUNDS.get(custom);
                     if (s != null) {
                        return s;
                     }
                  }
               }

               this.plugin.getLogger().warning("[JoinEffect] หา sound ไม่เจอ: " + rawName);
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private Particle resolveParticle(String rawName) {
      if (rawName != null && !rawName.isBlank()) {
         String normalized = rawName.trim().toLowerCase();
         Particle p = (Particle)Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(normalized));
         if (p != null) {
            return p;
         } else {
            p = (Particle)Registry.PARTICLE_TYPE.get(NamespacedKey.minecraft(normalized.replace('_', '.')));
            if (p != null) {
               return p;
            } else {
               if (normalized.contains(":")) {
                  NamespacedKey custom = NamespacedKey.fromString(normalized);
                  if (custom != null) {
                     p = (Particle)Registry.PARTICLE_TYPE.get(custom);
                     if (p != null) {
                        return p;
                     }
                  }
               }

               this.plugin.getLogger().warning("[JoinEffect] หา particle ไม่เจอ: " + rawName);
               return null;
            }
         }
      } else {
         return null;
      }
   }

   private void playEffects(Player player, String triggerType) {
      try {
         if (!this.joinConfig.getBoolean("join_effects.enabled", true)) {
            this.plugin.getLogger().info("[JoinEffect] disabled in config");
            return;
         }

         ConfigurationSection effects = this.joinConfig.getConfigurationSection("join_effects.effects");
         if (effects == null) {
            this.plugin.getLogger().warning("[JoinEffect] ไม่มี section 'join_effects.effects'");
            return;
         }

         boolean isFirstJoin = !player.hasPlayedBefore();
         this.plugin.getLogger().info("[JoinEffect] ═══ " + player.getName() + " trigger=" + triggerType + " isFirstJoin=" + isFirstJoin + " total_effects=" + effects.getKeys(false).size());

         for(String key : effects.getKeys(false)) {
            ConfigurationSection effect = effects.getConfigurationSection(key);
            if (effect == null) {
               this.plugin.getLogger().warning("[JoinEffect]   ⚠ '" + key + "' ไม่ใช่ section — skip");
            } else {
               String trigger = effect.getString("trigger", "JOIN").toUpperCase();
               boolean willRun = false;
               switch (trigger) {
                  case "JOIN" -> willRun = triggerType.equals("JOIN");
                  case "FIRST_JOIN" -> willRun = isFirstJoin && triggerType.equals("JOIN");
                  case "PERMISSION" -> willRun = triggerType.equals("JOIN");
                  case "RESPAWN" -> willRun = false;
               }

               this.plugin.getLogger().info("[JoinEffect]   → '" + key + "' trigger=" + trigger + " willRun=" + willRun);
               if (willRun) {
                  this.playEffect(player, key, effect);
               }
            }
         }
      } catch (Exception e) {
         this.plugin.getLogger().severe("[JoinEffect] error in playEffects: " + e.getMessage());
         e.printStackTrace();
      }

   }

   private void playEffect(Player player, String effectKey, ConfigurationSection effect) {
      if (!effect.getBoolean("enabled", true)) {
         this.plugin.getLogger().info("[JoinEffect]     skip " + effectKey + " (enabled=false)");
      } else {
         String perm = effect.getString("permission", "");
         if (perm != null && !perm.isEmpty() && !player.hasPermission(perm)) {
            this.plugin.getLogger().info("[JoinEffect]     skip " + effectKey + " (no perm: " + perm + ")");
         } else {
            long delay = effect.getLong("delay_ticks", 0L);
            Runnable action = () -> {
               if (player.isOnline()) {
                  this.plugin.getLogger().info("[JoinEffect]     ✦ EXECUTE " + effectKey);
                  List<String> messages = new ArrayList();
                  List<String> rawList = effect.getStringList("messages");
                  if (!rawList.isEmpty()) {
                     messages.addAll(rawList);
                     this.plugin.getLogger().info("[JoinEffect]       messages (getStringList): " + messages.size());
                  }

                  if (messages.isEmpty()) {
                     List<?> list = effect.getList("messages");
                     if (list != null) {
                        for(Object o : list) {
                           messages.add(o == null ? "" : o.toString());
                        }

                        this.plugin.getLogger().info("[JoinEffect]       messages (getList fallback): " + messages.size());
                     }
                  }

                  if (messages.isEmpty()) {
                     String single = effect.getString("messages", "");
                     if (single != null && !single.isEmpty()) {
                        for(String s : single.split("\n")) {
                           messages.add(s);
                        }

                        this.plugin.getLogger().info("[JoinEffect]       messages (getString fallback): " + messages.size());
                     }
                  }

                  if (!messages.isEmpty()) {
                     int total = messages.size();

                     for(int i = 0; i < total; ++i) {
                        String line = (String)messages.get(i);
                        if (line == null) {
                           line = "";
                        }

                        String processed = PlaceholderManager.setPlaceholders(player, line);
                        processed = processed.replace("{player}", player.getName());

                        try {
                           player.sendMessage(processed);
                           this.plugin.getLogger().info("[JoinEffect]       [" + (i + 1) + "/" + total + "] ✓ " + processed);
                        } catch (Exception e) {
                           this.plugin.getLogger().warning("[JoinEffect]       [" + (i + 1) + "/" + total + "] ✗ " + e.getMessage());
                        }
                     }
                  }

                  String single = effect.getString("message", "");
                  if (single != null && !single.isEmpty()) {
                     String processed = PlaceholderManager.setPlaceholders(player, single);
                     processed = processed.replace("{player}", player.getName());
                     this.plugin.getLogger().info("[JoinEffect]       single: '" + processed + "'");
                     player.sendMessage(processed);
                  }

                  if (effect.getBoolean("particle.enabled", true)) {
                     String pType = effect.getString("particle.type", "FIREWORK");
                     Particle particle = this.resolveParticle(pType);
                     if (particle != null) {
                        int count = effect.getInt("particle.count", 30);
                        player.getWorld().spawnParticle(particle, player.getLocation(), count, effect.getDouble("particle.offset_x", (double)0.5F), effect.getDouble("particle.offset_y", (double)1.0F), effect.getDouble("particle.offset_z", (double)0.5F), effect.getDouble("particle.extra", 0.1));
                     }
                  }

                  if (effect.getBoolean("sound.enabled", true)) {
                     Sound sound = this.resolveSound(effect.getString("sound.type", "ENTITY_PLAYER_LEVELUP"));
                     if (sound != null) {
                        player.playSound(player.getLocation(), sound, (float)effect.getDouble("sound.volume", (double)1.0F), (float)effect.getDouble("sound.pitch", (double)1.0F));
                     }
                  }

               }
            };
            if (delay > 0L) {
               Bukkit.getScheduler().runTaskLater(this.plugin, action, delay);
            } else {
               Bukkit.getScheduler().runTask(this.plugin, action);
            }

         }
      }
   }

   @EventHandler
   public void onPlayerJoin(PlayerJoinEvent event) {
      Player player = event.getPlayer();
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (player.isOnline()) {
            this.playEffects(player, "JOIN");
         }
      }, 1L);
   }

   @EventHandler
   public void onPlayerRespawn(PlayerRespawnEvent event) {
      Player player = event.getPlayer();
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (player.isOnline()) {
            ConfigurationSection effects = this.joinConfig.getConfigurationSection("join_effects.effects");
            if (effects != null) {
               for(String key : effects.getKeys(false)) {
                  ConfigurationSection effect = effects.getConfigurationSection(key);
                  if (effect != null) {
                     String trigger = effect.getString("trigger", "JOIN").toUpperCase();
                     if (trigger.equals("RESPAWN")) {
                        this.playEffect(player, key, effect);
                     }
                  }
               }

            }
         }
      }, 2L);
   }
}
