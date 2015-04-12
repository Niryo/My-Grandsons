package niry.mygrandsons;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Save video in the background
 */
public class SaveVideoTask extends SaveFileAsync {
    private final Context context;
    public SaveVideoTask(Context context){
        super(context);
        this.context=context;
    }

    @Override
    protected Void doInBackground(String... url) {

        try {
            File rootDir = new File(MainActivity.getSaveFilePath(context));
            URL address = new URL(url[0]);
            HttpURLConnection connection = (HttpURLConnection) address.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.connect();
            //the name of the file will be the current time:
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

            sendBroadcastIncomingMessage();
            super.sendNotification();

        } catch (Exception e) {

           //todo
        }
        return null;
    }
}
