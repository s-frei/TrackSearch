package io.sfrei.tracksearch.config;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.TimeZone;

@SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
@Slf4j
@UtilityClass
public class TrackSearchConfig {

    public final String POSITION_KEY_SUFFIX = "Position";
    public final String OFFSET_KEY_SUFFIX = "Offset";
    public final String HEADER_LANGUAGE_ENGLISH = "Accept-Language: en";
    public final String HEADER_YOUTUBE_CLIENT_NAME = "x-youtube-client-name: 1";
    public final String HEADER_YOUTUBE_CLIENT_VERSION = "x-youtube-client-version: 2.20201020.05.00";

    public String TIMEZONE = "UTC";
    public Integer DEFAULT_PLAYLIST_OFFSET = 20;

    public void setTime() {
        TimeZone.setDefault(TimeZone.getTimeZone(TIMEZONE));
    }

    public Integer getPlaylistOffset() {
        return DEFAULT_PLAYLIST_OFFSET;
    }

}
