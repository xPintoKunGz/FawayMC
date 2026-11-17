package com.holomery.gui;

import com.holomery.HoloMeryPlugin;
import com.holomery.color.ColorParser;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class GuiItemBuilder {

    private final ItemStack item;
    private final ItemMeta meta;

    public GuiItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public GuiItemBuilder(ItemStack itemStack) {
        this.item = itemStack.clone();
        this.meta = item.getItemMeta();
    }

    public GuiItemBuilder name(String name) {
        if (meta != null) {
            meta.setDisplayName(ColorParser.colorize(name));
        }
        return this;
    }

    public GuiItemBuilder lore(String... lines) {
        if (meta != null) {
            List<String> list = new ArrayList<>();
            for (String l : lines) {
                list.add(ColorParser.colorize(l));
            }
            meta.setLore(list);
        }
        return this;
    }

    public GuiItemBuilder lore(List<String> lines) {
        if (meta != null) {
            List<String> list = new ArrayList<>();
            for (String l : lines) {
                list.add(ColorParser.colorize(l));
            }
            meta.setLore(list);
        }
        return this;
    }

    public GuiItemBuilder glow(boolean glow) {
        if (meta != null && glow) {
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public GuiItemBuilder hideFlags() {
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_POTION_EFFECTS);
        }
        return this;
    }

    public GuiItemBuilder data(String key, String value) {
        if (meta != null && key != null && value != null) {
            NamespacedKey nsKey = new NamespacedKey(HoloMeryPlugin.getInstance(), key);
            meta.getPersistentDataContainer().set(nsKey, PersistentDataType.STRING, value);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }

    public static String getData(ItemStack item, String key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        NamespacedKey nsKey = new NamespacedKey(HoloMeryPlugin.getInstance(), key);
        return meta.getPersistentDataContainer().get(nsKey, PersistentDataType.STRING);
    }
}
