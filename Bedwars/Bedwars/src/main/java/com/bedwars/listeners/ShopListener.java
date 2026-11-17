package com.bedwars.listeners;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.ArenaManager;
import com.bedwars.arena.ArenaState;
import com.bedwars.arena.Team;
import com.bedwars.gui.*;
import com.bedwars.util.ItemBuilder;
import com.bedwars.util.MessageUtil;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ShopListener implements Listener {

    private final BedwarsPlugin plugin;

    public ShopListener(BedwarsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = event.getView().getTitle();

        // 0. Spectator Teleporter GUI
        if (title.equals(SpectatorGUI.TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() != Material.PLAYER_HEAD) return;

            if (clicked.getItemMeta() instanceof org.bukkit.inventory.meta.SkullMeta skullMeta) {
                if (skullMeta.getOwningPlayer() != null && skullMeta.getOwningPlayer().isOnline()) {
                    Player target = skullMeta.getOwningPlayer().getPlayer();
                    if (target != null && target.isOnline()) {
                        player.teleport(target.getLocation());
                        MessageUtil.sendMessage(player, "§a✔ Teleported to spectate §e" + target.getName() + "§a!");
                        MessageUtil.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
                        player.closeInventory();
                    }
                }
            }
            return;
        }

        // 1. Team Selector GUI (Lobby only)
        if (title.equals(TeamSelectorGUI.TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Arena arena = plugin.getArenaManager().getArena(player);
            if (arena == null) return;

            for (Team team : arena.getTeams().values()) {
                if (clicked.getType() == team.getColor().getWoolMaterial()) {
                    if (team.hasPlayer(player.getUniqueId())) {
                        MessageUtil.sendMessage(player, "§cYou are already on this team!");
                        return;
                    }
                    if (team.getAlivePlayerCount() >= arena.getPlayersPerTeam()) {
                        MessageUtil.sendMessage(player, "§cThis team is full!");
                        MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                    Team oldTeam = arena.getPlayerTeam(player.getUniqueId());
                    if (oldTeam != null) {
                        oldTeam.removePlayer(player.getUniqueId());
                    }
                    team.addPlayer(player.getUniqueId());
                    player.setPlayerListName(team.getColor().getChatColor() + player.getName());
                    MessageUtil.sendMessage(player, "§aYou joined " + team.getColor().getPrefix() + "§a!");
                    MessageUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                    player.closeInventory();
                    return;
                }
            }
            return;
        }

        // 2. Arena Selector GUI
        if (title.startsWith(ArenaSelectorGUI.GUI_TITLE_PREFIX) || title.contains("Arena Selector")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            int clickedSlot = event.getSlot();

            // Category Tab Clicks (Row 0: slots 0, 1, 2, 3, 4)
            for (ArenaSelectorGUI.ModeCategory cat : ArenaSelectorGUI.ModeCategory.values()) {
                if (cat.getSlot() == clickedSlot) {
                    ArenaSelectorGUI.open(player, cat);
                    return;
                }
            }

            ArenaManager am = plugin.getArenaManager();

            if (clicked.getType() == Material.NETHER_STAR) {
                player.closeInventory();
                Arena best = am.findBestAvailableArena();
                if (best != null) {
                    best.join(player);
                } else {
                    MessageUtil.sendMessage(player, "§cNo available games to join right now!");
                }
                return;
            }

            if (clicked.getType() == Material.BARRIER && clickedSlot == 40) {
                player.closeInventory();
                return;
            }

            if (clicked.getItemMeta() != null && clicked.getItemMeta().hasLore()) {
                String aName = null;
                for (String line : clicked.getItemMeta().getLore()) {
                    if (line.contains("ID: §8")) {
                        aName = ChatColor.stripColor(line).replace("ID: ", "").trim();
                        break;
                    }
                }
                if (aName != null) {
                    Arena target = am.getArena(aName);
                    if (target != null) {
                        player.closeInventory();
                        if (event.isRightClick()) {
                            com.bedwars.gui.PracticeGUI.open(player, target);
                        } else {
                            target.join(player);
                        }
                    }
                }
            }
            return;
        }

        // 2.5. Bot Practice GUI
        if (title.startsWith(com.bedwars.gui.PracticeGUI.GUI_TITLE_PREFIX)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Arena arena = com.bedwars.gui.PracticeGUI.getSelectedArena(player.getUniqueId());
            if (arena == null) {
                player.closeInventory();
                return;
            }

            int slot = event.getSlot();
            Set<com.bedwars.arena.TeamColor> selected = com.bedwars.gui.PracticeGUI.getSelectedBotTeams(player.getUniqueId());
            Team playerTeam = arena.getPlayerTeam(player.getUniqueId());
            List<Team> enemyTeams = arena.getTeams().values().stream().filter(t -> !t.equals(playerTeam)).toList();

            // Slot 36: Cancel
            if (slot == 36 || clicked.getType() == Material.BARRIER) {
                player.closeInventory();
                com.bedwars.gui.PracticeGUI.clear(player.getUniqueId());
                return;
            }

            // Preset 1: 1 Enemy Team (Slot 10)
            if (slot == 10) {
                selected.clear();
                if (!enemyTeams.isEmpty()) selected.add(enemyTeams.get(0).getColor());
                com.bedwars.gui.PracticeGUI.open(player, arena);
                return;
            }

            // Preset 2: 2 Enemy Teams (Slot 12)
            if (slot == 12 && enemyTeams.size() >= 2) {
                selected.clear();
                selected.add(enemyTeams.get(0).getColor());
                selected.add(enemyTeams.get(1).getColor());
                com.bedwars.gui.PracticeGUI.open(player, arena);
                return;
            }

            // Preset 3: 3 Enemy Teams (Slot 14)
            if (slot == 14 && enemyTeams.size() >= 3) {
                selected.clear();
                for (int i = 0; i < 3; i++) selected.add(enemyTeams.get(i).getColor());
                com.bedwars.gui.PracticeGUI.open(player, arena);
                return;
            }

            // Preset 4: All Enemy Teams (Slot 16)
            if (slot == 16) {
                selected.clear();
                for (Team t : enemyTeams) selected.add(t.getColor());
                com.bedwars.gui.PracticeGUI.open(player, arena);
                return;
            }

            // Wool toggles (Slots 20 to 25)
            if (slot >= 20 && slot <= 25) {
                for (Team t : enemyTeams) {
                    if (t.getColor().getWoolMaterial() == clicked.getType()) {
                        if (selected.contains(t.getColor())) {
                            selected.remove(t.getColor());
                        } else {
                            selected.add(t.getColor());
                        }
                        com.bedwars.gui.PracticeGUI.open(player, arena);
                        return;
                    }
                }
            }

            // Start Practice Match (Slot 40)
            if (slot == 40 || clicked.getType() == Material.EMERALD_BLOCK) {
                if (selected.isEmpty()) {
                    MessageUtil.sendMessage(player, "§c✖ Please select at least 1 enemy team with bots!");
                    MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    return;
                }
                player.closeInventory();
                List<Team> botTeams = new ArrayList<>();
                for (com.bedwars.arena.TeamColor tc : selected) {
                    Team t = arena.getTeam(tc);
                    if (t != null) botTeams.add(t);
                }
                com.bedwars.gui.PracticeGUI.clear(player.getUniqueId());
                arena.startPracticeMatch(player, botTeams);
                return;
            }
            return;
        }

        // 3. Shop GUI
        if (title.startsWith(ShopGUI.SHOP_TITLE_PREFIX)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Arena arena = plugin.getArenaManager().getArena(player);
            if (arena == null || arena.getState() != ArenaState.IN_GAME) return;
            Team team = arena.getPlayerTeam(player.getUniqueId());

            int slot = event.getSlot();

            // Shortcut to Upgrades GUI (Slot 8)
            if (slot == 8) {
                UpgradesGUI.open(player);
                return;
            }

            // Check if clicking category bar (Strictly Slots 9 to 17)
            if (slot >= 9 && slot <= 17) {
                ShopGUI.ShopCategory targetCat = ShopGUI.ShopCategory.fromBarSlot(slot);
                if (targetCat != null) {
                    ShopGUI.open(player, targetCat);
                }
                return;
            }

            // Item slot purchase (Slots 18 to 53)
            if (slot >= 18) {
                ShopGUI.ShopCategory currentCat = getCategoryFromTitle(title);
                ShopGUI.ShopItemDef def = ShopGUI.getItem(currentCat, slot);
                if (def == null) return;

                // Progressive Tool Upgrades (Pickaxe & Axe)
                if (def.getAction() == ShopGUI.ShopAction.TOOL_PICKAXE_UPGRADE) {
                    ShopGUI.DynamicToolInfo info = ShopGUI.getDynamicPickaxeInfo(player, arena);
                    if (info.maxed) {
                        MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        MessageUtil.sendMessage(player, "§a✔ You have already reached the maximum pickaxe tier!");
                        return;
                    }
                    int scaled = ShopGUI.getScaledPrice(info.basePrice, arena);
                    int bal = ShopGUI.countItem(player, info.currency);
                    if (bal < scaled) {
                        MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        MessageUtil.sendMessage(player, "§cYou do not have enough funds to upgrade pickaxe!");
                        return;
                    }
                    ShopGUI.deductItem(player, info.currency, scaled);
                    upgradePlayerPickaxe(player, arena, info.icon.getType(), info.name);
                    MessageUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
                    ShopGUI.open(player, currentCat);
                    return;
                }

                if (def.getAction() == ShopGUI.ShopAction.TOOL_AXE_UPGRADE) {
                    ShopGUI.DynamicToolInfo info = ShopGUI.getDynamicAxeInfo(player, arena);
                    if (info.maxed) {
                        MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        MessageUtil.sendMessage(player, "§a✔ You have already reached the maximum axe tier!");
                        return;
                    }
                    int scaled = ShopGUI.getScaledPrice(info.basePrice, arena);
                    int bal = ShopGUI.countItem(player, info.currency);
                    if (bal < scaled) {
                        MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        MessageUtil.sendMessage(player, "§cYou do not have enough funds to upgrade axe!");
                        return;
                    }
                    ShopGUI.deductItem(player, info.currency, scaled);
                    upgradePlayerAxe(player, arena, info.icon.getType(), info.name);
                    MessageUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
                    ShopGUI.open(player, currentCat);
                    return;
                }

                // Check if already owned BEFORE deducting funds!
                if (ShopGUI.isAlreadyOwnedOrHigher(player, arena, def)) {
                    MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    MessageUtil.sendMessage(player, "§c✖ You already have this item or a higher tier!");
                    return;
                }

                int scaledPrice = ShopGUI.getScaledPrice(def.getPrice(), arena);
                int balance = ShopGUI.countItem(player, def.getCurrency());
                if (balance < scaledPrice) {
                    MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    MessageUtil.sendMessage(player, "§cYou do not have enough funds to purchase " + def.getCustomName() + "!");
                    return;
                }

                // Deduct currency
                ShopGUI.deductItem(player, def.getCurrency(), scaledPrice);

                // Perform action
                handleShopAction(player, arena, team, def);

                MessageUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.3f);

                // Refresh shop to update balances
                ShopGUI.open(player, currentCat);
            }
            return;
        }

        // 4. Upgrades GUI
        if (title.equals(UpgradesGUI.UPGRADE_TITLE)) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            Arena arena = plugin.getArenaManager().getArena(player);
            if (arena == null || arena.getState() != ArenaState.IN_GAME) return;
            Team team = arena.getPlayerTeam(player.getUniqueId());
            if (team == null) return;

            int slot = event.getSlot();
            handleTeamUpgradeSlot(player, arena, team, slot);
            UpgradesGUI.open(player);
        }
    }

    private ShopGUI.ShopCategory getCategoryFromTitle(String title) {
        String catName = title.replace(ShopGUI.SHOP_TITLE_PREFIX, "").trim();
        for (ShopGUI.ShopCategory cat : ShopGUI.ShopCategory.values()) {
            if (cat.getDisplayName().equalsIgnoreCase(catName)) return cat;
        }
        return ShopGUI.ShopCategory.QUICK_BUY;
    }

    private void handleShopAction(Player player, Arena arena, Team team, ShopGUI.ShopItemDef def) {
        switch (def.getAction()) {
            case SWORD_STONE -> giveOrUpgradeSword(player, team, Material.STONE_SWORD, def.getCustomName());
            case SWORD_IRON -> giveOrUpgradeSword(player, team, Material.IRON_SWORD, def.getCustomName());
            case SWORD_DIAMOND -> giveOrUpgradeSword(player, team, Material.DIAMOND_SWORD, def.getCustomName());
            case SWORD_NETHERITE -> giveOrUpgradeSword(player, team, Material.NETHERITE_SWORD, def.getCustomName());

            case ARMOR_CHAIN -> {
                arena.setPlayerArmorTier(player.getUniqueId(), Arena.ArmorTier.CHAIN);
                arena.updatePlayerArmor(player, team);
            }
            case ARMOR_IRON -> {
                arena.setPlayerArmorTier(player.getUniqueId(), Arena.ArmorTier.IRON);
                arena.updatePlayerArmor(player, team);
            }
            case ARMOR_DIAMOND -> {
                arena.setPlayerArmorTier(player.getUniqueId(), Arena.ArmorTier.DIAMOND);
                arena.updatePlayerArmor(player, team);
            }
            case ARMOR_NETHERITE -> {
                arena.setPlayerArmorTier(player.getUniqueId(), Arena.ArmorTier.NETHERITE);
                arena.updatePlayerArmor(player, team);
            }
            case TOOL_SHEARS -> {
                if (arena != null) arena.setHasShears(player.getUniqueId(), true);
                ItemStack shears = new ItemBuilder(Material.SHEARS).unbreakable(true).name(def.getCustomName()).build();
                player.getInventory().addItem(shears);
            }
            case POPUP_FORTRESS -> {
                ItemStack fortress = new ItemBuilder(Material.PACKED_ICE).name(def.getCustomName()).build();
                player.getInventory().addItem(fortress);
            }
            case BED_DEFENDER_GOLEM -> {
                ItemStack golem = new ItemBuilder(Material.IRON_BLOCK).name(def.getCustomName()).build();
                player.getInventory().addItem(golem);
            }
            case RADAR_TRACKER -> {
                ItemStack radar = new ItemBuilder(Material.COMPASS).name(def.getCustomName()).build();
                player.getInventory().addItem(radar);
            }
            case ELYTRA_EQUIP -> {
                ItemStack elytra = new ItemStack(Material.ELYTRA);
                if (elytra.getItemMeta() instanceof org.bukkit.inventory.meta.Damageable dmg) {
                    dmg.setDamage(407); // Only 25 durability left!
                    dmg.setDisplayName("§d★ Fragile Celestial Elytra");
                    dmg.setLore(java.util.List.of(
                            "§7Fragile wings for tactical gliding",
                            "§c⚠ Low durability - breaks quickly after flight!"
                    ));
                    elytra.setItemMeta(dmg);
                }
                player.getInventory().setChestplate(elytra);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_ELYTRA, 1f, 1f);
            }
            case STICK_KNOCKBACK -> {
                int kbLevel = (team != null && team.getSharpnessLevel() > 0) ? 2 : 1;
                ItemStack stick = new ItemBuilder(Material.STICK)
                        .name("§d⚡ Kinetic Knockback Stick (Tier " + kbLevel + ")")
                        .lore(
                                "§7Knocks foes off bridges easily",
                                "§e▶ Current Level: Knockback " + kbLevel + " (Max 2)",
                                "§7(Upgrades with team sword sharpness)"
                        )
                        .enchant(Enchantment.KNOCKBACK, kbLevel)
                        .build();
                player.getInventory().addItem(stick);
            }
            case GIVE_ITEM -> {
                Material mat = def.getItemToGive().getType();
                if (ShopGUI.getPickaxeTier(mat) > 0) {
                    upgradePlayerPickaxe(player, arena, mat, def.getCustomName());
                    return;
                }
                if (ShopGUI.getAxeTier(mat) > 0) {
                    upgradePlayerAxe(player, arena, mat, def.getCustomName());
                    return;
                }

                ItemStack give = def.getItemToGive().clone();

                if (give.getType().name().endsWith("_WOOL") && team != null) {
                    give.setType(team.getColor().getWoolMaterial());
                }
                if ((give.getType() == Material.GLASS || give.getType().name().endsWith("_STAINED_GLASS")) && team != null) {
                    give.setType(team.getColor().getGlassMaterial());
                }

                player.getInventory().addItem(give);
            }
        }
    }

    private void upgradePlayerPickaxe(Player player, Arena arena, Material pickMat, String name) {
        int newTier = ShopGUI.getPickaxeTier(pickMat);
        if (arena != null) {
            arena.setPlayerPickaxeTier(player.getUniqueId(), newTier);
        }
        int eff = newTier >= 4 ? 3 : (newTier == 3 ? 2 : 1);
        ItemStack newPick = new ItemBuilder(pickMat).unbreakable(true).name(name).enchant(Enchantment.EFFICIENCY, eff).build();

        // Replace old pickaxe in same inventory slot
        int slot = -1;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && ShopGUI.getPickaxeTier(it.getType()) > 0) {
                slot = i;
                break;
            }
        }
        if (slot != -1) {
            player.getInventory().setItem(slot, newPick);
        } else {
            player.getInventory().addItem(newPick);
        }
    }

    private void upgradePlayerAxe(Player player, Arena arena, Material axeMat, String name) {
        int newTier = ShopGUI.getAxeTier(axeMat);
        if (arena != null) {
            arena.setPlayerAxeTier(player.getUniqueId(), newTier);
        }
        int eff = newTier >= 4 ? 3 : (newTier == 3 ? 2 : 1);
        ItemStack newAxe = new ItemBuilder(axeMat).unbreakable(true).name(name).enchant(Enchantment.EFFICIENCY, eff).build();

        // Replace old axe in same inventory slot
        int slot = -1;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && ShopGUI.getAxeTier(it.getType()) > 0) {
                slot = i;
                break;
            }
        }
        if (slot != -1) {
            player.getInventory().setItem(slot, newAxe);
        } else {
            player.getInventory().addItem(newAxe);
        }
    }

    private void giveOrUpgradeSword(Player player, Team team, Material swordType, String displayName) {
        ItemStack newSword = new ItemBuilder(swordType)
                .unbreakable(true)
                .name(displayName)
                .build();

        if (team != null && team.getSharpnessLevel() > 0) {
            newSword.addEnchantment(Enchantment.SHARPNESS, team.getSharpnessLevel());
        }

        // Find existing sword and replace in same slot
        int swordSlot = -1;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType().name().endsWith("_SWORD")) {
                swordSlot = i;
                break;
            }
        }

        if (swordSlot != -1) {
            player.getInventory().setItem(swordSlot, newSword);
        } else {
            if (player.getInventory().getItem(0) == null) {
                player.getInventory().setItem(0, newSword);
            } else {
                player.getInventory().addItem(newSword);
            }
        }
    }

    private void handleTeamUpgradeSlot(Player player, Arena arena, Team team, int slot) {
        int diamonds = ShopGUI.countItem(player, Material.DIAMOND);

        // Slot 19: Sharpness (4 Diamonds)
        if (slot == 19) {
            if (team.getSharpnessLevel() > 0) return;
            if (diamonds < 4) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                MessageUtil.sendMessage(player, "§cYou do not have enough Diamonds for this upgrade!");
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, 4);
            team.setSharpnessLevel(1);

            for (UUID u : team.getPlayers()) {
                Player p = org.bukkit.Bukkit.getPlayer(u);
                if (p != null) {
                    for (ItemStack item : p.getInventory().getContents()) {
                        if (item != null) {
                            if (item.getType().name().endsWith("_SWORD")) {
                                item.addEnchantment(Enchantment.SHARPNESS, 1);
                            } else if (item.getType() == Material.STICK) {
                                item.addUnsafeEnchantment(Enchantment.KNOCKBACK, 2);
                                if (item.getItemMeta() != null) {
                                    var meta = item.getItemMeta();
                                    meta.setDisplayName("§d⚡ Kinetic Knockback Stick (Tier 2)");
                                    item.setItemMeta(meta);
                                }
                            }
                        }
                    }
                    MessageUtil.sendTitle(p, "§a§lUPGRADE PURCHASED!", "§fYour team received §eSharpness I§f!", 5, 30, 5);
                }
            }
            MessageUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
            return;
        }

        // Slot 20: Protection (2, 4, 8, 16 Diamonds)
        if (slot == 20) {
            int prot = team.getProtectionLevel();
            int[] costs = {2, 4, 8, 16};
            if (prot >= 4 || diamonds < costs[prot]) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, costs[prot]);
            team.setProtectionLevel(prot + 1);
            arena.updateTeamArmor(team);

            for (UUID u : team.getPlayers()) {
                Player p = org.bukkit.Bukkit.getPlayer(u);
                if (p != null) {
                    MessageUtil.sendTitle(p, "§b§lUPGRADE PURCHASED!", "§fTeam armor upgraded to §bProtection " + (prot + 1), 5, 30, 5);
                }
            }
            MessageUtil.playSound(player, Sound.BLOCK_ANVIL_USE, 1f, 1.2f);
            return;
        }

        // Slot 21: Maniac Miner / Haste (2, 4 Diamonds)
        if (slot == 21) {
            int haste = team.getHasteLevel();
            int cost = (haste == 0) ? 2 : 4;
            if (haste >= 2 || diamonds < cost) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, cost);
            team.setHasteLevel(haste + 1);

            for (UUID u : team.getPlayers()) {
                Player p = org.bukkit.Bukkit.getPlayer(u);
                if (p != null) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, Integer.MAX_VALUE, team.getHasteLevel() - 1));
                    MessageUtil.sendTitle(p, "§6§lUPGRADE PURCHASED!", "§fTeam received mining speed §6Haste " + team.getHasteLevel(), 5, 30, 5);
                }
            }
            MessageUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
            return;
        }

        // Slot 22: Thermal Forge (2, 4, 6, 8 Diamonds)
        if (slot == 22) {
            int forge = team.getForgeLevel();
            int[] costs = {2, 4, 6, 8};
            if (forge >= 4 || diamonds < costs[forge]) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, costs[forge]);
            team.setForgeLevel(forge + 1);
            arena.updateTeamSpawnerTier(team);

            for (UUID u : team.getPlayers()) {
                Player p = org.bukkit.Bukkit.getPlayer(u);
                if (p != null) {
                    MessageUtil.sendTitle(p, "§c§lFORGE UPGRADED!", "§fBase forge upgraded to §eTier " + (forge + 1), 5, 30, 5);
                }
            }
            MessageUtil.playSound(player, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1.2f);
            return;
        }

        // Slot 23: Heal Pool (1 Diamond)
        if (slot == 23) {
            if (team.hasHealPool() || diamonds < 1) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, 1);
            team.setHealPool(true);

            for (UUID u : team.getPlayers()) {
                Player p = org.bukkit.Bukkit.getPlayer(u);
                if (p != null) {
                    MessageUtil.sendTitle(p, "§d§lUPGRADE PURCHASED!", "§fTeam regeneration pool is now active!", 5, 30, 5);
                }
            }
            MessageUtil.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
            return;
        }

        // Slot 24: Iron Skin (2 Diamonds)
        if (slot == 24) {
            if (team.hasIronSkin() || diamonds < 2) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, 2);
            team.setIronSkin(true);
            MessageUtil.playSound(player, Sound.ITEM_ARMOR_EQUIP_NETHERITE, 1f, 1.2f);
            MessageUtil.sendMessage(player, "§a✔ Base Iron Skin defense upgrade activated!");
            return;
        }

        // Slot 25: Mineral Magnet (2 Diamonds)
        if (slot == 25) {
            if (team.hasMineralMagnet() || diamonds < 2) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, 2);
            team.setMineralMagnet(true);
            MessageUtil.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
            MessageUtil.sendMessage(player, "§a✔ Mineral Magnet upgrade activated!");
            return;
        }

        // Slot 28: Base Siren Trap (Alarm) (1 Diamond)
        if (slot == 28) {
            if (diamonds < 1 || team.getActiveTraps().size() >= 3) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, 1);
            team.addTrap("ALARM");
            MessageUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
            MessageUtil.sendMessage(player, "§a✔ Base Alarm Siren Trap armed!");
            return;
        }

        // Slot 29: Counter-Assault Trap (1 Diamond)
        if (slot == 29) {
            if (diamonds < 1 || team.getActiveTraps().size() >= 3) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, 1);
            team.addTrap("COUNTER");
            MessageUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
            MessageUtil.sendMessage(player, "§a✔ Counter-Assault Trap armed!");
            return;
        }

        // Slot 30: Gravity Web Trap (1 Diamond)
        if (slot == 30) {
            if (diamonds < 1 || team.getActiveTraps().size() >= 3) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, 1);
            team.addTrap("GRAVITY");
            MessageUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
            MessageUtil.sendMessage(player, "§a✔ Gravity Web Trap armed!");
            return;
        }

        // Slot 31: Sabotage Trap (1 Diamond)
        if (slot == 31) {
            if (diamonds < 1 || team.getActiveTraps().size() >= 3) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, 1);
            team.addTrap("SABOTAGE");
            MessageUtil.playSound(player, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
            MessageUtil.sendMessage(player, "§a✔ Sabotage Trap armed!");
            return;
        }

        // Slot 33: Dragon Ally (5 Diamonds)
        if (slot == 33) {
            if (team.hasDragonBuff() || diamonds < 5) {
                MessageUtil.playSound(player, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
            ShopGUI.deductItem(player, Material.DIAMOND, 5);
            team.setDragonBuff(true);
            MessageUtil.playSound(player, Sound.ENTITY_ENDER_DRAGON_GROWL, 1f, 1f);
            MessageUtil.sendMessage(player, "§a✔ Ender Dragon Ally summoned!");
        }
    }
}
