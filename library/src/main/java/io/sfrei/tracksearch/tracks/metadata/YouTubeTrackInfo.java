package io.sfrei.tracksearch.tracks.metadata;

import lombok.Getter;

import java.util.List;

@Getter
public class YouTubeTrackInfo extends TrackInfo<YouTubeTrackFormat> {

    private final String scriptUrl;

    public YouTubeTrackInfo(List<YouTubeTrackFormat> formats, String scriptUrl) {
        super(formats);
        this.scriptUrl = scriptUrl;
    }

}
