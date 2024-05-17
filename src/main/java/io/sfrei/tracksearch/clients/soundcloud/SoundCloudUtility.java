/*
 * Copyright (C) 2024 s-frei (sfrei.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.sfrei.tracksearch.clients.soundcloud;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sfrei.tracksearch.tracks.TrackListProvider;
import io.sfrei.tracksearch.tracks.TrackStreamProvider;
import io.sfrei.tracksearch.clients.common.QueryType;
import io.sfrei.tracksearch.exceptions.SoundCloudException;
import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.GenericTrackList;
import io.sfrei.tracksearch.tracks.SoundCloudTrack;
import io.sfrei.tracksearch.tracks.deserializer.soundcloud.SoundCloudTrackDeserializer;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.SoundCloudTrackInfo;
import io.sfrei.tracksearch.utils.ObjectMapperBuilder;
import io.sfrei.tracksearch.utils.json.JsonElement;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public final class SoundCloudUtility {

    private static final String SOUNDCLOUD_CLIENT_ID_PREFIX = "client_id:";
    private static final String HYDRATABLE_SCRIPT_PREFIX = "window.__sc_hydration = ";
    private static final String SOUNDCLOUD_CLIENT_ID_REGEX = SOUNDCLOUD_CLIENT_ID_PREFIX + "\"[a-zA-Z0-9]+\"";
    private static final Pattern SOUNDCLOUD_CLIENT_ID_PATTERN = Pattern.compile(SOUNDCLOUD_CLIENT_ID_REGEX);

    private static final ObjectMapper MAPPER = ObjectMapperBuilder.create()
            .addDeserializer(SoundCloudTrack.SoundCloudTrackBuilder.class, new SoundCloudTrackDeserializer()).get();

    List<String> getCrossOriginScripts(final String html) {
        final Document doc = Jsoup.parse(html);
        final Elements scriptsDom = doc.getElementsByTag("script");
        return scriptsDom.stream()
                .filter(element -> element.hasAttr("crossorigin"))
                .map(element -> element.attr("src"))
                .peek(crossOriginScript -> log.trace("CrossOriginScript: {}", crossOriginScript))
                .collect(Collectors.toList());
    }

    Optional<String> getClientID(final String script) {
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

    String extractTrackURL(final String html) throws TrackSearchException {
        Document document = Jsoup.parse(html);
        Element embedUrlMeta = document.select("meta[itemprop=embedUrl]").first();

        return Optional.ofNullable(embedUrlMeta)
                .map(url -> url.attr("content"))
                .map(HttpUrl::parse)
                .map(url -> url.queryParameter("url"))
                .map(url -> url.replace("//api.", "//api-v2."))
                .orElseThrow(() -> new TrackSearchException("Failed extracting track URL"));
    }

    SoundCloudTrack extractSoundCloudTrack(final String json,
                                           final TrackStreamProvider<SoundCloudTrack> trackStreamProvider)
            throws SoundCloudException {

        final JsonElement trackJsonElement = JsonElement.readTreeCatching(MAPPER, json)
                .orElseThrow(() -> new SoundCloudException("Cannot parse SoundCloud track JSON"));

        return trackJsonElement.mapCatching(MAPPER, SoundCloudTrack.SoundCloudTrackBuilder.class)
                .trackStreamProvider(trackStreamProvider)
                .build();
    }

    GenericTrackList<SoundCloudTrack> extractSoundCloudTracks(final String json, final QueryType queryType, final String query,
                                                              final TrackListProvider<SoundCloudTrack> nextTrackListFunction,
                                                              final TrackStreamProvider<SoundCloudTrack> trackStreamProvider)
            throws SoundCloudException {

        final JsonElement responseElement = JsonElement.readTreeCatching(MAPPER, json)
                .orElseThrow(() -> new SoundCloudException("Cannot parse SoundCloudTracks JSON"))
                .paths("collection");

        final List<SoundCloudTrack> scTracks = responseElement.elements()
                .map(element -> element.mapCatching(MAPPER, SoundCloudTrack.SoundCloudTrackBuilder.class))
                .filter(Objects::nonNull)
                .peek(soundCloudTrackBuilder -> soundCloudTrackBuilder.trackStreamProvider(trackStreamProvider))
                .map(SoundCloudTrack.SoundCloudTrackBuilder::build)
                .collect(Collectors.toList());

        final Map<String, String> queryInformation = SoundCloudClient.makeQueryInformation(query);
        final GenericTrackList<SoundCloudTrack> trackList = GenericTrackList.using(queryType, queryInformation, nextTrackListFunction).withTracks(scTracks);

        final int tracksSize = scTracks.size();
        trackList.addQueryInformationValue(SoundCloudClient.OFFSET_KEY, tracksSize);
        log.debug("Found {} SoundCloud tracks for {}: {}", tracksSize, queryType, query);
        return trackList;
    }

    SoundCloudTrackInfo extractTrackInfoFromHTML(final String html) throws SoundCloudException {

        Document document = Jsoup.parse(html);

        final Optional<String> hydrationData = document.select("script").stream()
                .filter(scriptTag -> scriptTag.data().startsWith(HYDRATABLE_SCRIPT_PREFIX))
                .findFirst()
                .map(element -> element.data().replaceFirst(HYDRATABLE_SCRIPT_PREFIX, ""));

        if (hydrationData.isEmpty()) throw new SoundCloudException("Hydration data could not be found in HTML");

        final JsonElement hydrationDataElement = JsonElement.readTreeCatching(MAPPER, hydrationData.get())
                .orElseThrow(() -> new SoundCloudException("Cannot parse hydration data JSON"));

        final Optional<JsonElement> hydratableSoundElement = hydrationDataElement.arrayElements()
                .filter(element -> element.asString("hydratable").equals("sound"))
                .findFirst();

        if (hydratableSoundElement.isEmpty())
            throw new SoundCloudException("Hydratable sound element not be found in JSON");

        return extractTrackInfo(hydratableSoundElement.get().paths("data"));
    }

    public static SoundCloudTrackInfo extractTrackInfo(final JsonElement jsonElement) {

        final List<SoundCloudTrackFormat> trackFormats = jsonElement.paths("media", "transcodings")
                .arrayElements()
                .map(transcoding -> {
                    final String formatUrl = transcoding.asString("url");
                    final String audioQuality = transcoding.asString("quality");

                    final JsonElement formatElement = transcoding.paths("format");
                    final String mimeType = formatElement.asString("mime_type");
                    final String protocol = formatElement.asString("protocol");

                    return SoundCloudTrackFormat.builder()
                            .mimeType(mimeType)
                            .type(FormatType.Audio)
                            .audioQuality(audioQuality)
                            .protocol(protocol)
                            .url(formatUrl)
                            .build();
                })
                .collect(Collectors.toList());

        return new SoundCloudTrackInfo(trackFormats);
    }

    String extractStreamUrl(final String json) throws SoundCloudException {
        return JsonElement.readTreeCatching(MAPPER, json)
                .map(element -> element.asString("url"))
                .orElseThrow(() -> new SoundCloudException("Cannot extract stream URL from JSON"));
    }

}
