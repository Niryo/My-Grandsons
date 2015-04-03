package huji.ac.il.test;

import android.os.AsyncTask;
import android.os.SystemClock;

import net.sumppen.whatsapi4j.WhatsApi;
import net.sumppen.whatsapi4j.WhatsAppException;

class Login extends AsyncTask<WhatsApi, Void, Boolean> {
    @Override
    protected Boolean doInBackground(WhatsApi... wa) {
        while (true) {
            try {
                wa[0].loginWithPassword("17+gaHU/Pa6VVGUgqkxQRtI/t+g=");
                System.out.println("login success!");
                break;
            } catch (WhatsAppException e) {
                System.out.println("failed loggin");
                SystemClock.sleep(2);

            }
        }

        return null;
    }
}