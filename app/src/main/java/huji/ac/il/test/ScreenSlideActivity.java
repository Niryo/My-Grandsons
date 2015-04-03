package huji.ac.il.test;






import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.Toast;


import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;


public class ScreenSlideActivity extends FragmentActivity {

    private SharedPreferences preferences;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;
    private ArrayList<String> fileNameList;
    private BroadcastReceiver IncomingMessagesReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getApplicationContext(), "received message in activity..!", Toast.LENGTH_SHORT).show();
            Log.w("customMsg", "receive message!");
        }
    };

//    public static class IncomingMessagesReceiver extends BroadcastReceiver {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            Log.d("Service", "Sending broadcast to activity");
//            if( intent.getAction().equals("CUSTOM_INCOMING_MESSAGE")){ //todo: change name of notification to something meaningful
//                Log.w("customMsg","NEW MESSAGE NOTIFICATION RECEIVED FROM SERVICE!");
//                //todo:notify adapter
//            }
//
//        }
//    };

    @Override
    protected void onStart(){
        super.onStart();
        IntentFilter intentFilter = new IntentFilter("CUSTOM_INCOMING_MESSAGE");
        registerReceiver(this.IncomingMessagesReceiver, intentFilter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("activity got created!");

        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        setContentView(R.layout.activity_screen_slide);
        fileNameList=getImagesFromStorage();
        // Instantiate a ViewPager and a PagerAdapter.
        this.preferences = getPreferences(MODE_PRIVATE);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), fileNameList);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(this.preferences.getInt("currentItem", 0));

    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(this.IncomingMessagesReceiver);
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt("currentItem", mPager.getCurrentItem());
        ed.commit();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public ArrayList<String> getImagesFromStorage()
    {
        ArrayList<String> fileNameList= new ArrayList<String>();
        File file= new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+ File.separator + "savedFiles");

        if (file.isDirectory())
        {
            File[] listOfFiles= file.listFiles();
            for (int i = 0; i <listOfFiles.length; i++)
            {

                fileNameList.add(listOfFiles[i].getAbsolutePath());
            }
        }
        return fileNameList;
    }

}

