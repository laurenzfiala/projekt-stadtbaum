package com.laurenzfiala.stadtbaum;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This is the main screen of the app.
 * It loads the device mapping (for beacons) and shows the corresponding webpage in a webview.
 * TODO cleanup
 */
public class MainActivity extends AppCompatActivity {

    /**
     * See {@link BluetoothCoordinator}.
     */
    private BluetoothCoordinator bluetoothCoordinator;

    /**
     * Fetches the device mapping JSON file from {@link JsonFetcher#DEVICE_MAPPING_URL}.
     */
    private JsonFetcher jsonFetcher;

    /**
     * The {@link WebView} to show the webpage in.
     */
    private WebView                 webView;

    /**
     * The loading overlay.
     */
    private LinearLayout             loadingPanel;

    /**
     * List of statuses to show in th eloading panel.
     */
    private ArrayList<TextView>     statusTexts;

    /**
     * Snackbar to show
     */
    private Snackbar snackbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.webView = (WebView) findViewById(R.id.panel_display);
        this.loadingPanel = (LinearLayout) findViewById(R.id.panel_loading);

        this.statusTexts = new ArrayList<>();

        this.webView.setWebViewClient(new CustomWebViewClient());

        checkInternetAndFetchDeviceMapping();

        this.bluetoothCoordinator = new BluetoothCoordinator(this);
        this.bluetoothCoordinator.scan();

    }

    /**
     * Checks whether this device has internet access and if so, continues to fetch
     * the neccessary beacon mapping.
     */
    private void checkInternetAndFetchDeviceMapping() {

        if(Utils.isNetworkAvailable(this)) {
            this.jsonFetcher = new JsonFetcher(this);
            this.jsonFetcher.execute();
        } else {
            Dialogs.noInternetDialog(this, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    checkInternetAndFetchDeviceMapping();
                }
            });
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // show error to user

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * Start scan when bluetooth has been enabled.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothCoordinator.REQUEST_ENABLE_BT:
                this.bluetoothCoordinator.scan();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Shows the mapped webpage in the {@link #webView}.
     */
    public void displayPage() {

        final String mappedUrl = this.jsonFetcher.getDeviceUrlMapping().get(this.bluetoothCoordinator.getNearestDevice().getAddress());

        // only load if beacon is in mapping and not already loaded
        if (mappedUrl != null && !mappedUrl.equals(this.webView.getOriginalUrl())) {
            MainActivity.this.postLoadingStatus(R.string.found_device_page_loading);

            if(Utils.isNetworkAvailable(MainActivity.this)) {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        MainActivity.this.webView.loadUrl(mappedUrl);
                    }
                }, 4000);

            } else {
                Dialogs.noInternetDialog(this, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        MainActivity.this.displayPage();
                    }
                });
            }

        }

    }

    /**
     * Shows a {@link Snackbar} to the user with the specified content.
     */
    public void showSnackbar(final int stringId) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.snackbar = Snackbar.make(findViewById(R.id.layout_root),
                        getString(stringId), Snackbar.LENGTH_INDEFINITE);
                MainActivity.this.snackbar.show();
                Utils.snackbarUndismissable(MainActivity.this.snackbar);
            }
        });

    }

    /**
     * Hides {@link #snackbar}.
     */
    public void hideSnackbar() {

        if (this.snackbar == null) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MainActivity.this.snackbar.dismiss();
            }
        });

    }

    /**
     * See {@link #postLoadingStatus(int, boolean)}.
     * Shortcut method. Does not clear previous statuses.
     */
    public void postLoadingStatus(final int stringId) {
        postLoadingStatus(stringId, false);
    }

    /**
     * Show the loading panel and set the status text shown.
     * @param stringId The string resource if id to be shown.
     * @param clear Whether to clear previous statuses.
     */
    public void postLoadingStatus(final int stringId, final boolean clear) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if(clear) {
                    for (TextView t : MainActivity.this.statusTexts) {
                        ((LinearLayout) t.getParent()).removeView(t);
                    }
                }

                MainActivity.this.loadingPanel.setVisibility(View.VISIBLE);

                TextView text = new TextView(MainActivity.this);
                text.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.loading_text));
                text.setGravity(Gravity.CENTER_HORIZONTAL);
                text.setText(getString(stringId));

                final int padding = (int) getResources().getDimension(R.dimen.default_margin);
                text.setPadding(padding, 0, padding, 0);

                for (TextView t : MainActivity.this.statusTexts) {
                    t.setTextColor(ContextCompat.getColor(MainActivity.this, R.color.loading_text_subtle));
                }

                MainActivity.this.statusTexts.add(text);
                MainActivity.this.loadingPanel.addView(text);
            }
        });
    }

    /**
     * TODO
     * @param key the key to check
     * @return
     */
    public boolean mappingContainsKey(String key) {
        return this.jsonFetcher.getDeviceUrlMapping().containsKey(key);
    }

    /**
     * Custom client for the webview to handle errors etc coming form the {@link WebView}.
     */
    public class CustomWebViewClient extends WebViewClient {

        /**
         * Hide the loading panel when the page is fully loaded.
         * @param view See {@link WebViewClient#onPageFinished(WebView, String)}
         * @param url See {@link WebViewClient#onPageFinished(WebView, String)}
         */
        @Override
        public void onPageFinished(WebView view, String url) {
            MainActivity.this.loadingPanel.setVisibility(View.GONE);
            super.onPageFinished(view, url);
        }

        /**
         * Show an error to the user if a page could not be loaded.
         * @param view See {@link WebViewClient#onReceivedError(WebView, WebResourceRequest, WebResourceError)}
         * @param request See {@link WebViewClient#onReceivedError(WebView, WebResourceRequest, WebResourceError)}
         * @param error See {@link WebViewClient#onReceivedError(WebView, WebResourceRequest, WebResourceError)}
         */
        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            Log.e(this.getClass().getSimpleName(), "Could not load webpage: " + error.toString());

            Dialogs.errorPrompt(MainActivity.this, "Could not load webpage.\n" + error.toString()); // TODO dialog

            super.onReceivedError(view, request, error);
        }
    }

}
