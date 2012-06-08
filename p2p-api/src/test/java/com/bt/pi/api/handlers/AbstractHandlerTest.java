package com.bt.pi.api.handlers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Calendar;

import org.springframework.ws.transport.context.TransportContext;
import org.springframework.ws.transport.http.HttpExchangeConnection;

import com.sun.net.httpserver.HttpExchange;

public abstract class AbstractHandlerTest {
	
	protected TransportContext transportContext;
	protected HttpExchangeConnection connection;
	protected HttpExchange httpExchange;
	protected Object userid = "userid";
	
	public void before(){
		this.connection = mock(HttpExchangeConnection.class);
		this.transportContext = mock(TransportContext.class);
		this.httpExchange = mock(HttpExchange.class);
		
		when(this.transportContext.getConnection()).thenReturn(connection);
		when(this.connection.getHttpExchange()).thenReturn(httpExchange);
		when(this.httpExchange.getAttribute("koala.api.userid")).thenReturn(userid);
	}
	
	protected void assertDateEquals(Calendar startTime) {
		Calendar now = Calendar.getInstance();
		assertTrue(Math.abs(now.getTimeInMillis() - startTime.getTimeInMillis()) < 6000);
	}
}
