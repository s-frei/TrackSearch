package io.sfrei.tracksearch.clients.soundcloud;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.exceptions.SoundCloudException;
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.utils.json.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
class SoundCloudUtility {

    private static final String SOUNDCLOUD_CLIENT_ID_PREFIX = "client_id:";
    private static final String SOUNDCLOUD_CLIENT_ID_REGEX = SOUNDCLOUD_CLIENT_ID_PREFIX + "\"[a-zA-Z0-9]+\"";
    private static final Pattern SOUNDCLOUD_CLIENT_ID_PATTERN = Pattern.compile(SOUNDCLOUD_CLIENT_ID_REGEX);

    private static final String SOUNDCLOUD_STREAM_PREFIX = "\"url\":";
    private static final String SOUNDCLOUD_STREAM_REGEX = SOUNDCLOUD_STREAM_PREFIX + "\"https://api-v2.soundcloud.com/media/[a-zA-Z0-9:/-]+/stream/progressive\"";
    private static final Pattern SOUNDCLOUD_STREAM_URL_PATTERN = Pattern.compile(SOUNDCLOUD_STREAM_REGEX);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    List<String> getCrossOriginScripts(String html) {
        Document doc = Jsoup.parse(html);
        Elements scriptsDom = doc.getElementsByTag("script");
        return scriptsDom.stream()
                .filter(element -> element.hasAttr("crossorigin"))
                .map(element -> element.attr("src"))
                .peek(crossOriginScript -> log.trace("CrossOriginScript: {}", crossOriginScript))
                .collect(Collectors.toList());
    }

    Optional<String> getClientID(String script) {
        Matcher clientIdMatcher = SOUNDCLOUD_CLIENT_ID_PATTERN.matcher(script);
        if (clientIdMatcher.find()) {
            String clientID = clientIdMatcher.group()
                    .replace(SOUNDCLOUD_CLIENT_ID_PREFIX, "")
                    .replaceAll("[^a-zA-Z0-9]+", "");
            log.debug("ClientID was found: {} ", clientID);
            return Optional.of(clientID);
        }
        return Optional.empty();
    }

    BaseTrackList<SoundCloudTrack> getSoundCloudTracks(String json, QueryType queryType, String query,
                                                       Function<SoundCloudTrack, String> streamUrlProvider)
            throws SoundCloudException {

        try {

            List<SoundCloudTrack> scTracks = JsonElement.read(MAPPER, json).get("collection").elements()
                    .map(content -> {
                        try {
                            return content.mapToObject(MAPPER, SoundCloudTrack.class);
                        } catch (JsonProcessingException e) {
                            return null;
                        }
                    }).filter(Objects::nonNull)
                    .peek(soundCloudTrack -> soundCloudTrack.setStreamUrlProvider(streamUrlProvider))
                    .collect(Collectors.toList());

            int foundTracks = scTracks.size();
            Map<String, String> queryInformation = SoundCloudClient.makeQueryInformation(query);
            BaseTrackList<SoundCloudTrack> trackList = new BaseTrackList<>(scTracks, queryType, queryInformation);
            trackList.addQueryInformationValue(SoundCloudClient.OFFSET_KEY, foundTracks);
            log.debug("Found {} SoundCloud Tracks", foundTracks);
            return trackList;
        } catch (IOException e) {
            throw new SoundCloudException("GetSoundCloudTracks - " + e.getMessage());
        }
    }

    Optional<String> getProgressiveStreamUrl(String html) {
        Matcher streamUrlMatcher = SOUNDCLOUD_STREAM_URL_PATTERN.matcher(html);
        if (streamUrlMatcher.find()) {
            String progressiveStreamUrl = streamUrlMatcher.group()
                    .replace(SOUNDCLOUD_STREAM_PREFIX, "")
                    .replaceAll("\"", "");
            log.trace("ProgressiveStreamURL was found: {}", progressiveStreamUrl);
            return Optional.of(progressiveStreamUrl);
        }
        return Optional.empty();
    }

    String getStreamUrl(String body) {
        return body
                .replace(SOUNDCLOUD_STREAM_PREFIX, "")
                .replace("{\"", "")
                .replace("\"}", "");
    }

}
