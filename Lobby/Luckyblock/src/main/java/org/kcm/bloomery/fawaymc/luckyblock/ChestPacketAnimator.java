package org.kcm.bloomery.fawaymc.luckyblock;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.PacketType.Play.Server;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChestPacketAnimator implements Listener {

   private final Plugin plugin;
   private final Map<String, AnimationTask> tasks = new HashMap<>();
   private final ProtocolManager protocolManager;
   private final boolean available;
   private final double maxDistanceSq;

   public ChestPacketAnimator(Plugin plugin) {
      this.plugin = plugin;
      this.maxDistanceSq = 4096.0;

      ProtocolManager pm = null;
      boolean av = false;
      try {
         if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
            pm = ProtocolLibrary.getProtocolManager();
            av = true;
         }
      } catch (Throwable ignored) {}

      this.protocolManager = pm;
      this.available = av;

      if (!this.available) {
         plugin.getLogger().warning("[ChestAnim] ProtocolLib not found — chest open animation DISABLED");
      } else {
         plugin.getLogger().info("[ChestAnim] ProtocolLib hooked — chest open animation ENABLED");
      }

      plugin.getServer().getPluginManager().registerEvents(this, plugin);
   }

   public boolean isAvailable() {
      return this.available;
   }

   private String key(Location loc) {
      return loc.getWorld().getName() + ":"
              + loc.getBlockX() + ":"
              + loc.getBlockY() + ":"
              + loc.getBlockZ();
   }

   public void open(Location chestLoc, Material material) {
      if (!this.available || chestLoc == null || material == null) return;
      if (chestLoc.getWorld() == null) return;
      if (!this.isAnimatable(material)) return;

      String k = this.key(chestLoc);
      this.close(chestLoc);

      AnimationTask task = new AnimationTask(chestLoc, material);
      task.param = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
      this.tasks.put(k, task);
      this.sendToNearbyPlayers(task, 1, task.param);

      task.taskId = Bukkit.getScheduler().runTaskTimer(this.plugin, () -> {
         if (this.tasks.containsKey(k)) {
            if (task.location.getWorld() != null) {
               Material current = task.location.getBlock().getType();
               if (!this.isAnimatable(current)) {
                  this.close(task.location);
               } else {
                  this.sendToNearbyPlayers(task, 1, task.param);
               }
            }
         }
      }, 20L, 20L).getTaskId();
   }

   public void close(Location chestLoc) {
      if (!this.available || chestLoc == null) return;

      String k = this.key(chestLoc);
      // ★ FIX: ลบ Cast (AnimationTask) ที่ไม่จำเป็นออก
      AnimationTask task = this.tasks.remove(k);
      if (task != null) {
         if (task.taskId != -1) {
            try {
               Bukkit.getScheduler().cancelTask(task.taskId);
            } catch (Exception ignored) {}
            task.taskId = -1;
         }
         this.sendCloseToNearby(task);
      }
   }

   private void sendCloseToNearby(AnimationTask task) {
      if (task.location.getWorld() == null) return;

      for (Player p : task.location.getWorld().getPlayers()) {
         if (p.getLocation().distanceSquared(task.location) <= this.maxDistanceSq) {
            this.sendPacket(p, task, 1, 0);
            Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
               if (p.isOnline()) {
                  this.sendRealBlock(p, task.location);
               }
            }, 15L);
         }
      }

      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (task.location.getWorld() != null) {
            for (Player p : task.location.getWorld().getPlayers()) {
               if (p.getLocation().distanceSquared(task.location) <= this.maxDistanceSq) {
                  this.sendPacket(p, task, 1, 0);
               }
            }
         }
      }, 5L);
   }

   private void sendRealBlock(Player p, Location loc) {
      try {
         Block block = loc.getBlock();
         BlockData data = block.getBlockData();
         PacketContainer packet = new PacketContainer(Server.BLOCK_CHANGE);
         packet.getBlockPositionModifier().write(0, new BlockPosition(
                 loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));

         try {
            packet.getBlockData().write(0, WrappedBlockData.createData(data));
            this.protocolManager.sendServerPacket(p, packet);
         } catch (Throwable ignored) {
            PacketContainer legacy = new PacketContainer(Server.BLOCK_CHANGE);
            legacy.getBlockPositionModifier().write(0, new BlockPosition(
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
            legacy.getBlocks().write(0, data.getMaterial());
            this.protocolManager.sendServerPacket(p, legacy);
         }
      } catch (Throwable ignored) {}
   }

   public void closeAll() {
      // ★ FIX: ใช้ new ArrayList<>(...) แทน new HashMap(this.tasks) แบบ Raw Type
      for (AnimationTask t : new ArrayList<>(this.tasks.values())) {
         this.close(t.location);
      }
      this.tasks.clear();
   }

   private boolean isAnimatable(Material mat) {
      if (mat == null) return false;
      if (mat == Material.CHEST) return true;
      if (mat == Material.TRAPPED_CHEST) return true;
      if (mat == Material.ENDER_CHEST) return true;
      if (mat == Material.BARREL) return true;
      return mat.name().endsWith("_SHULKER_BOX");
   }

   private void sendToNearbyPlayers(AnimationTask task, int action, int param) {
      if (task.location.getWorld() == null) return;
      for (Player p : task.location.getWorld().getPlayers()) {
         if (p.getLocation().distanceSquared(task.location) <= this.maxDistanceSq) {
            this.sendPacket(p, task, action, param);
         }
      }
   }

   private void sendPacket(Player p, AnimationTask task, int action, int param) {
      try {
         PacketContainer packet = new PacketContainer(Server.BLOCK_ACTION);
         packet.getBlockPositionModifier().write(0, new BlockPosition(
                 task.location.getBlockX(), task.location.getBlockY(), task.location.getBlockZ()));
         packet.getBlocks().write(0, task.material);
         packet.getIntegers().write(0, action);
         packet.getIntegers().write(1, param);
         this.protocolManager.sendServerPacket(p, packet);
      } catch (Exception ignored) {}
   }

   @EventHandler
   public void onJoin(PlayerJoinEvent event) {
      if (!this.available || this.tasks.isEmpty()) return;

      Player p = event.getPlayer();
      Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
         if (p.isOnline()) {
            for (AnimationTask task : this.tasks.values()) {
               if (task.location.getWorld() == p.getWorld()
                       && p.getLocation().distanceSquared(task.location) <= this.maxDistanceSq) {
                  this.sendPacket(p, task, 1, task.param);
               }
            }
         }
      }, 20L);
   }

   @EventHandler
   public void onQuit(PlayerQuitEvent event) {
      // ไม่ต้องทำอะไร
   }

   private static class AnimationTask {
      final Location location;
      final Material material;
      int taskId = -1;
      int param = 1;

      AnimationTask(Location loc, Material mat) {
         this.location = loc.clone();
         this.material = mat;
      }
   }
}