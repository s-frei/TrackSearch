package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;

public interface TrackSearchClient<T extends Track> {

    TrackList<T> getTracksForSearch(String search) throws TrackSearchException;

    TrackList<T> getNext(TrackList<? extends Track> trackList) throws TrackSearchException;

    String getStreamUrl(T track) throws TrackSearchException;

    boolean hasPagingValues(TrackList<? extends Track> trackList);

}
