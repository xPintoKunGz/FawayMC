package com.imagery.model;

import java.util.*;

/**
 * Represents an Album containing a set of ordered image pages.
 */
public class ImageAlbum {

    private final String id;
    private String title;
    private final List<String> pages;
    private int autoRotateSeconds;

    public ImageAlbum(String id, String title, List<String> pages, int autoRotateSeconds) {
        this.id = id;
        this.title = title;
        this.pages = pages != null ? new ArrayList<>(pages) : new ArrayList<>();
        this.autoRotateSeconds = autoRotateSeconds;
    }

    public ImageAlbum(String id, String title) {
        this(id, title, new ArrayList<>(), 0);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getPages() {
        return pages;
    }

    public void addPage(String fileName) {
        if (fileName != null && !pages.contains(fileName)) {
            pages.add(fileName);
        }
    }

    public void removePage(int index) {
        if (index >= 0 && index < pages.size()) {
            pages.remove(index);
        }
    }

    public int getAutoRotateSeconds() {
        return autoRotateSeconds;
    }

    public void setAutoRotateSeconds(int autoRotateSeconds) {
        this.autoRotateSeconds = autoRotateSeconds;
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("title", title);
        map.put("pages", pages);
        map.put("autoRotateSeconds", autoRotateSeconds);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ImageAlbum deserialize(Map<String, Object> map) {
        String id = (String) map.get("id");
        String title = (String) map.getOrDefault("title", id);
        List<String> pages = (List<String>) map.getOrDefault("pages", new ArrayList<String>());
        int auto = ((Number) map.getOrDefault("autoRotateSeconds", 0)).intValue();
        return new ImageAlbum(id, title, pages, auto);
    }
}

