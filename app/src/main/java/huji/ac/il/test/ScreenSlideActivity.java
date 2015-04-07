package huji.ac.il.test;






import android.app.ActionBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
    private ScreenSlidePagerAdapter mPagerAdapter;
    private ArrayList<String> fileNameList;
    private BroadcastReceiver IncomingMessagesReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String>  fileNameListCopy= getImagesFromStorage();
            int position= fileNameList.size()-1;
            Log.w("customMsg", "adapter size before: "+mPagerAdapter.getCount());
            for(int i= position+1; i<fileNameListCopy.size(); i++){
                fileNameList.add(fileNameListCopy.get(i));
            }

            Log.w("customMsg", "adapter size after: "+mPagerAdapter.getCount());
            mPagerAdapter.notifyDataSetChanged();
            final ScreenSlidePageFragment currentFragment= (ScreenSlidePageFragment) mPagerAdapter.getRegisteredFragment( mPager.getCurrentItem());
            currentFragment.attachButton(fileNameList.size()-1, mPager);
            mPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                }

                @Override
                public void onPageSelected(int position) {

                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    currentFragment.removeButton();
                    mPager.setOnPageChangeListener(null);
                }

            });

        }
    };



    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("activity got created!");
        Log.w("customMsg", "starting server");
        Intent intentStartWhatsApiService = new Intent(this, WhatsApiService.class);
        intentStartWhatsApiService.putExtra("command", "START_WHATSAPP_SERVICE");
        startService(intentStartWhatsApiService);

        IntentFilter intentFilter = new IntentFilter("CUSTOM_INCOMING_MESSAGE");
        registerReceiver(this.IncomingMessagesReceiver, intentFilter);
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        setContentView(R.layout.activity_screen_slide);
        fileNameList=getImagesFromStorage();
        // Instantiate a ViewPager and a PagerAdapter.
        this.preferences = getPreferences(MODE_PRIVATE);
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setPageMargin(15);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager(), fileNameList);
        mPager.setAdapter(mPagerAdapter);
        mPager.setCurrentItem(this.preferences.getInt("currentItem", 0));
//        if("NOTIFICATION".equals(getIntent().getStringExtra("info"))){ //todo: save last seen page and start from it
//            mPager.setCurrentItem(this.fileNameList.size()-1);
//        }

    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        unregisterReceiver(this.IncomingMessagesReceiver);

    }

    @Override
    protected void onPause() {
        super.onPause();
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

    private ArrayList<String> getImagesFromStorage()
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

    private void attachNewMessageButton(){
        Fragment currentFragment=mPagerAdapter.getRegisteredFragment( mPager.getCurrentItem());


    }

}

