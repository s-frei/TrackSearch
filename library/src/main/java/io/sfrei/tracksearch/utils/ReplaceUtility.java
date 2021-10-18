package io.sfrei.tracksearch.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ReplaceUtility {

    public String replaceUnnecessary(final String chars) {
        return chars
                .replaceAll("@", " at ")
                .replaceAll("_", " ")
                .replaceAll("\\s(\\[]\\(\\))", "")
                .replaceAll("[^\\p{javaAlphabetic}0-9&()\\[\\]\\-.\\s]", "")
                .replaceAll("\\s?HD", "")
                .replaceAll("(?i)\\(\\s?Official (Music )?Video\\s?\\)", "")
                .replaceAll("(?i)\\(\\s?Official Audio\\s?\\)", "")
                .replaceAll("(?i)\\s?\\(HQ Audio\\s?\\)", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public String replaceNonDigits(final String chars) {
        return chars.replaceAll("[^\\d.]", "");
    }

}
