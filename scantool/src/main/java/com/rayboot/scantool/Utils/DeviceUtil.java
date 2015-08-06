package com.rayboot.scantool.Utils;

import android.content.Context;
import android.content.res.Configuration;

/**
 * Created by wswd on 2015/7/23.
 */
public class DeviceUtil {

    /**
     * 是否为横屏
     * @param context
     * @return
     */
    public static boolean isLandscape(Context context) {
        Configuration configuration = context.getResources().getConfiguration(); //获取设置的配置信息
        int ori = configuration.orientation ; //获取屏幕方向

        return ori == configuration.ORIENTATION_LANDSCAPE;
    }
}
