package com.bedwars.gui;

import com.bedwars.arena.Arena;
import com.bedwars.arena.Team;
import com.bedwars.arena.TeamColor;
import com.bedwars.util.ItemBuilder;
import com.bedwars.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamSelectorGUI {

    public static final String TITLE = "§8✦ §a§lSelect Team §8✦";

    public static void open(Player player, Arena arena) {
        if (arena == null) return;

        Inventory inv = Bukkit.createInventory(null, 45, TITLE);

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 36; i < 45; i++) inv.setItem(i, border);

        inv.setItem(4, new ItemBuilder(Material.COMPASS)
                .name("§a§l✦ Choose Your Team ✦")
                .lore("§7Click on a team wool below to join!")
                .build());

        int slot = 10;
        Team currentTeam = arena.getPlayerTeam(player.getUniqueId());

        for (Team team : arena.getTeams().values()) {
            if (slot >= 35) break;
            if (slot == 17 || slot == 18 || slot == 26 || slot == 27) slot += 2;

            TeamColor tc = team.getColor();
            int currentCount = team.getAlivePlayerCount();
            int maxPerTeam = arena.getPlayersPerTeam();
            boolean isMyTeam = (currentTeam == team);
            boolean isFull = (currentCount >= maxPerTeam);

            List<String> lore = new ArrayList<>();
            lore.add("§8────────────────────");
            lore.add("§7Players: §e" + currentCount + "§7/§b" + maxPerTeam);
            lore.add("§7Team Members:");
            if (team.getPlayers().isEmpty()) {
                lore.add(" §8- (Empty)");
            } else {
                for (UUID u : team.getPlayers()) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null) {
                        lore.add(" §f● " + p.getName() + (p.getUniqueId().equals(player.getUniqueId()) ? " §b(You)" : ""));
                    }
                }
            }
            lore.add("§8────────────────────");
            if (isMyTeam) {
                lore.add("§a✔ You are on this team");
            } else if (isFull) {
                lore.add("§c✖ Team is full");
            } else {
                lore.add("§e▶ Click to join this team!");
            }

            ItemStack woolItem = new ItemBuilder(tc.getWoolMaterial())
                    .name(tc.getChatColor() + "§l● Team " + tc.getDisplayName())
                    .lore(lore)
                    .build();

            inv.setItem(slot++, woolItem);
        }

        player.openInventory(inv);
        MessageUtil.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f);
    }
}
