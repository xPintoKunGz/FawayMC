package com.bedwars.integration;

import com.holomery.manager.HologramManager;
import com.holomery.model.Hologram;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.List;

public class HoloMeryHook {

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("HoloMery");
    }

    public static boolean createOrUpdateHologram(String id, Location loc, List<String> lines) {
        if (!isAvailable()) return false;
        try {
            HologramManager hm = HologramManager.getInstance();
            if (hm == null) return false;

            Hologram holo = hm.getHologram(id);
            if (holo == null) {
                holo = hm.createHologram(id, loc);
                if (holo == null) return false;
            } else {
                holo.clearLines();
            }

            for (String line : lines) {
                holo.addLine(line);
            }

            if (!holo.isSpawned()) {
                holo.spawn();
            }
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean updateLine(String id, int lineIndex, String newLine) {
        if (!isAvailable()) return false;
        try {
            HologramManager hm = HologramManager.getInstance();
            if (hm == null) return false;

            Hologram holo = hm.getHologram(id);
            if (holo != null && lineIndex < holo.getLineCount()) {
                holo.setLine(lineIndex, newLine);
                return true;
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    public static void deleteHologram(String id) {
        if (!isAvailable()) return;
        try {
            HologramManager hm = HologramManager.getInstance();
            if (hm != null && hm.exists(id)) {
                hm.deleteHologram(id);
            }
        } catch (Throwable ignored) {}
    }

    public static void deleteAllBedwarsHolograms() {
        if (!isAvailable()) return;
        try {
            HologramManager hm = HologramManager.getInstance();
            if (hm == null) return;
            java.util.List<String> toDelete = new java.util.ArrayList<>();
            for (Hologram holo : hm.getAllHolograms()) {
                if (holo.getId().startsWith("bw_")) {
                    toDelete.add(holo.getId());
                }
            }
            for (String id : toDelete) {
                hm.deleteHologram(id);
            }
        } catch (Throwable ignored) {}
    }
}

