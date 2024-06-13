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

package io.sfrei.tracksearch.clients.youtube;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.sfrei.tracksearch.clients.common.QueryType;
import io.sfrei.tracksearch.clients.common.SharedClient;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.*;
import io.sfrei.tracksearch.tracks.deserializer.youtube.YouTubeListTrackDeserializer;
import io.sfrei.tracksearch.tracks.deserializer.youtube.YouTubeURLTrackDeserializer;
import io.sfrei.tracksearch.tracks.metadata.MimeType;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.utils.CacheMap;
import io.sfrei.tracksearch.utils.ObjectMapperBuilder;
import io.sfrei.tracksearch.utils.URLModifier;
import io.sfrei.tracksearch.utils.json.JsonElement;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public final class YouTubeUtility {

    private static final String JS_SPLICE = ".splice";
    private static final String JS_SLICE = ".slice";
    private static final String JS_REVERSE = ".reverse";
    private static final String FUNCTION_CALL = "([a-zA-Z]+.([a-zA-Z0-9]+)\\(a,([0-9]+)\\))";
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile(FUNCTION_CALL);
    private static final Pattern OBFUSCATE_FUNCTIONS_CALLS_PATTERN = Pattern.compile(
            "function\\(a\\)\\{a=a.split\\(\"\"\\);([a-zA-Z1-9 =,-\\[\\]\"()]+);"
    );
    private static final String VAR_NAME = "[a-zA-Z0-9]+";
    private static final String FUNCTION_END = "}*,?\\n?";
    private static final String SLICE = wrap("\\(a,b\\)\\{return a\\.slice\\(b\\)");
    private static final String SPLICE = wrap("\\(a,b\\)\\{a\\.splice\\(0,b\\)");
    private static final String REVERSE = wrap("\\(a\\)\\{a\\.reverse\\(\\)");
    private static final String SWAP = wrap("\\(a,b\\)\\{var c=a\\[0\\];a\\[0\\]=a\\[b%a\\.length\\];a\\[b%a.length\\]=c");
    private static final Pattern OBFUSCATE_FUNCTIONS_PATTERN = Pattern.compile(
            "var " + VAR_NAME + "=\\{" +
                    "(("
                    + SLICE + "|" + SPLICE + "|" + REVERSE + "|" + SWAP +
                    ")+)"
    );
    private static final Pattern EMBEDDED_PLAYER_SCRIPT_PATTERN = Pattern.compile("src=\"(/[a-zA-Z0-9/-_.]+base.js)\"");

    private static final CacheMap<String, SignatureResolver> sigResolverCache = new CacheMap<>();

    private static final ObjectMapper MAPPER = ObjectMapperBuilder.create()
            .addDeserializer(YouTubeTrack.ListYouTubeTrackBuilder.class, new YouTubeListTrackDeserializer())
            .addDeserializer(YouTubeTrack.URLYouTubeTrackBuilder.class, new YouTubeURLTrackDeserializer())
            .get();

    private static String wrap(String functionContent) {
        return "(" + VAR_NAME + ":function" + functionContent + FUNCTION_END + ")";
    }

    static YouTubeTrack extractYouTubeTrack(final String json,
                                            final TrackInfoProvider<YouTubeTrack, YouTubeTrackInfo> trackInfoProvider,
                                            final TrackStreamProvider<YouTubeTrack> trackStreamProvider)
            throws YouTubeException {

        final JsonElement trackJsonElement = JsonElement.readTreeCatching(MAPPER, json)
                .orElseThrow(() -> new YouTubeException("Cannot parse YouTubeTrack JSON"));

        return playerResponseFromTrackJSON(trackJsonElement)
                .mapCatching(MAPPER, YouTubeTrack.URLYouTubeTrackBuilder.class).getBuilder()
                .trackStreamProvider(trackStreamProvider)
                .trackInfoProvider(trackInfoProvider)
                .build();
    }

    static GenericTrackList<YouTubeTrack> extractYouTubeTracks(final String json, final QueryType queryType, final String query,
                                                               final TrackListProvider<YouTubeTrack> nextTrackListFunction,
                                                               final TrackInfoProvider<YouTubeTrack, YouTubeTrackInfo> trackInfoProvider,
                                                               final TrackStreamProvider<YouTubeTrack> trackStreamProvider)
            throws YouTubeException {

        final JsonElement rootElement = JsonElement.readTreeCatching(MAPPER, json)
                .orElseThrow(() -> new YouTubeException("Cannot parse YouTubeTracks JSON"));

        final JsonElement responseElement = rootElement.paths("response").orElse(rootElement).elementAtIndex(1).paths("response");

        final JsonElement defaultElement = responseElement.asUnresolved()
                .paths("contents", "twoColumnSearchResultsRenderer", "primaryContents", "sectionListRenderer", "contents");

        final JsonElement contentHolder = defaultElement
                .lastForPath("itemSectionRenderer") // Avoid sponsored
                .orElse(responseElement)
                .paths("onResponseReceivedCommands")
                .firstElement()
                .paths("appendContinuationItemsAction", "continuationItems")
                .firstElement()
                .paths("itemSectionRenderer")
                .orElse(responseElement)
                .paths("onResponseReceivedCommands")
                .firstElement()
                .paths("appendContinuationItemsAction", "continuationItems")
                .firstElement()
                .paths("itemSectionRenderer")
                .orElse(responseElement)
                .paths("continuationContents", "itemSectionContinuation", "itemSectionContinuation")
                .orElse(responseElement)
                .paths("continuationContents", "sectionListContinuation", "contents")
                .firstElement()
                .paths("itemSectionRenderer");

        final String cToken = extractCToken(responseElement, defaultElement, contentHolder);

        final JsonElement contents = contentHolder.asUnresolved().paths("contents");
        final List<YouTubeTrack> ytTracks = contents.elements()
                .filter(content -> content.paths("videoRenderer", "upcomingEventData").isNull()) // Avoid premieres
                .filter(content -> content.paths("promotedSparklesWebRenderer").isNull()) // Avoid ads
                .map(content -> content.paths("videoRenderer").orElse(content).paths("searchPyvRenderer", "ads").firstElement().paths("promotedVideoRenderer"))
                .filter(renderer -> renderer.asUnresolved().paths("lengthText").isPresent()) // Avoid live streams
                .map(renderer -> renderer.mapCatching(MAPPER, YouTubeTrack.ListYouTubeTrackBuilder.class))
                .filter(Objects::nonNull)
                .map(YouTubeTrack.ListYouTubeTrackBuilder::getBuilder)
                .peek(youTubeTrackBuilder -> youTubeTrackBuilder.trackStreamProvider(trackStreamProvider))
                .peek(youTubeTrackBuilder -> youTubeTrackBuilder.trackInfoProvider(trackInfoProvider))
                .map(YouTubeTrack.YouTubeTrackBuilder::build)
                .collect(Collectors.toList());

        final Map<String, String> queryInformation = YouTubeClient.makeQueryInformation(query, cToken);
        final GenericTrackList<YouTubeTrack> trackList = GenericTrackList.using(queryType, queryInformation, nextTrackListFunction)
                .withTracks(ytTracks);

        int tracksSize = ytTracks.size();
        trackList.addQueryInformationValue(YouTubeClient.OFFSET_KEY, tracksSize);
        log.debug("Found {} YouTube Tracks for {}: {}", tracksSize, queryType, query);
        return trackList;
    }

    private static String extractCToken(JsonElement responseElement, JsonElement defaultElement, JsonElement contentHolder) {
        if (contentHolder.nodePresent("continuations")) {
            return contentHolder.asUnresolved()
                    .paths("continuations")
                    .firstElement()
                    .paths("nextContinuationData")
                    .asString("continuation");
        }
        return responseElement.asUnresolved()
                .paths("onResponseReceivedCommands")
                .firstElement()
                .paths("appendContinuationItemsAction", "continuationItems")
                .elementAtIndex(1)
                .paths("continuationItemRenderer", "continuationEndpoint", "continuationCommand")
                .orElse(defaultElement)
                .findElement("continuationItemRenderer")
                .paths("continuationEndpoint", "continuationCommand")
                .asString("token");
    }

    static YouTubeTrackInfo extractTrackInfo(final String trackJson, final String trackUrl)
            throws YouTubeException {

        final JsonElement jsonElement = JsonElement.readTreeCatching(MAPPER, trackJson)
                .orElseThrow(() -> new YouTubeException("Failed parsing Youtube track JSON"));

        final JsonElement playerElement;
        if (jsonElement.isArray()) {
            playerElement = jsonElement.elementAtIndex(2).paths("player");
        } else {
            playerElement = jsonElement.findElement("player");
        }

        String scriptUrl = null;

        final JsonElement playerArgs = playerElement.paths("args");
        if (playerElement.isPresent() && playerArgs.isPresent()) {

            scriptUrl =playerElement.paths("assets").asString("js");

            final JsonElement streamingData = playerArgs.paths("player_response")
                    .reReadTree(MAPPER)
                    .orElseThrow(() -> new YouTubeException("Failed parsing player response JSON"))
                    .paths("streamingData");

            return extractTrackInfoFromStreamingData(streamingData, trackUrl, scriptUrl);
        } else {
            final JsonElement playerResponse = playerResponseFromTrackJSON(jsonElement);

            final JsonElement streamingData = playerResponse.asUnresolved().paths("streamingData");
            return extractTrackInfoFromStreamingData(streamingData, trackUrl, scriptUrl);
        }
    }

    public static @NotNull YouTubeTrackInfo extractTrackInfoFromStreamingData(
            final JsonElement streamingData, final String trackUrl, String scriptUrl
    ) throws YouTubeException {

        final JsonElement formatsElement = streamingData.paths("formats");
        final Stream<YouTubeTrackFormat> formats = formatsElement.isPresent() ?
                getFormatsFromStream(formatsElement.arrayElements()) : Stream.empty();

        final Stream<JsonElement> adaptiveFormatsStream = streamingData.paths("adaptiveFormats").arrayElements();
        final Stream<YouTubeTrackFormat> adaptiveFormats = getFormatsFromStream(adaptiveFormatsStream);

        final List<YouTubeTrackFormat> trackFormats = Stream.concat(formats, adaptiveFormats).collect(Collectors.toList());

        if (trackFormats.stream().anyMatch(YouTubeTrackFormat::isStreamNotReady) && scriptUrl == null) {
            log.trace("Try to get player script trough embedded URL");
            final String embeddedUrl = trackUrl.replace("youtube.com/", "youtube.com/embed/");
            final String embeddedPageContent = SharedClient.request(embeddedUrl).getContent();
            if (embeddedPageContent != null) {
                final Matcher matcher = EMBEDDED_PLAYER_SCRIPT_PATTERN.matcher(embeddedPageContent);
                if (matcher.find()) {
                    log.trace("Found player script in embedded URL: '{}'", embeddedUrl);
                    scriptUrl = matcher.group(1);
                }
            }
        }

        return new YouTubeTrackInfo(trackFormats, scriptUrl);
    }

    private static JsonElement playerResponseFromTrackJSON(JsonElement jsonElement) {
        return jsonElement.elementAtIndex(2)
                .paths("playerResponse")
                .orElse(jsonElement)
                .paths("playerResponse");
    }

    private static Stream<YouTubeTrackFormat> getFormatsFromStream(final Stream<JsonElement> formats) {
        return formats.map(format -> {
            final String mimeType = format.asString("mimeType");
            final String audioQuality = format.asString("audioQuality");
            final String audioSampleRate = format.asString("audioSampleRate");

            final JsonElement cipherElement = format.paths("cipher")
                    .orElse(format)
                    .paths("signatureCipher");

            if (cipherElement.isNull()) {
                final String url = format.asString("url");
                return YouTubeTrackFormat.builder()
                        .mimeType(MimeType.byIdentifier(mimeType))
                        .audioQuality(audioQuality)
                        .audioSampleRate(audioSampleRate)
                        .streamReady(true)
                        .url(url)
                        .build();
            } else {
                final String cipher = cipherElement.asString();
                final Map<String, String> params = URLModifier.splitParamsAndDecode(cipher);
                return YouTubeTrackFormat.builder()
                        .mimeType(MimeType.byIdentifier(mimeType))
                        .audioQuality(audioQuality)
                        .audioSampleRate(audioSampleRate)
                        .streamReady(false)
                        .url(params.get("url"))
                        .sigParam(params.getOrDefault("sp", "sig"))
                        .sigValue(params.get("s"))
                        .build();
            }
        });
    }

    private static Map<String, SignaturePart.SignatureOccurrence> getObfuscateFunctionDefinitions(final String scriptPart) {
        final HashMap<String, SignaturePart.SignatureOccurrence> obfuscateFunctionsDefinitions = new HashMap<>();
        final String[] functions = scriptPart.split("\n");
        for (final String function : functions) {
            final String functionName = getFunctionName(function);
            if (functionName == null)
                continue;

            final SignaturePart.SignatureOccurrence occurrence;
            if (function.contains(JS_SLICE))
                occurrence = SignaturePart.SignatureOccurrence.SLICE;
            else if (function.contains(JS_SPLICE))
                occurrence = SignaturePart.SignatureOccurrence.SPLICE;
            else if (function.contains(JS_REVERSE))
                occurrence = SignaturePart.SignatureOccurrence.REVERSE;
            else
                occurrence = SignaturePart.SignatureOccurrence.SWAP;

            obfuscateFunctionsDefinitions.put(functionName, occurrence);
        }
        return obfuscateFunctionsDefinitions;
    }

    private static String getFunctionName(final String wholeFunction) {
        final String[] split = wholeFunction.split(":function");
        return split.length > 0 ? split[0] : null;
    }

    static String getSignature(final YouTubeTrackFormat youtubeTrackFormat, String scriptUrl, final String scriptBody)
            throws YouTubeException {

        final String sigValue = youtubeTrackFormat.getSigValue();

        if (sigResolverCache.containsKey(scriptUrl)) {
            log.trace("Use cached signature resolver for: {}", scriptUrl);
            return sigResolverCache.get(scriptUrl).resolveSignature(sigValue);
        }

        final Matcher obfuscateFunctionsMatcher = OBFUSCATE_FUNCTIONS_PATTERN.matcher(scriptBody);
        if (!obfuscateFunctionsMatcher.find())
            throw new YouTubeException("Was not able to find obfuscate functions");

        final String obfuscateFunctions = obfuscateFunctionsMatcher.group(1);
        final Map<String, SignaturePart.SignatureOccurrence> obfuscateFunctionDefinitions =
                getObfuscateFunctionDefinitions(obfuscateFunctions);

        final Matcher obfuscateFunctionsCallsMatcher = OBFUSCATE_FUNCTIONS_CALLS_PATTERN.matcher(scriptBody);
        if (!obfuscateFunctionsCallsMatcher.find())
            throw new YouTubeException("Was not able to find obfuscate functions calls");

        final Matcher obfuscateFunctionCallMatcher = FUNCTION_CALL_PATTERN.matcher(obfuscateFunctionsCallsMatcher.group(1));

        final SignatureResolver signatureResolver = new SignatureResolver();

        while (obfuscateFunctionCallMatcher.find()) {
            final String obfuscateFunctionName = obfuscateFunctionCallMatcher.group(2).trim();
            final SignaturePart.SignatureOccurrence signatureOccurrence = obfuscateFunctionDefinitions.get(obfuscateFunctionName);

            if (signatureOccurrence == null)
                continue;

            final Integer obfuscateFunctionParameter = Integer.parseInt(obfuscateFunctionCallMatcher.group(3));
            signatureResolver.addSignaturePart(obfuscateFunctionName, signatureOccurrence, obfuscateFunctionParameter);
        }

        if (!sigResolverCache.containsKey(scriptUrl))
            sigResolverCache.put(scriptUrl, signatureResolver);

        return signatureResolver.resolveSignature(sigValue);
    }

    private record SignaturePart(String functionName,
                                 YouTubeUtility.SignaturePart.SignatureOccurrence occurrence,
                                 Integer parameter) {

        public enum SignatureOccurrence {
            SLICE, SPLICE, REVERSE, SWAP
        }
    }

    private static class SignatureResolver {

        private final List<SignaturePart> signatureParts;

        public SignatureResolver() {
            this.signatureParts = new ArrayList<>();
        }

        private void addSignaturePart(String functionName, SignaturePart.SignatureOccurrence occurrence, Integer parameter) {
            this.signatureParts.add(new SignaturePart(functionName, occurrence, parameter));
        }

        private String resolveSignature(final String signatureValue) {
            final StringBuilder signature = new StringBuilder(signatureValue);
            for (final SignaturePart signaturePart : signatureParts) {
                final Integer parameter = signaturePart.parameter();
                switch (signaturePart.occurrence()) {
                    case SLICE:
                    case SPLICE:
                        signature.delete(0, parameter);
                        break;
                    case REVERSE:
                        signature.reverse();
                        break;
                    case SWAP:
                        int charPos = parameter % signatureValue.length();
                        char swapChar = signature.charAt(0);
                        signature.setCharAt(0, signature.charAt(charPos));
                        signature.setCharAt(charPos, swapChar);
                        break;
                    default:
                        break;
                }
            }
            return signature.toString();
        }
    }

}
