package io.sfrei.tracksearch.tracks.metadata;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Getter
@ToString
@SuperBuilder
@AllArgsConstructor
public class TrackFormat {

    private final String mimeType;

    private final FormatType formatType;

    private final String audioQuality;

    private boolean streamReady;

    private final String url;

    public boolean streamNotReady() {
        return !streamReady;
    }

}
