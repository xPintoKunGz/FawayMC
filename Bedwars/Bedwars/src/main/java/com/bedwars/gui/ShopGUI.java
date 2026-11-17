package com.bedwars.gui;

import com.bedwars.BedwarsPlugin;
import com.bedwars.arena.Arena;
import com.bedwars.arena.Team;
import com.bedwars.util.ItemBuilder;
import com.bedwars.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ShopGUI {

    public static final String SHOP_TITLE_PREFIX = "§8✦ §6§lBEDWARS SHOP §8» §f";

    public enum ShopCategory {
        QUICK_BUY("Quick Buy", Material.NETHER_STAR, 9),
        BLOCKS("Blocks", Material.WHITE_WOOL, 10),
        WEAPONS("Weapons", Material.GOLDEN_SWORD, 11),
        ARMOR("Armor", Material.CHAINMAIL_CHESTPLATE, 12),
        TOOLS("Tools", Material.IRON_PICKAXE, 13),
        RANGED("Ranged", Material.BOW, 14),
        POTIONS("Potions", Material.BREWING_STAND, 15),
        FOOD("Food", Material.COOKED_BEEF, 16),
        TACTICS("Tactics", Material.TNT, 17);

        private final String displayName;
        private final Material icon;
        private final int barSlot;

        ShopCategory(String displayName, Material icon, int barSlot) {
            this.displayName = displayName;
            this.icon = icon;
            this.barSlot = barSlot;
        }

        public String getDisplayName() {
            return displayName;
        }

        public Material getIcon() {
            return icon;
        }

        public int getBarSlot() {
            return barSlot;
        }

        public static ShopCategory fromBarSlot(int slot) {
            for (ShopCategory cat : values()) {
                if (cat.barSlot == slot) return cat;
            }
            return null;
        }
    }

    public enum ShopAction {
        GIVE_ITEM,
        SWORD_STONE,
        SWORD_IRON,
        SWORD_DIAMOND,
        SWORD_NETHERITE,
        ARMOR_CHAIN,
        ARMOR_IRON,
        ARMOR_DIAMOND,
        ARMOR_NETHERITE,
        TOOL_SHEARS,
        TOOL_AXE_UPGRADE,
        TOOL_PICKAXE_UPGRADE,
        POPUP_FORTRESS,
        BED_DEFENDER_GOLEM,
        RADAR_TRACKER,
        ELYTRA_EQUIP,
        STICK_KNOCKBACK
    }

    public static class ShopItemDef {
        private final ItemStack itemToGive;
        private final String customName;
        private final List<String> extraLore;
        private final Material currency;
        private final int price;
        private final ShopAction action;

        public ShopItemDef(ItemStack itemToGive, String customName, List<String> extraLore, Material currency, int price, ShopAction action) {
            this.itemToGive = itemToGive;
            this.customName = customName;
            this.extraLore = (extraLore != null) ? extraLore : Collections.emptyList();
            this.currency = currency;
            this.price = price;
            this.action = action;
        }

        public ItemStack getItemToGive() { return itemToGive; }
        public String getCustomName() { return customName; }
        public List<String> getExtraLore() { return extraLore; }
        public Material getCurrency() { return currency; }
        public int getPrice() { return price; }
        public ShopAction getAction() { return action; }
    }

    // Category -> Slot -> ShopItemDef
    private static final Map<ShopCategory, Map<Integer, ShopItemDef>> CATALOG = new EnumMap<>(ShopCategory.class);

    static {
        for (ShopCategory cat : ShopCategory.values()) {
            CATALOG.put(cat, new HashMap<>());
        }
        initCatalog();
    }

    private static void initCatalog() {
        // --- 1. BLOCKS (Slots 19 to 29) ---
        registerItem(ShopCategory.BLOCKS, 19, new ItemStack(Material.WHITE_WOOL, 16),
                "§b✦ Nano Shield Wool x16",
                List.of("§7Team colored reinforced wool blocks", "§7Essential for bridge-building and bed defense"),
                Material.IRON_INGOT, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.BLOCKS, 20, new ItemStack(Material.OAK_PLANKS, 16),
                "§6✦ Reinforced Oak Planks x16",
                List.of("§7Sturdy hardwood planks that prevent bare-hand bed destruction"),
                Material.GOLD_INGOT, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.BLOCKS, 21, new ItemStack(Material.SPRUCE_PLANKS, 16),
                "§e✦ Hardened Spruce Planks x16",
                List.of("§7Dense spruce timber with high axe resistance"),
                Material.GOLD_INGOT, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.BLOCKS, 22, new ItemStack(Material.GLASS, 4),
                "§3✦ Team Blast-Proof Glass x4",
                List.of("§7Team-colored blast-resistant glass", "§a✔ 100% immune to TNT and Fireball explosions!"),
                Material.IRON_INGOT, 12, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.BLOCKS, 23, new ItemStack(Material.SANDSTONE, 16),
                "§e✦ Fortified Sandstone x16",
                List.of("§7Solid sandstone blocks for quick perimeter walls"),
                Material.IRON_INGOT, 12, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.BLOCKS, 24, new ItemStack(Material.CUT_SANDSTONE, 16),
                "§6✦ Cut Sandstone x16",
                List.of("§7Dense cut sandstone blocks, high durability"),
                Material.IRON_INGOT, 16, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.BLOCKS, 25, new ItemStack(Material.END_STONE, 12),
                "§e✦ Starlight End Stone x12",
                List.of("§7Highly blast-resistant lunar stone"),
                Material.IRON_INGOT, 24, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.BLOCKS, 26, new ItemStack(Material.TERRACOTTA, 16),
                "§d✦ Cyber Terracotta x16",
                List.of("§7Heavy ceramic defense blocks"),
                Material.IRON_INGOT, 12, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.BLOCKS, 28, new ItemStack(Material.SLIME_BLOCK, 4),
                "§a✦ Bouncy Slime Block x4",
                List.of("§7High-rebound block that negates fall damage"),
                Material.GOLD_INGOT, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.BLOCKS, 29, new ItemStack(Material.OBSIDIAN, 4),
                "§5✦ Abyssal Obsidian x4",
                List.of("§7Ultimate bed fortification material!", "§7Requires a diamond pickaxe to break"),
                Material.EMERALD, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.BLOCKS, 30, new ItemStack(Material.DIRT, 16),
                "§6✦ Bed Defense Dirt x16",
                List.of("§7Cheap, effective soil for burying beds and quick barricades"),
                Material.IRON_INGOT, 4, ShopAction.GIVE_ITEM);

        // --- 2. WEAPONS (One Sword Only! Replaces previous sword in inventory) ---
        registerItem(ShopCategory.WEAPONS, 20, new ItemBuilder(Material.STONE_SWORD).unbreakable(true).build(),
                "§a🗡 Tactical Stone Sword",
                List.of("§7Fast attack weapon (1.8.9 spam-click combat)", "§e▶ Replaces your current sword automatically"),
                Material.IRON_INGOT, 10, ShopAction.SWORD_STONE);

        registerItem(ShopCategory.WEAPONS, 21, new ItemBuilder(Material.IRON_SWORD).unbreakable(true).build(),
                "§f⚔ Knight's Vanguard Blade",
                List.of("§7High-impact iron blade (1.8.9 spam-click combat)", "§e▶ Replaces your current sword automatically"),
                Material.GOLD_INGOT, 7, ShopAction.SWORD_IRON);

        registerItem(ShopCategory.WEAPONS, 22, new ItemBuilder(Material.DIAMOND_SWORD).unbreakable(true).build(),
                "§b★ Titan Diamond Sword",
                List.of("§7Legendary combat sword for battle dominance", "§e▶ Replaces your current sword automatically"),
                Material.EMERALD, 4, ShopAction.SWORD_DIAMOND);

        registerItem(ShopCategory.WEAPONS, 23, new ItemBuilder(Material.NETHERITE_SWORD).unbreakable(true).build(),
                "§4★ God of War Netherite Blade",
                List.of("§7Maximum lethal damage output in the arena", "§e▶ Replaces your current sword automatically"),
                Material.EMERALD, 7, ShopAction.SWORD_NETHERITE);

        registerItem(ShopCategory.WEAPONS, 24, new ItemBuilder(Material.MACE).unbreakable(true).build(),
                "§c★ Colossal Titan Mace",
                List.of("§7Devastating 1.21 weapon! Deal massive fall-impact crush damage", "§4⚠ Legendary trade - extremely hard to obtain!"),
                Material.EMERALD, 18, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.WEAPONS, 25, new ItemBuilder(Material.STICK).enchant(Enchantment.KNOCKBACK, 1).build(),
                "§d⚡ Kinetic Knockback Stick",
                List.of("§7Knocks foes off bridges (scales with team sharpness, max II)"),
                Material.GOLD_INGOT, 5, ShopAction.STICK_KNOCKBACK);

        registerItem(ShopCategory.WEAPONS, 26, new ItemBuilder(Material.TRIDENT).unbreakable(true).build(),
                "§e⚡ Piercing War Trident",
                List.of("§7Close-quarters armor-piercing polearm"),
                Material.EMERALD, 3, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.WEAPONS, 28, new ItemBuilder(Material.SHIELD).unbreakable(true).build(),
                "§f🛡 Tactical Combat Shield",
                List.of("§7Block incoming melee strikes, arrows, and explosion damage"),
                Material.IRON_INGOT, 20, ShopAction.GIVE_ITEM);

        // --- 3. ARMOR (Permanent upgrades) ---
        registerItem(ShopCategory.ARMOR, 20, new ItemBuilder(Material.CHAINMAIL_BOOTS).unbreakable(true).build(),
                "§f🛡 Scout Chainmail Armor",
                List.of("§7Unlocks permanent chainmail armor (Leggings + Boots)", "§7Persists through respawns!"),
                Material.IRON_INGOT, 40, ShopAction.ARMOR_CHAIN);

        registerItem(ShopCategory.ARMOR, 22, new ItemBuilder(Material.IRON_BOOTS).unbreakable(true).build(),
                "§7🛡 Vanguard Iron Armor",
                List.of("§7Unlocks permanent iron armor (Leggings + Boots)", "§7Significantly mitigates all incoming damage!"),
                Material.GOLD_INGOT, 12, ShopAction.ARMOR_IRON);

        registerItem(ShopCategory.ARMOR, 24, new ItemBuilder(Material.DIAMOND_BOOTS).unbreakable(true).build(),
                "§b🛡 Imperium Diamond Armor",
                List.of("§7Unlocks permanent diamond armor (Leggings + Boots)", "§7Supreme protection for competitive combat!"),
                Material.EMERALD, 6, ShopAction.ARMOR_DIAMOND);

        registerItem(ShopCategory.ARMOR, 25, new ItemBuilder(Material.NETHERITE_BOOTS).unbreakable(true).build(),
                "§d🛡 Abyssal Netherite Armor",
                List.of("§7Unlocks permanent netherite armor (Leggings + Boots)", "§7Unmatched explosion and knockback resistance!"),
                Material.EMERALD, 10, ShopAction.ARMOR_NETHERITE);

        registerItem(ShopCategory.ARMOR, 26, new ItemBuilder(Material.SHIELD).unbreakable(true).build(),
                "§f🛡 Tactical Combat Shield",
                List.of("§7Block incoming melee strikes, arrows, and explosion damage"),
                Material.IRON_INGOT, 20, ShopAction.GIVE_ITEM);

        // --- 4. TOOLS ---
        registerItem(ShopCategory.TOOLS, 19, new ItemBuilder(Material.SHEARS).unbreakable(true).build(),
                "§a✂ Hyper Shears",
                List.of("§7Instantly shears wool. Permanent through deaths!"),
                Material.IRON_INGOT, 20, ShopAction.TOOL_SHEARS);

        registerItem(ShopCategory.TOOLS, 20, new ItemBuilder(Material.WOODEN_AXE).enchant(Enchantment.EFFICIENCY, 1).unbreakable(true).build(),
                "§e🪓 Lumberjack Axe Upgrade",
                List.of("§7Progressive axe upgrade: Wooden ➔ Stone ➔ Iron ➔ Diamond", "§7Permanent through deaths!"),
                Material.IRON_INGOT, 10, ShopAction.TOOL_AXE_UPGRADE);

        registerItem(ShopCategory.TOOLS, 21, new ItemBuilder(Material.WOODEN_PICKAXE).enchant(Enchantment.EFFICIENCY, 1).unbreakable(true).build(),
                "§e⛏ Earth Breaker Pickaxe Upgrade",
                List.of("§7Progressive pickaxe upgrade: Wooden ➔ Stone ➔ Iron ➔ Diamond", "§7Permanent through deaths!"),
                Material.IRON_INGOT, 10, ShopAction.TOOL_PICKAXE_UPGRADE);

        registerItem(ShopCategory.TOOLS, 22, new ItemBuilder(Material.SHIELD).unbreakable(true).build(),
                "§f🛡 Tactical Combat Shield",
                List.of("§7Block incoming melee strikes, arrows, and explosion damage"),
                Material.IRON_INGOT, 20, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TOOLS, 23, new ItemBuilder(Material.IRON_SHOVEL).enchant(Enchantment.EFFICIENCY, 2).unbreakable(true).build(),
                "§e⛏ Iron Shovel",
                List.of("§7Efficiency II - Quickly excavates terracotta and sandstone"),
                Material.IRON_INGOT, 5, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TOOLS, 24, new ItemBuilder(Material.FISHING_ROD).unbreakable(true).build(),
                "§b🎣 Tactical Fishing Rod",
                List.of("§7Hook foes to interrupt sprint combos and disrupt aim"),
                Material.GOLD_INGOT, 4, ShopAction.GIVE_ITEM);

        // --- 5. RANGED ---
        registerItem(ShopCategory.RANGED, 20, new ItemBuilder(Material.BOW).unbreakable(true).build(),
                "§e🏹 Stalker Bow",
                List.of("§7High-precision ranged bow"),
                Material.GOLD_INGOT, 16, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.RANGED, 21, new ItemBuilder(Material.BOW).enchant(Enchantment.POWER, 1).unbreakable(true).build(),
                "§b⚡ Thunderstrike Bow (Power I)",
                List.of("§7Electrified arrows with increased impact damage"),
                Material.GOLD_INGOT, 24, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.RANGED, 22, new ItemBuilder(Material.BOW).enchant(Enchantment.PUNCH, 1).unbreakable(true).build(),
                "§d💨 Sonic Shockwave Bow (Punch I)",
                List.of("§7Massive knockback! Propels enemies straight into the void"),
                Material.EMERALD, 8, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.RANGED, 23, new ItemBuilder(Material.CROSSBOW).enchant(Enchantment.QUICK_CHARGE, 2).unbreakable(true).build(),
                "§c🎯 Rapid-Fire Crossbow",
                List.of("§7Quick Charge II - High velocity firing speed"),
                Material.GOLD_INGOT, 20, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.RANGED, 24, new ItemStack(Material.ARROW, 8),
                "§f🎯 Carbon Arrows x8",
                List.of("§7Ammunition for bows and crossbows"),
                Material.GOLD_INGOT, 2, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.RANGED, 25, new ItemStack(Material.ARROW, 16),
                "§f🎯 Carbon Arrows x16",
                List.of("§7Large ammunition bundle"),
                Material.GOLD_INGOT, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.RANGED, 26, new ItemStack(Material.SNOWBALL, 16),
                "§f❄ Blizzard Snowballs x16",
                List.of("§7Rapid-fire throwables that disrupt sprint and knock foes off edges"),
                Material.IRON_INGOT, 24, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.RANGED, 28, new ItemStack(Material.WIND_CHARGE, 5),
                "§b💨 Cyclone Wind Charge x5",
                List.of("§71.21 wind burst that launches players upward and repels foes"),
                Material.EMERALD, 2, ShopAction.GIVE_ITEM);

        // --- 6. POTIONS ---
        ItemStack splashHeal = new ItemBuilder(Material.SPLASH_POTION)
                .potion(new PotionEffect(PotionEffectType.INSTANT_HEALTH, 1, 1), Color.RED).build();
        registerItem(ShopCategory.POTIONS, 19, splashHeal,
                "§c🧪 Splash Instant Health II",
                List.of("§7Splash on yourself or allies to instantly heal 4 hearts!"),
                Material.EMERALD, 1, ShopAction.GIVE_ITEM);

        ItemStack splashBlind = new ItemBuilder(Material.SPLASH_POTION)
                .potion(new PotionEffect(PotionEffectType.SLOWNESS, 160, 1), Color.BLACK).build();
        registerItem(ShopCategory.POTIONS, 20, splashBlind,
                "§8🧪 Splash Slowness II (8s)",
                List.of("§7Slows down approaching enemy rushers"),
                Material.EMERALD, 1, ShopAction.GIVE_ITEM);

        ItemStack speedPot = new ItemBuilder(Material.POTION)
                .potion(new PotionEffect(PotionEffectType.SPEED, 900, 1), Color.AQUA).build();
        registerItem(ShopCategory.POTIONS, 21, speedPot,
                "§b🧪 Potion of Speed II (45s)",
                List.of("§7Massive sprint speed boost"),
                Material.EMERALD, 1, ShopAction.GIVE_ITEM);

        ItemStack jumpPot = new ItemBuilder(Material.POTION)
                .potion(new PotionEffect(PotionEffectType.JUMP_BOOST, 900, 4), Color.LIME).build();
        registerItem(ShopCategory.POTIONS, 22, jumpPot,
                "§a🧪 Potion of Jump Boost V (45s)",
                List.of("§7Leap over defensive perimeter walls with ease"),
                Material.EMERALD, 1, ShopAction.GIVE_ITEM);

        ItemStack invisPot = new ItemBuilder(Material.POTION)
                .potion(new PotionEffect(PotionEffectType.INVISIBILITY, 600, 0), Color.GRAY).build();
        registerItem(ShopCategory.POTIONS, 23, invisPot,
                "§8🧪 Potion of Invisibility (30s)",
                List.of("§7Become fully invisible for covert infiltration"),
                Material.EMERALD, 2, ShopAction.GIVE_ITEM);

        ItemStack strPot = new ItemBuilder(Material.POTION)
                .potion(new PotionEffect(PotionEffectType.STRENGTH, 400, 0), Color.MAROON).build();
        registerItem(ShopCategory.POTIONS, 24, strPot,
                "§4🧪 Potion of Strength I (20s)",
                List.of("§7Empowers melee damage"),
                Material.EMERALD, 2, ShopAction.GIVE_ITEM);

        ItemStack regenPot = new ItemBuilder(Material.POTION)
                .potion(new PotionEffect(PotionEffectType.REGENERATION, 500, 1), Color.FUCHSIA).build();
        registerItem(ShopCategory.POTIONS, 25, regenPot,
                "§d🧪 Potion of Regeneration II (25s)",
                List.of("§7Rapidly restores health over time"),
                Material.EMERALD, 1, ShopAction.GIVE_ITEM);

        // --- 7. FOOD (NEW CATEGORY requested by user!) ---
        registerItem(ShopCategory.FOOD, 19, new ItemStack(Material.BREAD, 4),
                "§e🍞 Fresh Baked Bread x4",
                List.of("§7Nutritious baked bread", "§a+ Eating restores hunger and heals +2 hearts!"),
                Material.IRON_INGOT, 2, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.FOOD, 20, new ItemStack(Material.COOKED_PORKCHOP, 4),
                "§6🥓 Roasted Porkchop x4",
                List.of("§7Savory roasted pork", "§a+ Eating restores hunger and heals +2 hearts!"),
                Material.IRON_INGOT, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.FOOD, 21, new ItemStack(Material.COOKED_CHICKEN, 4),
                "§f🍗 Crisp Roast Chicken x4",
                List.of("§7Crispy roasted chicken", "§a+ Eating restores hunger and heals +2 hearts!"),
                Material.IRON_INGOT, 3, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.FOOD, 22, new ItemStack(Material.GOLDEN_CARROT, 4),
                "§6🥕 Golden Carrot x4",
                List.of("§7Highest saturation food in Minecraft", "§a+ Eating restores hunger and heals +2 hearts!"),
                Material.GOLD_INGOT, 3, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.FOOD, 23, new ItemStack(Material.COOKED_BEEF, 4),
                "§c🥩 Prime Grilled Steak x4",
                List.of("§7High-protein steak for enduring combat", "§a+ Eating restores hunger and heals +2 hearts!"),
                Material.IRON_INGOT, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.FOOD, 24, new ItemStack(Material.SWEET_BERRIES, 16),
                "§d🍇 Mountain Sweet Berries x16",
                List.of("§7Quick snack for fast consumption", "§a+ Eating heals +2 hearts each!"),
                Material.IRON_INGOT, 2, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.FOOD, 25, new ItemStack(Material.GOLDEN_APPLE),
                "§e🍏 Golden Apple",
                List.of("§7Grants Absorption and instant health bonus", "§a+ Eating heals +4 hearts!"),
                Material.GOLD_INGOT, 3, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.FOOD, 26, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE),
                "§6🍏 God Apple (Enchanted Golden Apple)",
                List.of("§7Supreme mythic apple! High tier defense and regen", "§a+ Eating heals +7 hearts!"),
                Material.EMERALD, 6, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.FOOD, 28, new ItemStack(Material.CAKE),
                "§f🎂 Victory Cake",
                List.of("§7Placeable feast cake for the whole squad!"),
                Material.IRON_INGOT, 6, ShopAction.GIVE_ITEM);

        // --- 8. TACTICS ---
        ItemStack fw1 = new ItemStack(Material.FIREWORK_ROCKET, 3);
        org.bukkit.inventory.meta.FireworkMeta fm1 = (org.bukkit.inventory.meta.FireworkMeta) fw1.getItemMeta();
        if (fm1 != null) {
            fm1.setPower(1);
            fw1.setItemMeta(fm1);
        }

        ItemStack fw2 = new ItemStack(Material.FIREWORK_ROCKET, 3);
        org.bukkit.inventory.meta.FireworkMeta fm2 = (org.bukkit.inventory.meta.FireworkMeta) fw2.getItemMeta();
        if (fm2 != null) {
            fm2.setPower(2);
            fm2.addEffect(org.bukkit.FireworkEffect.builder().withColor(Color.YELLOW).withFade(Color.ORANGE).with(org.bukkit.FireworkEffect.Type.STAR).build());
            fw2.setItemMeta(fm2);
        }

        ItemStack fw3 = new ItemStack(Material.FIREWORK_ROCKET, 3);
        org.bukkit.inventory.meta.FireworkMeta fm3 = (org.bukkit.inventory.meta.FireworkMeta) fw3.getItemMeta();
        if (fm3 != null) {
            fm3.setPower(3);
            fm3.addEffect(org.bukkit.FireworkEffect.builder().withColor(Color.RED).withFade(Color.PURPLE).with(org.bukkit.FireworkEffect.Type.BALL_LARGE).trail(true).flicker(true).build());
            fw3.setItemMeta(fm3);
        }

        registerItem(ShopCategory.TACTICS, 19, new ItemStack(Material.GOLDEN_APPLE),
                "§e🍏 Golden Apple",
                List.of("§7Emergency combat food! Grants Absorption and heals +4 HP"),
                Material.GOLD_INGOT, 3, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 20, new ItemStack(Material.FIRE_CHARGE),
                "§6🔥 Plasma Fireball",
                List.of("§7Right-click to launch! Knocks foes back and destroys player blocks", "§a✔ 100% safe: never destroys arena map blocks"),
                Material.IRON_INGOT, 40, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 21, new ItemStack(Material.TNT),
                "§c💣 C4 Timed TNT",
                List.of("§7High-yield explosive with auto-ignite 2s fuse", "§a✔ Destroys player placed blocks only!"),
                Material.GOLD_INGOT, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 22, new ItemStack(Material.ENDER_PEARL),
                "§3🌀 Quantum Warp Pearl",
                List.of("§7Instantly teleports forward across gaps"),
                Material.EMERALD, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 23, new ItemStack(Material.EGG),
                "§b🌈 Bifrost Bridge Egg",
                List.of("§7Throws an egg that generates a team-wool bridge in mid-air!"),
                Material.EMERALD, 1, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 24, new ItemStack(Material.PACKED_ICE),
                "§b🛡 Tactical Ice Barrier Wall (Non-Melting)",
                List.of("§7Deploys an instant 5x4 barrier wall of permanent non-melting ice in front of you!"),
                Material.IRON_INGOT, 24, ShopAction.POPUP_FORTRESS);

        registerItem(ShopCategory.TACTICS, 25, new ItemStack(Material.IRON_BLOCK),
                "§e🤖 Bed Defender Golem (2 Mins)",
                List.of("§7Summons a defensive guardian golem (lives for 120s, protects allies)"),
                Material.IRON_INGOT, 120, ShopAction.BED_DEFENDER_GOLEM);

        registerItem(ShopCategory.TACTICS, 26, new ItemStack(Material.HOGLIN_SPAWN_EGG),
                "§6🐗 Hoglin War Beast",
                List.of("§7Summons a ferocious assault beast (never attacks allies)"),
                Material.EMERALD, 6, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 28, new ItemStack(Material.RAVAGER_SPAWN_EGG),
                "§c🦏 Ravager Siege Titan",
                List.of("§7Summons a colossal armored siege beast (never attacks allies)"),
                Material.EMERALD, 12, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 29, new ItemStack(Material.ENCHANTED_GOLDEN_APPLE),
                "§6🍏 God Apple",
                List.of("§7Restores +14 HP and grants maximum absorption", "§a▶ Requires Emeralds to trade"),
                Material.EMERALD, 6, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 30, new ItemStack(Material.ELYTRA),
                "§d★ Celestial Dragon Elytra",
                List.of("§7Equips directly to your chestplate to glide!", "§c⚠ Fragile: Breaks quickly after flight!", "§4⚠ Legendary rarity - 16 Emeralds!"),
                Material.EMERALD, 16, ShopAction.ELYTRA_EQUIP);

        registerItem(ShopCategory.TACTICS, 31, fw1,
                "§e🎆 Tier 1 Flight Rocket x3",
                List.of("§7Smooth firework boost for Elytra glide"),
                Material.GOLD_INGOT, 1, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 32, fw2,
                "§6🎆 Tier 2 Starlight Rocket x3",
                List.of("§7Golden starburst firework with longer boost duration"),
                Material.GOLD_INGOT, 3, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 33, fw3,
                "§c🎆 Tier 3 Dragon Flame Rocket x3",
                List.of("§7Massive burst firework with maximum boost height!"),
                Material.EMERALD, 1, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 34, new ItemBuilder(Material.FLINT_AND_STEEL).build(),
                "§c🔥 Tactical Flint & Steel",
                List.of("§7Ignite fire to burn obstacles or deter pursuers"),
                Material.GOLD_INGOT, 3, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.TACTICS, 35, new ItemStack(Material.COMPASS),
                "§d📡 Eagle Eye Radar Tracker",
                List.of("§7Right-click to scan and locate the nearest enemy"),
                Material.EMERALD, 2, ShopAction.RADAR_TRACKER);

        registerItem(ShopCategory.TACTICS, 37, new ItemStack(Material.WATER_BUCKET),
                "§9💧 Tactical Water Bucket",
                List.of("§7Cushion falls and absorb explosion damage (cleared on reset)"),
                Material.GOLD_INGOT, 3, ShopAction.GIVE_ITEM);

        // --- 0. QUICK BUY ---
        registerItem(ShopCategory.QUICK_BUY, 19, new ItemStack(Material.WHITE_WOOL, 16),
                "§b✦ Nano Shield Wool x16",
                List.of("§7Team colored building blocks"),
                Material.IRON_INGOT, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.QUICK_BUY, 20, new ItemStack(Material.OAK_PLANKS, 16),
                "§6✦ Reinforced Oak Planks x16",
                List.of("§7Sturdy hardwood defense blocks"),
                Material.GOLD_INGOT, 4, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.QUICK_BUY, 21, new ItemBuilder(Material.STONE_SWORD).unbreakable(true).build(),
                "§a🗡 Tactical Stone Sword",
                List.of("§7Fast attack weapon (1.8.9 PvP)", "§e▶ Replaces your current sword automatically"),
                Material.IRON_INGOT, 10, ShopAction.SWORD_STONE);

        registerItem(ShopCategory.QUICK_BUY, 22, new ItemBuilder(Material.IRON_SWORD).unbreakable(true).build(),
                "§f⚔ Knight's Vanguard Blade",
                List.of("§7High damage combat blade", "§e▶ Replaces your current sword automatically"),
                Material.GOLD_INGOT, 7, ShopAction.SWORD_IRON);

        registerItem(ShopCategory.QUICK_BUY, 23, new ItemBuilder(Material.CHAINMAIL_BOOTS).unbreakable(true).build(),
                "§f🛡 Scout Chainmail Armor",
                List.of("§7Permanent armor upgrade"),
                Material.IRON_INGOT, 40, ShopAction.ARMOR_CHAIN);

        registerItem(ShopCategory.QUICK_BUY, 24, new ItemBuilder(Material.IRON_BOOTS).unbreakable(true).build(),
                "§7🛡 Vanguard Iron Armor",
                List.of("§7Permanent armor upgrade"),
                Material.GOLD_INGOT, 12, ShopAction.ARMOR_IRON);

        registerItem(ShopCategory.QUICK_BUY, 25, new ItemBuilder(Material.SHEARS).unbreakable(true).build(),
                "§a✂ Hyper Shears",
                List.of("§7Permanent wool-cutting tool!"),
                Material.IRON_INGOT, 20, ShopAction.TOOL_SHEARS);

        registerItem(ShopCategory.QUICK_BUY, 26, new ItemBuilder(Material.SHIELD).unbreakable(true).build(),
                "§f🛡 Tactical Combat Shield",
                List.of("§7Block incoming strikes and projectiles"),
                Material.IRON_INGOT, 20, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.QUICK_BUY, 28, new ItemBuilder(Material.WOODEN_PICKAXE).enchant(Enchantment.EFFICIENCY, 1).unbreakable(true).build(),
                "§e⛏ Pickaxe Upgrade",
                List.of("§7Progressive pickaxe upgrade", "§7Permanent through deaths!"),
                Material.IRON_INGOT, 10, ShopAction.TOOL_PICKAXE_UPGRADE);

        registerItem(ShopCategory.QUICK_BUY, 29, new ItemStack(Material.GOLDEN_APPLE),
                "§e🍏 Golden Apple",
                List.of("§7Restores hunger and heals +4 HP"),
                Material.GOLD_INGOT, 3, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.QUICK_BUY, 30, new ItemStack(Material.FIRE_CHARGE),
                "§6🔥 Plasma Fireball",
                List.of("§7Destroys enemy blocks without damaging map"),
                Material.IRON_INGOT, 40, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.QUICK_BUY, 31, new ItemStack(Material.TNT),
                "§c💣 C4 Timed TNT",
                List.of("§7High explosive with 2s fuse"),
                Material.GOLD_INGOT, 5, ShopAction.GIVE_ITEM);

        registerItem(ShopCategory.QUICK_BUY, 32, new ItemStack(Material.IRON_BLOCK),
                "§e🤖 Iron Golem Bed Defender",
                List.of("§7Base guardian golem"),
                Material.IRON_INGOT, 120, ShopAction.BED_DEFENDER_GOLEM);

        registerItem(ShopCategory.QUICK_BUY, 33, new ItemStack(Material.COMPASS),
                "§d📡 Eagle Eye Radar Tracker",
                List.of("§7Tracks nearest enemy"),
                Material.EMERALD, 2, ShopAction.RADAR_TRACKER);

        registerItem(ShopCategory.QUICK_BUY, 34, new ItemStack(Material.WATER_BUCKET),
                "§9💧 Tactical Water Bucket",
                List.of("§7Water bucket for MLG and defense"),
                Material.GOLD_INGOT, 3, ShopAction.GIVE_ITEM);
    }

    private static void registerItem(ShopCategory cat, int slot, ItemStack item, String name, List<String> lore, Material cur, int price, ShopAction act) {
        registerItem(null, cat, slot, item, name, lore, cur, price, act);
    }

    private static void registerItem(String configKey, ShopCategory cat, int slot, ItemStack item, String name, List<String> lore, Material cur, int price, ShopAction act) {
        if (BedwarsPlugin.getInstance() != null) {
            org.bukkit.configuration.file.FileConfiguration cfg = BedwarsPlugin.getInstance().getConfig();
            if (cfg != null) {
                String key = configKey;
                if (key == null || key.isEmpty()) {
                    key = item.getType().name().toLowerCase().replace("_", "-");
                }
                // Check direct key or stripped key
                String foundPath = null;
                if (cfg.contains("shop.prices." + key)) {
                    foundPath = "shop.prices." + key;
                } else if (key.endsWith("-wool") && cfg.contains("shop.prices.wool")) {
                    foundPath = "shop.prices.wool";
                } else if (key.equals("fire-charge") && cfg.contains("shop.prices.fireball")) {
                    foundPath = "shop.prices.fireball";
                }

                if (foundPath != null) {
                    price = cfg.getInt(foundPath + ".price", price);
                    String curStr = cfg.getString(foundPath + ".currency");
                    if (curStr != null && !curStr.isEmpty()) {
                        try {
                            cur = Material.valueOf(curStr.toUpperCase());
                        } catch (IllegalArgumentException ignored) {}
                    }
                    int amt = cfg.getInt(foundPath + ".amount", item.getAmount());
                    if (amt > 0 && amt != item.getAmount()) {
                        item = item.clone();
                        item.setAmount(amt);
                    }
                }
            }
        }
        CATALOG.get(cat).put(slot, new ShopItemDef(item, name, lore, cur, price, act));
    }

    public static void reloadCatalog() {
        for (ShopCategory cat : ShopCategory.values()) {
            if (CATALOG.containsKey(cat)) {
                CATALOG.get(cat).clear();
            }
        }
        initCatalog();
    }

    public static ShopItemDef getItem(ShopCategory cat, int slot) {
        if (cat == null) return null;
        Map<Integer, ShopItemDef> map = CATALOG.get(cat);
        return (map != null) ? map.get(slot) : null;
    }

    public static class DynamicToolInfo {
        public ItemStack icon;
        public String name;
        public Material currency;
        public int basePrice;
        public boolean maxed;
        public List<String> lore;
    }

    public static DynamicToolInfo getDynamicPickaxeInfo(Player player, Arena arena) {
        int curTier = (arena != null) ? arena.getPlayerPickaxeTier(player.getUniqueId()) : 0;
        DynamicToolInfo info = new DynamicToolInfo();
        if (curTier >= 4) {
            info.icon = new ItemStack(Material.DIAMOND_PICKAXE);
            info.name = "§b⛏ Cosmic Drill Pickaxe (MAXED)";
            info.currency = Material.IRON_INGOT;
            info.basePrice = 0;
            info.maxed = true;
            info.lore = List.of("§7Tier 4 Diamond Pickaxe (Efficiency III)", "§a✔ Maximum upgrade reached!");
        } else if (curTier == 0) {
            info.icon = new ItemStack(Material.WOODEN_PICKAXE);
            info.name = "§e⛏ Earth Breaker Wooden Pickaxe I";
            info.currency = Material.IRON_INGOT;
            info.basePrice = 10;
            info.maxed = false;
            info.lore = List.of("§7Tier 1 Pickaxe - Efficiency I", "§7Permanent tool progression!");
        } else if (curTier == 1) {
            info.icon = new ItemStack(Material.STONE_PICKAXE);
            info.name = "§a⛏ Tactical Stone Pickaxe II";
            info.currency = Material.IRON_INGOT;
            info.basePrice = 10;
            info.maxed = false;
            info.lore = List.of("§7Tier 2 Pickaxe - Efficiency I", "§7Upgrades your pickaxe to Stone!");
        } else if (curTier == 2) {
            info.icon = new ItemStack(Material.IRON_PICKAXE);
            info.name = "§f⛏ Steel Drill Iron Pickaxe III";
            info.currency = Material.GOLD_INGOT;
            info.basePrice = 3;
            info.maxed = false;
            info.lore = List.of("§7Tier 3 Pickaxe - Efficiency II", "§7Upgrades your pickaxe to Iron!");
        } else {
            info.icon = new ItemStack(Material.DIAMOND_PICKAXE);
            info.name = "§b⛏ Cosmic Drill Diamond Pickaxe IV";
            info.currency = Material.EMERALD;
            info.basePrice = 3;
            info.maxed = false;
            info.lore = List.of("§7Tier 4 Pickaxe - Efficiency III", "§7Mines obsidian rapidly! Upgrades to Diamond!");
        }
        return info;
    }

    public static DynamicToolInfo getDynamicAxeInfo(Player player, Arena arena) {
        int curTier = (arena != null) ? arena.getPlayerAxeTier(player.getUniqueId()) : 0;
        DynamicToolInfo info = new DynamicToolInfo();
        if (curTier >= 4) {
            info.icon = new ItemStack(Material.DIAMOND_AXE);
            info.name = "§b🪓 Colossal Diamond Waraxe (MAXED)";
            info.currency = Material.IRON_INGOT;
            info.basePrice = 0;
            info.maxed = true;
            info.lore = List.of("§7Tier 4 Diamond Axe (Efficiency III)", "§a✔ Maximum upgrade reached!");
        } else if (curTier == 0) {
            info.icon = new ItemStack(Material.WOODEN_AXE);
            info.name = "§e🪓 Lumberjack Wooden Axe I";
            info.currency = Material.IRON_INGOT;
            info.basePrice = 10;
            info.maxed = false;
            info.lore = List.of("§7Tier 1 Axe - Efficiency I", "§7Permanent tool progression!");
        } else if (curTier == 1) {
            info.icon = new ItemStack(Material.STONE_AXE);
            info.name = "§a🪓 Tactical Stone Axe II";
            info.currency = Material.IRON_INGOT;
            info.basePrice = 10;
            info.maxed = false;
            info.lore = List.of("§7Tier 2 Axe - Efficiency I", "§7Upgrades your axe to Stone!");
        } else if (curTier == 2) {
            info.icon = new ItemStack(Material.IRON_AXE);
            info.name = "§6🪓 Berserker Battleaxe III";
            info.currency = Material.GOLD_INGOT;
            info.basePrice = 3;
            info.maxed = false;
            info.lore = List.of("§7Tier 3 Axe - Efficiency II", "§7Upgrades your axe to Iron!");
        } else {
            info.icon = new ItemStack(Material.DIAMOND_AXE);
            info.name = "§b🪓 Colossal Diamond Waraxe IV";
            info.currency = Material.EMERALD;
            info.basePrice = 2;
            info.maxed = false;
            info.lore = List.of("§7Tier 4 Axe - Efficiency III", "§7Demolishes wood instantly! Upgrades to Diamond!");
        }
        return info;
    }

    public static void open(Player player, ShopCategory category) {
        if (category == null) category = ShopCategory.QUICK_BUY;

        Inventory inv = Bukkit.createInventory(null, 54, SHOP_TITLE_PREFIX + category.getDisplayName());
        Arena arena = BedwarsPlugin.getInstance().getArenaManager().getArena(player);
        Team team = (arena != null) ? arena.getPlayerTeam(player.getUniqueId()) : null;

        int iron = countItem(player, Material.IRON_INGOT);
        int gold = countItem(player, Material.GOLD_INGOT);
        int diamond = countItem(player, Material.DIAMOND);
        int emerald = countItem(player, Material.EMERALD);

        // Row 0 (0-8): Wallet and Status Header
        ItemStack divider = new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name(" ").build();
        inv.setItem(0, divider);
        inv.setItem(1, new ItemBuilder(Material.IRON_INGOT).name("§f§lIron: §a" + iron).build());
        inv.setItem(2, divider);
        inv.setItem(3, new ItemBuilder(Material.GOLD_INGOT).name("§6§lGold: §a" + gold).build());
        inv.setItem(4, divider);
        inv.setItem(5, new ItemBuilder(Material.DIAMOND).name("§b§lDiamond: §a" + diamond).build());
        inv.setItem(6, divider);
        inv.setItem(7, new ItemBuilder(Material.EMERALD).name("§a§lEmerald: §a" + emerald).build());
        inv.setItem(8, new ItemBuilder(Material.NETHER_STAR)
                .name("§b§l✦ Team Upgrades")
                .lore("§7Click to open Team Upgrades menu")
                .build());

        // Row 1 (9-17): Category Bar (Strictly row 1, NEVER overlaps items!)
        for (ShopCategory cat : ShopCategory.values()) {
            boolean selected = (cat == category);
            ItemStack catItem = new ItemBuilder(cat.getIcon())
                    .name((selected ? "§a§l▶ §e" : "§7") + cat.getDisplayName())
                    .lore(selected ? "§aCurrently viewing this category" : "§eClick to view category")
                    .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                    .build();
            inv.setItem(cat.getBarSlot(), catItem);
        }

        // Rows 2-5 (18-53): Items Grid
        Map<Integer, ShopItemDef> items = CATALOG.get(category);
        if (items != null) {
            Material teamWool = (team != null) ? team.getColor().getWoolMaterial() : Material.WHITE_WOOL;

            for (Map.Entry<Integer, ShopItemDef> entry : items.entrySet()) {
                int slot = entry.getKey();
                ShopItemDef def = entry.getValue();

                boolean isPickUpgrade = def.getAction() == ShopAction.TOOL_PICKAXE_UPGRADE;
                boolean isAxeUpgrade = def.getAction() == ShopAction.TOOL_AXE_UPGRADE;
                DynamicToolInfo dynamicInfo = null;
                if (isPickUpgrade) dynamicInfo = getDynamicPickaxeInfo(player, arena);
                else if (isAxeUpgrade) dynamicInfo = getDynamicAxeInfo(player, arena);

                Material currency = (dynamicInfo != null) ? dynamicInfo.currency : def.getCurrency();
                int basePrice = (dynamicInfo != null) ? dynamicInfo.basePrice : def.getPrice();
                String customName = (dynamicInfo != null) ? dynamicInfo.name : def.getCustomName();
                List<String> extraLore = (dynamicInfo != null) ? dynamicInfo.lore : def.getExtraLore();

                int curAmount = switch (currency) {
                    case IRON_INGOT -> iron;
                    case GOLD_INGOT -> gold;
                    case DIAMOND -> diamond;
                    default -> emerald;
                };

                String curName = switch (currency) {
                    case IRON_INGOT -> "Iron";
                    case GOLD_INGOT -> "Gold";
                    case DIAMOND -> "Diamond";
                    default -> "Emerald";
                };

                int finalPrice = getScaledPrice(basePrice, arena);
                boolean canAfford = curAmount >= finalPrice;
                int diff = finalPrice - curAmount;

                boolean alreadyOwned = (dynamicInfo != null) ? dynamicInfo.maxed : isAlreadyOwnedOrHigher(player, arena, def);

                String economyTag = "";
                if (basePrice > 0) {
                    if (finalPrice > basePrice) economyTag = " §c(+" + (finalPrice - basePrice) + " large match surcharge)";
                    else if (finalPrice < basePrice) economyTag = " §a(-" + (basePrice - finalPrice) + " small match discount)";
                }

                List<String> lore = new ArrayList<>();
                for (String extra : extraLore) {
                    lore.add(extra);
                }
                lore.add("§8────────────────────");
                if (alreadyOwned) {
                    lore.add("§a✔ Already Owned (Maxed)!");
                } else {
                    lore.add("§7Cost: §e" + finalPrice + " " + curName + economyTag);
                    lore.add("§8────────────────────");
                    if (canAfford) {
                        lore.add("§a✔ Can Afford! Click to purchase");
                    } else {
                        lore.add("§c✖ Insufficient funds! (Need " + diff + " " + curName + ")");
                    }
                }

                ItemStack display = (dynamicInfo != null) ? dynamicInfo.icon.clone() : def.getItemToGive().clone();
                if (display.getType().name().endsWith("_WOOL") && team != null) {
                    display.setType(team.getColor().getWoolMaterial());
                }
                if ((display.getType() == Material.GLASS || display.getType().name().endsWith("_STAINED_GLASS")) && team != null) {
                    display.setType(team.getColor().getGlassMaterial());
                }

                ItemStack finalItem = new ItemBuilder(display)
                        .name(customName)
                        .lore(lore)
                        .flags(org.bukkit.inventory.ItemFlag.HIDE_ATTRIBUTES)
                        .build();

                inv.setItem(slot, finalItem);
            }
        }

        player.openInventory(inv);
        MessageUtil.playSound(player, Sound.UI_BUTTON_CLICK, 0.8f, 1.2f);
    }

    public static int getScaledPrice(int basePrice, Arena arena) {
        if (arena == null) return Math.max(1, basePrice);
        int players = arena.getPlayers().size();
        if (players >= 12) {
            return Math.max(1, (int) Math.ceil(basePrice * 1.30));  // 30% surcharge for 12+ players
        } else if (players >= 8) {
            return Math.max(1, (int) Math.ceil(basePrice * 1.20));  // 20% surcharge for 8-11 players
        } else if (players <= 2) {
            return Math.max(1, (int) Math.round(basePrice * 0.70)); // 30% discount for 1-2 players
        } else if (players <= 4) {
            return Math.max(1, (int) Math.round(basePrice * 0.80)); // 20% discount for 3-4 players
        }
        return Math.max(1, basePrice);
    }

    public static int getPickaxeTier(Material mat) {
        if (mat == null) return 0;
        return switch (mat) {
            case WOODEN_PICKAXE -> 1;
            case STONE_PICKAXE -> 2;
            case IRON_PICKAXE -> 3;
            case DIAMOND_PICKAXE -> 4;
            case NETHERITE_PICKAXE -> 5;
            default -> 0;
        };
    }

    public static int getAxeTier(Material mat) {
        if (mat == null) return 0;
        return switch (mat) {
            case WOODEN_AXE -> 1;
            case STONE_AXE -> 2;
            case IRON_AXE -> 3;
            case DIAMOND_AXE -> 4;
            case NETHERITE_AXE -> 5;
            default -> 0;
        };
    }

    public static int getSwordTier(Material mat) {
        if (mat == null) return 0;
        return switch (mat) {
            case WOODEN_SWORD -> 1;
            case STONE_SWORD -> 2;
            case IRON_SWORD -> 3;
            case DIAMOND_SWORD -> 4;
            case NETHERITE_SWORD -> 5;
            default -> 0;
        };
    }

    public static int getPlayerSwordTier(Player player) {
        int max = 0;
        for (ItemStack it : player.getInventory().getContents()) {
            if (it != null) {
                int t = getSwordTier(it.getType());
                if (t > max) max = t;
            }
        }
        return max;
    }

    public static boolean isAlreadyOwnedOrHigher(Player player, Arena arena, ShopItemDef def) {
        if (player == null || def == null) return false;
        ItemStack item = def.getItemToGive();
        Material mat = (item != null) ? item.getType() : null;

        // Pickaxes
        if (mat != null && getPickaxeTier(mat) > 0) {
            int targetTier = getPickaxeTier(mat);
            int curTier = (arena != null) ? arena.getPlayerPickaxeTier(player.getUniqueId()) : 0;
            for (ItemStack it : player.getInventory().getContents()) {
                if (it != null && getPickaxeTier(it.getType()) > curTier) curTier = getPickaxeTier(it.getType());
            }
            return curTier >= targetTier;
        }

        // Axes
        if (mat != null && getAxeTier(mat) > 0) {
            int targetTier = getAxeTier(mat);
            int curTier = (arena != null) ? arena.getPlayerAxeTier(player.getUniqueId()) : 0;
            for (ItemStack it : player.getInventory().getContents()) {
                if (it != null && getAxeTier(it.getType()) > curTier) curTier = getAxeTier(it.getType());
            }
            return curTier >= targetTier;
        }

        // Shears
        if (def.getAction() == ShopAction.TOOL_SHEARS || (mat != null && mat == Material.SHEARS)) {
            return (arena != null && arena.hasShears(player.getUniqueId())) || player.getInventory().contains(Material.SHEARS);
        }

        // Swords
        if (def.getAction() == ShopAction.SWORD_STONE && getPlayerSwordTier(player) >= 2) return true;
        if (def.getAction() == ShopAction.SWORD_IRON && getPlayerSwordTier(player) >= 3) return true;
        if (def.getAction() == ShopAction.SWORD_DIAMOND && getPlayerSwordTier(player) >= 4) return true;
        if (def.getAction() == ShopAction.SWORD_NETHERITE && getPlayerSwordTier(player) >= 5) return true;

        // Armors
        if (arena != null) {
            Arena.ArmorTier curArmor = arena.getPlayerArmorTier(player.getUniqueId());
            if (def.getAction() == ShopAction.ARMOR_CHAIN && curArmor.ordinal() >= Arena.ArmorTier.CHAIN.ordinal()) return true;
            if (def.getAction() == ShopAction.ARMOR_IRON && curArmor.ordinal() >= Arena.ArmorTier.IRON.ordinal()) return true;
            if (def.getAction() == ShopAction.ARMOR_DIAMOND && curArmor.ordinal() >= Arena.ArmorTier.DIAMOND.ordinal()) return true;
            if (def.getAction() == ShopAction.ARMOR_NETHERITE && curArmor.ordinal() >= Arena.ArmorTier.NETHERITE.ordinal()) return true;
        }

        return false;
    }

    public static int countItem(Player player, Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public static void deductItem(Player player, Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                if (item.getAmount() <= remaining) {
                    remaining -= item.getAmount();
                    player.getInventory().setItem(i, null);
                } else {
                    item.setAmount(item.getAmount() - remaining);
                    remaining = 0;
                }
                if (remaining <= 0) break;
            }
        }
    }
}
