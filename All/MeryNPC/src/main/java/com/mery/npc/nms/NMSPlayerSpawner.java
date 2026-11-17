package com.mery.npc.nms;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mery.npc.MeryNPC;
import com.mery.npc.model.NPCData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class NMSPlayerSpawner {

    public static final String DEFAULT_STEVE_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2YxMTc3MzhjYjg1NWY5MWU0YzlmZTNlNWQ1Y2FkOWI0NTg5N2QxMWY5OTFjZTFmNDgwZmJlZTI4MWVhY2EifX19";

    private final MeryNPC plugin;

    // Reflection caches
    private Class<?> craftServerClass;
    private Class<?> craftWorldClass;
    private Class<?> craftPlayerClass;
    private Class<?> serverPlayerClass;
    private Class<?> packetClass;
    private boolean initialized = false;

    public NMSPlayerSpawner(MeryNPC plugin) {
        this.plugin = plugin;
        initReflection();
    }

    private void initReflection() {
        try {
            craftServerClass = getCraftClass("CraftServer");
            craftWorldClass = getCraftClass("CraftWorld");
            craftPlayerClass = getCraftClass("entity.CraftPlayer");

            try {
                serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
                packetClass = Class.forName("net.minecraft.network.protocol.Packet");
            } catch (ClassNotFoundException e) {
                serverPlayerClass = getNMSClass("EntityPlayer");
                packetClass = getNMSClass("Packet");
            }

            initialized = true;
        } catch (Throwable t) {
            plugin.getLogger().warning("NMS reflection initialization: " + t.getMessage());
            initialized = false;
        }
    }

    public boolean isSupported() {
        return initialized;
    }

    public Object createServerPlayer(NPCData data) {
        if (!initialized || data.getLocation() == null) return null;

        try {
            Object craftServer = Bukkit.getServer();
            Method getServerMethod = craftServer.getClass().getMethod("getServer");
            Object minecraftServer = getServerMethod.invoke(craftServer);

            Object craftWorld = data.getLocation().getWorld();
            Method getHandleWorld = craftWorld.getClass().getMethod("getHandle");
            Object serverLevel = getHandleWorld.invoke(craftWorld);

            UUID npcUUID = UUID.nameUUIDFromBytes(("MeryNPC_" + data.getId()).getBytes());

            // Minecraft client strictly requires valid ASCII name [a-zA-Z0-9_] between 1-16 chars
            String profileName = "NPC_" + data.getId();
            if (data.getName() != null && !data.getName().trim().isEmpty()) {
                String clean = data.getName().replaceAll("[^a-zA-Z0-9_]", "");
                if (!clean.isEmpty()) {
                    profileName = clean.length() > 16 ? clean.substring(0, 16) : clean;
                }
            }

            GameProfile profile = new GameProfile(npcUUID, profileName);

            String skinVal = data.getSkinValue();
            String skinSig = data.getSkinSignature();
            if (skinVal == null || skinVal.isEmpty()) {
                skinVal = DEFAULT_STEVE_TEXTURE;
                skinSig = "";
            }

            // Use reflection for properties() / getProperties() to avoid NoSuchMethodError across different AuthLib versions
            try {
                Method getPropertiesMethod = findMethod(profile.getClass(), new String[]{"properties", "getProperties"});
                Object propertyMap = getPropertiesMethod != null ? getPropertiesMethod.invoke(profile) : null;

                Object propertyObj = null;
                try {
                    Class<?> propClass = Class.forName("com.mojang.authlib.properties.Property");
                    for (Constructor<?> c : propClass.getConstructors()) {
                        Class<?>[] pts = c.getParameterTypes();
                        if (pts.length == 3 && pts[0] == String.class && pts[1] == String.class && pts[2] == String.class) {
                            propertyObj = c.newInstance("textures", skinVal, skinSig != null ? skinSig : "");
                            break;
                        } else if (pts.length == 2 && pts[0] == String.class && pts[1] == String.class) {
                            propertyObj = c.newInstance("textures", skinVal);
                            break;
                        }
                    }
                } catch (Throwable ignored) {}

                if (propertyObj != null && propertyMap != null) {
                    for (Method m : propertyMap.getClass().getMethods()) {
                        if (m.getName().equals("put")) {
                            if (m.getParameterCount() == 2) {
                                try {
                                    m.invoke(propertyMap, "textures", propertyObj);
                                    break;
                                } catch (Throwable ignored) {}
                            } else if (m.getParameterCount() == 1) {
                                try {
                                    m.invoke(propertyMap, propertyObj);
                                    break;
                                } catch (Throwable ignored) {}
                            }
                        }
                    }
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Error applying skin texture to GameProfile: " + t.getMessage());
            }

            Object serverPlayer = null;

            for (Constructor<?> c : serverPlayerClass.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 4) {
                    try {
                        Class<?> clientInfoClass = Class.forName("net.minecraft.server.level.ClientInformation");
                        Method createDefaultMethod = clientInfoClass.getMethod("createDefault");
                        Object clientInfo = createDefaultMethod.invoke(null);
                        serverPlayer = c.newInstance(minecraftServer, serverLevel, profile, clientInfo);
                        break;
                    } catch (Throwable ignored) {}
                } else if (params.length == 3) {
                    try {
                        serverPlayer = c.newInstance(minecraftServer, serverLevel, profile);
                        break;
                    } catch (Throwable ignored) {}
                }
            }

            if (serverPlayer == null) return null;

            Location loc = data.getLocation();
            Method setPosMethod = findMethod(serverPlayer.getClass(), new String[]{"setPos", "p", "a"}, double.class, double.class, double.class);
            if (setPosMethod != null) {
                setPosMethod.invoke(serverPlayer, loc.getX(), loc.getY(), loc.getZ());
            }

            Method setRotMethod = findMethod(serverPlayer.getClass(), new String[]{"setRot", "o", "setRotation"}, float.class, float.class);
            if (setRotMethod != null) {
                setRotMethod.invoke(serverPlayer, loc.getYaw(), loc.getPitch());
            }

            enableSkinLayers(serverPlayer);

            return serverPlayer;
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not create ServerPlayer for NPC #" + data.getId() + ": " + t.getMessage());
            return null;
        }
    }

    private void enableSkinLayers(Object serverPlayer) {
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

    public boolean spawnForPlayer(Player player, Object serverPlayer, NPCData data) {
        if (!initialized || serverPlayer == null || player == null || !player.isOnline()) return false;

        try {
            // 1. ClientboundPlayerInfoUpdatePacket (ADD_PLAYER + GAME_MODE + LISTED)
            Object infoPacket = createPlayerInfoPacket(serverPlayer);
            if (infoPacket != null) {
                sendPacket(player, infoPacket);
            }

            // 2. ClientboundAddEntityPacket
            Object spawnPacket = createSpawnPacket(serverPlayer, data.getLocation());
            if (spawnPacket != null) {
                sendPacket(player, spawnPacket);
            } else {
                return false;
            }

            // 3. Metadata packet (all skin layers enabled)
            Object metadataPacket = createMetadataPacket(serverPlayer);
            if (metadataPacket != null) {
                sendPacket(player, metadataPacket);
            }

            // 4. Head Yaw packet
            Object headPacket = createRotateHeadPacket(serverPlayer, data.getLocation().getYaw());
            if (headPacket != null) {
                sendPacket(player, headPacket);
            }

            // 5. Equipment packet (Holding sword & shield)
            updateEquipmentForPlayer(player, serverPlayer, data);

            // DO NOT remove player from tab in modern Minecraft 1.20+ / 1.21+
            // Sending ClientboundPlayerInfoRemovePacket causes modern Minecraft clients
            // to instantly destroy and despawn the entity from the world!

            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Error sending spawn packet to " + player.getName() + ": " + t.getMessage());
            return false;
        }
    }

    public void updateRotation(Player player, Object serverPlayer, float yaw, float pitch) {
        if (!initialized || serverPlayer == null || player == null || !player.isOnline()) return;

        try {
            Object headPacket = createRotateHeadPacket(serverPlayer, yaw);
            if (headPacket != null) {
                sendPacket(player, headPacket);
            }
        } catch (Throwable ignored) {}
    }

    public void destroyForPlayer(Player player, Object serverPlayer) {
        if (!initialized || serverPlayer == null || player == null || !player.isOnline()) return;

        try {
            int entityId = getEntityId(serverPlayer);
            Class<?> removePacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
            Constructor<?> constructor = removePacketClass.getConstructor(int[].class);
            Object packet = constructor.newInstance(new int[]{entityId});
            sendPacket(player, packet);

            removePlayerFromTab(player, serverPlayer);
        } catch (Throwable ignored) {}
    }

    private Object createPlayerInfoPacket(Object serverPlayer) {
        try {
            Class<?> infoPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");

            // 1. Try static factory createPlayerInitializing(Collection) (Standard in 1.20.2+)
            try {
                Method createInit = infoPacketClass.getMethod("createPlayerInitializing", Collection.class);
                return createInit.invoke(null, Collections.singletonList(serverPlayer));
            } catch (Throwable ignored) {}

            // 2. Fallback to constructor with EnumSet
            Class<?> actionEnum = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            EnumSet<?> actions;
            try {
                // Try to include ADD_PLAYER and UPDATE_GAME_MODE and UPDATE_LISTED
                actions = EnumSet.allOf((Class<Enum>) actionEnum);
            } catch (Throwable t) {
                Object addPlayerAction = Enum.valueOf((Class<Enum>) actionEnum, "ADD_PLAYER");
                actions = EnumSet.of((Enum) addPlayerAction);
            }

            for (Constructor<?> c : infoPacketClass.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 2) {
                    if (params[0].isAssignableFrom(EnumSet.class) || params[0].equals(EnumSet.class)) {
                        return c.newInstance(actions, Collections.singletonList(serverPlayer));
                    } else if (params[0].equals(actionEnum)) {
                        Object addPlayerAction = Enum.valueOf((Class<Enum>) actionEnum, "ADD_PLAYER");
                        return c.newInstance(addPlayerAction, serverPlayer);
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private void removePlayerFromTab(Player player, Object serverPlayer) {
        try {
            Object profile = getProfile(serverPlayer);
            if (profile == null) return;
            Method getIdMethod = profile.getClass().getMethod("getId");
            UUID uuid = (UUID) getIdMethod.invoke(profile);
            Class<?> removeClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
            Constructor<?> c = removeClass.getConstructor(List.class);
            Object packet = c.newInstance(Collections.singletonList(uuid));
            sendPacket(player, packet);
        } catch (Throwable ignored) {}
    }

    private Object createSpawnPacket(Object serverPlayer, Location loc) {
        // 1. Try serverPlayer.getAddEntityPacket()
        try {
            for (Method m : serverPlayer.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("addentity") && m.getReturnType().getSimpleName().contains("Packet")) {
                    if (m.getParameterCount() == 0) {
                        return m.invoke(serverPlayer);
                    }
                }
            }
        } catch (Throwable ignored) {}

        // 2. Try ClientboundAddEntityPacket constructors
        try {
            Class<?> addEntityClass = Class.forName("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
            for (Constructor<?> c : addEntityClass.getConstructors()) {
                Class<?>[] params = c.getParameterTypes();
                if (params.length == 1 && params[0].isAssignableFrom(serverPlayer.getClass())) {
                    return c.newInstance(serverPlayer);
                }
                if (params.length == 3 && params[0].isAssignableFrom(serverPlayer.getClass())) {
                    Class<?> blockPosClass = Class.forName("net.minecraft.core.BlockPos");
                    Field zeroField = blockPosClass.getField("ZERO");
                    Object zeroPos = zeroField.get(null);
                    return c.newInstance(serverPlayer, 0, zeroPos);
                }
            }
        } catch (Throwable ignored) {}

        // 3. Try ClientboundAddPlayerPacket(ServerPlayer)
        try {
            Class<?> addPlayerClass = Class.forName("net.minecraft.network.protocol.game.ClientboundAddPlayerPacket");
            Constructor<?> c = addPlayerClass.getConstructor(serverPlayerClass);
            return c.newInstance(serverPlayer);
        } catch (Throwable ignored) {}

        return null;
    }

    private Object createMetadataPacket(Object serverPlayer) {
        try {
            int id = getEntityId(serverPlayer);
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

                // If items list is null or empty, do NOT send ClientboundSetEntityDataPacket
                // as Netty's StreamCodec will throw NullPointerException on items.iterator() and kick the player!
                if (packedData == null || !(packedData instanceof List) || ((List<?>) packedData).isEmpty()) {
                    return null;
                }

                Class<?> metadataPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
                for (Constructor<?> c : metadataPacketClass.getConstructors()) {
                    Class<?>[] params = c.getParameterTypes();
                    if (params.length == 2 && params[0] == int.class && params[1].isAssignableFrom(List.class)) {
                        return c.newInstance(id, packedData);
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

    public void updateEquipmentForPlayer(Player player, Object serverPlayer, NPCData data) {
        try {
            int entityId = getEntityId(serverPlayer);
            Class<?> equipPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket");
            Class<?> craftItemStackClass = getCraftClass("inventory.CraftItemStack");
            Method asNMSCopy = craftItemStackClass.getMethod("asNMSCopy", ItemStack.class);

            Class<?> slotClass = Class.forName("net.minecraft.world.entity.EquipmentSlot");

            List<Object> pairs = new ArrayList<>();
            Class<?> pairClass = Class.forName("com.mojang.datafixers.util.Pair");
            Constructor<?> pairConstructor = pairClass.getConstructor(Object.class, Object.class);

            Map<String, ItemStack> equip = data.getEquipment();
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "MAINHAND", equip.get("MAIN_HAND"));
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "OFFHAND", equip.get("OFF_HAND"));
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "HEAD", equip.get("HELMET"));
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "CHEST", equip.get("CHESTPLATE"));
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "LEGS", equip.get("LEGGINGS"));
            addEquipPair(pairs, pairConstructor, slotClass, asNMSCopy, "FEET", equip.get("BOOTS"));

            if (!pairs.isEmpty()) {
                Constructor<?> equipConstructor = equipPacketClass.getConstructor(int.class, List.class);
                Object packet = equipConstructor.newInstance(entityId, pairs);
                sendPacket(player, packet);
            }
        } catch (Throwable ignored) {}
    }

    private void addEquipPair(List<Object> list, Constructor<?> pairConstructor, Class<?> slotClass, Method asNMSCopy, String slotName, ItemStack item) {
        if (item == null) return;
        try {
            Object slot = Enum.valueOf((Class<Enum>) slotClass, slotName);
            Object nmsItem = asNMSCopy.invoke(null, item);
            list.add(pairConstructor.newInstance(slot, nmsItem));
        } catch (Throwable ignored) {}
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

    public int getEntityId(Object serverPlayer) {
        try {
            Method getIdMethod = findMethod(serverPlayer.getClass(), new String[]{"getId", "aj", "an", "am"});
            if (getIdMethod != null) {
                return (int) getIdMethod.invoke(serverPlayer);
            }
        } catch (Throwable ignored) {}
        return -1;
    }

    public GameProfile getProfile(Object serverPlayer) {
        try {
            Method getGameProfile = findMethod(serverPlayer.getClass(), new String[]{"getGameProfile", "fP", "fQ", "fR"});
            if (getGameProfile != null) {
                return (GameProfile) getGameProfile.invoke(serverPlayer);
            }
        } catch (Throwable ignored) {}
        return null;
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

    private Class<?> getNMSClass(String name) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        if (version.contains(".v1_")) {
            String v = version.substring(version.lastIndexOf('.') + 1);
            return Class.forName("net.minecraft.server." + v + "." + name);
        } else {
            return Class.forName("net.minecraft.server." + name);
        }
    }
}
