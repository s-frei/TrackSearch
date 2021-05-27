package io.sfrei.tracksearch_web.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sfrei.tracksearch.clients.MultiTrackSearchClient;
import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.clients.setup.TrackSource;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import io.sfrei.tracksearch_web.problems.TrackSearchProblem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Base64;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class TrackSearchService {

    private final MultiTrackSearchClient trackSearchClient;

    private final TrackSearchCookie cookieBuilder;

    public TrackSearchService(MultiTrackSearchClient trackSearchClient) {
        this.trackSearchClient = trackSearchClient;
        this.cookieBuilder = new TrackSearchCookie();
    }

    public TrackList<Track> getTracksForSearch(String search, Set<TrackSource> trackSources) {
        log.debug("GetTracksForSearch for: {} - sources: {}", search, trackSources);
        try {
            return trackSources == null ?
                    trackSearchClient.getTracksForSearch(search) :
                    trackSearchClient.getTracksForSearch(search, trackSources);
        } catch (TrackSearchException e) {
            throw new TrackSearchProblem(e);
        }
    }

    public TrackList<Track> getNextTracks(TrackList<Track> trackList) {
        log.debug("GetNextTracks for: {}", trackList.getQueryParam());
        try {
            return trackSearchClient.getNext(trackList);
        } catch (TrackSearchException e) {
            throw new TrackSearchProblem(e);
        }
    }

    public String getStreamUrl(Track track) {
        log.debug("GetStreamUrl: {}", track);
        try {
            return trackSearchClient.getStreamUrl(track);
        } catch (TrackSearchException e) {
            throw new TrackSearchProblem(e);
        }
    }

    public TrackSearchCookie cookie() {
        return this.cookieBuilder;
    }

    public static class TrackSearchCookie {

        private static final String COOKIE_PREFIX = "tracksearch_";
        public static final String QUERY_TYPE = COOKIE_PREFIX + "query_t";
        public static final String QUERY_INFORMATION = COOKIE_PREFIX + "query_i";

        private final ObjectMapper objectMapper;

        private TrackSearchCookie() {
            objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        }

        public MultiValueMap<String, String> getCookieResponseHeaders(TrackList<Track> trackList)
                throws TrackSearchProblem {

            QueryType queryType = trackList.getQueryType();
            Map<String, String> queryInformation = trackList.getQueryInformation();

            MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

            try {

                final String qtValue = queryType.name();
                headers.add(HttpHeaders.SET_COOKIE, cookie(QUERY_TYPE, qtValue));

                final String qiValue = objectMapper.writeValueAsString(queryInformation);
                headers.add(HttpHeaders.SET_COOKIE, cookie(QUERY_INFORMATION, encode(qiValue)));

                return headers;

            } catch (JsonProcessingException e) {
                throw new TrackSearchProblem("Cannot build cookie", e);
            }
        }

        private String cookie(String name, String value) {
            return name.concat("=").concat(value);
        }

        @SuppressWarnings("unchecked")
        public TrackList<Track> tackListFromRequestHeaders(String qtValue, String qiValue)
                throws TrackSearchProblem {

            if (qtValue.isEmpty() || qiValue.isEmpty())
                throw new TrackSearchProblem("Cookie not set - search for something first");

            QueryType queryType = QueryType.valueOf(qtValue);

            try {
                Map<String, String> queryInformation = objectMapper.readValue(decode(qiValue), Map.class);
                return new BaseTrackList<>(null, queryType, queryInformation);
            } catch (JsonProcessingException e) {
                throw new TrackSearchProblem("Cannot parse cookie", e);
            }
        }

        private String encode(String json) {
            return Base64.getEncoder().encodeToString(json.getBytes());
        }

        private String decode(String cookieValue) {
            return new String(Base64.getDecoder().decode(cookieValue));
        }

    }

}
