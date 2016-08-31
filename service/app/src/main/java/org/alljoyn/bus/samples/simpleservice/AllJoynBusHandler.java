package org.alljoyn.bus.samples.simpleservice;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SessionPortListener;
import org.alljoyn.bus.Status;

/* This class will handle all AllJoyn calls. See onCreate(). */
class AllJoynBusHandler extends Handler {

    private static final String TAG = AllJoynBusHandler.class.getName();

    /*
     * Name used as the well-known name and the advertised name.  This name must be a unique name
     * both to the bus and to the network as a whole.  The name uses reverse URL style of naming.
     */
    private static final String SERVICE_NAME = "org.alljoyn.bus.samples.simple";
    private BusAttachment mBus;
    private Context mContext;
    private String mPackageName;
    private Handler mUIHandler;

    /* The AllJoyn object that is our service. */
    private SimpleService mSimpleService;

    public AllJoynBusHandler(final Looper looper,
                             final Context context,
                             final String packageName,
                             final Handler uiHandler) {
        super(looper);
        mContext = context;
        mPackageName = packageName;
        mUIHandler = uiHandler;

        /* Start our service. */
        mSimpleService = new SimpleService(this, mContext);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            /* Connect to the bus and start our service. */
            case Constants.CONNECT: {
                org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(mContext);
                /*
                 * All communication through AllJoyn begins with a BusAttachment.
                 *
                 * A BusAttachment needs a name. The actual name is unimportant except for internal
                 * security. As a default we use the class name as the name.
                 *
                 * By default AllJoyn does not allow communication between devices (i.e. bus to bus
                 * communication).  The second argument must be set to Receive to allow
                 * communication between devices.
                 */
                mBus = new BusAttachment(mPackageName, BusAttachment.RemoteMessage.Receive);

                /*
                 * Create a bus listener class
                 */
                mBus.registerBusListener(new BusListener());

                /*
                 * To make a service available to other AllJoyn peers, first register a BusObject with
                 * the BusAttachment at a specific path.
                 *
                 * Our service is the SimpleService BusObject at the "/SimpleService" path.
                 */
                Status status = mBus.registerBusObject(mSimpleService, "/SimpleService");
                logStatus("BusAttachment.registerBusObject()", status);
                if (status != Status.OK) {
                    sendUiMessage(Constants.FINISH, msg.obj);
                    return;
                }

                /*
                 * The next step in making a service available to other AllJoyn peers is to connect the
                 * BusAttachment to the bus with a well-known name.
                 */
                /*
                 * connect the BusAttachement to the bus
                 */
                status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (status != Status.OK) {
                    sendUiMessage(Constants.FINISH, msg.obj);
                    return;
                }

                /*
                 *  We register our signal handler which is implemented inside the SimpleService
                 */
                status = mBus.registerSignalHandlers(mSimpleService);
                if (status != Status.OK) {
                    Log.i(TAG, "Problem while registering signal handler");
                    return;
                }

                /*
                 * Create a new session listening on the contact port of the chat service.
                 */
                Mutable.ShortValue contactPort = new Mutable.ShortValue(Constants.CONTACT_PORT);

                SessionOpts sessionOpts = new SessionOpts();
                sessionOpts.traffic = SessionOpts.TRAFFIC_MESSAGES;
                sessionOpts.isMultipoint = false;
                sessionOpts.proximity = SessionOpts.PROXIMITY_ANY;
                sessionOpts.transports = SessionOpts.TRANSPORT_ANY;

                status = mBus.bindSessionPort(contactPort, sessionOpts, new SessionPortListener() {
                    @Override
                    public boolean acceptSessionJoiner(short sessionPort, String joiner, SessionOpts sessionOpts) {
                        if (sessionPort == Constants.CONTACT_PORT) {
                            return true;
                        } else {
                            return false;
                        }
                    }

                    public void sessionJoined(short sessionPort, int id, String joiner) {}
                });
                logStatus(String.format("BusAttachment.bindSessionPort(%d, %s)",
                        contactPort.value, sessionOpts.toString()), status);
                if (status != Status.OK) {
                    sendUiMessage(Constants.FINISH, msg.obj);
                    return;
                }

                /*
                 * request a well-known name from the bus
                 */
                int flag = BusAttachment.ALLJOYN_REQUESTNAME_FLAG_REPLACE_EXISTING | BusAttachment.ALLJOYN_REQUESTNAME_FLAG_DO_NOT_QUEUE;

                status = mBus.requestName(SERVICE_NAME, flag);
                logStatus(String.format("BusAttachment.requestName(%s, 0x%08x)", SERVICE_NAME, flag), status);
                if (status == Status.OK) {
                    /*
                     * If we successfully obtain a well-known name from the bus
                     * advertise the same well-known name
                     */
                    status = mBus.advertiseName(SERVICE_NAME, sessionOpts.transports);
                    logStatus(String.format("BusAttachement.advertiseName(%s)", SERVICE_NAME), status);
                    if (status != Status.OK) {
                        /*
                         * If we are unable to advertise the name, release
                         * the well-known name from the local bus.
                         */
                        status = mBus.releaseName(SERVICE_NAME);
                        logStatus(String.format("BusAttachment.releaseName(%s)", SERVICE_NAME), status);
                        sendUiMessage(Constants.FINISH, msg.obj);
                        return;
                    }
                }

                break;
            }

            /* Release all resources acquired in connect. */
            case Constants.DISCONNECT: {
                /*
                 * It is important to unregister the BusObject before disconnecting from the bus.
                 * Failing to do so could result in a resource leak.
                 */
                mBus.unregisterBusObject(mSimpleService);
                mBus.disconnect();
                this.getLooper().quit();
                break;
            }default:
                break;
        }
    }

    private void logStatus(String msg, Status status) {
        String log = String.format("%s: %s", msg, status);
        if (status == Status.OK) {
            Util.logInfo(TAG, log);
        } else {
            Message toastMsg = mUIHandler.obtainMessage(Constants.MESSAGE_POST_TOAST, log);
            mUIHandler.sendMessage(toastMsg);
            Log.e(TAG, log);
        }
    }

    /* Helper function to send a message to the UI thread. */
    public void sendUiMessage(int what, Object obj) {
        mUIHandler.sendMessage(mUIHandler.obtainMessage(what, obj));
    }
}
