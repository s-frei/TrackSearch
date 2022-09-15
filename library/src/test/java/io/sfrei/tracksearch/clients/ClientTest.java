package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;

import java.io.IOException;

public interface ClientTest<T extends Track> {

    void tracksForSearch(String key) throws TrackSearchException;

    void trackListGotPagingValues(TrackList<T> trackList);

    void getNextTracks(TrackList<T> trackList) throws TrackSearchException;

    void checkMetadata(TrackList<T> trackList);

    void getStreamUrl(T track) throws IOException;

}
