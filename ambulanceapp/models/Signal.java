package me.ajaybala.ambulanceapp.models;

import com.firebase.geofire.GeoLocation;
import com.google.android.gms.maps.model.Marker;

public class Signal {
    GeoLocation geoLocation;
    String key;
    Marker marker;
    int signal_value = 0;
    String direction = "N";
    boolean isAlerted=false;
    Signal north;
    Signal south;
    Signal west;
    Signal east;

    public GeoLocation getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(GeoLocation geoLocation) {
        this.geoLocation = geoLocation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Marker getMarker() {
        return marker;
    }

    public void setMarker(Marker marker) {
        this.marker = marker;
    }

    public boolean isAlerted() {
        return isAlerted;
    }

    public void setAlerted(boolean alerted) {
        isAlerted = alerted;
    }

    public int getSignal_value() {
        return signal_value;
    }

    public void setSignal_value(int signal_value) {
        this.signal_value = signal_value;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public Signal getNorth() {
        return north;
    }

    public void setNorth(Signal north) {
        this.north = north;
    }

    public Signal getSouth() {
        return south;
    }

    public void setSouth(Signal south) {
        this.south = south;
    }

    public Signal getWest() {
        return west;
    }

    public void setWest(Signal west) {
        this.west = west;
    }

    public Signal getEast() {
        return east;
    }

    public void setEast(Signal east) {
        this.east = east;
    }
}
