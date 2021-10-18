package io.sfrei.tracksearch.utils;

import lombok.experimental.UtilityClass;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@UtilityClass
public class TimeUtility {

    private final Date DATE_REFERENCE = new Date(0);
    private final List<String> TIME_FORMATS = Arrays.asList("HH:mm:ss", "mm:ss", "ss");

    public Long getSecondsForTimeString(final String time) {
        for (final String format : TIME_FORMATS) {
            try {
                return (new SimpleDateFormat(format).parse(time).getTime() - DATE_REFERENCE.getTime()) / 1000L;
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public Long getSecondsForMilliseconds(final Long milliseconds) {
        if (milliseconds == null)
            return null;

        return milliseconds / 1000;
    }

}
