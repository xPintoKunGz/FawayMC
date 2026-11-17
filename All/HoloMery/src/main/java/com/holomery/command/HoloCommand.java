package com.holomery.command;

import com.holomery.HoloMeryPlugin;
import com.holomery.color.ColorParser;
import com.holomery.gui.HoloEditorGUI;
import com.holomery.gui.HoloListGUI;
import com.holomery.manager.HologramManager;
import com.holomery.manager.ThemeManager;
import com.holomery.model.BillboardMode;
import com.holomery.model.HoloLine;
import com.holomery.model.Hologram;
import com.holomery.util.MessageUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Modern, intuitive and streamlined command handler for HoloMery with interactive GUI menus.
 */
public class HoloCommand implements CommandExecutor, TabCompleter {

    private final HoloMeryPlugin plugin;
    private final HologramManager manager;
    private final ThemeManager themeManager;

    public HoloCommand(HoloMeryPlugin plugin) {
        this.plugin = plugin;
        this.manager = HologramManager.getInstance();
        this.themeManager = ThemeManager.getInstance();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("holomery.admin")) {
            MessageUtil.sendError(sender, "You do not have permission to use HoloMery.");
            return true;
        }

        if (args.length == 0) {
            if (sender instanceof Player player) {
                HoloListGUI.open(player, 0);
            } else {
                sendHelpMenu(sender);
            }
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "gui", "menu" -> handleGui(sender);
            case "edit" -> handleEditGui(sender, args);
            case "create", "spawn" -> handleCreate(sender, args);
            case "add", "addline" -> handleAddLine(sender, args);
            case "set", "setline" -> handleSetLine(sender, args);
            case "insert" -> handleInsertLine(sender, args);
            case "swap" -> handleSwap(sender, args);
            case "move" -> handleMove(sender, args);
            case "spin" -> handleSpin(sender, args);
            case "background", "bg" -> handleBackground(sender, args);
            case "delete", "remove" -> handleDelete(sender, args);
            case "page" -> handlePage(sender, args);
            case "action" -> handleAction(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "tp", "teleport" -> handleTeleport(sender, args);
            case "movehere" -> handleMoveHere(sender, args);
            case "nudge" -> handleNudge(sender, args);
            case "style" -> handleStyle(sender, args);
            case "theme" -> handleTheme(sender, args);
            case "themes" -> handleThemesList(sender);
            case "preview" -> handlePreview(sender, args);
            case "reload" -> handleReload(sender);
            case "help" -> sendHelpMenu(sender);
            default -> MessageUtil.sendError(sender, "Unknown subcommand. Type &e/hm help &ffor a list of commands.");
        }

        return true;
    }

