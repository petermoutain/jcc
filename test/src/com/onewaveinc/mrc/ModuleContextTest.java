package com.onewaveinc.mrc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;

public class ModuleContextTest {

    @Test
    public void testEmpty() {
        File rootDirectory = new File("test/empty");
        assertTrue(rootDirectory.isDirectory());

        Properties properties = new Properties();
        properties.put(ModuleContext.PROPERTY_MRC_READONLY, "true");
        properties.put(ModuleContext.PROPERTY_MRC_FAILURE, "false");

        ModuleContext moduleContext = new ModuleContext(rootDirectory.getAbsolutePath(), properties);
        moduleContext.run();

        assertTrue(moduleContext.isRunning());
    }
    
    @Test
    public void testExtension() throws IOException {
        File rootDirectory = new File("test/extension");
        assertTrue(rootDirectory.isDirectory());

        Properties properties = new Properties();
        properties.put(ModuleContext.PROPERTY_MRC_READONLY, "true");
        properties.put(ModuleContext.PROPERTY_MRC_FAILURE, "false");
        
        ModuleContext moduleContext = new ModuleContext(rootDirectory.getAbsolutePath(), properties);
        moduleContext.run();
        
        assertTrue(moduleContext.isRunning());
        
        Module a = moduleContext.getModule("a");
        assertEquals("/1.txt in a.ex.ex", readFirstLine(a.getModuleFile("/1.txt")));
        assertEquals("/2.txt in a.ex", readFirstLine(a.getModuleFile("/2.txt")));
        assertEquals("/3/3.txt in a.ex.ex", readFirstLine(a.getModuleFile("/3/3.txt")));
        
        Module b = moduleContext.getModule("b");
        assertEquals(Version.parse("1.0.0.1"), b.getVersion());
    }
    
    private String readFirstLine(File file) throws IOException {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            return reader.readLine();
        } finally {
            if (reader != null) reader.close();
        }
    }

}
