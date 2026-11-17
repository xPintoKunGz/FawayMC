package me.editmery.menu;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public class MenuItem {
    private final ItemStack itemStack;
    private final String command;
    private final boolean suggestOnly;
    private final MenuType nextMenu;
    private final Consumer<Player> action;

    public MenuItem(ItemStack itemStack, String command) {
        this(itemStack, command, false, null, null);
    }

    public MenuItem(ItemStack itemStack, MenuType nextMenu) {
        this(itemStack, null, false, nextMenu, null);
    }

    public MenuItem(ItemStack itemStack, String command, boolean suggestOnly) {
        this(itemStack, command, suggestOnly, null, null);
    }

    public MenuItem(ItemStack itemStack, Consumer<Player> action) {
        this(itemStack, null, false, null, action);
    }

    public MenuItem(ItemStack itemStack, String command, boolean suggestOnly, MenuType nextMenu, Consumer<Player> action) {
        this.itemStack = itemStack;
        this.command = command;
        this.suggestOnly = suggestOnly;
        this.nextMenu = nextMenu;
        this.action = action;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public String getCommand() {
        return command;
    }

    public boolean isSuggestOnly() {
        return suggestOnly;
    }

    public MenuType getNextMenu() {
        return nextMenu;
    }

    public Consumer<Player> getAction() {
        return action;
    }
}
