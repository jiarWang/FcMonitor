package com.student0.www;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.student0.monitor.ResourceMonitor;

/**
 * @createDate: 6/30/21
 * @author: 飞彻
 * @description:
 */
public class DemoResource extends ResourceMonitor {
    private static final String TAG = DemoResource.class.getSimpleName();
    public DemoResource(Resources resources) {
        super(resources);
    }

    @Override
    public Drawable getDrawable(int id) throws NotFoundException {
        Log.i(TAG, "加载Drawable: " + getResourceName(id));
        return super.getDrawable(id);
    }
}
