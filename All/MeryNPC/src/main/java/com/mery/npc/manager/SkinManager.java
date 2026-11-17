package com.mery.npc.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mery.npc.MeryNPC;
import com.mery.npc.model.NPCData;
import com.mery.npc.nms.NMSPlayerSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class SkinManager {

    private final MeryNPC plugin;
    // Cache: key (playerName or URL) -> SkinProperty [value, signature, skinUrl]
    private final Map<String, SkinProperty> skinCache = new ConcurrentHashMap<>();

    public static class SkinProperty {
        public final String value;
        public final String signature;
        public final String skinUrl;

        public SkinProperty(String value, String signature, String skinUrl) {
            this.value = value;
            this.signature = signature;
            this.skinUrl = skinUrl;
        }
    }

    public SkinManager(MeryNPC plugin) {
        this.plugin = plugin;
    }

    public static boolean isUrl(String input) {
        if (input == null) return false;
        String lower = input.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    /**
     * Creates a guaranteed classic Steve skull.
     */
    public ItemStack createSteveSkull() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        try {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.fromString("c06f8906-4c8a-4911-9c29-ea1dbd1aab82"), "Steve");
            PlayerTextures textures = profile.getTextures();
            textures.setSkin(URI.create("https://textures.minecraft.net/texture/3f117738cb855f91e4c9fe3e5d5cad9b45897d11f991ce1f480fbee281eaca").toURL());
            profile.setTextures(textures);
            meta.setOwnerProfile(profile);
        } catch (Exception e) {
            meta.setOwner("MHF_Steve");
        }

        head.setItemMeta(meta);
        return head;
    }

    public ItemStack createSteveChestplate() {
        ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(Color.fromRGB(0, 168, 181));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createSteveLeggings() {
        ItemStack item = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(Color.fromRGB(43, 52, 141));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createSteveBoots() {
        ItemStack item = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(Color.fromRGB(60, 60, 60));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createSteveSword() {
        return new ItemStack(Material.DIAMOND_SWORD);
    }

    public ItemStack createSteveShield() {
        return new ItemStack(Material.SHIELD);
    }

    public void applyDefaultSteveEquipment(NPCData data) {
        data.setSkinName("Steve");
        data.setSkinValue(NMSPlayerSpawner.DEFAULT_STEVE_TEXTURE);
        data.setSkinSignature("");
        if ("ARMOR_STAND".equalsIgnoreCase(data.getType())) {
            Map<String, ItemStack> equip = data.getEquipment();
            equip.put("HELMET", createSteveSkull());
            equip.put("CHESTPLATE", createSteveChestplate());
            equip.put("LEGGINGS", createSteveLeggings());
            equip.put("BOOTS", createSteveBoots());
        }
    }

    /**
     * Fetches skin asynchronously from a URL (via MineSkin API + textures.minecraft.net fallback),
     * online player profile, or Mojang API.
     */
    public void fetchSkin(String input, Consumer<SkinProperty> callback) {
        fetchSkin(input, false, callback);
    }

    public void fetchSkin(String input, boolean slimModel, Consumer<SkinProperty> callback) {
        if (input == null || input.trim().isEmpty()) {
            callback.accept(null);
            return;
        }
        String trimmed = input.trim();
        String cacheKey = (slimModel ? "slim:" : "") + trimmed.toLowerCase();
        if (skinCache.containsKey(cacheKey)) {
            callback.accept(skinCache.get(cacheKey));
            return;
        }

        // Case 1: Skin URL (https://...png, https://textures.minecraft.net/..., https://mineskin.org/...)
        if (isUrl(trimmed)) {
            fetchSkinFromUrl(trimmed, slimModel, cacheKey, callback);
            return;
        }

        // Case 2: Raw Base64 Texture Value (eyJ0ZXh0dXJlcyI6...)
        if (trimmed.length() > 60 && trimmed.startsWith("eyJ")) {
            String extractedUrl = extractSkinUrlFromBase64(trimmed);
            SkinProperty sp = new SkinProperty(trimmed, "", extractedUrl);
            skinCache.put(cacheKey, sp);
            callback.accept(sp);
            return;
        }

        // Case 3: Online player on this server
        org.bukkit.entity.Player online = Bukkit.getPlayerExact(trimmed);
        if (online != null) {
            for (com.destroystokyo.paper.profile.ProfileProperty prop : online.getPlayerProfile().getProperties()) {
                if ("textures".equals(prop.getName()) && prop.getValue() != null && !prop.getValue().isEmpty()) {
                    String extractedUrl = extractSkinUrlFromBase64(prop.getValue());
                    SkinProperty sp = new SkinProperty(prop.getValue(), prop.getSignature(), extractedUrl);
                    skinCache.put(cacheKey, sp);
                    callback.accept(sp);
                    return;
                }
            }
        }

        // Case 4: Mojang API by Player Name
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL uuidUrl = URI.create("https://api.mojang.com/users/profiles/minecraft/" + trimmed).toURL();
                HttpURLConnection uuidConn = (HttpURLConnection) uuidUrl.openConnection();
                uuidConn.setRequestMethod("GET");
                uuidConn.setConnectTimeout(5000);
                uuidConn.setReadTimeout(5000);

                if (uuidConn.getResponseCode() != 200) {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                    return;
                }

                JsonObject uuidJson = JsonParser.parseReader(new InputStreamReader(uuidConn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
                String idStr = uuidJson.get("id").getAsString();

                URL profileUrl = URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + idStr + "?unsigned=false").toURL();
                HttpURLConnection profileConn = (HttpURLConnection) profileUrl.openConnection();
                profileConn.setRequestMethod("GET");
                profileConn.setConnectTimeout(5000);
                profileConn.setReadTimeout(5000);

                if (profileConn.getResponseCode() != 200) {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                    return;
                }

                JsonObject profileJson = JsonParser.parseReader(new InputStreamReader(profileConn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
                JsonArray properties = profileJson.getAsJsonArray("properties");

                String textureValue = null;
                String textureSignature = null;
                String skinUrl = null;

                for (JsonElement elem : properties) {
                    JsonObject prop = elem.getAsJsonObject();
                    if ("textures".equals(prop.get("name").getAsString())) {
                        textureValue = prop.get("value").getAsString();
                        if (prop.has("signature")) {
                            textureSignature = prop.get("signature").getAsString();
                        }
                        skinUrl = extractSkinUrlFromBase64(textureValue);
                        break;
                    }
                }

                if (textureValue != null) {
                    SkinProperty property = new SkinProperty(textureValue, textureSignature, skinUrl);
                    skinCache.put(cacheKey, property);
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(property));
                } else {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
                }

            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    /**
     * Resolves a Skin URL into a Mojang-signed texture property via MineSkin API,
     * with automatic fallback for direct textures.minecraft.net / NameMC links.
     */
    private void fetchSkinFromUrl(String rawUrl, boolean slimModel, String cacheKey, Consumer<SkinProperty> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String normalizedUrl = normalizeSkinUrl(rawUrl);

                // 1. Check if it's a MineSkin share page URL: https://mineskin.org/<id> or https://minesk.in/<id>
                if (normalizedUrl.contains("mineskin.org/") || normalizedUrl.contains("minesk.in/")) {
                    String[] parts = normalizedUrl.split("/");
                    String lastPart = parts[parts.length - 1].split("\\?")[0];
                    if (!lastPart.isEmpty() && !"url".equalsIgnoreCase(lastPart)) {
                        SkinProperty fromId = fetchMineSkinById(lastPart);
                        if (fromId != null) {
                            skinCache.put(cacheKey, fromId);
                            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(fromId));
                            return;
                        }
                    }
                }

                // 2. Generate signed texture via MineSkin API (https://api.mineskin.org/generate/url)
                URL apiUrl = URI.create("https://api.mineskin.org/generate/url").toURL();
                HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("User-Agent", "MeryNPC-Studio/26.3");

                JsonObject payload = new JsonObject();
                payload.addProperty("url", normalizedUrl);
                payload.addProperty("visibility", 1);
                if (slimModel) {
                    payload.addProperty("variant", "slim");
                }

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    try (InputStream is = conn.getInputStream()) {
                        JsonObject resp = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
                        SkinProperty parsed = parseMineSkinResponse(resp, normalizedUrl);
                        if (parsed != null) {
                            skinCache.put(cacheKey, parsed);
                            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(parsed));
                            return;
                        }
                    }
                }

                // 3. Fallback: If it's a direct textures.minecraft.net URL, encode unsigned Base64 texture JSON
                if (normalizedUrl.contains("textures.minecraft.net/texture/")) {
                    String json = "{\"textures\":{\"SKIN\":{\"url\":\"" + normalizedUrl + "\"}}}";
                    String b64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
                    SkinProperty fallbackProp = new SkinProperty(b64, "", normalizedUrl);
                    skinCache.put(cacheKey, fallbackProp);
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(fallbackProp));
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to fetch skin from URL (" + rawUrl + "): " + e.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(null));
            }
        });
    }

    private String normalizeSkinUrl(String url) {
        String u = url.trim();
        // Convert NameMC texture page https://namemc.com/skin/<hash> -> https://s.namemc.com/i/<hash>.png
        if (u.contains("namemc.com/skin/")) {
            String[] segs = u.split("namemc.com/skin/");
            if (segs.length > 1) {
                String hash = segs[1].split("[/?#]")[0];
                return "https://s.namemc.com/i/" + hash + ".png";
            }
        }
        return u;
    }

    private SkinProperty fetchMineSkinById(String id) {
        try {
            URL url = URI.create("https://api.mineskin.org/get/uuid/" + id).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(7000);
            conn.setReadTimeout(7000);
            conn.setRequestProperty("User-Agent", "MeryNPC-Studio/26.3");
            if (conn.getResponseCode() == 200) {
                JsonObject resp = JsonParser.parseReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)).getAsJsonObject();
                return parseMineSkinResponse(resp, null);
            }
        } catch (Exception ignored) {}
        return null;
    }

    private SkinProperty parseMineSkinResponse(JsonObject resp, String fallbackUrl) {
        try {
            if (resp.has("data") && resp.get("data").isJsonObject()) {
                JsonObject data = resp.getAsJsonObject("data");
                if (data.has("texture") && data.get("texture").isJsonObject()) {
                    JsonObject tex = data.getAsJsonObject("texture");
                    String val = tex.has("value") ? tex.get("value").getAsString() : null;
                    String sig = tex.has("signature") ? tex.get("signature").getAsString() : "";
                    String url = tex.has("url") ? tex.get("url").getAsString() : extractSkinUrlFromBase64(val);
                    if (val != null && !val.isEmpty()) {
                        return new SkinProperty(val, sig, url != null ? url : fallbackUrl);
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractSkinUrlFromBase64(String textureValue) {
        if (textureValue == null || textureValue.isEmpty()) return null;
        try {
            String decoded = new String(Base64.getDecoder().decode(textureValue), StandardCharsets.UTF_8);
            JsonObject decodedJson = JsonParser.parseString(decoded).getAsJsonObject();
            JsonObject texturesObj = decodedJson.getAsJsonObject("textures");
            if (texturesObj != null && texturesObj.has("SKIN")) {
                return texturesObj.getAsJsonObject("SKIN").get("url").getAsString();
            }
        } catch (Exception ignored) {}
        return null;
    }

    public ItemStack createSkull(String skinName, String skinValue, String skinUrl) {
        if ("Steve".equalsIgnoreCase(skinName) || (skinName == null && skinUrl == null)) {
            return createSteveSkull();
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        try {
            String safeProfileName = (skinName != null && !skinName.isEmpty() && !isUrl(skinName) && skinName.length() <= 16)
                    ? skinName
                    : "MeryNPC";
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), safeProfileName);
            PlayerTextures textures = profile.getTextures();

            String finalUrl = skinUrl != null && !skinUrl.isEmpty() ? skinUrl : extractSkinUrlFromBase64(skinValue);
            if (finalUrl != null && !finalUrl.isEmpty()) {
                textures.setSkin(URI.create(finalUrl).toURL());
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            } else if (skinName != null && !skinName.isEmpty() && !isUrl(skinName) && skinName.length() <= 16) {
                meta.setOwner(skinName);
            }
        } catch (Exception e) {
            if (skinName != null && !skinName.isEmpty() && !isUrl(skinName) && skinName.length() <= 16) {
                meta.setOwner(skinName);
            }
        }

        head.setItemMeta(meta);
        return head;
    }

    /**
     * Applies skin (by Player Name or Skin URL) to an NPCData and updates its live model.
     */
    public void applySkinToNPC(NPCData npcData, String skinInput, Runnable onSuccess, Runnable onFailure) {
        applySkinToNPC(npcData, skinInput, false, onSuccess, onFailure);
    }

    public void applySkinToNPC(NPCData npcData, String skinInput, boolean slimModel, Runnable onSuccess, Runnable onFailure) {
        if ("Steve".equalsIgnoreCase(skinInput) || "reset".equalsIgnoreCase(skinInput)) {
            npcData.setSkinName("Steve");
            npcData.setSkinValue(NMSPlayerSpawner.DEFAULT_STEVE_TEXTURE);
            npcData.setSkinSignature("");
            if ("ARMOR_STAND".equalsIgnoreCase(npcData.getType())) {
                npcData.getEquipment().put("HELMET", createSteveSkull());
            } else {
                ItemStack helm = npcData.getEquipment().get("HELMET");
                if (helm != null && helm.getType() == Material.PLAYER_HEAD) {
                    npcData.getEquipment().remove("HELMET");
                }
            }
            plugin.getNpcManager().updateSkin(npcData);
            if (onSuccess != null) onSuccess.run();
            return;
        }

        boolean inputIsUrl = isUrl(skinInput);
        String displayLabel = inputIsUrl ? "CustomURL" : skinInput;

        fetchSkin(skinInput, slimModel, skinProperty -> {
            if (skinProperty != null) {
                npcData.setSkinName(displayLabel);
                npcData.setSkinValue(skinProperty.value);
                npcData.setSkinSignature(skinProperty.signature != null ? skinProperty.signature : "");
                if ("ARMOR_STAND".equalsIgnoreCase(npcData.getType())) {
                    npcData.getEquipment().put("HELMET", createSkull(displayLabel, skinProperty.value, skinProperty.skinUrl));
                } else {
                    ItemStack helm = npcData.getEquipment().get("HELMET");
                    if (helm != null && helm.getType() == Material.PLAYER_HEAD) {
                        npcData.getEquipment().remove("HELMET");
                    }
                }

                plugin.getNpcManager().updateSkin(npcData);
                if (onSuccess != null) onSuccess.run();
            } else if (!inputIsUrl) {
                // Even if Mojang API failed or rate-limited for a username, set skinName so ResolvableProfile can resolve by name
                npcData.setSkinName(skinInput);
                npcData.setSkinValue("");
                npcData.setSkinSignature("");
                plugin.getNpcManager().updateSkin(npcData);
                if (onFailure != null) onFailure.run();
            } else {
                if (onFailure != null) onFailure.run();
            }
        });
    }
}
