package niry.mygrandsons;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import net.sumppen.whatsapi4j.WhatsApi;


import org.json.JSONException;
import org.json.JSONObject;




/**
 * Registration activity
 */
public class RegistrationActivity extends Activity {
    private WhatsApi wa = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        Button enterCode = (Button) findViewById(R.id.enterCode);
        enterCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences preferences = getApplicationContext().getSharedPreferences(MainActivity.SHARED_INFORMATION, MODE_PRIVATE); //todo: remove
                SharedPreferences.Editor ed = preferences.edit();
                String countryCode = ((EditText) findViewById(R.id.countryCode)).getText().toString();
                String phone = ((EditText) findViewById(R.id.phoneNumber)).getText().toString().substring(1); //the substring is for getting rid of the zero
                final String phoneNumber = countryCode + phone;
                ed.putString(MainActivity.SHARED_PHONE_NUMBER, phoneNumber);
                ed.commit();
                setEnterCodeLayout();
            }
        });
        Button register = (Button) findViewById(R.id.registerButton);
        register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String countryCode = ((EditText) findViewById(R.id.countryCode)).getText().toString();
                String phone = ((EditText) findViewById(R.id.phoneNumber)).getText().toString().substring(1); //the substring is for getting rid of the zero
                final String phoneNumber = countryCode + phone;
                final TextView status = (TextView) findViewById(R.id.status);
                status.setVisibility(View.VISIBLE);
                status.setText("Sending code..\n(can take several minutes)");

                try {
                    wa = new WhatsApi(phoneNumber, "myGrandsons", "myGrandsons", getApplicationContext());
                } catch (Exception e) {
                    //todo
                }
                try {
                    wa.codeRequest("sms", null, null, new WhatsApi.Callback() {
                        @Override
                        public void doJob(JSONObject response) {
                            try {
                                if (response.getString("status").equals("sent")) {
                                    //save phone number:
                                    SharedPreferences preferences = getApplicationContext().getSharedPreferences(MainActivity.SHARED_INFORMATION, MODE_PRIVATE);
                                    SharedPreferences.Editor ed = preferences.edit();
                                    ed.putString("phone_number", phoneNumber);
                                    ed.commit();
                                    setEnterCodeLayout();
                                }
                                else if(response.getString("status").equals("ok")){
                                    finishRegistration(response);
                                }
                                else{
                                    status.setText("Status: code could not been sent.\nReason: " + response.getString("reason"));
                                }

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

    /**
     * Set the "enter code" page
     */
    private void setEnterCodeLayout() {
        if (wa == null) {
            SharedPreferences preferences = getApplicationContext().getSharedPreferences(MainActivity.SHARED_INFORMATION, MODE_PRIVATE);
            String phoneNumber = preferences.getString(MainActivity.SHARED_PHONE_NUMBER, MainActivity.NOT_EXISTS);
//            String phoneNumber="972535059773"; //todo

            try {
                wa = new WhatsApi(phoneNumber, "myGrandsons", "myGrandsons", getApplicationContext()); //todo

            } catch (Exception e) {
                //TODO
            }
        }
        setContentView(R.layout.enter_code);
        final Button finish = (Button) findViewById(R.id.finish);
        finish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String part1 = ((EditText) findViewById(R.id.codePart1)).getText().toString();
                String part2 = ((EditText) findViewById(R.id.codePart2)).getText().toString();
                String code = part1 + part2;
                try {
                    wa.codeRegister(code, new WhatsApi.Callback() {
                        @Override
                        public void doJob(JSONObject response) {
                            try {
                                if (response.getString("status").equals("ok")) {
                                    finishRegistration(response);
                                } else {

                                    ((TextView) findViewById(R.id.codeProblem)).setText("There was a problem with the code: " + response.getString("reason"));

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }




    private void finishRegistration(JSONObject response){
        try {
            SharedPreferences preferences = getApplicationContext().getSharedPreferences(MainActivity.SHARED_INFORMATION, MODE_PRIVATE);
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(MainActivity.SHARED_PASSWORD, response.getString("pw"));
            ed.putString(MainActivity.SHARED_PHONE_NUMBER, response.getString("login"));
            ed.commit();

            //sets the wakeup alarm (later will be set on boot:
            Intent pollIntent = new Intent(getApplicationContext(), WhatsApiService.class);
            pollIntent.putExtra(WhatsApiService.ACTION, WhatsApiService.ACTION_WAKE_AND_POLL);
            PendingIntent operation = PendingIntent.getService(getApplicationContext(), 0, pollIntent, 0);
            AlarmManager am = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
            am.setRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 900000, 900000, operation);


            Intent intent =new Intent(RegistrationActivity.this, MainActivity.class);
            intent.putExtra(MainActivity.ACTION, MainActivity.FIRST_SETUP);
            startActivity(intent);
            finish();
        }catch (Exception e){
            //todo:
        }
    }
}
