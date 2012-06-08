/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.servlet;

import javax.servlet.http.HttpServletResponse;

public class WSSecurityHandlerException extends RuntimeException {
    private static final long serialVersionUID = -3108752903669563236L;
    private final int httpCode;

    public WSSecurityHandlerException(String message) {
        super(message);
        httpCode = HttpServletResponse.SC_BAD_REQUEST;
    }

    public WSSecurityHandlerException(String message, int theHttpCode) {
        super(message);
        this.httpCode = theHttpCode;
    }

    public WSSecurityHandlerException(Throwable t) {
        super(t);
        httpCode = HttpServletResponse.SC_BAD_REQUEST;

    }

    public int getHttpCode() {
        return httpCode;
    }

}
