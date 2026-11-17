package com.mery.npc.model;

import java.util.HashMap;
import java.util.Map;

public class NPCCommand {

    private String command;
    private CommandType type;
    private ClickTrigger clickTrigger;
    private String permission;
    private String noPermissionMessage;
    private String cooldownMessage;
    private double cooldown; // in seconds
    private double cost;
    private int delay; // in ticks
    private boolean random;
    private boolean ignorePermMsg;
    private boolean ignoreMoneyMsg;

    public NPCCommand(String command, CommandType type, ClickTrigger clickTrigger) {
        this.command = command;
        this.type = type != null ? type : CommandType.CONSOLE;
        this.clickTrigger = clickTrigger != null ? clickTrigger : ClickTrigger.ANY;
        this.permission = "";
        this.noPermissionMessage = "";
        this.cooldownMessage = "";
        this.cooldown = 0;
        this.cost = 0;
        this.delay = 0;
        this.random = false;
        this.ignorePermMsg = false;
        this.ignoreMoneyMsg = false;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("command", command);
        map.put("type", type.name());
        map.put("clickTrigger", clickTrigger.name());
        if (permission != null && !permission.isEmpty()) map.put("permission", permission);
        if (noPermissionMessage != null && !noPermissionMessage.isEmpty()) map.put("noPermissionMessage", noPermissionMessage);
        if (cooldownMessage != null && !cooldownMessage.isEmpty()) map.put("cooldownMessage", cooldownMessage);
        if (cooldown > 0) map.put("cooldown", cooldown);
        if (cost > 0) map.put("cost", cost);
        if (delay > 0) map.put("delay", delay);
        if (random) map.put("random", true);
        if (ignorePermMsg) map.put("ignorePermMsg", true);
        if (ignoreMoneyMsg) map.put("ignoreMoneyMsg", true);
        return map;
    }

    public static NPCCommand deserialize(Map<String, Object> map) {
        String cmd = (String) map.getOrDefault("command", "");
        CommandType type = CommandType.fromString((String) map.getOrDefault("type", "CONSOLE"));
        ClickTrigger trigger = ClickTrigger.fromString((String) map.getOrDefault("clickTrigger", "ANY"));

        NPCCommand npcCmd = new NPCCommand(cmd, type, trigger);
        if (map.containsKey("permission")) npcCmd.setPermission((String) map.get("permission"));
        if (map.containsKey("noPermissionMessage")) npcCmd.setNoPermissionMessage((String) map.get("noPermissionMessage"));
        if (map.containsKey("cooldownMessage")) npcCmd.setCooldownMessage((String) map.get("cooldownMessage"));
        if (map.containsKey("cooldown") && map.get("cooldown") instanceof Number) npcCmd.setCooldown(((Number) map.get("cooldown")).doubleValue());
        if (map.containsKey("cost") && map.get("cost") instanceof Number) npcCmd.setCost(((Number) map.get("cost")).doubleValue());
        if (map.containsKey("delay") && map.get("delay") instanceof Number) npcCmd.setDelay(((Number) map.get("delay")).intValue());
        if (map.containsKey("random")) npcCmd.setRandom(Boolean.TRUE.equals(map.get("random")));
        if (map.containsKey("ignorePermMsg")) npcCmd.setIgnorePermMsg(Boolean.TRUE.equals(map.get("ignorePermMsg")));
        if (map.containsKey("ignoreMoneyMsg")) npcCmd.setIgnoreMoneyMsg(Boolean.TRUE.equals(map.get("ignoreMoneyMsg")));

        return npcCmd;
    }

    // Getters and Setters
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public CommandType getType() { return type; }
    public void setType(CommandType type) { this.type = type; }

    public ClickTrigger getClickTrigger() { return clickTrigger; }
    public void setClickTrigger(ClickTrigger clickTrigger) { this.clickTrigger = clickTrigger; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = permission; }

    public String getNoPermissionMessage() { return noPermissionMessage; }
    public void setNoPermissionMessage(String noPermissionMessage) { this.noPermissionMessage = noPermissionMessage; }

    public double getCooldown() { return cooldown; }
    public void setCooldown(double cooldown) { this.cooldown = cooldown; }

    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }

    public int getDelay() { return delay; }
    public void setDelay(int delay) { this.delay = delay; }

    public String getCooldownMessage() { return cooldownMessage; }
    public void setCooldownMessage(String cooldownMessage) { this.cooldownMessage = cooldownMessage != null ? cooldownMessage : ""; }

    public boolean isRandom() { return random; }
    public void setRandom(boolean random) { this.random = random; }

    public boolean isIgnorePermMsg() { return ignorePermMsg; }
    public void setIgnorePermMsg(boolean ignorePermMsg) { this.ignorePermMsg = ignorePermMsg; }

    public boolean isIgnoreMoneyMsg() { return ignoreMoneyMsg; }
    public void setIgnoreMoneyMsg(boolean ignoreMoneyMsg) { this.ignoreMoneyMsg = ignoreMoneyMsg; }
}

