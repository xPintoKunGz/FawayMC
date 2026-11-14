package org.kcm.bloomery.fawaymc.leaderboard.hologram;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.kcm.bloomery.fawaymc.leaderboard.Global;

public class ChatInputManager implements Listener {
   private final Global plugin;
   private final Map<UUID, Consumer<String>> pending = new HashMap();

   public ChatInputManager(Global plugin) {
      this.plugin = plugin;
   }

   public void request(Player p, String prompt, Consumer<String> callback) {
      this.pending.put(p.getUniqueId(), callback);
      p.closeInventory();
      p.sendMessage("§8§m─────────────────");
      p.sendMessage("§e§l> §f" + prompt);
      p.sendMessage("§7Type §ccancel §7to cancel.");
      p.sendMessage("§8§m─────────────────");
   }

   public void cancel(Player p) {
      this.pending.remove(p.getUniqueId());
   }

   @EventHandler(
      priority = EventPriority.LOWEST
   )
   public void onChat(AsyncPlayerChatEvent e) {
      UUID uuid = e.getPlayer().getUniqueId();
      Consumer<String> cb = (Consumer)this.pending.remove(uuid);
      if (cb != null) {
         e.setCancelled(true);
         String msg = e.getMessage();
         if (msg.equalsIgnoreCase("cancel")) {
            e.getPlayer().sendMessage("§7Cancelled.");
         } else {
            Bukkit.getScheduler().runTask(this.plugin, () -> {
               try {
                  cb.accept(msg);
               } catch (Throwable t) {
                  this.plugin.getLogger().warning("[Hologram] chat input callback failed: " + t.getMessage());
               }

            });
         }
      }
   }
}
