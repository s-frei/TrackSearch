package io.sfrei.tracksearch.clients.setup;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public enum TrackSource {

    Youtube, Soundcloud;

    public static Set<TrackSource> setOf(TrackSource... sources) {
        return Arrays.stream(sources).collect(Collectors.toSet());
    }

}
