package com.bt.pi.app.networkmanager.addressing;

public class AddressNotAssignedException extends RuntimeException {
	private static final long serialVersionUID = 7473977759431404165L;
	
	public AddressNotAssignedException(String message) {
		super(message);
	}
}
