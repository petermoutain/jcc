package com.onewaveinc.mrc.console;

import java.io.IOException;
import java.io.Writer;

public interface ConsoleService {
    
    boolean execute(String commandLine, Writer out) throws IOException;

}
