package com.imagery.command;

import com.imagery.ImageryPlugin;
import com.imagery.model.ImageMetadata;
import com.imagery.model.PlacedImage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tab completer providing smart suggestions for commands, file names, and placed IDs.
 */
public class ImageryTabCompleter implements TabCompleter {

    private final ImageryPlugin plugin;
    private static final List<String> SUB_COMMANDS = Arrays.asList(
            "gui", "gallery", "album", "list", "place", "resize", "page", "cmd", "tp", "highlight", "delete", "reload", "help"
    );

    public ImageryTabCompleter(ImageryPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], SUB_COMMANDS, completions);
            return completions;
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("place")) {
                List<String> fileNames = new ArrayList<>();
                for (ImageMetadata meta : plugin.getImageFileManager().getAllMetadata()) {
                    fileNames.add(meta.getFileName());
                }
                StringUtil.copyPartialMatches(args[1], fileNames, completions);
                return completions;
            }

            if (sub.equals("album")) {
                completions.add("place");
                return completions;
            }

            if (sub.equals("cmd")) {
                completions.add("add");
                completions.add("remove");
                completions.add("list");
                return completions;
            }

            if (sub.equals("resize") || sub.equals("page") || sub.equals("tp") || sub.equals("teleport") ||
                sub.equals("highlight") || sub.equals("hl") || sub.equals("delete") || sub.equals("remove")) {
                List<String> ids = new ArrayList<>();
                for (PlacedImage img : plugin.getPlacedImageManager().getAllPlacedImages()) {
                    ids.add(img.getId());
                }
                StringUtil.copyPartialMatches(args[1], ids, completions);
                return completions;
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("album") && args[1].equalsIgnoreCase("place")) {
                List<String> albumIds = new ArrayList<>();
                for (com.imagery.model.ImageAlbum a : plugin.getAlbumManager().getAllAlbums()) {
                    albumIds.add(a.getId());
                }
                StringUtil.copyPartialMatches(args[2], albumIds, completions);
                return completions;
            }
            if (sub.equals("cmd")) {
                List<String> ids = new ArrayList<>();
                for (PlacedImage img : plugin.getPlacedImageManager().getAllPlacedImages()) {
                    ids.add(img.getId());
                }
                StringUtil.copyPartialMatches(args[2], ids, completions);
                return completions;
            }
        }

        if (args.length == 4 && args[0].equalsIgnoreCase("cmd") && args[1].equalsIgnoreCase("add")) {
            completions.add("player");
            completions.add("console");
            completions.add("message");
            return completions;
        }

        if (args.length == 3 && (args[0].equalsIgnoreCase("place") || args[0].equalsIgnoreCase("resize"))) {
            completions.add("2");
            completions.add("3");
            completions.add("4");
            completions.add("5");
            return completions;
        }

        if (args.length == 4 && (args[0].equalsIgnoreCase("place") || args[0].equalsIgnoreCase("resize"))) {
            completions.add("2");
            completions.add("3");
            completions.add("4");
            return completions;
        }

        return completions;
    }
}

