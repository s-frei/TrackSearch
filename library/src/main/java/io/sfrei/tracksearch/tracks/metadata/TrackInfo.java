package io.sfrei.tracksearch.tracks.metadata;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class TrackInfo<T extends TrackFormat> {

    private final List<T> formats;

}
