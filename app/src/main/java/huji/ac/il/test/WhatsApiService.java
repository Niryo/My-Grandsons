package huji.ac.il.test;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import net.sumppen.whatsapi4j.MessageProcessor;
import net.sumppen.whatsapi4j.WhatsApi;

public class WhatsApiService extends IntentService {
    private WhatsApi wa;

    public WhatsApiService() {
        super("WhatsApiService");
    }


    private void connectAndLogin() {
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
    }

    private void pollMessage() {
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


    @Override
    protected void onHandleIntent(Intent intent) {
        if ("START_WHATSAPP_SERVICE".equals(intent.getStringExtra("command"))) {
            connectAndLogin();
            while (true) {
                Log.w("customMsg", "pulling!");
                pollMessage();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else if ("POLL".equals(intent.getStringExtra("command"))) {
            Log.w("customMsg", "service got command to pull messages");
            connectAndLogin();
            pollMessage();
        }
    }

}


