package com.merycraft.meryitem.item;

import com.merycraft.meryitem.MeryItemPlugin;
import com.merycraft.meryitem.effect.ItemEffect;
import com.merycraft.meryitem.util.ColorUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

/**
 * Manages all registered MeryItems, configuration loading, and player stat aggregation.
 */
public class ItemManager {

    private final MeryItemPlugin plugin;
    private final Map<String, CustomItem> itemMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private final NamespacedKey idKey;

    public ItemManager(MeryItemPlugin plugin) {
        this.plugin = plugin;
        this.idKey = new NamespacedKey(plugin, "meryitem_id");
    }

    public void loadAll() {
        itemMap.clear();
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) {
            itemsFolder.mkdirs();
        }

        loadDirectory(itemsFolder, "Default");
        plugin.getLogger().info("Successfully loaded " + itemMap.size() + " custom MeryItems across " + getAllFolders().size() + " folders.");
    }

    private void loadDirectory(File dir, String folderHint) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                loadDirectory(file, file.getName());
            } else if (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml")) {
                String currentFolderHint = folderHint;
                if ("Default".equalsIgnoreCase(folderHint) && !file.getName().equalsIgnoreCase("custom_saved.yml")) {
                    currentFolderHint = file.getName().substring(0, file.getName().lastIndexOf('.'));
                }
                loadFile(file, currentFolderHint);
            }
        }
    }

    public void loadFile(File file) {
        loadFile(file, "Default");
    }

    public void loadFile(File file, String folderHint) {
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        for (String key : config.getKeys(false)) {
            ConfigurationSection sec = config.getConfigurationSection(key);
            if (sec == null) continue;

            String matName = sec.getString("material", "DIAMOND_SWORD");
            Material material = Material.matchMaterial(matName);
            if (material == null) material = Material.STONE;

            String name = sec.getString("name", key);
            ItemRarity rarity = ItemRarity.fromString(sec.getString("rarity", "COMMON"));
            ItemType type = ItemType.fromString(sec.getString("type", "MISC"));

            CustomItem item = new CustomItem(key, material, name, rarity, type);
            item.setFolder(sec.getString("folder", folderHint));
            item.setHideEnchants(sec.getBoolean("hide-enchants", false));
            item.setCustomModelData(sec.getInt("custom-model-data", 0));
            item.setRequiredLevel(sec.getInt("required-level", 0));
            item.setUnbreakable(sec.getBoolean("unbreakable", false));
            item.setGlowing(sec.getBoolean("glowing", false));
            item.setEffect(ItemEffect.fromString(sec.getString("effect", "NONE")));
            item.setParticleMotion(com.merycraft.meryitem.effect.ParticleMotion.fromString(sec.getString("particle-motion", "AURA")));
            item.setShowRarity(sec.getBoolean("show-rarity", false));
            item.setShowAbilityLore(sec.getBoolean("show-ability-lore", false));

            // Enchantments
            ConfigurationSection enchSec = sec.getConfigurationSection("enchantments");
            if (enchSec != null) {
                for (String enchName : enchSec.getKeys(false)) {
                    org.bukkit.enchantments.Enchantment ench = org.bukkit.enchantments.Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(enchName.toLowerCase()));
                    if (ench == null) ench = org.bukkit.enchantments.Enchantment.getByName(enchName.toUpperCase());
                    if (ench != null) {
                        item.setEnchantment(ench, enchSec.getInt(enchName));
                    }
                }
            }

            // Potion Effects
            ConfigurationSection potSec = sec.getConfigurationSection("potion-effects");
            if (potSec != null) {
                for (String potName : potSec.getKeys(false)) {
                    org.bukkit.potion.PotionEffectType pet = org.bukkit.potion.PotionEffectType.getByName(potName.toUpperCase());
                    if (pet == null) pet = org.bukkit.potion.PotionEffectType.getByKey(org.bukkit.NamespacedKey.minecraft(potName.toLowerCase()));
                    if (pet != null) {
                        item.setPotionEffect(pet, potSec.getInt(potName));
                    }
                }
            }
            item.setPotionTrigger(sec.getString("potion-trigger", "HOLD"));

            // Click Commands
            item.setClickCommands(sec.getStringList("commands"));
            item.setCommandClickType(sec.getString("command-click", "RIGHT"));

            // Stats
            ConfigurationSection statsSec = sec.getConfigurationSection("stats");
            if (statsSec != null) {
                for (String statName : statsSec.getKeys(false)) {
                    ItemStat stat = ItemStat.fromString(statName);
                    if (stat != null) {
                        item.setStat(stat, statsSec.getDouble(statName));
                    }
                }
            }

            // Abilities
            List<String> abilityNames = sec.getStringList("abilities");
            for (String abName : abilityNames) {
                ItemAbility ability = ItemAbility.fromString(abName);
                if (ability != null) {
                    item.addAbility(ability);
                }
            }

            // Custom lore
            item.setCustomLore(sec.getStringList("lore"));

            registerItem(item);
        }
    }

    public void registerItem(CustomItem item) {
        if (item != null) {
            itemMap.put(item.getId(), item);
        }
    }

    public CustomItem getItem(String id) {
        if (id == null) return null;
        return itemMap.get(id);
    }

    public Collection<CustomItem> getAllItems() {
        return Collections.unmodifiableCollection(itemMap.values());
    }

    public boolean isMeryItem(ItemStack stack) {
        return getMeryItemId(stack) != null;
    }

    public String getMeryItemId(ItemStack stack) {
        if (stack == null || !stack.hasItemMeta()) return null;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(idKey, PersistentDataType.STRING)) {
            return pdc.get(idKey, PersistentDataType.STRING);
        }
        return null;
    }

    public CustomItem getFromItemStack(ItemStack stack) {
        String id = getMeryItemId(stack);
        if (id != null && itemMap.containsKey(id)) {
            return itemMap.get(id);
        }

        // If not in saved catalog, reconstruct an in-memory CustomItem dynamically from PDC metadata
        if (stack != null && stack.hasItemMeta()) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                String itemId = (id != null) ? id : ("ITEM_" + (System.currentTimeMillis() % 100000));
                String name = meta.hasDisplayName() ? meta.getDisplayName() : ("&#F472B6✦ " + stack.getType().name());

                String rarityStr = pdc.get(new NamespacedKey(plugin, "meryitem_rarity"), PersistentDataType.STRING);
                ItemRarity rarity = rarityStr != null ? ItemRarity.fromString(rarityStr) : ItemRarity.COMMON;

                String typeStr = pdc.get(new NamespacedKey(plugin, "meryitem_type"), PersistentDataType.STRING);
                ItemType type = typeStr != null ? ItemType.fromString(typeStr) : ItemType.SWORD;

                CustomItem dynamicItem = new CustomItem(itemId, stack.getType(), name, rarity, type);

                NamespacedKey folderKey = new NamespacedKey(plugin, "meryitem_folder");
                if (pdc.has(folderKey, PersistentDataType.STRING)) {
                    dynamicItem.setFolder(pdc.get(folderKey, PersistentDataType.STRING));
                }

                NamespacedKey hideEnchKey = new NamespacedKey(plugin, "meryitem_hide_enchants");
                if (pdc.has(hideEnchKey, PersistentDataType.BYTE)) {
                    Byte b = pdc.get(hideEnchKey, PersistentDataType.BYTE);
                    dynamicItem.setHideEnchants(b != null && b == 1);
                }

                Byte showRarity = pdc.get(new NamespacedKey(plugin, "meryitem_show_rarity"), PersistentDataType.BYTE);
                dynamicItem.setShowRarity(showRarity != null && showRarity == 1);

                Byte showAbility = pdc.get(new NamespacedKey(plugin, "meryitem_show_ability_lore"), PersistentDataType.BYTE);
                dynamicItem.setShowAbilityLore(showAbility == null || showAbility == 1);

                Byte unb = pdc.get(new NamespacedKey(plugin, "meryitem_unbreakable"), PersistentDataType.BYTE);
                dynamicItem.setUnbreakable(unb != null ? (unb == 1) : (meta.isUnbreakable()));

                Byte glow = pdc.get(new NamespacedKey(plugin, "meryitem_glowing"), PersistentDataType.BYTE);
                dynamicItem.setGlowing(glow != null && glow == 1);

                // Recover abilities from PDC
                NamespacedKey abKey = new NamespacedKey(plugin, "mery_abilities");
                if (pdc.has(abKey, PersistentDataType.STRING)) {
                    String abRaw = pdc.get(abKey, PersistentDataType.STRING);
                    if (abRaw != null && !abRaw.isEmpty()) {
                        for (String abName : abRaw.split(";;")) {
                            ItemAbility ab = ItemAbility.fromString(abName);
                            if (ab != null) {
                                dynamicItem.addAbility(ab);
                            }
                        }
                    }
                }

                String effStr = pdc.get(new NamespacedKey(plugin, "meryitem_effect"), PersistentDataType.STRING);
                if (effStr != null) {
                    dynamicItem.setEffect(ItemEffect.fromString(effStr));
                }
                String motionStr = pdc.get(new NamespacedKey(plugin, "meryitem_particle_motion"), PersistentDataType.STRING);
                if (motionStr != null) {
                    dynamicItem.setParticleMotion(com.merycraft.meryitem.effect.ParticleMotion.fromString(motionStr));
                }

                // Recover enchantments from meta
                for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> e : meta.getEnchants().entrySet()) {
                    dynamicItem.setEnchantment(e.getKey(), e.getValue());
                }

                // Recover potion effects from PDC
                for (NamespacedKey k : pdc.getKeys()) {
                    if (k.getNamespace().equals(plugin.getName().toLowerCase()) && k.getKey().startsWith("merypot_")) {
                        String potName = k.getKey().substring("merypot_".length());
                        org.bukkit.potion.PotionEffectType pet = org.bukkit.potion.PotionEffectType.getByName(potName.toUpperCase());
                        if (pet == null) pet = org.bukkit.potion.PotionEffectType.getByKey(NamespacedKey.minecraft(potName.toLowerCase()));
                        if (pet != null) {
                            Integer lvl = pdc.get(k, PersistentDataType.INTEGER);
                            if (lvl != null && lvl > 0) {
                                dynamicItem.setPotionEffect(pet, lvl);
                            }
                        }
                    }
                }

                // Recover custom stats from PDC
                for (ItemStat stat : ItemStat.values()) {
                    NamespacedKey statKey = new NamespacedKey(plugin, "stat_" + stat.name().toLowerCase());
                    if (pdc.has(statKey, PersistentDataType.DOUBLE)) {
                        Double val = pdc.get(statKey, PersistentDataType.DOUBLE);
                        if (val != null && val != 0.0) {
                            dynamicItem.setStat(stat, val);
                        }
                    }
                }

                NamespacedKey potTriggerKey = new NamespacedKey(plugin, "mery_pot_trigger");
                if (pdc.has(potTriggerKey, PersistentDataType.STRING)) {
                    dynamicItem.setPotionTrigger(pdc.get(potTriggerKey, PersistentDataType.STRING));
                }

                // Recover click commands from PDC
                NamespacedKey cmdKey = new NamespacedKey(plugin, "mery_commands");
                if (pdc.has(cmdKey, PersistentDataType.STRING)) {
                    String raw = pdc.get(cmdKey, PersistentDataType.STRING);
                    if (raw != null && !raw.isEmpty()) {
                        dynamicItem.setClickCommands(Arrays.asList(raw.split(";;")));
                        dynamicItem.addAbility(ItemAbility.RUN_COMMAND);
                    }
                }
                NamespacedKey cmdTypeKey = new NamespacedKey(plugin, "mery_cmd_click");
                if (pdc.has(cmdTypeKey, PersistentDataType.STRING)) {
                    dynamicItem.setCommandClickType(pdc.get(cmdTypeKey, PersistentDataType.STRING));
                }

                NamespacedKey customLoreKey = new NamespacedKey(plugin, "mery_custom_lore");
                if (pdc.has(customLoreKey, PersistentDataType.STRING)) {
                    String rawLore = pdc.get(customLoreKey, PersistentDataType.STRING);
                    if (rawLore != null && !rawLore.isEmpty()) {
                        dynamicItem.setCustomLore(Arrays.asList(rawLore.split(";;")));
                    }
                } else if (meta.hasLore()) {
                    List<String> userLines = new ArrayList<>();
                    for (String line : meta.getLore()) {
                        String clean = ColorUtil.strip(line).trim();
                        if (clean.startsWith("Type:") || clean.startsWith("⚔") || clean.startsWith("🛡") ||
                            clean.startsWith("🔮") || clean.startsWith("❖") || clean.startsWith("🧪") ||
                            clean.startsWith("✨") || clean.startsWith("💎") || clean.startsWith("🌟") ||
                            clean.startsWith("◆") || clean.contains("MeryITEM") || clean.startsWith("⌛ Cooldown")) {
                            continue;
                        }
                        userLines.add(line);
                    }
                    dynamicItem.setCustomLore(userLines);
                }

                return dynamicItem;
            }
        }
        return null;
    }

    /**
     * Compute total combined stats from a player's equipment.
     */
    public Map<ItemStat, Double> getPlayerTotalStats(Player player) {
        Map<ItemStat, Double> total = new EnumMap<>(ItemStat.class);

        // Main Hand
        addStackStats(player.getInventory().getItemInMainHand(), total, true);

        // Off Hand
        addStackStats(player.getInventory().getItemInOffHand(), total, false);

        // Armor
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            addStackStats(armor, total, false);
        }

        return total;
    }

    private void addStackStats(ItemStack stack, Map<ItemStat, Double> total, boolean isMainHand) {
        if (stack == null || !stack.hasItemMeta()) return;
        CustomItem customItem = getFromItemStack(stack);
        if (customItem == null) return;

        // If weapon is in off-hand, don't count weapon attack stats unless designed
        if (!isMainHand && customItem.getType().isWeapon()) {
            return;
        }

        for (Map.Entry<ItemStat, Double> entry : customItem.getStats().entrySet()) {
            total.merge(entry.getKey(), entry.getValue(), Double::sum);
        }
    }

    public double getPlayerStat(Player player, ItemStat stat) {
        return getPlayerTotalStats(player).getOrDefault(stat, 0.0);
    }

    public Set<String> getAllFolders() {
        Set<String> folders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        folders.add("Default");
        for (CustomItem item : itemMap.values()) {
            if (item.getFolder() != null && !item.getFolder().trim().isEmpty()) {
                folders.add(item.getFolder().trim());
            }
        }
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (itemsFolder.exists() && itemsFolder.isDirectory()) {
            File[] files = itemsFolder.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isDirectory()) {
                        folders.add(f.getName());
                    } else if (f.getName().endsWith(".yml") || f.getName().endsWith(".yaml")) {
                        String base = f.getName().substring(0, f.getName().lastIndexOf('.'));
                        if (!base.equalsIgnoreCase("custom_saved")) {
                            folders.add(base);
                        }
                    }
                }
            }
        }
        return folders;
    }

    public List<CustomItem> getItemsByFolder(String folder) {
        List<CustomItem> list = new ArrayList<>();
        for (CustomItem item : itemMap.values()) {
            if (folder == null || folder.equalsIgnoreCase("ALL") || item.getFolder().equalsIgnoreCase(folder)) {
                list.add(item);
            }
        }
        return list;
    }

    public boolean createFolder(String folderName) {
        if (folderName == null || folderName.trim().isEmpty()) return false;
        String clean = folderName.trim().replaceAll("[^a-zA-Z0-9_-]", "_");
        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) itemsFolder.mkdirs();
        File file = new File(itemsFolder, clean + ".yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Save an item into its designated folder YAML file.
     */
    public void saveCustomItem(CustomItem item) {
        registerItem(item);
        String folder = item.getFolder();
        if (folder == null || folder.trim().isEmpty()) folder = "Default";

        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (!itemsFolder.exists()) itemsFolder.mkdirs();

        File file;
        if (folder.equalsIgnoreCase("Default")) {
            file = new File(itemsFolder, "custom_saved.yml");
        } else {
            String cleanName = folder.replaceAll("[^a-zA-Z0-9_-]", "_");
            file = new File(itemsFolder, cleanName + ".yml");
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        String path = item.getId();
        config.set(path + ".folder", folder);
        config.set(path + ".hide-enchants", item.isHideEnchants());
        config.set(path + ".material", item.getMaterial().name());
        config.set(path + ".name", item.getDisplayName());
        config.set(path + ".rarity", item.getRarity().name());
        config.set(path + ".type", item.getType().name());
        config.set(path + ".unbreakable", item.isUnbreakable());
        config.set(path + ".glowing", item.isGlowing());
        config.set(path + ".show-rarity", item.isShowRarity());
        config.set(path + ".show-ability-lore", item.isShowAbilityLore());
        if (item.getEffect() != null && item.getEffect() != ItemEffect.NONE) {
            config.set(path + ".effect", item.getEffect().name());
            config.set(path + ".particle-motion", item.getParticleMotion().name());
        } else {
            config.set(path + ".effect", null);
            config.set(path + ".particle-motion", null);
        }

        if (!item.getEnchantments().isEmpty()) {
            for (Map.Entry<org.bukkit.enchantments.Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                config.set(path + ".enchantments." + entry.getKey().getKey().getKey(), entry.getValue());
            }
        } else {
            config.set(path + ".enchantments", null);
        }

        if (!item.getPotionEffects().isEmpty()) {
            for (Map.Entry<org.bukkit.potion.PotionEffectType, Integer> entry : item.getPotionEffects().entrySet()) {
                config.set(path + ".potion-effects." + entry.getKey().getName(), entry.getValue());
            }
            config.set(path + ".potion-trigger", item.getPotionTrigger());
        } else {
            config.set(path + ".potion-effects", null);
        }

        config.set(path + ".commands", item.getClickCommands());
        config.set(path + ".command-click", item.getCommandClickType());

        if (item.getCustomModelData() > 0) {
            config.set(path + ".custom-model-data", item.getCustomModelData());
        }

        for (Map.Entry<ItemStat, Double> entry : item.getStats().entrySet()) {
            config.set(path + ".stats." + entry.getKey().name(), entry.getValue());
        }

        List<String> abs = new ArrayList<>();
        for (ItemAbility ab : item.getAbilities()) {
            abs.add(ab.name());
        }
        config.set(path + ".abilities", abs);
        config.set(path + ".lore", item.getCustomLore());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save item " + item.getId(), e);
        }
    }

    /**
     * Delete a custom item by ID from memory and all item YAML files.
     */
    public boolean deleteCustomItem(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        CustomItem removed = itemMap.remove(id);

        File itemsFolder = new File(plugin.getDataFolder(), "items");
        if (itemsFolder.exists() && itemsFolder.isDirectory()) {
            deleteFromDirectory(itemsFolder, id);
        }
        return (removed != null);
    }

    private void deleteFromDirectory(File dir, String id) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                deleteFromDirectory(f, id);
            } else if (f.getName().endsWith(".yml") || f.getName().endsWith(".yaml")) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(f);
                if (config.contains(id)) {
                    config.set(id, null);
                    try {
                        config.save(f);
                    } catch (IOException e) {
                        plugin.getLogger().log(Level.SEVERE, "Failed to remove item " + id + " from " + f.getName(), e);
                    }
                }
            }
        }
    }
}

