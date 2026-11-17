package com.bedwars.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationUtil {

    public static String serialize(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + ";" +
                String.format("%.2f", loc.getX()) + ";" +
                String.format("%.2f", loc.getY()) + ";" +
                String.format("%.2f", loc.getZ()) + ";" +
                String.format("%.2f", loc.getYaw()) + ";" +
                String.format("%.2f", loc.getPitch());
    }

    public static Location deserialize(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        String[] parts = str.split(";");
        if (parts.length < 4) return null;

        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;

        try {
            double x = Double.parseDouble(parts[1].replace(",", "."));
            double y = Double.parseDouble(parts[2].replace(",", "."));
            double z = Double.parseDouble(parts[3].replace(",", "."));
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4].replace(",", ".")) : 0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5].replace(",", ".")) : 0f;
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String serializeBlock(Location loc) {
        if (loc == null || loc.getWorld() == null) return "";
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    public static Location deserializeBlock(String str) {
        if (str == null || str.trim().isEmpty()) return null;
        String[] parts = str.split(";");
        if (parts.length < 4) return null;
        World world = Bukkit.getWorld(parts[0]);
        if (world == null) return null;
        try {
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

