package com.onewaveinc.mrc.web;

import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import com.onewaveinc.mrc.ModuleContext;

/**
 * 桥接 Listener，用于将外部 Web 容器的事件转发到在 MRC 内注册的监听器
 * 
 * @author gmice
 */
public class ModuleContextBridgeListener implements ServletContextListener, ServletContextAttributeListener, ServletRequestAttributeListener, ServletRequestListener, HttpSessionListener, HttpSessionAttributeListener {
    
    private ServletContextAttributeListener servletContextAttributeListener;
    
    private ServletRequestAttributeListener servletRequestAttributeListener;
    
    private ServletRequestListener servletRequestListener;

    private HttpSessionAttributeListener sessionAttributeListener;
    
    private HttpSessionListener sessionListener;
    
    public void attributeAdded(HttpSessionBindingEvent e) {
        if (sessionAttributeListener != null) {
            sessionAttributeListener.attributeAdded(e);
        }
    }

    public void attributeAdded(ServletContextAttributeEvent e) {
        if (servletContextAttributeListener != null) {
            servletContextAttributeListener.attributeAdded(e);
        }
    }

    public void attributeAdded(ServletRequestAttributeEvent e) {
        if (servletRequestAttributeListener != null) {
            servletRequestAttributeListener.attributeAdded(e);
        }
    }

    public void attributeRemoved(HttpSessionBindingEvent e) {
        if (sessionAttributeListener != null) {
            sessionAttributeListener.attributeRemoved(e);
        }
    }
    
    public void attributeRemoved(ServletContextAttributeEvent e) {
        if (servletContextAttributeListener != null) {
            servletContextAttributeListener.attributeRemoved(e);
        }
    }
    
    public void attributeRemoved(ServletRequestAttributeEvent e) {
        if (servletRequestAttributeListener != null) {
            servletRequestAttributeListener.attributeRemoved(e);
        }
    }

    public void attributeReplaced(HttpSessionBindingEvent e) {
        if (sessionAttributeListener != null) {
            sessionAttributeListener.attributeReplaced(e);
        }
    }

    public void attributeReplaced(ServletContextAttributeEvent e) {
        if (servletContextAttributeListener != null) {
            servletContextAttributeListener.attributeReplaced(e);
        }
    }
    
    public void attributeReplaced(ServletRequestAttributeEvent e) {
        if (servletRequestAttributeListener != null) {
            servletRequestAttributeListener.attributeReplaced(e);
        }
    }
    
    public void contextDestroyed(ServletContextEvent e) {
        servletContextAttributeListener = null;
        servletRequestAttributeListener = null;
        servletRequestListener = null;
        sessionAttributeListener = null;
        sessionListener = null;
    }

    public void contextInitialized(ServletContextEvent e) {
        ModuleContext moduleContext = (ModuleContext) e.getServletContext().getAttribute(ModuleContext.SERVLET_CONTEXT_ATTRIBUTE);
        servletContextAttributeListener = (ServletContextAttributeListener) moduleContext.getAttribute(ModuleContext.Attributes.SERVLET_CONTEXT_ATTRIBUTE_LISTENER);
        servletRequestAttributeListener = (ServletRequestAttributeListener) moduleContext.getAttribute(ModuleContext.Attributes.SERVLET_REQUEST_ATTRIBUTE_LISTENER);
        servletRequestListener = (ServletRequestListener) moduleContext.getAttribute(ModuleContext.Attributes.SERVLET_REQUEST_LISTENER);
        sessionAttributeListener = (HttpSessionAttributeListener) moduleContext.getAttribute(ModuleContext.Attributes.HTTP_SESSION_ATTRIBUTE_LISTENER);
        sessionListener = (HttpSessionListener) moduleContext.getAttribute(ModuleContext.Attributes.HTTP_SESSION_LISTENER);
    }

    public void requestDestroyed(ServletRequestEvent e) {
        if (servletRequestListener != null) {
            servletRequestListener.requestDestroyed(e);
        }
    }
    
    public void requestInitialized(ServletRequestEvent e) {
        if (servletRequestListener != null) {
            servletRequestListener.requestInitialized(e);
        }
    }

    public void sessionCreated(HttpSessionEvent e) {
        if (sessionListener != null) {
            sessionListener.sessionCreated(e);
        }
    }

    public void sessionDestroyed(HttpSessionEvent e) {
        if (sessionListener != null) {
            sessionListener.sessionDestroyed(e);
        }
    }

}
