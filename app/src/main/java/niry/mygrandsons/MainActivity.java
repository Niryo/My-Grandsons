package niry.mygrandsons;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class MainActivity extends Activity { //todo: set font sizes , playbutton size, leave group, check new boot receiver alarm, test remove item at view pager.
    public static final String SHARED_INFORMATION = "SHARED_INFORMATION";
    public static final String SHARED_PASSWORD = "SHARED_PASSWORD";
    public static final String NOT_EXISTS = "NOT_EXISTS";
    public static final String SHARED_PHONE_NUMBER = "phone_number";
    public static final String IDENTITY = "myGrandsons";
    public static final String NICK_NAME = "myGrandsons";
    public static final String ACTION= "ACTION";
    public static final String FIRST_SETUP= "FIRST_SETUP";
    public static final String WAIT_FOR_DOWNLOAD= "WAIT_FOR_DOWNLOAD";
    public static final String SHARED_DIR_PATH = "SHARED_DIR_PATH";
    private final String SAVED_FILES_DIR = "Saved Files";


    public static String getSaveFilePath(Context context){
        SharedPreferences preferences = context.getSharedPreferences(MainActivity.SHARED_INFORMATION, context.MODE_PRIVATE);
        String path=  preferences.getString(MainActivity.SHARED_DIR_PATH, MainActivity.NOT_EXISTS);
        if (path==MainActivity.NOT_EXISTS){
            return null;
        }
        return path;
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(getIntent()!=null && MainActivity.FIRST_SETUP.equals(getIntent().getStringExtra(MainActivity.ACTION))){
            if(checkAndSetRootDir()){
            saveManualPage();}
            else{
                //todo

            }
        }
        if(checkPassword()){
            if(checkAndSetRootDir()){
            startActivity(new Intent(MainActivity.this, SlideshowActivity.class));
            this.finish();

            }else{
                //todo:
            }

        }
        else{
            startActivity(new Intent(MainActivity.this, RegistrationActivity.class));
        }
        finish();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Checks for saved whatsApp password
     * @return true if there is saved whatsApp password
     */
    private boolean checkPassword() {
        SharedPreferences preferences = getApplicationContext().getSharedPreferences(SHARED_INFORMATION, MODE_PRIVATE);
        String password = preferences.getString(SHARED_PASSWORD, NOT_EXISTS);
        if (password.equals(NOT_EXISTS)) {
            return false;
        }
        return true;
    }

    /**
     * Check that root dir exists
     * @return true if dir exists
     */
    private boolean checkAndSetRootDir(){
        if (Environment.MEDIA_MOUNTED.equals( Environment.getExternalStorageState())) {
            // We can read and write the media
            File rootDir = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+ File.separator + SAVED_FILES_DIR);
            //if dir doesn't exist, create it:
            if (!rootDir.exists() || !rootDir.isDirectory()){
                rootDir.mkdir();
            }
            String dirPath= getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+ File.separator + SAVED_FILES_DIR;
            SharedPreferences preferences = getApplicationContext().getSharedPreferences(MainActivity.SHARED_INFORMATION, MODE_PRIVATE); //todo: remove
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(MainActivity.getSaveFilePath(this), dirPath);
            ed.commit();
            return true;

        } //todo: manage internal storage.

        return false;
    }



    /**
     * Save the first slide:
     */
    private void saveManualPage() {
        final String manual = "Hello grandson!\n"; //todo: change to instruction
        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");


        BufferedWriter out;
        try {
            FileWriter fileWriter = new FileWriter(MainActivity.getSaveFilePath(this) + File.separator + sdf.format(calendar.getTime()) + ".txt");
            out = new BufferedWriter(fileWriter);
            out.write(manual);
            out.close();
        } catch (Exception e) {
        }
    }

}
