package com.laurenzfiala.stadtbaum;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Laurenz Fiala on 19/09/2017.
 * TODO jdoc
 */
class JsonFetcher extends AsyncTask<Void, Void, Void> {

    /**
     * URL at which the JSON mapping file should be accessible.
     */
    private static final String     DEVICE_MAPPING_URL = "https://laurenzfiala.github.io/projekt-stadtbaum/webresources/devicemapping.json";

    /**
     * The reference to the main activity for UI tasks and response.
     */
    private MainActivity mainActivity;

    /**
     * Key is device address, value is mapped URL to display.
     */
    public Map<String, String> deviceUrlMapping = new HashMap<>();

    public JsonFetcher(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    protected Void doInBackground(Void... noParams) {

        loadDeviceMapping();

        return null;
    }

    /**
     * Loads teh device mapping json from the webserver.
     */
    private void loadDeviceMapping() {
        HttpURLConnection connection = null;
        BufferedReader reader = null;

        try {
            URL url = new URL(DEVICE_MAPPING_URL);
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
                this.deviceUrlMapping.put(
                        current.getString("address"),
                        current.getString("display_url")
                );
            }
            Log.i(this.getClass().getSimpleName(), "Device mapping retrieved.");

        } catch (IOException | JSONException e) {
            Log.e(this.getClass().getSimpleName(), e.getMessage());
            cancel(true);
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
    }

    @Override
    protected void onCancelled() {
        Dialogs.errorPrompt(this.mainActivity, this.mainActivity.getString(R.string.error_jsonfetch));
        super.onCancelled();
    }

    public Map<String, String> getDeviceUrlMapping() {
        return this.deviceUrlMapping;
    }

}
