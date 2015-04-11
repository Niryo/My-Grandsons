package huji.ac.il.test;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Save text file in the background
 */
public class SaveTextFile extends SaveFileAsync {

    private final Context context;

    public SaveTextFile(Context context) {
        super(context);
        this.context = context;
    }


    @Override
    protected Void doInBackground(String... text) {
        //the name of the file will be the current time:
        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");

        File rootDir = new File(MainActivity.SAVED_FILES_DIR_PATH);
        try {

            FileWriter fileWriter = new FileWriter(MainActivity.SAVED_FILES_DIR_PATH + File.separator + sdf.format(calendar.getTime()) + ".txt");
            BufferedWriter out = new BufferedWriter(fileWriter);
            out.write(text[0]);
            out.close();

            sendBroadcastIncomingMessage();
            sendNotification();

        } catch (Exception e) {
            //todo
        }

        return null;
    }
}
