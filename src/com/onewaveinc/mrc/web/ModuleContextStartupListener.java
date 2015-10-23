package com.onewaveinc.mrc.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.onewaveinc.mrc.ModuleContext;

/**
 * 监听器，负责在 WEB 环境中启动 MRC
 * 
 * @author gmice
 */
public class ModuleContextStartupListener implements ServletContextListener {

    public void contextDestroyed(ServletContextEvent event) {
        ModuleContext moduleContext = (ModuleContext) event.getServletContext().getAttribute(ModuleContext.SERVLET_CONTEXT_ATTRIBUTE);
        moduleContext.stop();
    }

    public void contextInitialized(ServletContextEvent event) {
        ModuleContext moduleContext = new ModuleContextInWeb(event.getServletContext());
        moduleContext.run();
    }

}
