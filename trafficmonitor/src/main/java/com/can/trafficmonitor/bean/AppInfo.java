package com.can.trafficmonitor.bean;

import android.graphics.drawable.Drawable;

/**
 * Created by HEKANG on 2017/2/13.
 */

public class AppInfo {

    private String packageName;
    private String name;
    private Drawable drawable;
    private int uId;
    private long received;   // unit is byte
    private long transmitted;  // unit is byte

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public int getuId() {
        return uId;
    }

    public void setuId(int uId) {
        this.uId = uId;
    }

    public long getReceived() {
        return received;
    }

    public void setReceived(long received) {
        this.received = received;
    }

    public long getTransmitted() {
        return transmitted;
    }

    public void setTransmitted(long transmitted) {
        this.transmitted = transmitted;
    }
}
