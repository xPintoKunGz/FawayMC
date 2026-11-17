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

import java.util.List;

public class UpgradesGUI {

    public static final String UPGRADE_TITLE = "§8✦ §b§lTEAM UPGRADES §8✦";

    public static void open(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, UPGRADE_TITLE);
        Arena arena = BedwarsPlugin.getInstance().getArenaManager().getArena(player);
        if (arena == null) return;
        Team team = arena.getPlayerTeam(player.getUniqueId());
        if (team == null) return;

        int diamonds = ShopGUI.countItem(player, Material.DIAMOND);

        ItemStack border = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        for (int i = 0; i < 9; i++) inv.setItem(i, border);
        for (int i = 36; i < 45; i++) inv.setItem(i, border);

        // Diamonds balance at top
        inv.setItem(4, new ItemBuilder(Material.DIAMOND)
                .name("§b§lYour Diamonds: §a" + diamonds)
                .lore("§7Spend Diamonds to purchase permanent upgrades for your whole team!")
                .build());

        // 1. Sharpness (4 Diamonds) - Slot 19
        boolean hasSharp = team.getSharpnessLevel() > 0;
        inv.setItem(19, new ItemBuilder(Material.IRON_SWORD)
                .name("§e§lSharpened Swords")
                .lore(
                        "§7Permanently grants Sharpness I to all team swords!",
                        "",
                        "§7Cost: §b4 Diamonds",
                        "§8────────────────────",
                        hasSharp ? "§a✔ Unlocked!" : (diamonds >= 4 ? "§e▶ Click to purchase!" : "§c✖ Need " + (4 - diamonds) + " more Diamonds")
                ).build());

        // 2. Reinforced Armor (Protection) - Slot 20
        int prot = team.getProtectionLevel();
        int[] protCosts = {2, 4, 8, 16};
        int nextProtCost = prot < 4 ? protCosts[prot] : 0;
        inv.setItem(20, new ItemBuilder(Material.IRON_CHESTPLATE)
                .name("§b§lReinforced Armor")
                .lore(
                        "§7Increases Protection enchantment on all team armor",
                        "§7Current: §eProtection " + (prot > 0 ? prot : "0"),
                        "",
                        prot < 4 ? "§7Next Tier Cost: §b" + nextProtCost + " Diamonds" : "§a✔ Maximum Tier Reached",
                        "§8────────────────────",
                        prot >= 4 ? "§a✔ Max Protection" : (diamonds >= nextProtCost ? "§e▶ Click to upgrade to Tier " + (prot + 1) : "§c✖ Insufficient Diamonds")
                ).build());

        // 3. Maniac Miner (Haste) - Slot 21
        int haste = team.getHasteLevel();
        int nextHasteCost = haste == 0 ? 2 : 4;
        inv.setItem(21, new ItemBuilder(Material.GOLDEN_PICKAXE)
                .name("§6§lManiac Miner")
                .lore(
                        "§7Permanently grants Haste mining speed to all teammates",
                        "§7Current: §eHaste " + (haste > 0 ? haste : "0"),
                        "",
                        haste < 2 ? "§7Cost: §b" + nextHasteCost + " Diamonds" : "§a✔ Maximum Tier Reached",
                        "§8────────────────────",
                        haste >= 2 ? "§a✔ Maxed Out" : (diamonds >= nextHasteCost ? "§e▶ Click to purchase!" : "§c✖ Insufficient Diamonds")
                ).build());

        // 4. Thermal Forge - Slot 22
        int forge = team.getForgeLevel();
        int[] forgeCosts = {2, 4, 6, 8};
        int nextForgeCost = forge < 4 ? forgeCosts[forge] : 0;
        inv.setItem(22, new ItemBuilder(Material.FURNACE)
                .name("§c§lThermal Base Forge")
                .lore(
                        "§7Significantly speeds up Iron & Gold production at base generator",
                        "§7Tier 3 spawns base Diamonds, Tier 4 spawns base Emeralds!",
                        "§7Current: §eTier " + forge,
                        "",
                        forge < 4 ? "§7Next Tier Cost: §b" + nextForgeCost + " Diamonds" : "§a✔ Maximum Tier Reached",
                        "§8────────────────────",
                        forge >= 4 ? "§a✔ Maxed Out" : (diamonds >= nextForgeCost ? "§e▶ Click to upgrade to Tier " + (forge + 1) : "§c✖ Insufficient Diamonds")
                ).build());

