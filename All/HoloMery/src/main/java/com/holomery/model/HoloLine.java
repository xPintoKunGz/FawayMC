package com.holomery.model;

import com.holomery.color.ColorParser;
import com.holomery.integration.PapiHook;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.UUID;

/**
 * High-performance line component supporting TextDisplay, ItemDisplay, BlockDisplay, and Mob entities.
 * Features bright lighting, robust JOML rotation matrices, and smooth 360 spin animation.
 */
public class HoloLine {

    private String rawText;
    private UUID entityId;
    private Entity cachedEntity;
    private LineType type;

    public HoloLine(String rawText) {
        this.rawText = rawText;
        this.type = LineType.detect(rawText);
    }

    public String getRawText() {
        return rawText;
    }

    public void setRawText(String rawText) {
        this.rawText = rawText;
        this.type = LineType.detect(rawText);
    }

    public LineType getType() {
        return type;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public Entity getCachedEntity() {
        if (cachedEntity != null && cachedEntity.isValid()) {
            return cachedEntity;
        }
        return null;
    }

    /**
     * Spawns the appropriate entity (TextDisplay, ItemDisplay, BlockDisplay, or Mob).
     */
    public Entity spawn(Plugin plugin, Location location, Hologram hologram, int lineIndex) {
        despawn();

        if (location == null || location.getWorld() == null) {
            return null;
        }

        this.type = LineType.detect(rawText);

        switch (type) {
            case ITEM -> spawnItemDisplay(plugin, location, hologram, lineIndex);
            case BLOCK -> spawnBlockDisplay(plugin, location, hologram, lineIndex);
            case MOB -> spawnMobEntity(plugin, location, hologram, lineIndex);
            case TEXT -> spawnTextDisplay(plugin, location, hologram, lineIndex);
        }

        return cachedEntity;
    }

    private void spawnTextDisplay(Plugin plugin, Location location, Hologram hologram, int lineIndex) {
        TextDisplay display = (TextDisplay) location.getWorld().spawnEntity(location, EntityType.TEXT_DISPLAY);
        this.cachedEntity = display;
        this.entityId = display.getUniqueId();

        tagEntity(display, plugin, hologram, lineIndex);
        applyTextAttributes(display, hologram);
    }

    private void spawnItemDisplay(Plugin plugin, Location location, Hologram hologram, int lineIndex) {
        ItemDisplay display = (ItemDisplay) location.getWorld().spawnEntity(location, EntityType.ITEM_DISPLAY);
        this.cachedEntity = display;
        this.entityId = display.getUniqueId();

        tagEntity(display, plugin, hologram, lineIndex);

        String matName = LineType.extractValue(rawText).toUpperCase().replace("MINECRAFT:", "").trim();
        Material mat = Material.matchMaterial(matName);
        if (mat == null) mat = Material.DIAMOND;

        display.setItemStack(new ItemStack(mat));
        display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.GROUND);
        display.setBillboard(hologram.getBillboard().getBukkitBillboard());
        display.setViewRange(hologram.getViewRange());
        display.setBrightness(new Display.Brightness(15, 15));
        display.setInterpolationDuration(2);

        float scale = hologram.getScale() * 0.9f;
        Transformation transformation = new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0f, 0f, 1f, 0f),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0f, 0f, 1f, 0f)
        );
        display.setTransformation(transformation);
    }

    private void spawnBlockDisplay(Plugin plugin, Location location, Hologram hologram, int lineIndex) {
        BlockDisplay display = (BlockDisplay) location.getWorld().spawnEntity(location, EntityType.BLOCK_DISPLAY);
        this.cachedEntity = display;
        this.entityId = display.getUniqueId();

        tagEntity(display, plugin, hologram, lineIndex);

        String matName = LineType.extractValue(rawText).toUpperCase().replace("MINECRAFT:", "").trim();
        Material mat = Material.matchMaterial(matName);
        if (mat == null || !mat.isBlock()) mat = Material.BEACON;

        display.setBlock(mat.createBlockData());
        display.setBillboard(hologram.getBillboard().getBukkitBillboard());
        display.setViewRange(hologram.getViewRange());
        display.setBrightness(new Display.Brightness(15, 15));
        display.setInterpolationDuration(2);

        float scale = hologram.getScale() * 0.65f;
        float offset = -scale / 2f;
        Transformation transformation = new Transformation(
                new Vector3f(offset, offset, offset),
                new AxisAngle4f(0f, 0f, 1f, 0f),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0f, 0f, 1f, 0f)
        );
        display.setTransformation(transformation);
    }

    private void spawnMobEntity(Plugin plugin, Location location, Hologram hologram, int lineIndex) {
        String entityName = LineType.extractValue(rawText).toUpperCase().replace("MINECRAFT:", "").trim();
        EntityType entityType;
        try {
            entityType = EntityType.valueOf(entityName);
        } catch (IllegalArgumentException e) {
            entityType = EntityType.ALLAY;
        }

        try {
            Entity mob = location.getWorld().spawnEntity(location, entityType);
            this.cachedEntity = mob;
            this.entityId = mob.getUniqueId();

            tagEntity(mob, plugin, hologram, lineIndex);

            if (mob instanceof Mob livingMob) {
                livingMob.setAI(false);
                livingMob.setSilent(true);
                livingMob.setInvulnerable(true);
                livingMob.setCollidable(false);
                livingMob.setRemoveWhenFarAway(false);
            }
            mob.setGravity(false);
            mob.setInvulnerable(true);
            mob.setSilent(true);

            // Apply size scaling
            applyMobScale(mob, hologram.getScale());
        } catch (Exception e) {
            // Fallback to text if mob cannot be spawned
            spawnTextDisplay(plugin, location, hologram, lineIndex);
        }
    }

    private void applyMobScale(Entity mob, float scale) {
        // Modern 1.20.5+ / 1.21+ GENERIC_SCALE
        try {
            Attribute scaleAttr = Attribute.valueOf("GENERIC_SCALE");
            if (mob instanceof LivingEntity living) {
                AttributeInstance instance = living.getAttribute(scaleAttr);
                if (instance != null) {
                    instance.setBaseValue(scale);
                }
            }
        } catch (Throwable ignored) {}

        if (mob instanceof Slime slime) {
            slime.setSize(Math.max(1, Math.min(10, Math.round(scale * 2))));
        } else if (mob instanceof Phantom phantom) {
            phantom.setSize(Math.max(1, Math.min(10, Math.round(scale * 2))));
        }
    }

    private void tagEntity(Entity entity, Plugin plugin, Hologram hologram, int lineIndex) {
        entity.setMetadata("HoloMery-Id", new FixedMetadataValue(plugin, hologram.getId()));
        entity.setMetadata("HoloMery-Line", new FixedMetadataValue(plugin, lineIndex));
        entity.setPersistent(true);
    }

    public void updateDisplay(Hologram hologram, Player targetPlayer) {
        Entity entity = getCachedEntity();
        if (entity == null) return;

        if (entity instanceof TextDisplay textDisplay) {
            String parsed = rawText;
            if (PapiHook.isHooked()) {
                parsed = PapiHook.parse(targetPlayer, parsed);
            }
            textDisplay.setText(ColorParser.colorize(parsed));
            applyTextAttributes(textDisplay, hologram);
        } else if (entity instanceof ItemDisplay itemDisplay) {
            itemDisplay.setBillboard(hologram.getBillboard().getBukkitBillboard());
            itemDisplay.setViewRange(hologram.getViewRange());
            itemDisplay.setBrightness(new Display.Brightness(15, 15));
            float scale = hologram.getScale() * 0.9f;
            itemDisplay.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 1f, 0f),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0f, 0f, 1f, 0f)
            ));
        } else if (entity instanceof BlockDisplay blockDisplay) {
            blockDisplay.setBillboard(hologram.getBillboard().getBukkitBillboard());
            blockDisplay.setViewRange(hologram.getViewRange());
            blockDisplay.setBrightness(new Display.Brightness(15, 15));
            float scale = hologram.getScale() * 0.65f;
            float offset = -scale / 2f;
            blockDisplay.setTransformation(new Transformation(
                    new Vector3f(offset, offset, offset),
                    new AxisAngle4f(0f, 0f, 1f, 0f),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0f, 0f, 1f, 0f)
            ));
        } else if (entity instanceof LivingEntity) {
            applyMobScale(entity, hologram.getScale());
        }
    }

    /**
     * Smoothly rotates the line 360 degrees around the Y axis.
     */
    public void applySpin(Hologram hologram, float angleRadians) {
        Entity entity = getCachedEntity();
        if (entity == null) return;

        if (entity instanceof ItemDisplay itemDisplay) {
            float scale = hologram.getScale() * 0.9f;
            itemDisplay.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(angleRadians, 0f, 1f, 0f),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0f, 0f, 1f, 0f)
            ));
        } else if (entity instanceof BlockDisplay blockDisplay) {
            float scale = hologram.getScale() * 0.65f;
            float offset = -scale / 2f;
            blockDisplay.setTransformation(new Transformation(
                    new Vector3f(offset, offset, offset),
                    new AxisAngle4f(angleRadians, 0f, 1f, 0f),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f(0f, 0f, 1f, 0f)
            ));
        } else if (entity instanceof LivingEntity mob) {
            Location loc = mob.getLocation();
            float degrees = (float) Math.toDegrees(angleRadians) % 360f;
            loc.setYaw(degrees);
            mob.teleport(loc);
        }
    }

    private void applyTextAttributes(TextDisplay display, Hologram hologram) {
        display.setBillboard(hologram.getBillboard().getBukkitBillboard());

        String parsed = rawText;
        if (PapiHook.isHooked()) {
            parsed = PapiHook.parse(null, parsed);
        }
        display.setText(ColorParser.colorize(parsed));

        display.setShadowed(hologram.isShadow());
        display.setSeeThrough(hologram.isSeeThrough());
        display.setAlignment(hologram.getAlignment());
        display.setViewRange(hologram.getViewRange());

        if (hologram.isTransparentBackground()) {
            display.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        } else if (hologram.getBackgroundColor() != null) {
            display.setBackgroundColor(hologram.getBackgroundColor());
        } else {
            display.setBackgroundColor(null);
        }

        float scale = hologram.getScale();
        display.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0f, 0f, 1f, 0f),
                new Vector3f(scale, scale, scale),
                new AxisAngle4f(0f, 0f, 1f, 0f)
        ));
    }

    public void despawn() {
        if (cachedEntity != null && cachedEntity.isValid()) {
            cachedEntity.remove();
        }
        cachedEntity = null;
    }
}
