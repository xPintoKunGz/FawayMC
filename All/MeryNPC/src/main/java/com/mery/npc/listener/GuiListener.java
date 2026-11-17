package com.mery.npc.listener;

import com.mery.npc.MeryNPC;
import com.mery.npc.gui.*;
import com.mery.npc.model.ClickTrigger;
import com.mery.npc.model.CommandType;
import com.mery.npc.model.NPCCommand;
import com.mery.npc.model.NPCData;
import com.mery.npc.util.ColorUtil;
import com.mery.npc.util.SoundUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuiListener implements Listener {

    private final MeryNPC plugin;
    private static final List<String> AURA_PRESETS = Arrays.asList("NONE", "FLAME", "SOUL", "ENCHANT", "CHERRY", "END", "HEART", "TOTEM");
    private static final double[] SCALE_PRESETS = new double[]{0.7, 1.0, 1.3, 1.6, 2.0};
    private static final List<String> ARCHETYPE_PRESETS = Arrays.asList("PLAYER", "VILLAGER", "ALLAY", "IRON_GOLEM", "WARDEN", "ARMOR_STAND");
    private static final double[] COOLDOWN_PRESETS = new double[]{0.0, 1.0, 3.0, 5.0, 10.0, 30.0};

    public GuiListener(MeryNPC plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        if (event.getInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
            GuiHolder holder = (GuiHolder) event.getInventory().getHolder();
            NPCData data = holder.getNpcData();
            if (data == null) return;

            int slot = event.getRawSlot();
            String guiType = holder.getGuiType();

            if (guiType.equals("MAIN")) {
                handleMainClick(player, data, slot, event.getClick());
            } else if (guiType.equals("EQUIPMENT")) {
                handleEquipmentClick(player, data, slot, event.getClick(), event.getCursor());
            } else if (guiType.equals("POSE")) {
                handlePoseClick(player, data, slot);
            } else if (guiType.equals("COMMANDS")) {
                handleCommandsClick(player, data, slot, event.getClick());
            } else if (guiType.equals("HOLOGRAM")) {
                handleHologramClick(player, data, slot, event.getClick());
            }
        }
    }

    private void handleMainClick(Player player, NPCData data, int slot, ClickType click) {
        switch (slot) {
            case 4: // Rename or Clear Name
                if (click.isRightClick()) {
                    data.setName("");
                    plugin.getNpcManager().updateHologram(data);
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playSuccess(player);
                    MainNPCGui.open(player, data);
                } else {
                    ChatInputListener.requestInput(player, data, ChatInputListener.InputMode.RENAME,
                            "พิมพ์ชื่อใหม่ของหุ่น (รองรับสี & และ &#HEX หรือพิมพ์ none เพื่อไร้ชื่อ):");
                }
                break;

            case 19: // Wardrobe & Gear
                SoundUtil.playClick(player);
                EquipmentGui.open(player, data);
                break;

            case 20: // Stance
                SoundUtil.playClick(player);
                PoseGui.open(player, data);
                break;

            case 21: // 3D Skin
                if (click.isShiftClick()) {
                    plugin.getSkinManager().applySkinToNPC(data, "Steve", () -> {
                        plugin.getStorageManager().saveNPCs();
                        SoundUtil.playSuccess(player);
                        MainNPCGui.open(player, data);
                    }, null);
                } else if (click.isRightClick()) {
                    plugin.getSkinManager().applySkinToNPC(data, player.getName(), () -> {
                        plugin.getStorageManager().saveNPCs();
                        SoundUtil.playSuccess(player);
                        MainNPCGui.open(player, data);
                    }, () -> {
                        plugin.getStorageManager().saveNPCs();
                        MainNPCGui.open(player, data);
                    });
                } else {
                    ChatInputListener.requestInput(player, data, ChatInputListener.InputMode.SKIN,
                            "พิมพ์ชื่อผู้เล่น Minecraft หรือวางลิงก์ URL รูปสกิน (.png / NameMC / MineSkin):");
                }
                break;

            case 22: // Floating Banner (Hologram Studio)
                if (click.isShiftClick()) {
                    data.getHologramLines().clear();
                    plugin.getNpcManager().updateHologram(data);
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playSuccess(player);
                    MainNPCGui.open(player, data);
                } else if (click.isRightClick()) {
                    ChatInputListener.requestInput(player, data, ChatInputListener.InputMode.ADD_HOLO,
                            "พิมพ์ข้อความบรรทัดใหม่ที่ต้องการให้ลอยเหนือหัวหุ่น:");
                } else {
                    SoundUtil.playClick(player);
                    HologramGui.open(player, data);
                }
                break;

            case 23: // Action Studio
                SoundUtil.playClick(player);
                CommandListGui.open(player, data);
                break;

            case 24: // Particle Aura Ring
                int curAuraIdx = AURA_PRESETS.indexOf(data.getParticleAura().toUpperCase());
                String nextAura = AURA_PRESETS.get((curAuraIdx + 1) % AURA_PRESETS.size());
                data.setParticleAura(nextAura);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playClick(player);
                MainNPCGui.open(player, data);
                break;

            case 25: // Scale & Glowing
                if (click.isRightClick()) {
                    data.setGlowing(!data.isGlowing());
                } else {
                    double curScale = data.getScale();
                    double nextScale = SCALE_PRESETS[0];
                    for (int i = 0; i < SCALE_PRESETS.length; i++) {
                        if (Math.abs(curScale - SCALE_PRESETS[i]) < 0.05) {
                            nextScale = SCALE_PRESETS[(i + 1) % SCALE_PRESETS.length];
                            break;
                        }
                    }
                    data.setScale(nextScale);
                }
                plugin.getNpcManager().updateVisuals(data);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playClick(player);
                MainNPCGui.open(player, data);
                break;

            case 29: // Entity Archetype
                int curTypeIdx = ARCHETYPE_PRESETS.indexOf(data.getType().toUpperCase());
                String nextType = ARCHETYPE_PRESETS.get((curTypeIdx + 1) % ARCHETYPE_PRESETS.size());
                data.setType(nextType);
                plugin.getNpcManager().spawnNPC(data);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playSuccess(player);
                MainNPCGui.open(player, data);
                break;

            case 31: // Auto-Gaze
                data.setLookAtPlayer(!data.isLookAtPlayer());
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playClick(player);
                MainNPCGui.open(player, data);
                break;

            case 33: // Warp, Summon & Clone
                if (click.isShiftClick()) {
                    NPCData cloned = plugin.getNpcManager().createNPC(data.getName(), player.getLocation(), data.getType());
                    cloned.setSkinName(data.getSkinName());
                    cloned.setSkinValue(data.getSkinValue());
                    cloned.setSkinSignature(data.getSkinSignature());
                    cloned.setGlowing(data.isGlowing());
                    cloned.setScale(data.getScale());
                    cloned.setParticleAura(data.getParticleAura());
                    cloned.setLookAtPlayer(data.isLookAtPlayer());
                    cloned.setHologramLines(new ArrayList<>(data.getHologramLines()));
                    cloned.getEquipment().putAll(data.getEquipment());
                    for (NPCCommand c : data.getCommands()) {
                        cloned.getCommands().add(NPCCommand.deserialize(c.serialize()));
                    }
                    plugin.getNpcManager().spawnNPC(cloned);
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playSuccess(player);
                    player.sendMessage(ColorUtil.colorize("&#43E97B✔ โคลนหุ่นจำลองตัวใหม่ (&#FFD166#" + cloned.getId() + "&#43E97B) มายืนที่ตำแหน่งของคุณแล้ว!"));
                    MainNPCGui.open(player, cloned);
                } else if (click.isLeftClick()) {
                    if (data.getLocation() != null) {
                        player.teleport(data.getLocation());
                        SoundUtil.playTeleport(player);
                        player.sendMessage(plugin.getConfigManager().getMessage("npc-teleported-to", "%id%", String.valueOf(data.getId())));
                    }
                } else if (click.isRightClick()) {
                    plugin.getNpcManager().teleportNPC(data, player.getLocation());
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playTeleport(player);
                    player.sendMessage(plugin.getConfigManager().getMessage("npc-teleported-here", "%id%", String.valueOf(data.getId())));
                }
                break;

            case 40: // Close
                SoundUtil.playClick(player);
                player.closeInventory();
                break;

            case 44: // Delete
                if (click.isShiftClick()) {
                    plugin.getNpcManager().removeNPC(data.getId());
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playSuccess(player);
                    player.sendMessage(plugin.getConfigManager().getMessage("npc-removed", "%id%", String.valueOf(data.getId())));
                    player.closeInventory();
                } else {
                    player.sendMessage(ColorUtil.colorize("&#FF5858✖ กด Shift + คลิก เพื่อยืนยันการลบหุ่นจำลองตัวนี้!"));
                    SoundUtil.playError(player);
                }
                break;
        }
    }

    private void handleEquipmentClick(Player player, NPCData data, int slot, ClickType click, ItemStack cursor) {
        if (slot == 45) {
            SoundUtil.playClick(player);
            MainNPCGui.open(player, data);
            return;
        }

        if (slot == 49) {
            PlayerInventory pInv = player.getInventory();
            if (pInv.getHelmet() != null) data.getEquipment().put("HELMET", pInv.getHelmet().clone());
            if (pInv.getChestplate() != null) data.getEquipment().put("CHESTPLATE", pInv.getChestplate().clone());
            if (pInv.getLeggings() != null) data.getEquipment().put("LEGGINGS", pInv.getLeggings().clone());
            if (pInv.getBoots() != null) data.getEquipment().put("BOOTS", pInv.getBoots().clone());
            if (pInv.getItemInMainHand() != null && pInv.getItemInMainHand().getType() != Material.AIR) {
                data.getEquipment().put("MAIN_HAND", pInv.getItemInMainHand().clone());
            }
            if (pInv.getItemInOffHand() != null && pInv.getItemInOffHand().getType() != Material.AIR) {
                data.getEquipment().put("OFF_HAND", pInv.getItemInOffHand().clone());
            }

            plugin.getNpcManager().updateEquipment(data);
            plugin.getStorageManager().saveNPCs();
            SoundUtil.playEquip(player);
            player.sendMessage(plugin.getConfigManager().getMessage("equip-copied", "%id%", String.valueOf(data.getId())));
            EquipmentGui.open(player, data);
            return;
        }

        if (slot == 51) {
            data.getEquipment().clear();
            plugin.getNpcManager().updateEquipment(data);
            plugin.getStorageManager().saveNPCs();
            SoundUtil.playEquip(player);
            EquipmentGui.open(player, data);
            return;
        }

        String slotKey = null;
        if (slot == 10) slotKey = "HELMET";
        else if (slot == 19) slotKey = "CHESTPLATE";
        else if (slot == 28) slotKey = "LEGGINGS";
        else if (slot == 37) slotKey = "BOOTS";
        else if (slot == 23) slotKey = "MAIN_HAND";
        else if (slot == 25) slotKey = "OFF_HAND";

        if (slotKey != null) {
            if (click.isRightClick()) {
                data.getEquipment().remove(slotKey);
                plugin.getNpcManager().updateEquipment(data);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playEquip(player);
                EquipmentGui.open(player, data);
            } else if (cursor != null && cursor.getType() != Material.AIR) {
                data.getEquipment().put(slotKey, cursor.clone());
                plugin.getNpcManager().updateEquipment(data);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playEquip(player);
                EquipmentGui.open(player, data);
            }
        }
    }

    private void handlePoseClick(Player player, NPCData data, int slot) {
        if (slot == 45) {
            SoundUtil.playClick(player);
            MainNPCGui.open(player, data);
            return;
        }

        String preset = null;
        switch (slot) {
            case 10: preset = "GUARD"; break;
            case 11: preset = "ATTACK"; break;
            case 12: preset = "DEFEND"; break;
            case 13: preset = "SALUTE"; break;
            case 14: preset = "POINTING"; break;
            case 15: preset = "CHEERING"; break;
            case 16: preset = "ZOMBIE"; break;
            case 22: preset = "SITTING"; break;
            case 31: preset = "STANDING"; break;
        }

        if (preset != null) {
            data.getPose().applyPreset(preset);
            plugin.getNpcManager().updatePose(data);
            plugin.getStorageManager().saveNPCs();
            SoundUtil.playSuccess(player);
            player.sendMessage(plugin.getConfigManager().getMessage("pose-updated", "%id%", String.valueOf(data.getId()), "%pose%", preset));
            PoseGui.open(player, data);
        }
    }

    private void handleCommandsClick(Player player, NPCData data, int slot, ClickType click) {
        if (slot == 45) {
            SoundUtil.playClick(player);
            MainNPCGui.open(player, data);
            return;
        }

        if (slot == 49) {
            ChatInputListener.requestInput(player, data, ChatInputListener.InputMode.ADD_ACTION,
                    "พิมพ์คำสั่งหรือข้อความที่ต้องการให้ทำงานเมื่อคลิกหุ่น (เช่น spawn หรือ give %player% diamond 1):");
            return;
        }

        if (slot == 51 && click.isShiftClick()) {
            data.getCommands().clear();
            plugin.getStorageManager().saveNPCs();
            SoundUtil.playSuccess(player);
            CommandListGui.open(player, data);
            return;
        }

        int startSlot = 10;
        int listIndex = -1;
        int currentSlot = startSlot;

        for (int i = 0; i < data.getCommands().size(); i++) {
            if (currentSlot == slot) {
                listIndex = i;
                break;
            }
            currentSlot++;
            if (currentSlot % 9 == 8) currentSlot += 2;
        }

        if (listIndex != -1 && listIndex < data.getCommands().size()) {
            NPCCommand cmd = data.getCommands().get(listIndex);
            if (click.isShiftClick() && click.isRightClick()) {
                data.getCommands().remove(listIndex);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playSuccess(player);
                player.sendMessage(plugin.getConfigManager().getMessage("cmd-removed", "%index%", String.valueOf(listIndex + 1)));
                CommandListGui.open(player, data);
            } else if (click == ClickType.DROP || click == ClickType.CONTROL_DROP || click == ClickType.MIDDLE) {
                double curCd = cmd.getCooldown();
                double nextCd = COOLDOWN_PRESETS[0];
                for (int i = 0; i < COOLDOWN_PRESETS.length; i++) {
                    if (Math.abs(curCd - COOLDOWN_PRESETS[i]) < 0.05) {
                        nextCd = COOLDOWN_PRESETS[(i + 1) % COOLDOWN_PRESETS.length];
                        break;
                    }
                }
                cmd.setCooldown(nextCd);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playClick(player);
                CommandListGui.open(player, data);
            } else if (click.isLeftClick()) {
                CommandType[] types = CommandType.values();
                CommandType nextType = types[(cmd.getType().ordinal() + 1) % types.length];
                cmd.setType(nextType);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playClick(player);
                CommandListGui.open(player, data);
            } else if (click.isRightClick()) {
                ClickTrigger[] triggers = ClickTrigger.values();
                ClickTrigger nextTrig = triggers[(cmd.getClickTrigger().ordinal() + 1) % triggers.length];
                cmd.setClickTrigger(nextTrig);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playClick(player);
                CommandListGui.open(player, data);
            }
        }
    }

    private void handleHologramClick(Player player, NPCData data, int slot, ClickType click) {
        if (slot == 45) {
            SoundUtil.playClick(player);
            MainNPCGui.open(player, data);
            return;
        }

        if (slot == 4) {
            if (click.isRightClick()) {
                data.setName("");
                plugin.getNpcManager().updateHologram(data);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playSuccess(player);
                HologramGui.open(player, data);
            } else {
                ChatInputListener.requestInput(player, data, ChatInputListener.InputMode.RENAME_HOLO_GUI,
                        "พิมพ์ชื่อหลักบนหัวหุ่น (รองรับสี & / &#HEX / PlaceholderAPI หรือพิมพ์ none เพื่อซ่อน):");
            }
            return;
        }

        if (slot == 48) {
            if (click.isShiftClick()) {
                data.setHologramOffset(0.25);
            } else if (click.isRightClick()) {
                data.setHologramOffset(Math.round((data.getHologramOffset() - 0.1) * 100.0) / 100.0);
            } else {
                data.setHologramOffset(Math.round((data.getHologramOffset() + 0.1) * 100.0) / 100.0);
            }
            plugin.getNpcManager().updateHologram(data);
            plugin.getStorageManager().saveNPCs();
            SoundUtil.playClick(player);
            HologramGui.open(player, data);
            return;
        }

        if (slot == 49) {
            ChatInputListener.requestInput(player, data, ChatInputListener.InputMode.ADD_HOLO,
                    "พิมพ์ข้อความบรรทัดใหม่ที่ต้องการเพิ่ม (หรือพิมพ์ blank เพื่อเว้นบรรทัดว่าง):");
            return;
        }

        if (slot == 51 && click.isShiftClick()) {
            data.getHologramLines().clear();
            plugin.getNpcManager().updateHologram(data);
            plugin.getStorageManager().saveNPCs();
            SoundUtil.playSuccess(player);
            HologramGui.open(player, data);
            return;
        }

        int startSlot = 10;
        int listIndex = -1;
        int currentSlot = startSlot;

        for (int i = 0; i < data.getHologramLines().size(); i++) {
            if (currentSlot == slot) {
                listIndex = i;
                break;
            }
            currentSlot++;
            if (currentSlot % 9 == 8) currentSlot += 2;
        }

        if (listIndex != -1 && listIndex < data.getHologramLines().size()) {
            List<String> lines = data.getHologramLines();
            if (click.isShiftClick() && click.isRightClick()) {
                lines.remove(listIndex);
                plugin.getNpcManager().updateHologram(data);
                plugin.getStorageManager().saveNPCs();
                SoundUtil.playSuccess(player);
                player.sendMessage(plugin.getConfigManager().getMessage("holo-line-removed", "%line%", String.valueOf(listIndex + 1)));
                HologramGui.open(player, data);
            } else if (click == ClickType.DROP || click == ClickType.CONTROL_DROP || click == ClickType.MIDDLE) {
                if (listIndex > 0) {
                    String tmp = lines.get(listIndex - 1);
                    lines.set(listIndex - 1, lines.get(listIndex));
                    lines.set(listIndex, tmp);
                    plugin.getNpcManager().updateHologram(data);
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playClick(player);
                    HologramGui.open(player, data);
                }
            } else if (click.isShiftClick() && click.isLeftClick()) {
                if (listIndex < lines.size() - 1) {
                    String tmp = lines.get(listIndex + 1);
                    lines.set(listIndex + 1, lines.get(listIndex));
                    lines.set(listIndex, tmp);
                    plugin.getNpcManager().updateHologram(data);
                    plugin.getStorageManager().saveNPCs();
                    SoundUtil.playClick(player);
                    HologramGui.open(player, data);
                }
            } else if (click.isLeftClick()) {
                String currentRaw = lines.get(listIndex);
                ChatInputListener.requestInput(player, data, ChatInputListener.InputMode.EDIT_HOLO_LINE, listIndex,
                        "พิมพ์ข้อความใหม่สำหรับบรรทัดที่ &#FFD166#" + (listIndex + 1) + " &7(เดิม: &f" + currentRaw.replace("§", "&") + "&7):");
            } else if (click.isRightClick()) {
                ChatInputListener.requestInput(player, data, ChatInputListener.InputMode.INSERT_HOLO_LINE, listIndex + 1,
                        "พิมพ์ข้อความที่ต้องการแทรกเป็นบรรทัดที่ &#FFD166#" + (listIndex + 2) + "&f:");
            }
        }
    }
}
