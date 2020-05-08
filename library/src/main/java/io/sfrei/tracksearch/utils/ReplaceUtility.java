package io.sfrei.tracksearch.utils;

public class ReplaceUtility {

    public static String replaceUnnecessary(String chars) {
        return chars
                .replaceAll("@", " at ")
                .replaceAll("\\s+", " ")
                .replaceAll("\\s(\\[]\\(\\))", "")
                .replaceAll("[^A-Za-z0-9 &()/\\[\\]\\-]", "")
                .replaceAll("\\s?HD", "")
                .replaceAll("(?i)\\(\\s?Official (Music )?Video\\s?\\)", "")
                .replaceAll("(?i)\\(\\s?Official Audio\\s?\\)", "")
                .replaceAll("(?i)\\s?\\(HQ Audio\\s?\\)", "")
                .trim();
    }

}
