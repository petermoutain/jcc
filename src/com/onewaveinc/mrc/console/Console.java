package com.onewaveinc.mrc.console;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.JMException;
import javax.management.ObjectName;

import com.onewaveinc.mrc.ModuleContext;

public class Console implements ConsoleMBean {
    
    private static class ConsoleSession extends Thread {
	//todo compile
        
        private Socket socket;
        
        private BufferedReader in;
        
        private BufferedWriter out;
        
        public ConsoleSession(Socket socket) {
            this.socket = socket;
            this.setDaemon(true);
        }
        
        public void close() {
            sessions.remove(this);
            try {
                socket.close();
            } catch (IOException ignore) {}
        }
        
        public void run() {
            logger.info("控制台会话(" + socket.getRemoteSocketAddress() + ")开始");
            
            sessions.add(this);
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                
                String commandLine;
                while ((commandLine = in.readLine()) != null) {
                    ConsoleService consoleService = (ConsoleService) moduleContext.getAttribute(ModuleContext.Attributes.CONSOLE_SERVICE);
                    
                    if (!consoleService.execute(commandLine, out)) {
                        if ("".equals(commandLine.trim())) {
                            // EMPTY
                        } else {
                            out.write("未知命令：" + commandLine + LF);
                        }
                    }
                    
                    out.write(LF);
                    out.flush();
                }
            } catch (IOException ignore) {
            } finally {
                logger.info("控制台会话(" + socket.getRemoteSocketAddress() + ")结束");
                close();
            }
        }
        
    }
    
    private static Console instance;
    
    private static final String LF = "\n";
    
    private static final Logger logger = Logger.getLogger(Console.class.getName());
    
    private static Set<ConsoleSession> sessions = Collections.synchronizedSet(new HashSet<ConsoleSession>());
    
    private int port;
    
    private ServerSocket serverSocket;
    
    private boolean toClose;
    
    private Console() {}
    
    public int getPort() {
        return port;
    }
    
    private void _start() throws IOException {
        serverSocket = new ServerSocket(0, 50, InetAddress.getByAddress(new byte[] { 0x7f, 0x0, 0x0, 0x1 }));
        port = serverSocket.getLocalPort();
        
        logger.info("控制台服务监听端口：" + port);
        
        // 启动端口监听
        new Thread(new Runnable() {
            public void run() {
                for (;;) {
                    try {
                        final Socket socket = serverSocket.accept();
                        new ConsoleSession(socket).start();
                    } catch (IOException e) {
                        if (toClose) break;
                        logger.log(Level.SEVERE, e.getMessage(), e);
                    }
                }
            }
        }).start();
    }

    private void _stop() throws IOException {
        toClose = true;
        
        for (ConsoleSession session : sessions) {
            session.close();
        }
        serverSocket.close();
    }
    
    private static ModuleContext moduleContext;
    
    private static boolean mbeanRegistered;
    
    /**
     * 启动控制台服务
     */
    public static void start(ModuleContext moduleContext) {
        if (instance != null) return; // 服务已启动，直接返回
        
        Console.moduleContext = moduleContext;
        
        instance = new Console();
        try {
            instance._start();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "控制台服务启动时出错", e);
            instance = null;
            return;
        }
        
        try {
            ManagementFactory.getPlatformMBeanServer().registerMBean(instance, new ObjectName("com.onewaveinc.mrc.console:type=Console"));
            mbeanRegistered = true;
        } catch (JMException e) {
            logger.log(Level.SEVERE, "控制台服务注册为MBean时出错", e);
        }
    }
    
    /**
     * 停止控制台服务
     */
    public static void stop() {
        if (instance == null) return; // 服务已停止，直接返回
        
        try {
            instance._stop();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "控制台服务停止时出错", e);
        }
        
        if (mbeanRegistered) {
            try {
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(new ObjectName("com.onewaveinc.mrc.console:type=Console"));
                mbeanRegistered = false;
            } catch (JMException e) {
                logger.log(Level.SEVERE, "控制台服务MBean注销时出错", e);
            }
        }
    }
    
}
