package com.bt.nia.koala.robustness.commands;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.OpsWebsiteAccessor;
import com.bt.nia.koala.robustness.commands.RegisterKernelRamdiskScenarioCommand.RegistrationType;

public class DeregisterKernelRamdiskScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(DeregisterKernelRamdiskScenarioCommand.class);

	private final String usernameParamsKey;
	private final String imageIdKey;
	private final RegistrationType registrationType;

	public DeregisterKernelRamdiskScenarioCommand(ScenarioRunDetails runDetails, String usernameParamsKey, String imageIdKey, RegistrationType registrationType) {
		super(runDetails);
		this.usernameParamsKey = usernameParamsKey;
		this.imageIdKey = imageIdKey;
		this.registrationType = registrationType;
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String username = (String) params.get(usernameParamsKey);
		String imageId = (String) params.get(imageIdKey);

		if (registrationType == RegistrationType.KERNEL) {
			OpsWebsiteAccessor.deleteKernel(username, imageId);
		} else {
			OpsWebsiteAccessor.deleteRamdisk(username, imageId);
		}
	}
}
