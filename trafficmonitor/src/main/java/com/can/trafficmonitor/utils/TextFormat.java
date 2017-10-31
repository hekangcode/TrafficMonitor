package com.can.trafficmonitor.utils;

import android.content.Context;

import com.can.trafficmonitor.R;

import java.text.DecimalFormat;

/**
 * Created by HEKANG on 2017/2/22.
 */

public class TextFormat {

    public static String formatByte(long data, Context context) {
        DecimalFormat format = new DecimalFormat(context.getString(R.string.format_text_style));
        if (data < 1024) {
            return data + context.getResources().getString(R.string.format_unit_byte);
        } else if (data < 1024 * 1024) {
            return format.format(data / 1024f) + context.getResources().getString(R.string.format_unit_k);
        } else if (data < 1024 * 1024 * 1024) {
            return format.format(data / 1024f / 1024f) + context.getResources().getString(R.string.format_unit_m);
        } else if (data < 1024 * 1024 * 1024 * 1024) {
            return format.format(data / 1024f / 1024f / 1024f) + context.getResources().getString(R.string
                    .format_unit_g);
        } else {
            return context.getResources().getString(R.string.format_beyond_scope);
        }
    }
}
