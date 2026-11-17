package com.merycraft.meryitem.item;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.effect.ItemEffect;
import com.merycraft.meryitem.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

/**
 * Custom Item definition in MeryITEM.
 */
public class CustomItem {

    private String id;
    private String folder = "Default";
    private boolean hideEnchants = false;
    private String displayName;
    private Material material;
    private int customModelData;
    private ItemRarity rarity;
    private ItemType type;
    private boolean showRarity = false; // Toggle rarity & type banner on/off
    private final Map<ItemStat, Double> stats = new EnumMap<>(ItemStat.class);
    private final Map<Enchantment, Integer> enchantments = new HashMap<>();
    private final Map<PotionEffectType, Integer> potionEffects = new HashMap<>();
    private String potionTrigger = "HOLD"; // "HOLD", "RIGHT", or "LEFT"
    private boolean showAbilityLore = true; // Toggle built-in ability and cooldown lore banner on/off
    private final List<String> clickCommands = new ArrayList<>();
    private String commandClickType = "RIGHT"; // "RIGHT" or "LEFT"
    private final List<ItemAbility> abilities = new ArrayList<>();
    private final List<String> customLore = new ArrayList<>();
    private int requiredLevel;
    private boolean unbreakable;
    private boolean glowing;
    private ItemEffect effect = ItemEffect.NONE;
    private com.merycraft.meryitem.effect.ParticleMotion particleMotion = com.merycraft.meryitem.effect.ParticleMotion.AURA;

