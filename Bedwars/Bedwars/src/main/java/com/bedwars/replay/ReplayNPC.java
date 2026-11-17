package com.bedwars.replay;

import com.bedwars.arena.TeamColor;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * High-performance, 100% packet-based Player NPC for Bedwars Replays.
 * Spawns real Minecraft player models with skins, team colors, armor, and weapons.
 * ZERO entities exist on the server world, ensuring 100% isolation from live gameplay.
 */
public class ReplayNPC {

    public static final String DEFAULT_STEVE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2YxMTc3MzhjYjg1NWY5MWU0YzlmZTNlNWQ1Y2FkOWI0NTg5N2QxMWY5OTFjZTFmNDgwZmJlZTI4MWVhY2EifX19";

    private final UUID uuid;
    private final String originalName;
    private final TeamColor teamColor;
    private final String skinValue;
    private final String skinSignature;

    private Object serverPlayer;
    private int entityId = -1;
    private GameProfile gameProfile;
    private boolean spawned = false;
    private byte lastFlags = 0;
    private final Set<UUID> activeViewers = new HashSet<>();

    public ReplayNPC(UUID uuid, String name, TeamColor teamColor, String skinValue, String skinSignature) {
        this.uuid = uuid;
        this.originalName = name;
        this.teamColor = teamColor;
        this.skinValue = (skinValue != null && !skinValue.isEmpty()) ? skinValue : DEFAULT_STEVE_TEXTURE;
        this.skinSignature = (skinSignature != null) ? skinSignature : "";
    }

