package huji.ac.il.test;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.Toast;

import net.sumppen.whatsapi4j.WhatsApi;

/**
 * Receive the boot-complete action and set the wakeup alarm
 */
public class BootReceiver extends BroadcastReceiver {
    public final int TIME_INTERVAL= 900000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent pollIntent= new Intent(context, WhatsApiService.class);
            pollIntent.putExtra(WhatsApiService.ACTION, WhatsApiService.ACTION_WAKE_AND_POLL);
            PendingIntent operation = PendingIntent.getService(context, 0, pollIntent, 0);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+TIME_INTERVAL, TIME_INTERVAL, operation);
        }
    }
}
