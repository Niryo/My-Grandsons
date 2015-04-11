package huji.ac.il.test;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import java.io.File;


public class MainActivity extends Activity { //todo: set font sizes , clean code, playbutton size, leave group, check for saved files dir.
    public static final String SHARED_INFORMATION = "SHARED_INFORMATION";
    public static final String SHARED_PASSWORD = "SHARED_PASSWORD";
    public static final String NOT_EXISTS = "NOT_EXISTS";
    private final String SAVED_FILES_DIR = "Saved Files";
    public static String SAVED_FILES_DIR_PATH = null;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(checkPassword()){
            setSavedFilesDirectoryPath();
            startActivity(new Intent(MainActivity.this, Slideshow.class));
            this.finish();
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

    private void setSavedFilesDirectoryPath(){
        this.SAVED_FILES_DIR_PATH = getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+ File.separator + SAVED_FILES_DIR;
    }

}
