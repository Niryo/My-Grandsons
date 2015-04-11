package huji.ac.il.test;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


public class MainActivity extends Activity { //todo: set font sizes , clean code, playbutton size, leave group,
    private static final String SHARED_INFORMATION = "SHARED_INFORMATION";
    private static final String SHARED_PASSWORD = "SHARED_PASSWORD";
    private static final String NOT_EXIST = "NOT_EXIST";



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(checkPassword()){
            startActivity(new Intent(MainActivity.this, ScreenSlideActivity.class));
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
        String password = preferences.getString(SHARED_PASSWORD, NOT_EXIST);
        if (password.equals(NOT_EXIST)) {
            return false;
        }
        return true;
    }

}
