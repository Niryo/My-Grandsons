package huji.ac.il.test;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
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
import android.os.SystemClock;
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
    private WhatsApi wa;

    // Handler that receives messages from the thread
    private final class StartService implements Runnable {
            private int startId;
            public StartService(int startId){
                this.startId=startId;
            }
        @Override
        public void run() {
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


            try {
                wa.connect();
                Log.w("customMsg", "connect success!");

            } catch (Exception e) {

                e.printStackTrace();
            }


            try {
                wa.loginWithPassword("17+gaHU/Pa6VVGUgqkxQRtI/t+g=");
                Log.w("customMsg", "login success!");


            } catch (Exception e) {
                Log.w("customMsg", "login failed!");
                e.printStackTrace();

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
        }
    }


    private final class PollMessages implements Runnable{
        private int startId;
        public PollMessages(int startId){
            this.startId=startId;
        }
        @Override
        public void run() {
            try {
                Log.w("customMsg", "wake for pulling!");
                wa.pollMessages();
            } catch (Exception e) {
                e.printStackTrace();

            }
            stopSelf(this.startId);

        }
    }





    @Override
    public void onCreate() {

        AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        Intent pollIntent= new Intent();
        pollIntent.setAction("POLL");
        PendingIntent operation = PendingIntent.getBroadcast(getApplicationContext(), 0, pollIntent, 0);
        am.cancel(operation);
        am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+100000, 100000, operation);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


    final int startIdCopy=startId;
        if (intent == null || "START_WHATSAPP_SERVICE".equals(intent.getStringExtra("command"))) {
            Log.w("customMsg", "restarting, intent=null");
            new Thread(new StartService(startId)).start();

        }

        else if ("POLL".equals(intent.getStringExtra("command"))){
            Log.w("customMsg", "service got command to pull messages");
            new Thread(new PollMessages(startId)).start();
        }

//        // If we get killed, after returning from here, restart
        return START_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
    }


}


