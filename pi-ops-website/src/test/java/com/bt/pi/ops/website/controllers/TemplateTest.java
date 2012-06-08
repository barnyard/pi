package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceActivityState;
import com.bt.pi.ops.website.entities.ReadOnlyInstance;
import com.bt.pi.ops.website.entities.ReadOnlyUser;

import freemarker.template.Configuration;
import freemarker.template.Template;

@RunWith(MockitoJUnitRunner.class)
public class TemplateTest {
	private Configuration configuration = new Configuration();
	private String instanceId1 = "i-11111111";
	private String instanceId2 = "i-22222222";
	@Mock
	private Instance instance1 = mock(Instance.class);
	@Mock
	private Instance instance2 = mock(Instance.class);
	private Map<String, Object> model = new HashMap<String, Object>();
	private String templateName = "user_instances_validation.ftl";
	@Mock
	private ReadOnlyUser readOnlyUser;
	private String username = "fred";
	private String pid = "jdfsHAHFajfkaFSS";

	@Before
	public void before() {
		configuration.setClassForTemplateLoading(getClass(), "/templates");
		model.put("pid", pid);
	}

	@Test
	public void testUserInstanceValidation() throws Exception {
		// setup
		when(readOnlyUser.getUsername()).thenReturn(username);
		model.put("user", readOnlyUser);

		when(instance1.getInstanceId()).thenReturn(instanceId1);
		when(instance1.getInstanceActivityState()).thenReturn(InstanceActivityState.AMBER);
		when(instance2.getInstanceId()).thenReturn(instanceId2);
		when(instance2.getInstanceActivityState()).thenReturn(InstanceActivityState.RED);
		model.put("instances", Arrays.asList(new ReadOnlyInstance[] { new ReadOnlyInstance(instance1), new ReadOnlyInstance(instance2) }));

		// act
		Template template = configuration.getTemplate(templateName);
		String result = FreeMarkerTemplateUtils.processTemplateIntoString(template, model);
		System.err.println(result);

		// assert
		assertTrue(result.startsWith("<html>\n<head><title>User Instance Validation</title></head>\n<body>"));
		assertTrue(result.contains("<form action=\"/users/" + username + "/instancevalidation/" + pid + "\" method=\"post\">"));
		assertTrue(result.contains("<tr style=\"background-color: YELLOW;\">"));
		assertTrue(result.contains("<td>" + instanceId1 + "</td><td align=\"center\"><input type=\"checkbox\" name=\"validate_" + instanceId1 + "\" value=\"true\"/></td>"));
		assertTrue(result.contains("<tr style=\"background-color: RED;\">"));
		assertTrue(result.contains("<td>" + instanceId2 + "</td><td align=\"center\"><input type=\"checkbox\" name=\"validate_" + instanceId2 + "\" value=\"true\"/></td>"));
		assertTrue(result.endsWith("</body>\n</html>\n"));
	}
}
