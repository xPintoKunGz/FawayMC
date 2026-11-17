package com.antimery.listener;

import com.antimery.AntiMery;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class CombatListener implements Listener {

    private final AntiMery plugin;

    public CombatListener(AntiMery plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();
        Entity target = event.getEntity();

        // 1. AutoClicker check
        if (plugin.getCheckManager().getAutoClickerCheck().check(player)) {
            event.setCancelled(true);
            return;
        }

        // 2. Reach check
        if (plugin.getCheckManager().getReachCheck().check(player, target)) {
            event.setCancelled(true);
            return;
        }

        // 3. KillAura check
        if (plugin.getCheckManager().getKillAuraCheck().check(player, target)) {
            event.setCancelled(true);
            return;
        }

        // 4. Criticals check
        if (plugin.getCheckManager().getCriticalsCheck().check(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            plugin.getCheckManager().getAutoClickerCheck().check(event.getPlayer());
        }
    }
}

