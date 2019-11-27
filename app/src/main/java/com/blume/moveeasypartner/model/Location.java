package com.blume.moveeasypartner.model;

public class Location {
    public double latitude;
    public double longitude;

    public Location (double Latitude, double Longitude){
        this.latitude = Latitude;
        this.longitude = Longitude;

    }
    public double getLatitude(){
        return latitude;
    }
    public double getLongitude(){
        return longitude;
    }
}