    private void handleGui(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "GUI menu can only be opened by players in-game.");
            return;
        }
        HoloListGUI.open(player, 0);
    }

    private void handleEditGui(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "GUI editor can only be opened by players in-game.");
            return;
        }

        if (args.length < 2) {
            HoloListGUI.open(player, 0);
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        HoloEditorGUI.open(player, holo);
    }

    private void handlePage(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: &e/hm page <id> <next|prev|add|remove|#>");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        String action = args[2].toLowerCase();
        Player p = (sender instanceof Player player) ? player : null;

        switch (action) {
            case "next" -> {
                holo.nextPage(p);
                MessageUtil.sendSuccess(sender, "Switched to page &b" + (holo.getActivePageIndex() + 1) + "&7/&b" + holo.getPageCount());
            }
            case "prev", "previous" -> {
                holo.prevPage(p);
                MessageUtil.sendSuccess(sender, "Switched to page &b" + (holo.getActivePageIndex() + 1) + "&7/&b" + holo.getPageCount());
            }
            case "add" -> {
                holo.createNewPage();
                holo.setPage(holo.getPageCount() - 1);
                holo.addLine("<cyberpunk>✦ Page " + holo.getPageCount() + " ✦</cyberpunk>");
                manager.saveHolograms();
                MessageUtil.sendSuccess(sender, "Created and switched to new Page &b" + holo.getPageCount());
            }
            case "remove", "delete" -> {
                if (holo.removePage(holo.getActivePageIndex())) {
                    manager.saveHolograms();
                    MessageUtil.sendSuccess(sender, "Removed current page.");
                } else {
                    MessageUtil.sendError(sender, "Cannot delete the only remaining page.");
                }
            }
            default -> {
                try {
                    int pageNum = Integer.parseInt(action);
                    int idx = pageNum - 1;
                    if (idx >= 0 && idx < holo.getPageCount()) {
                        holo.setPage(idx);
                        MessageUtil.sendSuccess(sender, "Switched to page &b" + pageNum);
                    } else {
                        MessageUtil.sendError(sender, "Invalid page number. Hologram has &e" + holo.getPageCount() + " &cpages.");
                    }
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(sender, "Invalid action! Use next, prev, add, remove, or a page number.");
                }
            }
        }
    }

    private void handleAction(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: &e/hm action <id> <add|remove|list|clear> [args...]");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        String sub = args[2].toLowerCase();
        switch (sub) {
            case "add" -> {
                if (args.length < 4) {
                    MessageUtil.sendError(sender, "Usage: &e/hm action <id> add <PAGE:N|[player] cmd|[console] cmd|[message] text|[sound] sound>");
                    return;
                }
                String action = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                holo.addClickAction(action);
                manager.saveHolograms();
                MessageUtil.sendSuccess(sender, "Added click action to Page " + (holo.getActivePageIndex() + 1) + ": &b" + action);
            }
            case "remove" -> {
                if (args.length < 4) {
                    MessageUtil.sendError(sender, "Usage: &e/hm action <id> remove <index>");
                    return;
                }
                try {
                    int idx = Integer.parseInt(args[3]) - 1;
                    if (holo.removeClickAction(idx)) {
                        manager.saveHolograms();
                        MessageUtil.sendSuccess(sender, "Removed click action #" + (idx + 1) + " from Page " + (holo.getActivePageIndex() + 1) + ".");
                    } else {
                        MessageUtil.sendError(sender, "Invalid action index.");
                    }
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(sender, "Action index must be an integer.");
                }
            }
            case "list" -> {
                java.util.List<String> actions = holo.getClickActions();
                MessageUtil.sendRaw(sender, "\n<gradient:#FF0080:#FA709A>&l✦ Click Actions for &b" + holo.getId() + " &7[Page " + (holo.getActivePageIndex() + 1) + "] ✦</gradient>");
                if (actions.isEmpty()) {
                    MessageUtil.sendRaw(sender, "&8  (None - flips to next page by default)");
                } else {
                    for (int i = 0; i < actions.size(); i++) {
                        MessageUtil.sendRaw(sender, " &8#" + (i + 1) + " &b" + actions.get(i));
                    }
                }
                MessageUtil.sendRaw(sender, "");
            }
            case "clear" -> {
                holo.clearClickActions();
                manager.saveHolograms();
                MessageUtil.sendSuccess(sender, "Cleared all click actions on Page " + (holo.getActivePageIndex() + 1) + ".");
            }
            default -> MessageUtil.sendError(sender, "Unknown action command. Use: add, remove, list, or clear.");
        }
    }

    private void handleCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "Only in-game players can create holograms at their location.");
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: &e/hm create <id> [text...]");
            return;
        }

        String id = args[1].toLowerCase();
        if (manager.exists(id)) {
            MessageUtil.sendError(sender, "A hologram with ID '&e" + id + "&c' already exists!");
            return;
        }

        Location loc = player.getLocation().add(0, 1.2, 0);
        Hologram holo = manager.createHologram(id, loc);

        if (args.length > 2) {
            String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            holo.addLine(text);
        } else {
            holo.addLine("<cyberpunk>✦ &l" + id.toUpperCase() + " ✦</cyberpunk>");
        }

        manager.saveHolograms();
        MessageUtil.sendSuccess(sender, "Hologram '&b" + id + "&a' created successfully!");
    }

    private void handleAddLine(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: &e/hm add <id> <text|ITEM:NAME|BLOCK:NAME|MOB:NAME>");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        holo.addLine(text);
        manager.saveHolograms();

        MessageUtil.sendSuccess(sender, "Added line to '&b" + id + "&a' [Page " + (holo.getActivePageIndex() + 1) + "]: " + ColorParser.colorize(text));
    }

    private void handleSetLine(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtil.sendError(sender, "Usage: &e/hm set <id> <line#> <text...>");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        int lineNum;
        try {
            lineNum = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Line number must be an integer (1, 2, 3...)");
            return;
        }

        int index = lineNum - 1;
        if (index < 0 || index >= holo.getLineCount()) {
            MessageUtil.sendError(sender, "Invalid line number! Page has &e" + holo.getLineCount() + " &clines.");
            return;
        }

        String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        holo.setLine(index, text);
        manager.saveHolograms();

        MessageUtil.sendSuccess(sender, "Line " + lineNum + " updated on '&b" + id + "&a': " + ColorParser.colorize(text));
    }

    private void handleInsertLine(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtil.sendError(sender, "Usage: &e/hm insert <id> <line#> <text...>");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        int lineNum;
        try {
            lineNum = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Line number must be an integer (1, 2, 3...)");
            return;
        }

        int index = Math.max(0, lineNum - 1);
        String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        holo.insertLine(index, text);
        manager.saveHolograms();

        MessageUtil.sendSuccess(sender, "Inserted new line at #" + (index + 1) + " on '&b" + id + "&a'.");
    }

    private void handleSwap(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtil.sendError(sender, "Usage: &e/hm swap <id> <line1> <line2>");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        try {
            int l1 = Integer.parseInt(args[2]) - 1;
            int l2 = Integer.parseInt(args[3]) - 1;
            if (holo.swapLines(l1, l2)) {
                manager.saveHolograms();
                MessageUtil.sendSuccess(sender, "Swapped line " + (l1 + 1) + " and line " + (l2 + 1) + " on '&b" + id + "&a'!");
            } else {
                MessageUtil.sendError(sender, "Invalid line numbers! Hologram has &e" + holo.getLineCount() + " &clines.");
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Line numbers must be integers (1, 2, 3...)");
        }
    }

    private void handleMove(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtil.sendError(sender, "Usage: &e/hm move <id> <fromLine> <toLine>");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        try {
            int from = Integer.parseInt(args[2]) - 1;
            int to = Integer.parseInt(args[3]) - 1;
            if (holo.moveLine(from, to)) {
                manager.saveHolograms();
                MessageUtil.sendSuccess(sender, "Moved line from position " + (from + 1) + " to " + (to + 1) + " on '&b" + id + "&a'!");
            } else {
                MessageUtil.sendError(sender, "Invalid line numbers! Hologram has &e" + holo.getLineCount() + " &clines.");
            }
        } catch (NumberFormatException e) {
            MessageUtil.sendError(sender, "Line numbers must be integers (1, 2, 3...)");
        }
    }

    private void handleSpin(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: &e/hm spin <id> [true|false]");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        boolean spinState = (args.length >= 3) ? Boolean.parseBoolean(args[2]) : !holo.isSpin();
        holo.setSpin(spinState);
        manager.saveHolograms();

        MessageUtil.sendSuccess(sender, "360° Spin Animation for '&b" + id + "&a' is now &b" + (spinState ? "ENABLED (หมุนรอบ)" : "DISABLED (ตามผู้เล่น)") + "&a.");
    }

    private void handleBackground(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: &e/hm bg <id> [on|off|toggle]");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        if (args.length >= 3) {
            String val = args[2].toLowerCase();
            if (val.equals("on") || val.equals("true") || val.equals("black") || val.equals("default")) {
                holo.setTransparentBackground(false);
            } else if (val.equals("off") || val.equals("false") || val.equals("transparent") || val.equals("none")) {
                holo.setTransparentBackground(true);
            } else {
                holo.toggleBackground();
            }
        } else {
            holo.toggleBackground();
        }

        manager.saveHolograms();
        boolean hasBlackBox = !holo.isTransparentBackground();
        MessageUtil.sendSuccess(sender, "พื้นหลังสีดำของโฮโลแกรม '&b" + id + "&a': " + (hasBlackBox ? "&eเปิด (ON - มีกล่องดำ)" : "&bปิด (OFF - โปร่งใส ไร้กล่องดำ)"));
    }

    private void handleDelete(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: &e/hm delete <id> [line#]");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        if (args.length >= 3) {
            try {
                int lineNum = Integer.parseInt(args[2]);
                int index = lineNum - 1;
                if (holo.removeLine(index)) {
                    manager.saveHolograms();
                    MessageUtil.sendSuccess(sender, "Removed line " + lineNum + " from '&b" + id + "&a'.");
                } else {
                    MessageUtil.sendError(sender, "Invalid line number! Page has &e" + holo.getLineCount() + " &clines.");
                }
            } catch (NumberFormatException e) {
                MessageUtil.sendError(sender, "Line number must be an integer.");
            }
            return;
        }

        if (manager.deleteHologram(id)) {
            MessageUtil.sendSuccess(sender, "Hologram '&b" + id + "&a' deleted permanently.");
        } else {
            MessageUtil.sendError(sender, "Failed to delete hologram '&e" + id + "&c'.");
        }
    }

    private void handleList(CommandSender sender) {
        Collection<Hologram> holos = manager.getAllHolograms();
        if (holos.isEmpty()) {
            MessageUtil.sendInfo(sender, "No holograms have been created yet. Create one with &e/hm create <id>");
            return;
        }

        MessageUtil.sendRaw(sender, "\n<gradient:#00F2FE:#4FACFE:#FF0080>&l✦ Active Holograms (" + holos.size() + ") ✦</gradient>");
        for (Hologram holo : holos) {
            Location loc = holo.getLocation();
            String world = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";
            if (sender instanceof Player player) {
                MessageUtil.sendHoloListEntry(player, holo.getId(), holo.getLineCount(), world, loc.getX(), loc.getY(), loc.getZ());
            } else {
                sender.sendMessage(ColorParser.colorize("&8 ▪ &b" + holo.getId() + " &7(&f" + holo.getPageCount() + " pages&7) at &e" + world));
            }
        }
        MessageUtil.sendRaw(sender, "");
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: &e/hm info <id>");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        Location loc = holo.getLocation();
        String worldName = loc.getWorld() != null ? loc.getWorld().getName() : "unknown";

        MessageUtil.sendRaw(sender, "\n<gradient:#00F2FE:#4FACFE:#FF0080>&l✦ Hologram Details: &b" + holo.getId() + " &l✦</gradient>");
        MessageUtil.sendRaw(sender, "&7Location: &f" + worldName + " &8[" + String.format("%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ()) + "&8]");
        MessageUtil.sendRaw(sender, "&7Pages: &b" + (holo.getActivePageIndex() + 1) + "/" + holo.getPageCount() + " &8| &7Click-Flip: &b" + holo.isClickPageSwitch());
        MessageUtil.sendRaw(sender, "&7Billboard: &b" + holo.getBillboard().name() + " &8| &7Scale: &b" + holo.getScale() + " &8| &7Spacing: &b" + holo.getLineSpacing());
        MessageUtil.sendRaw(sender, "&7Shadow: &b" + holo.isShadow() + " &8| &7See-Through: &b" + holo.isSeeThrough() + " &8| &7Transparent: &b" + holo.isTransparentBackground());
        MessageUtil.sendRaw(sender, "&7Lines on Page " + (holo.getActivePageIndex() + 1) + " (&f" + holo.getLineCount() + "&7):");

        for (int i = 0; i < holo.getLineCount(); i++) {
            HoloLine line = holo.getLine(i);
            MessageUtil.sendRaw(sender, " &8#" + (i + 1) + " &7[" + line.getType().name() + "] &r" + ColorParser.colorize(line.getRawText()));
        }
        MessageUtil.sendRaw(sender, "");
    }

    private void handleTeleport(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "Only players can teleport to holograms.");
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: &e/hm tp <id>");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        player.teleport(holo.getLocation());
        MessageUtil.sendSuccess(sender, "Teleported to hologram '&b" + id + "&a'!");
    }

    private void handleMoveHere(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtil.sendError(sender, "Only players can move holograms to their location.");
            return;
        }

        if (args.length < 2) {
            MessageUtil.sendError(sender, "Usage: &e/hm movehere <id>");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        Location loc = player.getLocation().add(0, 1.2, 0);
        holo.setLocation(loc);
        manager.saveHolograms();

        MessageUtil.sendSuccess(sender, "Moved hologram '&b" + id + "&a' to your current position!");
    }

    private void handleNudge(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: &e/hm nudge <id> <up|down|north|south|east|west> [amount]");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        String dir = args[2].toLowerCase();
        double amount = 0.25;
        if (args.length >= 4) {
            try {
                amount = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                MessageUtil.sendError(sender, "Amount must be a decimal number (e.g. 0.1, 0.5)");
                return;
            }
        }

        double dx = 0, dy = 0, dz = 0;
        switch (dir) {
            case "up" -> dy = amount;
            case "down" -> dy = -amount;
            case "north" -> dz = -amount;
            case "south" -> dz = amount;
            case "east" -> dx = amount;
            case "west" -> dx = -amount;
            default -> {
                MessageUtil.sendError(sender, "Invalid direction! Use up, down, north, south, east, or west.");
                return;
            }
        }

        holo.nudge(dx, dy, dz);
        manager.saveHolograms();
        MessageUtil.sendSuccess(sender, "Nudged '&b" + id + "&a' " + dir + " by " + amount + " blocks.");
    }

    private void handleStyle(CommandSender sender, String[] args) {
        if (args.length < 4) {
            MessageUtil.sendError(sender, "Usage: &e/hm style <id> <property> <value>");
            MessageUtil.sendInfo(sender, "Properties: &bbillboard&7, &bscale&7, &bshadow&7, &bseethrough&7, &bbackground&7, &balignment&7, &bspacing");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        String prop = args[2].toLowerCase();
        String val = args[3];

        switch (prop) {
            case "billboard" -> {
                BillboardMode mode = BillboardMode.fromString(val);
                holo.setBillboard(mode);
                MessageUtil.sendSuccess(sender, "Set billboard mode to &b" + mode.name() + "&a for '&b" + id + "&a'.");
            }
            case "scale" -> {
                try {
                    float scale = Float.parseFloat(val);
                    holo.setScale(scale);
                    MessageUtil.sendSuccess(sender, "Set scale to &b" + scale + "&a for '&b" + id + "&a'.");
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(sender, "Scale must be a number (e.g. 1.0, 1.5, 2.0).");
                }
            }
            case "shadow" -> {
                boolean shadow = Boolean.parseBoolean(val);
                holo.setShadow(shadow);
                MessageUtil.sendSuccess(sender, "Set shadow to &b" + shadow + "&a for '&b" + id + "&a'.");
            }
            case "seethrough", "see-through" -> {
                boolean seeThrough = Boolean.parseBoolean(val);
                holo.setSeeThrough(seeThrough);
                MessageUtil.sendSuccess(sender, "Set see-through to &b" + seeThrough + "&a for '&b" + id + "&a'.");
            }
            case "background" -> {
                if (val.equalsIgnoreCase("transparent") || val.equalsIgnoreCase("none")) {
                    holo.setTransparentBackground(true);
                    holo.setBackgroundColor(null);
                    MessageUtil.sendSuccess(sender, "Set background to &btransparent&a for '&b" + id + "&a'.");
                } else if (val.equalsIgnoreCase("default")) {
                    holo.setTransparentBackground(false);
                    holo.setBackgroundColor(null);
                    MessageUtil.sendSuccess(sender, "Set background to &bdefault&a for '&b" + id + "&a'.");
                } else {
                    try {
                        String clean = val.replace("#", "");
                        if (clean.length() == 6) {
                            int rgb = Integer.parseInt(clean, 16);
                            holo.setBackgroundColor(Color.fromRGB(rgb));
                        } else if (clean.length() == 8) {
                            int argb = (int) Long.parseLong(clean, 16);
                            holo.setBackgroundColor(Color.fromARGB(argb));
                        }
                        MessageUtil.sendSuccess(sender, "Set background color to &b#" + clean + "&a for '&b" + id + "&a'.");
                    } catch (Exception e) {
                        MessageUtil.sendError(sender, "Invalid color! Use 'transparent', 'default', or hex '#RRGGBB'.");
                    }
                }
            }
            case "alignment" -> {
                try {
                    TextDisplay.TextAlignment align = TextDisplay.TextAlignment.valueOf(val.toUpperCase());
                    holo.setAlignment(align);
                    MessageUtil.sendSuccess(sender, "Set alignment to &b" + align.name() + "&a for '&b" + id + "&a'.");
                } catch (Exception e) {
                    MessageUtil.sendError(sender, "Invalid alignment! Use CENTER, LEFT, or RIGHT.");
                }
            }
            case "spacing" -> {
                try {
                    double spacing = Double.parseDouble(val);
                    holo.setLineSpacing(spacing);
                    MessageUtil.sendSuccess(sender, "Set line spacing to &b" + spacing + "&a for '&b" + id + "&a'.");
                } catch (NumberFormatException e) {
                    MessageUtil.sendError(sender, "Spacing must be a number (e.g. 0.25, 0.30).");
                }
            }
            case "spin" -> {
                boolean spin = Boolean.parseBoolean(val);
                holo.setSpin(spin);
                MessageUtil.sendSuccess(sender, "Set spin animation to &b" + spin + "&a for '&b" + id + "&a'.");
            }
            default -> {
                MessageUtil.sendError(sender, "Unknown property: &e" + prop);
                return;
            }
        }

        manager.saveHolograms();
    }

    private void handleTheme(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: &e/hm theme <id> <theme_name>");
            return;
        }

        String id = args[1].toLowerCase();
        Hologram holo = manager.getHologram(id);
        if (holo == null) {
            MessageUtil.sendError(sender, "Hologram '&e" + id + "&c' not found.");
            return;
        }

        String themeName = args[2].toLowerCase();
        if (!themeManager.hasTheme(themeName)) {
            MessageUtil.sendError(sender, "Theme '&e" + themeName + "&c' does not exist! Type &e/hm themes &cto see all.");
            return;
        }

        for (int i = 0; i < holo.getLineCount(); i++) {
            HoloLine line = holo.getLine(i);
            String raw = line.getRawText();
            raw = raw.replaceAll("<[a-zA-Z0-9_]+>", "").replaceAll("</[a-zA-Z0-9_]+>", "");
            holo.setLine(i, "<" + themeName + ">" + raw + "</" + themeName + ">");
        }

        manager.saveHolograms();
        MessageUtil.sendSuccess(sender, "Applied theme '&b" + themeName + "&a' to current page of '&b" + id + "&a'!");
    }

    private void handleThemesList(CommandSender sender) {
        MessageUtil.sendRaw(sender, "\n<gradient:#00F2FE:#4FACFE:#FF0080>&l✦ HoloMery Color Themes ✦</gradient>");
        MessageUtil.sendRaw(sender, "&7Use tags like &f<themename>Your Text</themename> &7in any line!\n");

        for (String themeName : themeManager.getThemeNames()) {
            String sample = ColorParser.colorize("<" + themeName + ">████████ " + themeName.toUpperCase() + " PALETTE ████████</" + themeName + ">");
            MessageUtil.sendRaw(sender, " &8▪ &f" + themeName + ": " + sample);
        }

        MessageUtil.sendRaw(sender, "\n&7Tip: Create your own custom themes in &eplugins/HoloMery/themes.yml&7!\n");
    }

    private void handlePreview(CommandSender sender, String[] args) {
        if (args.length < 3) {
            MessageUtil.sendError(sender, "Usage: &e/hm preview <theme> <text...>");
            return;
        }

        String theme = args[1].toLowerCase();
        String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String formatted = ColorParser.colorize("<" + theme + ">" + text + "</" + theme + ">");

        MessageUtil.sendRaw(sender, "&8[&bPreview &7(" + theme + ")&8] &r" + formatted);
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        themeManager.loadThemes();
        manager.loadHolograms();
        MessageUtil.sendSuccess(sender, "Configuration, themes, and holograms reloaded successfully!");
    }

    private void sendHelpMenu(CommandSender sender) {
        MessageUtil.sendRaw(sender, "\n<gradient:#00F2FE:#4FACFE:#FF0080>&l✦ HoloMery v" + plugin.getDescription().getVersion() + " - Command Guide ✦</gradient>");
        MessageUtil.sendRaw(sender, "&b/hm gui &8- &eOpen Interactive Holograms Browser GUI");
        MessageUtil.sendRaw(sender, "&b/hm edit <id> &8- &eOpen In-Game Visual Hologram Editor");
        MessageUtil.sendRaw(sender, "&b/hm create <id> [text...] &8- &7Create hologram at eye level");
        MessageUtil.sendRaw(sender, "&b/hm page <id> <next|prev|add|remove|#> &8- &7Multi-page controls");
        MessageUtil.sendRaw(sender, "&b/hm add <id> <text|ITEM:x|BLOCK:x|MOB:x> &8- &7Add a new line");
        MessageUtil.sendRaw(sender, "&b/hm set <id> <line#> <text...> &8- &7Edit existing line (1-based)");
        MessageUtil.sendRaw(sender, "&b/hm delete <id> [line#] &8- &7Delete line or whole hologram");
        MessageUtil.sendRaw(sender, "&b/hm list &8- &7Interactive list with [TP], [INFO], [DEL]");
        MessageUtil.sendRaw(sender, "&b/hm info <id> &8- &7Inspect detailed settings & lines");
        MessageUtil.sendRaw(sender, "&b/hm tp <id> &8- &7Teleport to hologram");
        MessageUtil.sendRaw(sender, "&b/hm movehere <id> &8- &7Move hologram to your location");
        MessageUtil.sendRaw(sender, "&b/hm nudge <id> <dir> [amt] &8- &7Fine-tune position");
        MessageUtil.sendRaw(sender, "&b/hm bg <id> [on|off] &8- &eเปิด/ปิดพื้นหลังสีดำของตัวหนังสือ");
        MessageUtil.sendRaw(sender, "&b/hm spin <id> [true|false] &8- &eเปิด/ปิดหมุนรอบ 360 องศา");
        MessageUtil.sendRaw(sender, "&b/hm style <id> <prop> <val> &8- &7Customise scale/shadow/billboard/bg");
        MessageUtil.sendRaw(sender, "&b/hm theme <id> <theme> &8- &7Apply vibrant gradient palette");
        MessageUtil.sendRaw(sender, "&b/hm themes &8- &7Show all gradient palettes with live sample");
        MessageUtil.sendRaw(sender, "&b/hm reload &8- &7Reload config, themes & holograms");
        MessageUtil.sendRaw(sender, "<gradient:#00F2FE:#FF0080>&m--------------------------------------------------</gradient>\n");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("holomery.admin")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> subs = List.of(
                    "gui", "edit", "create", "page", "action", "add", "set", "insert", "swap", "move", "bg", "background", "spin", "delete", "list", "info",
                    "tp", "movehere", "nudge", "style", "theme", "themes", "preview", "reload", "help"
            );
            return filterPrefix(subs, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (List.of("edit", "page", "action", "add", "set", "insert", "swap", "move", "bg", "background", "spin", "delete", "info", "tp", "movehere", "nudge", "style", "theme").contains(sub)) {
                return filterPrefix(manager.getAllHolograms().stream().map(Hologram::getId).collect(Collectors.toList()), args[1]);
            }
            if (sub.equals("preview")) {
                return filterPrefix(new ArrayList<>(themeManager.getThemeNames()), args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            String id = args[1].toLowerCase();
            Hologram holo = manager.getHologram(id);

            if (sub.equals("bg") || sub.equals("background")) {
                return filterPrefix(List.of("on", "off", "toggle"), args[2]);
            } else if (sub.equals("action")) {
                return filterPrefix(List.of("add", "remove", "list", "clear"), args[2]);
            } else if (sub.equals("page")) {
                List<String> actions = new ArrayList<>(List.of("next", "prev", "add", "remove"));
                if (holo != null) {
                    for (int i = 1; i <= holo.getPageCount(); i++) actions.add(String.valueOf(i));
                }
                return filterPrefix(actions, args[2]);
            } else if (sub.equals("set") || sub.equals("insert") || sub.equals("delete") || sub.equals("swap") || sub.equals("move")) {
                if (holo != null) {
                    List<String> indices = new ArrayList<>();
                    for (int i = 1; i <= holo.getLineCount(); i++) {
                        indices.add(String.valueOf(i));
                    }
                    return filterPrefix(indices, args[2]);
                }
            } else if (sub.equals("spin")) {
                return filterPrefix(List.of("true", "false"), args[2]);
            } else if (sub.equals("nudge")) {
                return filterPrefix(List.of("up", "down", "north", "south", "east", "west"), args[2]);
            } else if (sub.equals("style")) {
                return filterPrefix(List.of("billboard", "scale", "shadow", "seethrough", "background", "alignment", "spacing", "spin"), args[2]);
            } else if (sub.equals("theme")) {
                return filterPrefix(new ArrayList<>(themeManager.getThemeNames()), args[2]);
            } else if (sub.equals("add")) {
                return filterPrefix(List.of("ITEM:DIAMOND_SWORD", "ITEM:NETHER_STAR", "BLOCK:BEACON", "BLOCK:NETHERITE_BLOCK", "MOB:ALLAY", "MOB:WARDEN"), args[2]);
            }
        }

        if (args.length == 4) {
            String sub = args[0].toLowerCase();
            String id = args[1].toLowerCase();
            Hologram holo = manager.getHologram(id);

            if (sub.equals("action")) {
                if (args[2].equalsIgnoreCase("add")) {
                    return filterPrefix(List.of("PAGE:1", "PAGE:2", "PAGE:NEXT", "[player] spawn", "[console] give %player_name% diamond 1", "[sound] ENTITY_EXPERIENCE_ORB_PICKUP", "[message] <cyberpunk>Hello!</cyberpunk>"), args[3]);
                }
            }

            if (sub.equals("swap") || sub.equals("move")) {
                if (holo != null) {
                    List<String> indices = new ArrayList<>();
                    for (int i = 1; i <= holo.getLineCount(); i++) {
                        indices.add(String.valueOf(i));
                    }
                    return filterPrefix(indices, args[3]);
                }
            }

            String prop = args[2].toLowerCase();
            if (sub.equals("style")) {
                switch (prop) {
                    case "billboard" -> {
                        return filterPrefix(List.of("CENTER", "FIXED", "VERTICAL", "HORIZONTAL"), args[3]);
                    }
                    case "shadow", "seethrough", "spin" -> {
                        return filterPrefix(List.of("true", "false"), args[3]);
                    }
                    case "background" -> {
                        return filterPrefix(List.of("transparent", "default", "#000000", "#80000000"), args[3]);
                    }
                    case "alignment" -> {
                        return filterPrefix(List.of("CENTER", "LEFT", "RIGHT"), args[3]);
                    }
                    case "scale" -> {
                        return filterPrefix(List.of("0.8", "1.0", "1.2", "1.5", "2.0"), args[3]);
                    }
                    case "spacing" -> {
                        return filterPrefix(List.of("0.2", "0.25", "0.3", "0.35", "0.4"), args[3]);
                    }
                }
            } else if (sub.equals("nudge")) {
                return filterPrefix(List.of("0.1", "0.25", "0.5", "1.0"), args[3]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> filterPrefix(List<String> list, String prefix) {
        String lower = prefix.toLowerCase();
        return list.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
