package io.sfrei.tracksearch.tracks.metadata;

import lombok.*;

@Getter
@ToString(callSuper = true)
public class SoundCloudTrackFormat extends TrackFormat {

    private final String protocol;

    @Builder
    public SoundCloudTrackFormat(String mimeType, String audioQuality, boolean streamReady, String url,
                                 String protocol) {
        super(mimeType, audioQuality, streamReady, url);
        this.protocol = protocol;
    }

}
