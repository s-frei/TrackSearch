package io.sfrei.tracksearch.tracks.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class TrackFormat {

    private final String mimeType;

    private final String audioQuality;

    @Setter
    private boolean streamReady;

    private final String url;

}
