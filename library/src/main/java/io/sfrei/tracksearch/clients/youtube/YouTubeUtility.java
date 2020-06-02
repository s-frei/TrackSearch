package io.sfrei.tracksearch.clients.youtube;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sfrei.tracksearch.clients.setup.QueryType;
import io.sfrei.tracksearch.exceptions.YouTubeException;
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;
import io.sfrei.tracksearch.utils.URLUtility;
import io.sfrei.tracksearch.utils.json.JsonElement;
import io.sfrei.tracksearch.utils.json.JsonUtility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
class YouTubeUtility {

    private static final String YOUTUBE_HOSTNAME = "youtube.com";

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String JS_SPLICE = ".splice";
    private static final String JS_SLICE = ".slice";
    private static final String JS_REVERSE = ".reverse";
    private static final String FUNCTION_CALL = "([a-zA-Z]+.([a-zA-Z0-9]+)\\(a,([0-9]+)\\);)";
    private static final Pattern FUNCTION_CALL_PATTERN = Pattern.compile(FUNCTION_CALL);
    private static final Pattern OBFUSCATE_FUNCTIONS_CALLS_PATTERN = Pattern.compile(
            "function\\(a\\)\\{a=a\\.split\\(\"\"\\);" + FUNCTION_CALL + "+"
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

    private static String wrap(String functionContent) {
        return "(" + VAR_NAME + ":function" + functionContent + FUNCTION_END + ")";
    }

    private static final String [] defaultRoute = {"contents", "twoColumnSearchResultsRenderer", "primaryContents",
    "sectionListRenderer", "contents"};

    protected BaseTrackList<YouTubeTrack> getYouTubeTracks(String json, QueryType queryType, String query) throws YouTubeException {
        try {
            JsonNode response = MAPPER.readTree(json).get(1).get("response");
            JsonElement responseElement = new JsonElement(response);

            JsonElement defaultElement = responseElement.get(defaultRoute);

            //JSON list named "contents" (could be several)
            JsonElement contentHolder;
            if (defaultElement.notNull()) {
                contentHolder = defaultElement.getFirst().get("itemSectionRenderer");
            } else {
                JsonElement pagedElements = responseElement.get("continuationContents");
                JsonElement pagedContentHolder;
                pagedContentHolder = pagedElements.get("itemSectionContinuation");
                if (pagedContentHolder.notNull())
                    contentHolder = pagedContentHolder;
                else
                    contentHolder = pagedElements.get("sectionListContinuation", "contents").getFirst().get("itemSectionRenderer");
            }

            String ctoken;
            if (contentHolder.get("continuations").notNull()) {
                ctoken = contentHolder.get("continuations").getFirst().get("nextContinuationData")
                        .getStringFor("continuation");
            } else {
                ctoken = defaultElement.get(1).get("continuationItemRenderer", "continuationEndpoint", "continuationCommand")
                        .getStringFor("token");
            }

            JsonNode contents = contentHolder.get("contents").getNode();
            if (contents == null)
                throw new YouTubeException("Content section for tracks is empty");

            Iterator<JsonNode> elements = contents.elements();
            List<YouTubeTrack> ytTracks = new ArrayList<>();
            while (elements.hasNext()) {
                String jsonObject = elements.next().toString();
                YouTubeTrack youTubeTrack = MAPPER.readValue(jsonObject, YouTubeTrack.class);
                if (youTubeTrack != null) ytTracks.add(youTubeTrack);
            }

            int foundTracks = ytTracks.size();

            Map<String, String> queryInformation = YouTubeClient.makeQueryInformation(query, ctoken);
            BaseTrackList<YouTubeTrack> trackList = new BaseTrackList<>(ytTracks, queryType, queryInformation);
            trackList.setQueryInformationValue(YouTubeClient.OFFSET_KEY, foundTracks);
            log.debug("Found {} YouTube Tracks", foundTracks);
            return trackList;
        } catch (IOException e) {
            throw new YouTubeException("GetYouTubeTubeTracks - " + e.getMessage());
        }
    }

    public YouTubeTrackInfo getTrackInfo(String json) {
        try {
            AtomicReference<String> scriptUrl = new AtomicReference<>(null);
            List<YouTubeTrackFormat> trackFormats = new ArrayList<>();

            JsonNode jsonNode = MAPPER.readTree(json);
            List<JsonNode> playerNodes = jsonNode.findValues("player");
            for (JsonNode node : playerNodes) {
                JsonNode assets = node.get("assets");
                JsonNode args = node.get("args");
                if (assets != null) {
                    scriptUrl.set(JsonUtility.getStringFor(assets, "js"));
                }
                if (args != null) {
                    JsonNode playerResponseTextNode = args.get("player_response");
                    JsonNode playerResponseNode = MAPPER.readTree(playerResponseTextNode.textValue()); //re-read

                    JsonNode formats = playerResponseNode.get("streamingData").get("adaptiveFormats");
                    for (JsonNode format : formats) {

                        if (!JsonUtility.getStringForContaining(format, "mimeType", "audio"))
                            continue;

                        JsonElement formatElement = new JsonElement(format);
                        String mimeType = formatElement.getStringFor("mimeType");
                        String audioQuality = formatElement.getStringFor("audioQuality");
                        String audioSampleRate = formatElement.getStringFor("audioSampleRate");

                        String cipher = formatElement.getStringForValues("cipher", "signatureCipher");

                        YouTubeTrackFormat trackFormat;
                        if (cipher == null) {
                            String url = formatElement.getStringFor("url");
                            trackFormat = YouTubeTrackFormat.builder()
                                    .mimeType(mimeType)
                                    .audioQuality(audioQuality)
                                    .audioSampleRate(audioSampleRate)
                                    .streamReady(true)
                                    .url(url)
                                    .sigParam(null)
                                    .sigValue(null)
                                    .build();
                        } else {
                            Map<String, String> params = URLUtility.splitAndDecodeUrl(cipher);
                            trackFormat = YouTubeTrackFormat.builder()
                                    .mimeType(mimeType)
                                    .audioQuality(audioQuality)
                                    .audioSampleRate(audioSampleRate)
                                    .streamReady(false)
                                    .url(params.get("url"))
                                    .sigParam(params.getOrDefault("sp", "sig"))
                                    .sigValue(params.get("s"))
                                    .build();
                        }
                        trackFormats.add(trackFormat);
                    }
                }
            }
            return new YouTubeTrackInfo(trackFormats, scriptUrl.get());
        } catch (JsonProcessingException e) {
            log.error("Error parsing Youtube track JSON: " + e.getMessage());
            return null;
        }
    }

    private List<SignaturePart> getObfuscateFunctionDefinitions(String scriptPart) {
        List<SignaturePart> obfuscateFunctionDefinitions = new ArrayList<>();
        String[] functions = scriptPart.split("\n");
        for (String function : functions) {
            String functionName = getFunctionName(function);
            if (functionName == null)
                continue;

            SignaturePart obfuscateFunctionDefinition = SignaturePart.builder().functionName(functionName).build();
            if (function.contains(JS_SLICE))
                obfuscateFunctionDefinition.setOccurrence(SignaturePart.SignatureOccurrence.SLICE);
            else if (function.contains(JS_SPLICE))
                obfuscateFunctionDefinition.setOccurrence(SignaturePart.SignatureOccurrence.SPLICE);
            else if (function.contains(JS_REVERSE))
                obfuscateFunctionDefinition.setOccurrence(SignaturePart.SignatureOccurrence.REVERSE);
            else
                obfuscateFunctionDefinition.setOccurrence(SignaturePart.SignatureOccurrence.SWAP);

            obfuscateFunctionDefinitions.add(obfuscateFunctionDefinition);
        }
        return obfuscateFunctionDefinitions;
    }

    private String getFunctionName(String wholeFunction) {
        String[] split = wholeFunction.split(":function");
        return split.length > 0 ? split[0] : null;
    }

    private SignaturePart getSignaturePart(List<SignaturePart> parts, String functionName, Integer parameter) {
        for (SignaturePart part : parts) {
            if (part.getFunctionName().equals(functionName))
                return new SignaturePart(part.getFunctionName(), part.getOccurrence(), parameter);
        }
        return null;
    }

    public String getSignature(YouTubeTrackFormat youtubeTrackFormat, String scriptBody) throws YouTubeException {

        Matcher obfuscateFunctionsMatcher = OBFUSCATE_FUNCTIONS_PATTERN.matcher(scriptBody);
        if (!obfuscateFunctionsMatcher.find())
            throw new YouTubeException("Was not able to find obfuscate functions");

        String obfuscateFunctions = obfuscateFunctionsMatcher.group(1);
        List<SignaturePart> obfuscateFunctionDefinitions = getObfuscateFunctionDefinitions(obfuscateFunctions);

        Matcher obfuscateFunctionsCallsMatcher = OBFUSCATE_FUNCTIONS_CALLS_PATTERN.matcher(scriptBody);
        if (!obfuscateFunctionsCallsMatcher.find())
            throw new YouTubeException("Was not able to find obfuscate functions calls");

        Matcher obfuscateFunctionCallsMatcher = FUNCTION_CALL_PATTERN.matcher(obfuscateFunctionsCallsMatcher.group());

        SignatureResolver signatureResolver = new SignatureResolver();
        while (obfuscateFunctionCallsMatcher.find()) {
            String obfuscateFunctionName = obfuscateFunctionCallsMatcher.group(2);
            Integer obfuscateFunctionParameter = Integer.parseInt(obfuscateFunctionCallsMatcher.group(3));

            SignaturePart signaturePart = getSignaturePart(obfuscateFunctionDefinitions, obfuscateFunctionName, obfuscateFunctionParameter);
            if (signaturePart != null)
                signatureResolver.addSignaturePart(signaturePart);
        }

        return signatureResolver.resolveSignature(youtubeTrackFormat.getSigValue());
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class SignaturePart {

        private final String functionName;
        private SignatureOccurrence occurrence;
        private Integer parameter;

        public enum SignatureOccurrence {
            SLICE, SPLICE, REVERSE, SWAP
        }
    }

    private static class SignatureResolver {

        private final List<SignaturePart> signatureParts;

        public SignatureResolver() {
            this.signatureParts = new ArrayList<>();
        }

        private void addSignaturePart(SignaturePart signaturePart) {
            signatureParts.add(signaturePart);
        }

        private String resolveSignature(String signatureValue) {
            StringBuilder signature = new StringBuilder(signatureValue);
            for (SignaturePart signaturePart : signatureParts) {
                Integer parameter = signaturePart.getParameter();
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
                }
            }
            return signature.toString();
        }
    }

}
