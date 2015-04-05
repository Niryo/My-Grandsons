package huji.ac.il.test;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;


public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "BootReciever!", Toast.LENGTH_SHORT).show();
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
//            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//            Intent intentPollMsg = new Intent(context, WhatsApiService.class);
//            intentPollMsg.putExtra("command", "LISTEN_TO_MSG");
//            PendingIntent pi = PendingIntent.getService(context, 0, intentPollMsg, 0);
//            am.cancel(pi);
//             am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
//                        SystemClock.elapsedRealtime() + seconds*1000,
//                        seconds*1000, pi);

            Intent intentPollMsg = new Intent(context, WhatsApiService.class);
            intentPollMsg.putExtra("command", "START_WHATSAPP_SERVICE");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            context.startService(intentPollMsg);
        }
    }
}
