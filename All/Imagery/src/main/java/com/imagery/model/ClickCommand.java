package com.imagery.model;

import com.imagery.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents a command executed when a player clicks a placed image or album in the world.
 */
public class ClickCommand {

    public enum Type {
        PLAYER,
        CONSOLE,
        MESSAGE
    }

    private final Type type;
    private final String command;

    public ClickCommand(Type type, String command) {
        this.type = type;
        this.command = command;
    }

    public Type getType() {
        return type;
    }

    public String getCommand() {
        return command;
    }

    /**
     * Executes the command for the clicking player with placeholders.
     */
    public void execute(Player player, PlacedImage image) {
        if (player == null || command == null || command.isEmpty()) return;

        String parsed = command
                .replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString())
                .replace("%world%", image.getWorldName())
                .replace("%x%", String.valueOf(image.getOriginX()))
                .replace("%y%", String.valueOf(image.getOriginY()))
                .replace("%z%", String.valueOf(image.getOriginZ()))
                .replace("%page%", String.valueOf(image.getCurrentPage() + 1))
                .replace("%max_page%", String.valueOf(Math.max(1, image.getPages().size())))
                .replace("%file%", image.getCurrentFileName());

        if (parsed.startsWith("/")) {
            parsed = parsed.substring(1);
        }

        switch (type) {
            case PLAYER -> player.performCommand(parsed);
            case CONSOLE -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            case MESSAGE -> player.sendMessage(ColorUtil.color(parsed));
        }
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type.name());
        map.put("command", command);
        return map;
    }

    public static ClickCommand deserialize(Map<String, Object> map) {
        Type type = Type.valueOf((String) map.getOrDefault("type", "PLAYER"));
        String cmd = (String) map.getOrDefault("command", "");
        return new ClickCommand(type, cmd);
    }
}

