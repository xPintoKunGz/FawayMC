package com.bedwars.integration;

import com.bedwars.arena.Team;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MeryNPCHook {

    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("MeryNPC");
    }

    public static boolean spawnShopNPC(Location loc, Team team) {
        if (!isAvailable()) return false;
        try {
            String teamName = (team != null) ? " §7(" + team.getColor().getChatColor() + team.getColor().getDisplayName() + "§7)" : "";
            String idName = "Shop_" + (team != null ? team.getColor().name() : "Lobby");

            List<String> lines = new ArrayList<>();
            lines.add("§e§l✦ ITEM SHOP" + teamName + " ✦");
            lines.add("§7[ Right-click to Open ]");

            return createAndSpawnNPC(idName, loc, lines, "bw shop");
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean spawnUpgradeNPC(Location loc, Team team) {
        if (!isAvailable()) return false;
        try {
            String teamName = (team != null) ? " §7(" + team.getColor().getChatColor() + team.getColor().getDisplayName() + "§7)" : "";
            String idName = "Upgrade_" + (team != null ? team.getColor().name() : "Lobby");

            List<String> lines = new ArrayList<>();
            lines.add("§b§l✦ TEAM UPGRADES" + teamName + " ✦");
            lines.add("§7[ Right-click to Open ]");

            return createAndSpawnNPC(idName, loc, lines, "bw upgrade");
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean createAndSpawnNPC(String idName, Location loc, List<String> lines, String command) {
        try {
            Class<?> meryClass = Class.forName("com.mery.npc.MeryNPC");
            Object pluginInstance = meryClass.getMethod("getInstance").invoke(null);
            if (pluginInstance == null) return false;

            Object npcManager = meryClass.getMethod("getNpcManager").invoke(pluginInstance);
            if (npcManager == null) return false;

            // createNPC(String, Location, String)
            Method createMethod = npcManager.getClass().getMethod("createNPC", String.class, Location.class, String.class);
            Object npcData = createMethod.invoke(npcManager, idName, loc, "VILLAGER");
            if (npcData == null) return false;

            // setLookAtPlayer(true)
            try {
                npcData.getClass().getMethod("setLookAtPlayer", boolean.class).invoke(npcData, true);
            } catch (Throwable ignored) {}

            // setHologramLines(List)
            try {
                npcData.getClass().getMethod("setHologramLines", List.class).invoke(npcData, lines);
            } catch (Throwable ignored) {}

            // setCommands(List<NPCCommand>)
            try {
                Class<?> triggerClass = Class.forName("com.mery.npc.model.ClickTrigger");
                Class<?> typeClass = Class.forName("com.mery.npc.model.CommandType");
                Class<?> cmdClass = Class.forName("com.mery.npc.model.NPCCommand");

                Object rightClick = Enum.valueOf((Class<Enum>) triggerClass, "RIGHT_CLICK");
                Object playerType = Enum.valueOf((Class<Enum>) typeClass, "PLAYER");

                Constructor<?> cmdConstructor = cmdClass.getConstructor(String.class, typeClass, triggerClass);
                Object cmdObj = cmdConstructor.newInstance(command, playerType, rightClick);

                npcData.getClass().getMethod("setCommands", List.class).invoke(npcData, Collections.singletonList(cmdObj));
            } catch (Throwable ignored) {}

            // spawnNPC(NPCData)
            Method spawnMethod = npcManager.getClass().getMethod("spawnNPC", npcData.getClass());
            spawnMethod.invoke(npcManager, npcData);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isMeryNPCEntity(Entity entity, String keyword) {
        if (!isAvailable() || entity == null) return false;
        try {
            Class<?> meryClass = Class.forName("com.mery.npc.MeryNPC");
            Object pluginInstance = meryClass.getMethod("getInstance").invoke(null);
            if (pluginInstance == null) return false;

            Object npcManager = meryClass.getMethod("getNpcManager").invoke(pluginInstance);
            if (npcManager == null) return false;

            Method getByEntity = npcManager.getClass().getMethod("getNPCByEntity", Entity.class);
            Object npcData = getByEntity.invoke(npcManager, entity);
            if (npcData != null) {
                if (keyword == null || keyword.isEmpty()) return true;
                Method getName = npcData.getClass().getMethod("getName");
                String name = (String) getName.invoke(npcData);
                return name != null && name.toLowerCase().contains(keyword.toLowerCase());
            }
        } catch (Throwable ignored) {}
        return false;
    }
}
