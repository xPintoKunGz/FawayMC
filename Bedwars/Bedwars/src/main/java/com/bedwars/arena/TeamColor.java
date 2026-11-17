package com.bedwars.arena;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;

public enum TeamColor {
    RED("Red", ChatColor.RED, Color.fromRGB(220, 20, 60), Material.RED_WOOL, Material.RED_BED, Material.RED_CONCRETE),
    BLUE("Blue", ChatColor.BLUE, Color.fromRGB(30, 80, 240), Material.BLUE_WOOL, Material.BLUE_BED, Material.BLUE_CONCRETE),
    GREEN("Green", ChatColor.DARK_GREEN, Color.fromRGB(34, 139, 34), Material.GREEN_WOOL, Material.GREEN_BED, Material.GREEN_CONCRETE),
    YELLOW("Yellow", ChatColor.YELLOW, Color.fromRGB(255, 215, 0), Material.YELLOW_WOOL, Material.YELLOW_BED, Material.YELLOW_CONCRETE),
    AQUA("Aqua", ChatColor.AQUA, Color.fromRGB(0, 220, 255), Material.LIGHT_BLUE_WOOL, Material.LIGHT_BLUE_BED, Material.LIGHT_BLUE_CONCRETE),
    WHITE("White", ChatColor.WHITE, Color.fromRGB(255, 255, 255), Material.WHITE_WOOL, Material.WHITE_BED, Material.WHITE_CONCRETE),
    PINK("Pink", ChatColor.LIGHT_PURPLE, Color.fromRGB(255, 105, 180), Material.PINK_WOOL, Material.PINK_BED, Material.PINK_CONCRETE),
    GRAY("Gray", ChatColor.DARK_GRAY, Color.fromRGB(80, 80, 80), Material.GRAY_WOOL, Material.GRAY_BED, Material.GRAY_CONCRETE),
    ORANGE("Orange", ChatColor.GOLD, Color.fromRGB(255, 140, 0), Material.ORANGE_WOOL, Material.ORANGE_BED, Material.ORANGE_CONCRETE),
    MAGENTA("Magenta", ChatColor.LIGHT_PURPLE, Color.fromRGB(218, 50, 190), Material.MAGENTA_WOOL, Material.MAGENTA_BED, Material.MAGENTA_CONCRETE),
    LIME("Lime", ChatColor.GREEN, Color.fromRGB(50, 205, 50), Material.LIME_WOOL, Material.LIME_BED, Material.LIME_CONCRETE),
    CYAN("Cyan", ChatColor.DARK_AQUA, Color.fromRGB(0, 150, 150), Material.CYAN_WOOL, Material.CYAN_BED, Material.CYAN_CONCRETE),
    PURPLE("Purple", ChatColor.DARK_PURPLE, Color.fromRGB(128, 0, 128), Material.PURPLE_WOOL, Material.PURPLE_BED, Material.PURPLE_CONCRETE),
    BROWN("Brown", ChatColor.GOLD, Color.fromRGB(139, 69, 19), Material.BROWN_WOOL, Material.BROWN_BED, Material.BROWN_CONCRETE),
    LIGHT_GRAY("Light Gray", ChatColor.GRAY, Color.fromRGB(169, 169, 169), Material.LIGHT_GRAY_WOOL, Material.LIGHT_GRAY_BED, Material.LIGHT_GRAY_CONCRETE),
    BLACK("Black", ChatColor.BLACK, Color.fromRGB(20, 20, 20), Material.BLACK_WOOL, Material.BLACK_BED, Material.BLACK_CONCRETE);

    private final String displayName;
    private final ChatColor chatColor;
    private final Color armorColor;
    private final Material woolMaterial;
    private final Material bedMaterial;
    private final Material concreteMaterial;

    TeamColor(String displayName, ChatColor chatColor, Color armorColor, Material woolMaterial, Material bedMaterial, Material concreteMaterial) {
        this.displayName = displayName;
        this.chatColor = chatColor;
        this.armorColor = armorColor;
        this.woolMaterial = woolMaterial;
        this.bedMaterial = bedMaterial;
        this.concreteMaterial = concreteMaterial;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ChatColor getChatColor() {
        return chatColor;
    }

    public Color getArmorColor() {
        return armorColor;
    }

    public Material getWoolMaterial() {
        return woolMaterial;
    }

    public Material getBedMaterial() {
        return bedMaterial;
    }

    public Material getConcreteMaterial() {
        return concreteMaterial;
    }

    public Material getGlassMaterial() {
        return switch (this) {
            case RED -> Material.RED_STAINED_GLASS;
            case BLUE -> Material.BLUE_STAINED_GLASS;
            case GREEN -> Material.GREEN_STAINED_GLASS;
            case YELLOW -> Material.YELLOW_STAINED_GLASS;
            case AQUA -> Material.LIGHT_BLUE_STAINED_GLASS;
            case WHITE -> Material.WHITE_STAINED_GLASS;
            case PINK -> Material.PINK_STAINED_GLASS;
            case GRAY -> Material.GRAY_STAINED_GLASS;
            case ORANGE -> Material.ORANGE_STAINED_GLASS;
            case MAGENTA -> Material.MAGENTA_STAINED_GLASS;
            case LIME -> Material.LIME_STAINED_GLASS;
            case CYAN -> Material.CYAN_STAINED_GLASS;
            case PURPLE -> Material.PURPLE_STAINED_GLASS;
            case BROWN -> Material.BROWN_STAINED_GLASS;
            case LIGHT_GRAY -> Material.LIGHT_GRAY_STAINED_GLASS;
            case BLACK -> Material.BLACK_STAINED_GLASS;
        };
    }

    public String getPrefix() {
        return chatColor + "● " + displayName;
    }
}
