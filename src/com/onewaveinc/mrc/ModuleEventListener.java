package com.onewaveinc.mrc;

/**
 * 模块事件监听器接口。MRC 发送模块事件以通知模块进行相应生命周期的处理工作
 * 
 * @author gmice
 */
public interface ModuleEventListener {

    /**
     * 监听模块事件回调入口，实现此方法来进行模块相应生命周期的处理工作
     * 
     * @param event 模块事件
     * @throws Exception
     */
    void listen(ModuleEvent event) throws Exception;

}
