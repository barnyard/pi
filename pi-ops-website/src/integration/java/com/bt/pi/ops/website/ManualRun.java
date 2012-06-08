/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.bt.pi.api.utils.IdFactory;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.core.application.health.ErrorLogAppender;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.id.PId;

public class ManualRun extends IntegrationTestBase {
	@Before
	public void before() {
		classPathXmlApplicationContext.getBean(ErrorLogAppender.class).setErrorLogAppenderLevel("WARN");

		IdFactory idFactory = classPathXmlApplicationContext.getBean(IdFactory.class);
		String instanceId = idFactory.createNewInstanceId(new AvailabilityZone("drone zone", 99, 99, "").getGlobalAvailabilityZoneCode());
		System.err.println("Instance being seeded has id " + instanceId);

		Instance instance1 = new Instance(instanceId, "abuser", "default");
		instance1.setAvailabilityZone("drone zone");

		PId id = piIdBuilder.getPIdForEc2AvailabilityZone(instance1);
		BlockingDhtWriter writer = dhtClientFactory.createBlockingWriter();
		writer.put(id, instance1);
	}

	@Test
	@Ignore
	public void startUpAndWait() throws HttpException, IOException, InterruptedException {
		System.err.println("********************************************");
		System.err.println("********************************************");
		System.err.println("********************************************");
		System.err.println("********************************************");
		System.err.println("Website is up and running!");

		long minutes = 4;
		Thread.sleep(minutes * 60000);
	}
}
