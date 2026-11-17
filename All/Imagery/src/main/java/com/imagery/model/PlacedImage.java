package com.imagery.model;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.serialization.ConfigurationSerializable;

import java.util.*;

/**
 * Represents an active placed image or interactive album in the Minecraft world.
 */
public class PlacedImage implements ConfigurationSerializable {

    private final String id;
    private final String fileName;
    private final String worldName;
    private final int originX;
    private final int originY;
    private final int originZ;
    private final BlockFace facing;
    private int width;
    private int height;
    private boolean invisibleFrame;
    private boolean glowing;
    private final List<Integer> mapIds;
    private final List<UUID> frameUuids;

    // Album and Page Changing additions
    private String albumId;
    private final List<String> pages;
    private int currentPage = 0;
    private boolean clickToNextPage = true;
    private int autoRotateSeconds = 0;

    // Attached click commands
    private final List<ClickCommand> clickCommands;

    public PlacedImage(String id, String fileName, String worldName, int originX, int originY, int originZ,
                       BlockFace facing, int width, int height, boolean invisibleFrame, boolean glowing) {
        this.id = id;
        this.fileName = fileName;
        this.worldName = worldName;
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.facing = facing;
        this.width = width;
        this.height = height;
        this.invisibleFrame = invisibleFrame;
        this.glowing = glowing;
        this.mapIds = new ArrayList<>();
        this.frameUuids = new ArrayList<>();
        this.pages = new ArrayList<>();
        if (fileName != null && !fileName.isEmpty()) {
            this.pages.add(fileName);
        }
        this.clickCommands = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getFileName() {
        return fileName;
    }

    public String getCurrentFileName() {
        if (pages.isEmpty()) return fileName;
        if (currentPage < 0 || currentPage >= pages.size()) {
            currentPage = 0;
        }
        return pages.get(currentPage);
    }

    public String getWorldName() {
        return worldName;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldName);
    }

    public int getOriginX() {
        return originX;
    }

    public int getOriginY() {
        return originY;
    }

    public int getOriginZ() {
        return originZ;
    }

    public Location getOriginLocation() {
        World w = getWorld();
        if (w == null) return null;
        return new Location(w, originX + 0.5, originY + 0.5, originZ + 0.5);
    }

    public Location getCenterLocation() {
        World w = getWorld();
        if (w == null) return null;

        double cx = originX + 0.5;
        double cy = originY - (height / 2.0) + 0.5;
        double cz = originZ + 0.5;

        switch (facing) {
            case SOUTH -> cx += (width / 2.0) - 0.5;
            case NORTH -> cx -= (width / 2.0) - 0.5;
            case EAST -> cz -= (width / 2.0) - 0.5;
            case WEST -> cz += (width / 2.0) - 0.5;
            default -> {}
        }

        return new Location(w, cx, cy, cz);
    }

    public Location getViewLocation() {
        Location center = getCenterLocation();
        if (center == null) return null;

        double distance = Math.max(3.0, Math.max(width, height) * 0.9);
        Location viewLoc = center.clone();

        switch (facing) {
            case SOUTH -> {
                viewLoc.add(0, 0, distance);
                viewLoc.setYaw(180);
            }
            case NORTH -> {
                viewLoc.add(0, 0, -distance);
                viewLoc.setYaw(0);
            }
            case EAST -> {
                viewLoc.add(distance, 0, 0);
                viewLoc.setYaw(90);
            }
            case WEST -> {
                viewLoc.add(-distance, 0, 0);
                viewLoc.setYaw(-90);
            }
            default -> {}
        }
        viewLoc.setPitch(0);
        return viewLoc;
    }

