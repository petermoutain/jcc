package com.onewaveinc.mrc;

/**
 * 依赖关系不满足异常
 * 
 * @author gmice
 */
public class UnsatisfiedDependencyException extends Exception {

    private static final long serialVersionUID = 1L;

    public UnsatisfiedDependencyException(String message) {
        super(message);
    }

    public UnsatisfiedDependencyException(String message, Throwable cause) {
        super(message, cause);
    }

}
