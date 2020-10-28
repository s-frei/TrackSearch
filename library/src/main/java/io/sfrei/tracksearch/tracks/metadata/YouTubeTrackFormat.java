package io.sfrei.tracksearch.tracks.metadata;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@ToString(callSuper = true)
public class YouTubeTrackFormat extends TrackFormat {

    private final String audioSampleRate;
    private final String sigParam;
    private final String sigValue;

}
