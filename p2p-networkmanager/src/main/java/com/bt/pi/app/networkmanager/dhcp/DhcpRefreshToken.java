/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.app.networkmanager.dhcp;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.bt.pi.app.common.entities.SecurityGroup;

public class DhcpRefreshToken {
	private static final int DEFAULT_REFRESH_WAIT_TIME_MILLIS = 30 * 1000;
	private CountDownLatch dhcpRefreshCompleteLatch;
	private String dhcpDaemonPath;
	private String netRuntimePath;
	private Collection<SecurityGroup> securityGroups;
	private Throwable exception;
	
	protected DhcpRefreshToken(String aDhcpDaemonPath, String aNetRuntimePath, Collection<SecurityGroup> aSecurityGroups) {
		super();
		this.dhcpDaemonPath = aDhcpDaemonPath;
		this.netRuntimePath = aNetRuntimePath;
		this.securityGroups = aSecurityGroups;
		this.exception = null;
		
		this.dhcpRefreshCompleteLatch = new CountDownLatch(1);
	}
	
	public String getDhcpDaemonPath() {
		return dhcpDaemonPath;
	}
	
	public String getNetRuntimePath() {
		return netRuntimePath;
	}
	
	public Collection<SecurityGroup> getSecurityGroups() {
		return securityGroups;
	}
	
	protected void flagCompleted() {
		dhcpRefreshCompleteLatch.countDown();
	}
	
	protected void flagFailed(Throwable t) {
		this.exception = t;
		dhcpRefreshCompleteLatch.countDown();
	}
	
	public void blockUntilRefreshCompleted() {
		blockUntilRefreshCompleted(DEFAULT_REFRESH_WAIT_TIME_MILLIS, TimeUnit.MILLISECONDS);
	}	
	
	public void blockUntilRefreshCompleted(long units, TimeUnit timeUnit) {
		boolean isCompleted;
		try {
			isCompleted = dhcpRefreshCompleteLatch.await(units, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
		
		if (!isCompleted) {
			throw new DhcpRefreshTimeoutException(String.format("DHCP refresh did not happen after %d %s", units, timeUnit));
		}
		
		if (exception != null) {
			throw new DhcpRefreshFailedException("DHCP refresh failed", exception);
		}
	}
}
