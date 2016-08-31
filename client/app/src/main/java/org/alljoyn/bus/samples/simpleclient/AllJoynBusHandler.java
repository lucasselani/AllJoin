package org.alljoyn.bus.samples.simpleclient;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import org.alljoyn.bus.BusAttachment;
import org.alljoyn.bus.BusException;
import org.alljoyn.bus.BusListener;
import org.alljoyn.bus.Mutable;
import org.alljoyn.bus.ProxyBusObject;
import org.alljoyn.bus.SessionListener;
import org.alljoyn.bus.SessionOpts;
import org.alljoyn.bus.SignalEmitter;
import org.alljoyn.bus.Status;

/* This class will handle all AllJoyn calls. See onCreate(). */
class AllJoynBusHandler extends Handler {

    private static String TAG = AllJoynBusHandler.class.getName();

    /*
     * Name used as the well-known name and the advertised name of the service this client is
     * interested in.  This name must be a unique name both to the bus and to the network as a
     * whole.
     *
     * The name uses reverse URL style of naming, and matches the name used by the service.
     */
    private static final String SERVICE_NAME = "org.alljoyn.bus.samples.simple";


    private BusAttachment mBus;
    private ProxyBusObject mProxyObj;
    private SimpleInterface mSimpleInterface;

    private int mSessionId;
    private boolean mIsConnected;
    private boolean mIsStoppingDiscovery;
    private SimpleService mSimpleService;


    private Context mContext = null;
    private String mPackageName = "";
    private Handler mUIHandler = null;

