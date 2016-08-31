/*
 * Copyright AllSeen Alliance. All rights reserved.
 *
 *    Permission to use, copy, modify, and/or distribute this software for any
 *    purpose with or without fee is hereby granted, provided that the above
 *    copyright notice and this permission notice appear in all copies.
 *
 *    THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 *    WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 *    MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
 *    ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 *    WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 *    ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
 *    OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package org.alljoyn.bus.samples.simpleclient;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class Client extends Activity {
    /* Load the native alljoyn_java library. */
    static {
        System.loadLibrary("alljoyn_java");
    }

    private static final String TAG = Client.class.getName();


    private EditText mEditText;
    private Button mButtonPing;
    private Button mButtonMessage;
    private ArrayAdapter<String> mListViewArrayAdapter;
    private ListView mListView;

    /* Handler used to make calls to AllJoyn methods. See onCreate(). */
    private AllJoynBusHandler mBusHandler;
    private ProgressDialog mDialog;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_PING:
                    String ping = (String) msg.obj;
                    mListViewArrayAdapter.add("Ping:  " + ping);
                    break;
                case Constants.MESSAGE_PING_REPLY:
                    String ret = (String) msg.obj;
                    mListViewArrayAdapter.add(/*"Reply:  " + */ret);
                    mEditText.setText("");
                    break;
                case Constants.MESSAGE_POST_TOAST:
                    Toast.makeText(getApplicationContext(), (String) msg.obj, Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_START_PROGRESS_DIALOG:
                    mDialog = ProgressDialog.show(Client.this,
                            "",
                            "Finding Simple Service.\nPlease wait...",
                            true,
                            true);
                    break;
                case Constants.MESSAGE_STOP_PROGRESS_DIALOG:
                    mDialog.dismiss();
                    break;
                case Constants.FINISH:
                    finish();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mListViewArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mListView = (ListView) findViewById(R.id.ListView);
        mListView.setAdapter(mListViewArrayAdapter);

        mEditText = (EditText) findViewById(R.id.EditText);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {

                    /* Call the remote object's Ping method. */
                Message msg = mBusHandler.obtainMessage(Constants.PING,
                        view.getText().toString());
                mBusHandler.sendMessage(msg);
                return true;
            }
        });

        mButtonPing = (Button) findViewById(R.id.ButtonPing);
        mButtonPing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message msg = mBusHandler.obtainMessage(Constants.PING,
                        mEditText.getText().toString());
                mBusHandler.sendMessage(msg);
                mEditText.getText().clear();
            }
        });

        mButtonMessage = (Button) findViewById(R.id.ButtonSignal);
        mButtonMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message msg = mBusHandler.obtainMessage(Constants.MESSAGE,
                        mEditText.getText().toString());
                mBusHandler.sendMessage(msg);
                mEditText.getText().clear();
            }
        });

        /* Make all AllJoyn calls through a separate handler thread to prevent blocking the UI. */
        HandlerThread busThread = new HandlerThread("BusHandler");
        busThread.start();
        mBusHandler = new AllJoynBusHandler(busThread.getLooper(), this, getPackageName(), mHandler);

        /* Connect to an AllJoyn object. */
        mBusHandler.sendEmptyMessage(Constants.CONNECT);
        mHandler.sendEmptyMessage(Constants.MESSAGE_START_PROGRESS_DIALOG);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.quit:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /* Disconnect to prevent resource leaks. */
        mBusHandler.sendEmptyMessage(Constants.DISCONNECT);
    }
}