package me.editmery;

import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.util.translation.TranslationManager;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class ThemeTranslations {

    private static final Map<String, String> THEME_STRINGS = new LinkedHashMap<>();

    static {
        // Prefix
        THEME_STRINGS.put("prefix", "&8[&d&lEdit&b&lMery&8]&7 {0}");

        // Position and selection messages
        THEME_STRINGS.put("worldedit.selection.cuboid.explain.primary",
                "&bFirst position &7set to &d{0}&7.");
        THEME_STRINGS.put("worldedit.selection.cuboid.explain.primary-area",
                "&bFirst position &7set to &d{0} &8(&e{1}&8)&7.");
        THEME_STRINGS.put("worldedit.selection.cuboid.explain.secondary",
                "&dSecond position &7set to &b{0}&7.");
        THEME_STRINGS.put("worldedit.selection.cuboid.explain.secondary-area",
                "&dSecond position &7set to &b{0} &8(&e{1}&8)&7.");
        THEME_STRINGS.put("worldedit.selection.extend.explain.primary",
                "&bStarted selection &7at &d{0} &8(&e{1}&8)&7.");
        THEME_STRINGS.put("worldedit.selection.extend.explain.secondary",
                "&dExtended selection &7to &b{0} &8(&e{1}&8)&7.");

        // Operation done (//set, etc.)
        THEME_STRINGS.put("worldedit.set.done",
                "&aOperation completed &8(&b&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.set.done.verbose",
                "&aOperation completed &8(&b&l{0}&7 blocks&8)&7.");

        // Replace operations
        THEME_STRINGS.put("worldedit.replace.replaced",
                "&7Replaced &8(&b&l{0}&8) &7blocks.");
        THEME_STRINGS.put("worldedit.replacenear.replaced",
                "&7Replaced &8(&b&l{0}&8) &7blocks.");

        // Undo & Redo
        THEME_STRINGS.put("worldedit.undo.undone",
                "&7Undid &8(&e{0}&8) &7available edits.");
        THEME_STRINGS.put("worldedit.undo.none",
                "&cNothing left to undo.");
        THEME_STRINGS.put("worldedit.redo.redone",
                "&7Redid &8(&a{0}&8) &7available edits.");
        THEME_STRINGS.put("worldedit.redo.none",
                "&cNothing left to redo.");

        // Clipboard (Copy, Cut, Paste, Clear)
        THEME_STRINGS.put("fawe.worldedit.copy.command.copy",
                "&8(&b&l{0}&8) &7blocks copied to clipboard.");
        THEME_STRINGS.put("fawe.worldedit.cut.command.cut.lazy",
                "&8(&b&l{0}&8) &7blocks will be removed on paste.");
        THEME_STRINGS.put("worldedit.paste.pasted",
                "&7Clipboard pasted at &d{0}&7.");
        THEME_STRINGS.put("fawe.worldedit.paste.command.paste",
                "&7Clipboard pasted at &d{0}&7.");
        THEME_STRINGS.put("worldedit.clearclipboard.cleared",
                "&7Clipboard cleared &a✔");

        // Wand & Tools
        THEME_STRINGS.put("worldedit.wand.selwand.info",
                "&8[&bLeft Click&8] &7Select pos #1 &8| [&dRight Click&8] &7Select pos #2");
        THEME_STRINGS.put("worldedit.wand.navwand.info",
                "&8[&bLeft Click&8] &7Jump to location &8| [&dRight Click&8] &7Pass through walls");
        THEME_STRINGS.put("worldedit.tool.selwand.equip",
                "&7Selection wand bound to &b{0}&7.");
        THEME_STRINGS.put("worldedit.tool.navwand.equip",
                "&7Navigation wand bound to &b{0}&7.");
        THEME_STRINGS.put("worldedit.tool.mask.set",
                "&7Brush mask set to &b{0}&7.");
        THEME_STRINGS.put("worldedit.tool.material.set",
                "&7Brush material set to &b{0}&7.");
        THEME_STRINGS.put("worldedit.tool.range.set",
                "&7Brush range set to &e{0}&7.");
        THEME_STRINGS.put("worldedit.tool.size.set",
                "&7Brush size set to &e{0}&7.");
        THEME_STRINGS.put("worldedit.limit.set",
                "&7Block change limit set to &e{0}&7.");

        // Geometry & Shapes
        THEME_STRINGS.put("worldedit.line.changed",
                "&7Created line &8(&a&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.curve.changed",
                "&7Created curve &8(&a&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.walls.changed",
                "&7Created walls &8(&a&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.faces.changed",
                "&7Created faces &8(&a&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.hollow.changed",
                "&7Hollowed &8(&a&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.smooth.changed",
                "&7Terrain smoothed &8(&a&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.move.moved",
                "&7Moved &8(&a&l{0}&8) &7blocks.");
        THEME_STRINGS.put("worldedit.stack.changed",
                "&7Stacked &8(&a&l{0}&8) &7blocks &8(&7Undo with &e//undo&8)&7.");
        THEME_STRINGS.put("worldedit.overlay.overlaid",
                "&7Overlaid &8(&a&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.naturalize.naturalized",
                "&7Naturalized &8(&a&l{0}&8) &7blocks.");
        THEME_STRINGS.put("worldedit.sphere.created",
                "&7Created sphere &8(&a&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.cyl.created",
                "&7Created cylinder &8(&a&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.pyramid.created",
                "&7Created pyramid &8(&a&l{0}&7 blocks&8)&7.");
        THEME_STRINGS.put("worldedit.drain.drained",
                "&7Drained &8(&a&l{0}&8) &7blocks.");
        THEME_STRINGS.put("worldedit.fill.created",
                "&7Filled &8(&a&l{0}&8) &7blocks.");
        THEME_STRINGS.put("worldedit.fillr.created",
                "&7Filled &8(&a&l{0}&8) &7blocks.");
        THEME_STRINGS.put("worldedit.fixlava.fixed",
                "&7Fixed &8(&a&l{0}&8) &7lava blocks.");
        THEME_STRINGS.put("worldedit.fixwater.fixed",
                "&7Fixed &8(&a&l{0}&8) &7water blocks.");
        THEME_STRINGS.put("worldedit.removeabove.removed",
                "&7Removed &8(&a&l{0}&8) &7blocks above.");
        THEME_STRINGS.put("worldedit.removebelow.removed",
                "&7Removed &8(&a&l{0}&8) &7blocks below.");
        THEME_STRINGS.put("worldedit.removenear.removed",
                "&7Removed &8(&a&l{0}&8) &7nearby blocks.");
        THEME_STRINGS.put("worldedit.thaw.removed",
                "&7Thawed &8(&a&l{0}&8) &7snow blocks.");
        THEME_STRINGS.put("worldedit.green.changed",
                "&7Greened &8(&a&l{0}&8) &7blocks.");
        THEME_STRINGS.put("worldedit.center.changed",
                "&7Center set &8(&a&l{0}&7 blocks changed&8)&7.");
        THEME_STRINGS.put("fawe.worldedit.selection.selection.count",
                "&7Counted &8(&b&l{0}&8) &7blocks in selection.");
    }

    public static Map<String, String> getThemeStrings() {
        return THEME_STRINGS;
    }

    public static void apply(TranslationManager tm, String prefix) {
        if (tm == null) return;

        Map<String, String> allFormatted = new HashMap<>(THEME_STRINGS);
        if (prefix != null && !prefix.isEmpty()) {
            allFormatted.put("prefix", prefix);
        }

        Map<String, String> defaultMap = tm.getTranslationMap(tm.getDefaultLocale());
        if (defaultMap != null) {
            defaultMap.putAll(allFormatted);
        }
        Map<String, String> engMap = tm.getTranslationMap(Locale.ENGLISH);
        if (engMap != null) {
            engMap.putAll(allFormatted);
        }

        try {
            Field field = TranslationManager.class.getDeclaredField("translationMap");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<Locale, Map<String, String>> translationMap =
                    (Map<Locale, Map<String, String>>) field.get(tm);
            if (translationMap != null) {
                for (Map<String, String> m : translationMap.values()) {
                    m.putAll(allFormatted);
                }
            }
        } catch (Throwable ignored) {}
    }
}
