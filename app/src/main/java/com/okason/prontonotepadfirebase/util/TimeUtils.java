package com.okason.prontonotepadfirebase.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Valentine on 2/9/2016.
 */
public class TimeUtils {

    private static final int SECOND_MILLIS = 1000;
    private static final int MINUTE_MILLIS = 60 * SECOND_MILLIS;
    private static final int HOUR_MILLIS = 60 * MINUTE_MILLIS;
    private static final int DAY_MILLIS = 24 * HOUR_MILLIS;


    public static String getTimeAgo(long time) {
        if (time < 1000000000000L) {
            // if timestamp given in seconds, convert to millis
            time *= 1000;
        }

        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return null;
        }

        // TODO: localize
        final long diff = now - time;
        if (diff < MINUTE_MILLIS) {
            return "just now";
        } else if (diff < 2 * MINUTE_MILLIS) {
            return "a minute ago";
        } else if (diff < 50 * MINUTE_MILLIS) {
            return diff / MINUTE_MILLIS + " minutes ago";
        } else if (diff < 90 * MINUTE_MILLIS) {
            return "an hour ago";
        } else if (diff < 24 * HOUR_MILLIS) {
            return diff / HOUR_MILLIS + " hours ago";
        } else if (diff < 48 * HOUR_MILLIS) {
            return "yesterday";
        } else {
            return diff / DAY_MILLIS + " days ago";
        }
    }


    public static String getReadableModifiedDate(long date){

        String displayDate = new SimpleDateFormat("MMM dd, yyyy - h:mm a").format(new Date(date));
        return displayDate;
    }


    public static String getDueDate(long date){

        String displayDate = new SimpleDateFormat("MMM dd, yyyy").format(new Date(date));
        return displayDate;
    }

    public static String getDatetimeSuffix(long date){
        String timeStamp = new SimpleDateFormat("yyyy_MMM_dd_HH_mm").format(new Date(date));
        return timeStamp;
    }


}
