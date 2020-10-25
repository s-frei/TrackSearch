package io.sfrei.tracksearch.config;

import lombok.extern.slf4j.Slf4j;

import java.util.TimeZone;

@Slf4j
public class TrackSearchConfig {

    public static final String POSITION_KEY_SUFFIX = "Position";
    public static final String OFFSET_KEY_SUFFIX = "Offset";
    private static final String TIMEZONE = "UTC";
    private static final Integer DEFAULT_PLAYLIST_OFFSET = 20;
    public static final String HEADER_LANGUAGE_ENGLISH = "Accept-Language: en";
    public static final String HEADER_YOUTUBE_CLIENT_NAME = "x-youtube-client-name: 1";
    public static final String HEADER_YOUTUBE_CLIENT_VERSION = "x-youtube-client-version: 2.20201020.05.00";

    public static void setTime() {
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE));
    }

    public static Integer getDefaultPlaylistOffset() {
        return DEFAULT_PLAYLIST_OFFSET;
    }

}
