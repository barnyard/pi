package com.bt.nia.koala.robustness.commands;

@SuppressWarnings("serial")
public class UnexpectedOutputException extends RuntimeException {
	public UnexpectedOutputException(String message) {
		super(message);
	}

	public UnexpectedOutputException(String message, Throwable cause) {
		super(message, cause);
	}
}
