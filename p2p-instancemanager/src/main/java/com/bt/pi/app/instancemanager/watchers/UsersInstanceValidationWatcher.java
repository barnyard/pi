package com.bt.pi.app.instancemanager.watchers;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceActivityState;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.instancemanager.handlers.PauseInstanceServiceHelper;
import com.bt.pi.app.instancemanager.handlers.TerminateInstanceServiceHelper;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.continuation.scattergather.ScatterGatherContinuationRunnable;
import com.bt.pi.core.continuation.scattergather.UpdateResolvingPiScatterGatherContinuation;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.mail.MailSender;
import com.bt.pi.core.util.template.TemplateHelper;

@Component
@DependsOn("watcherService")
public class UsersInstanceValidationWatcher extends AbstractWatcherRunnable {
    private static final String HANDLE_RESULT_S = "handleResult(%s)";
    private static final long TWO = 2L;
    private static final long FIVE = 5L;
    private static final long SIXTEEN = 16L;
    private static final long TWENTY_FOUR = 24L;
    private static final long TWENTY_EIGHT = 28L;
    private static final long SIXTY = 60L;
    private static final long ONE_THOUSAND = 1000L;
    private static final Log LOG = LogFactory.getLog(UsersInstanceValidationWatcher.class);
    private static final long ONE_DAY = TWENTY_FOUR * SIXTY * SIXTY * ONE_THOUSAND;
    private static final String DEFAULT_VALIDATION_MILLIS = "" + (TWENTY_EIGHT * ONE_DAY);
    private static final String DEFAULT_GRACE_MILLIS = "" + (SIXTEEN * ONE_DAY);
    private static final String DEFAULT_TERMINATE_MILLIS = "" + (FIVE * ONE_DAY);
    private static final String DEFAULT_VALIDATION_EMAIL_SUBJECT = "your instances need validating";
    private static final String DEFAULT_VALIDATION_EMAIL_TEMPLATE = "instancevalidation.email.ftl";
    private static final String DEFAULT_OPS_WEBSITE_DNS_NAME = "ops.fr001.baynard.cloud12cn.com";
    private static final long DEFAULT_INITIAL_MILLIS = 30 * 1000;
    private static final long DEFAULT_REPEATING_MILLIS = 6 * 60 * 60 * 1000;
    private long validationMillis = Long.parseLong(DEFAULT_VALIDATION_MILLIS);
    private long graceMillis = Long.parseLong(DEFAULT_GRACE_MILLIS);
    private long terminateMillis = Long.parseLong(DEFAULT_TERMINATE_MILLIS);
    private String subject;
    private String template = DEFAULT_VALIDATION_EMAIL_TEMPLATE;
    @Resource
    private TemplateHelper templateHelper;
    private String opsWebsiteDnsName = DEFAULT_OPS_WEBSITE_DNS_NAME;
    @Resource
    private LocalStorageUserHandler localStorageUserHandler;
    @Resource
    private MailSender mailSender;
    private long initialIntervalMillis = DEFAULT_INITIAL_MILLIS;
    private long repeatingIntervalMillis = DEFAULT_REPEATING_MILLIS;
    private boolean enabled = true;
    @Resource
    private TerminateInstanceServiceHelper terminateInstanceServiceHelper;
    @Resource
    private PauseInstanceServiceHelper pauseInstanceServiceHelper;

    public UsersInstanceValidationWatcher() {
        this.templateHelper = null;
        this.localStorageUserHandler = null;
        this.mailSender = null;
        this.terminateInstanceServiceHelper = null;
        this.pauseInstanceServiceHelper = null;
    }

    @PostConstruct
    public void postConstruct() {
        if (null != this.getWatcherService())
            this.getWatcherService().replaceTask(getClass().getSimpleName(), this, initialIntervalMillis, repeatingIntervalMillis);
    }

    @Property(key = "instance.validation.initial.interval.millis", defaultValue = "" + DEFAULT_INITIAL_MILLIS)
    public void setInitialIntervalMillis(long value) {
        this.initialIntervalMillis = value;
        postConstruct();
    }

