/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import java.util.Collection;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.api.entities.ReservationInstances;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZoneNotFoundException;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.net.utils.IpAddressUtils;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.conf.Property;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;

@Component
public class RunInstancesServiceHelper extends ServiceHelperBase {
    private static final int NUMBER_OF_RESERVED_ADDRESSES_IN_SECURITY_GROUP = 3;

    private static final Log LOG = LogFactory.getLog(RunInstancesServiceHelper.class);

    private int instanceTaskQueueRetries;

    @Resource(name = "generalBlockingCache")
    private BlockingDhtCache instanceTypeCache;

    public RunInstancesServiceHelper() {
    }

    @Property(key = "instance.task.queue.retries", defaultValue = "5")
    public void setInstanceTaskQueueRetries(int num) {
        instanceTaskQueueRetries = num;
    }

    public int getInstanceTaskQueueRetries() {
        return instanceTaskQueueRetries;
    }

    public ReservationInstances runInstances(final Reservation reservation) {
        LOG.debug(String.format("runInstances(%s)", reservation));

        String securityGroupUrl = SecurityGroup.getUrl(reservation.getUserId(), reservation.getSecurityGroupName());
        PId securityGroupId = getPiIdBuilder().getPId(securityGroupUrl).forLocalRegion();
        SecurityGroup securityGroup = (SecurityGroup) getDhtClientFactory().createBlockingReader().get(securityGroupId);

        validateReservation(reservation, securityGroup);

        reservation.setReservationId(getIdFactory().createNewReservationId());

        AvailabilityZone availabilityZone;
        if (StringUtils.isNotEmpty(reservation.getAvailabilityZone())) {
            try {
                availabilityZone = getAvailabilityZoneByName(reservation.getAvailabilityZone());
            } catch (AvailabilityZoneNotFoundException e) {
                throw new IllegalArgumentException(String.format("Unknown availability zone: %s", reservation.getAvailabilityZone()));
            }
        } else {
            availabilityZone = getLocalAvailabilityZone();
            reservation.setAvailabilityZone(availabilityZone.getAvailabilityZoneName());
        }

        // setup return object
        ReservationInstances reservationInstances = new ReservationInstances();
        reservationInstances.setReservation(reservation);

        for (int i = 0; i < reservation.getMaxCount(); i++) {
            String instanceId = getIdFactory().createNewInstanceId(availabilityZone.getGlobalAvailabilityZoneCode());
            // create instance
            Instance instance = new Instance(reservation);
            instance.setInstanceType(reservation.getInstanceType());
            instance.setUserId(reservation.getUserId());
            instance.setInstanceId(instanceId);
            instance.setState(InstanceState.PENDING);
            instance.setLaunchTime(System.currentTimeMillis());
            instance.setAvailabilityZoneCode(availabilityZone.getAvailabilityZoneCodeWithinRegion());
            instance.setRegionCode(availabilityZone.getRegionCode());
            LOG.info(String.format("Requesting new %s", instance));
            reservationInstances.getInstances().add(instance);

            // create instance in dht
            PId instanceDhtId = getPiIdBuilder().getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId));
            BlockingDhtWriter blockingDhtWriter = getDhtClientFactory().createBlockingWriter();
            AddNewInstanceResolver addNewInstanceResolver = new AddNewInstanceResolver();
            blockingDhtWriter.update(instanceDhtId, instance, addNewInstanceResolver);

