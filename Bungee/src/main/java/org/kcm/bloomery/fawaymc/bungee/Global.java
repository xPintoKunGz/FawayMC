package org.kcm.bloomery.fawaymc.bungee;

import net.md_5.bungee.api.plugin.Plugin;
import org.kcm.bloomery.fawaymc.bungee.command.LobbyCommand;
import org.kcm.bloomery.fawaymc.bungee.command.SetLobbyCommand;
import org.kcm.bloomery.fawaymc.bungee.listener.JoinListener;
import org.kcm.bloomery.fawaymc.bungee.listener.PingListener;
import org.kcm.bloomery.fawaymc.bungee.manager.LobbyManager;
import org.kcm.bloomery.fawaymc.bungee.manager.MotdManager;

public final class Global extends Plugin {
   private static Global instance;
   private MotdManager motdManager;
   private LobbyManager lobbyManager;

   public void onEnable() {
      instance = this;
      this.motdManager = new MotdManager(this);
      this.lobbyManager = new LobbyManager(this);
      this.getProxy().getPluginManager().registerListener(this, new PingListener(this));
      this.getProxy().getPluginManager().registerListener(this, new JoinListener(this));
      this.getProxy().getPluginManager().registerCommand(this, new SetLobbyCommand(this));
      this.getProxy().getPluginManager().registerCommand(this, new LobbyCommand(this));
      this.getLogger().info("§a[FawayMC-Bungeecord] enabled!");
      this.getLogger().info("§7Lobby server: §e" + this.lobbyManager.getLobbyServerName());
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
}
