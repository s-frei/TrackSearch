/*
 * Copyright (C) 2023 s-frei (sfrei.io)
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.clients.setup.ResponseWrapper;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.utils.CacheMap;
import io.sfrei.tracksearch.utils.URLUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
class YouTubeUtility {

    private static final ObjectMapper MAPPER = new ObjectMapper();
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

    private final CacheMap<String, SignatureResolver> sigResolverCache;

    public YouTubeUtility() {
        sigResolverCache = new CacheMap<>();
    }

    private static String wrap(String functionContent) {
        return "(" + VAR_NAME + ":function" + functionContent + FUNCTION_END + ")";
    }

    protected BaseTrackList<YouTubeTrack> getYouTubeTracks(final String json, final QueryType queryType, final String query,
                                                           final Function<YouTubeTrack, String> streamUrlProvider)
            throws YouTubeException {

        final JsonElement rootElement = JsonElement.readHandled(MAPPER, json)
                .orElseThrow(() -> new YouTubeException("Cannot parse YouTubeTracks JSON"));

        final JsonElement responseElement = rootElement.path("response").orElse(rootElement).getAtIndex(1).path("response");

        final JsonElement defaultElement = responseElement.asUnresolved()
                .path("contents", "twoColumnSearchResultsRenderer", "primaryContents", "sectionListRenderer", "contents");

        final JsonElement contentHolder = defaultElement
                .firstElementWhereNotFound("itemSectionRenderer", "promotedSparklesWebRenderer")
                .orElse(responseElement)
                .path("onResponseReceivedCommands")
                .getFirstField()
                .path("appendContinuationItemsAction", "continuationItems")
                .getFirstField()
                .path("itemSectionRenderer")
                .orElse(responseElement)
                .path("onResponseReceivedCommands")
                .getFirstField()
                .path("appendContinuationItemsAction", "continuationItems")
                .getFirstField()
                .path("itemSectionRenderer")
                .orElse(responseElement)
                .path("continuationContents", "itemSectionContinuation", "itemSectionContinuation")
                .orElse(responseElement)
                .path("continuationContents", "sectionListContinuation", "contents")
                .getFirstField()
                .path("itemSectionRenderer");

        final String cToken = extractCToken(responseElement, defaultElement, contentHolder);

        final JsonElement contents = contentHolder.asUnresolved().path("contents");
        final List<YouTubeTrack> ytTracks = contents.elements()
                .filter(content -> content.path("videoRenderer", "upcomingEventData").isNull()) // Avoid premieres
                .filter(content -> content.path("promotedSparklesWebRenderer").isNull()) // Avoid ads
                .map(content -> content.path("videoRenderer").orElse(content).path("searchPyvRenderer", "ads").getFirstField().path("promotedVideoRenderer"))
                .filter(renderer -> renderer.asUnresolved().path("lengthText").isPresent()) // Avoid live streams
                .map(renderer -> renderer.mapToObjectHandled(MAPPER, YouTubeTrack.class))
                .filter(Objects::nonNull)
                .peek(youTubeTrack -> youTubeTrack.setStreamUrlProvider(streamUrlProvider))
                .collect(Collectors.toList());


        final Map<String, String> queryInformation = YouTubeClient.makeQueryInformation(query, cToken);
        final BaseTrackList<YouTubeTrack> trackList = new BaseTrackList<>(ytTracks, queryType, queryInformation);

        int tracksSize = ytTracks.size();
        trackList.addQueryInformationValue(YouTubeClient.OFFSET_KEY, tracksSize);
        log.debug("Found {} YouTube Tracks for {}: {}", tracksSize, queryType, query);
        return trackList;
    }

    private static String extractCToken(JsonElement responseElement, JsonElement defaultElement, JsonElement contentHolder) {
        if (contentHolder.fieldPresent("continuations")) {
            return contentHolder.asUnresolved()
                    .path("continuations")
                    .getFirstField()
                    .path("nextContinuationData")
                    .fieldAsString("continuation");
        }
        return responseElement.asUnresolved()
                .path("onResponseReceivedCommands")
                .getFirstField()
                .path("appendContinuationItemsAction", "continuationItems")
                .getAtIndex(1)
                .path("continuationItemRenderer", "continuationEndpoint", "continuationCommand")
                .orElse(defaultElement)
                .firstElementFor("continuationItemRenderer")
                .path("continuationEndpoint", "continuationCommand")
                .fieldAsString("token");
    }

    protected YouTubeTrackInfo getTrackInfo(final String json, final String trackUrl, Function<String, ResponseWrapper> requester) {
        try {
            final JsonElement jsonElement = JsonElement.read(MAPPER, json);

            final JsonElement playerElement;
            if (jsonElement.isArray()) {
                playerElement = jsonElement.getAtIndex(2).path("player");
            } else {
                playerElement = jsonElement.firstElementFor("player");
            }

            AtomicReference<String> scriptUrl = new AtomicReference<>(null);

            final JsonElement streamingData;

            if (playerElement != null) {

                final JsonElement args = playerElement.path("args");
                if (playerElement.isPresent() && args.isPresent()) {
                    scriptUrl.set(playerElement.path("assets").fieldAsString("js"));

                    streamingData = args.path("player_response")
                            .reRead(MAPPER)
                            .path("streamingData");
                } else {
                    streamingData = jsonElement.getAtIndex(2)
                            .path("playerResponse", "streamingData");
                }

            } else {
                streamingData = jsonElement.path("playerResponse", "streamingData");
            }

            final JsonElement formatsElement = streamingData.path("formats");
            final Stream<YouTubeTrackFormat> formats = streamingData.path("formats").isPresent() ?
                    getFormatsFromStream(formatsElement.arrayElements()) : Stream.empty();

            final Stream<JsonElement> adaptiveFormatsStream = streamingData.path("adaptiveFormats").arrayElements();
            final Stream<YouTubeTrackFormat> adaptiveFormats = getFormatsFromStream(adaptiveFormatsStream);

            final List<YouTubeTrackFormat> trackFormats = Stream.concat(formats, adaptiveFormats).collect(Collectors.toList());

            if (trackFormats.stream().anyMatch(YouTubeTrackFormat::streamNotReady) && scriptUrl.get() == null) {
                log.trace("Try to get player script trough embedded URL");
                final String embeddedUrl = trackUrl.replace("youtube.com/", "youtube.com/embed/");
                final String embeddedPageContent = requester.apply(embeddedUrl).getContent();
                if (embeddedPageContent != null) {
                    final Matcher matcher = EMBEDDED_PLAYER_SCRIPT_PATTERN.matcher(embeddedPageContent);
                    if (matcher.find()) {
                        log.trace("Found player script in embedded URL: '{}'", embeddedUrl);
                        scriptUrl.set(matcher.group(1));
                    }
                }
            }

            return new YouTubeTrackInfo(trackFormats, scriptUrl.get());

        } catch (JsonProcessingException e) {
            log.error("Error parsing Youtube info JSON: {}", e.getMessage());
            return null;
        }
    }

    private Stream<YouTubeTrackFormat> getFormatsFromStream(final Stream<JsonElement> formats) {
        return formats.map(format -> {
            final String mimeType = format.fieldAsString("mimeType");
            final FormatType formatType = FormatType.getFormatType(mimeType);
            final String audioQuality = format.fieldAsString("audioQuality");
            final String audioSampleRate = format.fieldAsString("audioSampleRate");

            final JsonElement cipherElement = format.path("cipher")
                    .orElse(format)
                    .path("signatureCipher");

            if (cipherElement.isNull()) {
                final String url = format.fieldAsString("url");
                return YouTubeTrackFormat.builder()
                        .mimeType(mimeType)
                        .formatType(formatType)
                        .audioQuality(audioQuality)
                        .audioSampleRate(audioSampleRate)
                        .streamReady(true)
                        .url(url)
                        .build();
            } else {
                final String cipher = cipherElement.fieldAsString();
                final Map<String, String> params = URLUtility.splitParamsAndDecode(cipher);
                return YouTubeTrackFormat.builder()
                        .mimeType(mimeType)
                        .formatType(formatType)
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

    private Map<String, SignaturePart.SignatureOccurrence> getObfuscateFunctionDefinitions(final String scriptPart) {
        final HashMap<String, SignaturePart.SignatureOccurrence> obfuscateFunctionsDefinitios = new HashMap<>();
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

            obfuscateFunctionsDefinitios.put(functionName, occurrence);
        }
        return obfuscateFunctionsDefinitios;
    }

    private String getFunctionName(final String wholeFunction) {
        final String[] split = wholeFunction.split(":function");
        return split.length > 0 ? split[0] : null;
    }

    protected String getSignature(final YouTubeTrackFormat youtubeTrackFormat, String scriptUrl, final String scriptBody)
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

    @Value
    private static class SignaturePart {

        String functionName;
        SignatureOccurrence occurrence;
        Integer parameter;

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
                final Integer parameter = signaturePart.getParameter();
                switch (signaturePart.getOccurrence()) {
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
