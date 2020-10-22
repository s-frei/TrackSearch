package io.sfrei.tracksearch.utils;

import io.sfrei.tracksearch.config.TrackSearchConfig;
import io.sfrei.tracksearch.tracks.BaseTrackList;
import io.sfrei.tracksearch.tracks.Track;
import io.sfrei.tracksearch.tracks.TrackList;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class TrackListHelper {

    public static <T extends Track> BaseTrackList<T> updatePagingValues(BaseTrackList<T> newTrackList, TrackList<?> oldTrackList,
                                                                        String positionKey, String offsetKey) {

        String oldOffsetValue = oldTrackList.getQueryInformation().get(offsetKey);
        String newOffsetValue = newTrackList.getQueryInformation().get(offsetKey);

        if (oldOffsetValue == null || newOffsetValue == null)
            return newTrackList.setPagingValues(positionKey, 0, offsetKey, 0);

        int newPosition = Integer.parseInt(oldOffsetValue);
        int offset = Integer.parseInt(newOffsetValue);
        int newOffset = newPosition + offset;
        return newTrackList.setPagingValues(positionKey, newPosition, offsetKey, newOffset);
    }

    public static void mergePositionValues(BaseTrackList<? extends Track> trackList, String positionKey, String offsetKey) {
        AtomicInteger position = new AtomicInteger(0);
        AtomicInteger offset = new AtomicInteger(0);

        for (String key : trackList.getQueryInformation().keySet()) {
            if (key.contains(TrackSearchConfig.POSITION_KEY_SUFFIX)) {
                position.getAndUpdate(pos -> pos += trackList.getQueryInformationIntValue(key));
            }
            else if (key.contains(TrackSearchConfig.OFFSET_KEY_SUFFIX)) {
                offset.getAndUpdate(off -> off += trackList.getQueryInformationIntValue(key));
            }
        }
        trackList.setPagingValues(positionKey, position.get(), offsetKey, offset.get());
    }

    public static boolean hasQueryInformation(TrackList<? extends Track> trackList, String... keys) {
        Map<String, String> queryInformation = trackList.getQueryInformation();
        for (String key : keys) {
            if (queryInformation.get(key) == null)
                return false;
        }
        return true;
    }

}
