package com.onewaveinc.mrc;

/**
 * 模块依赖项格式异常
 * 
 * @author gmice
 */
public class RequireEntryFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public RequireEntryFormatException(String msg) {
        super(msg);
    }

    public RequireEntryFormatException(String msg, Throwable thrown) {
        super(msg, thrown);
    }

}
