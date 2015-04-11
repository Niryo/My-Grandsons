package huji.ac.il.test;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;


import java.util.Date;


import net.sumppen.whatsapi4j.MessageProcessor;
import net.sumppen.whatsapi4j.ProtocolNode;

public class myMessageProcessor implements MessageProcessor {

    private final Context context;
    public myMessageProcessor(Context context){
       this.context= context;
    }

	public void processMessage(ProtocolNode message) {
		String from = message.getAttribute("from");
		if(message.getAttribute("type").equals("text")) {
			ProtocolNode body = message.getChild("body");
			String text = new String(body.getData());
            String hex = new String(body.getData());
            if(text.startsWith("##")){
                text=text.substring(2);
                new SaveTextFile(context).execute(text);
            }
			String participant = message.getAttribute("participant");
			if(participant != null && !participant.isEmpty()) {
				//Group message
				System.out.println(participant+"("+from+") ::: "+hex);
			} else {
				//Private message
				System.out.println(from+" ::: "+hex);
			}
		}
		if(message.getAttribute("type").equals("media")) {
			ProtocolNode media = message.getChild("media");
			String type = media.getAttribute("type");
			if(type.equals("location")) {
				System.out.println(from+" ::: ("+media.getAttribute("longitude")+","+media.getAttribute("latitude")+")");
			} else if (type.equals("image")) {

                new SaveImageTask(context).execute(media.getAttribute("url"));

				String caption = media.getAttribute("caption");
				if(caption == null)
					caption = "";
				String pathname = "preview-image-"+(new Date().getTime())+".jpg";
				System.out.println(from+" ::: "+caption+"(image): "+media.getAttribute("url"));

			} else if (type.equals("video")) {
                new SaveVideoTask(context).execute(media.getAttribute("url"));
				String caption = media.getAttribute("caption");
				if(caption == null)
					caption = "";
				String pathname = "preview-video-"+(new Date().getTime())+".jpg";
				System.out.println(from+" ::: "+caption+"(video): "+media.getAttribute("url"));

			} else {
				System.out.println(from+" ::: media/"+type);
			}
			
		}

        pollMoreMessages();
        pollMoreMessages();
	}


    /**
     * Called after downloading one file to check if there are more messeages to be polled.
     */
    private void pollMoreMessages(){
        Intent pollIntent=new Intent(context, WhatsApiService.class);
        pollIntent.putExtra(WhatsApiService.ACTION,WhatsApiService.ACTION_FAST_POLL);
        context.startService(pollIntent);
    }


    private void sendNotification(){
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle("My notification")
                        .setContentText("Hello World!");

        int id = 0;
        Intent resultIntent = new Intent(context, Slideshow.class);
        resultIntent.putExtra("info","NOTIFICATION");
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
// Adds the back stack
//        stackBuilder.addParentStack(ScreenSlideActivity.class);
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
