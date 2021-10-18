package io.sfrei.tracksearch.clients.helper;

import io.sfrei.tracksearch.clients.TrackSearchClient;
import io.sfrei.tracksearch.clients.setup.Client;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.Track;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.function.Function;

@Slf4j
public final class ClientHelper {

    public static <T extends Track> Optional<String> getStreamUrl(TrackSearchClient<T> searchClient, T track,
                                                                  Function<String, Integer> requester, int retries)
            throws TrackSearchException {
        while (retries >= 0) {
            try {
                final String streamUrl = searchClient.getStreamUrl(track);
                final int code = requester.apply(streamUrl);
                if (Client.successResponseCode(code))
                    return Optional.ofNullable(streamUrl);
                log.debug("Retry to get stream URL");
                retries--;
            } catch (TrackSearchException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }

}
