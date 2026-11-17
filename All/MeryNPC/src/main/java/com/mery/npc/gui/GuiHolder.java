package com.mery.npc.gui;

import com.mery.npc.model.NPCData;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class GuiHolder implements InventoryHolder {

    private final NPCData npcData;
    private final String guiType;

    public GuiHolder(NPCData npcData, String guiType) {
        this.npcData = npcData;
        this.guiType = guiType;
    }

    public NPCData getNpcData() {
        return npcData;
    }

    public String getGuiType() {
        return guiType;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}

