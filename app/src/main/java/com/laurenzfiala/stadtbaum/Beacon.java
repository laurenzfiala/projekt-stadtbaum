package com.laurenzfiala.stadtbaum;

import android.support.annotation.NonNull;

/**
 * Created by Laurenz Fiala on 19/09/2017.
 * DAO to hold data for a single bluetooth beacon.
 */
public class Beacon implements Comparable<Beacon> {

    private String address;
    private int rssi;

    public Beacon(String address, int rssi) {
        this.address = address;
        this.rssi = rssi;
    }

    public String getAddress() {
        return this.address;
    }

    public int getRssi() {
        return this.rssi;
    }

    @Override
    public int compareTo(@NonNull Beacon beacon) {
        return this.getRssi() - beacon.getRssi();
    }
}
