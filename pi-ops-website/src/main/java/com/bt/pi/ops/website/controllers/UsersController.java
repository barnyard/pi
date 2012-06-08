package com.bt.pi.ops.website.controllers;

import java.net.URI;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Resource;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.api.service.CertificateGenerationException;
import com.bt.pi.api.service.UserManagementService;
import com.bt.pi.api.service.UserNotFoundException;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.instancemanager.watchers.UsersInstanceValidationWatcher;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.id.PId;
import com.bt.pi.ops.website.entities.ReadOnlyUser;
import com.bt.pi.ops.website.exception.CannotCreateException;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.client.ClientResponse.Status;
import com.sun.jersey.api.view.Viewable;

@Component
@Path("/users")
public class UsersController extends ControllerBase {
	private static final String SLASH_INSTANCEVALIDATION = "/instancevalidation";
	private static final String TRUE = "true";
	private static final String USER_DOES_NOT_EXIST = "User does not exist";
	private static final String USER_HAS_BEEN_DELETED = "User has been deleted";
	private static final String UNABLE_TO_CREATE_CERTIFICATE_FOR_USER = "Unable to create certificate for user";
	private static final String SLASH_USERNAME = "/{username}";
	private static final String ENABLED = "enabled";
	private static final String EMAIL = "email";
	private static final String REALNAME = "realname";
	private static final String USERNAME = "username";
	private static final String ACCESS_KEY = "accesskey";
	private static final String SLASH_ACCESS_KEYS = "/accesskeys/{" + ACCESS_KEY + "}";
	private static final String CERTIFICATE_PATH = SLASH_USERNAME + "/certificate";
	private static final String EXTERNAL_REF_ID = "externalrefid";
	private static final String APPLICATION_ZIP = "application/zip";
	private static final String CHECKBOX_FIELD = "checkbox";
	private static final String MAX_INSTANCES = "maxInstances";
	private static final String MAX_CORES = "maxCores";
	private static final String UNCHANGED = "UNCHANGED";
	private static final String DEFAULT_OPS_WEBSITE_DNS_NAME = "ops.fr001.baynard.cloud12cn.com";
	private static final Log LOG = LogFactory.getLog(UsersController.class);
	private UserManagementService userManagementService;
	private String opsWebsiteDnsName = DEFAULT_OPS_WEBSITE_DNS_NAME;
	@Resource
	private UsersInstanceValidationWatcher usersInstanceValidationWatcher;

	public UsersController() {
		userManagementService = null;
		usersInstanceValidationWatcher = null;
	}

	@Resource
	public void setUserManagementService(UserManagementService theUserManagementService) {
		userManagementService = theUserManagementService;
	}

	@Property(key = "ops.website.dns.name", defaultValue = DEFAULT_OPS_WEBSITE_DNS_NAME)
	public void setOpsWebsiteDnsName(String value) {
		this.opsWebsiteDnsName = value;
	}

	@POST
	public Response postUser(@FormParam(USERNAME) String username, @FormParam(REALNAME) String realname, @FormParam(EMAIL) String emailAddress, @FormParam(ENABLED) String enabled,
			@FormParam(EXTERNAL_REF_ID) String externalRefId, @FormParam(MAX_INSTANCES) String maxInstancesStr, @FormParam(MAX_CORES) String maxCoresStr) {
		LOG.info(String.format("Creating a new user: username %s, real name %s, email %s, enabled %s, external ref id %s, maxInstances %s, maxCores %s", username, realname, emailAddress, enabled,
				externalRefId, maxInstancesStr, maxCoresStr));

		boolean isEnabled = validateAndGetEnabledBoolean(enabled);
		checkNotNullOrEmpty(username, realname, emailAddress);
		Integer maxInstances = validateAndGetMaxInstances(maxInstancesStr);
		Integer maxCores = validateAndGetMaxCores(maxCoresStr);

		User user = new User(username.toLowerCase(Locale.getDefault()), realname, emailAddress, isEnabled, externalRefId);
		if (null == maxInstances || maxInstances > -1)
			user.setMaxInstances(maxInstances);
		if (null == maxCores || maxCores > -1)
			user.setMaxCores(maxCores);

		if (!userManagementService.addUser(user)) {
			throw new CannotCreateException("Unable to create user");
		}

		return Response.created(URI.create(username.toLowerCase(Locale.getDefault()))).status(Status.CREATED).entity(getViewable(getUser(username))).build();
	}

