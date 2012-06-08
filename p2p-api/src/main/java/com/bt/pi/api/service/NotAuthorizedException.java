/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import com.bt.pi.api.ApiException;

public class NotAuthorizedException extends ApiException {

    private static final long serialVersionUID = 1693621912584895235L;

    public NotAuthorizedException(String message) {
        super(message);
    }
}
