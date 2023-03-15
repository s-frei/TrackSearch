package io.sfrei.tracksearch.utils;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

import java.time.LocalTime;

@UtilityClass
public class TimeUtility {

    private static final String TIME_STRING_DEFAULT = "00:00:00";

    // Input like '1:22:45', '3:45', ...
    public Long getSecondsForTimeString(@NonNull final String time) {

        final char[] defaultTimeStringArray = TIME_STRING_DEFAULT.toCharArray();
        final char[] reverseTimeStringArray = new StringBuffer(time).reverse().toString().toCharArray();

        for (int i = 0; i < reverseTimeStringArray.length; i++) {
            defaultTimeStringArray[defaultTimeStringArray.length - 1 - i] = reverseTimeStringArray[i];
        }

        return Integer.toUnsignedLong(LocalTime.parse(String.valueOf(defaultTimeStringArray)).toSecondOfDay());
    }

    public Long getSecondsForMilliseconds(final Long milliseconds) {
        if (milliseconds == null)
            return null;

        return milliseconds / 1000;
    }

}