            reservation.addInstanceId(instance.getInstanceId());
        }

        getUserService().addInstancesToUser(reservation.getUserId(), reservation.getInstanceIds(), reservation.getInstanceType());

        // write security group to DHT
        getDhtClientFactory().createBlockingWriter().update(securityGroupId, null, new AddInstanceToSecurityGroupResolver(reservation.getInstanceIds()));

        // add to task processing queue
        PId runInstanceQueueId = getPiIdBuilder().getPId(PiQueue.RUN_INSTANCE.getUrl()).forGlobalAvailablityZoneCode(availabilityZone.getGlobalAvailabilityZoneCode());
        for (String instanceId : reservation.getInstanceIds()) {
            getTaskProcessingQueueHelper().addUrlToQueue(runInstanceQueueId, Instance.getUrl(instanceId), instanceTaskQueueRetries);
        }

        // anycast message
        PubSubMessageContext pubSubMessageContext = getApiApplicationManager().newPubSubMessageContextFromGlobalAvzCode(PiTopics.RUN_INSTANCE, availabilityZone.getGlobalAvailabilityZoneCode());
        pubSubMessageContext.randomAnycast(EntityMethod.CREATE, reservation);

        return reservationInstances;
    }

    private void validateReservation(Reservation reservation, SecurityGroup securityGroup) {
        LOG.debug(String.format("validateReservation(%s, %s)", reservation, securityGroup));
        String imageId = reservation.getImageId();

        if (StringUtils.isBlank(imageId))
            throw new IllegalArgumentException("image Id must be supplied");

        Image image = readImage(imageId);
        checkSecurityGroup(reservation);
        InstanceTypeConfiguration instanceTypeConfiguration = checkInstanceType(reservation);
        checkAllImagesSupplied(reservation, image);
        checkImagesAccess(reservation, image);
        checkImageState(image);
        checkKernelStateAndType(reservation, image);
        checkRamdiskStateAndType(reservation, image);
        checkKeyPair(reservation);
        checkUserInstancesCount(reservation);
        checkUserCoreCount(reservation, instanceTypeConfiguration);
        checkSecurityGroupCapacity(reservation, securityGroup);
    }

    private void checkUserCoreCount(Reservation reservation, InstanceTypeConfiguration instanceTypeConfiguration) {
        if (null == reservation.getInstanceType() || null == instanceTypeConfiguration)
            return;

        int requestedCores = instanceTypeConfiguration.getNumCores();
        LOG.debug(String.format("instance %s cores %d", instanceTypeConfiguration.getInstanceType(), instanceTypeConfiguration.getNumCores()));
        int currentCores = getCurrentCores(reservation.getUserId());
        LOG.debug(String.format("user %s current cores %d", reservation.getUserId(), currentCores));
        int maxCores = getMaxCores(reservation.getUserId());
        LOG.debug(String.format("user %s max cores %d", reservation.getUserId(), maxCores));

        if (currentCores + requestedCores > maxCores)
            throw new IllegalStateException(String.format("Unable to run instances as user %s currently has %s cores when the maximum is %s.", reservation.getUserId(), currentCores, maxCores));
    }

    private int getMaxCores(String userId) {
        return getUserService().getMaxCores(userId);
    }

    private int getCurrentCores(String userId) {
        return getUserService().getCurrentCores(userId);
    }

    private InstanceTypeConfiguration checkInstanceType(Reservation reservation) {
        if (null == reservation.getInstanceType())
            return null;

        PId instanceTypesId = getPiIdBuilder().getPId(InstanceTypes.URL_STRING);
        InstanceTypes instanceTypes = instanceTypeCache.get(instanceTypesId);

        InstanceTypeConfiguration result = instanceTypes.getInstanceTypeConfiguration(reservation.getInstanceType());
        if (null == result)
            throw new IllegalStateException("Unable to find instance type:" + reservation.getInstanceType());
        if (result.isDeprecated())
            throw new IllegalStateException(String.format("Instance type %s is deprecated", reservation.getInstanceType()));
        return result;
    }

    private void checkSecurityGroupCapacity(Reservation reservation, SecurityGroup securityGroup) {
        LOG.debug(String.format("checking security group: %s", securityGroup));

        int numberOfInstancesInSecurityGroup = securityGroup.getInstances().size();

        if (numberOfInstancesInSecurityGroup == 0) {
            LOG.debug(String.format("No instances in security group: %s", securityGroup));
            return;
        }

        if (securityGroup.getNetmask() == null) {
            LOG.debug(String.format("Unable to get the netmask for security group: %s", securityGroup));
            return;
        }

        int availableNumberOfInstancesInSecurityGroup = getSecurityGroupCapacity(securityGroup);

        LOG.debug(String.format("SecurityGroup: %s - Number of Instances: %d - Capacity in SecurityGroup: %d", securityGroup.getUrl(), numberOfInstancesInSecurityGroup, availableNumberOfInstancesInSecurityGroup));

        if (numberOfInstancesInSecurityGroup + reservation.getMinCount() > availableNumberOfInstancesInSecurityGroup - NUMBER_OF_RESERVED_ADDRESSES_IN_SECURITY_GROUP)
            throw new IllegalStateException("unable to run instances as no more capacity in security group");
    }

    private int getSecurityGroupCapacity(SecurityGroup securityGroup) {
        String netmask = securityGroup.getNetmask();
        int addrsInSlashnet = IpAddressUtils.addrsInSlashnet(IpAddressUtils.netmaskToSlashnet(netmask));
        return addrsInSlashnet;
    }

    private void checkUserInstancesCount(Reservation reservation) {
        User u = getUserManagementService().getUser(reservation.getUserId());
        if (u.getInstanceIds().length + reservation.getMaxCount() > u.getMaxInstances()) {
            throw new IllegalStateException(String.format("Unable to run instances as user %s currently has %s instances when the maximum is %s.", u.getUsername(), u.getInstanceIds().length, u.getMaxInstances()));
        }
    }

    private void checkSecurityGroup(Reservation reservation) {
        if (StringUtils.isBlank(reservation.getSecurityGroupName()))
            return;

        User user = getUserManagementService().getUser(reservation.getUserId());

        if (!user.getSecurityGroupIds().contains(reservation.getSecurityGroupName()))
            throw new IllegalArgumentException(String.format("Security group %s does not exist for user %s", reservation.getSecurityGroupName(), user.getUsername()));
    }

    private void checkKeyPair(Reservation reservation) {
        if (StringUtils.isBlank(reservation.getKeyName()))
            return;

        User user = getUserManagementService().getUser(reservation.getUserId());
        if (user.getKeyPair(reservation.getKeyName()) == null)
            throw new IllegalArgumentException(String.format("User %s does not have key pair \"%s\"", reservation.getUserId(), reservation.getKeyName()));
    }

    private void checkKernelStateAndType(Reservation reservation, Image image) {
        LOG.debug(String.format("checkKernelStateAndType(%s, %s)", reservation, image));
        String kernelId = image.getKernelId();
        if (StringUtils.isNotBlank(reservation.getKernelId()))
            kernelId = reservation.getKernelId();
        if (StringUtils.isBlank(kernelId))
            return;
        Image kernelImage = readImage(kernelId);
        if (!ImageState.AVAILABLE.equals(kernelImage.getState()))
            throw new IllegalArgumentException(String.format("kernel %s must be in state %s", kernelId, ImageState.AVAILABLE));
        if (MachineType.KERNEL.equals(kernelImage.getMachineType()))
            return;
        throw new IllegalArgumentException(String.format("kernel %s must be a %s machine type", kernelId, MachineType.KERNEL));
    }

    private void checkRamdiskStateAndType(Reservation reservation, Image image) {
        LOG.debug(String.format("checkRamdiskStateAndType(%s, %s)", reservation, image));
        String ramdiskId = image.getRamdiskId();
        if (StringUtils.isNotBlank(reservation.getRamdiskId()))
            ramdiskId = reservation.getRamdiskId();
        if (StringUtils.isBlank(ramdiskId))
            return;
        Image ramdiskImage = readImage(ramdiskId);
        if (!ImageState.AVAILABLE.equals(ramdiskImage.getState()))
            throw new IllegalArgumentException(String.format("ramdisk %s must be in state %s", ramdiskId, ImageState.AVAILABLE));
        if (MachineType.RAMDISK.equals(ramdiskImage.getMachineType()))
            return;
        throw new IllegalArgumentException(String.format("ramdisk %s must be a %s machine type", ramdiskId, MachineType.RAMDISK));
    }

    private void checkAllImagesSupplied(Reservation reservation, Image image) {
        LOG.debug(String.format("checkAllImagesSupplied(%s, %s)", reservation, image));
        if (ImagePlatform.windows.equals(image.getPlatform()))
            return;
        if (StringUtils.isBlank(reservation.getKernelId()) && StringUtils.isBlank(image.getKernelId()))
            throw new IllegalArgumentException("You must supply a kernel as the image has no default one");
        if (ImagePlatform.linux.equals(image.getPlatform()))
            return;
        if (ImagePlatform.opensolaris.equals(image.getPlatform()))
            return;
        if (StringUtils.isBlank(reservation.getRamdiskId()) && StringUtils.isBlank(image.getRamdiskId()))
            throw new IllegalArgumentException("You must supply a ramdisk as the image has no default one");
    }

    private void checkImagesAccess(Reservation reservation, Image image) {
        LOG.debug(String.format("checkImagesAccess(%s, %s)", reservation, image));
        String userId = reservation.getUserId();
        User user = getUserManagementService().getUser(userId);

        checkImageAccess(reservation.getImageId(), user, "image");
        checkImageAccess(reservation.getKernelId(), user, "kernel");
        checkImageAccess(reservation.getRamdiskId(), user, "ramdisk");
    }

    private void checkImageState(Image image) {
        LOG.debug(String.format("checkImageState(%s)", image));
        if (ImageState.AVAILABLE.equals(image.getState()))
            return;
        throw new IllegalArgumentException(String.format("image %s must be in state %s", image.getImageId(), ImageState.AVAILABLE));
    }

    private Image readImage(String imageId) {
        LOG.debug(String.format("readImage(%s)", imageId));
        PId imagePastryId = getPiIdBuilder().getPId(Image.getUrl(imageId));
        Image image = (Image) getDhtClientFactory().createBlockingReader().get(imagePastryId);
        if (null == image)
            throw new IllegalArgumentException(String.format("image %s not found", imageId));
        return image;
    }

    static final class AddInstanceToSecurityGroupResolver implements UpdateResolver<SecurityGroup> {
        private final Collection<String> reservationInstanceIds;

        private AddInstanceToSecurityGroupResolver(Collection<String> instanceIds) {
            this.reservationInstanceIds = instanceIds;
        }

        public Collection<String> getReservationInstanceIds() {
            return this.reservationInstanceIds;
        }

        @Override
        public SecurityGroup update(SecurityGroup existingEntity, SecurityGroup requestedEntity) {
            if (null == existingEntity)
                return null;

            for (String instanceId : reservationInstanceIds) {
                existingEntity.getInstances().put(instanceId, new InstanceAddress());
            }

            return existingEntity;
        }
    }

    /**
     * Ensure entry is unique
     */
    static final class AddNewInstanceResolver implements UpdateResolver<PiEntity> {
        public AddNewInstanceResolver() {
        }

        public PiEntity update(PiEntity previousEntry, PiEntity newEntry) {
            if (null == previousEntry) {
                return newEntry;
            }
            return null;
        }
    }
}
