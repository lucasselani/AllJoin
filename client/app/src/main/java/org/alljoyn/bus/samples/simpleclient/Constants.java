package org.alljoyn.bus.samples.simpleclient;

public class Constants {

    public static final int MESSAGE_PING = 1;
    public static final int MESSAGE_PING_REPLY = 2;
    public static final int MESSAGE_POST_TOAST = 3;
    public static final int MESSAGE_START_PROGRESS_DIALOG = 4;
    public static final int MESSAGE_STOP_PROGRESS_DIALOG = 5;
    public static final int FINISH = 6;

    /* These are the messages sent to the BusHandler from the UI. */
    public static final int CONNECT = 1;
    public static final int JOIN_SESSION = 2;
    public static final int DISCONNECT = 3;
    public static final int PING = 4;
    public static final int MESSAGE = 5;

    public static final short CONTACT_PORT = 42;

}
