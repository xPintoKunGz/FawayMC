package org.kcm.bloomery.fawaymc.daily.gui;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import me.clip.placeholderapi.PlaceholderAPI; // ✅ ADDED
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.kcm.bloomery.fawaymc.daily.Global;
import org.kcm.bloomery.fawaymc.daily.data.PlayerStreak;
import org.kcm.bloomery.fawaymc.daily.manager.DailyManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DailyMainGui {
    public static final NamespacedKey ACTION_KEY = new NamespacedKey("fawaydaily", "gui_action");
    public static final int[] DAY_SLOTS = new int[]{10, 11, 12, 13, 14, 15, 16};
    public static final int PROFILE_SLOT = 4;
    public static final int COUNTDOWN_SLOT = 22;
    public static final int STREAK_SLOT = 29;
    public static final int RANK_SLOT = 31;
    public static final int TOTAL_SLOT = 33;
    public static final int CLOSE_SLOT = 45;
    public static final int INFO_SLOT = 49;

    private final Global plugin;
    private final DailyManager manager;

    public DailyMainGui(Global plugin) {
        this.plugin = plugin;
        this.manager = plugin.getDailyManager();
    }

    public void open(Player player) {
        String title = this.color(player, this.plugin.getConfig().getString("gui.title", "&8&l✦ &e&lDAILY REWARDS &8&l✦"));
        DailyGuiHolder holder = new DailyGuiHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, title);
        holder.setInventory(inv);
        ItemStack border = this.createBorder();
        ItemStack filler = this.createFiller();

        for (int i = 0; i < 54; ++i) {
            inv.setItem(i, filler);
        }

        for (int i = 0; i < 9; ++i) {
            inv.setItem(i, border);
        }

        for (int i = 45; i < 54; ++i) {
            inv.setItem(i, border);
        }

        inv.setItem(9, border);
        inv.setItem(17, border);
        inv.setItem(18, border);
        inv.setItem(26, border);
        inv.setItem(27, border);
        inv.setItem(35, border);
        inv.setItem(36, border);
        inv.setItem(44, border);

        PlayerStreak streak = this.manager.getCache().get(player.getUniqueId());
        if (streak == null) {
            streak = new PlayerStreak(player.getUniqueId());
        }

        boolean canClaim = this.manager.canClaim(streak);
        int nextDay = this.manager.getNextDay(streak);
        String rank = this.manager.getPlayerRank(player);
        int cycleLength = this.plugin.getConfig().getInt("settings.cycle_length", 7);
        long remaining = this.manager.getTimeUntilNextClaim(streak);
        long coins = this.manager.getPlayerCoins(player.getUniqueId());
        boolean broken = this.manager.isStreakBroken(streak);
        int currentStreak = streak.getStreakDays();
        int effectiveStreak = (canClaim && nextDay == 1) ? 0 : (broken ? 0 : currentStreak);

        inv.setItem(PROFILE_SLOT, this.createProfileHead(player, streak, rank, nextDay, coins, broken));

        for (int day = 1; day <= cycleLength && day <= DAY_SLOTS.length; ++day) {
            int slot = DAY_SLOTS[day - 1];
            ConfigurationSection cfg = this.manager.getRewards(rank, day);
            DayState state = this.computeState(day, nextDay, canClaim, effectiveStreak, cycleLength);
            inv.setItem(slot, this.createDayItem(player, day, state, cfg, cycleLength, effectiveStreak, remaining, canClaim));
        }

        inv.setItem(COUNTDOWN_SLOT, this.createClaimOrCountdownItem(player, remaining, canClaim, nextDay, streak));
        inv.setItem(STREAK_SLOT, this.createStreakInfo(player, streak, broken));
        inv.setItem(RANK_SLOT, this.createRankInfo(player, rank));
        inv.setItem(TOTAL_SLOT, this.createTotalInfo(player, streak));
        inv.setItem(CLOSE_SLOT, this.createButton(player, Material.BARRIER, "&c&l✘ Close", List.of("&7Click to close this menu"), "close"));
        inv.setItem(INFO_SLOT, this.createButton(player, Material.BOOK, "&e&lℹ Info", List.of(
                "&7Daily Rewards System", "",
                "&f&lHOW IT WORKS",
                " &8▪ &7Claim once every &e24 hours",
                " &8▪ &7Missing &e48 hours &7resets streak",
                " &8▪ &7Higher ranks get better boxes",
                " &8▪ &7Day " + cycleLength + " = milestone reward",
                "",
                "&f&lCOMMANDS",
                " &8▪ &f/daily &7— open menu",
                " &8▪ &f/daily claim &7— quick claim"
        ), "noop"));
        player.openInventory(inv);
    }

    public ItemStack createClaimOrCountdownItem(Player player, long remaining, boolean canClaim, int nextDay, PlayerStreak streak) {
        String texture = this.plugin.getConfig().getString("gui.heads.claim.texture", "");
        Material fallback = canClaim ? Material.FIREWORK_ROCKET : Material.CLOCK;

        if (canClaim) {
            String displayName = this.color(player, "&a&l⚡ CLAIM REWARD ⚡");
            List<String> lore = new ArrayList<>();
            lore.add(this.color(player, "&7Click to claim your daily reward!"));
            lore.add("");
            lore.add(this.color(player, " &8▪ &7Day: &e" + nextDay));
            int newStreak = streak.getStreakDays() + 1;
            lore.add(this.color(player, " &8▪ &7Streak will become: &e" + newStreak));
            lore.add("");
            lore.add(this.color(player, " &8▪ &7Status: &a&l► READY NOW!"));
            lore.add("");
            lore.add(this.color(player, "&a&l► CLICK NOW!"));

            ItemStack item = this.createHeadOrMaterial(player, texture, fallback, displayName, lore);
            this.glow(item);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, "claim");
                item.setItemMeta(meta);
            }
            return item;
        } else {
            String displayName = this.color(player, "&e&l⏱ Next Claim");
            List<String> lore = new ArrayList<>();
            lore.add(this.color(player, "&7Your next claim is in:"));
            String countdownStr = this.manager.formatCountdown(remaining);
            lore.add(this.color(player, " &8▪ &e&l" + countdownStr));
            lore.add("");
            lore.add(this.color(player, " &8▪ &7Status: &cCLAIMED TODAY"));
            lore.add("");
            lore.add(this.color(player, "&7The timer continues even when"));
            lore.add(this.color(player, "&7you leave the server."));
            return this.createHeadOrMaterial(player, texture, fallback, displayName, lore);
        }
    }

    public void refreshTimeBased(Player player, Inventory inv) {
        if (player == null || inv == null) return;

        PlayerStreak streak = this.manager.getCache().get(player.getUniqueId());
        if (streak == null) return;

        boolean canClaim = this.manager.canClaim(streak);
        int nextDay = this.manager.getNextDay(streak);
        long remaining = this.manager.getTimeUntilNextClaim(streak);
        String rank = this.manager.getPlayerRank(player);
        int cycleLength = this.plugin.getConfig().getInt("settings.cycle_length", 7);
        boolean broken = this.manager.isStreakBroken(streak);
        int currentStreak = streak.getStreakDays();
        int effectiveStreak = (canClaim && nextDay == 1) ? 0 : (broken ? 0 : currentStreak);

        if (inv.getSize() > COUNTDOWN_SLOT) {
            inv.setItem(COUNTDOWN_SLOT, this.createClaimOrCountdownItem(player, remaining, canClaim, nextDay, streak));
        }

        if (!canClaim) {
            int nextUpDay = effectiveStreak + 1;
            if (nextUpDay >= 1 && nextUpDay <= cycleLength && nextUpDay <= DAY_SLOTS.length) {
                int slot = DAY_SLOTS[nextUpDay - 1];
                if (slot < inv.getSize()) {
                    ConfigurationSection cfg = this.manager.getRewards(rank, nextUpDay);
                    DayState state = this.computeState(nextUpDay, nextDay, canClaim, effectiveStreak, cycleLength);
                    inv.setItem(slot, this.createDayItem(player, nextUpDay, state, cfg, cycleLength, effectiveStreak, remaining, canClaim));
                }
            }
        }
    }

    private DayState computeState(int day, int nextDay, boolean canClaim, int effectiveStreak, int cycleLength) {
        if (effectiveStreak > 0 && day <= effectiveStreak) {
            return DayState.CLAIMED;
        } else if (day == nextDay && canClaim) {
            return DayState.CURRENT;
        } else {
            return (canClaim && nextDay == 1 && day == 1) ? DayState.CURRENT : DayState.LOCKED;
        }
    }

    private Material resolveDayIcon(ConfigurationSection cfg, DayState state, int day, int cycleLength) {
        if (state == DayState.CLAIMED) {
            return Material.MINECART;
        }
        if (day == cycleLength) {
            return Material.TNT_MINECART;
        }

        String matName = cfg != null ? cfg.getString("icon", "CHEST_MINECART") : "CHEST_MINECART";
        String upper = matName.toUpperCase();

        if (upper.equals("CHEST") || upper.equals("TRAPPED_CHEST") || upper.equals("CHEST_MINECART")) {
            return Material.CHEST_MINECART;
        }
        if (upper.equals("ENDER_CHEST") || upper.equals("FURNACE_MINECART")) {
            return Material.FURNACE_MINECART;
        }
        if (upper.equals("NETHER_STAR") || upper.equals("TNT_MINECART")) {
            return Material.TNT_MINECART;
        }

        if (day <= 3) return Material.CHEST_MINECART;
        if (day <= 6) return Material.FURNACE_MINECART;
        return Material.TNT_MINECART;
    }

    private ItemStack createDayItem(Player player, int day, DayState state, ConfigurationSection cfg,
                                    int cycleLength, int effectiveStreak, long remaining, boolean canClaim) {
        Material mat = this.resolveDayIcon(cfg, state, day, cycleLength);
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayName = cfg != null ? cfg.getString("display_name", "&eDay " + day) : "&7Day " + day;

        String prefix;
        switch (state) {
            case CLAIMED: prefix = "&a&l✔ "; break;
            case CURRENT: prefix = "&e&l▶ "; break;
            default:      prefix = "&7&l🔒 "; break;
        }

        meta.setDisplayName(this.color(player, prefix + displayName));

        List<String> lore = new ArrayList<>();
        lore.add(this.color(player, "&7Day &e" + day + " &7of the cycle"));
        lore.add("");

        if (cfg != null) {
            int coins = cfg.getInt("coins", 0);
            ConfigurationSection boxes = cfg.getConfigurationSection("boxes");

            if (coins > 0 || (boxes != null && !boxes.getKeys(false).isEmpty())) {
                lore.add(this.color(player, "&f&lREWARDS"));
                if (coins > 0) {
                    lore.add(this.color(player, " &8▪ &6" + coins + " Coins"));
                }
                if (boxes != null) {
                    for (String boxId : boxes.getKeys(false)) {
                        int amount = boxes.getInt(boxId);
                        if (amount > 0) {
                            lore.add(this.color(player, " &8▪ &e" + amount + "x " + this.formatBoxNameColored(boxId) + " &7Box"));
                        }
                    }
                }
                lore.add("");
            }
        } else {
            lore.add(this.color(player, "&cNo rewards configured"));
            lore.add("");
        }

        switch (state) {
            case CLAIMED:
                lore.add(this.color(player, "&a&l✔ CLAIMED"));
                break;
            case CURRENT:
                lore.add(this.color(player, "&e&l► READY TO CLAIM"));
                lore.add(this.color(player, "&7Use the &eclaim button below"));
                break;
            case LOCKED:
                if (day == effectiveStreak + 1 && !canClaim) {
                    lore.add(this.color(player, "&6&l⏳ NEXT UP"));
                    String countdown = this.manager.formatCountdown(remaining);
                    lore.add(this.color(player, "&7Unlocks in: &e" + countdown));
                } else if (effectiveStreak > 0) {
                    lore.add(this.color(player, "&7&l🔒 LOCKED"));
                    lore.add(this.color(player, "&7Complete &eDay " + effectiveStreak + " &7first"));
                } else {
                    lore.add(this.color(player, "&7&l🔒 LOCKED"));
                }
                break;
        }

        meta.setLore(lore);
        if (state == DayState.CLAIMED || state == DayState.CURRENT) {
            try {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } catch (Throwable ignored) {}
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createProfileHead(Player player, PlayerStreak streak, String rank,
                                        int nextDay, long coins, boolean broken) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        meta.setOwningPlayer(player);
        meta.setDisplayName(this.color(player, "&e&l✦ &f&l" + player.getName() + " &e&l✦"));

        List<String> lore = new ArrayList<>();
        lore.add(this.color(player, "&7Your daily reward profile."));
        lore.add("");
        lore.add(this.color(player, "&e&lSTATISTIC:"));

        int streakDays = streak.getStreakDays();
        lore.add(this.color(player, " &8▪ &7Streak: &e" + streakDays + " &7day(s)"
                + (broken ? " &c(broken)" : "")));

        int totalClaims = streak.getTotalClaims();
        lore.add(this.color(player, " &8▪ &7Total Claims: &f" + totalClaims));

        lore.add(this.color(player, " &8▪ &7Rank: &b" + rank));
        lore.add("");
        lore.add(this.color(player, "&a&lNEXT:"));
        lore.add(this.color(player, " &8▪ &7Day: &e" + nextDay));

        boolean canClaimNow = this.manager.canClaim(streak);
        lore.add(this.color(player, " &8▪ &7Status: " + (canClaimNow ? "&aReady!" : "&cClaimed today")));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        head.setItemMeta(meta);
        return head;
    }

    private ItemStack createStreakInfo(Player player, PlayerStreak streak, boolean broken) {
        String texture = this.plugin.getConfig().getString("gui.heads.streak.texture", "");
        Material fallback = Material.BLAZE_POWDER;

        int current = streak.getStreakDays();
        String displayName = this.color(player, "&6&l🔥 Daily Streak");

        List<String> lore = new ArrayList<>();
        if (broken && current > 1) {
            lore.add(this.color(player, "&c&l✘ STREAK BROKEN"));
            lore.add(this.color(player, " &8▪ &7Previous: &7&m" + current + "&r &7days"));
            lore.add(this.color(player, " &8▪ &7Next claim resets to &eDay 1"));
            lore.add("");
            lore.add(this.color(player, "&7Claim now to restart your streak!"));
        } else if (current == 0) {
            lore.add(this.color(player, " &8▪ &7Current: &e0 &7days"));
            lore.add("");
            lore.add(this.color(player, "&7Claim your first daily reward"));
            lore.add(this.color(player, "&7to start a streak!"));
        } else {
            lore.add(this.color(player, " &8▪ &7Current: &e" + current + " &7day(s)"));
            lore.add(this.color(player, " &8▪ &7Next: &eDay " + (current + 1)));
            lore.add("");
            lore.add(this.color(player, "&7Keep claiming every &e24 hours"));
            lore.add(this.color(player, "&7to build your streak!"));
        }

        return this.createHeadOrMaterial(player, texture, fallback, displayName, lore);
    }

    private ItemStack createRankInfo(Player player, String rank) {
        String texture = this.plugin.getConfig().getString("gui.heads.rank.texture", "");
        Material fallback = Material.GOLDEN_HELMET;

        String displayName = this.color(player, "&b&l👑 " + player.getName() + "'s Rank");
        List<String> lore = new ArrayList<>();
        lore.add(this.color(player, " &8▪ &7Current: &b" + rank + player.getName()));
        lore.add("");
        lore.add(this.color(player, "&7Higher ranks get better"));
        lore.add(this.color(player, "&7lucky vault boxes!"));

        return this.createHeadOrMaterial(player, texture, fallback, displayName, lore);
    }

    private ItemStack createTotalInfo(Player player, PlayerStreak streak) {
        String texture = this.plugin.getConfig().getString("gui.heads.total.texture", "");
        Material fallback = Material.EXPERIENCE_BOTTLE;

        String displayName = this.color(player, "&d&l📊 Total Claims");
        List<String> lore = new ArrayList<>();
        lore.add(this.color(player, " &8▪ &7Total: &f" + streak.getTotalClaims() + " &7time(s)"));
        lore.add("");
        lore.add(this.color(player, "&7Since you started playing!"));

        return this.createHeadOrMaterial(player, texture, fallback, displayName, lore);
    }

    private ItemStack createHeadOrMaterial(Player player, String texture, Material fallback, String name, List<String> lore) {
        if (texture != null && !texture.isEmpty()) {
            try {
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    UUID profileId = UUID.nameUUIDFromBytes(texture.getBytes());
                    PlayerProfile profile = Bukkit.createProfile(profileId);
                    profile.setProperty(new ProfileProperty("textures", texture));
                    meta.setPlayerProfile(profile);
                    meta.setDisplayName(name);
                    meta.setLore(lore);
                    meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
                    head.setItemMeta(meta);
                    return head;
                }
            } catch (Throwable ignored) {}
        }

        ItemStack item = new ItemStack(fallback);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void glow(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            try {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } catch (Throwable ignored) {}
            item.setItemMeta(meta);
        }
    }

    private ItemStack createButton(Player player, Material mat, String name, List<String> lore, String action) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.setDisplayName(this.color(player, name));
        List<String> colored = new ArrayList<>();
        for (String s : lore) {
            colored.add(this.color(player, s));
        }
        meta.setLore(colored);
        meta.getPersistentDataContainer().set(ACTION_KEY, PersistentDataType.STRING, action);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBorder() {
        Material mat = Material.matchMaterial(this.plugin.getConfig().getString("gui.border.material", "BLACK_STAINED_GLASS_PANE"));
        if (mat == null) mat = Material.BLACK_STAINED_GLASS_PANE;

        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(" ");
            item.setItemMeta(m);
        }
        return item;
    }

    private ItemStack createFiller() {
        Material mat = Material.matchMaterial(this.plugin.getConfig().getString("gui.filler.material", "GRAY_STAINED_GLASS_PANE"));
        if (mat == null) mat = Material.GRAY_STAINED_GLASS_PANE;

        ItemStack item = new ItemStack(mat);
        ItemMeta m = item.getItemMeta();
        if (m != null) {
            m.setDisplayName(" ");
            item.setItemMeta(m);
        }
        return item;
    }

    private String getBoxColor(String boxId) {
        if (boxId == null) return "&7";
        switch (boxId.toLowerCase()) {
            case "common":    return "&7";
            case "rare":      return "&9";
            case "epic":      return "&5";
            case "legendary": return "&6";
            case "mythic":    return "&c";
            default:          return "&e";
        }
    }

    private String formatBoxNameColored(String boxId) {
        return this.getBoxColor(boxId) + this.formatBoxName(boxId);
    }

    private String formatBoxName(String boxId) {
        return boxId.substring(0, 1).toUpperCase() + boxId.substring(1).toLowerCase();
    }

    private String formatNumber(long n) {
        if (n >= 1_000_000L) {
            return String.format("%.1fM", (double) n / 1_000_000.0);
        } else if (n >= 1_000L) {
            return String.format("%.1fk", (double) n / 1_000.0);
        }
        return String.valueOf(n);
    }

    private String color(Player player, String text) {
        if (text == null) return "";
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                text = PlaceholderAPI.setPlaceholders(player, text);
            } catch (Throwable ignored) {}
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    private enum DayState {
        CLAIMED,
        CURRENT,
        LOCKED
    }
}