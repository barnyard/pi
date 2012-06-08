/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.utils;

public class RetriesExhaustedException extends RuntimeException {
	private static final long serialVersionUID = -3108752903669563236L;

	public RetriesExhaustedException(String message) {
		super(message);
	}

	public RetriesExhaustedException(String message, Exception e) {
		super(message, e);
	}
}
