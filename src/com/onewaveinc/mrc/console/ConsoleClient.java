package com.onewaveinc.mrc.console;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import sun.management.ConnectorAddressLink;

import com.sun.tools.attach.VirtualMachine;

public class ConsoleClient {
    
    private static final String CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage:");
            System.out.println("    java -jar mrc.jar <pid>");
            System.out.println("    java -jar mrc.jar <pid> <command>");
            System.exit(1);
        }
        
        String pid = args[0];
        String command = null;
        if (args.length > 1) {
            StringBuilder builder = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                builder.append(args[i]);
                builder.append(' ');
            }
            builder.setCharAt(builder.length() - 1, '\n');
            command = builder.toString();
        }

        VirtualMachine vm = VirtualMachine.attach(pid);

        String connectorAddress = ConnectorAddressLink.importFrom(Integer.valueOf(pid));
        if (connectorAddress == null) {
            String agent = vm.getSystemProperties().getProperty("java.home") + File.separator + "lib" + File.separator + "management-agent.jar";
            vm.loadAgent(agent);
            connectorAddress = vm.getAgentProperties().getProperty(CONNECTOR_ADDRESS);
        }

        JMXServiceURL jmxServiceUrl = new JMXServiceURL(connectorAddress);
        JMXConnector jmxConnector = JMXConnectorFactory.connect(jmxServiceUrl, null);
        MBeanServerConnection jmxConnection = jmxConnector.getMBeanServerConnection();

        int port = (Integer) jmxConnection.getAttribute(new ObjectName("com.onewaveinc.mrc.console:type=Console"), "Port");
        
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] { 0x7f, 0x0, 0x0, 0x1 }), port));
        
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));

        out.write(command == null ? "version\n" : command);
        out.flush();
        
        L:
        for (;;) {
            for (;;) {
                try {
                    String line = in.readLine();
                    if (line.length() == 0) break;
                    System.out.println(line);
                } catch (SocketException e) {
                    break L;
                }
            }
            System.out.println();
            System.out.flush();
            
            if (command != null) break L;
            
            System.out.print("mrc> ");
            
            String commandLine = stdin.readLine();
            
            if (commandLine == null || "exit".equals(commandLine)) {
                break;
            }
            
            out.write(commandLine + "\n");
            out.flush();
        }
        
        socket.close();
        
        vm.detach();
    }
    
}
