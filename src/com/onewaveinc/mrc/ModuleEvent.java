package com.onewaveinc.mrc;

/**
 * 模块事件
 * 
 * @author gmice
 * 
 */
public class ModuleEvent {

    public final static String PREPARE = "prepare";

    public final static String RESOLVE = "resolve";

    public final static String START = "start";

    public final static String STOP = "stop";

    private String event;

    private Module module;

    private ModuleContext moduleContext;

    public ModuleEvent(String event, Module module, ModuleContext moduleContext) {
        this.event = event;
        this.module = module;
        this.moduleContext = moduleContext;
    }

    public String getEvent() {
        return event;
    }

    public Module getModule() {
        return module;
    }

    public ModuleContext getModuleContext() {
        return moduleContext;
    }

}
