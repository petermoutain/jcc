package com.onewaveinc.mrc.web;

import javax.servlet.ServletContext;

import com.onewaveinc.mrc.ModuleContext;

/**
 * WEB 环境下的 MRC，与 servlet context 相关联
 * 
 * @author gmice
 */
public class ModuleContextInWeb extends ModuleContext {

    private static final String WEB_INF = "/WEB-INF";

    public ModuleContextInWeb(ServletContext servletContext) {
        super(servletContext.getRealPath(WEB_INF));

        // 将 MRC 注册到 ServletContext 中
        servletContext.setAttribute(ModuleContext.SERVLET_CONTEXT_ATTRIBUTE, this);

        // 将 ServletContext 注册到 MRC 中
        this.setAttribute(ModuleContext.Attributes.SERVLET_CONTEXT, servletContext);
    }

}
