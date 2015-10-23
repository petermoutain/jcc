package com.onewaveinc.mrc;

import java.lang.reflect.Method;

/**
 * 实现了模块监听器接口的辅助基类，继承此类以更简便地实现模块事件监听器
 * 
 * @author gmice
 */
public class ModuleEventListenerSupport implements ModuleEventListener {

    /**
     * 当前的 MRC 实例
     */
    protected ModuleContext moduleContext;

    /**
     * 当前的模块实例
     */
    protected Module module;

    public void listen(ModuleEvent event) throws ModuleException {
        module = event.getModule();
        moduleContext = event.getModuleContext();
        Method method;
        try {
            method = this.getClass().getMethod(event.getEvent());
            method.invoke(this);
        } catch (Exception e) {
            if (e.getCause() instanceof ModuleException) {
                throw (ModuleException) e.getCause();
            }
            throw new ModuleException("对模块 " + module + " 发送事件 " + event.getEvent() + " 时出错", e);
        }
    }

    /**
     * 接收到 prepare 事件时的处理方法，覆盖此方法以实现模块准备的逻辑
     * 
     * @throws ModuleException
     */
    public void prepare() throws ModuleException {}

    /**
     * 接收到 resolve 事件时的处理方法，覆盖此方法以实现模块解析的逻辑
     * 
     * @throws ModuleException
     */
    public void resolve() throws ModuleException {}

    /**
     * 接收到 start 事件时的处理方法，覆盖此方法以实现模块启动的逻辑
     * 
     * @throws ModuleException
     */
    public void start() throws ModuleException {}

    /**
     * 接收到 stop 事件时的处理方法，覆盖此方法以实现模块停止的逻辑
     * 
     * @throws ModuleException
     */
    public void stop() throws ModuleException {}

}
