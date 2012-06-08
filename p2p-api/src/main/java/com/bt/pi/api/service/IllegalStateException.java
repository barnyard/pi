package com.bt.pi.api.service;

import com.bt.pi.api.ApiException;

public class IllegalStateException extends ApiException {

    private static final long serialVersionUID = -1255538290709797265L;

    public IllegalStateException(String message) {
        super(message);
    }
}