    @Property(key = "instance.validation.repeating.interval.millis", defaultValue = "" + DEFAULT_REPEATING_MILLIS)
    public void setRepeatingIntervalMillis(long value) {
        this.repeatingIntervalMillis = value;
        postConstruct();
    }

    @Property(key = "instance.validation.email.millis", defaultValue = DEFAULT_VALIDATION_MILLIS)
    public void setValidationMillis(long value) {
        this.validationMillis = value;
    }

    @Property(key = "instance.validation.grace.millis", defaultValue = DEFAULT_GRACE_MILLIS)
    public void setGraceMillis(long value) {
        this.graceMillis = value;
    }

    @Property(key = "instance.validation.terminate.millis", defaultValue = DEFAULT_TERMINATE_MILLIS)
    public void setTerminateMillis(long value) {
        this.terminateMillis = value;
    }

    @Property(key = "instance.validation.email.subject", defaultValue = DEFAULT_VALIDATION_EMAIL_SUBJECT)
    public void setSubject(String s) {
        this.subject = s;
    }

    @Property(key = "instance.validation.email.template", defaultValue = DEFAULT_VALIDATION_EMAIL_TEMPLATE)
    public void setTemplate(String s) {
        this.template = s;
    }

    @Property(key = "ops.website.dns.name", defaultValue = DEFAULT_OPS_WEBSITE_DNS_NAME)
    public void setOpsWebsiteDnsName(String value) {
        this.opsWebsiteDnsName = value;
    }

    @Property(key = "instance.validation.enabled", defaultValue = "true")
    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    @Override
    public void run() {
        LOG.debug("run()");
        if (!enabled)
            return;
        LOG.debug(this.localStorageUserHandler.getUserPIds());
        for (PId userPId : this.localStorageUserHandler.getUserPIds()) {
            // TODO: use cache to get user?
            getDhtClientFactory().createReader().getAsync(userPId, new PiContinuation<User>() {
                @Override
                public void handleResult(final User result) {
                    LOG.debug(String.format(HANDLE_RESULT_S, result));
                    if (null == result)
                        return;
                    // to avoid blocking selector thread
                    getTaskExecutor().execute(new Runnable() {
                        @Override
                        public void run() {
                            processUserInstances(result);
                        }
                    });
                }
            });
        }
    }

    private void processUserInstances(User user) {
        LOG.debug(String.format("processUserInstances(%s)", user.getUsername()));

        final AtomicInteger expiredInstanceCount = new AtomicInteger();
        final List<Instance> instancesToBePaused = new ArrayList<Instance>();
        final List<String> instanceIdsToBeTerminated = new ArrayList<String>();
        List<ScatterGatherContinuationRunnable> runnables = new ArrayList<ScatterGatherContinuationRunnable>();

        final UpdateResolvingPiContinuation<Instance> continuation = new UpdateResolvingPiContinuation<Instance>() {
            @Override
            public Instance update(Instance existingEntity, Instance requestedEntity) {
                return processInstance(expiredInstanceCount, instancesToBePaused, instanceIdsToBeTerminated, existingEntity);
            }

            @Override
            public void handleResult(Instance result) {
                LOG.debug(String.format(HANDLE_RESULT_S, result));
                if (null != result) {
                    LOG.debug(String.format("Instance %s updated to activity state %s", result.getInstanceId(), result.getInstanceActivityState()));
                }
            }
        };

        for (final String instanceId : user.getInstanceIds()) {
            final PId id = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
            final UpdateResolvingPiScatterGatherContinuation<Instance> updateResolvingPiScatterGatherContinuation = new UpdateResolvingPiScatterGatherContinuation<Instance>(continuation);
            runnables.add(new ScatterGatherContinuationRunnable(updateResolvingPiScatterGatherContinuation) {
                @Override
                public void run() {
                    getDhtClientFactory().createWriter().update(id, updateResolvingPiScatterGatherContinuation);
                }
            });
        }

        LOG.debug(String.format("runnables size: %s", runnables.size()));
        getScatterGatherContinuationRunner().execute(runnables, TWO, TimeUnit.MINUTES);

        for (Instance instance : instancesToBePaused) {
            pauseInstanceServiceHelper.pauseInstance(instance);
        }

        if (instanceIdsToBeTerminated.size() > 0) {
            LOG.debug(String.format("terminating instances %s", instanceIdsToBeTerminated));
            terminateInstanceServiceHelper.terminateInstance(user.getUsername(), instanceIdsToBeTerminated);
        }

        if (expiredInstanceCount.get() > 0) {
            sendEmail(user);
        }
    }

