package com.onewaveinc.mrc;

/**
 * 模块范围格式异常
 * 
 * @author gmice
 */
public class VersionRangeFormatException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public VersionRangeFormatException(String msg) {
        super(msg);
    }

    public VersionRangeFormatException(String msg, Throwable thrown) {
        super(msg, thrown);
    }

}
