package com.cigc.limit.domain;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/2 0002.
 */
public class Location  implements Serializable{
    private double lat;
    private double lng;

    public Location() {
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
