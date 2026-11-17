package com.antimery.client;

public class ClientInfo {

    private final String rawBrand;
    private final String cleanName;
    private final ClientType type;

    public ClientInfo(String rawBrand, String cleanName, ClientType type) {
        this.rawBrand = rawBrand;
        this.cleanName = cleanName;
        this.type = type;
    }

    public String getRawBrand() {
        return rawBrand;
    }

    public String getCleanName() {
        return cleanName;
    }

    public ClientType getType() {
        return type;
    }

    @Override
    public String toString() {
        return cleanName + " (" + type.getDisplayName() + ")";
    }
}

