package huji.ac.il.test;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;


import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


import net.sumppen.whatsapi4j.MessageProcessor;
import net.sumppen.whatsapi4j.ProtocolNode;

public class myMessageProcessor implements MessageProcessor {

    private class DownloadImageTask extends AsyncTask<String, Void, Void> {

        private Context context;
        public DownloadImageTask(Context context){
                this.context=context;
        }


        @Override
        protected Void doInBackground(String... urls) {
            String url = urls[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                System.out.println("Failed downloading image");
                e.printStackTrace();
            }
            System.out.println("success downloading image!");
            try{
                System.out.println("now trying to save the file");
                Calendar calendar = new GregorianCalendar();
                SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
                File rootDir = new File(context.getExternalFilesDir(null).getAbsolutePath()+File.separator + "savedFiles");
                if( !rootDir.exists()){//todo: move this check to the main activity
                    rootDir.mkdir();
                }

                FileOutputStream out=null;
                out = new FileOutputStream(new File(rootDir, sdf.format(calendar.getTime()) + ".jpg"));
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                out.close();
                System.out.println("success saving to file");
                Intent new_intent = new Intent();
                new_intent.setAction("CUSTOM_INCOMING_MESSAGE");
                context.sendBroadcast(new_intent);


            }catch(Exception e){
                System.out.println("failed saving image");
                e.printStackTrace();
            }
            return null;
        }

    }

    private class DownloadVideoTask extends AsyncTask<String, Void ,Void> {
        private Context context;

        public DownloadVideoTask(Context context){
            this.context=context;
        }



        @Override
        protected Void doInBackground(String... url) {
            try {
                File rootDir = new File(context.getExternalFilesDir(null).getAbsolutePath() + File.separator + "savedFiles");
                if (!rootDir.exists()) {
                    rootDir.mkdir();
                }

                URL address = new URL(url[0]);
                HttpURLConnection connection = (HttpURLConnection) address.openConnection();
                connection.setRequestMethod("GET");
                connection.setDoOutput(true);
                connection.connect();
                Calendar calendar = new GregorianCalendar();
                SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
                FileOutputStream out = new FileOutputStream(new File(rootDir,
                        sdf.format(calendar.getTime()) + ".mp4"));
                InputStream in = connection.getInputStream();
                byte[] buffer = new byte[1024];
                int len1 = 0;

                while ((len1 = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len1);
                }
                out.close();
                System.out.println("success saving video!");
                Intent new_intent = new Intent();
                new_intent.setAction("CUSTOM_INCOMING_MESSAGE");
                context.sendBroadcast(new_intent);

            } catch (Exception e) {

                System.out.println("failed downloading video");
            }
            return null;
        }
    }
    private final Context context;
    public myMessageProcessor(Context context){
       this.context= context;
    }

	public void processMessage(ProtocolNode message) {
		String from = message.getAttribute("from");
		if(message.getAttribute("type").equals("text")) {
			ProtocolNode body = message.getChild("body");
			String hex = new String(body.getData());
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

                new DownloadImageTask(context).execute(media.getAttribute("url"));

				String caption = media.getAttribute("caption");
				if(caption == null)
					caption = "";
				String pathname = "preview-image-"+(new Date().getTime())+".jpg";
				System.out.println(from+" ::: "+caption+"(image): "+media.getAttribute("url"));

			} else if (type.equals("video")) {
                new DownloadVideoTask(context).execute(media.getAttribute("url"));
				String caption = media.getAttribute("caption");
				if(caption == null)
					caption = "";
				String pathname = "preview-video-"+(new Date().getTime())+".jpg";
				System.out.println(from+" ::: "+caption+"(video): "+media.getAttribute("url"));

			} else {
				System.out.println(from+" ::: media/"+type);
			}
			
		}
	}

	private void writePreview(String pathname, byte[] preview) {
		File path = new File(pathname);
//		try {
//			Files.write(preview, path); todo save file

//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		System.out.println("Preview: "+path.getAbsolutePath());
	}

}
