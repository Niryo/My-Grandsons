package huji.ac.il.test;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;

import net.sumppen.whatsapi4j.MessageProcessor;
import net.sumppen.whatsapi4j.WhatsApi;

/**
 * Created by Nir on 10/04/2015.
 */
public class WhatsApiService extends Service {
    public static final String ACTION = "ACTION";
    public static final String ACTION_KILL= "ACTION_KILL";
    public static final String ACTION_START= "ACTION_START";
    public static final String ACTION_FAST_POLL= "ACTION_FAST_POLL";
    public static final String ACTION_WAKE_AND_POLL= "ACTION_WAKE_AND_POLL";

    private boolean running=true;
    private WhatsApi wa;


    private void connectAndLogin() {

        SharedPreferences preferences = getApplicationContext().getSharedPreferences("information", MODE_PRIVATE);
        String username = preferences.getString("phone_number", "0");
        String identity = "myGrandsons";
        String nickname = "myGrandsons";

        try {
            wa = new WhatsApi(username, identity, nickname, getApplicationContext());
            MessageProcessor mp = new myMessageProcessor(getApplicationContext());
            wa.setNewMessageBind(mp);
        } catch (Exception e) {
            Log.w("customMsg", "can't create whatsApi object!");
            e.printStackTrace();
        }

        //============ login===============


        try {
            wa.connect();
            Log.w("customMsg", "connect success!");

        } catch (Exception e) {

            e.printStackTrace();
        }


        try {
//            wa.loginWithPassword("17+gaHU/Pa6VVGUgqkxQRtI/t+g=");
            String password= preferences.getString("password", "0");
            wa.loginWithPassword(password);
            Log.w("customMsg", "login success!");


        } catch (Exception e) {
            Log.w("customMsg", "login failed!");
            //todo: print connection problem
            e.printStackTrace();

        }
    }

    private void pollMessage() {
        Log.w("customMsg", "pulling!");
        try {
            wa.pollMessages();
            wa.pollMessages();
            wa.sendPresence();
        } catch (Exception e) {
            Log.w("customMsg", "pulling error!");
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.w("customMsg", "service destroyed");

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        final int startIdCopy=startId;
        if ("START_WHATSAPP_SERVICE".equals(intent.getStringExtra("command"))) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectAndLogin();
                    while(running) {
                        pollMessage();
                        try {
                            Thread.sleep(15000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    Log.w("customMsg", "ending pulling loop");
                    stopSelf();
                }
            }).start();

        }else if("FAST_POLL".equals(intent.getStringExtra("command"))){
            Log.w("customMsg", "Fast poll");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    pollMessage();
                }
            }).start();

        } else if("KILL".equals(intent.getStringExtra("command"))){
            Log.w("customMsg", "killing the server");
            this.running=false;
            stopSelf();
        } else if("WAKE_AND_POLL".equals(intent.getStringExtra("command"))){
            Log.w("customMsg", "WAKE AND POLL");
            new Thread(new Runnable() {
                @Override
                public void run() {
                    connectAndLogin();
                    pollMessage();
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    stopSelf();
                }
            }).start();
        }


        return START_NOT_STICKY;

    }
}
