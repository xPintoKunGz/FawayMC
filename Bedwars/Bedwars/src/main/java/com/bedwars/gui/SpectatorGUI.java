package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.Team;
import com.bedwars.util.ItemBuilder;
import com.bedwars.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class SpectatorGUI {

    public static final String TITLE = "§8✦ §b§lSpectator Teleporter §8✦";

    public static void open(Player spectator, Arena arena) {
        if (arena == null) return;

        Inventory inv = Bukkit.createInventory(null, 36, TITLE);

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 27; i < 36; i++) inv.setItem(i, border);

        int slot = 9;
        for (UUID uuid : arena.getPlayers()) {
            Player target = Bukkit.getPlayer(uuid);
            if (target != null && target.isOnline() && arena.getPlayers().contains(uuid)) {
                Team team = arena.getPlayerTeam(uuid);
                String teamName = (team != null) ? team.getColor().getChatColor() + team.getColor().getDisplayName() : "§fNo Team";

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    meta.setOwningPlayer(target);
                    meta.setDisplayName(teamName + " §8» §f" + target.getName());
                    meta.setLore(java.util.List.of(
                            "§7Health: §c" + (int) target.getHealth() + "§7/§c20 ❤",
                            "§7Food: §e" + target.getFoodLevel() + "§7/§e20 🍗",
                            "",
                            "§e▶ Click to spectate this player"
                    ));
                    head.setItemMeta(meta);
                }

                inv.setItem(slot++, head);
                if (slot >= 27) break;
            }
        }

        spectator.openInventory(inv);
        MessageUtil.playSound(spectator, Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }
}
