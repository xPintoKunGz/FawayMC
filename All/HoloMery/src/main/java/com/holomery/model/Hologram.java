package com.holomery.model;

import com.holomery.HoloMeryPlugin;
import com.holomery.color.ColorParser;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * High-performance modern Hologram container managing multi-page Display Entities,
 * items, blocks, mobs, 360 spin animation, and interactive click-to-flip navigation.
 */
public class Hologram {

    private final String id;
    private Location location;
    private double lineSpacing = 0.30;

    // Multi-page system
    private final List<HoloPage> pages = new ArrayList<>();
    private int activePageIndex = 0;
    private boolean clickPageSwitch = true;
    private int autoPageIntervalSeconds = 0;

    // Visual attributes
    private BillboardMode billboard = BillboardMode.CENTER;
    private boolean shadow = true;
    private boolean seeThrough = false;
    private boolean transparentBackground = false;
    private Color backgroundColor = null;
    private float scale = 1.0f;
    private TextDisplay.TextAlignment alignment = TextDisplay.TextAlignment.CENTER;
    private float viewRange = 1.0f;

    // Animation: 360 degree spin
    private boolean spin = false;
    private float spinAngle = 0f;

    private boolean spawned = false;
    private Interaction interactionEntity;

    public Hologram(String id, Location location) {
        this.id = id;
        this.location = location.clone();
        this.pages.add(new HoloPage());
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location.clone();
    }

    public void setLocation(Location location) {
        this.location = location.clone();
        if (spawned) {
            respawn();
        }
    }

    public double getLineSpacing() {
        return lineSpacing;
    }

    public void setLineSpacing(double lineSpacing) {
        this.lineSpacing = Math.max(0.05, Math.min(5.0, lineSpacing));
        if (spawned) {
            respawn();
        }
    }

    // --- Page Management ---

    public List<HoloPage> getPages() {
        return Collections.unmodifiableList(pages);
    }

    public int getPageCount() {
        return pages.size();
    }

    public int getActivePageIndex() {
        return activePageIndex;
    }

    public HoloPage getActivePage() {
        if (activePageIndex >= 0 && activePageIndex < pages.size()) {
            return pages.get(activePageIndex);
        }
        if (pages.isEmpty()) {
            pages.add(new HoloPage());
        }
        return pages.get(0);
    }

    public HoloPage getPage(int index) {
        if (index >= 0 && index < pages.size()) {
            return pages.get(index);
        }
        return null;
    }

    public void addPage(HoloPage page) {
        pages.add(page);
    }

    public HoloPage createNewPage() {
        HoloPage newPage = new HoloPage();
        pages.add(newPage);
        return newPage;
    }

    public boolean removePage(int index) {
        if (pages.size() <= 1) return false;
        if (index >= 0 && index < pages.size()) {
            HoloPage removed = pages.remove(index);
            if (activePageIndex >= pages.size()) {
                activePageIndex = pages.size() - 1;
            }
            if (spawned) {
                removed.despawn();
                respawn();
            }
            return true;
        }
        return false;
    }

