package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;

import java.util.Set;

public interface MultiTrackSearchClient extends TrackSearchClient<Track>  {

    TrackList<Track> getTracksForSearch(String search) throws TrackSearchException;

    TrackList<Track> getNext(TrackList<? extends Track> trackList) throws TrackSearchException;

    String getStreamUrl(Track track) throws TrackSearchException;

    TrackList<Track> getTracksForSearch(String search, Set<TrackSource> sources) throws TrackSearchException;

}
