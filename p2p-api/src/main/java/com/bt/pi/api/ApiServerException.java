package com.bt.pi.api;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

@SoapFault(faultCode = FaultCode.SERVER)
public class ApiServerException extends RuntimeException {

    private static final long serialVersionUID = -6967507036814730454L;

    public ApiServerException() {
        super("Internal Server Error");
    }
}
