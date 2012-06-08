package com.bt.pi.app.imagemanager.crypto;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ImageCryptoExceptionTest {

	@Test
	public void shouldConstructAnExceptionWithAMessage() {
		String message = "some exception message";
		ImageCryptoException exception = new ImageCryptoException(message);

		assertEquals(message, exception.getMessage());
	}

	@Test
	public void shouldConstructAnExceptionWithAMessageAndThrowable() {
		String message = "some exception message";
		String causeMessage = "Original Cause";
		RuntimeException e = new RuntimeException(causeMessage);
		ImageCryptoException exception = new ImageCryptoException(message, e);

		assertEquals(message, exception.getMessage());
		assertEquals(causeMessage, exception.getCause().getMessage());
	}

}
