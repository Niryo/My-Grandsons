package niry.mygrandsons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Nir on 05/04/2015.
 */


    public  class PollMessagesWhileAsleep extends BroadcastReceiver {

        public PollMessagesWhileAsleep(){
            super();
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if("POLL".equals(intent.getAction())) {
               Intent pollIntent=new Intent(context, WhatsApiService.class);
                pollIntent.putExtra("command","WAKE_AND_POLL");
                context.startService(pollIntent);
            }
        }
    }

