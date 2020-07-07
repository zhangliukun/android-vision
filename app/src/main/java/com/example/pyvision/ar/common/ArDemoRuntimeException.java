/*
 * Copyright (c) Huawei Technologies Co., Ltd. 2020-2020. All rights reserved.
 */

package com.example.pyvision.ar.common;

/**
 * AR demo runtime exception.Throw this exception uniformly when
 * there is a runtime exception in this ar example
 *
 * @author HW
 * @since 2020-03-25
 */
public class ArDemoRuntimeException extends RuntimeException {
    /**
     * Constructor.
     */
    public ArDemoRuntimeException() {
        super();
    }

    /**
     * Constructor.
     *
     * @param message message
     */
    public ArDemoRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message message
     * @param cause cause
     */
    public ArDemoRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }
}