package org.kcm.bloomery.fawaymc.leaderboard.util;

import org.bukkit.ChatColor;

public class FontUtil {
   private static final int[] WIDTHS = new int[256];

   public static int getStringWidth(String text) {
      if (text == null) {
         return 0;
      } else {
         String stripped = ChatColor.stripColor(text);
         int width = 0;

         for(int i = 0; i < stripped.length(); ++i) {
            char c = stripped.charAt(i);
            int charWidth = c < 256 ? WIDTHS[c] : 6;
            width += charWidth + 1;
         }

         return width;
      }
   }

   static {
      for(int i = 0; i < 256; ++i) {
         WIDTHS[i] = 6;
      }

      String thin = "i!.,:;'|";

      for(char c : thin.toCharArray()) {
         WIDTHS[c] = 2;
      }

      String medium = "fkltI[](){}\"<>-_=+~*";

      for(char c : medium.toCharArray()) {
         WIDTHS[c] = 4;
      }

      WIDTHS[32] = 4;
      WIDTHS[64] = 7;
   }
}
