package io.sfrei.tracksearch.tracks.metadata;

import lombok.*;

@Getter
@ToString(callSuper = true)
public class YouTubeTrackFormat extends TrackFormat {

    private final String audioSampleRate;
    private final String sigParam;
    private final String sigValue;

    @Builder
    public YouTubeTrackFormat(String mimeType, FormatType formatType, String audioQuality, boolean streamReady, String url,
                              String audioSampleRate, String sigParam, String sigValue) {
        super(mimeType, formatType, audioQuality, streamReady, url);
        this.audioSampleRate = audioSampleRate;
        this.sigParam = sigParam;
        this.sigValue = sigValue;
    }

}
