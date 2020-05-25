package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;

/**
 * Main interface containing all functionality a client offers to the user.
 * @param <T> the track type the client implementing this is used for.
 */
public interface TrackSearchClient<T extends Track> {

    /**
     * Search for tracks using a string containing keywords.
     * @param search keywords to search for.
     * @return A tracklist containing all found tracks.
     * @throws TrackSearchException when the client encountered a problem on searching.
     */
    TrackList<T> getTracksForSearch(String search) throws TrackSearchException;

    /**
     * Search for the next tracks for last result.
     * @param trackList a previous search result for that client.
     * @return A tracklist containing the next tracks available.
     * @throws TrackSearchException when the client encounters a problem on getting the next tracks.
     */
    TrackList<T> getNext(TrackList<? extends Track> trackList) throws TrackSearchException;

    /**
     * Get the audio stream URL in the highest possible audio resolution.
     * @param track from this client.
     * @return The audio stream URL.
     * @throws TrackSearchException when the URL could not be exposed.
     */
    String getStreamUrl(T track) throws TrackSearchException;

    /**
     * Check the tracklist for this client if the paging values to get next are present.
     * @param trackList a previous search result for this client.
     * @return either the paging values are present or not.
     */
    boolean hasPagingValues(TrackList<? extends Track> trackList);

}
