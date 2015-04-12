package niry.mygrandsons;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;




/**
 * An abstract class that represents a background file downloader
 */
public abstract class SaveFileAsync extends AsyncTask<String, Void ,Void> {
  private final Context context;
  private final String NOTIFICATION_TITLE = "You got new message!";
  private final String NOTIFICATION_CONTENT = "Click here to open My Grandsons app and see your message";

    public SaveFileAsync(Context context1){
       this.context = context1;
   }

public void sendBroadcastIncomingMessage(){
    Intent intent = new Intent();
    intent.setAction(SlideshowActivity.NEW_INCOMING_MESSAGE);
    context.sendBroadcast(intent);
}

    /**
     * Notify the native Notification Manager
     */
    public void sendNotification(){
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(NOTIFICATION_TITLE)
                        .setContentText(NOTIFICATION_CONTENT);

        int id = 0;
        Intent resultIntent = new Intent(context, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
// Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
// Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(resultPendingIntent);
        builder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
        Uri uri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        builder.setSound(uri);

        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(id, builder.build());

    }


}
