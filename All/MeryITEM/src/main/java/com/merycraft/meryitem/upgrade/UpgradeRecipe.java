package com.merycraft.meryitem.upgrade;

import com.merycraft.meryitem.item.ItemStat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Definition of an upgrade material / recipe to boost custom weapon stats or attach commands.
 */
public class UpgradeRecipe {

    private final String id;
    private final String requiredMaterialOrName; // e.g. "DIAMOND" or specific display name e.g. "Refined Diamond"
    private final int requiredAmount;
    private final ItemStat targetStat;
    private final double statBonus;
    private final String attachCommand; // Optional command to attach
    private final String description;

    public UpgradeRecipe(String id, String requiredMaterialOrName, int requiredAmount, ItemStat targetStat, double statBonus, String attachCommand, String description) {
        this.id = id;
        this.requiredMaterialOrName = requiredMaterialOrName;
        this.requiredAmount = Math.max(1, requiredAmount);
        this.targetStat = targetStat;
        this.statBonus = statBonus;
        this.attachCommand = attachCommand;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getRequiredMaterialOrName() {
        return requiredMaterialOrName;
    }

    public int getRequiredAmount() {
        return requiredAmount;
    }

    public ItemStat getTargetStat() {
        return targetStat;
    }

    public double getStatBonus() {
        return statBonus;
    }

    public String getAttachCommand() {
        return attachCommand;
    }

    public String getDescription() {
        return description;
    }
}

