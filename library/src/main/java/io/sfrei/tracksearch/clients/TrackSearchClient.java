package io.sfrei.tracksearch.clients;

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import lombok.NonNull;

/**
 * Main interface containing all functionality a client offers to the user.
 * @param <T> the track type the client implementing this is used for.
 */
public interface TrackSearchClient<T extends Track> {

    /**
     * Search for tracks using a string containing keywords.
     * @param search keywords to search for.
     * @return a track list containing all found tracks.
     * @throws TrackSearchException when the client encountered a problem on searching.
     */
    TrackList<T> getTracksForSearch(@NonNull String search) throws TrackSearchException;

    /**
     * Search for the next tracks for last result.
     * @param trackList a previous search result for that client.
     * @return a track list containing the next tracks available.
     * @throws TrackSearchException when the client encounters a problem on getting the next tracks.
     */
    TrackList<T> getNext(@NonNull TrackList<? extends Track> trackList) throws TrackSearchException;

    /**
     * Get the audio stream URL in the highest possible audio resolution.
     * @param track from this client.
     * @return the audio stream URL.
     * @throws TrackSearchException when the URL could not be exposed.
     */
    String getStreamUrl(@NonNull T track) throws TrackSearchException;

    /**
     * Get the audio stream URL in the highest possible audio resolution and retry when there was a failure.
     * @param track from this client.
     * @param retries retry when stream URL resolving was not successful. This is determined with another request/s.
     * @return the audio stream URL.
     * @throws TrackSearchException when the URL could not be exposed.
     */
    String getStreamUrl(@NonNull T track, int retries) throws TrackSearchException;

    /**
     * Check the track list for this client if the paging values to get next are present.
     * @param trackList a previous search result for this client.
     * @return either the paging values are present or not.
     */
    boolean hasPagingValues(@NonNull TrackList<? extends Track> trackList);

}
