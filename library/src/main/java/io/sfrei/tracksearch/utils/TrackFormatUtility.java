package io.sfrei.tracksearch.utils;

import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackFormat;
import io.sfrei.tracksearch.tracks.metadata.YouTubeTrackInfo;

import java.util.concurrent.atomic.AtomicReference;

public class TrackFormatUtility {

    public static YouTubeTrackFormat getBestTrackFormat(YouTubeTrackInfo youtubeTrackInfo) {
        AtomicReference<YouTubeTrackFormat> format = new AtomicReference<>(null);
        for (YouTubeTrackFormat trackFormat : youtubeTrackInfo.getFormats()) {
            if (format.get() == null)
                format.set(trackFormat);

            String currentAudioQuality = format.get().getAudioQuality();
            String anotherAudioQuality = trackFormat.getAudioQuality();
            if (YoutubeAudioQualities.audioQualityBetter(currentAudioQuality, anotherAudioQuality)) {
                format.set(trackFormat);
                continue;
            }

            Integer currentSamplerate = Integer.parseInt(format.get().getAudioSampleRate());
            Integer anotherSamplerate = Integer.parseInt(trackFormat.getAudioSampleRate());
            if (currentSamplerate < anotherSamplerate) {
                format.set(trackFormat);
            }
        }
        return format.get();
    }

    private static enum YoutubeAudioQualities {
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
