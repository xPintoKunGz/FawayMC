package org.kcm.bloomery.fawaymc.global;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PlayerItemsManager {

    private final Global plugin;
    private final ItemsConfigManager itemsConfig;
    private final Set<UUID> hiddenPlayers = new HashSet<>();

    public PlayerItemsManager(Global plugin, ItemsConfigManager itemsConfig) {
        this.plugin = plugin;
        this.itemsConfig = itemsConfig;
    }

    private FileConfiguration cfg() {
        return itemsConfig.getConfig();
    }

    // ===== Item checks =====

    public boolean isSelectorItem(ItemStack item) {
        if (!cfg().getBoolean("gui.open_item.enabled", true)) return false;
        if (item == null || !item.hasItemMeta()) return false;
        String expectedName = ChatColor.translateAlternateColorCodes('&', cfg().getString("gui.open_item.name", ""));
        return item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equals(expectedName);
    }

    public boolean isProfileItem(ItemStack item) {
        if (!cfg().getBoolean("profile.item.enabled", true)) return false;
        if (item == null || !item.hasItemMeta()) return false;
        String expectedName = ChatColor.translateAlternateColorCodes('&', cfg().getString("profile.item.name", ""));
        return item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equals(expectedName);
    }

    public boolean isVisibilityItem(ItemStack item) {
        if (!cfg().getBoolean("visibility.item.enabled", true)) return false;
        if (item == null || !item.hasItemMeta()) return false;
        String shownName = ChatColor.translateAlternateColorCodes('&', cfg().getString("visibility.item.shown_name", ""));
        String hiddenName = ChatColor.translateAlternateColorCodes('&', cfg().getString("visibility.item.hidden_name", ""));
        if (!item.getItemMeta().hasDisplayName()) return false;
        String displayName = item.getItemMeta().getDisplayName();
        return displayName.equals(shownName) || displayName.equals(hiddenName);
    }

    public boolean isAnySpecialItem(ItemStack item) {
        return isSelectorItem(item) || isProfileItem(item) || isVisibilityItem(item);
    }

    // ===== Give items =====

    public void givePlayerItems(Player player) {
        if (cfg().getBoolean("gui.open_item.enabled", true)) {
            int slot = cfg().getInt("gui.open_item.slot", 4);
            String matName = cfg().getString("gui.open_item.material", "NETHER_STAR");
            String name = ChatColor.translateAlternateColorCodes('&', cfg().getString("gui.open_item.name", "&bServer Selector"));
            List<String> rawLore = cfg().getStringList("gui.open_item.lore");
            List<String> lore = new ArrayList<>();
            for (String l : rawLore) lore.add(ChatColor.translateAlternateColorCodes('&', l));
            Material material = Material.matchMaterial(matName);
            if (material == null) material = Material.NETHER_STAR;
            ItemStack item = new ItemStack(material);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(name);
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            player.getInventory().setItem(slot, item);
        }

        if (cfg().getBoolean("profile.item.enabled", true)) {
            int slot = cfg().getInt("profile.item.slot", 0);
            String name = ChatColor.translateAlternateColorCodes('&', cfg().getString("profile.item.name", "&eMy Profile"));
            List<String> rawLore = cfg().getStringList("profile.item.lore");
            List<String> lore = new ArrayList<>();
            for (String l : rawLore) lore.add(ChatColor.translateAlternateColorCodes('&', l));
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(player);
                meta.setDisplayName(name);
                meta.setLore(lore);
                head.setItemMeta(meta);
            }
            player.getInventory().setItem(slot, head);
        }

        updateVisibilityItem(player);
    }

    // ===== Visibility =====

    public void updateVisibilityItem(Player player) {
        if (!cfg().getBoolean("visibility.item.enabled", true)) return;
        int slot = cfg().getInt("visibility.item.slot", 8);
        boolean hidden = hiddenPlayers.contains(player.getUniqueId());
        String matName = hidden ? cfg().getString("visibility.item.hidden_material", "GRAY_DYE") : cfg().getString("visibility.item.shown_material", "LIME_DYE");
        String name = hidden ? cfg().getString("visibility.item.hidden_name", "&cPlayers: Hidden") : cfg().getString("visibility.item.shown_name", "&aPlayers: Shown");
        List<String> rawLore = hidden ? cfg().getStringList("visibility.item.hidden_lore") : cfg().getStringList("visibility.item.shown_lore");
        List<String> lore = new ArrayList<>();
        for (String l : rawLore) lore.add(ChatColor.translateAlternateColorCodes('&', l));
        Material material = Material.matchMaterial(matName);
        if (material == null) material = hidden ? Material.GRAY_DYE : Material.LIME_DYE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        player.getInventory().setItem(slot, item);
    }

    public void togglePlayerVisibility(Player player) {
        UUID uuid = player.getUniqueId();
        if (hiddenPlayers.contains(uuid)) {
            hiddenPlayers.remove(uuid);
            for (Player target : plugin.getServer().getOnlinePlayers()) player.showPlayer(plugin, target);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', cfg().getString("visibility.messages.shown", "&aแสดงผู้เล่นทั้งหมดแล้ว!")));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
        } else {
            hiddenPlayers.add(uuid);
            for (Player target : plugin.getServer().getOnlinePlayers()) player.hidePlayer(plugin, target);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', cfg().getString("visibility.messages.hidden", "&cซ่อนผู้เล่นคนอื่นทั้งหมดแล้ว!")));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
        }
        updateVisibilityItem(player);
    }

    public boolean isHidden(Player player) {
        return hiddenPlayers.contains(player.getUniqueId());
    }

    public void onJoin(Player player) {
        for (UUID hiddenUuid : hiddenPlayers) {
            Player hidingPlayer = plugin.getServer().getPlayer(hiddenUuid);
            if (hidingPlayer != null) hidingPlayer.hidePlayer(plugin, player);
        }
    }

    public void onQuit(Player player) {
        hiddenPlayers.remove(player.getUniqueId());
    }
}