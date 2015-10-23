package com.onewaveinc.mrc;

import java.util.ArrayList;
import java.util.List;

/**
 * 延迟初始化模块事件监听器。当第一次收到模块事件时，根据实际的监听器类名来实例化
 * 
 * @author gmice
 */
public class LazyInitializedModuleEventListener implements ModuleEventListener {

    private String listenerDef;

    private List<ModuleEventListener> listeners;

    /**
     * @param listenerDef 监听器类名
     */
    public LazyInitializedModuleEventListener(String listenerDef) {
        this.listenerDef = listenerDef;
    }

    public void listen(ModuleEvent event) throws Exception {
        if (listeners == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            listeners = new ArrayList<ModuleEventListener>();
            for (String part : listenerDef.split(";")) {
                Class<?> clazz = classLoader.loadClass(part.trim());
                ModuleEventListener listener = (ModuleEventListener) clazz.newInstance();
                listeners.add(listener);
            }
        }

        for (ModuleEventListener listener : listeners) {
            listener.listen(event);
        }
    }

}
