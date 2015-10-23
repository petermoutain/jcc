package com.onewaveinc.mrc;


public interface ModuleContextClassLoaderFilter {
    
    byte[] doFilter(String name, byte[] classData) throws Exception;

}
