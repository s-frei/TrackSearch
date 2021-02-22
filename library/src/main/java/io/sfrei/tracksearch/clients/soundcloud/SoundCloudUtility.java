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

    private static final String STREAM_URL_MAIM_PART = "https://api-v2.soundcloud.com/media/[a-zA-Z0-9:/-]+";
    private static final String PROGRESSIVE_SOUNDCLOUD_STREAM_REGEX = STREAM_URL_MAIM_PART + "/stream/progressive"; // the stream to go for
    private static final String ALTERNATIVE_PROGRESSIVE_SOUNDCLOUD_STREAM_REGEX = STREAM_URL_MAIM_PART + "/preview/progressive"; // non SC Go(+) membership
    private static final String ALTERNATIVE_SOUNDCLOUD_STREAM_REGEX = STREAM_URL_MAIM_PART + "/stream/hls"; // .m3u8 - hls stream
    private static final Pattern PROGRESSIVE_SOUNDCLOUD_STREAM_URL_PATTERN = Pattern.compile(PROGRESSIVE_SOUNDCLOUD_STREAM_REGEX);
    private static final Pattern ALTERNATIVE_PROGRESSIVE_SOUNDCLOUD_STREAM_URL_PATTERN = Pattern.compile(ALTERNATIVE_PROGRESSIVE_SOUNDCLOUD_STREAM_REGEX);
    private static final Pattern ALTERNATIVE_SOUNDCLOUD_STREAM_URL_PATTERN = Pattern.compile(ALTERNATIVE_SOUNDCLOUD_STREAM_REGEX);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    protected List<String> getCrossOriginScripts(final String html) {
        final Document doc = Jsoup.parse(html);
        final Elements scriptsDom = doc.getElementsByTag("script");
        return scriptsDom.stream()
                .filter(element -> element.hasAttr("crossorigin"))
                .map(element -> element.attr("src"))
                .peek(crossOriginScript -> log.trace("CrossOriginScript: {}", crossOriginScript))
                .collect(Collectors.toList());
    }

    protected Optional<String> getClientID(final String script) {
        final Matcher clientIdMatcher = SOUNDCLOUD_CLIENT_ID_PATTERN.matcher(script);
        if (clientIdMatcher.find()) {
            final String clientID = clientIdMatcher.group()
                    .replace(SOUNDCLOUD_CLIENT_ID_PREFIX, "")
                    .replaceAll("[^a-zA-Z0-9]+", "");
            log.debug("ClientID was found: {} ", clientID);
            return Optional.of(clientID);
        }
        return Optional.empty();
    }

    protected BaseTrackList<SoundCloudTrack> getSoundCloudTracks(final String json, final QueryType queryType, final String query,
                                                                 final Function<SoundCloudTrack, String> streamUrlProvider)
            throws SoundCloudException {

        final JsonElement responseElement;
        try {
            responseElement = JsonElement.read(MAPPER, json).get("collection");
        } catch (JsonProcessingException e) {
            throw new SoundCloudException("GetSoundCloudTracks - " + e.getMessage());
        }

        final List<SoundCloudTrack> scTracks = responseElement.elements()
                .map(content -> {
                    try {
                        return content.mapToObject(MAPPER, SoundCloudTrack.class);
                    } catch (JsonProcessingException e) {
                        log.error("Error parsing SoundCloud track JSON: {}", e.getMessage());
                        return null;
                    }
                }).filter(Objects::nonNull)
                .peek(soundCloudTrack -> soundCloudTrack.setStreamUrlProvider(streamUrlProvider))
                .collect(Collectors.toList());

        final int foundTracks = scTracks.size();
        final Map<String, String> queryInformation = SoundCloudClient.makeQueryInformation(query);
        final BaseTrackList<SoundCloudTrack> trackList = new BaseTrackList<>(scTracks, queryType, queryInformation);
        trackList.addQueryInformationValue(SoundCloudClient.OFFSET_KEY, foundTracks);
        log.debug("Found {} SoundCloud Tracks", foundTracks);
        return trackList;
    }

    protected Optional<String> getUrlForStream(final String html) {
        final Matcher progressiveStreamUrlMatcher = PROGRESSIVE_SOUNDCLOUD_STREAM_URL_PATTERN.matcher(html);
        if (progressiveStreamUrlMatcher.find()) {
            final String progressiveStreamUrl = progressiveStreamUrlMatcher.group();
            log.trace("ProgressiveStreamURL was found: {}", progressiveStreamUrl);
            return Optional.of(progressiveStreamUrl);
        }
        final Matcher alternativeProgressiveStreamUrlMatcher = ALTERNATIVE_PROGRESSIVE_SOUNDCLOUD_STREAM_URL_PATTERN.matcher(html);
        if (alternativeProgressiveStreamUrlMatcher.find()) {
            final String alternativeProgressiveStreamUrl = alternativeProgressiveStreamUrlMatcher.group();
            log.trace("Alternative ProgressiveStreamURL was found: {}", alternativeProgressiveStreamUrl);
            return Optional.of(alternativeProgressiveStreamUrl);
        }
        final Matcher alternativeStreamUrlMatcher = ALTERNATIVE_SOUNDCLOUD_STREAM_URL_PATTERN.matcher(html);
        if (alternativeStreamUrlMatcher.find()) {
            final String alternativeStreamUrl = alternativeStreamUrlMatcher.group();
            log.trace("Alternative StreamURL was found: {}", alternativeStreamUrl);
            return Optional.of(alternativeStreamUrl);
        }
        return Optional.empty();
    }

    protected String extractStreamUrl(final String body) {
        return body.replace("\"url\":", "").replaceAll("[{}\"]", "");
    }

}
