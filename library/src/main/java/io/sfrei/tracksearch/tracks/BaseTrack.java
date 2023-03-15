package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.Duration;

@Getter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseTrack implements Track {

    private final TrackSource source;

    private final String title;

    private final Duration duration;

    @EqualsAndHashCode.Include
    private final String url;

    private String pretty(String title) {
        return String.format("%s: Title: '%s' - %s - URL: %s", source.name(), title, durationFormatted(), url);
    }

    public String pretty() {
        return pretty(title);
    }

    public String prettyAndClean() {
        return pretty(getCleanTitle());
    }

}
