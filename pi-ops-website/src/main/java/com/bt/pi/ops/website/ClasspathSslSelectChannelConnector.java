/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ClasspathSslSelectChannelConnector extends org.eclipse.jetty.server.ssl.SslSelectChannelConnector {
	private static final Log LOG = LogFactory.getLog(ClasspathSslSelectChannelConnector.class);

	public ClasspathSslSelectChannelConnector() {
		super();
	}

	public void setKeystore(String theKeyStore) {
		final String certPath = getClass().getClassLoader().getResource(theKeyStore).getPath();
		LOG.debug("Found certificate keystore at " + certPath);
		super.setKeystore(certPath);
	}
}
