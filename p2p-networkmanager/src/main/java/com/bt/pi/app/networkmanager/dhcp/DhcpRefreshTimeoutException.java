/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.dhcp;

public class DhcpRefreshTimeoutException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public DhcpRefreshTimeoutException(String message) {
		super(message);
	}
}
