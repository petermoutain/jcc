package com.onewaveinc.mrc;

/**
 * 模块状态枚举类
 * 
 * @author gmice
 */
public enum ModuleState {

    /**
     * 模块已经成功安装
     */
    INSTALLED,

    /**
     * 模块根据 MRC 获知了其他模块的存在，若存在隐含依赖，则预先进行数据准备工作，现已准备就绪
     */
    PREPARED,

    /**
     * 模块所需要的准备数据都已可用，已准备好进行启动
     */
    RESOLVED,

    /**
     * 模块已启动并正在运行
     */
    ACTIVE

}
