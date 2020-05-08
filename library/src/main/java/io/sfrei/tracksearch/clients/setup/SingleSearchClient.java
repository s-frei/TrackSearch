package io.sfrei.tracksearch.clients.setup;

import io.sfrei.tracksearch.clients.TrackSearchClient;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;

public abstract class SingleSearchClient<T extends Track> extends ClientProvider implements TrackSearchClient<T> {

    protected void throwIfPagingValueMissing(ClientProvider source, TrackList<? extends Track> trackList)
            throws TrackSearchException {

        if (!hasPagingValues(trackList))
            throw new TrackSearchException("Can not get next - paging value/s missing for " + source.getClass().getSimpleName());
    }

}