    public CustomItem(String id, Material material, String displayName, ItemRarity rarity, ItemType type) {
        this.id = id.toUpperCase();
        this.material = material != null ? material : Material.STONE;
        this.displayName = displayName;
        this.rarity = rarity != null ? rarity : ItemRarity.COMMON;
        this.type = type != null ? type : ItemType.MISC;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id != null && !id.isEmpty()) {
            this.id = id.toUpperCase();
        }
    }

    public String getFolder() {
        return (folder != null && !folder.trim().isEmpty()) ? folder.trim() : "Default";
    }

    public void setFolder(String folder) {
        this.folder = (folder != null && !folder.trim().isEmpty()) ? folder.trim() : "Default";
    }

    public boolean isHideEnchants() {
        return hideEnchants;
    }

    public void setHideEnchants(boolean hideEnchants) {
        this.hideEnchants = hideEnchants;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Material getMaterial() {
        return material;
    }

    public void setMaterial(Material material) {
        this.material = material;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public void setCustomModelData(int customModelData) {
        this.customModelData = customModelData;
    }

    public ItemRarity getRarity() {
        return rarity;
    }

    public void setRarity(ItemRarity rarity) {
        this.rarity = rarity;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public boolean isShowRarity() {
        return showRarity;
    }

    public void setShowRarity(boolean showRarity) {
        this.showRarity = showRarity;
    }

    public boolean isShowAbilityLore() {
        return showAbilityLore;
    }

    public void setShowAbilityLore(boolean showAbilityLore) {
        this.showAbilityLore = showAbilityLore;
    }

    public Map<ItemStat, Double> getStats() {
        return Collections.unmodifiableMap(stats);
    }

    public double getStat(ItemStat stat) {
        return stats.getOrDefault(stat, 0.0);
    }

    public void setStat(ItemStat stat, double value) {
        if (value == 0) {
            stats.remove(stat);
        } else {
            stats.put(stat, value);
        }
    }

    public Map<Enchantment, Integer> getEnchantments() {
        return Collections.unmodifiableMap(enchantments);
    }

    public int getEnchantmentLevel(Enchantment ench) {
        return enchantments.getOrDefault(ench, 0);
    }

    public void setEnchantment(Enchantment ench, int level) {
        if (level <= 0) {
            enchantments.remove(ench);
        } else {
            enchantments.put(ench, Math.min(255, level));
        }
    }

    public void removeEnchantment(Enchantment ench) {
        enchantments.remove(ench);
    }

    public Map<PotionEffectType, Integer> getPotionEffects() {
        return Collections.unmodifiableMap(potionEffects);
    }

    public int getPotionLevel(PotionEffectType type) {
        return potionEffects.getOrDefault(type, 0);
    }

    public void setPotionEffect(PotionEffectType type, int level) {
        if (level <= 0) {
            potionEffects.remove(type);
        } else {
            potionEffects.put(type, Math.min(250, level));
        }
    }

    public void removePotionEffect(PotionEffectType type) {
        potionEffects.remove(type);
    }

    public String getPotionTrigger() {
        if (potionTrigger == null) return "HOLD";
        String s = potionTrigger.toUpperCase();
        return (s.equals("RIGHT") || s.equals("LEFT")) ? s : "HOLD";
    }

    public void setPotionTrigger(String trigger) {
        if (trigger != null && (trigger.equalsIgnoreCase("RIGHT") || trigger.equalsIgnoreCase("LEFT"))) {
            this.potionTrigger = trigger.toUpperCase();
        } else {
            this.potionTrigger = "HOLD";
        }
    }

    public List<String> getClickCommands() {
        return Collections.unmodifiableList(clickCommands);
    }

    public String getCommandClickType() {
        return (commandClickType != null && commandClickType.equalsIgnoreCase("LEFT")) ? "LEFT" : "RIGHT";
    }

    public void setCommandClickType(String commandClickType) {
        if (commandClickType != null && commandClickType.equalsIgnoreCase("LEFT")) {
            this.commandClickType = "LEFT";
        } else {
            this.commandClickType = "RIGHT";
        }
    }

    public void addClickCommand(String cmd) {
        if (cmd != null && !clickCommands.contains(cmd)) {
            clickCommands.add(cmd);
        }
    }

    public void setClickCommands(List<String> commands) {
        this.clickCommands.clear();
        if (commands != null) {
            this.clickCommands.addAll(commands);
        }
    }

    public void clearClickCommands() {
        this.clickCommands.clear();
    }

    public List<ItemAbility> getAbilities() {
        return Collections.unmodifiableList(abilities);
    }

    public void addAbility(ItemAbility ability) {
        if (ability != null && !abilities.contains(ability)) {
            abilities.add(ability);
        }
    }

    public void removeAbility(ItemAbility ability) {
        abilities.remove(ability);
    }

    public List<String> getCustomLore() {
        return customLore;
    }

    public void setCustomLore(List<String> lore) {
        this.customLore.clear();
        if (lore != null) {
            this.customLore.addAll(lore);
        }
    }

    public int getRequiredLevel() {
        return requiredLevel;
    }

    public void setRequiredLevel(int requiredLevel) {
        this.requiredLevel = requiredLevel;
    }

    public boolean isUnbreakable() {
        return unbreakable;
    }

    public void setUnbreakable(boolean unbreakable) {
        this.unbreakable = unbreakable;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public ItemEffect getEffect() {
        return effect;
    }

    public void setEffect(ItemEffect effect) {
        this.effect = effect != null ? effect : ItemEffect.NONE;
    }

    public com.merycraft.meryitem.effect.ParticleMotion getParticleMotion() {
        return particleMotion != null ? particleMotion : com.merycraft.meryitem.effect.ParticleMotion.AURA;
    }

    public void setParticleMotion(com.merycraft.meryitem.effect.ParticleMotion particleMotion) {
        this.particleMotion = particleMotion != null ? particleMotion : com.merycraft.meryitem.effect.ParticleMotion.AURA;
    }

    /**
     * Builds the ItemStack representation with PersistentDataContainer tags.
     */
    public ItemStack buildItemStack(int amount) {
        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Display Name
        String name = (displayName != null && !displayName.isEmpty()) ? displayName : (showRarity ? rarity.getPrimaryColor() + id : "&#CBD5E1" + id);
        meta.setDisplayName(ColorUtil.color(name));

        // Custom Model Data
        if (customModelData > 0) {
            meta.setCustomModelData(customModelData);
        }

        // Unbreakable
        if (unbreakable) {
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        }

        // Flags
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        // Enchantments
        if (!enchantments.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                meta.addEnchant(entry.getKey(), entry.getValue(), true);
            }
            if (hideEnchants) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        } else if (glowing) {
            try {
                meta.setEnchantmentGlintOverride(true);
            } catch (Throwable t) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
        }

        // Set Lore (clean and empty if no lore added!)
        List<String> lore = LoreBuilder.buildLore(this);
        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        } else {
            meta.setLore(null);
        }

        // Store MeryITEM metadata into PersistentDataContainer
        NamespacedKey idKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_id");
        NamespacedKey folderKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_folder");
        NamespacedKey rarityKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_rarity");
        NamespacedKey typeKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_type");
        NamespacedKey showRarityKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_show_rarity");

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(idKey, PersistentDataType.STRING, id);
        pdc.set(folderKey, PersistentDataType.STRING, getFolder());
        pdc.set(rarityKey, PersistentDataType.STRING, rarity.name());
        pdc.set(typeKey, PersistentDataType.STRING, type.name());
        pdc.set(showRarityKey, PersistentDataType.BYTE, (byte) (showRarity ? 1 : 0));
        NamespacedKey showAbilityKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_show_ability_lore");
        pdc.set(showAbilityKey, PersistentDataType.BYTE, (byte) (showAbilityLore ? 1 : 0));

        NamespacedKey unbreakKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_unbreakable");
        pdc.set(unbreakKey, PersistentDataType.BYTE, (byte) (unbreakable ? 1 : 0));

        NamespacedKey hideEnchKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_hide_enchants");
        pdc.set(hideEnchKey, PersistentDataType.BYTE, (byte) (hideEnchants ? 1 : 0));

        NamespacedKey glowKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_glowing");
        pdc.set(glowKey, PersistentDataType.BYTE, (byte) (glowing ? 1 : 0));

        if (!customLore.isEmpty()) {
            NamespacedKey customLoreKey = new NamespacedKey(MeryItemPlugin.getInstance(), "mery_custom_lore");
            pdc.set(customLoreKey, PersistentDataType.STRING, String.join(";;", customLore));
        }

        if (effect != null && effect != ItemEffect.NONE) {
            NamespacedKey effectKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_effect");
            pdc.set(effectKey, PersistentDataType.STRING, effect.name());
            NamespacedKey motionKey = new NamespacedKey(MeryItemPlugin.getInstance(), "meryitem_particle_motion");
            pdc.set(motionKey, PersistentDataType.STRING, getParticleMotion().name());
        }

        // Store stats into PDC
        for (Map.Entry<ItemStat, Double> entry : stats.entrySet()) {
            NamespacedKey statKey = new NamespacedKey(MeryItemPlugin.getInstance(), "stat_" + entry.getKey().name().toLowerCase());
            pdc.set(statKey, PersistentDataType.DOUBLE, entry.getValue());
        }

        // Store potion effects directly into PDC so held items grant them immediately even unsaved!
        for (Map.Entry<PotionEffectType, Integer> entry : potionEffects.entrySet()) {
            NamespacedKey potKey = new NamespacedKey(MeryItemPlugin.getInstance(), "merypot_" + entry.getKey().getName().toLowerCase());
            pdc.set(potKey, PersistentDataType.INTEGER, entry.getValue());
        }
        NamespacedKey potTriggerKey = new NamespacedKey(MeryItemPlugin.getInstance(), "mery_pot_trigger");
        pdc.set(potTriggerKey, PersistentDataType.STRING, getPotionTrigger());

        // Store abilities into PDC so they persist and execute properly even on unsaved items!
        if (!abilities.isEmpty()) {
            NamespacedKey abilitiesKey = new NamespacedKey(MeryItemPlugin.getInstance(), "mery_abilities");
            List<String> abNames = new ArrayList<>();
            for (ItemAbility ab : abilities) {
                abNames.add(ab.name());
            }
            pdc.set(abilitiesKey, PersistentDataType.STRING, String.join(";;", abNames));
        }

        // Store commands and click type directly in PDC (hidden from lore!)
        if (!clickCommands.isEmpty()) {
            NamespacedKey cmdKey = new NamespacedKey(MeryItemPlugin.getInstance(), "mery_commands");
            NamespacedKey cmdTypeKey = new NamespacedKey(MeryItemPlugin.getInstance(), "mery_cmd_click");
            pdc.set(cmdKey, PersistentDataType.STRING, String.join(";;", clickCommands));
            pdc.set(cmdTypeKey, PersistentDataType.STRING, getCommandClickType());
        }

        item.setItemMeta(meta);
        return item;
    }
}
