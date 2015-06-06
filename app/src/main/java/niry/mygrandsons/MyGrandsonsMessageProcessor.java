package niry.mygrandsons;


import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;


import net.sumppen.whatsapi4j.MessageProcessor;
import net.sumppen.whatsapi4j.ProtocolNode;

public class MyGrandsonsMessageProcessor implements MessageProcessor {

    private final Context context;
    private Handler   handler;
    public MyGrandsonsMessageProcessor(Context context){
        this.context= context;
       this.handler  = new Handler(context.getMainLooper());
    }

	public void processMessage(ProtocolNode message) {
		if(message.getAttribute("type").equals("text")) {
			ProtocolNode body = message.getChild("body");
			String text = new String(body.getData());
            String hex = new String(body.getData());
            if(text.startsWith("##")){
                text=text.substring(2);
                final String finalText = text;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        new SaveTextFile(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, finalText);
                    }
                });


            }
		}
		if(message.getAttribute("type").equals("media")) {
			final ProtocolNode media = message.getChild("media");
			String type = media.getAttribute("type");
			 if (type.equals("image")) {
                     handler.post(new Runnable() {
                         @Override
                         public void run() {
                             new SaveImageTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, media.getAttribute("url"));
                         }
                     });



			} else if (type.equals("video")) {
                 handler.post(new Runnable() {
                     @Override
                     public void run() {
                         new SaveVideoTask(context).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, media.getAttribute("url"));
                     }
                 });
			}
		}

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


}
