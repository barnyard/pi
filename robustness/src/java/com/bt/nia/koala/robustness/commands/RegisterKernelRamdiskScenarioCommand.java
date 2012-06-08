package com.bt.nia.koala.robustness.commands;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.bt.nia.koala.robustness.OpsWebsiteAccessor;
import com.bt.nia.koala.robustness.commands.ImageDetails.ImageType;

public class RegisterKernelRamdiskScenarioCommand extends Ec2ScenarioCommandBase {
	private static final Log log = LogFactory.getLog(RegisterKernelRamdiskScenarioCommand.class);

	private final String usernameParamsKey;
	private final String manifestLocationKey;
	private final String imageIdKey;
	private final RegistrationType registrationType;

	private final ScenarioRunDetails runDetails;

	public static enum RegistrationType {
		KERNEL, RAMDISK;

		public static RegistrationType valueOf(ImageType imageType) {
			if (imageType.equals(ImageType.KERNEL))
				return KERNEL;
			if (imageType.equals(ImageType.RAMDISK))
				return RAMDISK;
			throw new IllegalArgumentException("Unsupported image type");
		}
	}

	public RegisterKernelRamdiskScenarioCommand(ScenarioRunDetails runDetails, String manifestLocationKey, String usernameParamsKey, String imageIdKey, RegistrationType registrationType) {
		super(runDetails);
		this.runDetails = runDetails;
		this.manifestLocationKey = manifestLocationKey;
		this.usernameParamsKey = usernameParamsKey;
		this.imageIdKey = imageIdKey;
		this.registrationType = registrationType;
	}

	@Override
	public void execute(Map<String, Object> params) throws Throwable {
		String username = (String) params.get(usernameParamsKey);
		String manifestLocation = (String) params.get(manifestLocationKey);
		log.debug("registering " + manifestLocation + " for " + username + " as " + registrationType.toString());

		// this is a dirty, nasty hacky thing...
		// added to enforce a chance for gluster to propogate its data
		Thread.sleep(10000);

		if (registrationType == RegistrationType.KERNEL) {
			params.put(imageIdKey, OpsWebsiteAccessor.registerKernel(username, manifestLocation));
		} else {
			params.put(imageIdKey, OpsWebsiteAccessor.registerRamdisk(username, manifestLocation));
		}
		log.debug("storing registered image id " + params.get(imageIdKey) + " in " + imageIdKey);
	}

	@Override
	protected Ec2ScenarioCommandBase getCompensationCommand() {
		return new DeregisterKernelRamdiskScenarioCommand(runDetails, usernameParamsKey, imageIdKey, registrationType);
	}
}
