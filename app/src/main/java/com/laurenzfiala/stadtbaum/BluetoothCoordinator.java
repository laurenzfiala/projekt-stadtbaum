package com.laurenzfiala.stadtbaum;

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
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.util.Log;

/**
 * Created by Laurenz Fiala on 20/09/2017.
 * Handles all bluetooth tasks in the app.
 */
class BluetoothCoordinator {

    /**
     * Request code to enable bluetooth.
     */
    public static final int        REQUEST_ENABLE_BT = 1;

    /**
     * The interval in milliseconds to scan for bluetooth beacons.
     * See {@link #BT_SCAN_INTERVAL}.
     *
     * TODO implement logic
     */
    private static final int BT_SCAN_DURATION = 5000;

    /**
     * The interval in milliseconds to start scanning for bluetooth beacons.
     * See {@link #BT_SCAN_DURATION}.
     *
     * TODO implement logic
     */
    private static final int BT_SCAN_INTERVAL = 10000;

    /**
     * Ref to main activity to update UI etc.
     */
    private MainActivity mainActivity;

    /**
     * {@link BluetoothAdapter} used throughout this class.
     */
    private BluetoothAdapter bluetoothAdapter;

    /**
     * Holds the nearest bluetooth beacon that is also present
     * in the device mapping.
     */
    private Beacon nearestDevice;

    /**
     * Handles cases where user turns off bluetooth during application runtime.
     */
    public final BroadcastReceiver btEventReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            // It means the user has changed his bluetooth state.
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                switch (BluetoothCoordinator.this.bluetoothAdapter.getState()) {
                    case BluetoothAdapter.STATE_TURNING_OFF:
                    case BluetoothAdapter.STATE_OFF:
                        Log.e(BluetoothCoordinator.class.getSimpleName(), "user is turning/turned off bluetooth");
                        BluetoothCoordinator.this.mainActivity.showSnackbar(R.string.info_stop_scan_ble_off);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.e(BluetoothCoordinator.class.getSimpleName(), "user has turned on bluetooth");
                        BluetoothCoordinator.this.mainActivity.hideSnackbar();
                        scan();
                        break;
                }

            }
        }
    };

    public BluetoothCoordinator(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    /**
     * Starts routines in a separate task for UI responsiveness.
     * If neccessary, enables Bluetooth and starts scanning for BLE devices.
     */
    public void scan() {

        // bind receiver to bluetooth state change events
        this.mainActivity.registerReceiver(this.btEventReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        AsyncTask asyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] objects) {
                enableBleAndScan();
                return null;
            }
        };
        asyncTask.execute();

    }

    /**
     * Checks bluetooth availability and if deactivated, requests activation.
     * Also calls {@link #scanBleBeacons()} to start scanning.
     */
    private void enableBleAndScan() {

        final BluetoothManager bluetoothManager =
                (BluetoothManager) this.mainActivity.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();

        if (this.bluetoothAdapter == null) {
            Log.e("ERROR", "Device does not support bluetooth, adapter is null.");
            Dialogs.errorPrompt(this.mainActivity, this.mainActivity.getString(R.string.error_no_ble));
        }
        if (!this.bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            this.mainActivity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            scanBleBeacons();
        }

    }

    /**
     * Scan for BLE beacons and if one is contained in {@link JsonFetcher#deviceUrlMapping}
     * and has higher rssi, the corresponding webpage is displayed.
     */
    private void scanBleBeacons() {

        this.mainActivity.postLoadingStatus(R.string.start_ble_search);

        if (Build.VERSION.SDK_INT >= 21) { // android lollipop and higher

            this.bluetoothAdapter.getBluetoothLeScanner().startScan(new ScanCallback() {
                @Override
                @TargetApi(21)
                public void onScanResult(int callbackType, ScanResult result) {

                    Beacon newBeacon = new Beacon(result.getDevice().getAddress(), result.getRssi());
                    if (BluetoothCoordinator.this.mainActivity.mappingContainsKey(newBeacon.getAddress())) {

                        if (BluetoothCoordinator.this.nearestDevice == null || newBeacon.compareTo(BluetoothCoordinator.this.nearestDevice) > 0) {

                            BluetoothCoordinator.this.nearestDevice = newBeacon;
                            Log.i(this.getClass().getSimpleName(), "SCAN RESULT: address = " + result.getDevice().getAddress() + ", rssi = " + result.getRssi());

                            BluetoothCoordinator.this.mainActivity.displayPage();
                        }

                    }
                    super.onScanResult(callbackType, result);
                }
            });

        } else {

            this.bluetoothAdapter.startLeScan(new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice bluetoothDevice, int i, byte[] bytes) {
                    bluetoothDevice.connectGatt(BluetoothCoordinator.this.mainActivity, false, new BluetoothGattCallback() {
                        @Override
                        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {

                            Beacon newBeacon = new Beacon(bluetoothDevice.getAddress(), rssi);
                            if (BluetoothCoordinator.this.mainActivity.mappingContainsKey(newBeacon.getAddress())) {

                                if (BluetoothCoordinator.this.nearestDevice == null || newBeacon.compareTo(BluetoothCoordinator.this.nearestDevice) > 0) {

                                    BluetoothCoordinator.this.nearestDevice = newBeacon;
                                    Log.i(this.getClass().getSimpleName(), "SCAN RESULT: address = " + bluetoothDevice.getAddress() + ", rssi = " + rssi);

                                    BluetoothCoordinator.this.mainActivity.displayPage();
                                }

                            }
                            super.onReadRemoteRssi(gatt, rssi, status);
                        }
                    });
                }
            });

        }

    }

    public Beacon getNearestDevice() {
        return nearestDevice;
    }

    /*private pause() {

        if (Build.VERSION.SDK_INT >= 21) { // android lollipop and higher

            this.bluetoothAdapter.getBluetoothLeScanner().

        } else {

            this.bluetoothAdapter.stopLeScan(leScanCallbackOld);

        }

    }*/

}
