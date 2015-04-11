package niry.mygrandsons;


import android.content.Context;
import android.content.Intent;


import net.sumppen.whatsapi4j.MessageProcessor;
import net.sumppen.whatsapi4j.ProtocolNode;

public class MyGrandsonsMessageProcessor implements MessageProcessor {

    private final Context context;
    public MyGrandsonsMessageProcessor(Context context){
       this.context= context;
    }

	public void processMessage(ProtocolNode message) {
		if(message.getAttribute("type").equals("text")) {
			ProtocolNode body = message.getChild("body");
			String text = new String(body.getData());
            String hex = new String(body.getData());
            if(text.startsWith("##")){
                text=text.substring(2);
                new SaveTextFile(context).execute(text);
            }
		}
		if(message.getAttribute("type").equals("media")) {
			ProtocolNode media = message.getChild("media");
			String type = media.getAttribute("type");
			 if (type.equals("image")) {
                new SaveImageTask(context).execute(media.getAttribute("url"));

			} else if (type.equals("video")) {
                new SaveVideoTask(context).execute(media.getAttribute("url"));

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
