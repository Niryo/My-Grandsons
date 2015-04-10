package huji.ac.il.test;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.sumppen.whatsapi4j.WhatsApi;
import net.sumppen.whatsapi4j.WhatsAppException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;


public class RegistrationActivity extends Activity {
    private WhatsApi wa=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        Button enterCode = (Button) findViewById(R.id.enterCode);
        enterCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getApplicationContext().getSharedPreferences("information", MODE_PRIVATE); //todo: remove
                SharedPreferences.Editor ed = preferences.edit();
                ed.putString("phone_number","972535059773");
                ed.commit();
                setEnterCodeLayout();
            }
        });
        Button register = (Button) findViewById(R.id.registerButton);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView status = (TextView) findViewById(R.id.status);
                status.setVisibility(View.VISIBLE);
                status.setText("Sending code..\n(can take several minutes)");
                String countryCode = ((EditText) findViewById(R.id.countryCode)).getText().toString();
                String phone = ((EditText) findViewById(R.id.phoneNumber)).getText().toString().substring(1); //the substring is for getting rid of the zero
                final String phoneNumber = countryCode + phone;
                Log.w("customMsg", "PhoneNumber is:phoneNumber");
                try {
                    wa = new WhatsApi(phoneNumber, "myGrandsons", "myGrandsons", getApplicationContext());
                } catch (Exception e) {

                }
                try {
                    wa.codeRequest("sms", null, null, new WhatsApi.Callback() {
                        @Override
                        public void doJob(JSONObject response) {
                            try {
                                if (response.getString("status").equals( "sent")) {
                               SharedPreferences preferences = getApplicationContext().getSharedPreferences("information", MODE_PRIVATE);
                                    SharedPreferences.Editor ed = preferences.edit();
                                    ed.putString("phone_number",phoneNumber);
                                    ed.commit();
                                setEnterCodeLayout();
                                }
                                status.setText("Status: code could not been sent.\nReason: " + response.getString("reason"));

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Log.w("custmMsg", "response is: " + response.toString());
                        }
                    });


                } catch (Exception e) {
                    v.setVisibility(View.VISIBLE);
                    status.setText("Status: code could not been sent.\nPlease check the phone number and country code and try again");
                }
            }
        });

    }

    private void setEnterCodeLayout(){
        if(wa==null){
            SharedPreferences preferences = getApplicationContext().getSharedPreferences("information", MODE_PRIVATE);
            String phoneNumber= preferences.getString("phone_number", "0");
//            String phoneNumber="972535059773"; //todo

            try {
                wa = new WhatsApi(phoneNumber, "myGrandsons", "myGrandsons", getApplicationContext()); //todo
//                wa = new WhatsApi(phoneNumber, "test", "nir", getApplicationContext());
            }catch(Exception e){
                Log.w("customMsg", "can't initiate whatsApi");
            }
          }
        setContentView(R.layout.enter_code);
        final Button finish= (Button) findViewById(R.id.finish);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String part1 = ((EditText) findViewById(R.id.codePart1)).getText().toString();
                String part2 = ((EditText) findViewById(R.id.codePart2)).getText().toString();
                String code= part1+part2;
                try {
                    wa.codeRegister(code, new WhatsApi.Callback() {
                        @Override
                        public void doJob(JSONObject response) {
                            Log.w("customMsg","response: "+ response.toString());

                            try {
                                if(response.getString("status").equals("ok")){
                                    SharedPreferences preferences = getApplicationContext().getSharedPreferences("information", MODE_PRIVATE);
                                    SharedPreferences.Editor ed = preferences.edit();
                                    ed.putString("password",response.getString("pw"));
                                    ed.putString("phone_number", response.getString("login"));
                                    ed.commit();

                                    AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                                    Intent pollIntent= new Intent();
                                    pollIntent.setAction("POLL");
                                    PendingIntent operation = PendingIntent.getBroadcast( getApplicationContext(), 0, pollIntent, 0);
                                    am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime()+150000, 150000, operation);
                                    saveManualPage();
                                    startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                                    finish();
                                }
                               else{

                                        ((TextView)findViewById(R.id.codeProblem)).setText("There was a problem with the code: "+ response.getString("reason"));

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.w("customMsg", "can't register with code");
                    e.printStackTrace();
                }
            }
        });
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_registration, menu);
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

    private void saveManualPage(){
        final String manual="Hello grandson!\n";
        Calendar calendar = new GregorianCalendar();
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmss");
        File rootDir = new File(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+File.separator + "savedFiles");
        if( !rootDir.exists()){//todo: move this check to the main activity
            rootDir.mkdir();
        }

        BufferedWriter out;
        try {
            FileWriter fileWriter = new FileWriter(getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+File.separator + "savedFiles"+File.separator + sdf.format(calendar.getTime()) + ".txt");
            out = new BufferedWriter(fileWriter);
            out.write(manual);
            out.close();
        }catch(Exception e){
            Log.w("customMsg", "can't write to file!");
        }
    }
}
