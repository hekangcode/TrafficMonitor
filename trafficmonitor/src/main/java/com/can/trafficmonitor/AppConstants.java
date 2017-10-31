package com.can.trafficmonitor;

/**
 * Created by HEKANG on 2017/3/8.
 * Des:常量
 */

public class AppConstants {

    /**
     * 应用白名单说明：由于本模块有停止应用进程的功能，所以要慎重使用。白名单组成 类型：
     * 1、launcher
     * 2、com.can.trafficmonitor（模块本身的包名）
     * 3、哪个应用引入该arr时，需要添加，否则会自己把自己杀死，例如：com.can.appstore
     */
    public static final String[] APP_WHITE_LIST = {"cn.cibntv.ott", "com.cantv.launcher", "com" +
            ".can.trafficmonitor"};
}
