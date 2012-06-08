package com.bt.nia.koala.robustness.commands;

@SuppressWarnings("serial")
public class FailStateInOutputException extends RuntimeException {
	public FailStateInOutputException(String message) {
		super(message);
	}
}
