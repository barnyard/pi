package com.bt.pi.api;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.CLIENT)
public abstract class ApiException extends RuntimeException {

    private static final long serialVersionUID = -6967507036814730454L;

    public ApiException(String message) {
        super(message);
    }
}
