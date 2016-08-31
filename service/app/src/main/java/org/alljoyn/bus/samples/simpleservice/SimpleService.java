package org.alljoyn.bus.samples.simpleservice;

import android.content.Context;

import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusObject;
import org.alljoyn.bus.annotation.BusSignalHandler;

/* The class that is our AllJoyn service.  It implements the SimpleInterface. */
class SimpleService implements SimpleInterface, BusObject {

    private AllJoynBusHandler mAllJoynBusHandler;
    private Context mContext;

    public SimpleService(final AllJoynBusHandler allJoynBusHandler,
                         final Context context) {
        mAllJoynBusHandler = allJoynBusHandler;
        mContext = context;
    }


    /*
     * This is the code run when the client makes a call to the Ping method of the
     * SimpleInterface.  This implementation just returns the received String to the caller.
     *
     * This code also prints the string it received from the user and the string it is
     * returning to the user to the screen.
     */
    public String Ping(String inStr) {
        mAllJoynBusHandler.sendUiMessage(Constants.MESSAGE_PING, inStr);

         /* Simply echo the ping message. */
        return /*mContext.getString(R.string.msg_received_successfully) +*/ inStr;
    }

    @BusSignalHandler(iface = "org.alljoyn.bus.samples.simple.SimpleInterface", signal = "SendMessage")
    public void SendMessage(String message) throws BusException {
        mAllJoynBusHandler.sendUiMessage(Constants.MESSAGE_SIGNAL, message);
    }
}
