package org.alljoyn.bus.samples.simpleclient;


import android.util.Log;

public class Util {

    /*
    * print the status or result to the Android log. If the result is the expected
    * result only print it to the log.  Otherwise print it to the error log and
    * Sent a Toast to the users screen.
    */
    public static void logInfo(final String tag,
                               final String msg) {
        Log.i(tag, msg);
    }
}
