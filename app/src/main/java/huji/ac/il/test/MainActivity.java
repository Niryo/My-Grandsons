package huji.ac.il.test;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.ListView;


import net.sumppen.whatsapi4j.MessageProcessor;
import net.sumppen.whatsapi4j.WhatsApi;
import net.sumppen.whatsapi4j.WhatsAppException;


import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        boolean loggedIn = false;

        String username = "972535059773";
        String identity = "test";
        String nickname = "nir";
//        if(!WhatsApiService.isServiceRunning()) {
            Log.w("customMsg", "service seems to be down, restarting now:");
            Intent intentPollMsg = new Intent(this, WhatsApiService.class);
            intentPollMsg.putExtra("command", "START_WHATSAPP_SERVICE");
            startService(intentPollMsg);

//        }
//        else{
//            Log.w("customMsg", "service working!");
//        }
        startActivity(new Intent(MainActivity.this, ScreenSlideActivity.class));
        this.finish();



//        WhatsApi wa = null;
//
//        try {
//            wa = new WhatsApi(username, identity, nickname, getApplicationContext());
//            MessageProcessor mp = new myMessageProcessor(getApplicationContext());
//            wa.setNewMessageBind(mp);
//
//
//        } catch (NoSuchAlgorithmException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        } catch (net.sumppen.whatsapi4j.WhatsAppException e1) {
//            // TODO Auto-generated catch block
//            e1.printStackTrace();
//        }
//
//        new Connect().execute(wa);
//        startActivity(new Intent(MainActivity.this, ScreenSlideActivity.class));
//        this.finish();




//
//
//        try {
//            wa.codeRequest("sms", "il", "he", new WhatsApi.Callback(){
//
//                @Override
//                public void doJob(JSONObject response) {
//                    System.out.println("The response is:" + response.toString());
//                }
//            });
//        } catch (UnsupportedEncodingException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (WhatsAppException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (JSONException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