	private boolean validateAndGetEnabledBoolean(String enabled) {
		if (!StringUtils.isEmpty(enabled) && !CHECKBOX_FIELD.equals(enabled) && !TRUE.equals(enabled))
			throw new IllegalArgumentException("Bad argument value for enabled attribute");
		boolean isEnabled = CHECKBOX_FIELD.equals(enabled) || TRUE.equals(enabled);
		return isEnabled;
	}

	private Integer validateAndGetMaxInstances(String maxInstances) {
		LOG.debug(String.format("validateAndGetMaxInstances(%s)", maxInstances));
		return validateMax(maxInstances, MAX_INSTANCES);
	}

	private Integer validateAndGetMaxCores(String maxCores) {
		LOG.debug(String.format("validateAndGetMaxCores(%s)", maxCores));
		return validateMax(maxCores, MAX_CORES);
	}

	private Integer validateMax(String max, String name) {
		String message = name + " must be a valid number greater than -1";
		int result = -1;
		if (StringUtils.isBlank(max))
			return null;
		if (UNCHANGED.equals(max))
			return -1;
		try {
			result = Integer.parseInt(max);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException(message);
		}
		if (result < 0)
			throw new IllegalArgumentException(message);
		return result;
	}

	@PUT
	@Path(SLASH_USERNAME)
	public Viewable putUser(@PathParam(USERNAME) String username, @FormParam(REALNAME) String realname, @FormParam(EMAIL) String emailAddress, @FormParam(ENABLED) String enabled,
			@FormParam(EXTERNAL_REF_ID) String externalRefId, @FormParam(MAX_INSTANCES) String maxInstancesStr, @FormParam(MAX_CORES) String maxCoresStr) {
		LOG.info(String.format("Updating a new user: username %s, real name %s, email %s, enabled %s, external ref id %s, maxInstances %s, maxCores %s", username, realname, emailAddress, enabled,
				externalRefId, maxInstancesStr, maxCoresStr));
		Boolean isEnabled = null;

		if (!StringUtils.isEmpty(enabled))
			isEnabled = validateAndGetEnabledBoolean(enabled);

		Integer maxInstances = validateAndGetMaxInstances(maxInstancesStr);
		Integer maxCores = validateAndGetMaxCores(maxCoresStr);
		try {
			userManagementService.updateUser(username, realname, emailAddress, isEnabled, externalRefId, maxInstances, maxCores);
		} catch (UserNotFoundException ex) {
			throw new NotFoundException("");
		}

		return getViewable(getUser(username));
	}

	@Path("/view" + SLASH_USERNAME)
	@Produces(MediaType.TEXT_HTML)
	@GET
	public Viewable getUserHtml(@PathParam(USERNAME) String username) {
		return getViewable(getUser(username));
	}

	protected Viewable getViewable(ReadOnlyUser user, String templateName) {
		Map<String, Object> model = new HashMap<String, Object>();
		model.put("user", user);
		model.put("instanceIds", user.getInstanceIds());
		model.put("imageIds", user.getImageIds());
		model.put("securityGroupIds", user.getSecurityGroupIds());
		model.put("volumeIds", user.getVolumeIds());
		model.put(ENABLED, user.isEnabled());
		model.put(MAX_INSTANCES, user.getMaxInstances());
		model.put(MAX_CORES, user.getMaxCores());
		return new Viewable(templateName, model);
	}

	private Viewable getViewable(ReadOnlyUser user) {
		return getViewable(user, "single_user");
	}

	@Path(SLASH_USERNAME)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@GET
	public ReadOnlyUser getUser(@PathParam(USERNAME) String username) {
		checkNotNullOrEmpty(username);

		LOG.debug("Getting user " + username);

		try {
			User user = userManagementService.getUser(username);
			if (user.isDeleted()) {
				throw new NotFoundException(USER_HAS_BEEN_DELETED);
			}
			return new ReadOnlyUser(user);
		} catch (UserNotFoundException e) {
			throw new NotFoundException(USER_DOES_NOT_EXIST);
		}
	}

