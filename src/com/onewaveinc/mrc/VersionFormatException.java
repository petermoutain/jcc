package com.onewaveinc.mrc;

/**
 * 模块版本格式异常
 * 
 * @author gmice
 */
public class VersionFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VersionFormatException(String msg) {
        super(msg);
    }

    public VersionFormatException(String msg, Throwable thrown) {
        super(msg, thrown);
    }

}
