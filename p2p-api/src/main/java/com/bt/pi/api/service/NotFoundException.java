/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import com.bt.pi.api.ApiException;

public class NotFoundException extends ApiException {

    private static final long serialVersionUID = -714984029275068036L;

    public NotFoundException(String message) {
        super(message);
    }
}