	@Path(SLASH_ACCESS_KEYS)
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	@GET
	public ReadOnlyUser getUserByApiAccessKey(@PathParam(ACCESS_KEY) String accessKey) {
		checkNotNullOrEmpty(accessKey);

		LOG.debug(String.format("Getting user with access key %s", accessKey));

		try {
			User user = userManagementService.getUserByApiAccessKey(accessKey);
			if (user.isDeleted()) {
				throw new NotFoundException(USER_HAS_BEEN_DELETED);
			}
			return new ReadOnlyUser(user);
		} catch (UserNotFoundException e) {
			throw new NotFoundException(USER_DOES_NOT_EXIST);
		}
	}

	@Path(SLASH_ACCESS_KEYS)
	@Produces(MediaType.TEXT_HTML)
	@GET
	public Viewable getUserByApiAccessKeyHtml(@PathParam(ACCESS_KEY) String accessKey) {
		return getViewable(getUserByApiAccessKey(accessKey));
	}

	@DELETE
	@Path(SLASH_USERNAME)
	public Response deleteUser(@PathParam(USERNAME) String username) {
		checkNotNullOrEmpty(username);
		LOG.debug("Deleting user " + username);
		userManagementService.deleteUser(username);
		return Response.noContent().build();
	}

	@GET
	@Path(CERTIFICATE_PATH)
	@Produces(MediaType.TEXT_HTML)
	public Viewable getUserCertificate(@PathParam(USERNAME) String username) {
		checkNotNullOrEmpty(username);

		return buildModelAndView("post_user_cert", USERNAME, username);
	}

	@POST
	@Path(CERTIFICATE_PATH)
	@Produces(APPLICATION_ZIP)
	public Response postUserCertificate(@PathParam(USERNAME) String username) {
		LOG.info("Creating a new certificate for user " + username);

		byte[] cert;
		try {
			cert = userManagementService.updateUserCertificate(username);
		} catch (CertificateGenerationException e) {
			LOG.warn(UNABLE_TO_CREATE_CERTIFICATE_FOR_USER, e);
			throw new CannotCreateException(UNABLE_TO_CREATE_CERTIFICATE_FOR_USER + username);
		}

		Response response = Response.ok(cert).build();
		return response;
	}

	@POST
	@Path("/{username}/pi-certs.zip")
	@Produces(APPLICATION_ZIP)
	public Response postUserCertificateWithFilename(@PathParam(USERNAME) String username) {
		return postUserCertificate(username);
	}

	@POST
	@Path("/disable" + SLASH_USERNAME)
	public Viewable disableUser(@PathParam(USERNAME) String username) {
		userManagementService.setUserEnabledFlag(username, false);
		return getViewable(getUser(username));
	}

	@POST
	@Path("/enable" + SLASH_USERNAME)
	public Viewable enableUser(@PathParam(USERNAME) String username) {
		userManagementService.setUserEnabledFlag(username, true);
		return getViewable(getUser(username));
	}

	@GET
	@Path(SLASH_USERNAME + SLASH_INSTANCEVALIDATION)
	public Viewable getInstanceValidationAddress(@PathParam(USERNAME) String username) {
		LOG.debug(String.format("getInstanceValidationAddress(%s)", username));

		PId pId = getPiIdBuilder().getPId(User.getUrl(username));
		Map<String, Object> model = new HashMap<String, Object>();
		model.put(USERNAME, username);
		model.put("pid", pId.toStringFull());
		model.put("ops_website_dns_name", opsWebsiteDnsName);
		return new Viewable("instance_validation_address", model);
	}

	@POST
	@Path(SLASH_USERNAME + SLASH_INSTANCEVALIDATION)
	public Response sendInstanceValidationEmail(@PathParam(USERNAME) String username) {
		LOG.debug(String.format("sendInstanceValidationEmail(%s)", username));

		try {
			User user = userManagementService.getUser(username);
			if (user.isDeleted()) {
				throw new NotFoundException(USER_HAS_BEEN_DELETED);
			}
			this.usersInstanceValidationWatcher.sendEmail(user);
			return Response.ok().build();
		} catch (UserNotFoundException e) {
			throw new NotFoundException(USER_DOES_NOT_EXIST);
		}
	}
}
