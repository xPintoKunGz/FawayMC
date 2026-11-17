package me.editmery;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitPlayer;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.util.translation.TranslationManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import me.editmery.menu.EditMeryMenuListener;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EditMeryPlugin extends WorldEditPlugin {

    private static EditMeryPlugin editMeryInstance;
    private EditMeryManager manager;
    private EditMeryCommand commandExecutor;

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnable() {
        // Clean up any stale player metadata from previous classloader reloads
        cleanStaleMetadata();

        // Initialize FAWE / WorldEdit core & platform adapters
        super.onEnable();

        editMeryInstance = this;
        saveDefaultConfig();

        this.manager = new EditMeryManager(this);
        this.manager.loadConfig();

        this.commandExecutor = new EditMeryCommand(this, manager);

        // Register event listener
        getServer().getPluginManager().registerEvents(new EditMeryListener(this, manager), this);
        getServer().getPluginManager().registerEvents(new EditMeryMenuListener(this, manager), this);

        // Register /editmery command via plugin command if present
        PluginCommand editMeryCmd = getCommand("editmery");
        if (editMeryCmd != null) {
            editMeryCmd.setExecutor(commandExecutor);
            editMeryCmd.setTabCompleter(commandExecutor);
        }

        // Register dynamic aliases fallback directly into CommandMap
        registerCommandMapFallback();

        // Apply custom EditMery prefix and beautified colors to all WorldEdit commands
        applyWorldEditPrefix();

        getLogger().info(ColorUtil.colorize("&d[EditMery] &aPlugin EditMery loaded successfully with WorldGuard support and custom WorldEdit theme!"));
    }

    private void cleanStaleMetadata() {
        try {
            for (Player p : getServer().getOnlinePlayers()) {
                if (p.hasMetadata("WE")) {
                    for (MetadataValue mv : p.getMetadata("WE")) {
                        Object val = mv.value();
                        if (val != null && !(val instanceof BukkitPlayer)) {
                            p.removeMetadata("WE", mv.getOwningPlugin());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public BukkitPlayer wrapPlayer(Player player) {
        if (player == null) return null;
        try {
            if (player.hasMetadata("WE")) {
                for (MetadataValue mv : player.getMetadata("WE")) {
                    Object val = mv.value();
                    if (val != null && !(val instanceof BukkitPlayer)) {
                        player.removeMetadata("WE", mv.getOwningPlugin());
                    }
                }
            }
        } catch (Throwable ignored) {}
        return super.wrapPlayer(player);
    }

    public void applyWorldEditPrefix() {
        String rawPrefix = manager.getWorldEditPrefix();
        if (rawPrefix == null || rawPrefix.isEmpty()) {
            rawPrefix = "&8[&d&lEdit&b&lMery&8]&7 {0}";
        }
        final String prefixPattern = ColorUtil.colorize(rawPrefix);

        // 1. In-memory injection to WorldEdit TranslationManager
        try {
            WorldEdit we = WorldEdit.getInstance();
            if (we != null) {
                TranslationManager tm = we.getTranslationManager();
                if (tm != null) {
                    ThemeTranslations.apply(tm, prefixPattern);
                }
            }
        } catch (Throwable t) {
            getLogger().warning("Could not apply in-memory theme translations: " + t.getMessage());
        }

        // 2. Patch strings.json on disk so restarts or reloads also use EditMery prefix & colors
        patchDiskStringsJson(prefixPattern);
    }

    private void patchDiskStringsJson(String prefixPattern) {
        try {
            File pluginsDir = getDataFolder().getParentFile();
            if (pluginsDir == null || !pluginsDir.exists()) return;

            File[] targets = new File[]{
                    new File(pluginsDir, "FastAsyncWorldEdit/lang/strings.json"),
                    new File(pluginsDir, "EditMery/lang/strings.json")
            };

            for (File f : targets) {
                if (f.exists()) {
                    String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
                    boolean modified = false;

                    if (content.contains("&4&lFAWE") || !content.contains("EditMery")) {
                        content = content.replace("&8(&4&lFAWE&8)&7 {0}", prefixPattern);
                        content = content.replace("&8(&4&lFAWE&8)", prefixPattern.replace("{0}", "").trim());
                        content = content.replace("FAWE", "EditMery");
                        content = content.replace("FastAsyncWorldEdit", "EditMery");
                        modified = true;
                    }

                    // Also inject beautified command strings
                    for (Map.Entry<String, String> entry : ThemeTranslations.getThemeStrings().entrySet()) {
                        String keyQuote = "\"" + entry.getKey() + "\":";
                        if (content.contains(keyQuote)) {
                            String val = entry.getValue().replace("\"", "\\\"");
                            int keyIdx = content.indexOf(keyQuote);
                            int valStart = content.indexOf("\"", keyIdx + keyQuote.length());
                            int valEnd = content.indexOf("\"", valStart + 1);
                            if (valStart != -1 && valEnd != -1) {
                                content = content.substring(0, valStart + 1) + val + content.substring(valEnd);
                                modified = true;
                            }
                        }
                    }

                    if (modified) {
                        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
                        getLogger().info("Successfully patched " + f.getName() + " with EditMery theme colors!");
                    }
                }
            }
        } catch (Throwable t) {
            getLogger().warning("Could not patch strings.json on disk: " + t.getMessage());
        }
    }

    private void registerCommandMapFallback() {
        try {
            Field commandMapField = getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(getServer());

            BukkitCommand dynamicCmd = new BukkitCommand("editmery") {
                @Override
                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    return commandExecutor.onCommand(sender, this, commandLabel, args);
                }

                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                    return commandExecutor.onTabComplete(sender, this, alias, args);
                }
            };
            dynamicCmd.setAliases(Arrays.asList("em", "emery", "editm"));
            dynamicCmd.setDescription("EditMery Mode Toggle & Settings (OP Only)");
            dynamicCmd.setPermission("editmery.use");

            commandMap.register("editmery", dynamicCmd);
        } catch (Throwable ignored) {
            // Already handled via plugin.yml
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (label.equalsIgnoreCase("editmery") || label.equalsIgnoreCase("em") 
                || label.equalsIgnoreCase("emery") || label.equalsIgnoreCase("editm")) {
            return commandExecutor.onCommand(sender, cmd, label, args);
        }

        // For all other WorldEdit commands dispatched directly to this plugin
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!manager.isEnabled(player) && !manager.canBypass(player)) {
                ItemStack mainHand = player.getInventory().getItemInMainHand();
                if (mainHand != null && mainHand.getType() == org.bukkit.Material.WOODEN_AXE) {
                    player.sendMessage(ColorUtil.colorize("&8(&#B388FF✦ &#FF80ABEditMery&8) &#94A3B8ขวาน WorldEdit ปิดอยู่ &8| &#F8FAFCพิมพ์ &#FFD166/em enable &#F8FAFCเพื่อเปิดใช้งาน"));
                } else {
                    if (player.isOp() || player.hasPermission("editmery.use")) {
                        player.sendMessage(ColorUtil.colorize("&8(&#B388FF✦ &#FF80ABEditMery&8) &#EF476F✖ &cโหมด EditMery ปิดอยู่ &8(พิมพ์ &#FFD166/em enable &8เพื่อเปิดใช้งาน)"));
                    } else {
                        player.sendMessage(ColorUtil.colorize("&8(&#B388FF✦ &#FF80ABEditMery&8) &#EF476F✖ &cคำสั่งนี้สงวนสิทธิ์เฉพาะผู้ดูแลเซิร์ฟเวอร์ (OP) เท่านั้น"));
                    }
                }
                return true;
            }
        }

        return super.onCommand(sender, cmd, label, args);
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            manager.cleanup();
        }
        // Clean metadata on disable
        try {
            for (Player p : getServer().getOnlinePlayers()) {
                p.removeMetadata("WE", this);
            }
        } catch (Throwable ignored) {}

        super.onDisable();
    }

    public static EditMeryPlugin getEditMeryInstance() {
        return editMeryInstance;
    }

    public EditMeryManager getManager() {
        return manager;
    }
}
