package com.antimery.check.chat;

import com.antimery.AntiMery;
import com.antimery.check.Check;
import com.antimery.check.CheckType;
import com.antimery.punishment.PunishmentType;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatFilterCheck extends Check {

    private final List<String> thaiBadWords = new ArrayList<>();
    private final List<String> karaokeBadWords = new ArrayList<>();
    private final List<String> englishBadWords = new ArrayList<>();
    private final List<String> whitelist = new ArrayList<>();
    private final List<CompiledWord> badWordPatterns = new ArrayList<>();

    public ChatFilterCheck(AntiMery plugin) {
        super(plugin, CheckType.CHAT_FILTER, "chat-filter");
        loadWordLists();
    }

    @Override
    public boolean shouldIgnorePlayer(Player player) {
        if (!isEnabled()) return true;

        // When filter-everyone is true (default), NO ONE is ignored (OPs, Admins, Creative are all checked!)
        boolean filterEveryone = plugin.getConfigManager().getCheckBoolean(configPath, "filter-everyone", true);
        if (filterEveryone) {
            return false;
        }

        return super.shouldIgnorePlayer(player);
    }

    public void loadWordLists() {
        thaiBadWords.clear();
        karaokeBadWords.clear();
        englishBadWords.clear();
        whitelist.clear();
        badWordPatterns.clear();

        thaiBadWords.addAll(plugin.getConfigManager().getCheckStringList(configPath, "thai-badwords"));
        karaokeBadWords.addAll(plugin.getConfigManager().getCheckStringList(configPath, "karaoke-badwords"));
        englishBadWords.addAll(plugin.getConfigManager().getCheckStringList(configPath, "english-badwords"));
        whitelist.addAll(plugin.getConfigManager().getCheckStringList(configPath, "whitelist"));

        // Combine all bad words and sort by length descending to match longest phrases first
        Set<String> allWords = new LinkedHashSet<>();
        allWords.addAll(thaiBadWords);
        allWords.addAll(karaokeBadWords);
        allWords.addAll(englishBadWords);

        List<String> sortedWords = new ArrayList<>(allWords);
        sortedWords.sort((a, b) -> Integer.compare(b.length(), a.length()));

        for (String word : sortedWords) {
            String w = word.trim();
            if (w.isEmpty()) continue;
            boolean isShortEnglish = englishBadWords.contains(w) && w.length() <= 3 && w.matches("^[a-zA-Z]+$");
            Pattern p = buildPattern(w, isShortEnglish);
            if (p != null) {
                badWordPatterns.add(new CompiledWord(w, p));
            }
        }
    }

    private Pattern buildPattern(String word, boolean isShortEnglish) {
        StringBuilder regex = new StringBuilder("(?i)");

        if (isShortEnglish) {
            regex.append("(?<=[^a-zA-Z]|^)");
        }

        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (i > 0) {
                regex.append("[\\s._\\-*~]*");
            }
            regex.append(Pattern.quote(String.valueOf(c)));
            regex.append("+");
        }

        if (isShortEnglish) {
            regex.append("(?=[^a-zA-Z]|$)");
        }

        try {
            return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } catch (Exception e) {
            return Pattern.compile(Pattern.quote(word), Pattern.CASE_INSENSITIVE);
        }
    }

    public FilterResult processMessage(Player player, String rawMessage) {
        if (shouldIgnorePlayer(player)) {
            return new FilterResult(false, rawMessage, Collections.emptyList());
        }

        // 1. Identify protected regions from whitelist
        List<int[]> protectedRanges = getProtectedRanges(rawMessage);

        // 2. Find all bad word match ranges that do not overlap with protected ranges
        List<MatchSpan> matches = new ArrayList<>();
        List<String> detectedWords = new ArrayList<>();

        for (CompiledWord cw : badWordPatterns) {
            Matcher m = cw.pattern.matcher(rawMessage);
            while (m.find()) {
                int start = m.start();
                int end = m.end();

                // Check overlap with whitelist
                if (overlaps(start, end, protectedRanges)) {
                    continue;
                }

                // Check overlap with already found matches
                boolean overlapsExisting = false;
                for (MatchSpan existing : matches) {
                    if (start < existing.end && end > existing.start) {
                        overlapsExisting = true;
                        break;
                    }
                }

                if (!overlapsExisting) {
                    matches.add(new MatchSpan(start, end, m.group()));
                    detectedWords.add(cw.word);
                }
            }
        }

        // If no bad words detected, pass through cleanly
        if (matches.isEmpty()) {
            return new FilterResult(false, rawMessage, Collections.emptyList());
        }

        // 3. Handle Violation and Silent Atlas Tracking
        String action = plugin.getConfigManager().getCheckString(configPath, "action", "CENSOR").toUpperCase();
        String replacement = plugin.getConfigManager().getCheckString(configPath, "censor-replacement", "***");
        int vl = plugin.getPunishmentManager().addViolation(player, type);
        int maxVl = getMaxVL();

        String wordSummary = String.join(", ", detectedWords);

        // บันทึกเข้าสู่ Atlas Suspicion System
        if (plugin.getAtlasManager() != null) {
            plugin.getAtlasManager().recordFlag(player, type, 20.0, "พบคำหยาบ: [" + wordSummary + "]");
        }

        // Silent alert (ไม่สแปมแชทแอดมิน)
        plugin.getPunishmentManager().alertStaff(player, type, vl, maxVl, "พบคำหยาบ: [" + wordSummary + "]");

        // หากพิมพ์คำหยาบเยอะแล้ว (ถึงขีดจำกัด) ทำการเตะหรือแบนทันที!
        String onMaxVlAction = plugin.getConfigManager().getCheckString(configPath, "action-on-max-vl", "KICK");
        if (vl >= maxVl) {
            PunishmentType punish = PunishmentType.fromString(onMaxVlAction);
            plugin.getPunishmentManager().executePunishment(player, type, punish, "พิมพ์คำหยาบคายซ้ำเกินกำหนด (" + wordSummary + ")");
        }

        // 4. Determine chat result based on action (Silent Censor)
        if ("BLOCK".equals(action)) {
            // บล็อคข้อความแบบเงียบ
            return new FilterResult(true, rawMessage, detectedWords);
        } else if ("NOTHING".equals(action)) {
            return new FilterResult(false, rawMessage, detectedWords);
        } else {
            // Default "CENSOR": เซ็นเซอร์เฉพาะคำหยาบเป็น *** อย่างเงียบๆ โดยไม่แจ้งเตือนผู้พิมพ์ให้รำคาญ
            matches.sort((a, b) -> Integer.compare(b.start, a.start)); // Sort descending by start index
            StringBuilder sb = new StringBuilder(rawMessage);
            for (MatchSpan span : matches) {
                sb.replace(span.start, span.end, replacement);
            }

            return new FilterResult(false, sb.toString(), detectedWords);
        }
    }

    private List<int[]> getProtectedRanges(String text) {
        List<int[]> ranges = new ArrayList<>();
        for (String white : whitelist) {
            String w = white.trim();
            if (w.isEmpty()) continue;
            Pattern p = Pattern.compile(Pattern.quote(w), Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(text);
            while (m.find()) {
                ranges.add(new int[]{m.start(), m.end()});
            }
        }
        return ranges;
    }

    private boolean overlaps(int start, int end, List<int[]> ranges) {
        for (int[] r : ranges) {
            if (start < r[1] && end > r[0]) {
                return true;
            }
        }
        return false;
    }

    private static class CompiledWord {
        final String word;
        final Pattern pattern;

        CompiledWord(String word, Pattern pattern) {
            this.word = word;
            this.pattern = pattern;
        }
    }

    private static class MatchSpan {
        final int start;
        final int end;
        final String text;

        MatchSpan(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
        }
    }

    public static class FilterResult {
        private final boolean blocked;
        private final String message;
        private final List<String> detectedWords;

        public FilterResult(boolean blocked, String message, List<String> detectedWords) {
            this.blocked = blocked;
            this.message = message;
            this.detectedWords = detectedWords;
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getMessage() {
            return message;
        }

        public List<String> getDetectedWords() {
            return detectedWords;
        }
    }
}
