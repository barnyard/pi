/* (c) British Telecommunications plc, 2010, All Rights Reserved */
/* (c) British Telecommunications plc, 2010, All Rights Reserved */
package com.bt.pi.ops.website;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;

public class SpringResourceConfigTest {
	private SpringResourceConfig resourceConfig;
	private ApplicationContext appContext;
	private Map<String, Object> objectMap;
	private Collection<Object> beans;
	private MyPathClass myPathClass;
	private MyProviderClass myProviderClass;

	@SuppressWarnings("unchecked")
	@Before
	public void doBefore() throws IOException {
		resourceConfig = new SpringResourceConfig("com.bt.pi.ops.website");

		myPathClass = new MyPathClass();
		myProviderClass = new MyProviderClass();
		appContext = mock(ApplicationContext.class);
		objectMap = mock(Map.class);
		beans = new ArrayList<Object>();

		when(objectMap.values()).thenReturn(beans);
		when(appContext.getBeansWithAnnotation(Path.class)).thenReturn(objectMap);
		when(appContext.getBeansWithAnnotation(Provider.class)).thenReturn(objectMap);
	}

	@Test
	public void settingAppContextShouldLoadAllPathBeans() {
		// setup
		beans.add(myPathClass);

		// act
		resourceConfig.setApplicationContext(appContext);

		// assert
		assertTrue(resourceConfig.getSingletons().contains(myPathClass));
	}

	@Test
	public void settingAppContextShouldLoadAllProviderBeans() {
		// setup
		beans.add(myProviderClass);

		// act
		resourceConfig.setApplicationContext(appContext);

		// assert
		assertTrue(resourceConfig.getSingletons().contains(myProviderClass));
	}

	@Test
	public void settingAppContextShouldNotLoadBeansNotInPackage() {
		// setup
		Object obj = mock(Object.class);
		beans.add(obj);

		// act
		resourceConfig.setApplicationContext(appContext);

		// assert
		assertFalse(resourceConfig.getSingletons().contains(obj));
	}

	private static class MyPathClass {
	}

	private static class MyProviderClass {
	}
}
