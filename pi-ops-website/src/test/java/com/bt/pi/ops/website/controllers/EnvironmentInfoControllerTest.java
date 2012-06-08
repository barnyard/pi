package com.bt.pi.ops.website.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.bt.pi.core.util.common.CommandResult;
import com.bt.pi.core.util.common.CommandRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnvironmentInfoControllerTest {
	private static final String RPM_QUERY_COMMAND = "rpm -q --queryformat 'Build time: %{buildtime:date} - Install time: %{installtime:date} Name: %{name} Version: %{version} Release: %{release}\n' pi-cloud";

	@Mock
	private CommandRunner commandRunner;

	@InjectMocks
	EnvironmentInfoController controller = new EnvironmentInfoController();

	@Test
	public void shouldGetRpmDetails() {
		// setup
		String rpmQueryCommandOutput = "Build time: Mon 17 Jan 2011 02:42:06 PM GMT - Install time: Mon 17 Jan 2011 03:29:02 PM GMT Name: pi-cloud Version: 0.9 Release: 7467";
		List<String> output = Arrays.asList(rpmQueryCommandOutput);
		CommandResult commandResult = new CommandResult(0, output, null);
		when(commandRunner.run(RPM_QUERY_COMMAND)).thenReturn(commandResult);

		// act
		Response response = controller.getRpmDetails();

		// assert
		assertNotNull(response);
		assertEquals(Arrays.asList(rpmQueryCommandOutput).toString(), response.getEntity().toString());
	}
}
