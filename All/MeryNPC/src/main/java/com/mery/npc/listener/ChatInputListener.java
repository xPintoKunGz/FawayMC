package com.mery.npc.listener;

import com.mery.npc.MeryNPC;
import com.mery.npc.gui.CommandListGui;
import com.mery.npc.gui.HologramGui;
import com.mery.npc.gui.MainNPCGui;
import com.mery.npc.model.ClickTrigger;
import com.mery.npc.model.CommandType;
import com.mery.npc.model.NPCCommand;
import com.mery.npc.model.NPCData;
import com.mery.npc.util.ColorUtil;
import com.mery.npc.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatInputListener implements Listener {

    public enum InputMode {
        RENAME,
        RENAME_HOLO_GUI,
        SKIN,
        ADD_HOLO,
        EDIT_HOLO_LINE,
        INSERT_HOLO_LINE,
        ADD_ACTION
    }

    public static class PendingInput {
        public final NPCData npcData;
        public final InputMode mode;
        public final int lineIndex;

        public PendingInput(NPCData npcData, InputMode mode) {
            this(npcData, mode, -1);
        }

        public PendingInput(NPCData npcData, InputMode mode, int lineIndex) {
            this.npcData = npcData;
            this.mode = mode;
            this.lineIndex = lineIndex;
        }
    }

    private static final Map<UUID, PendingInput> pendingMap = new ConcurrentHashMap<>();
    private final MeryNPC plugin;

    public ChatInputListener(MeryNPC plugin) {
        this.plugin = plugin;
    }

    public static void requestInput(Player player, NPCData data, InputMode mode, String prompt) {
        requestInput(player, data, mode, -1, prompt);
    }

    public static void requestInput(Player player, NPCData data, InputMode mode, int lineIndex, String prompt) {
        pendingMap.put(player.getUniqueId(), new PendingInput(data, mode, lineIndex));
        player.closeInventory();
        player.sendMessage(ColorUtil.colorize(""));
        player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFMeryNPC Studio &8» &f" + prompt));
        player.sendMessage(ColorUtil.colorize("&7  (พิมพ์ข้อความลงในแชทได้เลย หรือพิมพ์ &#FF5858cancel &7เพื่อยกเลิก)"));
        player.sendMessage(ColorUtil.colorize(""));
        SoundUtil.playClick(player);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingInput pending = pendingMap.remove(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        String msg = event.getMessage().trim();

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (msg.equalsIgnoreCase("cancel") || msg.equalsIgnoreCase("ยกเลิก")) {
                player.sendMessage(ColorUtil.colorize("&#FF6584✖ ยกเลิกการพิมพ์ข้อมูลแล้ว"));
                if (pending.mode == InputMode.ADD_HOLO || pending.mode == InputMode.EDIT_HOLO_LINE
                        || pending.mode == InputMode.INSERT_HOLO_LINE || pending.mode == InputMode.RENAME_HOLO_GUI) {
                    HologramGui.open(player, pending.npcData);
                } else if (pending.mode == InputMode.ADD_ACTION) {
                    CommandListGui.open(player, pending.npcData);
                } else {
                    MainNPCGui.open(player, pending.npcData);
                }
                return;
            }

            NPCData data = pending.npcData;
            switch (pending.mode) {
                case RENAME:
                case RENAME_HOLO_GUI:
                    String newName = (msg.equalsIgnoreCase("none") || msg.equalsIgnoreCase("empty")) ? "" : msg;
                    data.setName(newName);
                    plugin.getNpcManager().updateHologram(data);
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playSuccess(player);
                    player.sendMessage(ColorUtil.colorize("&#43E97B✔ เปลี่ยนชื่อหุ่น MeryNPC เป็น: &f" + (newName.isEmpty() ? "(ไร้ชื่อ)" : newName)));
                    if (pending.mode == InputMode.RENAME_HOLO_GUI) {
                        HologramGui.open(player, data);
                    } else {
                        MainNPCGui.open(player, data);
                    }
                    break;

                case SKIN:
                    String rawSkinMsg = msg;
                    boolean slim = false;
                    if (rawSkinMsg.toLowerCase().startsWith("url ")) {
                        rawSkinMsg = rawSkinMsg.substring(4).trim();
                    }
                    if (rawSkinMsg.toLowerCase().endsWith(" slim")) {
                        slim = true;
                        rawSkinMsg = rawSkinMsg.substring(0, rawSkinMsg.length() - 5).trim();
                    }
                    String skinInput = rawSkinMsg.equalsIgnoreCase("me") ? player.getName() : rawSkinMsg;
                    String displaySkinName = com.mery.npc.manager.SkinManager.isUrl(skinInput) ? "URL Skin" : skinInput;
                    player.sendMessage(plugin.getConfigManager().getMessage("skin-updating", "%name%", displaySkinName));
                    plugin.getSkinManager().applySkinToNPC(data, skinInput, slim, () -> {
                        plugin.getStorageManager().saveNPCs();
                        SoundUtil.playSuccess(player);
                        player.sendMessage(plugin.getConfigManager().getMessage("skin-updated", "%id%", String.valueOf(data.getId()), "%name%", displaySkinName));
                        MainNPCGui.open(player, data);
                    }, () -> {
                        plugin.getStorageManager().saveNPCs();
                        if (com.mery.npc.manager.SkinManager.isUrl(skinInput)) {
                            SoundUtil.playError(player);
                            player.sendMessage(ColorUtil.colorize("&#FF5858✖ ไม่สามารถโหลดสกินจาก URL นี้ได้ กรุณาตรวจสอบว่าเป็นลิงก์ไฟล์รูปสกิน .png โดยตรง"));
                        } else {
                            SoundUtil.playSuccess(player);
                        }
                        MainNPCGui.open(player, data);
                    });
                    break;

                case ADD_HOLO:
                    String lineToAdd = (msg.equalsIgnoreCase("blank") || msg.equalsIgnoreCase("empty")) ? " " : msg;
                    data.getHologramLines().add(lineToAdd);
                    plugin.getNpcManager().updateHologram(data);
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playSuccess(player);
                    player.sendMessage(plugin.getConfigManager().getMessage("holo-line-added", "%line%", String.valueOf(data.getHologramLines().size())));
                    HologramGui.open(player, data);
                    break;

                case EDIT_HOLO_LINE:
                    String editedLine = (msg.equalsIgnoreCase("blank") || msg.equalsIgnoreCase("empty")) ? " " : msg;
                    int editIdx = pending.lineIndex;
                    if (editIdx >= 0 && editIdx < data.getHologramLines().size()) {
                        data.getHologramLines().set(editIdx, editedLine);
                        plugin.getNpcManager().updateHologram(data);
                        plugin.getStorageManager().saveNPCs();
                        SoundUtil.playSuccess(player);
                        player.sendMessage(ColorUtil.colorize("&#43E97B✔ แก้ไข Hologram บรรทัดที่ &#FFD166#" + (editIdx + 1) + " &#43E97Bเป็น: &f" + editedLine));
                    }
                    HologramGui.open(player, data);
                    break;

                case INSERT_HOLO_LINE:
                    String insertedLine = (msg.equalsIgnoreCase("blank") || msg.equalsIgnoreCase("empty")) ? " " : msg;
                    int insIdx = Math.max(0, Math.min(pending.lineIndex, data.getHologramLines().size()));
                    data.getHologramLines().add(insIdx, insertedLine);
                    plugin.getNpcManager().updateHologram(data);
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playSuccess(player);
                    player.sendMessage(ColorUtil.colorize("&#43E97B✔ แทรก Hologram บรรทัดที่ &#FFD166#" + (insIdx + 1) + " &#43E97Bเรียบร้อยแล้ว!"));
                    HologramGui.open(player, data);
                    break;

                case ADD_ACTION:
                    String cleanCmd = msg.startsWith("/") ? msg.substring(1).trim() : msg.trim();
                    String lowerCmd = cleanCmd.toLowerCase();
                    CommandType detectedType = CommandType.CONSOLE;
                    if (lowerCmd.startsWith("server ") || lowerCmd.startsWith("send ")
                            || lowerCmd.startsWith("connect ") || lowerCmd.startsWith("bungee ")) {
                        detectedType = CommandType.SERVER;
                    }
                    NPCCommand cmd = new NPCCommand(cleanCmd, detectedType, ClickTrigger.ANY);
                    data.getCommands().add(cmd);
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playSuccess(player);
                    player.sendMessage(ColorUtil.colorize("&#43E97B✔ เพิ่ม Action ใหม่สำเร็จ! &8[&#FFD166" + detectedType.name() + "&8] &f" + cleanCmd));
                    CommandListGui.open(player, data);
                    break;
            }
        });
    }
}