    public void sendEmail(User user) {
        LOG.debug(String.format("sendEmail(%s)", user));
        String text = generateEmailText(user);
        mailSender.send(user.getEmailAddress(), subject, text);
    }

    private Instance processInstance(final AtomicInteger expiredInstanceCount, final List<Instance> instancesToBePaused, final List<String> instanceIdsToBeTerminated, Instance existingEntity) {
        LOG.debug(String.format("processInstance(%s, %s, %s, %s)", expiredInstanceCount, instancesToBePaused, instanceIdsToBeTerminated, existingEntity));
        if (null == existingEntity) {
            LOG.warn("instance not found");
            return null;
        }
        if (!InstanceState.RUNNING.equals(existingEntity.getState())) {
            LOG.debug(String.format("Instance %s is not RUNNING", existingEntity.getInstanceId()));
            return null;
        }
        LOG.debug(String.format("Instance %s is %s since %s", existingEntity.getInstanceId(), existingEntity.getInstanceActivityState(), new Date(existingEntity.getInstanceActivityStateChangeTimestamp())));
        switch (existingEntity.getInstanceActivityState()) {
        case GREEN:
            return processGreenInstance(expiredInstanceCount, existingEntity);
        case AMBER:
            return processAmberInstance(instancesToBePaused, existingEntity);
        case RED:
            return processRedInstance(instanceIdsToBeTerminated, existingEntity);

        default:
            return null;
        }
    }

    private Instance processGreenInstance(final AtomicInteger expiredInstanceCount, Instance existingEntity) {
        if (System.currentTimeMillis() - existingEntity.getInstanceActivityStateChangeTimestamp() > validationMillis) {
            LOG.info(String.format("Instance %s is %s and ready for re-validation", existingEntity.getInstanceId(), existingEntity.getInstanceActivityState()));
            expiredInstanceCount.incrementAndGet();
            existingEntity.setInstanceActivityState(InstanceActivityState.AMBER);
            return existingEntity;
        }
        return null;
    }

    private Instance processAmberInstance(final List<Instance> instancesToBePaused, Instance existingEntity) {
        if (System.currentTimeMillis() - existingEntity.getInstanceActivityStateChangeTimestamp() > graceMillis) {
            LOG.info(String.format("Instance %s is %s and ready for pausing", existingEntity.getInstanceId(), existingEntity.getInstanceActivityState()));
            existingEntity.setInstanceActivityState(InstanceActivityState.RED);
            instancesToBePaused.add(existingEntity);
            return existingEntity;
        }
        return null;
    }

    private Instance processRedInstance(final List<String> instanceIdsToBeTerminated, Instance existingEntity) {
        if (System.currentTimeMillis() - existingEntity.getInstanceActivityStateChangeTimestamp() > terminateMillis) {
            LOG.info(String.format("Instance %s is %s and ready for termination", existingEntity.getInstanceId(), existingEntity.getInstanceActivityState()));
            instanceIdsToBeTerminated.add(existingEntity.getInstanceId());
        }
        return null;
    }

    private String generateEmailText(User user) {
        LOG.debug(String.format("generateEmailText(%s)", user));
        Map<String, Object> model = new HashMap<String, Object>();
        model.put("user", user);
        model.put("userPid", getPiIdBuilder().getPId(user).toStringFull());
        model.put("ops_website_dns_name", opsWebsiteDnsName);
        return templateHelper.generate(template, model);
    }
}
