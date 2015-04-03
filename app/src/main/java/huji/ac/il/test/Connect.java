package huji.ac.il.test;

import android.os.AsyncTask;

import net.sumppen.whatsapi4j.WhatsApi;

import java.io.IOException;

class Connect extends AsyncTask<WhatsApi, Void, WhatsApi> {
    protected void onPostExecute(WhatsApi wa) {
        System.out.println("connect success!");
        System.out.println("now login");
        new Login().execute(wa);

    }

    @Override
    protected WhatsApi doInBackground(WhatsApi... wa) {
        try {
            wa[0].connect();
        } catch (IOException e) {
            System.out.println("Connect failed");
        }
        return wa[0];
    }

}