package huji.ac.il.test;

import android.app.IntentService;
import android.content.Intent;

import net.sumppen.whatsapi4j.MessagePoller;


public class MessagePollerService extends IntentService {

    public MessagePollerService(String name) {
        super(name);
    }

    @Override


    @Override
    protected void onHandleIntent(Intent intent) {
        MessagePoller poller = new MessagePoller(this);
    }
}
