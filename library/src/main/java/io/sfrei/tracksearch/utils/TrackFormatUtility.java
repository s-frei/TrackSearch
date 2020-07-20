package io.sfrei.tracksearch.utils;

import io.sfrei.tracksearch.exceptions.TrackSearchException;
import io.sfrei.tracksearch.tracks.YouTubeTrack;
import io.sfrei.tracksearch.tracks.metadata.FormatType;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class TrackFormatUtility {

    public static YouTubeTrackFormat getBestTrackFormat(YouTubeTrack youtubeTrack, boolean includeVideo)
            throws TrackSearchException {

        AtomicReference<YouTubeTrackFormat> format = new AtomicReference<>(null);
        for (YouTubeTrackFormat trackFormat : youtubeTrack.getTrackInfo().getFormats()) {
            if (format.get() == null) {
                if (trackFormat.getAudioQuality() == null || trackFormat.getAudioSampleRate() == null)
                    continue;
                if (trackFormat.getFormatType().equals(FormatType.Unknown))
                    continue;
                if (!trackFormat.getFormatType().equals(FormatType.Audio) && !includeVideo)
                    continue;

                format.set(trackFormat);
            }

            String currentAudioQuality = format.get().getAudioQuality();
            String anotherAudioQuality = trackFormat.getAudioQuality();
            if (YoutubeAudioQualities.audioQualityBetter(currentAudioQuality, anotherAudioQuality)) {
                format.set(trackFormat);
                continue;
            }

            int currentSampleRate = Integer.parseInt(format.get().getAudioSampleRate());
            int anotherSampleRate = Integer.parseInt(trackFormat.getAudioSampleRate());
            if (currentSampleRate < anotherSampleRate) {
                format.set(trackFormat);
            }
        }
        if (format.get() != null) {
            return format.get();
        }

        if (!includeVideo) {
            log.warn("No audio mime type found for: {} - {} - trying to get alternative",
                    youtubeTrack.getCleanTitle(), youtubeTrack.getUrl());
            return getBestTrackFormat(youtubeTrack, true);
        }

        throw new TrackSearchException("Could not get applicable track format");
    }

    private enum YoutubeAudioQualities {
        LOW("AUDIO_QUALITY_LOW"),
        MEDIUM("AUDIO_QUALITY_MEDIUM"),
        HIGH("AUDIO_QUALITY_HIGH"); //never seen

        String qualityId;

        YoutubeAudioQualities(String qualityId) {
            this.qualityId = qualityId;
        }

        public String getQualityId() {
            return qualityId;
        }

        private static int getOrdinalForQuality(String qualityId) {
            for (YoutubeAudioQualities quality : YoutubeAudioQualities.values()) {
                if(quality.getQualityId().equals(qualityId))
                    return quality.ordinal();
            }
            return -1;
        }

        public static boolean audioQualityBetter(String current, String other) {
            int currentQuality = YoutubeAudioQualities.getOrdinalForQuality(current);
            int otherQuality = YoutubeAudioQualities.getOrdinalForQuality(other);
            return otherQuality > currentQuality;
        }
    }

}
