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
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import org.json.JSONObject;

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
 * TODO jdoc
 * TODO cleanup
 */
public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_LOCATION = 2;
    //private static final int BT_SCAN_TIMEOUT = 60000;

    private static final int REQUEST_PERMISSIONS = 3;
    private static final String[] NEEDED_PERMISSIONS = new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};

    private static final String DEVICE_MAPPING_URL = "https://gist.githubusercontent.com/laurenzfiala/9d1993c5387e07c6d5b97d9743d8d40b/raw/1a56548b6ebb757b150da0a7b607b1d40d46dc07/stadtbaum.json";

    private WebView webView;
    private FrameLayout loadingPanel;
    private BluetoothAdapter bluetoothAdapter;

    /**
     * Contains found bluetooth beacons.
     * TODO update jdoc
     */
    private Beacon nearestDevice;
    public Map<String, String> deviceUrlMapping = new HashMap<>();

    private Snackbar searchInfo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.webView = (WebView) findViewById(R.id.panel_display);
        this.loadingPanel = (FrameLayout) findViewById(R.id.panel_loading);

        this.webView.setWebViewClient(new CustomWebViewClient());

        new JsonFetcher(this).execute(DEVICE_MAPPING_URL);

        AsyncTask asyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                requestPermissions();
                enableBle();
                return null;
            }
        };
        asyncTask.execute();

    }

    /**
     * Runtime request for "dangerous" coarse location permission.
     * Only request if not already requested.
     */
    private void requestPermissions() {
        if(Build.VERSION.SDK_INT >= 23) {
            int granted = 0;
            for (String perm : NEEDED_PERMISSIONS) {
                if (this.checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) {
                    granted++;
                }
            }
            if (granted < NEEDED_PERMISSIONS.length) {
                this.requestPermissions(NEEDED_PERMISSIONS, REQUEST_PERMISSIONS);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        // show error to user

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * TODO jdoc
     */
    private void enableBle() throws RuntimeException {

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        if (this.bluetoothAdapter == null) {
            throw new RuntimeException("Bluetooth is not supported by this device.");
        }
        if (!this.bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            scanBleBeacons();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                scanBleBeacons();
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * TODO jdoc
     */
    private void scanBleBeacons() {

        // show scan info
        MainActivity.this.searchInfo = Snackbar.make(findViewById(R.id.layout_root),
                R.string.start_ble_search, Snackbar.LENGTH_INDEFINITE);
        MainActivity.this.searchInfo.show();

        if (Build.VERSION.SDK_INT >= 21) { // android lollipop and higher
            this.bluetoothAdapter.getBluetoothLeScanner().startScan(new ScanCallback() {
                @Override
                @TargetApi(21)
                public void onScanResult(int callbackType, ScanResult result) {

                    Beacon newBeacon = new Beacon(result.getDevice().getAddress(), result.getRssi());
                    if(MainActivity.this.nearestDevice == null || newBeacon.compareTo(MainActivity.this.nearestDevice) > 0) {
                        MainActivity.this.nearestDevice = newBeacon;
                        Log.i("SCAN RESULT", "address = " + result.getDevice().getAddress() + ", rssi = " + result.getRssi());

                        displayPage();
                    }
                    super.onScanResult(callbackType, result);
                }
            });
        } else {
            this.bluetoothAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                    bluetoothDevice.connectGatt(MainActivity.this, false, new BluetoothGattCallback() {
                        @Override
                        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

                            Beacon newBeacon = new Beacon(bluetoothDevice.getAddress(), rssi);
                            if(MainActivity.this.nearestDevice.compareTo(newBeacon) > 0) {
                                MainActivity.this.nearestDevice = newBeacon;
                                Log.i("SCAN RESULT", "address = " + bluetoothDevice.getAddress() + ", rssi = " + rssi);

                                displayPage();
                            }
                            super.onReadRemoteRssi(gatt, rssi, status);
                        }
                    });
                }
            });
        }

    }

    /**
     * TODO jdoc
     */
    private void displayPage() {

        // TODO check if mapping has been fetched

        final String mappedUrl = this.deviceUrlMapping.get(this.nearestDevice.getAddress());
        if (mappedUrl != null) {
            MainActivity.this.searchInfo.setText(R.string.found_device_page_loading);

            this.webView.loadUrl(mappedUrl);
        }

    }

    public Map<String, String> getDeviceUrlMapping() {
        return this.deviceUrlMapping;
    }

    /**
     * TODO jdoc
     */
    public class CustomWebViewClient extends WebViewClient {

        @Override
        public void onPageFinished(WebView view, String url) {
            MainActivity.this.searchInfo.dismiss();
            MainActivity.this.loadingPanel.setVisibility(View.GONE);
            super.onPageFinished(view, url);
        }

        @Override
        public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
            super.onReceivedError(view, request, error); // TODO show error
        }
    }

}
