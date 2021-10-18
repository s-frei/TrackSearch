package io.sfrei.tracksearch.clients.setup;

import io.sfrei.tracksearch.clients.TrackSearchClient;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import org.jetbrains.annotations.Nullable;

import java.net.CookiePolicy;
import java.util.Map;

public abstract class SingleSearchClient<T extends Track> extends Client implements TrackSearchClient<T> {

    public SingleSearchClient(@Nullable CookiePolicy cookiePolicy, @Nullable Map<String, String> headers) {
        super(cookiePolicy, headers);
    }

    protected void throwIfPagingValueMissing(ClientProvider source, TrackList<? extends Track> trackList)
            throws TrackSearchException {

        if (!hasPagingValues(trackList))
            throw new TrackSearchException("Can not get next - paging value/s missing for " + source.getClass().getSimpleName());
    }

}
