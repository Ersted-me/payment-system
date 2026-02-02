package com.ersted.utils;

import lombok.experimental.UtilityClass;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@UtilityClass
public class DateTimeUtils {

    public static OffsetDateTime offsetDateTimeFromLong(long milliDateTime) {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(milliDateTime), ZoneOffset.UTC);
    }

}
