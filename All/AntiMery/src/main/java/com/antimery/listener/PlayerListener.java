package com.antimery.listener;

import com.antimery.AntiMery;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

public class PlayerListener implements Listener {

    private final AntiMery plugin;

    public PlayerListener(AntiMery plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Ignore small rotation-only movements
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) {
            return;
        }

        Player player = event.getPlayer();

        // Track allowFlight state (e.g. Double Jump plugin granting allowFlight)
        if (player.getAllowFlight()) {
            plugin.getCheckManager().getFlyCheck().recordAllowFlight(player);
            plugin.getCheckManager().getHighJumpCheck().recordAllowFlight(player);
        }

        // Track stepping on jump pads (pressure plates, sponge)
        if (to.getBlock().getType().name().contains("PLATE") || to.clone().subtract(0, 1, 0).getBlock().getType() == org.bukkit.Material.SPONGE) {
            plugin.getCheckManager().getFlyCheck().recordJumpPad(player);
            plugin.getCheckManager().getHighJumpCheck().recordJumpPad(player);
        }

        // 1. Fly check (ไม่ดึงกลับ / No Pullback - ตรวจจับอย่างเดียว พอเยอะแล้วเตะหรือแบน)
        plugin.getCheckManager().getFlyCheck().check(player, from, to);

        // 2. Speed check
        plugin.getCheckManager().getSpeedCheck().check(player, from, to);

        // 3. NoFall check
        plugin.getCheckManager().getNoFallCheck().check(player, from, to);

        // 4. Jesus check
        plugin.getCheckManager().getJesusCheck().check(player, from, to);

        // 5. HighJump check
        plugin.getCheckManager().getHighJumpCheck().check(player, from, to);

        // 6. FastClimb check
        plugin.getCheckManager().getFastClimbCheck().check(player, from, to);

        // 7. Prediction Anti-Cheat check (Vector Simulation & GameMode 0 Fly > 20s)
        plugin.getCheckManager().getPredictionCheck().check(player, from, to);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();

        // If player has allowFlight enabled by a command/plugin, or has permission/OP/Creative/Spectator:
        if (player.getAllowFlight() || player.isOp() || player.hasPermission("antimery.bypass")
                || player.hasPermission("antimery.bypass.fly") || player.hasPermission("essentials.fly")
                || player.hasPermission("cmi.command.fly") || player.getGameMode() == GameMode.CREATIVE
                || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        // Unauthorized fly toggle without allowFlight! Cancel it immediately!
        event.setCancelled(true);
        player.setFlying(false);
        player.setAllowFlight(false);

        plugin.getCheckManager().getFlyCheck().flag(player, "พยายามกดบินโดยไม่มีสิทธิ์หรือไม่มีคำสั่งอนุญาต");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMonitorToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            // A double jump plugin triggered a double jump!
            plugin.getCheckManager().getFlyCheck().recordDoubleJump(player);
            plugin.getCheckManager().getHighJumpCheck().recordDoubleJump(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        plugin.getCheckManager().getFlyCheck().recordTeleport(player);
        plugin.getCheckManager().getSpeedCheck().recordTeleport(player);
        plugin.getCheckManager().getHighJumpCheck().recordTeleport(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        plugin.getCheckManager().getFlyCheck().recordVelocity(player, event.getVelocity());
        plugin.getCheckManager().getSpeedCheck().recordVelocity(player, event.getVelocity());
        plugin.getCheckManager().getHighJumpCheck().recordVelocity(player, event.getVelocity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            // Record damage/knockback to prevent false positive in Fly/Speed/HighJump
            plugin.getCheckManager().getFlyCheck().recordVelocity(player, null);
            plugin.getCheckManager().getSpeedCheck().recordVelocity(player, null);
            plugin.getCheckManager().getHighJumpCheck().recordVelocity(player, null);
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (event.getNewGameMode() == GameMode.SURVIVAL || event.getNewGameMode() == GameMode.ADVENTURE) {
            if (!player.isOp() && !player.hasPermission("antimery.bypass.fly") && !player.getAllowFlight()) {
                player.setFlying(false);
                player.setAllowFlight(false);
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.getCheckManager().handleQuit(event.getPlayer());
        if (plugin.getClientDetector() != null) {
            plugin.getClientDetector().handleQuit(event.getPlayer());
        }
        if (plugin.getAtlasManager() != null) {
            plugin.getAtlasManager().handleQuit(event.getPlayer());
        }
    }
}
