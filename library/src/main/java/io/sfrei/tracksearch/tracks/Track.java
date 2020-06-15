package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.setup.TrackSource;

public interface Track {

    TrackSource getSource();

    String getTitle();

    String getCleanTitle();

    Long getLength();

    String getUrl();

    String getStreamUrl();

    boolean equals(Object o);

    String toPrettyString();

    String toPrettyCleanString();

}
