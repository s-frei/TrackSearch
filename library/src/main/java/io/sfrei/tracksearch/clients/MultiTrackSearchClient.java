package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;

import java.util.Set;

/**
 * Interact with all available (all implemented) clients at the same time. All calls will get
 * processed asynchronous and returned when all threads have finished.
 */
public interface MultiTrackSearchClient extends TrackSearchClient<Track>  {

    /**
     * Search for tracks using a string containing keywords.
     * @param search keywords to search for.
     * @return a tracklist containing all found tracks on all available clients.
     * @throws TrackSearchException when any client encountered a problem while searching.
     */
    TrackList<Track> getTracksForSearch(String search) throws TrackSearchException;

    /**
     * Search for the next tracks for last result.
     * @param trackList a previous search result for all clients.
     * @return a tracklist containing all next tracks available for all previously searched clients.
     * @throws TrackSearchException when any client encountered a problem while getting next.
     */
    TrackList<Track> getNext(TrackList<? extends Track> trackList) throws TrackSearchException;

    /**
     * Get the audio stream URL in the highest possible audio resolution.
     * @param track from any client.
     * @return the audio stream URL.
     * @throws TrackSearchException when the URL could not be exposed.
     */
    String getStreamUrl(Track track) throws TrackSearchException;

    /**
     * Search for tracks using a string containing keywords on pre selected track sources.
     * @param search keywords to search for.
     * @param sources available to search on.
     * @return a tracklist containing all found tracks for selected clients.
     * @throws TrackSearchException when any client encountered a problem while searching.
     */
    TrackList<Track> getTracksForSearch(String search, Set<TrackSource> sources) throws TrackSearchException;

}
