package com.yzy.monitor;

public class MonitorContextHolder {
    private static final ThreadLocal<MonitorContext> contextHolder = new ThreadLocal<>();

    public static void setContext(MonitorContext context) {
        contextHolder.set(context);
    }

    public static void removeContext() {
        contextHolder.remove();
    }

    public static MonitorContext getContext(){
        return contextHolder.get();
    }
}
