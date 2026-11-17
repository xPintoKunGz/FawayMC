package me.editmery;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

public class EditMeryManager {

    private final EditMeryPlugin plugin;
    private final Set<UUID> enabledPlayers = Collections.synchronizedSet(new HashSet<>());
    private final Map<UUID, Long> hintCooldown = new HashMap<>();

    // Prefix for all WorldEdit commands (//set, //pos1, //pos2, etc.)
    private String worldEditPrefix = "&8[&d&lEdit&b&lMery&8]&7 {0}";

    // Config options
    private boolean disableOnJoin = true;

    private String soundToggleOn = "ENTITY_PLAYER_LEVELUP";
    private float soundToggleOnPitch = 1.5f;
    private String soundToggleOff = "BLOCK_NOTE_BLOCK_BASS";
    private float soundToggleOffPitch = 0.8f;
    private String soundBlocked = "BLOCK_NOTE_BLOCK_BASS";
    private float soundBlockedPitch = 0.5f;

    private boolean enableBypass = false;
    private String bypassPermission = "editmery.bypass";

    // Messages
    private String msgEnabled;
    private String msgDisabled;
    private String msgStatusOn;
    private String msgStatusOff;
    private String msgNoPermission;
    private String msgReloaded;

    public EditMeryManager(EditMeryPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration config = plugin.getConfig();

        worldEditPrefix = config.getString("worldedit-prefix", "&8[&d&lEdit&b&lMery&8]&7 {0}");
        disableOnJoin = config.getBoolean("disable-on-join", true);

        soundToggleOn = config.getString("sounds.toggle-on", "ENTITY_PLAYER_LEVELUP");
        soundToggleOnPitch = (float) config.getDouble("sounds.toggle-on-pitch", 1.5);
        soundToggleOff = config.getString("sounds.toggle-off", "BLOCK_NOTE_BLOCK_BASS");
        soundToggleOffPitch = (float) config.getDouble("sounds.toggle-off-pitch", 0.8);
        soundBlocked = config.getString("sounds.blocked", "BLOCK_NOTE_BLOCK_BASS");
        soundBlockedPitch = (float) config.getDouble("sounds.blocked-pitch", 0.5);

        enableBypass = config.getBoolean("enable-bypass", false);
        bypassPermission = config.getString("bypass-permission", "editmery.bypass");

        msgEnabled = config.getString("messages.enabled",
                "&8(&#B388FF✦ &#FF80ABEditMery&8) &#06D6A0✔ &fเปิดใช้งานโหมดขวาน WorldEdit แล้ว &8(พิมพ์ &#EF476F/em disable &8เมื่อต้องการปิด)");
        msgDisabled = config.getString("messages.disabled",
                "&8(&#B388FF✦ &#FF80ABEditMery&8) &#EF476F✖ &fปิดการใช้งานโหมดขวาน WorldEdit &8(ขวานไม้ใช้ตัดไม้ทุบบล็อกได้ตามปกติ)");

        msgStatusOn = config.getString("messages.status-on",
                "&8(&#B388FF✦ &#FF80ABEditMery&8) &fสถานะปัจจุบัน: &#06D6A0● เปิดใช้งาน (ENABLED)");
        msgStatusOff = config.getString("messages.status-off",
                "&8(&#B388FF✦ &#FF80ABEditMery&8) &fสถานะปัจจุบัน: &#EF476F● ปิดการใช้งาน (DISABLED)");

        msgNoPermission = config.getString("messages.no-permission",
                "&8(&#B388FF✦ &#FF80ABEditMery&8) &#EF476F✖ คำสั่งนี้สามารถใช้งานได้เฉพาะผู้ดูแลเซิร์ฟเวอร์ (OP) เท่านั้น!");
        msgReloaded = config.getString("messages.reloaded",
                "&8(&#B388FF✦ &#FF80ABEditMery&8) &#06D6A0✔ รีโหลดการตั้งค่า EditMery เรียบร้อยแล้ว!");
    }

    public String getWorldEditPrefix() {
        return worldEditPrefix;
    }

    public boolean isEnabled(Player player) {
        if (player == null) return false;
        return enabledPlayers.contains(player.getUniqueId());
    }

    public void setEnabled(Player player, boolean enabled) {
        if (player == null) return;
        if (enabled) {
            enabledPlayers.add(player.getUniqueId());
        } else {
            enabledPlayers.remove(player.getUniqueId());
        }
    }

    public boolean toggle(Player player) {
        if (player == null) return false;
        boolean newState = !isEnabled(player);
        setEnabled(player, newState);
        return newState;
    }

    public void removePlayer(UUID uuid) {
        enabledPlayers.remove(uuid);
        hintCooldown.remove(uuid);
    }

    public void cleanup() {
        enabledPlayers.clear();
        hintCooldown.clear();
    }

    public boolean canBypass(Player player) {
        if (!enableBypass || player == null) return false;
        return player.isOp() || player.hasPermission(bypassPermission);
    }

    public void sendToggleSuccess(Player player, boolean enabled) {
        if (player == null) return;
        if (enabled) {
            ColorUtil.playSound(player, soundToggleOn, 1.0f, soundToggleOnPitch);
            player.sendMessage(ColorUtil.colorize(msgEnabled));
        } else {
            ColorUtil.playSound(player, soundToggleOff, 1.0f, soundToggleOffPitch);
            player.sendMessage(ColorUtil.colorize(msgDisabled));
        }
    }

    public void sendAxeHeldHint(Player player) {
        if (player == null) return;
        long now = System.currentTimeMillis();
        Long last = hintCooldown.get(player.getUniqueId());
        if (last != null && (now - last) < 5000L) {
            return;
        }
        hintCooldown.put(player.getUniqueId(), now);
        player.sendMessage(ColorUtil.colorize("&8(&#B388FF✦ &#FF80ABEditMery&8) &#94A3B8ขวานไม้ (โหมดตัดไม้ปกติ) &8| &#F8FAFCพิมพ์ &#FFD166/em enable &#F8FAFCเพื่อเปิดโหมดขวาน WorldEdit"));
    }

    public String getMsgStatus(boolean enabled) {
        return enabled ? msgStatusOn : msgStatusOff;
    }

    public String getMsgNoPermission() {
        return msgNoPermission;
    }

    public String getMsgReloaded() {
        return msgReloaded;
    }

    public boolean isDisableOnJoin() {
        return disableOnJoin;
    }
}
