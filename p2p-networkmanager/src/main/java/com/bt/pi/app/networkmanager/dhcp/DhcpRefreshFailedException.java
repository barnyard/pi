/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.dhcp;

public class DhcpRefreshFailedException extends RuntimeException {
	private static final long serialVersionUID = 1L;
	
	public DhcpRefreshFailedException(String message, Throwable t) {
		super(message, t);
	}
}
