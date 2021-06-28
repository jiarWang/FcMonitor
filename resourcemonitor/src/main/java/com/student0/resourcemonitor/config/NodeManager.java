package com.student0.resourcemonitor.config;

import java.util.HashSet;
import java.util.Set;

/**
 * @createDate: 6/28/21
 * @author: 飞彻
 * @description:
 */
public enum  NodeManager {
    Instance;

    private Set<String> mAllContext = new HashSet<>();

    public boolean containContext(String context){
        //com.example.myapplication.MainActivity
        if (mAllContext == null) return false;
        return mAllContext.contains(context);
    }

    public void setAllContext(Set<String> activityList) {
        mAllContext = activityList;
    }

    public boolean removeContext(String context){
        if (mAllContext == null) return false;
        return mAllContext.remove(context);
    }
}
