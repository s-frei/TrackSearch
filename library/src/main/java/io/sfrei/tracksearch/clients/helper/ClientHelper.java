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
                                                                  Function<String, Integer> requestForCodeFunction,
                                                                  final int retries) {

        return tryToGetStreamUrl(searchClient, track, requestForCodeFunction, retries + 1);
    }

    private static <T extends Track> Optional<String> tryToGetStreamUrl(TrackSearchClient<T> searchClient, T track,
                                                                        Function<String, Integer> requestForCodeFunction,
                                                                        int tries) {

        if (tries <= 0)
            return Optional.empty();

        try {
            final String streamUrl = searchClient.getStreamUrl(track);
            final int code = requestForCodeFunction.apply(streamUrl);
            if (Client.successResponseCode(code))
                return Optional.ofNullable(streamUrl);
            else {
                tries -= 1;
                log.warn("Error getting stream URL for {} - {} retries left",
                        searchClient.getClass().getSimpleName(), tries);
                return tryToGetStreamUrl(searchClient, track, requestForCodeFunction, tries);
            }
        } catch (TrackSearchException e) {
            log.error("Error getting stream URL for {}", e, searchClient.getClass().getSimpleName());
            return Optional.empty();
        }

    }

}
