package com.onewaveinc.mrc.console;

import java.io.IOException;
import java.io.Writer;

import com.onewaveinc.mrc.Module;
import com.onewaveinc.mrc.ModuleContext;

public class DefaultConsoleService implements ConsoleService {
    
    private static final String LF = "\n";
    
    private ModuleContext moduleContext;
    
    public DefaultConsoleService(ModuleContext moduleContext) {
        this.moduleContext = moduleContext;
    }

    public boolean execute(String commandLine, Writer out) throws IOException {
        String[] parts = commandLine.trim().split("\\s+");
        
        String command = parts[0];
        if ("".equals(command)) {
            return true;
        } else if ("version".equals(command)) {
            out.write(String.format("MRC 版本：%s" + LF, moduleContext.getVersion()));
            return true;
        } else if ("ls".equals(command)) {
            for (Module module : moduleContext.getModules()) {
                out.write(String.format("%-30s %-12s %s" + LF, module.getId(), module.getVersion(), module.getRootDirectory()));
            }
            return true;
        }
        
        return false;
    }

}
