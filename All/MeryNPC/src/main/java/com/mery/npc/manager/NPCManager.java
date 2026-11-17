package com.mery.npc.manager;

import com.destroystokyo.paper.SkinParts;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.mery.npc.MeryNPC;
import com.mery.npc.model.NPCData;
import com.mery.npc.model.NPCPose;
import com.mery.npc.nms.NMSPlayerSpawner;
import com.mery.npc.util.ColorUtil;
import com.mery.npc.util.PlaceholderUtil;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NPCManager {

    public static volatile boolean isSpawning = false;

    private final MeryNPC plugin;
    private final NamespacedKey npcIdKey;
    private final Map<Integer, NPCData> npcs = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> entityToNpcMap = new ConcurrentHashMap<>();

    public NPCManager(MeryNPC plugin) {
        this.plugin = plugin;
        this.npcIdKey = new NamespacedKey(plugin, "merynpc_id");
    }

    public NamespacedKey getNpcIdKey() {
        return npcIdKey;
    }

    public Map<Integer, NPCData> getAllNPCs() {
        return npcs;
    }

    public NPCData getNPC(int id) {
        return npcs.get(id);
    }

    public NPCData getNPCByEntity(Entity entity) {
        if (entity == null) return null;
        Integer id = entityToNpcMap.get(entity.getUniqueId());
        if (id != null) {
            return npcs.get(id);
        }
        try {
            if (entity.getPersistentDataContainer().has(npcIdKey, PersistentDataType.INTEGER)) {
                Integer pdcId = entity.getPersistentDataContainer().get(npcIdKey, PersistentDataType.INTEGER);
                if (pdcId != null && npcs.containsKey(pdcId)) {
                    entityToNpcMap.put(entity.getUniqueId(), pdcId);
                    return npcs.get(pdcId);
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public int getNextId() {
        int max = 0;
        for (int id : npcs.keySet()) {
            if (id > max) max = id;
        }
        return max + 1;
    }

    public void registerNPC(NPCData data) {
        npcs.put(data.getId(), data);
    }

    public NPCData createNPC(String name, Location loc, String type) {
        int id = getNextId();
        // If name is null or empty, leave it completely empty
        String finalName = (name != null) ? name.trim() : "";
        NPCData data = new NPCData(id, finalName, loc);

        if (type != null && !type.isEmpty()) {
            data.setType(type.toUpperCase());
        } else {
            data.setType("PLAYER"); // Default to real player NPC
        }

        // Default Steve skin if none set
        if (data.getSkinValue() == null || data.getSkinValue().isEmpty()) {
            data.setSkinValue(NMSPlayerSpawner.DEFAULT_STEVE_TEXTURE);
            data.setSkinName("Steve");
        }

        // Apply default Steve equipment
        if (data.getEquipment().isEmpty()) {
            plugin.getSkinManager().applyDefaultSteveEquipment(data);
        }

        npcs.put(id, data);
        spawnNPC(data);
        return data;
    }

    public void removeNPC(int id) {
        NPCData data = npcs.remove(id);
        if (data != null) {
            despawnNPC(data);
        }
    }

    public void spawnNPC(NPCData data) {
        if (data.getLocation() == null || data.getLocation().getWorld() == null) return;

        World world = data.getLocation().getWorld();
        int chunkX = data.getLocation().getBlockX() >> 4;
        int chunkZ = data.getLocation().getBlockZ() >> 4;

        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            world.loadChunk(chunkX, chunkZ);
        }

        // Despawn previous instance
        despawnNPC(data);
        cleanupOrphanedEntities(data, world);

        // MeryNPC Native 3D Persona Engine (Paper 26.3)
        boolean spawned = false;
        String typeStr = data.getType() != null ? data.getType().toUpperCase() : "PLAYER";
        if ("PLAYER".equals(typeStr) || "MANNEQUIN".equals(typeStr)) {
            spawned = spawnMannequin(data, world);
        } else if ("ARMOR_STAND".equals(typeStr)) {
            spawnArmorStandFallback(data, world);
            spawned = true;
        } else {
            spawned = spawnCustomEntityType(data, world, typeStr);
            if (!spawned) {
                spawned = spawnMannequin(data, world);
            }
        }
        if (!spawned) {
            spawnArmorStandFallback(data, world);
        }

        // Spawn Hologram above NPC
        spawnHologram(data);
    }

    public void applyVisualAttributes(NPCData data, Entity entity) {
        if (entity == null) return;
        entity.setGlowing(data.isGlowing());
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            try {
                AttributeInstance scaleAttr = living.getAttribute(Attribute.SCALE);
                if (scaleAttr != null) {
                    scaleAttr.setBaseValue(data.getScale());
                }
            } catch (Throwable ignored) {}
        }
    }

    public boolean spawnCustomEntityType(NPCData data, World world, String typeStr) {
        isSpawning = true;
        try {
            EntityType entityType = EntityType.valueOf(typeStr);
            if (!entityType.isSpawnable() || entityType.getEntityClass() == null) {
                return false;
            }
            Entity entity = world.spawn(data.getLocation(), entityType.getEntityClass(), CreatureSpawnEvent.SpawnReason.CUSTOM, e -> {
                e.getPersistentDataContainer().set(npcIdKey, PersistentDataType.INTEGER, data.getId());
                e.setInvulnerable(true);
                e.setSilent(true);
                e.setPersistent(false);
                e.setGravity(false);
                e.setCustomName(null);
                e.setCustomNameVisible(false);
                if (e instanceof LivingEntity) {
                    LivingEntity living = (LivingEntity) e;
                    living.setAI(false);
                    living.setCollidable(false);
                    living.setRemoveWhenFarAway(false);
                    living.setCanPickupItems(false);
                    applyLivingEquipment(data, living);
                }
            });
            if (entity == null || !entity.isValid()) {
                return false;
            }
            data.setEntityUuid(entity.getUniqueId());
            entityToNpcMap.put(entity.getUniqueId(), data.getId());
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            isSpawning = false;
        }
    }

    private void cleanupOrphanedEntities(NPCData data, World world) {
        if (data.getLocation() == null) return;
        try {
            for (Entity nearby : world.getNearbyEntities(data.getLocation(), 2.0, 4.0, 2.0)) {
                if (nearby instanceof Player) continue;
                Integer taggedId = nearby.getPersistentDataContainer().get(npcIdKey, PersistentDataType.INTEGER);
                if (taggedId != null && taggedId == data.getId()) {
                    entityToNpcMap.remove(nearby.getUniqueId());
                    nearby.remove();
                    continue;
                }
                // Remove old orphaned persistent TextDisplay holograms from previous reloads at the same spot
                if (nearby instanceof TextDisplay && !entityToNpcMap.containsKey(nearby.getUniqueId())) {
                    double dx = Math.abs(nearby.getLocation().getX() - data.getLocation().getX());
                    double dz = Math.abs(nearby.getLocation().getZ() - data.getLocation().getZ());
                    if (dx < 0.5 && dz < 0.5) {
                        nearby.remove();
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    private void stripLegacyArmorStandDisguise(NPCData data) {
        Map<String, ItemStack> equip = data.getEquipment();
        if (equip == null || equip.isEmpty()) return;
        ItemStack helm = equip.get("HELMET");
        ItemStack chest = equip.get("CHESTPLATE");
        ItemStack legs = equip.get("LEGGINGS");
        ItemStack boots = equip.get("BOOTS");
        if (helm != null && helm.getType() == Material.PLAYER_HEAD
                && chest != null && chest.getType() == Material.LEATHER_CHESTPLATE
                && legs != null && legs.getType() == Material.LEATHER_LEGGINGS
                && boots != null && boots.getType() == Material.LEATHER_BOOTS) {
            equip.remove("HELMET");
            equip.remove("CHESTPLATE");
            equip.remove("LEGGINGS");
            equip.remove("BOOTS");
        }
    }

    private void spawnArmorStandFallback(NPCData data, World world) {
        isSpawning = true;
        try {
            ArmorStand stand = world.spawn(data.getLocation(), ArmorStand.class, CreatureSpawnEvent.SpawnReason.CUSTOM, as -> {
                as.getPersistentDataContainer().set(npcIdKey, PersistentDataType.INTEGER, data.getId());
                as.setVisible(true);
                as.setArms(true);
                as.setBasePlate(false);
                as.setGravity(false);
                as.setInvulnerable(true);
                as.setPersistent(false);
                as.setRemoveWhenFarAway(false);
                as.setSmall(data.isSmall());
                as.setCustomNameVisible(false);
                as.setCanPickupItems(false);
                as.setCollidable(false);
            });
            data.setEntityUuid(stand.getUniqueId());
            entityToNpcMap.put(stand.getUniqueId(), data.getId());
            applyPose(data, stand);
            applyEquipment(data, stand);
        } finally {
            isSpawning = false;
        }
    }

    public boolean spawnMannequin(NPCData data, World world) {
        isSpawning = true;
        try {
            stripLegacyArmorStandDisguise(data);

            Mannequin mannequin = world.spawn(data.getLocation(), Mannequin.class, CreatureSpawnEvent.SpawnReason.CUSTOM, m -> {
                m.getPersistentDataContainer().set(npcIdKey, PersistentDataType.INTEGER, data.getId());
                m.setImmovable(true);
                m.setAI(false);
                m.setGravity(false);
                m.setInvulnerable(true);
                m.setCollidable(false);
                m.setSilent(true);
                m.setPersistent(false);
                m.setRemoveWhenFarAway(false);
                m.setCanPickupItems(false);
                m.setCustomName(null);
                m.setCustomNameVisible(false);
                m.setDescription(null);

                SkinParts.Mutable parts = m.getSkinParts();
                parts.setCapeEnabled(true);
                parts.setHatsEnabled(true);
                parts.setJacketEnabled(true);
                parts.setLeftPantsEnabled(true);
                parts.setRightPantsEnabled(true);
                parts.setLeftSleeveEnabled(true);
                parts.setRightSleeveEnabled(true);
                m.setSkinParts(parts);

                applyMannequinProfile(m, data);
                applyLivingEquipment(data, m);
                applyVisualAttributes(data, m);
            });

            if (mannequin == null || !mannequin.isValid()) {
                plugin.getLogger().warning("Mannequin spawn returned invalid entity for NPC #" + data.getId());
                return false;
            }

            data.setEntityUuid(mannequin.getUniqueId());
            entityToNpcMap.put(mannequin.getUniqueId(), data.getId());

            // If custom skinName is set without signed texture yet, fetch it asynchronously
            String skinName = data.getSkinName();
            String skinSig = data.getSkinSignature();
            if (skinName != null && !skinName.isEmpty()
                    && !"Steve".equalsIgnoreCase(skinName)
                    && !"CustomURL".equalsIgnoreCase(skinName)
                    && (skinSig == null || skinSig.isEmpty())) {
                plugin.getSkinManager().fetchSkin(skinName, prop -> {
                    if (prop != null && prop.value != null && !prop.value.isEmpty()) {
                        data.setSkinValue(prop.value);
                        data.setSkinSignature(prop.signature != null ? prop.signature : "");
                        Entity ent = data.getEntityUuid() != null ? Bukkit.getEntity(data.getEntityUuid()) : null;
                        if (ent instanceof Mannequin && ent.isValid()) {
                            applyMannequinProfile((Mannequin) ent, data);
                        }
                    }
                });
            }

            return true;
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not spawn Mannequin for NPC #" + data.getId() + ": " + t.getMessage());
            return false;
        } finally {
            isSpawning = false;
        }
    }

    public void applyMannequinProfile(Mannequin mannequin, NPCData data) {
        try {
            String skinName = data.getSkinName() != null && !data.getSkinName().trim().isEmpty()
                    ? data.getSkinName().trim()
                    : "Steve";
            String skinVal = data.getSkinValue();
            String skinSig = data.getSkinSignature();

            if (skinVal != null && !skinVal.isEmpty() && !"Steve".equalsIgnoreCase(skinName)) {
                String safeName = skinName.replaceAll("[^a-zA-Z0-9_]", "");
                if (safeName.isEmpty()) safeName = "MeryNPC";
                if (safeName.length() > 16) safeName = safeName.substring(0, 16);
                ProfileProperty texProp = (skinSig != null && !skinSig.isEmpty())
                        ? new ProfileProperty("textures", skinVal, skinSig)
                        : new ProfileProperty("textures", skinVal);
                ResolvableProfile profile = ResolvableProfile.resolvableProfile()
                        .name(safeName)
                        .uuid(UUID.randomUUID())
                        .addProperty(texProp)
                        .build();
                mannequin.setProfile(profile);
            } else if ("Steve".equalsIgnoreCase(skinName)) {
                mannequin.setProfile(Mannequin.defaultProfile());
            } else {
                String safeName = skinName.replaceAll("[^a-zA-Z0-9_]", "");
                if (safeName.isEmpty()) safeName = "MeryNPC";
                if (safeName.length() > 16) safeName = safeName.substring(0, 16);
                ResolvableProfile profile = ResolvableProfile.resolvableProfile()
                        .name(safeName)
                        .build();
                mannequin.setProfile(profile);
            }

            SkinParts.Mutable parts = mannequin.getSkinParts();
            parts.setCapeEnabled(true);
            parts.setHatsEnabled(true);
            parts.setJacketEnabled(true);
            parts.setLeftPantsEnabled(true);
            parts.setRightPantsEnabled(true);
            parts.setLeftSleeveEnabled(true);
            parts.setRightSleeveEnabled(true);
            mannequin.setSkinParts(parts);
            mannequin.setImmovable(true);
            mannequin.setDescription(null);
        } catch (Throwable t) {
            plugin.getLogger().warning("Error applying Mannequin skin: " + t.getMessage());
        }
    }

    public void applyLivingEquipment(NPCData data, LivingEntity living) {
        EntityEquipment eq = living.getEquipment();
        if (eq == null) return;
        Map<String, ItemStack> map = data.getEquipment();
        eq.setItemInMainHand(map.get("MAIN_HAND"));
        eq.setItemInOffHand(map.get("OFF_HAND"));
        eq.setHelmet(map.get("HELMET"));
        eq.setChestplate(map.get("CHESTPLATE"));
        eq.setLeggings(map.get("LEGGINGS"));
        eq.setBoots(map.get("BOOTS"));
    }

    public void despawnNPC(NPCData data) {
        // If Standalone NMS ServerPlayer
        if (data.getServerPlayer() != null) {
            for (UUID viewerId : data.getViewers()) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null && viewer.isOnline()) {
                    plugin.getNmsPlayerSpawner().destroyForPlayer(viewer, data.getServerPlayer());
                }
            }
            data.getViewers().clear();
            data.setServerPlayer(null);
        }

        // Remove hitbox or bukkit entity
        if (data.getEntityUuid() != null) {
            entityToNpcMap.remove(data.getEntityUuid());
            Entity entity = Bukkit.getEntity(data.getEntityUuid());
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
            data.setEntityUuid(null);
        }

        if (data.getHitboxEntityUuid() != null) {
            entityToNpcMap.remove(data.getHitboxEntityUuid());
            Entity entity = Bukkit.getEntity(data.getHitboxEntityUuid());
            if (entity != null && entity.isValid()) {
                entity.remove();
            }
            data.setHitboxEntityUuid(null);
        }

        despawnHologram(data);
    }

    public void despawnHologram(NPCData data) {
        if (data.getHologramEntityUuids() != null) {
            for (UUID uuid : data.getHologramEntityUuids()) {
                entityToNpcMap.remove(uuid);
                Entity e = Bukkit.getEntity(uuid);
                if (e != null && e.isValid()) {
                    e.remove();
                }
            }
            data.getHologramEntityUuids().clear();
        }
    }

    private List<String> buildRawHologramLines(NPCData data) {
        List<String> allLines = new ArrayList<>();
        if (data.getName() != null && !data.getName().trim().isEmpty()) {
            allLines.add(data.getName());
        }
        if (data.getHologramLines() != null && !data.getHologramLines().isEmpty()) {
            allLines.addAll(data.getHologramLines());
        }
        return allLines;
    }

    private String formatHologramBlock(NPCData data, List<String> rawLines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < rawLines.size(); i++) {
            sb.append(PlaceholderUtil.format(rawLines.get(i), null, data));
            if (i < rawLines.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public void spawnHologram(NPCData data) {
        despawnHologram(data);

        if (data.getLocation() == null || data.getLocation().getWorld() == null) return;

        World world = data.getLocation().getWorld();
        double height = 1.95 * data.getScale();
        Location holoLoc = data.getLocation().clone().add(0, height + data.getHologramOffset(), 0);

        List<String> allLines = buildRawHologramLines(data);

        // If no name and no hologram lines, do not spawn hologram at all!
        if (allLines.isEmpty()) return;

        // Modern 1.20+ TextDisplay with PlaceholderAPI support
        isSpawning = true;
        try {
            String formattedText = formatHologramBlock(data, allLines);

            TextDisplay display = world.spawn(holoLoc, TextDisplay.class, CreatureSpawnEvent.SpawnReason.CUSTOM, td -> {
                td.getPersistentDataContainer().set(npcIdKey, PersistentDataType.INTEGER, data.getId());
                td.setText(formattedText);
                td.setBillboard(Display.Billboard.CENTER);
                td.setDefaultBackground(false);
                td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
                td.setSeeThrough(false);
                td.setShadowed(true);
                td.setInvulnerable(true);
                td.setPersistent(false);
            });

            data.getHologramEntityUuids().add(display.getUniqueId());
            entityToNpcMap.put(display.getUniqueId(), data.getId());
            return;
        } catch (Throwable ignored) {
        } finally {
            isSpawning = false;
        }

        // Fallback: Invisible ArmorStands
        double lineSpacing = 0.28;
        Location currentLoc = holoLoc.clone().add(0, (allLines.size() - 1) * lineSpacing, 0);

        isSpawning = true;
        try {
            for (String line : allLines) {
                String formattedLine = PlaceholderUtil.format(line, null, data);
                ArmorStand lineStand = world.spawn(currentLoc, ArmorStand.class, CreatureSpawnEvent.SpawnReason.CUSTOM, as -> {
                    as.getPersistentDataContainer().set(npcIdKey, PersistentDataType.INTEGER, data.getId());
                    as.setMarker(true);
                    as.setVisible(false);
                    as.setGravity(false);
                    as.setInvulnerable(true);
                    as.setCustomName(formattedLine);
                    as.setCustomNameVisible(true);
                    as.setPersistent(false);
                });

                data.getHologramEntityUuids().add(lineStand.getUniqueId());
                entityToNpcMap.put(lineStand.getUniqueId(), data.getId());
                currentLoc.subtract(0, lineSpacing, 0);
            }
        } finally {
            isSpawning = false;
        }
    }

    /**
     * Live-updates PlaceholderAPI values on existing hologram entities in-place without flickering.
     */
    public void refreshHologramText(NPCData data) {
        if (data.getLocation() == null || data.getLocation().getWorld() == null) return;
        World world = data.getLocation().getWorld();
        int chunkX = data.getLocation().getBlockX() >> 4;
        int chunkZ = data.getLocation().getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) return;

        List<String> allLines = buildRawHologramLines(data);
        if (allLines.isEmpty()) {
            if (data.getHologramEntityUuids() != null && !data.getHologramEntityUuids().isEmpty()) {
                despawnHologram(data);
            }
            return;
        }

        if (data.getHologramEntityUuids() == null || data.getHologramEntityUuids().isEmpty()) {
            spawnHologram(data);
            return;
        }

        UUID firstUuid = data.getHologramEntityUuids().get(0);
        Entity firstEntity = Bukkit.getEntity(firstUuid);
        if (firstEntity == null || !firstEntity.isValid()) {
            spawnHologram(data);
            return;
        }

        if (firstEntity instanceof TextDisplay) {
            TextDisplay td = (TextDisplay) firstEntity;
            if (td.isDefaultBackground()) {
                td.setDefaultBackground(false);
            }
            td.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            String newText = formatHologramBlock(data, allLines);
            if (!newText.equals(td.getText())) {
                td.setText(newText);
            }
            return;
        }

        // ArmorStand fallback refresh
        for (int i = 0; i < data.getHologramEntityUuids().size() && i < allLines.size(); i++) {
            Entity e = Bukkit.getEntity(data.getHologramEntityUuids().get(i));
            if (e instanceof ArmorStand && e.isValid()) {
                String newLine = PlaceholderUtil.format(allLines.get(i), null, data);
                if (!newLine.equals(e.getCustomName())) {
                    e.setCustomName(newLine);
                }
            }
        }
    }

    public void refreshAllHolograms() {
        for (NPCData data : npcs.values()) {
            refreshHologramText(data);
        }
    }

    public void updateHologram(NPCData data) {
        spawnHologram(data);
    }

    public void applyPose(NPCData data, ArmorStand stand) {
        NPCPose pose = data.getPose();
        if (pose == null) return;

        stand.setHeadPose(pose.getHead());
        stand.setBodyPose(pose.getBody());
        stand.setLeftArmPose(pose.getLeftArm());
        stand.setRightArmPose(pose.getRightArm());
        stand.setLeftLegPose(pose.getLeftLeg());
        stand.setRightLegPose(pose.getRightLeg());
    }

    public void applyEquipment(NPCData data, ArmorStand stand) {
        EntityEquipment eq = stand.getEquipment();
        if (eq == null) return;

        if (data.getEquipment().isEmpty() || !data.getEquipment().containsKey("HELMET")) {
            plugin.getSkinManager().applyDefaultSteveEquipment(data);
        }

        applyLivingEquipment(data, stand);
    }

    public void updateEquipment(NPCData data) {
        // If Standalone NMS Player
        if (data.getServerPlayer() != null) {
            for (UUID viewerId : data.getViewers()) {
                Player viewer = Bukkit.getPlayer(viewerId);
                if (viewer != null && viewer.isOnline()) {
                    plugin.getNmsPlayerSpawner().updateEquipmentForPlayer(viewer, data.getServerPlayer(), data);
                }
            }
        }

        // If Mannequin or ArmorStand
        if (data.getEntityUuid() != null) {
            Entity entity = Bukkit.getEntity(data.getEntityUuid());
            if (entity instanceof ArmorStand) {
                applyEquipment(data, (ArmorStand) entity);
            } else if (entity instanceof LivingEntity) {
                applyLivingEquipment(data, (LivingEntity) entity);
            }
        }
    }

    public void updatePose(NPCData data) {
        if (data.getEntityUuid() != null) {
            Entity entity = Bukkit.getEntity(data.getEntityUuid());
            if (entity instanceof ArmorStand) {
                applyPose(data, (ArmorStand) entity);
            } else if (entity instanceof Mannequin && data.getPose() != null) {
                Mannequin m = (Mannequin) entity;
                String preset = data.getPose().getPresetName();
                if ("SITTING".equalsIgnoreCase(preset)) {
                    m.setPose(Pose.SITTING);
                } else if ("CROUCHING".equalsIgnoreCase(preset) || "SNEAKING".equalsIgnoreCase(preset) || "DEFEND".equalsIgnoreCase(preset)) {
                    m.setPose(Pose.SNEAKING);
                } else {
                    m.setPose(Pose.STANDING);
                }
            }
        }
    }

    public void updateSkin(NPCData data) {
        if (data.getEntityUuid() != null) {
            Entity entity = Bukkit.getEntity(data.getEntityUuid());
            if (entity instanceof Mannequin && entity.isValid()) {
                applyMannequinProfile((Mannequin) entity, data);
                return;
            }
        }

        spawnNPC(data);
    }

    public void updateVisuals(NPCData data) {
        if (data.getEntityUuid() != null) {
            Entity entity = Bukkit.getEntity(data.getEntityUuid());
            if (entity != null && entity.isValid()) {
                applyVisualAttributes(data, entity);
            }
        }
        updateHologram(data);
    }

    public void teleportNPC(NPCData data, Location newLoc) {
        data.setLocation(newLoc);
        spawnNPC(data);
    }

    public void respawnAll() {
        for (NPCData data : npcs.values()) {
            spawnNPC(data);
        }
    }

    public void checkAndSpawnPendingWorlds() {
        for (NPCData data : npcs.values()) {
            Entity existing = data.getEntityUuid() != null ? Bukkit.getEntity(data.getEntityUuid()) : null;
            if (existing != null && existing.isValid()) continue;
            Location loc = data.getLocation();
            if (loc != null && loc.getWorld() != null) {
                spawnNPC(data);
            }
        }
    }

    public void despawnAll() {
        for (NPCData data : npcs.values()) {
            despawnNPC(data);
        }
    }
}
