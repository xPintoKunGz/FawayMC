package com.antimery;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class ChatFilterTest {

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

        return Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    private String censor(String input, List<String> thaiWords, List<String> karaokeWords, List<String> englishWords, List<String> whitelist) {
        List<int[]> protectedRanges = new ArrayList<>();
        for (String white : whitelist) {
            Pattern p = Pattern.compile(Pattern.quote(white), Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(input);
            while (m.find()) {
                protectedRanges.add(new int[]{m.start(), m.end()});
            }
        }

        Set<String> allWords = new LinkedHashSet<>();
        allWords.addAll(thaiWords);
        allWords.addAll(karaokeWords);
        allWords.addAll(englishWords);

        List<String> sortedWords = new ArrayList<>(allWords);
        sortedWords.sort((a, b) -> Integer.compare(b.length(), a.length()));

        class MatchSpan {
            final int start, end;
            MatchSpan(int s, int e) { this.start = s; this.end = e; }
        }

        List<MatchSpan> matches = new ArrayList<>();
        for (String word : sortedWords) {
            boolean isShortEnglish = englishWords.contains(word) && word.length() <= 3 && word.matches("^[a-zA-Z]+$");
            Pattern pattern = buildPattern(word, isShortEnglish);
            Matcher m = pattern.matcher(input);
            while (m.find()) {
                int start = m.start();
                int end = m.end();

                boolean overlap = false;
                for (int[] r : protectedRanges) {
                    if (start < r[1] && end > r[0]) { overlap = true; break; }
                }
                if (overlap) continue;

                for (MatchSpan existing : matches) {
                    if (start < existing.end && end > existing.start) { overlap = true; break; }
                }
                if (!overlap) {
                    matches.add(new MatchSpan(start, end));
                }
            }
        }

        matches.sort((a, b) -> Integer.compare(b.start, a.start));
        StringBuilder sb = new StringBuilder(input);
        for (MatchSpan span : matches) {
            sb.replace(span.start, span.end, "***");
        }
        return sb.toString();
    }

    @Test
    public void testCensoring() {
        List<String> thaiWords = Arrays.asList("ควย", "เหี้ย", "สัส", "เย็ด", "มึง", "กู");
        List<String> karaokeWords = Arrays.asList("kuy", "kuay", "yed", "hee", "sus", "sat", "sud");
        List<String> englishWords = Arrays.asList("fuck", "shit", "ass", "bitch");
        List<String> whitelist = Arrays.asList("grass", "class", "compass");

        assertEquals("***พีค", censor("kuyพีค", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("***พีค", censor("KUYพีค", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("***", censor("kuy", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("***", censor("KUY", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("***พีค", censor("ควยพีค", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("***", censor("ควย", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("*** พีค", censor("k u y พีค", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("*** พีค", censor("ค ว ย พีค", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("ไอ้***พีคมาก", censor("ไอ้ควยพีคมาก", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("ไอ้***พีคมาก", censor("ไอ้kuyพีคมาก", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("Hello *** world", censor("Hello fuck world", thaiWords, karaokeWords, englishWords, whitelist));
        assertEquals("I love grass and class", censor("I love grass and class", thaiWords, karaokeWords, englishWords, whitelist));
    }
}