    public AllJoynBusHandler(final Looper looper,
                             final Context context,
                             final String packageName,
                             final Handler clientHandler) {
        super(looper);

        mContext = context;
        mPackageName = packageName;
        mUIHandler = clientHandler;
        mIsConnected = false;
        mIsStoppingDiscovery = false;
        mSimpleService = new SimpleService(this, mContext);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what) {
            /* Connect to a remote instance of an object implementing the SimpleInterface. */
            case Constants.CONNECT: {
                org.alljoyn.bus.alljoyn.DaemonInit.PrepareDaemon(mContext);
                /*
                 * All communication through AllJoyn begins with a BusAttachment.
                 *
                 * A BusAttachment needs a name. The actual name is unimportant except for internal
                 * security. As a default we use the class name as the name.
                 *
                 * By default AllJoyn does not allow communication between devices (i.e. bus to bus
                 * communication). The second argument must be set to Receive to allow communication
                 * between devices.
                 */
                mBus = new BusAttachment(mPackageName, BusAttachment.RemoteMessage.Receive);

                /*
                 * Create a bus listener class
                 */
                mBus.registerBusListener(new BusListener() {
                    @Override
                    public void foundAdvertisedName(String name, short transport, String namePrefix) {
                        Util.logInfo(TAG, String.format("MyBusListener.foundAdvertisedName(%s, 0x%04x, %s)", name, transport, namePrefix));
                        /*
                         * This client will only join the first service that it sees advertising
                         * the indicated well-known name.  If the program is already a member of
                         * a session (i.e. connected to a service) we will not attempt to join
                         * another session.
                         * It is possible to join multiple session however joining multiple
                         * sessions is not shown in this sample.
                         */
                        if (!mIsConnected) {
                            Message msg = obtainMessage(Constants.JOIN_SESSION);
                            msg.arg1 = transport;
                            msg.obj = name;
                            sendMessage(msg);
                        }
                    }
                });

                Status status = mBus.registerBusObject(mSimpleService, "/SimpleInterface");
                if (Status.OK != status) {
                    logStatus("BusAttachment.registerBusObject()", status);
                    return;
                }

                /* To communicate with AllJoyn objects, we must connect the BusAttachment to the bus. */
                status = mBus.connect();
                logStatus("BusAttachment.connect()", status);
                if (Status.OK != status) {
                    sendUiMessage(Constants.FINISH, msg.obj);
                    return;
                }

                /*
                 * Now find an instance of the AllJoyn object we want to call.  We start by looking for
                 * a name, then connecting to the device that is advertising that name.
                 *
                 * In this case, we are looking for the well-known SERVICE_NAME.
                 */
                status = mBus.findAdvertisedName(SERVICE_NAME);
                logStatus(String.format("BusAttachement.findAdvertisedName(%s)", SERVICE_NAME), status);
                if (Status.OK != status) {
                    sendUiMessage(Constants.FINISH, msg.obj);
                    return;
                }

                break;
            }
            case (Constants.JOIN_SESSION): {
                /*
                 * If discovery is currently being stopped don't join to any other sessions.
                 */
                if (mIsStoppingDiscovery) {
                    break;
                }

                /*
                 * In order to join the session, we need to provide the well-known
                 * contact port.  This is pre-arranged between both sides as part
                 * of the definition of the chat service.  As a result of joining
                 * the session, we get a session identifier which we must use to
                 * identify the created session communication channel whenever we
                 * talk to the remote side.
                 */
                short contactPort = Constants.CONTACT_PORT;
                SessionOpts sessionOpts = new SessionOpts();
                sessionOpts.transports = (short) msg.arg1;
                Mutable.IntegerValue sessionId = new Mutable.IntegerValue();

                Status status = mBus.joinSession((String) msg.obj, contactPort, sessionId, sessionOpts, new SessionListener() {
                    @Override
                    public void sessionLost(int sessionId, int reason) {
                        mIsConnected = false;
                        Util.logInfo(TAG, String.format("MyBusListener.sessionLost(sessionId = %d, reason = %d)", sessionId, reason));
                        mUIHandler.sendEmptyMessage(Constants.MESSAGE_START_PROGRESS_DIALOG);
                    }
                });
                logStatus("BusAttachment.joinSession() - sessionId: " + sessionId.value, status);

                if (status == Status.OK) {
                    /*
                     * To communicate with an AllJoyn object, we create a ProxyBusObject.
                     * A ProxyBusObject is composed of a name, path, sessionID and interfaces.
                     *
                     * This ProxyBusObject is located at the well-known SERVICE_NAME, under path
                     * "/SimpleService", uses sessionID of CONTACT_PORT, and implements the SimpleInterface.
                     */
                    mProxyObj = mBus.getProxyBusObject(SERVICE_NAME,
                            "/SimpleService",
                            sessionId.value,
                            new Class<?>[]{SimpleInterface.class});

                    /* We make calls to the methods of the AllJoyn object through one of its interfaces. */
                    mSimpleInterface = mProxyObj.getInterface(SimpleInterface.class);

                    mSessionId = sessionId.value;
                    mIsConnected = true;
                    mUIHandler.sendEmptyMessage(Constants.MESSAGE_STOP_PROGRESS_DIALOG);


                }
                break;
            }

            /* Release all resources acquired in the connect. */
            case Constants.DISCONNECT: {
                mIsStoppingDiscovery = true;
                if (mIsConnected) {
                    Status status = mBus.leaveSession(mSessionId);
                    logStatus("BusAttachment.leaveSession()", status);
                }
                mBus.disconnect();
                getLooper().quit();
                break;
            }

            /*
             * Call the service's Ping method through the ProxyBusObject.
             *
             * This will also print the String that was sent to the service and the String that was
             * received from the service to the user interface.
             */
            case Constants.PING: {
                try {
                    if (mSimpleInterface != null) {
                        sendUiMessage(Constants.MESSAGE_PING, msg.obj);
                        String reply = mSimpleInterface.Ping(msg.obj.toString());
                        sendUiMessage(Constants.MESSAGE_PING_REPLY, "Server response: " + reply);
                    }
                } catch (BusException ex) {
                    logException("SimpleInterface.Ping()", ex);
                }
                break;
            }

            case Constants.MESSAGE:
                    SignalEmitter emitter = new SignalEmitter(mSimpleService, mSessionId, SignalEmitter.GlobalBroadcast.Off);
                    SimpleInterface simpleInterface = emitter.getInterface(SimpleInterface.class);
                    try {
                        simpleInterface.SendMessage(msg.obj.toString());
                    } catch (BusException e) {
                        e.printStackTrace();
                    }
                 break;
            default:
                break;
        }
    }

    /* Helper function to send a message to the UI thread. */
    private void sendUiMessage(int what, Object obj) {
        mUIHandler.sendMessage(mUIHandler.obtainMessage(what, obj));
    }

    private void logStatus(String msg, Status status) {
        String log = String.format("%s: %s", msg, status);
        if (status == Status.OK) {
            Util.logInfo(TAG, log);
        } else {
            Message toastMsg = mUIHandler.obtainMessage(Constants.MESSAGE_POST_TOAST, log);
            mUIHandler.sendMessage(toastMsg);
            Util.logInfo(TAG, log);
        }
    }

    private void logException(String msg, BusException ex) {
        String log = String.format("%s: %s", msg, ex);
        Message toastMsg = mUIHandler.obtainMessage(Constants.MESSAGE_POST_TOAST, log);
        mUIHandler.sendMessage(toastMsg);
        Log.e(TAG, log, ex);
    }

}