    public boolean initServerPlayer(Location initialLoc) {
        if (serverPlayer != null) return true;

        try {
            Object craftServer = Bukkit.getServer();
            Method getServerMethod = craftServer.getClass().getMethod("getServer");
            Object minecraftServer = getServerMethod.invoke(craftServer);

            Object craftWorld = initialLoc.getWorld();
            Method getHandleWorld = craftWorld.getClass().getMethod("getHandle");
            Object serverLevel = getHandleWorld.invoke(craftWorld);

            // Clean profile name strictly [a-zA-Z0-9_] max 16 chars
            String cleanName = originalName.replaceAll("[^a-zA-Z0-9_]", "");
            if (cleanName.isEmpty()) cleanName = "Player";
            if (cleanName.length() > 16) cleanName = cleanName.substring(0, 16);

            this.gameProfile = new GameProfile(uuid, cleanName);

            // Apply skin texture to GameProfile properties
            try {
                Method getPropertiesMethod = findMethod(gameProfile.getClass(), new String[]{"properties", "getProperties"});
                Object propertyMap = getPropertiesMethod != null ? getPropertiesMethod.invoke(gameProfile) : null;

                Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
                Object propertyObj = null;
                for (Constructor<?> c : propClass.getConstructors()) {
                    Class<?>[] pts = c.getParameterTypes();
                    if (pts.length == 3 && pts[0] == String.class && pts[1] == String.class && pts[2] == String.class) {
                        propertyObj = c.newInstance("textures", skinValue, skinSignature);
                        break;
                    } else if (pts.length == 2 && pts[0] == String.class && pts[1] == String.class) {
                        propertyObj = c.newInstance("textures", skinValue);
                        break;
                    }
                }

                if (propertyObj != null && propertyMap != null) {
                    for (Method m : propertyMap.getClass().getMethods()) {
                        if (m.getName().equals("put")) {
                            if (m.getParameterCount() == 2) {
                                m.invoke(propertyMap, "textures", propertyObj);
                                break;
                            } else if (m.getParameterCount() == 1) {
                                m.invoke(propertyMap, propertyObj);
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                Bukkit.getLogger().warning("[Bedwars Replay] Error applying skin to GameProfile: " + t.getMessage());
            }

            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");

            for (Constructor<?> c : serverPlayerClass.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 4) {
                    try {
                        Class<?> clientInfoClass = Class.forName("net.minecraft.server.level.ClientInformation");
                        Method createDefaultMethod = clientInfoClass.getMethod("createDefault");
                        Object clientInfo = createDefaultMethod.invoke(null);
                        serverPlayer = c.newInstance(minecraftServer, serverLevel, gameProfile, clientInfo);
                        break;
                    } catch (Throwable ignored) {}
                } else if (params.length == 3) {
                    try {
                        serverPlayer = c.newInstance(minecraftServer, serverLevel, gameProfile);
                        break;
                    } catch (Throwable ignored) {}
                }
            }

            if (serverPlayer == null) return false;

            // Set initial position & rotation
            Method setPosMethod = findMethod(serverPlayer.getClass(), new String[]{"setPos", "p", "a"}, double.class, double.class, double.class);
            if (setPosMethod != null) {
                setPosMethod.invoke(serverPlayer, initialLoc.getX(), initialLoc.getY(), initialLoc.getZ());
            }

            Method setRotMethod = findMethod(serverPlayer.getClass(), new String[]{"setRot", "o", "setRotation"}, float.class, float.class);
            if (setRotMethod != null) {
                setRotMethod.invoke(serverPlayer, initialLoc.getYaw(), initialLoc.getPitch());
            }

            // Get entity ID
            Method getIdMethod = findMethod(serverPlayer.getClass(), new String[]{"getId", "aj", "an", "am"});
            if (getIdMethod != null) {
                this.entityId = (int) getIdMethod.invoke(serverPlayer);
            }

            // Enable all skin layers (Cape, Jacket, Left Sleeve, Right Sleeve, Left Pants, Right Pants, Hat)
            enableSkinLayers();

            return true;
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[Bedwars Replay] Could not initialize ServerPlayer for " + originalName + ": " + t.getMessage());
            return false;
        }
    }

    private void enableSkinLayers() {
        if (serverPlayer == null) return;
        try {
            Method getEntityDataMethod = findMethod(serverPlayer.getClass(), new String[]{"getEntityData", "al", "an"});
            if (getEntityDataMethod != null) {
                Object entityData = getEntityDataMethod.invoke(serverPlayer);
                Class<?> playerClass = Class.forName("net.minecraft.world.entity.player.Player");
                Field customisationField = null;
                for (Field f : playerClass.getDeclaredFields()) {
                    if (f.getName().contains("DATA_PLAYER_MODE_CUSTOMISATION") || f.getName().equals("bX") || f.getName().equals("bY") || f.getName().equals("bZ") || f.getName().equals("ca")) {
                        f.setAccessible(true);
                        customisationField = f;
                        break;
                    }
                }

                if (customisationField != null) {
                    Object dataAccessor = customisationField.get(null);
                    Method setMethod = findMethod(entityData.getClass(), new String[]{"set", "b"});
                    if (setMethod != null) {
                        setMethod.invoke(entityData, dataAccessor, (byte) 127);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public boolean spawn(Player viewer, Location loc) {
        if (viewer == null || !viewer.isOnline()) return false;
        if (!initServerPlayer(loc)) return false;

        try {
            // 1. ClientboundPlayerInfoUpdatePacket (ADD_PLAYER + GAME_MODE + LISTED)
            Object infoPacket = createPlayerInfoPacket(serverPlayer);
            if (infoPacket != null) {
                sendPacket(viewer, infoPacket);
            }

            // 2. ClientboundAddEntityPacket (or AddPlayerPacket)
            Object spawnPacket = createSpawnPacket(serverPlayer);
            if (spawnPacket != null) {
                sendPacket(viewer, spawnPacket);
            } else {
                return false;
            }

            // 3. Metadata packet (all skin layers enabled)
            Object metadataPacket = createMetadataPacket(serverPlayer);
            if (metadataPacket != null) {
                sendPacket(viewer, metadataPacket);
            }

            // 4. Rotate Head Yaw packet
            Object headPacket = createRotateHeadPacket(serverPlayer, loc.getYaw());
            if (headPacket != null) {
                sendPacket(viewer, headPacket);
            }

            this.spawned = true;
            return true;
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[Bedwars Replay] Error spawning NPC for " + viewer.getName() + ": " + t.getMessage());
            return false;
        }
    }

    public void teleportAndRotate(Player viewer, Location loc, float pitch, boolean sneaking, boolean sprinting) {
        if (!spawned || serverPlayer == null || viewer == null || !viewer.isOnline()) return;

        try {
            Method setPosMethod = findMethod(serverPlayer.getClass(), new String[]{"setPos", "p", "a"}, double.class, double.class, double.class);
            if (setPosMethod != null) {
                setPosMethod.invoke(serverPlayer, loc.getX(), loc.getY(), loc.getZ());
            }

            Method setRotMethod = findMethod(serverPlayer.getClass(), new String[]{"setRot", "o", "setRotation"}, float.class, float.class);
            if (setRotMethod != null) {
                setRotMethod.invoke(serverPlayer, loc.getYaw(), pitch);
            }

            // 1. Teleport packet
            Object tpPacket = createTeleportPacket(serverPlayer);
            if (tpPacket != null) {
                sendPacket(viewer, tpPacket);
            }

            // 2. Head yaw packet
            Object headPacket = createRotateHeadPacket(serverPlayer, loc.getYaw());
            if (headPacket != null) {
                sendPacket(viewer, headPacket);
            }

            // 3. Sneak / Sprint metadata flags update
            byte flags = 0;
            if (sneaking) flags |= 0x02; // crouching
            if (sprinting) flags |= 0x08; // sprinting

            if (flags != lastFlags) {
                lastFlags = flags;
                updateEntityFlags(viewer, flags);
            }
        } catch (Throwable ignored) {}
    }

    public void swingArm(Player viewer) {
        if (!spawned || serverPlayer == null || viewer == null || !viewer.isOnline()) return;

        try {
            Class<?> animClass = Class.forName("net.minecraft.network.protocol.game.ClientboundAnimatePacket");
            for (Constructor<?> c : animClass.getConstructors()) {
                Class<?>[] pts = c.getParameterTypes();
                if (pts.length == 2 && pts[1] == int.class) {
                    Object packet = c.newInstance(serverPlayer, 0); // 0 = swing main hand
                    sendPacket(viewer, packet);
                    break;
                }
            }
        } catch (Throwable ignored) {}
    }

    public void updateEquipment(Player viewer, ItemStack mainHand, ItemStack offHand, ItemStack helmet, ItemStack chest, ItemStack legs, ItemStack boots) {
        if (!spawned || serverPlayer == null || viewer == null || !viewer.isOnline()) return;

        try {
            Class<?> equipPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket");
            Class<?> craftItemStackClass = getCraftClass("inventory.CraftItemStack");
            Method asNMSCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);

            Class<?> slotClass = Class.forName("net.minecraft.world.entity.EquipmentSlot");

            List<Object> pairs = new ArrayList<>();
            Class<?> pairClass = Class.forName("com.mojang.datafixers.util.Pair");
            Constructor<?> pairConstructor = pairClass.getConstructor(Object.class, Object.class);

            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "MAINHAND", mainHand);
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "OFFHAND", offHand);
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "HEAD", helmet);
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "CHEST", chest);
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "LEGS", legs);
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "FEET", boots);

            if (!pairs.isEmpty()) {
                Constructor<?> equipConstructor = equipPacketClass.getConstructor(int.class, List.class);
                Object packet = equipConstructor.newInstance(entityId, pairs);
                sendPacket(viewer, packet);
            }
        } catch (Throwable ignored) {}
    }

    private void addEquipPair(List<Object> list, Constructor<?> pairConstructor, Class<?> slotClass, Method asNMSCopy, String slotName, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return;
        try {
            Object slot = Enum.valueOf((Class<Enum>) slotClass, slotName);
            Object nmsItem = asNMSCopy.invoke(null, item);
            list.add(pairConstructor.newInstance(slot, nmsItem));
        } catch (Throwable ignored) {}
    }

    private void updateEntityFlags(Player viewer, byte flags) {
        try {
            Method getEntityDataMethod = findMethod(serverPlayer.getClass(), new String[]{"getEntityData", "al", "an"});
            if (getEntityDataMethod != null) {
                Object entityData = getEntityDataMethod.invoke(serverPlayer);
                Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
                Field dataSharedFlagsField = null;
                for (Field f : entityClass.getDeclaredFields()) {
                    if (f.getName().contains("DATA_SHARED_FLAGS_ID") || f.getName().equals("ao") || f.getName().equals("ap") || f.getName().equals("aq")) {
                        f.setAccessible(true);
                        dataSharedFlagsField = f;
                        break;
                    }
                }

                if (dataSharedFlagsField != null) {
                    Object dataAccessor = dataSharedFlagsField.get(null);
                    Method setMethod = findMethod(entityData.getClass(), new String[]{"set", "b"});
                    if (setMethod != null) {
                        setMethod.invoke(entityData, dataAccessor, flags);
                        Object metaPacket = createMetadataPacket(serverPlayer);
                        if (metaPacket != null) {
                            sendPacket(viewer, metaPacket);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public void destroy(Player viewer) {
        if (!spawned || viewer == null || !viewer.isOnline()) return;

        try {
            if (entityId != -1) {
                Class<?> removePacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
                Constructor<?> constructor = removePacketClass.getConstructor(int[].class);
                Object packet = constructor.newInstance(new int[]{entityId});
                sendPacket(viewer, packet);
            }

            // Remove from Tab list
            Class<?> removeTabClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
            Constructor<?> cTab = removeTabClass.getConstructor(List.class);
            Object tabPacket = cTab.newInstance(Collections.singletonList(uuid));
            sendPacket(viewer, tabPacket);
        } catch (Throwable ignored) {}

        this.spawned = false;
    }

    // Packet Factory Helpers
    private Object createPlayerInfoPacket(Object serverPlayer) {
        try {
            Class<?> infoPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");

            // Static factory method createPlayerInitializing(Collection) (Standard 1.20.2+)
            try {
                Method createInit = infoPacketClass.getMethod("createPlayerInitializing", Collection.class);
                return createInit.invoke(null, Collections.singletonList(serverPlayer));
            } catch (Throwable ignored) {}

            // Fallback EnumSet constructor
            Class<?> actionEnum = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            EnumSet<?> actions;
            try {
                actions = EnumSet.allOf((Class<Enum>) actionEnum);
            } catch (Throwable t) {
                Object addPlayerAction = Enum.valueOf((Class<Enum>) actionEnum, "ADD_PLAYER");
                actions = EnumSet.of((Enum) addPlayerAction);
            }

            for (Constructor<?> c : infoPacketClass.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 2 && params[0].isAssignableFrom(EnumSet.class)) {
                    return c.newInstance(actions, Collections.singletonList(serverPlayer));
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private Object createSpawnPacket(Object serverPlayer) {
        try {
            for (Method m : serverPlayer.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("addentity") && m.getReturnType().getSimpleName().contains("Packet")) {
                    if (m.getParameterCount() == 0) {
                        return m.invoke(serverPlayer);
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> addEntityClass = Class.forName("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
            for (Constructor<?> c : addEntityClass.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(serverPlayer.getClass())) {
                    return c.newInstance(serverPlayer);
                }
            }
        } catch (Throwable ignored) {}

        return null;
    }

    private Object createMetadataPacket(Object serverPlayer) {
        try {
            Method getEntityDataMethod = findMethod(serverPlayer.getClass(), new String[]{"getEntityData", "al", "an"});
            if (getEntityDataMethod != null) {
                Object entityData = getEntityDataMethod.invoke(serverPlayer);
                if (entityData == null) return null;

                Object packedData = null;
                for (String mName : new String[]{"packAll", "getNonDefaultValues", "packDirty", "b", "c"}) {
                    Method m = findMethod(entityData.getClass(), new String[]{mName});
                    if (m != null) {
                        try {
                            Object res = m.invoke(entityData);
                            if (res instanceof List && !((List<?>) res).isEmpty()) {
                                packedData = res;
                                break;
                            }
                        } catch (Throwable ignored) {}
                    }
                }

                if (packedData instanceof List && !((List<?>) packedData).isEmpty()) {
                    Class<?> metadataPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
                    for (Constructor<?> c : metadataPacketClass.getConstructors()) {
                        Class<?>[] params = c.getParameterTypes();
                        if (params.length == 2 && params[0] == int.class && params[1].isAssignableFrom(List.class)) {
                            return c.newInstance(entityId, packedData);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private Object createRotateHeadPacket(Object serverPlayer, float yaw) {
        try {
            Class<?> headPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket");
            byte bYaw = (byte) ((yaw * 256.0F) / 360.0F);

            for (Constructor<?> c : headPacketClass.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 2 && params[1] == byte.class) {
                    return c.newInstance(serverPlayer, bYaw);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private Object createTeleportPacket(Object serverPlayer) {
        try {
            Class<?> tpClass = Class.forName("net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket");
            for (Constructor<?> c : tpClass.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(serverPlayer.getClass())) {
                    return c.newInstance(serverPlayer);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void sendPacket(Player player, Object packet) {
        if (packet == null || player == null || !player.isOnline()) return;

        try {
            Method getHandleMethod = player.getClass().getMethod("getHandle");
            Object craftPlayer = getHandleMethod.invoke(player);

            Field connectionField = null;
            for (Field f : craftPlayer.getClass().getFields()) {
                if (f.getName().equals("connection") || f.getName().equals("b") || f.getType().getSimpleName().contains("PacketListener")) {
                    connectionField = f;
                    break;
                }
            }
            if (connectionField == null) {
                for (Field f : craftPlayer.getClass().getDeclaredFields()) {
                    if (f.getName().equals("connection") || f.getName().equals("b") || f.getType().getSimpleName().contains("PacketListener")) {
                        f.setAccessible(true);
                        connectionField = f;
                        break;
                    }
                }
            }

            if (connectionField != null) {
                Object connection = connectionField.get(craftPlayer);
                Method sendMethod = null;
                for (Method m : connection.getClass().getMethods()) {
                    if (m.getName().equals("send") || m.getName().equals("sendPacket")) {
                        if (m.getParameterCount() == 1) {
                            sendMethod = m;
                            break;
                        }
                    }
                }
                if (sendMethod != null) {
                    sendMethod.invoke(connection, packet);
                }
            }
        } catch (Throwable ignored) {}
    }

    private Method findMethod(Class<?> clazz, String[] names, Class<?>... params) {
        for (String name : names) {
            try {
                Method m = clazz.getMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {}
        }
        for (Method m : clazz.getMethods()) {
            for (String name : names) {
                if (m.getName().equals(name)) {
                    m.setAccessible(true);
                    return m;
                }
            }
        }
        return null;
    }

    private Class<?> getCraftClass(String name) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        if (version.contains(".v1_")) {
            return Class.forName(version + "." + name);
        } else {
            return Class.forName("org.bukkit.craftbukkit." + name);
        }
    }

    // Getters
    public UUID getUuid() { return uuid; }
    public String getOriginalName() { return originalName; }
    public TeamColor getTeamColor() { return teamColor; }
    public int getEntityId() { return entityId; }
    public boolean isSpawned() { return spawned; }
}

