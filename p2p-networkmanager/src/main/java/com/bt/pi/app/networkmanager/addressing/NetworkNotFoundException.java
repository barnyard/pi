/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.addressing;

public class NetworkNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -3108752903669563236L;

	public NetworkNotFoundException(String message) {
		super(message);
	}
}
