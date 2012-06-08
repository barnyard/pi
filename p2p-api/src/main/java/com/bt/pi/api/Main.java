/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api;

import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.bt.pi.core.node.KoalaNode;

public class Main {

	public static void main(String[] args) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		String[] files = new String[] { "applicationContext-p2p-api-integration.xml" };
		ClassPathXmlApplicationContext classPathXmlApplicationContext = new ClassPathXmlApplicationContext(files);
		KoalaNode node = (KoalaNode) classPathXmlApplicationContext.getBean("koalaNode");
		node.start();

	}
}
