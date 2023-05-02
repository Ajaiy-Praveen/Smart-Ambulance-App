package me.ajaybala.ambulanceapp.models;

import com.google.android.libraries.places.api.model.Place;

public class PlaceDetail {
    public String primaryText;
    public String secondaryText;
    public String placeId;
    public boolean recent = false;
    public Place place;


    public PlaceDetail(String primaryText, String secondaryText, String placeId, boolean recent) {
        this.primaryText = primaryText;
        this.secondaryText = secondaryText;
        this.placeId = placeId;
        this.recent = recent;
    }

    public String getPrimaryText() {
        return primaryText;
    }

    public void setPrimaryText(String primaryText) {
        this.primaryText = primaryText;
    }

    public String getSecondaryText() {
        return secondaryText;
    }

    public void setSecondaryText(String secondaryText) {
        this.secondaryText = secondaryText;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }

    public boolean isRecent() {
        return recent;
    }

    public void setRecent(boolean recent) {
        this.recent = recent;
    }

    public Place getPlace() {
        return place;
    }

    public void setPlace(Place place) {
        this.place = place;
    }
}
