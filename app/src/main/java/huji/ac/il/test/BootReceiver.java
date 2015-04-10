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
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent pollIntent= new Intent();
            pollIntent.setAction("POLL");
            PendingIntent operation = PendingIntent.getBroadcast(context, 0, pollIntent, 0);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+900000, 900000, operation);
        }
    }
}
