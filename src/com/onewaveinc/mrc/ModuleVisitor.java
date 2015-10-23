package com.onewaveinc.mrc;

/**
 * 模块访问者
 * 
 * @author gmice
 */
public interface ModuleVisitor {

    /**
     * 访问模块，实现此方法以实现对模块进行处理的逻辑
     * 
     * @param module
     * @throws Exception
     */
    void visit(Module module) throws Exception;

}
