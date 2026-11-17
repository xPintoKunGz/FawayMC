package com.mery.npc.command;

import com.mery.npc.MeryNPC;
import com.mery.npc.gui.HologramGui;
import com.mery.npc.gui.MainNPCGui;
import com.mery.npc.listener.ChatInputListener;
import com.mery.npc.manager.SkinManager;
import com.mery.npc.model.ClickTrigger;
import com.mery.npc.model.CommandType;
import com.mery.npc.model.NPCCommand;
import com.mery.npc.model.NPCData;
import com.mery.npc.util.ColorUtil;
import com.mery.npc.util.PlaceholderUtil;
import com.mery.npc.util.SoundUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.RayTraceResult;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MeryNPCCommand implements CommandExecutor, TabCompleter {

    private final MeryNPC plugin;
    private static final Map<UUID, Integer> selectedNpc = new ConcurrentHashMap<>();

    public MeryNPCCommand(MeryNPC plugin) {
        this.plugin = plugin;
    }

    public static void setSelectedNpc(UUID playerUuid, int npcId) {
        selectedNpc.put(playerUuid, npcId);
    }

    public static Integer getSelectedNpcId(UUID playerUuid) {
        return selectedNpc.get(playerUuid);
    }

    private NPCData getSelectedOrNearest(Player player) {
        Integer selId = selectedNpc.get(player.getUniqueId());
        if (selId != null) {
            NPCData sel = plugin.getNpcManager().getNPC(selId);
            if (sel != null) return sel;
        }

        // Raytrace entity player is looking at
        try {
            RayTraceResult ray = player.getWorld().rayTraceEntities(
                    player.getEyeLocation(),
                    player.getEyeLocation().getDirection(),
                    10.0,
                    e -> !e.equals(player) && plugin.getNpcManager().getNPCByEntity(e) != null
            );
            if (ray != null && ray.getHitEntity() != null) {
                NPCData hitNpc = plugin.getNpcManager().getNPCByEntity(ray.getHitEntity());
                if (hitNpc != null) {
                    selectedNpc.put(player.getUniqueId(), hitNpc.getId());
                    return hitNpc;
                }
            }
        } catch (Throwable ignored) {}

        // Search nearest within 10 blocks
        NPCData nearest = null;
        double nearestDistSq = 100.0;
        for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
            if (data.getLocation() != null && data.getLocation().getWorld() != null && data.getLocation().getWorld().equals(player.getWorld())) {
                double distSq = data.getLocation().distanceSquared(player.getLocation());
                if (distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = data;
                }
            }
        }
        if (nearest != null) {
            selectedNpc.put(player.getUniqueId(), nearest.getId());
            return nearest;
        }

        return null;
    }

    private NPCData getSelectedOrTarget(Player player, String[] args, int idArgIndex) {
        if (idArgIndex >= 0 && args.length > idArgIndex) {
            try {
                int id = Integer.parseInt(args[idArgIndex]);
                NPCData byId = plugin.getNpcManager().getNPC(id);
                if (byId != null) return byId;
            } catch (NumberFormatException ignored) {}
        }
        return getSelectedOrNearest(player);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("merynpc.admin")) {
            sender.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player) {
                Player p = (Player) sender;
                NPCData target = getSelectedOrNearest(p);
                if (target != null) {
                    SoundUtil.playClick(p);
                    MainNPCGui.open(p, target);
                    return true;
                }
            }
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        if (sub.equals("help")) {
            sendHelp(sender, label);
            return true;
        }

        if (sub.equals("reload")) {
            plugin.getConfigManager().loadConfigs();
            plugin.getStorageManager().loadNPCs();
            sender.sendMessage(plugin.getConfigManager().getMessage("config-reloaded"));
            if (sender instanceof Player) SoundUtil.playSuccess((Player) sender);
            return true;
        }

        if (sub.equals("list") || sub.equals("catalog")) {
            Map<Integer, NPCData> all = plugin.getNpcManager().getAllNPCs();
            sender.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFคลังหุ่นจำลอง MeryNPC Studio &8[&#FFD166" + all.size() + " ตัว&8]:"));
            if (all.isEmpty()) {
                sender.sendMessage(ColorUtil.colorize("&7  (ยังไม่มีหุ่นจำลองในระบบ พิมพ์ &#FF6CAB/" + label + " spawn <ชื่อ> &7เพื่อสร้าง)"));
            } else {
                for (NPCData data : all.values()) {
                    String worldName = data.getWorldName();
                    int x = (int) Math.floor(data.getX());
                    int y = (int) Math.floor(data.getY());
                    int z = (int) Math.floor(data.getZ());
                    String title = data.getName().isEmpty() ? "&7(ไร้ชื่อ)" : data.getName();
                    sender.sendMessage(ColorUtil.colorize("  &#FFD166#" + data.getId() + " &f" + title + " &8| &7" + worldName + " (" + x + "," + y + "," + z + ") &8| &#FF6CAB" + data.getType() + " &8| &#43E97B" + data.getCommands().size() + " Actions"));
                }
            }
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Stick / Wand: /npc stick | /npc wand
        if (sub.equals("stick") || sub.equals("wand") || sub.equals("tool")) {
            ItemStack stick = new ItemStack(Material.STICK);
            ItemMeta meta = stick.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ColorUtil.colorize("&#FF6CAB✦ &#FFD166MeryNPC Target Stick &#FF6CAB✦"));
                meta.setLore(Arrays.asList(
                        ColorUtil.colorize("&7คลิกซ้าย หรือ คลิกขวา ที่ตัวหุ่นจำลอง"),
                        ColorUtil.colorize("&7เพื่อเลือกเป้าหมาย (Target) ทันที!"),
                        ColorUtil.colorize(""),
                        ColorUtil.colorize("&#43E97B[Shift + คลิกขวา] &aเปิดหน้าต่าง MeryNPC Studio"),
                        ColorUtil.colorize("&8(ใช้ไอเทม Stick ธรรมดาทั่วไปคลิกเลือกได้เช่นกัน)")
                ));
                stick.setItemMeta(meta);
            }
            player.getInventory().addItem(stick);
            player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#43E97Bได้รับไม้ &#FFD166MeryNPC Target Stick &#43E97Bแล้ว! &7(คลิกที่ตัว NPC เพื่อเลือก Target ได้เลย หรือใช้ Stick ธรรมดาก็ได้)"));
            SoundUtil.playSuccess(player);
            return true;
        }

        // Spawn / Create Persona: /mnpc spawn [name...]
        if (sub.equals("spawn") || sub.equals("create") || sub.equals("new")) {
            String type = "PLAYER";
            List<String> nameParts = new ArrayList<>();
            for (int i = 1; i < args.length; i++) {
                if ((args[i].equalsIgnoreCase("--type") || args[i].equalsIgnoreCase("-t")) && i + 1 < args.length) {
                    type = args[++i].toUpperCase();
                } else {
                    nameParts.add(args[i]);
                }
            }
            String name = String.join(" ", nameParts).trim();

            NPCData created = plugin.getNpcManager().createNPC(name, player.getLocation(), type);
            selectedNpc.put(player.getUniqueId(), created.getId());
            plugin.getStorageManager().saveNPCs();

            player.sendMessage(plugin.getConfigManager().getMessage("npc-created", "%id%", String.valueOf(created.getId()), "%name%", name.isEmpty() ? "(ไร้ชื่อ)" : name));
            SoundUtil.playSuccess(player);
            return true;
        }

        // Target / Select Persona: /mnpc target [id|name]
        if (sub.equals("target") || sub.equals("select") || sub.equals("sel")) {
            if (args.length > 1) {
                try {
                    int id = Integer.parseInt(args[1]);
                    NPCData data = plugin.getNpcManager().getNPC(id);
                    if (data != null) {
                        selectedNpc.put(player.getUniqueId(), id);
                        player.sendMessage(plugin.getConfigManager().getMessage("npc-selected", "%id%", String.valueOf(id), "%name%", data.getName()));
                        SoundUtil.playSuccess(player);
                        return true;
                    }
                } catch (NumberFormatException ignored) {
                    String searchName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                    for (NPCData data : plugin.getNpcManager().getAllNPCs().values()) {
                        if (ColorUtil.stripColor(data.getName()).equalsIgnoreCase(searchName)) {
                            selectedNpc.put(player.getUniqueId(), data.getId());
                            player.sendMessage(plugin.getConfigManager().getMessage("npc-selected", "%id%", String.valueOf(data.getId()), "%name%", data.getName()));
                            SoundUtil.playSuccess(player);
                            return true;
                        }
                    }
                }
            }

            NPCData nearest = getSelectedOrNearest(player);
            if (nearest != null) {
                selectedNpc.put(player.getUniqueId(), nearest.getId());
                player.sendMessage(plugin.getConfigManager().getMessage("npc-selected", "%id%", String.valueOf(nearest.getId()), "%name%", nearest.getName()));
                SoundUtil.playSuccess(player);
            } else {
                player.sendMessage(plugin.getConfigManager().getMessage("no-npc-selected"));
                SoundUtil.playError(player);
            }
            return true;
        }

        // Erase / Remove All
        if ((sub.equals("erase") || sub.equals("delete") || sub.equals("remove")) && args.length > 1 && args[1].equalsIgnoreCase("all")) {
            int count = plugin.getNpcManager().getAllNPCs().size();
            plugin.getNpcManager().despawnAll();
            plugin.getNpcManager().getAllNPCs().clear();
            selectedNpc.clear();
            plugin.getStorageManager().saveNPCs();
            player.sendMessage(ColorUtil.colorize("&#43E97B✔ ล้างหุ่นจำลองทั้งหมด (" + count + " ตัว) ออกจากระบบแล้ว!"));
            SoundUtil.playSuccess(player);
            return true;
        }

        boolean takesOptionalIdFirst = sub.equals("erase") || sub.equals("remove") || sub.equals("delete")
                || sub.equals("studio") || sub.equals("gui") || sub.equals("edit")
                || sub.equals("warp") || sub.equals("tp") || sub.equals("summon") || sub.equals("tphere")
                || sub.equals("glow");
        NPCData target = getSelectedOrTarget(player, args, takesOptionalIdFirst ? 1 : -1);
        if (target == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-npc-selected"));
            SoundUtil.playError(player);
            return true;
        }

        // Studio GUI: /mnpc studio [id]
        if (sub.equals("studio") || sub.equals("gui") || sub.equals("edit") || sub.equals("menu")) {
            SoundUtil.playClick(player);
            MainNPCGui.open(player, target);
            return true;
        }

        // Erase / Delete Persona: /mnpc erase [id]
        if (sub.equals("erase") || sub.equals("delete") || sub.equals("remove")) {
            plugin.getNpcManager().removeNPC(target.getId());
            selectedNpc.remove(player.getUniqueId());
            plugin.getStorageManager().saveNPCs();
            player.sendMessage(plugin.getConfigManager().getMessage("npc-removed", "%id%", String.valueOf(target.getId())));
            SoundUtil.playSuccess(player);
            return true;
        }

        // Particle Aura: /mnpc aura <none|flame|soul|enchant|cherry|end|heart|totem>
        if (sub.equals("aura") || sub.equals("particle")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFรูปแบบออร่าที่มี: &fNONE, FLAME, SOUL, ENCHANT, CHERRY, END, HEART, TOTEM"));
                return true;
            }
            String aura = args[1].toUpperCase();
            target.setParticleAura(aura);
            plugin.getStorageManager().saveNPCs();
            player.sendMessage(ColorUtil.colorize("&#43E97B✔ ตั้งค่าวงแหวนพาร์ติเคิลของหุ่น #" + target.getId() + " เป็น &#FFD166" + aura + " &#43E97Bแล้ว!"));
            SoundUtil.playSuccess(player);
            return true;
        }

        // Body Scale: /mnpc scale <0.5 - 2.5>
        if (sub.equals("scale") || sub.equals("size")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFการใช้งาน: &f/" + label + " scale <0.5 ถึง 2.5> &7(เช่น 0.7 ตัวเล็ก, 1.0 ปกติ, 1.8 ยักษ์)"));
                return true;
            }
            try {
                double sc = Double.parseDouble(args[1]);
                target.setScale(sc);
                plugin.getNpcManager().updateVisuals(target);
                plugin.getStorageManager().saveNPCs();
                player.sendMessage(ColorUtil.colorize("&#43E97B✔ ปรับขนาดตัวหุ่น #" + target.getId() + " เป็น &#FFD166" + target.getScale() + "x &#43E97Bแล้ว!"));
                SoundUtil.playSuccess(player);
            } catch (NumberFormatException e) {
                player.sendMessage(ColorUtil.colorize("&#FF5858✖ กรุณาระบุตัวเลขขนาด เช่น 0.8 หรือ 1.5"));
            }
            return true;
        }

        // Glowing Aura Toggle: /mnpc glow
        if (sub.equals("glow")) {
            target.setGlowing(!target.isGlowing());
            plugin.getNpcManager().updateVisuals(target);
            plugin.getStorageManager().saveNPCs();
            player.sendMessage(ColorUtil.colorize("&#43E97B✔ ขอบเรืองแสงของหุ่น #" + target.getId() + ": " + (target.isGlowing() ? "&#43E97Bเปิดใช้งาน" : "&#FF5858ปิดใช้งาน")));
            SoundUtil.playClick(player);
            return true;
        }

        // Archetype / Entity Model: /mnpc archetype <type>
        if (sub.equals("archetype") || sub.equals("type") || sub.equals("model")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFเผ่าพันธุ์ที่รองรับ: &fPLAYER, VILLAGER, ALLAY, IRON_GOLEM, WARDEN, ARMOR_STAND"));
                return true;
            }
            String newType = args[1].toUpperCase();
            target.setType(newType);
            plugin.getNpcManager().spawnNPC(target);
            plugin.getStorageManager().saveNPCs();
            player.sendMessage(ColorUtil.colorize("&#43E97B✔ เปลี่ยนเผ่าพันธุ์หุ่น #" + target.getId() + " เป็น &#FFD166" + newType + " &#43E97Bแล้ว!"));
            SoundUtil.playSuccess(player);
            return true;
        }

        // Clone Persona: /mnpc clone [newName]
        if (sub.equals("clone") || sub.equals("copy")) {
            String copyName = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : target.getName();
            NPCData cloned = plugin.getNpcManager().createNPC(copyName, player.getLocation(), target.getType());
            cloned.setSkinName(target.getSkinName());
            cloned.setSkinValue(target.getSkinValue());
            cloned.setSkinSignature(target.getSkinSignature());
            cloned.setGlowing(target.isGlowing());
            cloned.setScale(target.getScale());
            cloned.setParticleAura(target.getParticleAura());
            cloned.setLookAtPlayer(target.isLookAtPlayer());
            cloned.setHologramLines(new ArrayList<>(target.getHologramLines()));
            cloned.getEquipment().putAll(target.getEquipment());
            for (NPCCommand c : target.getCommands()) {
                cloned.getCommands().add(NPCCommand.deserialize(c.serialize()));
            }
            plugin.getNpcManager().spawnNPC(cloned);
            selectedNpc.put(player.getUniqueId(), cloned.getId());
            plugin.getStorageManager().saveNPCs();
            player.sendMessage(ColorUtil.colorize("&#43E97B✔ โคลนหุ่น #" + target.getId() + " เป็นหุ่นตัวใหม่ &#FFD166#" + cloned.getId() + " &#43E97Bเรียบร้อยแล้ว!"));
            SoundUtil.playSuccess(player);
            return true;
        }

        // Warp to Persona: /mnpc warp
        if (sub.equals("warp") || sub.equals("tp")) {
            if (target.getLocation() != null) {
                player.teleport(target.getLocation());
                SoundUtil.playTeleport(player);
                player.sendMessage(plugin.getConfigManager().getMessage("npc-teleported-to", "%id%", String.valueOf(target.getId())));
            }
            return true;
        }

        // Summon Persona Here: /mnpc summon
        if (sub.equals("summon") || sub.equals("tphere") || sub.equals("movehere")) {
            plugin.getNpcManager().teleportNPC(target, player.getLocation());
            plugin.getStorageManager().saveNPCs();
            SoundUtil.playTeleport(player);
            player.sendMessage(plugin.getConfigManager().getMessage("npc-teleported-here", "%id%", String.valueOf(target.getId())));
            return true;
        }

        // Title / Rename: /npc rename <ชื่อใหม่ | none>
        if (sub.equals("rename") || sub.equals("title") || sub.equals("name")) {
            if (args.length < 2) {
                ChatInputListener.requestInput(player, target, ChatInputListener.InputMode.RENAME,
                        "พิมพ์ชื่อใหม่ของหุ่น #" + target.getId() + " (รองรับสี & / &#HEX / PlaceholderAPI หรือพิมพ์ none เพื่อไร้ชื่อ):");
                return true;
            }
            String newName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            if (newName.equalsIgnoreCase("none") || newName.equalsIgnoreCase("empty") || newName.equalsIgnoreCase("clear") || newName.equals("\"\"")) {
                newName = "";
            }
            target.setName(newName);
            plugin.getNpcManager().updateHologram(target);
            plugin.getStorageManager().saveNPCs();
            player.sendMessage(plugin.getConfigManager().getMessage("npc-renamed", "%id%", String.valueOf(target.getId()), "%name%", newName.isEmpty() ? "(ไร้ชื่อ)" : PlaceholderUtil.format(newName, player, target)));
            SoundUtil.playSuccess(player);
            return true;
        }

        // 3D Skin & Skin URL: /npc skin <player_name | me | URL | url <URL> [slim]>
        if (sub.equals("skin")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtil.colorize("&#FF5858✖ การใช้งาน: &f/" + label + " skin <ชื่อผู้เล่น | me | ลิงก์ URL รูปสกิน .png> [slim]"));
                return true;
            }
            int argIdx = 1;
            if ((args[1].equalsIgnoreCase("url") || args[1].equalsIgnoreCase("-u") || args[1].equalsIgnoreCase("--url")) && args.length >= 3) {
                argIdx = 2;
            }
            String rawSkinArg = args[argIdx];
            boolean slim = false;
            for (int i = argIdx + 1; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("slim") || args[i].equalsIgnoreCase("-s") || args[i].equalsIgnoreCase("--slim")) {
                    slim = true;
                }
            }

            String skinInput = (rawSkinArg.equalsIgnoreCase("me") || rawSkinArg.equalsIgnoreCase("-c")) ? player.getName() : rawSkinArg;
            String displayLabel = SkinManager.isUrl(skinInput) ? "URL Skin" : skinInput;
            player.sendMessage(plugin.getConfigManager().getMessage("skin-updating", "%name%", displayLabel));

            plugin.getSkinManager().applySkinToNPC(target, skinInput, slim, () -> {
                plugin.getStorageManager().saveNPCs();
                player.sendMessage(plugin.getConfigManager().getMessage("skin-updated", "%id%", String.valueOf(target.getId()), "%name%", displayLabel));
                SoundUtil.playSuccess(player);
            }, () -> {
                plugin.getStorageManager().saveNPCs();
                if (SkinManager.isUrl(skinInput)) {
                    player.sendMessage(ColorUtil.colorize("&#FF5858✖ ไม่สามารถดึงสกินจาก URL ได้ กรุณาตรวจสอบว่าเป็นลิงก์ไฟล์สกิน .png โดยตรง (เช่น Imgur / NameMC / MineSkin)"));
                    SoundUtil.playError(player);
                } else {
                    player.sendMessage(plugin.getConfigManager().getMessage("skin-updated", "%id%", String.valueOf(target.getId()), "%name%", displayLabel));
                    SoundUtil.playSuccess(player);
                }
            });
            return true;
        }

        // Stance / Pose: /mnpc stance <preset>
        if (sub.equals("stance") || sub.equals("pose")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFท่วงท่าที่มี: &fstanding, guard, attack, defend, salute, pointing, cheering, sitting, zombie"));
                return true;
            }
            String preset = args[1].toUpperCase();
            target.getPose().applyPreset(preset);
            plugin.getNpcManager().updatePose(target);
            plugin.getStorageManager().saveNPCs();
            player.sendMessage(plugin.getConfigManager().getMessage("pose-updated", "%id%", String.valueOf(target.getId()), "%pose%", preset));
            SoundUtil.playSuccess(player);
            return true;
        }

        // Gear / Equip: /mnpc gear <hand|offhand|helmet|chest|legs|boots|mirror|wipe>
        if (sub.equals("gear") || sub.equals("equip")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFการใช้งาน: &f/" + label + " gear <hand|offhand|helmet|chest|legs|boots|mirror|wipe>"));
                return true;
            }
            String slot = args[1].toLowerCase();
            PlayerInventory pInv = player.getInventory();

            if (slot.equals("mirror") || slot.equals("copy")) {
                if (pInv.getHelmet() != null) target.getEquipment().put("HELMET", pInv.getHelmet().clone());
                if (pInv.getChestplate() != null) target.getEquipment().put("CHESTPLATE", pInv.getChestplate().clone());
                if (pInv.getLeggings() != null) target.getEquipment().put("LEGGINGS", pInv.getLeggings().clone());
                if (pInv.getBoots() != null) target.getEquipment().put("BOOTS", pInv.getBoots().clone());
                if (pInv.getItemInMainHand() != null && pInv.getItemInMainHand().getType() != Material.AIR) {
                    target.getEquipment().put("MAIN_HAND", pInv.getItemInMainHand().clone());
                }
                if (pInv.getItemInOffHand() != null && pInv.getItemInOffHand().getType() != Material.AIR) {
                    target.getEquipment().put("OFF_HAND", pInv.getItemInOffHand().clone());
                }
                plugin.getNpcManager().updateEquipment(target);
                plugin.getStorageManager().saveNPCs();
                player.sendMessage(plugin.getConfigManager().getMessage("equip-copied", "%id%", String.valueOf(target.getId())));
                SoundUtil.playEquip(player);
                return true;
            }

            if (slot.equals("wipe") || slot.equals("clear")) {
                target.getEquipment().clear();
                plugin.getNpcManager().updateEquipment(target);
                plugin.getStorageManager().saveNPCs();
                player.sendMessage(ColorUtil.colorize("&#43E97B✔ ถอดเครื่องแต่งกายและอาวุธของหุ่นออกทั้งหมดแล้ว!"));
                SoundUtil.playEquip(player);
                return true;
            }

            ItemStack held = pInv.getItemInMainHand();
            String equipKey = null;
            switch (slot) {
                case "hand":
                case "mainhand": equipKey = "MAIN_HAND"; break;
                case "offhand": held = pInv.getItemInOffHand(); equipKey = "OFF_HAND"; break;
                case "helmet":
                case "head": equipKey = "HELMET"; break;
                case "chestplate":
                case "chest": equipKey = "CHESTPLATE"; break;
                case "leggings":
                case "legs": equipKey = "LEGGINGS"; break;
                case "boots":
                case "feet": equipKey = "BOOTS"; break;
            }

            if (equipKey != null) {
                if (held != null && held.getType() != Material.AIR) {
                    target.getEquipment().put(equipKey, held.clone());
                } else {
                    target.getEquipment().remove(equipKey);
                }
                plugin.getNpcManager().updateEquipment(target);
                plugin.getStorageManager().saveNPCs();
                player.sendMessage(plugin.getConfigManager().getMessage("equip-updated", "%slot%", equipKey));
                SoundUtil.playEquip(player);
            }
            return true;
        }

        // Banner / Hologram: /mnpc banner <gui|list|set|edit|insert|add|del|clear> [line] [text...]
        if (sub.equals("banner") || sub.equals("holo") || sub.equals("hologram")) {
            if (args.length < 2 || args[1].equalsIgnoreCase("gui") || args[1].equalsIgnoreCase("studio")) {
                SoundUtil.playClick(player);
                HologramGui.open(player, target);
                return true;
            }
            String action = args[1].toLowerCase();

            if (action.equals("list") || action.equals("info")) {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFรายการบรรทัด Hologram ของหุ่น &#FFD166#" + target.getId() + "&7:"));
                String titleRaw = target.getName().isEmpty() ? "&7(ซ่อนชื่อหลัก)" : target.getName();
                player.sendMessage(ColorUtil.colorize("  &#FF6CAB[ชื่อหลัก #0] &f" + PlaceholderUtil.format(titleRaw, player, target) + " &8(&7โค้ด: &e" + titleRaw.replace("§", "&") + "&8)"));
                List<String> lines = target.getHologramLines();
                if (lines.isEmpty()) {
                    player.sendMessage(ColorUtil.colorize("  &7(ยังไม่มีบรรทัดเสริม พิมพ์ &#FF6CAB/" + label + " banner add <ข้อความ> &7เพื่อเพิ่ม)"));
                } else {
                    for (int i = 0; i < lines.size(); i++) {
                        String raw = lines.get(i);
                        String live = PlaceholderUtil.format(raw, player, target);
                        player.sendMessage(ColorUtil.colorize("  &#FFD166บรรทัด #" + (i + 1) + " &8» &f" + live + " &8(&7โค้ด: &e" + raw.replace("§", "&") + "&8)"));
                    }
                }
                player.sendMessage(ColorUtil.colorize("&7  💡 แก้ไขทีละบรรทัด: &#43E97B/" + label + " banner set <เลขบรรทัด> <ข้อความใหม่> &7หรือ &#43E97B/" + label + " banner gui"));
                return true;
            }

            if (action.equals("set") || action.equals("edit") || action.equals("line")) {
                if (args.length < 4) {
                    player.sendMessage(ColorUtil.colorize("&#FF5858✖ การใช้งาน: &f/" + label + " banner set <เลขบรรทัด 1,2,3...> <ข้อความใหม่...>"));
                    return true;
                }
                try {
                    int lineNum = Integer.parseInt(args[2]);
                    String newText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    if (newText.equalsIgnoreCase("blank") || newText.equalsIgnoreCase("empty")) {
                        newText = " ";
                    }
                    if (lineNum == 0) {
                        target.setName(newText.equalsIgnoreCase("none") ? "" : newText);
                        plugin.getNpcManager().updateHologram(target);
                        plugin.getStorageManager().saveNPCs();
                        player.sendMessage(ColorUtil.colorize("&#43E97B✔ แก้ไขชื่อหลัก (บรรทัด #0) เป็น: &f" + target.getName()));
                        SoundUtil.playSuccess(player);
                        return true;
                    }
                    int lineIdx = lineNum - 1;
                    List<String> lines = target.getHologramLines();
                    if (lineIdx >= 0 && lineIdx < lines.size()) {
                        lines.set(lineIdx, newText);
                        plugin.getNpcManager().updateHologram(target);
                        plugin.getStorageManager().saveNPCs();
                        player.sendMessage(ColorUtil.colorize("&#43E97B✔ แก้ไข Hologram บรรทัดที่ &#FFD166#" + lineNum + " &#43E97Bเป็น: &f" + PlaceholderUtil.format(newText, player, target)));
                        SoundUtil.playSuccess(player);
                    } else if (lineIdx == lines.size()) {
                        lines.add(newText);
                        plugin.getNpcManager().updateHologram(target);
                        plugin.getStorageManager().saveNPCs();
                        player.sendMessage(ColorUtil.colorize("&#43E97B✔ เพิ่ม Hologram บรรทัดที่ &#FFD166#" + lineNum + " &#43E97Bเป็น: &f" + PlaceholderUtil.format(newText, player, target)));
                        SoundUtil.playSuccess(player);
                    } else {
                        player.sendMessage(ColorUtil.colorize("&#FF5858✖ ไม่พบบรรทัดที่ #" + lineNum + " (ปัจจุบันมี " + lines.size() + " บรรทัด)"));
                        SoundUtil.playError(player);
                    }
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.colorize("&#FF5858✖ กรุณาระบุเลขบรรทัดเป็นตัวเลข เช่น /" + label + " banner set 1 ข้อความใหม่"));
                }
                return true;
            }

            if (action.equals("insert")) {
                if (args.length < 4) {
                    player.sendMessage(ColorUtil.colorize("&#FF5858✖ การใช้งาน: &f/" + label + " banner insert <เลขบรรทัด> <ข้อความ...>"));
                    return true;
                }
                try {
                    int lineNum = Integer.parseInt(args[2]);
                    String newText = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                    int idx = Math.max(0, Math.min(lineNum - 1, target.getHologramLines().size()));
                    target.getHologramLines().add(idx, newText);
                    plugin.getNpcManager().updateHologram(target);
                    plugin.getStorageManager().saveNPCs();
                    player.sendMessage(ColorUtil.colorize("&#43E97B✔ แทรกข้อความลงในบรรทัดที่ &#FFD166#" + (idx + 1) + " &#43E97Bเรียบร้อยแล้ว!"));
                    SoundUtil.playSuccess(player);
                } catch (NumberFormatException e) {
                    player.sendMessage(ColorUtil.colorize("&#FF5858✖ กรุณาระบุเลขบรรทัดเป็นตัวเลข!"));
                }
                return true;
            }

            if (action.equals("add")) {
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.colorize("&#FF5858✖ กรุณาระบุข้อความที่ต้องการเพิ่ม!"));
                    return true;
                }
                String line = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                target.getHologramLines().add(line);
                plugin.getNpcManager().updateHologram(target);
                plugin.getStorageManager().saveNPCs();
                player.sendMessage(plugin.getConfigManager().getMessage("holo-line-added", "%line%", String.valueOf(target.getHologramLines().size())));
                SoundUtil.playSuccess(player);
                return true;
            }

            if (action.equals("del") || action.equals("remove")) {
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.colorize("&#FF5858✖ กรุณาระบุเลขบรรทัดที่ต้องการลบ"));
                    return true;
                }
                try {
                    int lineIdx = Integer.parseInt(args[2]) - 1;
                    if (lineIdx >= 0 && lineIdx < target.getHologramLines().size()) {
                        target.getHologramLines().remove(lineIdx);
                        plugin.getNpcManager().updateHologram(target);
                        plugin.getStorageManager().saveNPCs();
                        player.sendMessage(plugin.getConfigManager().getMessage("holo-line-removed", "%line%", String.valueOf(lineIdx + 1)));
                        SoundUtil.playSuccess(player);
                    } else {
                        player.sendMessage(ColorUtil.colorize("&#FF5858✖ ไม่พบบรรทัดที่ #" + (lineIdx + 1)));
                    }
                } catch (NumberFormatException ignored) {}
                return true;
            }

            if (action.equals("clear")) {
                target.getHologramLines().clear();
                plugin.getNpcManager().updateHologram(target);
                plugin.getStorageManager().saveNPCs();
                player.sendMessage(plugin.getConfigManager().getMessage("holo-cleared", "%id%", String.valueOf(target.getId())));
                SoundUtil.playSuccess(player);
                return true;
            }
            return true;
        }

        // Direct BungeeCord Server Transfer Shortcut: /mnpc server <serverName> | /mnpc send <serverName>
        if (sub.equals("server") || sub.equals("bungee") || sub.equals("send")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFการใช้งาน: &f/" + label + " server <ชื่อเซิร์ฟเวอร์ BungeeCord> &7(เช่น lobby, survival)"));
                return true;
            }
            String serverName = args[args.length - 1];
            NPCCommand npcCmd = new NPCCommand("server " + serverName, CommandType.SERVER, ClickTrigger.ANY);
            target.getCommands().add(npcCmd);
            plugin.getStorageManager().saveNPCs();
            player.sendMessage(ColorUtil.colorize("&#43E97B✔ ผูกคำสั่งย้ายเซิร์ฟเวอร์ BungeeCord ไปยัง &#FFD166" + serverName + " &#43E97Bให้หุ่น #" + target.getId() + " เรียบร้อยแล้ว!"));
            SoundUtil.playSuccess(player);
            return true;
        }

        // Action Studio Command: /mnpc action <add|del|list|clear>
        if (sub.equals("action") || sub.equals("cmd") || sub.equals("command")) {
            if (args.length < 2) {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFการใช้งาน: &f/" + label + " action <add|del|list|clear>"));
                return true;
            }
            String act = args[1].toLowerCase();

            if (act.equals("add")) {
                if (args.length < 3) {
                    player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFการใช้งาน: &f/" + label + " action add [MODE] [TRIGGER] <คำสั่ง...>"));
                    return true;
                }
                // Support both quick "/mnpc action add <command...>" and "/mnpc action add <MODE> <TRIGGER> <command...>"
                CommandType type = CommandType.CONSOLE;
                ClickTrigger trigger = ClickTrigger.ANY;
                int cmdStart = 2;
                if (args.length >= 5 && isKnownMode(args[2]) && isKnownTrigger(args[3])) {
                    type = CommandType.fromString(args[2]);
                    trigger = ClickTrigger.fromString(args[3]);
                    cmdStart = 4;
                } else if (args.length >= 4 && isKnownMode(args[2])) {
                    type = CommandType.fromString(args[2]);
                    cmdStart = 3;
                }
                String commandStr = String.join(" ", Arrays.copyOfRange(args, cmdStart, args.length));
                if (commandStr.startsWith("/")) {
                    commandStr = commandStr.substring(1).trim();
                }
                String lowerCmd = commandStr.toLowerCase();
                if (lowerCmd.startsWith("server ") || lowerCmd.startsWith("send ")
                        || lowerCmd.startsWith("connect ") || lowerCmd.startsWith("bungee ")) {
                    type = CommandType.SERVER;
                }
                NPCCommand npcCmd = new NPCCommand(commandStr, type, trigger);
                target.getCommands().add(npcCmd);
                plugin.getStorageManager().saveNPCs();
                player.sendMessage(plugin.getConfigManager().getMessage("cmd-added", "%type%", type.name(), "%cmd%", commandStr));
                SoundUtil.playSuccess(player);
                return true;
            }

            if (act.equals("list") || act.equals("info")) {
                player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFรายการ Action ของหุ่น &#FFD166#" + target.getId() + " &8[&#43E97B" + target.getCommands().size() + "&8]:"));
                int idx = 1;
                for (NPCCommand c : target.getCommands()) {
                    player.sendMessage(ColorUtil.colorize("  &#FFD166#" + idx + " &8[&#FF6CAB" + c.getType().name() + "&8] &8(&#43E97B" + c.getClickTrigger().name() + "&8) &f" + c.getCommand()));
                    idx++;
                }
                return true;
            }

            if (act.equals("del") || act.equals("remove")) {
                if (args.length >= 3) {
                    try {
                        int idx = Integer.parseInt(args[2]) - 1;
                        if (idx >= 0 && idx < target.getCommands().size()) {
                            target.getCommands().remove(idx);
                            plugin.getStorageManager().saveNPCs();
                            player.sendMessage(plugin.getConfigManager().getMessage("cmd-removed", "%index%", String.valueOf(idx + 1)));
                            SoundUtil.playSuccess(player);
                        }
                    } catch (NumberFormatException ignored) {}
                }
                return true;
            }

            if (act.equals("clear") || act.equals("reset")) {
                target.getCommands().clear();
                plugin.getStorageManager().saveNPCs();
                player.sendMessage(ColorUtil.colorize("&#43E97B✔ ล้าง Action ทั้งหมดของหุ่นเรียบร้อยแล้ว!"));
                SoundUtil.playSuccess(player);
                return true;
            }
            return true;
        }

        // Auto-Gaze: /mnpc gaze
        if (sub.equals("gaze") || sub.equals("look")) {
            if (args.length > 1) {
                target.setLookAtPlayer(Boolean.parseBoolean(args[1]));
            } else {
                target.setLookAtPlayer(!target.isLookAtPlayer());
            }
            plugin.getStorageManager().saveNPCs();
            String st = target.isLookAtPlayer() ? "&#43E97Bเปิดใช้งาน" : "&#FF5858ปิดใช้งาน";
            player.sendMessage(ColorUtil.colorize("&#FF6CAB✦ &#7366FFระบบสายตามองตามผู้เล่น: " + st));
            SoundUtil.playClick(player);
            return true;
        }

        sendHelp(player, label);
        return true;
    }

    private boolean isKnownMode(String s) {
        for (CommandType t : CommandType.values()) {
            if (t.name().equalsIgnoreCase(s)) return true;
        }
        return false;
    }

    private boolean isKnownTrigger(String s) {
        for (ClickTrigger t : ClickTrigger.values()) {
            if (t.name().equalsIgnoreCase(s)) return true;
        }
        return false;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ColorUtil.colorize(""));
        sender.sendMessage(ColorUtil.colorize("&#FF6CAB╔═════════════════════════════════════════════════════════╗"));
        sender.sendMessage(ColorUtil.colorize("&#FF6CAB║  ✦ &#FFD166M E R Y N P C   S T U D I O   v 2 6 . 3 &#FF6CAB✦            ║"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF╠═════════════════════════════════════════════════════════╣"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " studio &8- &fเปิดแผงควบคุมอัจฉริยะ (ทำทุกอย่างได้ใน GUI!)"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " spawn [ชื่อ] &8- &7อัญเชิญหุ่นจำลอง 3 มิติตัวใหม่"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " rename <ชื่อใหม่|none> &8- &fเปลี่ยนชื่อบนหัวหุ่นจำลองทันที"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " target [id] / stick &8- &7ล็อกเป้าหมายหุ่น (หรือถือ Stick คลิกที่หุ่น!)"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " skin <ชื่อ | me | URL> &8- &fซิงค์สกิน 3 มิติจากชื่อหรือลิงก์ URL"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " aura <รูปแบบ> &8- &7ตั้งวงแหวนพาร์ติเคิลรอบตัวหุ่น"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " scale <0.5-2.5> &8- &7ย่อ/ขยายขนาดร่างกายหุ่น"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " glow &8- &7เปิด/ปิดแสงออร่าเรืองแสงรอบตัว"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " server <ชื่อเซิร์ฟ> &8- &fผูกคำสั่งย้ายเซิร์ฟ BungeeCord ทันที"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " action add <คำสั่ง> &8- &7ผูก Action เมื่อผู้เล่นคลิก"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " gear <slot|mirror|wipe> &8- &7สวมใส่อาวุธและชุดเกราะ"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " banner <gui|set|add|del|list> &8- &fแก้ไขป้าย Hologram ทีละบรรทัด"));
        sender.sendMessage(ColorUtil.colorize("&#7366FF║  &#FF6CAB/" + label + " clone / warp / summon / erase &8- &7จัดการพิกัดหุ่น"));
        sender.sendMessage(ColorUtil.colorize("&#FF6CAB╚═════════════════════════════════════════════════════════╝"));
        sender.sendMessage(ColorUtil.colorize(""));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("merynpc.admin")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = Arrays.asList(
                    "studio", "spawn", "rename", "target", "stick", "skin", "aura", "scale", "glow",
                    "server", "send", "action", "gear", "stance", "banner", "archetype", "gaze",
                    "clone", "warp", "summon", "erase", "list", "reload", "help"
            );
            return filter(subs, args[0]);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            switch (sub) {
                case "rename":
                case "title":
                case "name":
                    if (sender instanceof Player) {
                        NPCData target = getSelectedOrNearest((Player) sender);
                        if (target != null && !target.getName().isEmpty()) {
                            return filter(Arrays.asList(target.getName().replace("§", "&"), "none"), args[1]);
                        }
                    }
                    return filter(Collections.singletonList("none"), args[1]);
                case "skin":
                    List<String> skinSuggestions = new ArrayList<>(Arrays.asList("me", "url", "Steve"));
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        skinSuggestions.add(p.getName());
                    }
                    return filter(skinSuggestions, args[1]);
                case "aura":
                case "particle":
                    return filter(Arrays.asList("NONE", "FLAME", "SOUL", "ENCHANT", "CHERRY", "END", "HEART", "TOTEM"), args[1]);
                case "scale":
                case "size":
                    return filter(Arrays.asList("0.7", "1.0", "1.3", "1.6", "2.0"), args[1]);
                case "archetype":
                case "type":
                    return filter(Arrays.asList("PLAYER", "VILLAGER", "ALLAY", "IRON_GOLEM", "WARDEN", "ARMOR_STAND"), args[1]);
                case "stance":
                case "pose":
                    return filter(Arrays.asList("standing", "guard", "attack", "defend", "salute", "pointing", "cheering", "sitting", "zombie"), args[1]);
                case "gear":
                case "equip":
                    return filter(Arrays.asList("hand", "offhand", "helmet", "chest", "legs", "boots", "mirror", "wipe"), args[1]);
                case "banner":
                case "holo":
                case "hologram":
                    return filter(Arrays.asList("gui", "list", "set", "edit", "insert", "add", "del", "clear"), args[1]);
                case "action":
                case "cmd":
                    return filter(Arrays.asList("add", "del", "list", "clear"), args[1]);
                case "target":
                case "select":
                case "erase":
                case "remove":
                case "warp":
                case "summon":
                case "studio":
                    return filter(plugin.getNpcManager().getAllNPCs().keySet().stream().map(String::valueOf).collect(Collectors.toList()), args[1]);
            }
        }

        if (args.length == 3 && (sub.equals("banner") || sub.equals("holo") || sub.equals("hologram"))) {
            String act = args[1].toLowerCase();
            if (act.equals("set") || act.equals("edit") || act.equals("line") || act.equals("del") || act.equals("remove") || act.equals("insert")) {
                if (sender instanceof Player) {
                    NPCData target = getSelectedOrNearest((Player) sender);
                    if (target != null) {
                        List<String> lineNums = new ArrayList<>();
                        if (act.equals("set") || act.equals("edit")) lineNums.add("0");
                        for (int i = 1; i <= Math.max(1, target.getHologramLines().size()); i++) {
                            lineNums.add(String.valueOf(i));
                        }
                        return filter(lineNums, args[2]);
                    }
                }
                return filter(Arrays.asList("1", "2", "3", "4", "5"), args[2]);
            }
        }

        if (args.length == 4 && (sub.equals("banner") || sub.equals("holo") || sub.equals("hologram"))
                && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("edit")) && sender instanceof Player) {
            NPCData target = getSelectedOrNearest((Player) sender);
            if (target != null) {
                try {
                    int lineNum = Integer.parseInt(args[2]);
                    if (lineNum == 0 && !target.getName().isEmpty()) {
                        return Collections.singletonList(target.getName().replace("§", "&"));
                    } else if (lineNum >= 1 && lineNum <= target.getHologramLines().size()) {
                        return Collections.singletonList(target.getHologramLines().get(lineNum - 1).replace("§", "&"));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }

        if (args.length == 3 && (sub.equals("action") || sub.equals("cmd")) && args[1].equalsIgnoreCase("add")) {
            return filter(Arrays.asList("CONSOLE", "PLAYER", "OP", "SERVER", "MESSAGE", "ACTIONBAR", "TITLE"), args[2]);
        }

        if (args.length == 4 && (sub.equals("action") || sub.equals("cmd")) && args[1].equalsIgnoreCase("add")) {
            return filter(Arrays.asList("ANY", "RIGHT_CLICK", "LEFT_CLICK", "SHIFT_RIGHT_CLICK", "SHIFT_LEFT_CLICK"), args[3]);
        }

        return Collections.emptyList();
    }

    private List<String> filter(List<String> list, String input) {
        String lower = input.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
