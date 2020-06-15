package io.sfrei.tracksearch.tracks;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.utils.ReplaceUtility;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class BaseTrack {

    private final TrackSource source;

    private final String title;

    private final Long length;

    @EqualsAndHashCode.Include
    private final String url;

    public String getCleanTitle() {
        return ReplaceUtility.replaceUnnecessary(title);
    }

    public String toPrettyString() {
        return pretty(title);
    }

    public String toPrettyCleanString() {
        return pretty(getCleanTitle());
    }

    private String pretty(String title) {
        return "SOURCE: " + source.name() + " - Title: " + title + " - Length: " + length + " - Url: " + url;
    }

}
