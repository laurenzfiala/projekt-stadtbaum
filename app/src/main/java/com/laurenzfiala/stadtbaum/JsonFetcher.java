package com.laurenzfiala.stadtbaum;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by Laurenz Fiala on 19/09/2017.
 * TODO jdoc
 */
class JsonFetcher extends AsyncTask<String, Void, Void> {

    private MainActivity mainActivity;

    public JsonFetcher(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected Void doInBackground(String... params) {


        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(params[0]);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();

            reader = new BufferedReader(new InputStreamReader(stream));

            StringBuffer buffer = new StringBuffer();
            String line = "";

            while ((line = reader.readLine()) != null) {
                buffer.append(line+"\n");
            }

            JSONObject jo = new JSONObject(buffer.toString());
            JSONArray knownDevices = jo.getJSONArray("known_devices");

            JSONObject current;
            for(int i = 0; i < knownDevices.length(); i++) {
                current = knownDevices.getJSONObject(i);
                this.mainActivity.getDeviceUrlMapping().put(
                            current.getString("address"),
                            current.getString("display_url")
                        );
            }

            return null;


        } catch (MalformedURLException e) {
            e.printStackTrace(); // TODO handle properly
        } catch (IOException e) {
            e.printStackTrace(); // TODO handle properly
        } catch (JSONException e) {
            e.printStackTrace(); // TODO handle properly
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
        }
        return null;
    }
}
