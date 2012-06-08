package com.bt.pi.app.networkmanager.dhcp;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.networkmanager.dhcp.DhcpRefreshFailedException;
import com.bt.pi.app.networkmanager.dhcp.DhcpRefreshTimeoutException;
import com.bt.pi.app.networkmanager.dhcp.DhcpRefreshToken;


public class DhcpRefreshTokenTest {
	private DhcpRefreshToken dhcpRefreshToken;
	
	@Before
	public void before() {
		dhcpRefreshToken = new DhcpRefreshToken("/usr/sbin/dhcpd", "", new ArrayList<SecurityGroup>());
	}
	
	@Test
	public void shouldReleaseLatchWhenRefreshFlaggedAsComplete() {
		// setup
		new Thread(new Runnable() {			
			@Override public void run() {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
				dhcpRefreshToken.flagCompleted();
			}
		}).start();
		
		// act
		dhcpRefreshToken.blockUntilRefreshCompleted();
		
		// assert
		// unblocked
	}
	
	@Test(expected=DhcpRefreshFailedException.class)
	public void shouldReleaseLatchAndThrowCausingExceptionWhenRefreshFlaggedAsFailed() {
		// setup
		new Thread(new Runnable() {			
			@Override public void run() {
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {}
				dhcpRefreshToken.flagFailed(new RuntimeException("oops"));
			}
		}).start();
		
		// act
		dhcpRefreshToken.blockUntilRefreshCompleted();
	}
	
	@Test(expected=DhcpRefreshTimeoutException.class)
	public void shouldThrowExceptionWhenRefreshNotCompletedInTime() {
		// act
		dhcpRefreshToken.blockUntilRefreshCompleted(1, TimeUnit.SECONDS);
	}
}
