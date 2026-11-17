package com.merycraft.meryitem.item;

import com.merycraft.meryitem.effect.ItemEffect;
import com.merycraft.meryitem.util.ColorUtil;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds clean, modern MMOItems-grade item description (lore).
 * Automatically renders Unbreakable, Stats, Abilities, Potion Effects, Particle Auras, and Custom Lore.
 */
public final class LoreBuilder {

    private LoreBuilder() {}

    public static List<String> buildLore(CustomItem item) {
        List<String> lore = new ArrayList<>();
        ItemRarity rarity = item.getRarity();
        ItemType type = item.getType();

        // 1. Optional Header Category & Rarity Tag (Only if explicitly toggled ON)
        if (item.isShowRarity()) {
            String header = String.format("&#94A3B8Type: %s%s %s &#334155• %s%s",
                    rarity.getPrimaryColor(), type.getIcon(), type.getDisplayName(),
                    rarity.getPrimaryColor(), rarity.getDisplayName().toUpperCase());
            lore.add(ColorUtil.color(header));
            lore.add("");
        }

        // 2. Custom RPG Stats block (Attack Damage, Defense, Crit, Life Steal, etc.)
        Map<ItemStat, Double> stats = item.getStats();
        if (!stats.isEmpty()) {
            for (Map.Entry<ItemStat, Double> entry : stats.entrySet()) {
                ItemStat stat = entry.getKey();
                double val = entry.getValue();
                if (val == 0) continue;

                String line = String.format("&#94A3B8%s %s: %s%s",
                        stat.getIcon(),
                        stat.getDisplayName(),
                        stat.getHexColor(),
                        stat.formatValue(val));
                lore.add(ColorUtil.color(line));
            }
            lore.add("");
        }

        // 3. Custom Enchantments block (Max 255)
        Map<Enchantment, Integer> enchants = item.getEnchantments();
        if (!item.isHideEnchants() && !enchants.isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                String enchName = formatName(entry.getKey().getKey().getKey());
                lore.add(ColorUtil.color(String.format("&#C084FC🔮 %s &#F472B6%d", enchName, entry.getValue())));
            }
            lore.add("");
        }

        // 4. Special Abilities & Skills block (MMOItems-style active abilities & passives)
        List<ItemAbility> abilities = item.getAbilities();
        if (!abilities.isEmpty()) {
            for (ItemAbility ability : abilities) {
                if (ability == ItemAbility.RUN_COMMAND) continue; // Keep command syntax hidden from sword lore
                String trig = ability.getTrigger().equalsIgnoreCase("Passive")
                        ? "&#94A3B8[PASSIVE]"
                        : "&#F472B6[" + ability.getTrigger().toUpperCase() + "]";
                lore.add(ColorUtil.color(String.format("%s❖ %s %s", ability.getHexColor(), ability.getDisplayName(), trig)));
                lore.add(ColorUtil.color("  &#94A3B8" + ability.getDescription()));
                if (ability.getCooldownSeconds() > 0) {
                    lore.add(ColorUtil.color("  &#64748B⌛ Cooldown: " + ability.getCooldownSeconds() + "s"));
                }
            }
            lore.add("");
        }

        // 5. Potion Effects (Granted when held or clicked, max level 250, 9999s)
        Map<PotionEffectType, Integer> pots = item.getPotionEffects();
        if (!pots.isEmpty()) {
            String trigLabel = item.getPotionTrigger().equals("HOLD") ? "When Held" : (item.getPotionTrigger() + "-Click");
            for (Map.Entry<PotionEffectType, Integer> entry : pots.entrySet()) {
                String effName = formatName(entry.getKey().getName());
                lore.add(ColorUtil.color(String.format("&#34D399🧪 Effect: &#CBD5E1%s &#34D399%d &#64748B(%s, 9999s)", effName, entry.getValue(), trigLabel)));
            }
            lore.add("");
        }

        // 6. Visual Aura Particle Effect & Motion Pattern
        if (item.getEffect() != null && item.getEffect() != ItemEffect.NONE) {
            String motionName = item.getParticleMotion().getDisplayName();
            lore.add(ColorUtil.color(item.getEffect().getHexColor() + "✨ Aura Effect: &#CBD5E1" + item.getEffect().getDisplayName() + " &#64748B[" + motionName + "]"));
            lore.add("");
        }

        // 7. Special Item Attributes (Unbreakable & Glowing)
        if (item.isUnbreakable()) {
            lore.add(ColorUtil.color("&#38BDF8💎 Unbreakable"));
        }
        if (item.isGlowing() && enchants.isEmpty()) {
            lore.add(ColorUtil.color("&#FACC15🌟 Glowing Glint"));
        }

        // 8. Custom Description / Lore lines (Pure Description with dynamic placeholders: %sec%, %cooldown%, %cd%, %item_name%, %rarity%)
        List<String> customLore = item.getCustomLore();
        if (customLore != null && !customLore.isEmpty()) {
            int mainCd = 0;
            if (!item.getAbilities().isEmpty()) {
                mainCd = item.getAbilities().get(0).getCooldownSeconds();
            }
            for (String rawLine : customLore) {
                String parsedLine = rawLine
                        .replace("%sec%", String.valueOf(mainCd))
                        .replace("%cooldown%", String.valueOf(mainCd))
                        .replace("%cd%", String.valueOf(mainCd) + "s")
                        .replace("%item_name%", item.getDisplayName() != null ? item.getDisplayName() : item.getId())
                        .replace("%rarity%", item.getRarity().getDisplayName());
                lore.add(ColorUtil.color(parsedLine));
            }
        }

        // 9. Required Level (Only if > 0)
        if (item.getRequiredLevel() > 0) {
            lore.add("");
            lore.add(ColorUtil.color("&#FB7185◆ Requires Level: &#F43F5E" + item.getRequiredLevel()));
        }

        // 10. Optional Footer (Only if explicitly toggled ON)
        if (item.isShowRarity()) {
            lore.add("");
            lore.add(ColorUtil.color("&#F472B6✦ &#FDA4AFMeryITEM &#334155• " + rarity.getPrimaryColor() + rarity.getDisplayName()));
        }

        // Clean up trailing empty lines
        while (!lore.isEmpty() && (lore.get(lore.size() - 1) == null || lore.get(lore.size() - 1).trim().isEmpty())) {
            lore.remove(lore.size() - 1);
        }

        return lore;
    }

    private static String formatName(String raw) {
        if (raw == null) return "";
        String[] parts = raw.split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