        // 5. Heal Pool - Slot 23
        boolean hasHeal = team.hasHealPool();
        inv.setItem(23, new ItemBuilder(Material.BEACON)
                .name("§d§lHealing Nano Beacon")
                .lore(
                        "§7Continuously regenerates team health while inside your base",
                        "",
                        "§7Cost: §b1 Diamond",
                        "§8────────────────────",
                        hasHeal ? "§a✔ Unlocked!" : (diamonds >= 1 ? "§e▶ Click to purchase!" : "§c✖ Insufficient Diamonds")
                ).build());

        // 6. Iron Skin Base Guard - Slot 24
        boolean hasIronSkin = team.hasIronSkin();
        inv.setItem(24, new ItemBuilder(Material.SHIELD)
                .name("§8§lBase Iron Skin")
                .lore(
                        "§7Grants Resistance I to all teammates while defending your base",
                        "",
                        "§7Cost: §b2 Diamonds",
                        "§8────────────────────",
                        hasIronSkin ? "§a✔ Unlocked!" : (diamonds >= 2 ? "§e▶ Click to purchase!" : "§c✖ Insufficient Diamonds")
                ).build());

        // 7. Mineral Magnet - Slot 25
        boolean hasMagnet = team.hasMineralMagnet();
        inv.setItem(25, new ItemBuilder(Material.HOPPER)
                .name("§3§lMineral Magnet")
                .lore(
                        "§7Automatically pulls nearby spawned minerals to defending players!",
                        "",
                        "§7Cost: §b2 Diamonds",
                        "§8────────────────────",
                        hasMagnet ? "§a✔ Unlocked!" : (diamonds >= 2 ? "§e▶ Click to purchase!" : "§c✖ Insufficient Diamonds")
                ).build());

        // Traps Row (Slots 28, 29, 30, 31)
        List<String> traps = team.getActiveTraps();
        inv.setItem(28, new ItemBuilder(Material.TRIPWIRE_HOOK)
                .name("§c§lBase Siren Trap")
                .lore(
                        "§7Cancels enemy Invisibility, causes Glowing effect, and sounds alarm!",
                        "",
                        "§7Cost: §b1 Diamond",
                        "§8────────────────────",
                        traps.size() >= 3 ? "§c✖ Trap Queue Full (3/3)" : (diamonds >= 1 ? "§e▶ Click to set trap!" : "§c✖ Insufficient Diamonds")
                ).build());

        inv.setItem(29, new ItemBuilder(Material.FEATHER)
                .name("§a§lCounter-Assault Trap")
                .lore(
                        "§7Grants Speed II and Jump Boost II to all base defenders for 15s!",
                        "",
                        "§7Cost: §b1 Diamond",
                        "§8────────────────────",
                        traps.size() >= 3 ? "§c✖ Trap Queue Full (3/3)" : (diamonds >= 1 ? "§e▶ Click to set trap!" : "§c✖ Insufficient Diamonds")
                ).build());

        inv.setItem(30, new ItemBuilder(Material.COBWEB)
                .name("§4§lGravity Web Trap")
                .lore(
                        "§7Inflicts Blindness and severe Slowness III (5s) on invading enemies!",
                        "",
                        "§7Cost: §b1 Diamond",
                        "§8────────────────────",
                        traps.size() >= 3 ? "§c✖ Trap Queue Full (3/3)" : (diamonds >= 1 ? "§e▶ Click to set trap!" : "§c✖ Insufficient Diamonds")
                ).build());

        inv.setItem(31, new ItemBuilder(Material.REDSTONE_TORCH)
                .name("§6§lSabotage Trap")
                .lore(
                        "§7Inflicts Mining Fatigue III (10s) to disrupt enemy bed mining!",
                        "",
                        "§7Cost: §b1 Diamond",
                        "§8────────────────────",
                        traps.size() >= 3 ? "§c✖ Trap Queue Full (3/3)" : (diamonds >= 1 ? "§e▶ Click to set trap!" : "§c✖ Insufficient Diamonds")
                ).build());

        // Dragon Ally - Slot 33
        boolean hasDragon = team.hasDragonBuff();
        inv.setItem(33, new ItemBuilder(Material.DRAGON_HEAD)
                .name("§5§lEnder Dragon Ally")
                .lore(
                        "§7Summons an Ender Dragon to fight for your team during Sudden Death!",
                        "",
                        "§7Cost: §b5 Diamonds",
                        "§8────────────────────",
                        hasDragon ? "§a✔ Unlocked!" : (diamonds >= 5 ? "§e▶ Click to purchase!" : "§c✖ Insufficient Diamonds")
                ).build());

        player.openInventory(inv);
        MessageUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }
}
