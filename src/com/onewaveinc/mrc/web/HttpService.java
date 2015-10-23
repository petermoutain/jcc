package com.onewaveinc.mrc.web;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * HTTP 服务接口
 * 
 * @author gmice
 */
public interface HttpService {

    /**
     * 处理 HTTP 请求
     * 
     * @param path 路径
     * @param request HTTP 请求
     * @param response HTTP 相应
     * @param dispatcher 分发类型，REQUEST / FORWARD / INCLUDE
     * @throws ServletException
     * @throws IOException
     */
    void handle(String path, HttpServletRequest request, HttpServletResponse response, String dispatcher) throws ServletException, IOException;

}
