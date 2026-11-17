package com.holomery.model;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single page within a multi-page Hologram.
 */
public class HoloPage {

    private final List<HoloLine> lines = new ArrayList<>();
    private final List<String> clickActions = new ArrayList<>();

    public HoloPage() {
    }

    public HoloPage(List<String> rawLines) {
        if (rawLines != null) {
            for (String line : rawLines) {
                lines.add(new HoloLine(line));
            }
        }
    }

    public List<String> getClickActions() {
        return Collections.unmodifiableList(clickActions);
    }

    public void addClickAction(String action) {
        if (action != null && !action.trim().isEmpty()) {
            clickActions.add(action.trim());
        }
    }

    public boolean removeClickAction(int index) {
        if (index >= 0 && index < clickActions.size()) {
            clickActions.remove(index);
            return true;
        }
        return false;
    }

    public void clearClickActions() {
        clickActions.clear();
    }

    public List<HoloLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

    public int getLineCount() {
        return lines.size();
    }

    public HoloLine getLine(int index) {
        if (index >= 0 && index < lines.size()) {
            return lines.get(index);
        }
        return null;
    }

    public void addLine(String text) {
        lines.add(new HoloLine(text));
    }

    public void insertLine(int index, String text) {
        if (index < 0) index = 0;
        if (index > lines.size()) index = lines.size();
        lines.add(index, new HoloLine(text));
    }

    public boolean setLine(int index, String text) {
        if (index >= 0 && index < lines.size()) {
            lines.get(index).setRawText(text);
            return true;
        }
        return false;
    }

    public boolean removeLine(int index) {
        if (index >= 0 && index < lines.size()) {
            HoloLine removed = lines.remove(index);
            removed.despawn();
            return true;
        }
        return false;
    }

    public boolean swapLines(int index1, int index2) {
        if (index1 >= 0 && index1 < lines.size() && index2 >= 0 && index2 < lines.size() && index1 != index2) {
            Collections.swap(lines, index1, index2);
            return true;
        }
        return false;
    }

    public boolean moveLine(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < lines.size() && toIndex >= 0 && toIndex < lines.size() && fromIndex != toIndex) {
            HoloLine moved = lines.remove(fromIndex);
            lines.add(toIndex, moved);
            return true;
        }
        return false;
    }

    public void clearLines() {
        for (HoloLine line : lines) {
            line.despawn();
        }
        lines.clear();
    }

    public void spawn(Plugin plugin, Location startLoc, Hologram hologram) {
        Location currentLoc = startLoc.clone();
        for (int i = 0; i < lines.size(); i++) {
            HoloLine line = lines.get(i);
            line.spawn(plugin, currentLoc, hologram, i);

            double spacing = hologram.getLineSpacing() * hologram.getScale();
            if (line.getType() == LineType.BLOCK || line.getType() == LineType.MOB) {
                spacing *= 1.6;
            } else if (line.getType() == LineType.ITEM) {
                spacing *= 1.2;
            }
            currentLoc.subtract(0, spacing, 0);
        }
    }

    public void despawn() {
        for (HoloLine line : lines) {
            line.despawn();
        }
    }

    public void update(Hologram hologram) {
        for (HoloLine line : lines) {
            line.updateDisplay(hologram, null);
        }
    }
}