    public void nextPage(Player flipper) {
        if (pages.size() <= 1) return;
        getActivePage().despawn();
        activePageIndex = (activePageIndex + 1) % pages.size();
        getActivePage().spawn(HoloMeryPlugin.getInstance(), location, this);

        if (flipper != null) {
            try {
                flipper.playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);
                String msg = ColorParser.colorize("<gradient:#00F2FE:#4FACFE>&l✦ HoloMery</gradient> &8» &fPage &b" + (activePageIndex + 1) + "&7/&b" + pages.size());
                flipper.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            } catch (Exception ignored) {}
        }
    }

    public void prevPage(Player flipper) {
        if (pages.size() <= 1) return;
        getActivePage().despawn();
        activePageIndex = (activePageIndex - 1 + pages.size()) % pages.size();
        getActivePage().spawn(HoloMeryPlugin.getInstance(), location, this);

        if (flipper != null) {
            try {
                flipper.playSound(location, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.2f);
                String msg = ColorParser.colorize("<gradient:#00F2FE:#4FACFE>&l✦ HoloMery</gradient> &8» &fPage &b" + (activePageIndex + 1) + "&7/&b" + pages.size());
                flipper.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(msg));
            } catch (Exception ignored) {}
        }
    }

    public void setPage(int index) {
        if (index >= 0 && index < pages.size() && index != activePageIndex) {
            getActivePage().despawn();
            activePageIndex = index;
            if (spawned) {
                getActivePage().spawn(HoloMeryPlugin.getInstance(), location, this);
            }
        }
    }

    public boolean isClickPageSwitch() {
        return clickPageSwitch;
    }

    public void setClickPageSwitch(boolean clickPageSwitch) {
        this.clickPageSwitch = clickPageSwitch;
    }

    public int getAutoPageIntervalSeconds() {
        return autoPageIntervalSeconds;
    }

    public void setAutoPageIntervalSeconds(int autoPageIntervalSeconds) {
        this.autoPageIntervalSeconds = Math.max(0, autoPageIntervalSeconds);
    }

    // --- Line Helpers (operate on active page) ---

    public List<HoloLine> getLines() {
        return getActivePage().getLines();
    }

    public int getLineCount() {
        return getActivePage().getLineCount();
    }

    public HoloLine getLine(int index) {
        return getActivePage().getLine(index);
    }

    public void addLine(String text) {
        getActivePage().addLine(text);
        if (spawned) {
            respawn();
        }
    }

    public void insertLine(int index, String text) {
        getActivePage().insertLine(index, text);
        if (spawned) {
            respawn();
        }
    }

    public boolean setLine(int index, String text) {
        boolean ok = getActivePage().setLine(index, text);
        if (ok && spawned) {
            HoloLine line = getActivePage().getLine(index);
            if (line != null) {
                line.updateDisplay(this, null);
            }
        }
        return ok;
    }

    public boolean removeLine(int index) {
        boolean ok = getActivePage().removeLine(index);
        if (ok && spawned) {
            respawn();
        }
        return ok;
    }

    public boolean swapLines(int index1, int index2) {
        boolean ok = getActivePage().swapLines(index1, index2);
        if (ok && spawned) {
            respawn();
        }
        return ok;
    }

    public boolean moveLine(int fromIndex, int toIndex) {
        boolean ok = getActivePage().moveLine(fromIndex, toIndex);
        if (ok && spawned) {
            respawn();
        }
        return ok;
    }

    public void clearLines() {
        getActivePage().clearLines();
        if (spawned) {
            respawn();
        }
    }

    // --- Click Actions ---

    public List<String> getClickActions() {
        return getActivePage().getClickActions();
    }

    public void addClickAction(String action) {
        getActivePage().addClickAction(action);
    }

    public boolean removeClickAction(int index) {
        return getActivePage().removeClickAction(index);
    }

    public void clearClickActions() {
        getActivePage().clearClickActions();
    }

    // --- Spin Animation ---

    public boolean isSpin() {
        return spin;
    }

    public void setSpin(boolean spin) {
        this.spin = spin;
    }

    public void tickSpin() {
        if (!spawned || !spin) return;
        spinAngle = (float) ((spinAngle + 0.05f) % (Math.PI * 2));
        for (HoloLine line : getActivePage().getLines()) {
            line.applySpin(this, spinAngle);
        }
    }

    // --- Lifecycle ---

    public void spawn() {
        if (location == null || location.getWorld() == null) return;
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
            return;
        }

        HoloMeryPlugin plugin = HoloMeryPlugin.getInstance();
        getActivePage().spawn(plugin, location, this);

        spawnInteractionEntity(plugin);
        spawned = true;
    }

    private void spawnInteractionEntity(HoloMeryPlugin plugin) {
        despawnInteractionEntity();
        if (location == null || location.getWorld() == null) return;

        try {
            int lineCount = Math.max(1, getActivePage().getLineCount());
            float height = (float) (lineCount * lineSpacing * scale) + 0.5f;
            Location interLoc = location.clone().subtract(0, height - 0.5, 0);

            Interaction inter = (Interaction) location.getWorld().spawnEntity(interLoc, EntityType.INTERACTION);
            inter.setInteractionWidth(1.2f * scale);
            inter.setInteractionHeight(height);
            inter.setResponsive(true);
            inter.setMetadata("HoloMery-Id", new FixedMetadataValue(plugin, this.id));
            inter.setMetadata("HoloMery-Interact", new FixedMetadataValue(plugin, true));
            this.interactionEntity = inter;
        } catch (Exception ignored) {}
    }

    private void despawnInteractionEntity() {
        if (interactionEntity != null && interactionEntity.isValid()) {
            interactionEntity.remove();
        }
        interactionEntity = null;
    }

    public void despawn() {
        for (HoloPage page : pages) {
            page.despawn();
        }
        despawnInteractionEntity();
        spawned = false;
    }

    public void respawn() {
        despawn();
        spawn();
    }

    public void update(Player player) {
        if (!spawned) return;
        getActivePage().update(this);
    }

    public void nudge(double dx, double dy, double dz) {
        this.location.add(dx, dy, dz);
        if (spawned) {
            respawn();
        }
    }

    public boolean isSpawned() {
        return spawned;
    }

    // --- Styling Getters & Setters ---

    public BillboardMode getBillboard() {
        return billboard;
    }

    public void setBillboard(BillboardMode billboard) {
        this.billboard = billboard;
        if (spawned) {
            getActivePage().update(this);
        }
    }

    public boolean isShadow() {
        return shadow;
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
        if (spawned) {
            getActivePage().update(this);
        }
    }

    public boolean isSeeThrough() {
        return seeThrough;
    }

    public void setSeeThrough(boolean seeThrough) {
        this.seeThrough = seeThrough;
        if (spawned) {
            getActivePage().update(this);
        }
    }

    public boolean isTransparentBackground() {
        return transparentBackground;
    }

    public void setTransparentBackground(boolean transparentBackground) {
        this.transparentBackground = transparentBackground;
        if (transparentBackground) {
            this.backgroundColor = null;
        }
        if (spawned) {
            getActivePage().update(this);
        }
    }

    public void toggleBackground() {
        setTransparentBackground(!this.transparentBackground);
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
        this.transparentBackground = false;
        if (spawned) {
            getActivePage().update(this);
        }
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.1f, Math.min(10.0f, scale));
        if (spawned) {
            respawn();
        }
    }

    public TextDisplay.TextAlignment getAlignment() {
        return alignment;
    }

    public void setAlignment(TextDisplay.TextAlignment alignment) {
        this.alignment = alignment;
        if (spawned) {
            getActivePage().update(this);
        }
    }

    public float getViewRange() {
        return viewRange;
    }

    public void setViewRange(float viewRange) {
        this.viewRange = Math.max(0.1f, Math.min(10.0f, viewRange));
        if (spawned) {
            getActivePage().update(this);
        }
    }
}
