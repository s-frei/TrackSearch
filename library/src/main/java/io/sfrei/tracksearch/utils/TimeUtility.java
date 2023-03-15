package io.sfrei.tracksearch.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.time.Duration;
import java.time.LocalTime;

@UtilityClass
public class TimeUtility {

    private static final String TIME_STRING_DEFAULT = "00:00:00";

    public Duration getDurationForTimeString(@NonNull final String time) {

        final char[] defaultTimeStringArray = TIME_STRING_DEFAULT.toCharArray();
        final char[] reverseTimeStringArray = new StringBuffer(time).reverse().toString().toCharArray();

        for (int i = 0; i < reverseTimeStringArray.length; i++) {
            defaultTimeStringArray[defaultTimeStringArray.length - 1 - i] = reverseTimeStringArray[i];
        }

        LocalTime localTime = LocalTime.parse(String.valueOf(defaultTimeStringArray));
        LocalTime midnight = LocalTime.MIDNIGHT;
        return Duration.between(midnight, localTime);
    }

    public Duration getDurationForMilliseconds(final Long milliseconds) {
        if (milliseconds == null)
            return null;

        return Duration.ofMillis(milliseconds);
    }

    public String formatSeconds(Duration duration) {
        final String mmss = String.format("%02d:%02d", duration.toMinutesPart(), duration.toSecondsPart());

        final long hours = duration.toHours();
        return hours > 0 ? String.format("%02d:%s", hours, mmss) : mmss;
    }

}
