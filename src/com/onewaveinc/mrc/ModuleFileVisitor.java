package com.onewaveinc.mrc;

/**
 * 模块文件访问者
 * 
 * @author gmice
 */
public interface ModuleFileVisitor {

    /**
     * 访问模块文件，实现此方法以实现对模块文件进行处理的逻辑
     * 
     * @param file 模块文件
     * @throws Exception
     */
    void visit(ModuleFile file) throws Exception;

}
