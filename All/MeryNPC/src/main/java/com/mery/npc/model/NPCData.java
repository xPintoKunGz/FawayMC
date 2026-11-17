package com.mery.npc.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class NPCData {

    private int id;
    private String name;
    private String type; // PLAYER, VILLAGER, IRON_GOLEM, ARMOR_STAND, etc.

    // Persistent World & Coordinate Data (Never overwritten even if world loads late)
    private String worldName = "world";
    private String worldUid = "";
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private transient Location location;

    private boolean lookAtPlayer;
    private List<String> hologramLines;
    private double hologramOffset;
    private String skinName;
    private String skinValue;
    private String skinSignature;

    // Exclusive MeryNPC Studio Visuals
    private boolean glowing;
    private double scale;
    private String particleAura; // NONE, FLAME, SOUL, ENCHANT, CHERRY, END, HEART, TOTEM

    // Equipment: MAIN_HAND, OFF_HAND, HELMET, CHESTPLATE, LEGGINGS, BOOTS
    private Map<String, ItemStack> equipment;
    private NPCPose pose;
    private List<NPCCommand> commands;
    private boolean isSmall;
    private boolean showArms;

    // Runtime state (not saved in YAML)
    private transient UUID entityUuid;
    private transient UUID hitboxEntityUuid;
    private transient Object serverPlayer;
    private transient Set<UUID> viewers = new HashSet<>();
    private transient List<UUID> hologramEntityUuids = new ArrayList<>();

    public NPCData(int id, String name, Location location) {
        this.id = id;
        this.name = name != null ? name : "";
        this.type = "PLAYER";
        setLocation(location);
        this.lookAtPlayer = true;
        this.hologramLines = new ArrayList<>();
        this.hologramOffset = 0.35;
        this.skinName = "Steve";
        this.skinValue = "";
        this.skinSignature = "";
        this.glowing = false;
        this.scale = 1.0;
        this.particleAura = "NONE";
        this.equipment = new HashMap<>();
        this.pose = new NPCPose();
        this.commands = new ArrayList<>();
        this.isSmall = false;
        this.showArms = true;
    }

    public static World resolveWorld(String worldName, String worldUid) {
        if (worldName != null && !worldName.isEmpty()) {
            World direct = Bukkit.getWorld(worldName);
            if (direct != null) return direct;

            String stripped = worldName.contains(":") ? worldName.substring(worldName.indexOf(':') + 1) : worldName;
            World byStripped = Bukkit.getWorld(stripped);
            if (byStripped != null) return byStripped;

            for (World w : Bukkit.getWorlds()) {
                if (w.getName().equalsIgnoreCase(worldName)
                        || w.getName().equalsIgnoreCase(stripped)
                        || w.getKey().toString().equalsIgnoreCase(worldName)
                        || w.getKey().getKey().equalsIgnoreCase(stripped)) {
                    return w;
                }
            }
        }
        if (worldUid != null && !worldUid.isEmpty()) {
            try {
                World byUid = Bukkit.getWorld(UUID.fromString(worldUid));
                if (byUid != null) return byUid;
            } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }

    public boolean isInWorld(World world) {
        if (world == null) return false;
        if (worldUid != null && !worldUid.isEmpty() && world.getUID().toString().equalsIgnoreCase(worldUid)) {
            return true;
        }
        if (worldName != null && !worldName.isEmpty()) {
            String stripped = worldName.contains(":") ? worldName.substring(worldName.indexOf(':') + 1) : worldName;
            return world.getName().equalsIgnoreCase(worldName)
                    || world.getName().equalsIgnoreCase(stripped)
                    || world.getKey().toString().equalsIgnoreCase(worldName)
                    || world.getKey().getKey().equalsIgnoreCase(stripped);
        }
        return false;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("type", type);

        // Always serialize stored worldName & coordinates so unloaded worlds are NEVER lost!
        if (location != null && location.getWorld() != null) {
            this.worldName = location.getWorld().getName();
            this.worldUid = location.getWorld().getUID().toString();
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
        }
        Map<String, Object> locMap = new HashMap<>();
        locMap.put("world", worldName != null ? worldName : "world");
        if (worldUid != null && !worldUid.isEmpty()) {
            locMap.put("worldUid", worldUid);
        }
        locMap.put("x", x);
        locMap.put("y", y);
        locMap.put("z", z);
        locMap.put("yaw", (double) yaw);
        locMap.put("pitch", (double) pitch);
        map.put("location", locMap);

        map.put("lookAtPlayer", lookAtPlayer);
        map.put("hologramLines", hologramLines);
        map.put("hologramOffset", hologramOffset);
        map.put("skinName", skinName);
        map.put("skinValue", skinValue);
        map.put("skinSignature", skinSignature);
        map.put("glowing", glowing);
        map.put("scale", scale);
        map.put("particleAura", particleAura);
        map.put("isSmall", isSmall);
        map.put("showArms", showArms);

        // Equipment
        Map<String, Object> equipMap = new HashMap<>();
        for (Map.Entry<String, ItemStack> entry : equipment.entrySet()) {
            if (entry.getValue() != null) {
                equipMap.put(entry.getKey(), entry.getValue());
            }
        }
        map.put("equipment", equipMap);

        // Pose
        map.put("pose", pose.serialize());

        // Commands
        List<Map<String, Object>> cmdList = new ArrayList<>();
        for (NPCCommand cmd : commands) {
            cmdList.add(cmd.serialize());
        }
        map.put("commands", cmdList);

        return map;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> toMap(Object obj) {
        if (obj instanceof org.bukkit.configuration.ConfigurationSection) {
            return ((org.bukkit.configuration.ConfigurationSection) obj).getValues(false);
        }
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        }
        return Collections.emptyMap();
    }

    @SuppressWarnings("unchecked")
    public static NPCData deserialize(Map<String, Object> map) {
        int id = map.get("id") instanceof Number ? ((Number) map.get("id")).intValue() : 1;
        String name = (String) map.getOrDefault("name", "");

        String savedWorldName = "world";
        String savedWorldUid = "";
        double x = 0, y = 64, z = 0;
        float yaw = 0f, pitch = 0f;

        if (map.containsKey("location")) {
            Map<String, Object> locMap = toMap(map.get("location"));
            if (locMap.get("world") instanceof String && !((String) locMap.get("world")).isEmpty()) {
                savedWorldName = (String) locMap.get("world");
            }
            if (locMap.get("worldUid") instanceof String) {
                savedWorldUid = (String) locMap.get("worldUid");
            }
            if (locMap.get("x") instanceof Number) x = ((Number) locMap.get("x")).doubleValue();
            if (locMap.get("y") instanceof Number) y = ((Number) locMap.get("y")).doubleValue();
            if (locMap.get("z") instanceof Number) z = ((Number) locMap.get("z")).doubleValue();
            if (locMap.get("yaw") instanceof Number) yaw = ((Number) locMap.get("yaw")).floatValue();
            if (locMap.get("pitch") instanceof Number) pitch = ((Number) locMap.get("pitch")).floatValue();
        }

        World resolvedWorld = resolveWorld(savedWorldName, savedWorldUid);
        Location loc = resolvedWorld != null ? new Location(resolvedWorld, x, y, z, yaw, pitch) : null;

        NPCData data = new NPCData(id, name, loc);
        // Preserve exact saved worldName & coordinates even if resolvedWorld is null right now!
        data.setWorldName(savedWorldName);
        data.setWorldUid(savedWorldUid);
        data.setRawCoordinates(x, y, z, yaw, pitch);

        data.setType((String) map.getOrDefault("type", "PLAYER"));
        data.setLookAtPlayer((Boolean) map.getOrDefault("lookAtPlayer", true));
        data.setHologramOffset(map.get("hologramOffset") instanceof Number ? ((Number) map.get("hologramOffset")).doubleValue() : 0.35);
        data.setSkinName((String) map.getOrDefault("skinName", "Steve"));
        data.setSkinValue((String) map.getOrDefault("skinValue", ""));
        data.setSkinSignature((String) map.getOrDefault("skinSignature", ""));
        data.setGlowing(Boolean.TRUE.equals(map.get("glowing")));
        data.setScale(map.get("scale") instanceof Number ? ((Number) map.get("scale")).doubleValue() : 1.0);
        data.setParticleAura((String) map.getOrDefault("particleAura", "NONE"));
        data.setSmall((Boolean) map.getOrDefault("isSmall", false));
        data.setShowArms((Boolean) map.getOrDefault("showArms", true));

        if (map.containsKey("hologramLines") && map.get("hologramLines") instanceof List) {
            data.setHologramLines(new ArrayList<>((List<String>) map.get("hologramLines")));
        }

        if (map.containsKey("equipment")) {
            Map<String, Object> equipMap = toMap(map.get("equipment"));
            for (Map.Entry<String, Object> entry : equipMap.entrySet()) {
                if (entry.getValue() instanceof ItemStack) {
                    data.equipment.put(entry.getKey(), (ItemStack) entry.getValue());
                }
            }
        }

        if (map.containsKey("pose")) {
            data.setPose(NPCPose.deserialize(toMap(map.get("pose"))));
        }

        if (map.containsKey("commands") && map.get("commands") instanceof List) {
            List<?> cmdList = (List<?>) map.get("commands");
            for (Object cmdObj : cmdList) {
                Map<String, Object> cmdMap = toMap(cmdObj);
                if (!cmdMap.isEmpty()) {
                    data.commands.add(NPCCommand.deserialize(cmdMap));
                }
            }
        }

        return data;
    }

    // Getters and Setters
    public int getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name != null ? name : ""; }

    public String getWorldName() { return worldName; }
    public void setWorldName(String worldName) {
        if (worldName != null && !worldName.isEmpty()) {
            this.worldName = worldName;
        }
    }

    public String getWorldUid() { return worldUid; }
    public void setWorldUid(String worldUid) {
        this.worldUid = worldUid != null ? worldUid : "";
    }

    public void setRawCoordinates(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }

    public boolean isGlowing() { return glowing; }
    public void setGlowing(boolean glowing) { this.glowing = glowing; }

    public double getScale() { return scale; }
    public void setScale(double scale) { this.scale = Math.max(0.25, Math.min(3.0, scale)); }

    public String getParticleAura() { return particleAura != null ? particleAura : "NONE"; }
    public void setParticleAura(String particleAura) { this.particleAura = particleAura != null ? particleAura.toUpperCase() : "NONE"; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Location getLocation() {
        if (location == null || location.getWorld() == null) {
            World resolved = resolveWorld(worldName, worldUid);
            if (resolved != null) {
                this.worldName = resolved.getName();
                this.worldUid = resolved.getUID().toString();
                this.location = new Location(resolved, x, y, z, yaw, pitch);
            }
        }
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
        if (location != null) {
            this.x = location.getX();
            this.y = location.getY();
            this.z = location.getZ();
            this.yaw = location.getYaw();
            this.pitch = location.getPitch();
            if (location.getWorld() != null) {
                this.worldName = location.getWorld().getName();
                this.worldUid = location.getWorld().getUID().toString();
            }
        }
    }

    public boolean isLookAtPlayer() { return lookAtPlayer; }
    public void setLookAtPlayer(boolean lookAtPlayer) { this.lookAtPlayer = lookAtPlayer; }

    public List<String> getHologramLines() { return hologramLines; }
    public void setHologramLines(List<String> hologramLines) { this.hologramLines = hologramLines; }

    public double getHologramOffset() { return hologramOffset; }
    public void setHologramOffset(double hologramOffset) { this.hologramOffset = hologramOffset; }

    public String getSkinName() { return skinName; }
    public void setSkinName(String skinName) { this.skinName = skinName; }

    public String getSkinValue() { return skinValue; }
    public void setSkinValue(String skinValue) { this.skinValue = skinValue; }

    public String getSkinSignature() { return skinSignature; }
    public void setSkinSignature(String skinSignature) { this.skinSignature = skinSignature; }

    public Map<String, ItemStack> getEquipment() { return equipment; }
    public void setEquipment(Map<String, ItemStack> equipment) { this.equipment = equipment; }

    public NPCPose getPose() { return pose; }
    public void setPose(NPCPose pose) { this.pose = pose; }

    public List<NPCCommand> getCommands() { return commands; }
    public void setCommands(List<NPCCommand> commands) { this.commands = commands; }

    public boolean isSmall() { return isSmall; }
    public void setSmall(boolean small) { isSmall = small; }

    public boolean isShowArms() { return showArms; }
    public void setShowArms(boolean showArms) { this.showArms = showArms; }

    public UUID getEntityUuid() { return entityUuid; }
    public void setEntityUuid(UUID entityUuid) { this.entityUuid = entityUuid; }

    public UUID getHitboxEntityUuid() { return hitboxEntityUuid; }
    public void setHitboxEntityUuid(UUID hitboxEntityUuid) { this.hitboxEntityUuid = hitboxEntityUuid; }

    public Object getServerPlayer() { return serverPlayer; }
    public void setServerPlayer(Object serverPlayer) { this.serverPlayer = serverPlayer; }

    public Set<UUID> getViewers() { return viewers; }

    public List<UUID> getHologramEntityUuids() { return hologramEntityUuids; }
    public void setHologramEntityUuids(List<UUID> hologramEntityUuids) { this.hologramEntityUuids = hologramEntityUuids; }
}