    public BlockFace getFacing() {
        return facing;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean isInvisibleFrame() {
        return invisibleFrame;
    }

    public void setInvisibleFrame(boolean invisibleFrame) {
        this.invisibleFrame = invisibleFrame;
    }

    public boolean isGlowing() {
        return glowing;
    }

    public void setGlowing(boolean glowing) {
        this.glowing = glowing;
    }

    public List<Integer> getMapIds() {
        return mapIds;
    }

    public List<UUID> getFrameUuids() {
        return frameUuids;
    }

    public String getAlbumId() {
        return albumId;
    }

    public void setAlbumId(String albumId) {
        this.albumId = albumId;
    }

    public List<String> getPages() {
        return pages;
    }

    public void setPages(List<String> newPages) {
        this.pages.clear();
        if (newPages != null) {
            this.pages.addAll(newPages);
        }
        if (this.pages.isEmpty() && fileName != null) {
            this.pages.add(fileName);
        }
        this.currentPage = 0;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        if (pages.isEmpty()) {
            this.currentPage = 0;
            return;
        }
        this.currentPage = Math.max(0, Math.min(pages.size() - 1, currentPage));
    }

    public int nextPage() {
        if (pages.size() <= 1) return 0;
        currentPage = (currentPage + 1) % pages.size();
        return currentPage;
    }

    public int prevPage() {
        if (pages.size() <= 1) return 0;
        currentPage = (currentPage - 1 + pages.size()) % pages.size();
        return currentPage;
    }

    public boolean isClickToNextPage() {
        return clickToNextPage;
    }

    public void setClickToNextPage(boolean clickToNextPage) {
        this.clickToNextPage = clickToNextPage;
    }

    public int getAutoRotateSeconds() {
        return autoRotateSeconds;
    }

    public void setAutoRotateSeconds(int autoRotateSeconds) {
        this.autoRotateSeconds = autoRotateSeconds;
    }

    public List<ClickCommand> getClickCommands() {
        return clickCommands;
    }

    public void addClickCommand(ClickCommand command) {
        if (command != null) {
            this.clickCommands.add(command);
        }
    }

    public void removeClickCommand(int index) {
        if (index >= 0 && index < clickCommands.size()) {
            this.clickCommands.remove(index);
        }
    }

    public Location getTileLocation(int row, int col) {
        World w = getWorld();
        if (w == null) return null;

        int x = originX;
        int y = originY - row;
        int z = originZ;

        switch (facing) {
            case SOUTH -> x += col;
            case NORTH -> x -= col;
            case EAST -> z -= col;
            case WEST -> z += col;
            default -> {}
        }

        return new Location(w, x, y, z);
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("fileName", fileName);
        map.put("worldName", worldName);
        map.put("originX", originX);
        map.put("originY", originY);
        map.put("originZ", originZ);
        map.put("facing", facing.name());
        map.put("width", width);
        map.put("height", height);
        map.put("invisibleFrame", invisibleFrame);
        map.put("glowing", glowing);

        List<String> uuidStrings = new ArrayList<>();
        for (UUID uuid : frameUuids) {
            uuidStrings.add(uuid.toString());
        }
        map.put("frameUuids", uuidStrings);
        map.put("mapIds", mapIds);

        // Album & page data
        map.put("albumId", albumId);
        map.put("pages", pages);
        map.put("currentPage", currentPage);
        map.put("clickToNextPage", clickToNextPage);
        map.put("autoRotateSeconds", autoRotateSeconds);

        // Click commands
        List<Map<String, Object>> cmdList = new ArrayList<>();
        for (ClickCommand cmd : clickCommands) {
            cmdList.add(cmd.serialize());
        }
        map.put("clickCommands", cmdList);

        return map;
    }

    @SuppressWarnings("unchecked")
    public static PlacedImage deserialize(Map<String, Object> map) {
        String id = (String) map.get("id");
        String fileName = (String) map.get("fileName");
        String worldName = (String) map.get("worldName");
        int originX = ((Number) map.get("originX")).intValue();
        int originY = ((Number) map.get("originY")).intValue();
        int originZ = ((Number) map.get("originZ")).intValue();
        BlockFace facing = BlockFace.valueOf((String) map.get("facing"));
        int width = ((Number) map.get("width")).intValue();
        int height = ((Number) map.get("height")).intValue();
        boolean invisibleFrame = (Boolean) map.getOrDefault("invisibleFrame", true);
        boolean glowing = (Boolean) map.getOrDefault("glowing", false);

        PlacedImage placedImage = new PlacedImage(id, fileName, worldName, originX, originY, originZ,
                facing, width, height, invisibleFrame, glowing);

        if (map.containsKey("frameUuids")) {
            List<String> uuidStrings = (List<String>) map.get("frameUuids");
            for (String s : uuidStrings) {
                try {
                    placedImage.getFrameUuids().add(UUID.fromString(s));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        if (map.containsKey("mapIds")) {
            List<Number> ids = (List<Number>) map.get("mapIds");
            for (Number num : ids) {
                placedImage.getMapIds().add(num.intValue());
            }
        }

        if (map.containsKey("albumId")) {
            placedImage.setAlbumId((String) map.get("albumId"));
        }

        if (map.containsKey("pages")) {
            List<String> pagesList = (List<String>) map.get("pages");
            placedImage.getPages().clear();
            placedImage.getPages().addAll(pagesList);
        }

        placedImage.setCurrentPage(((Number) map.getOrDefault("currentPage", 0)).intValue());
        placedImage.setClickToNextPage((Boolean) map.getOrDefault("clickToNextPage", true));
        placedImage.setAutoRotateSeconds(((Number) map.getOrDefault("autoRotateSeconds", 0)).intValue());

        if (map.containsKey("clickCommands")) {
            List<Map<String, Object>> cmdList = (List<Map<String, Object>>) map.get("clickCommands");
            for (Map<String, Object> cMap : cmdList) {
                placedImage.getClickCommands().add(ClickCommand.deserialize(cMap));
            }
        }

        return placedImage;
    }
}
