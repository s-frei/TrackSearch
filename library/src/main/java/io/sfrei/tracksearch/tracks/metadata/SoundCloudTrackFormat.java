package io.sfrei.tracksearch.tracks.metadata;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@ToString(callSuper = true)
@SuperBuilder
public class SoundCloudTrackFormat extends TrackFormat {

    private final String protocol;

}
