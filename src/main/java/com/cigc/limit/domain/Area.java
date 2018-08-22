package com.cigc.limit.domain;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/7/3 0003.
 */
public class Area implements Serializable {
    private  String  FX;
    private  String  weizhi;


    public Area() {
    }

    public String getFX() {
        return FX;
    }

    public void setFX(String FX) {
        this.FX = FX;
    }

    public String getWeizhi() {
        return weizhi;
    }

    public void setWeizhi(String weizhi) {
        this.weizhi = weizhi;
    }
}
