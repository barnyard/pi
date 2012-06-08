//(c) British Telecommunications plc, 2009, All Rights Reserved
package com.bt.pi.api.service;

import com.bt.pi.api.ApiException;

public class UserNotFoundException extends ApiException {

    private static final long serialVersionUID = -1187693310900941007L;

    public UserNotFoundException(String message) {
        super(message);
    }
}
