package com.student0.resourcemonitor.config;

/**
 * @createDate: 6/27/21
 * @author: 飞彻
 * @description:
 */
public class ResourceMonitorExtension {
    public String className;
    public static String DEFAULT_APPLICATION = "com.student0.monitor.CustomApplication";

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }
}
