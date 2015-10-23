package com.onewaveinc.mrc.web;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.onewaveinc.mrc.ModuleContext;

/**
 * 桥接过滤器，用于把 HTTP 请求转换成对注册的 HTTPService 的实现类的调用
 * 
 * @author gmice
 */
public class ModuleBridgeFilter implements Filter {

    public static final String ATTRIBUTE_ORIGINAL_CLASSLOADER = "com.onewaveinc.mrc.web.ModuleBridgeFilter.original_classloader";

    private String dispatcher;

    private ServletContext servletContext;

    public void destroy() {
        dispatcher = null;
        servletContext = null;
    }

    public void doFilter(ServletRequest _request, ServletResponse _response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) _request;
        HttpServletResponse response = (HttpServletResponse) _response;

        String path = null;
        if ("INCLUDE".equals(dispatcher)) {
            // 当 dispatcher 类型为 include 时，request.getServletPath() 返回的是原始 request 的路径，所以这里要特殊处理
            path = (String) request.getAttribute("javax.servlet.include.servlet_path");
        }
        if (path == null) path = request.getServletPath();
        if (path != null) {
            // 从 servlet context 中获取 MRC 实例
            ModuleContext moduleContext = (ModuleContext) servletContext.getAttribute(ModuleContext.SERVLET_CONTEXT_ATTRIBUTE);
            if (moduleContext == null) {
                render(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, this.getClass().getResourceAsStream("no-mrc.html"));
                return;
            }

            // 从 MRC 属性中获取注册的 HTTPService 实例
            HttpService service = (HttpService) moduleContext.getAttribute(ModuleContext.Attributes.HTTP_SERVICE);
            if (service != null) {
                Thread thread = Thread.currentThread();
                ClassLoader original = thread.getContextClassLoader();
                request.setAttribute(ATTRIBUTE_ORIGINAL_CLASSLOADER, original);
                try {
                    thread.setContextClassLoader(moduleContext.getClassLoader());
                    service.handle(path, request, response, dispatcher);
                } finally {
                    thread.setContextClassLoader(original);
                    request.removeAttribute(ATTRIBUTE_ORIGINAL_CLASSLOADER);
                }
            } else {
                // 缺少注册的 HTTPService，无法处理 HTTP 请求
                String staticContent = (String) moduleContext.getAttribute(ModuleContext.Attributes.HTTP_STATIC_CONTENT);
                if (staticContent != null) {
                    // 有模块提供了静态页面内容来说明原因，输出此内容
                    render(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, new ByteArrayInputStream(staticContent.getBytes("UTF-8")));
                } else {
                    if (moduleContext.isRunning()) {
                        // MRC 正常启动，但没有 HTTP 服务模块
                        render(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, this.getClass().getResourceAsStream("no-http-service.html"));
                    } else {
                        if ("true".equalsIgnoreCase(moduleContext.getProperty(ModuleContext.PROPERTY_MRC_FAILURE))) {
                            // MRC 启动失败
                            render(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, this.getClass().getResourceAsStream("mrc-fail.html"));
                        } else {
                            // MRC 正在启动中
                            render(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, this.getClass().getResourceAsStream("mrc-is-running.html"));
                        }
                    }
                }
            }

            // TODO: 是否需要继续处理 filter chain？
        }
    }
    
    private void render(HttpServletResponse response, int status, InputStream in) throws IOException {
        response.setStatus(status);
        response.setContentType("text/html");
        
        OutputStream out = response.getOutputStream();
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) != -1) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        dispatcher = filterConfig.getInitParameter("dispatcher");
        servletContext = filterConfig.getServletContext();
    }

}
