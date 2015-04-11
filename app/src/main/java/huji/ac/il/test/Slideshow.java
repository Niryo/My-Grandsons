package huji.ac.il.test;



import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;


import java.io.File;
import java.util.ArrayList;


public class Slideshow extends FragmentActivity {
    private final String NEW_INCOMING_MESSAGE = "NEW_INCOMING_MESSAGE";
    private final int SPACE_BETWEEN_PAGES = 15;
    private final String SAVED_CURRENT_PAGE = "SAVED_CURRENT_PAGE";
    private final int NOT_EXISTS = 0;
    private SharedPreferences preferences;
    private ViewPager viewPager;
    private SlideshowPagerAdapter viewPagerAdapter;
    private ArrayList<String> fileNameList;


    /**
     * Listen for screen off/on intents and kill/start the whatsAppService
     */
    private BroadcastReceiver ScreenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction())){
                Intent killIntent = new Intent(context,WhatsApiService.class );
                killIntent.putExtra(WhatsApiService.ACTION, WhatsApiService.ACTION_KILL);
                context.startService(killIntent);
            }

            if(intent.ACTION_SCREEN_ON.equals(intent.getAction())){
                Intent startIntent = new Intent(context, WhatsApiService.class);
                startIntent.putExtra(WhatsApiService.ACTION, WhatsApiService.ACTION_START);
                context.startService(startIntent);
            }
        }
    };

    /**
     * Listens for incoming messages
     */
    private BroadcastReceiver IncomingMessageReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            ArrayList<String>  fileNameListCopy = getImagesFromStorage();
            int position= fileNameList.size();
            //we run on the new list starting from the end of the old list to get oll the new files:
            for(int i = position; i <fileNameListCopy.size(); i++){
                fileNameList.add(fileNameListCopy.get(i));
            }
            viewPagerAdapter.notifyDataSetChanged();

            //now we get the current page and attach to it the "new message":
            final ScreenSlidePageFragment currentFragment = (ScreenSlidePageFragment) viewPagerAdapter.getFragment(viewPager.getCurrentItem());
            currentFragment.attachButton(fileNameList.size()-1, viewPager);

            //we want to remove the button on page changes:
            viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                @Override
                public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //do nothing
                }

                @Override
                public void onPageSelected(int position) {
               //do nothing
                }

                @Override
                public void onPageScrollStateChanged(int state) {
                    currentFragment.removeButton();
                    viewPager.setOnPageChangeListener(null);
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
        //start whatsApiService:
        Intent startIntent = new Intent(this, WhatsApiService.class);
        startIntent.putExtra(WhatsApiService.ACTION, WhatsApiService.ACTION_START);
        startService(startIntent);

        //Register the incoming message receiver:
        IntentFilter intentFilter = new IntentFilter(NEW_INCOMING_MESSAGE);
        registerReceiver(this.IncomingMessageReceiver, intentFilter);
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(this.ScreenReceiver , filter);

        //get rid of status bar:
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);

        setContentView(R.layout.activity_screen_slide);
        fileNameList = getImagesFromStorage();
        // Instantiate a ViewPager and a PagerAdapter.
        this.preferences = getPreferences(MODE_PRIVATE);
        viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setPageMargin(SPACE_BETWEEN_PAGES);
        viewPagerAdapter = new SlideshowPagerAdapter(getSupportFragmentManager(), fileNameList);
        viewPager.setAdapter(viewPagerAdapter);
        //Set viewPager Current page based on saved current page:
        viewPager.setCurrentItem(this.preferences.getInt(SAVED_CURRENT_PAGE, NOT_EXISTS));
    }

    @Override
    protected  void onDestroy(){
        super.onDestroy();
        unregisterReceiver(this.IncomingMessageReceiver);

    }

    @Override
    protected void onPause() {
        super.onPause();
        //save the current page position:
        SharedPreferences.Editor ed = preferences.edit();
        ed.putInt(SAVED_CURRENT_PAGE, viewPager.getCurrentItem());
        ed.commit();
    }


    /**
     * Gets all files from storage
     * @return a list with all the files' names
     */
    private ArrayList<String> getImagesFromStorage()
    {
        ArrayList<String> fileNameList= new ArrayList<String>();
        File dir = new File(MainActivity.SAVED_FILES_DIR_PATH);

        File[] listOfFiles = dir.listFiles();
        for (int i = 0; i <listOfFiles.length; i++)
        {
            fileNameList.add(listOfFiles[i].getAbsolutePath());
        }

        return fileNameList;
    }
}

