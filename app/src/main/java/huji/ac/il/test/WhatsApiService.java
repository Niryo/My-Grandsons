package huji.ac.il.test;

import android.app.IntentService;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import net.sumppen.whatsapi4j.DecodeException;
import net.sumppen.whatsapi4j.IncompleteMessageException;
import net.sumppen.whatsapi4j.InvalidMessageException;
import net.sumppen.whatsapi4j.InvalidTokenException;
import net.sumppen.whatsapi4j.MessagePoller;
import net.sumppen.whatsapi4j.MessageProcessor;
import net.sumppen.whatsapi4j.WhatsApi;
import net.sumppen.whatsapi4j.WhatsAppException;

import org.json.JSONException;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public class WhatsApiService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private static boolean isRunning = false;


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        private WhatsApi wa;

        public ServiceHandler(Looper looper) {
            super(looper);
            this.wa = wa;
        }

        @Override
        public void handleMessage(Message msg) {
            if (msg.arg2 == 1) {

//                String username = msg.getData().getString("username");
//                String identity = msg.getData().getString("identity");
//                String nickname = msg.getData().getString("nickname");
                String username = "972535059773";
                String identity = "test";
                String nickname = "nir";
                try {
                    wa = new WhatsApi(username, identity, nickname, getApplicationContext());
                    MessageProcessor mp = new myMessageProcessor(getApplicationContext());
                    wa.setNewMessageBind(mp);
                } catch (Exception e) {
                    Log.w("customMsg", "can't create whatsApi object!");
                    e.printStackTrace();
                }

                //============ login===============

                while (true) {
                    try {
                        wa.connect();
                        Log.w("customMsg", "connect success!");
                        break;
                    } catch (Exception e) {
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e1) {
                            Log.w("customMsg", "fatal error!");
                            break;
                        }
                        e.printStackTrace();
                    }
                }

                while (true) {
                    try {
                        wa.loginWithPassword("17+gaHU/Pa6VVGUgqkxQRtI/t+g=");
                        Log.w("customMsg", "login success!");
                        break;

                    } catch (Exception e) {
                        Log.w("customMsg", "login failed!");
                        e.printStackTrace();
                        try {
                            Thread.sleep(3000);
                        } catch (InterruptedException e1) {
                            Log.w("customMsg", "fatal error!");
                            break;
                        }

                    }
                }


                //==============Poll messages==================

                while (true) {

                    Log.w("customMsg", "pulling!");
                    try {
                        wa.pollMessages();
                        wa.sendPresence();
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        Log.w("customMsg", "pulling error!");
                        Log.w("customMsg", e.getCause());
                    }
                }

            } else {
                Log.w("customMsg", "undefiend message!");
                stopSelf(msg.arg1);
            }

        }
    }

    @Override
    public void onCreate() {
        isRunning = true;
        HandlerThread thread = new HandlerThread("ServiceStartArguments");
        thread.start();
        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "service starting", Toast.LENGTH_SHORT).show();
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        if (intent == null || intent.getStringExtra("command").equals("START_WHATSAPP_SERVICE")) {
            msg.arg2 = 1;

        } else {
            msg.arg2 = -1;
        }

        mServiceHandler.sendMessage(msg);
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    public static boolean isServiceRunning() {
        return isRunning;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }


}


