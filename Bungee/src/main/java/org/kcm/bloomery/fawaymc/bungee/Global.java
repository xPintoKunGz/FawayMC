package org.kcm.bloomery.fawaymc.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.kcm.bloomery.fawaymc.bungee.command.FindCommand;
import org.kcm.bloomery.fawaymc.bungee.command.LobbyCommand;
import org.kcm.bloomery.fawaymc.bungee.command.SendCommand;
import org.kcm.bloomery.fawaymc.bungee.command.SetLobbyCommand;
import org.kcm.bloomery.fawaymc.bungee.listener.JoinListener;
import org.kcm.bloomery.fawaymc.bungee.listener.PingListener;
import org.kcm.bloomery.fawaymc.bungee.listener.SessionListener;
import org.kcm.bloomery.fawaymc.bungee.manager.LobbyManager;
import org.kcm.bloomery.fawaymc.bungee.manager.MotdManager;
import org.kcm.bloomery.fawaymc.bungee.manager.PlayerSessionManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public final class Global extends Plugin {
   private static Global instance;
   private MotdManager motdManager;
   private LobbyManager lobbyManager;
   private PlayerSessionManager sessionManager;
   private Configuration config;

   public void onEnable() {
      instance = this;

      loadConfig();

      this.motdManager = new MotdManager(this);
      this.lobbyManager = new LobbyManager(this);
      this.sessionManager = new PlayerSessionManager(this);

      // Listeners
      this.getProxy().getPluginManager().registerListener(this, new PingListener(this));
      this.getProxy().getPluginManager().registerListener(this, new JoinListener(this));
      this.getProxy().getPluginManager().registerListener(this, new SessionListener(this, this.sessionManager));

      // Commands
      this.getProxy().getPluginManager().registerCommand(this, new SetLobbyCommand(this));
      this.getProxy().getPluginManager().registerCommand(this, new LobbyCommand(this));
      this.getProxy().getPluginManager().registerCommand(this, new SendCommand(this));
      this.getProxy().getPluginManager().registerCommand(this, new FindCommand(this));

      // Auto-save sessions.yml ทุก 60 วินาที
      this.sessionManager.startAutoSave();

      this.getLogger().info("§a[FawayMC-Bungeecord] enabled!");
      this.getLogger().info("§7Lobby server: §e" + this.lobbyManager.getLobbyServerName());
   }

   public void onDisable() {
      if (this.sessionManager != null) {
         this.sessionManager.stop();
         this.sessionManager = null;
      }
      this.getLogger().info("§c[FawayMC-Bungeecord] disabled!");
   }

   private void loadConfig() {
      try {
         if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
         }
         File file = new File(getDataFolder(), "config.yml");
         if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
               Files.copy(in, file.toPath());
            }
         }
         this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   public Configuration getConfig() {
      return config;
   }

   public static Global getInstance() {
      return instance;
   }

   public MotdManager getMotdManager() {
      return this.motdManager;
   }

   public LobbyManager getLobbyManager() {
      return this.lobbyManager;
   }

   public PlayerSessionManager getSessionManager() {
      return this.sessionManager;
   }
}