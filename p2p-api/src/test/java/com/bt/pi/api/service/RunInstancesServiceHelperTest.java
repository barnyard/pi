/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.bt.pi.api.entities.ReservationInstances;
import com.bt.pi.api.service.RunInstancesServiceHelper.AddInstanceToSecurityGroupResolver;
import com.bt.pi.api.service.RunInstancesServiceHelper.AddNewInstanceResolver;
import com.bt.pi.api.utils.IdFactory;
import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZoneNotFoundException;
import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.ImageIndex;
import com.bt.pi.app.common.entities.ImageState;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceAddress;
import com.bt.pi.app.common.entities.InstanceState;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.app.common.entities.MachineType;
import com.bt.pi.app.common.entities.NetworkRule;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Reservation;
import com.bt.pi.app.common.entities.SecurityGroup;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.app.common.resource.PiTopics;
import com.bt.pi.core.application.PubSubMessageContext;
import com.bt.pi.core.application.watcher.task.TaskProcessingQueueHelper;
import com.bt.pi.core.continuation.UpdateResolver;
import com.bt.pi.core.dht.BlockingDhtReader;
import com.bt.pi.core.dht.BlockingDhtWriter;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.entity.EntityMethod;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.KoalaIdFactory;
import com.bt.pi.core.id.PId;

@RunWith(MockitoJUnitRunner.class)
public class RunInstancesServiceHelperTest {
    private static final String AVAILABILITY_ZONE_1_NAME = "availabilityZone1";
    private static final int AVAILABILITY_ZONE_1_CODE = 10;
    private static final String REGION_ZONE_1_NAME = "Region1";
    private static final int REGION_ZONE_1_CODE = 1;
    private static final String VALID_SECURITY_GROUP_2 = "validsg2";
    private static final String INVALID_SECURITY_GROUP = "invalidsg";
    private static final String VALID_SECURITY_GROUP = "validsg";
    private static final String INSTANCE_TYPE = "x1.valid";

    private String ownerId = "ownerId";
    private String imageId = "img-123";
    private String instanceId1 = "i-123";
    private String instanceId2 = "i-456";
    private String reservationId = "r-88899900";
    private String manifestLocation = "bucketName/manifest.xml";
    private String kernelId = "k-123";
    private String ramdiskId = "r-123";
    private String keyName = "key";

    private Reservation reservation;
    private User user;
    private ImageIndex imageIndex;
    private InstanceTypes instanceTypes;

    @Mock
    private ApiApplicationManager apiApplicationManager;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private BlockingDhtReader blockingDhtReader;
    @Mock
    private BlockingDhtWriter blockingDhtWriter;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private UserService userService;
    @Mock
    private UserManagementService userManagementService;
    @Mock
    private PubSubMessageContext pubSubMessageContext;
    @Mock
    private PId id123;
    @Mock
    private PId id456;
    @Mock
    private PId reservationPastryId;
    @Mock
    private TaskProcessingQueueHelper taskProcessingQueueHelper;
    @Mock
    private Image image;
    @Mock
    private Image kernel;
    @Mock
    private Image ramdisk;
    @Mock
    private PId imageIndexId;
    @Mock
    private IdFactory idFactory;
    @Mock
    private PId imagePastryId;
    @Mock
    private PId kernelPastryId;
    @Mock
    private PId ramdiskPastryId;
    @Mock
    private PId securityGroupId;
    @Mock
    private KoalaIdFactory koalaIdFactory;
    @Mock
    private PId instanceTypesId;
    @Mock
    private BlockingDhtCache instanceTypesCache;
    @Mock
    private PId runInstanceQueueId;

    @InjectMocks
    private RunInstancesServiceHelper runInstancesServiceHelper = new RunInstancesServiceHelper() {
        @SuppressWarnings("unused")
        protected ImageIndex getImageIndex() {
            return imageIndex;
        }
    };

    @Before
    public void setup() {
        AvailabilityZone avz = new AvailabilityZone(AVAILABILITY_ZONE_1_NAME, AVAILABILITY_ZONE_1_CODE, REGION_ZONE_1_CODE, "rockin");

        when(idFactory.createNewInstanceId(avz.getGlobalAvailabilityZoneCode())).thenReturn(instanceId1).thenReturn(instanceId2);
        when(idFactory.createNewReservationId()).thenReturn(reservationId);

        when(koalaIdFactory.getAvailabilityZoneWithinRegion()).thenReturn(AVAILABILITY_ZONE_1_CODE);
        when(koalaIdFactory.getRegion()).thenReturn(REGION_ZONE_1_CODE);
        when(koalaIdFactory.getGlobalAvailabilityZoneCode()).thenReturn(avz.getGlobalAvailabilityZoneCode());

        when(apiApplicationManager.getAvailabilityZoneByName(AVAILABILITY_ZONE_1_NAME)).thenReturn(avz);
        when(apiApplicationManager.getAvailabilityZoneByName("unknown")).thenThrow(new AvailabilityZoneNotFoundException("oops"));
        when(apiApplicationManager.getAvailabilityZoneByGlobalAvailabilityZoneCode(avz.getGlobalAvailabilityZoneCode())).thenReturn(avz);
        when(apiApplicationManager.getRegion(REGION_ZONE_1_NAME)).thenReturn(new Region(REGION_ZONE_1_NAME, REGION_ZONE_1_CODE, "bob", ""));
        when(apiApplicationManager.newPubSubMessageContextFromGlobalAvzCode(eq(PiTopics.RUN_INSTANCE), eq(avz.getGlobalAvailabilityZoneCode()))).thenReturn(pubSubMessageContext);

        setupReservation();
        setupPiIdBuilder(avz.getGlobalAvailabilityZoneCode());
        setupImages();
        setupDht();
        setupInstanceTypes();
    }

    private void setupInstanceTypes() {
        instanceTypes = new InstanceTypes();
        InstanceTypeConfiguration instanceTypeAConfiguration = new InstanceTypeConfiguration(INSTANCE_TYPE, 1, 1, 1);
        instanceTypes.addInstanceType(instanceTypeAConfiguration);

        when(piIdBuilder.getPId(InstanceTypes.URL_STRING)).thenReturn(instanceTypesId);

        when(instanceTypesCache.get(instanceTypesId)).thenReturn(instanceTypes);
    }

    private void setupImages() {
        imageIndex = new ImageIndex();

        when(image.getManifestLocation()).thenReturn(manifestLocation);
        when(image.getImageId()).thenReturn(imageId);

        imageIndex.getImages().add(imageId);
        imageIndex.getImages().add(kernelId);
        imageIndex.getImages().add(ramdiskId);

        when(image.getState()).thenReturn(ImageState.AVAILABLE);
        when(image.getMachineType()).thenReturn(MachineType.MACHINE);
        when(kernel.getState()).thenReturn(ImageState.AVAILABLE);
        when(kernel.getMachineType()).thenReturn(MachineType.KERNEL);
        when(ramdisk.getState()).thenReturn(ImageState.AVAILABLE);
        when(ramdisk.getMachineType()).thenReturn(MachineType.RAMDISK);
    }

    private void setupPiIdBuilder(int globalAvailabilityZoneCode) {
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId1))).thenReturn(id123);
        when(piIdBuilder.getPIdForEc2AvailabilityZone(Instance.getUrl(instanceId2))).thenReturn(id456);
        when(piIdBuilder.getPId(reservation)).thenReturn(reservationPastryId);
        when(piIdBuilder.getPId(ImageIndex.URL)).thenReturn(imageIndexId);
        when(imageIndexId.forLocalRegion()).thenReturn(imageIndexId);
        when(piIdBuilder.getPId(Image.getUrl(imageId))).thenReturn(imagePastryId);
        when(piIdBuilder.getPId(Image.getUrl(kernelId))).thenReturn(kernelPastryId);
        when(piIdBuilder.getPId(Image.getUrl(ramdiskId))).thenReturn(ramdiskPastryId);
        when(piIdBuilder.getPId(SecurityGroup.getUrl(ownerId, VALID_SECURITY_GROUP))).thenReturn(securityGroupId);
        when(securityGroupId.forLocalRegion()).thenReturn(securityGroupId);
        when(piIdBuilder.getPId(PiQueue.RUN_INSTANCE.getUrl())).thenReturn(runInstanceQueueId);
        when(runInstanceQueueId.forGlobalAvailablityZoneCode(globalAvailabilityZoneCode)).thenReturn(runInstanceQueueId);
    }

    private void setupDht() {
        when(dhtClientFactory.createBlockingReader()).thenReturn(blockingDhtReader);
        when(dhtClientFactory.createBlockingWriter()).thenReturn(blockingDhtWriter);
        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        setupUserInDht();
        when(blockingDhtReader.get(imageIndexId)).thenReturn(imageIndex);
        when(blockingDhtReader.get(imagePastryId)).thenReturn(image);
        when(blockingDhtReader.get(kernelPastryId)).thenReturn(kernel);
        when(blockingDhtReader.get(ramdiskPastryId)).thenReturn(ramdisk);

        SecurityGroup securityGroup1 = setupSecurityGroup(ownerId, VALID_SECURITY_GROUP, 1);
        when(blockingDhtReader.get(securityGroupId)).thenReturn(securityGroup1);
    }

    private SecurityGroup setupSecurityGroup(String ownerId, String securityGroupName, int instanceCount) {
        SecurityGroup securityGroup = new SecurityGroup(ownerId, securityGroupName, 10L, "172.0.0.0", "255.255.255.250", "147.149.2.5", new HashSet<NetworkRule>());
        for (int i = 0; i < instanceCount; i++) {
            String instanceId = String.format("i-abc%d", i);
            securityGroup.getInstances().put(instanceId, null);
        }
        return securityGroup;
    }

    private void setupReservation() {
        reservation = new Reservation();
        reservation.setKernelId(kernelId);
        reservation.setImageId(imageId);
        reservation.setRamdiskId(ramdiskId);
        reservation.setMaxCount(1);
        reservation.setMinCount(1);
        reservation.setUserId(ownerId);
        reservation.setAvailabilityZone(AVAILABILITY_ZONE_1_NAME);
        reservation.setSecurityGroupName(VALID_SECURITY_GROUP);
        reservation.setInstanceType(INSTANCE_TYPE);
    }

    private void setupUserInDht() {
        user = new User();
        user.addInstance(instanceId1, INSTANCE_TYPE);
        user.addInstance(instanceId2, INSTANCE_TYPE);
        user.setUsername(ownerId);
        user.getKeyPairs().add(new KeyPair(keyName));
        user.getSecurityGroupIds().add(VALID_SECURITY_GROUP);

        when(this.userManagementService.getUser(ownerId)).thenReturn(user);
        when(this.userService.getCurrentCores(ownerId)).thenReturn(0);
        when(this.userService.getMaxCores(ownerId)).thenReturn(user.getMaxCores());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesWithNonExistentAvailabilityZone() throws Exception {
        // setup
        reservation.setAvailabilityZone("unknown");

        // act
        runInstancesServiceHelper.runInstances(reservation);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRunInstancesSingle() {
        // setup
        long startTime = System.currentTimeMillis();

        // act
        ReservationInstances reservationInstances = runInstancesServiceHelper.runInstances(reservation);

        // assert
        assertEquals(reservationId, reservation.getReservationId());
        assertEquals(reservationInstances.getReservation().getKernelId(), kernelId);
        assertEquals(reservationInstances.getReservation().getImageId(), imageId);
        assertEquals(1, reservationInstances.getInstances().size());
        assertInstanceProperties(startTime, (Instance) reservationInstances.getInstances().toArray()[0]);

        verify(blockingDhtWriter).update(eq(id123), isA(Instance.class), isA(AddNewInstanceResolver.class));
        verify(userService).addInstancesToUser(eq(ownerId), isA(Collection.class), eq(INSTANCE_TYPE));
        verify(pubSubMessageContext).randomAnycast(eq(EntityMethod.CREATE), isA(Reservation.class));

        verify(taskProcessingQueueHelper).addUrlToQueue(eq(runInstanceQueueId), argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                String url = (String) argument;
                return url.equals("inst:" + instanceId1);
            }
        }), eq(runInstancesServiceHelper.getInstanceTaskQueueRetries()));
    }

    @Test(expected = IllegalStateException.class)
    public void testRunInstancesWhenOverInstanceCountLimit() {
        // setup
        int maxInstances = 2;
        user.setMaxInstances(maxInstances);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().equals(String.format("Unable to run instances as user %s currently has %s instances when the maximum is %s.", ownerId, 2, maxInstances)));
            throw e;
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testRunInstancesWhenOverCoreCountLimit() {
        // setup
        int maxCores = 2;
        user.setMaxCores(maxCores);
        when(userService.getCurrentCores(ownerId)).thenReturn(2);
        when(this.userService.getMaxCores(ownerId)).thenReturn(user.getMaxCores());

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().equals(String.format("Unable to run instances as user %s currently has %s cores when the maximum is %s.", ownerId, 2, maxCores)));
            throw e;
        }
    }

    private void assertInstanceProperties(long startTime, Instance instance) {
        assertTrue(instance.getInstanceId().equals(instanceId1) || instance.getInstanceId().equals(instanceId2));
        assertEquals(InstanceState.PENDING, instance.getState());
        assertTrue(instance.getLaunchTime() >= startTime && instance.getLaunchTime() <= System.currentTimeMillis());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleWithNotPublicImageThatIsNotOwnedByUser() {
        // setup
        imageIndex.getImages().remove(imageId);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("user %s does not have access to image %s", ownerId, imageId), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleWithNotPublicKernelThatIsNotOwnedByUser() {
        // setup
        imageIndex.getImages().remove(kernelId);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("user %s does not have access to kernel %s", ownerId, kernelId), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleWithNotPublicRamdiskThatIsNotOwnedByUser() {
        // setup
        imageIndex.getImages().remove(ramdiskId);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("user %s does not have access to ramdisk %s", ownerId, ramdiskId), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleWithImageNotAvailable() {
        // setup
        when(image.getState()).thenReturn(ImageState.PENDING);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("image %s must be in state %s", imageId, ImageState.AVAILABLE), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleWithKernelNotAvailable() {
        // setup
        when(kernel.getState()).thenReturn(ImageState.PENDING);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("kernel %s must be in state %s", kernelId, ImageState.AVAILABLE), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleWithKernelNotCorrectMachineType() {
        // setup
        when(kernel.getMachineType()).thenReturn(MachineType.MACHINE);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("kernel %s must be a %s machine type", kernelId, MachineType.KERNEL), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleWithRamdiskNotCorrectMachineType() {
        // setup
        when(ramdisk.getMachineType()).thenReturn(MachineType.KERNEL);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("ramdisk %s must be a %s machine type", ramdiskId, MachineType.RAMDISK), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleWithRamdiskNotAvailable() {
        // setup
        when(ramdisk.getState()).thenReturn(ImageState.PENDING);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("ramdisk %s must be in state %s", ramdiskId, ImageState.AVAILABLE), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleWithKernelNotFound() {
        // setup
        imageIndex.getImages().remove(kernelId);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("user %s does not have access to kernel %s", ownerId, kernelId), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleWithRamdiskNotFound() {
        // setup
        imageIndex.getImages().remove(ramdiskId);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("user %s does not have access to ramdisk %s", ownerId, ramdiskId), e.getMessage());
            throw e;
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleNoDefaultKernelId() {
        // setup
        when(image.getKernelId()).thenReturn(null);
        reservation.setKernelId(null);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals("You must supply a kernel as the image has no default one", e.getMessage());
            throw e;
        }
    }

    @Test
    public void testRunInstancesSingleWindowsNoDefaultKernelId() {
        // setup
        when(image.getKernelId()).thenReturn(null);
        when(image.getPlatform()).thenReturn(ImagePlatform.windows);
        reservation.setKernelId(null);

        // act
        runInstancesServiceHelper.runInstances(reservation);

        // assert
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(runInstanceQueueId), argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                String url = (String) argument;
                return url.equals("inst:" + instanceId1);
            }
        }), eq(runInstancesServiceHelper.getInstanceTaskQueueRetries()));
    }

    @Test
    public void testRunInstancesSingleLinuxNoDefaultRamdiskId() {
        // setup
        when(image.getRamdiskId()).thenReturn(null);
        when(image.getPlatform()).thenReturn(ImagePlatform.linux);
        reservation.setRamdiskId(null);

        // act
        runInstancesServiceHelper.runInstances(reservation);

        // assert
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(runInstanceQueueId), argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                String url = (String) argument;
                return url.equals("inst:" + instanceId1);
            }
        }), eq(runInstancesServiceHelper.getInstanceTaskQueueRetries()));
    }

    @Test
    public void testRunInstancesSingleSolarisNoDefaultRamdiskId() {
        // setup
        when(image.getRamdiskId()).thenReturn(null);
        when(image.getPlatform()).thenReturn(ImagePlatform.opensolaris);
        reservation.setRamdiskId(null);

        // act
        runInstancesServiceHelper.runInstances(reservation);

        // assert
        verify(taskProcessingQueueHelper).addUrlToQueue(eq(runInstanceQueueId), argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                String url = (String) argument;
                return url.equals("inst:" + instanceId1);
            }
        }), eq(runInstancesServiceHelper.getInstanceTaskQueueRetries()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunInstancesSingleNoDefaultRamdiskId() {
        // setup
        when(image.getRamdiskId()).thenReturn(null);
        reservation.setRamdiskId(null);

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals("You must supply a ramdisk as the image has no default one", e.getMessage());
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testRunInstancesMultiple() {
        // setup
        long startTime = System.currentTimeMillis();
        reservation.setMaxCount(2);

        // act
        ReservationInstances reservationInstances = runInstancesServiceHelper.runInstances(reservation);

        // assert
        assertNotNull(reservation.getReservationId());
        assertEquals(reservationInstances.getReservation().getKernelId(), "k-123");
        assertEquals(reservationInstances.getReservation().getImageId(), imageId);
        assertEquals(2, reservationInstances.getReservation().getInstanceIds().size());
        assertEquals(2, reservationInstances.getInstances().size());
        assertInstanceProperties(startTime, (Instance) reservationInstances.getInstances().toArray()[0]);
        assertInstanceProperties(startTime, (Instance) reservationInstances.getInstances().toArray()[1]);

        verify(blockingDhtWriter).update(eq(id123), isA(Instance.class), isA(AddNewInstanceResolver.class));
        verify(blockingDhtWriter).update(eq(id456), isA(Instance.class), isA(AddNewInstanceResolver.class));
        verify(userService).addInstancesToUser(eq(ownerId), isA(Collection.class), eq(INSTANCE_TYPE));
        verify(pubSubMessageContext).randomAnycast(eq(EntityMethod.CREATE), isA(Reservation.class));
        verify(taskProcessingQueueHelper, times(2)).addUrlToQueue(eq(runInstanceQueueId), argThat(new ArgumentMatcher<String>() {
            @Override
            public boolean matches(Object argument) {
                String url = (String) argument;
                return url.equals("inst:" + instanceId1) || url.equals("inst:" + instanceId2);
            }
        }), eq(runInstancesServiceHelper.getInstanceTaskQueueRetries()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThatRunInstanceWithInvalidKeyNameThrowsException() throws Exception {
        // setup
        reservation.setKeyName("unknown-key");

        // act
        try {
            runInstancesServiceHelper.runInstances(reservation);
        } catch (IllegalArgumentException e) {
            assertEquals(String.format("User %s does not have key pair \"unknown-key\"", ownerId), e.getMessage());
            throw e;
        }
    }

    @Test
    public void testRunInstancesWithKey() {
        // setup
        reservation.setKeyName(keyName);

        // act
        ReservationInstances reservationInstances = runInstancesServiceHelper.runInstances(reservation);

        // assert
        assertEquals(1, reservationInstances.getInstances().size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowErrorIfSecurityGroupDoesntExist() {
        // setup
        reservation.setSecurityGroupName(INVALID_SECURITY_GROUP);
        PId badSecurityGroupId = mock(PId.class);
        when(piIdBuilder.getPId(SecurityGroup.getUrl(ownerId, INVALID_SECURITY_GROUP))).thenReturn(badSecurityGroupId);
        when(badSecurityGroupId.forLocalRegion()).thenReturn(badSecurityGroupId);

        // act
        runInstancesServiceHelper.runInstances(reservation);
    }

    @Test
    public void shouldRunInstanceIfSecurityGroupExist() {
        // setup
        reservation.setSecurityGroupName(VALID_SECURITY_GROUP);

        // act
        ReservationInstances reservationInstances = runInstancesServiceHelper.runInstances(reservation);

        // assert
        assertEquals(1, reservationInstances.getInstances().size());
    }

    @Test
    public void shouldRunInstanceIfNoInstancesRunningForASecurityGroup() {
        // setup
        reservation.setSecurityGroupName(VALID_SECURITY_GROUP);

        SecurityGroup sg = setupSecurityGroup(ownerId, VALID_SECURITY_GROUP, 1);
        sg.getInstances().clear();
        sg.setNetmask(null);
        sg.setDnsAddress(null);

        when(blockingDhtReader.get(securityGroupId)).thenReturn(sg);

        // act
        ReservationInstances reservationInstances = runInstancesServiceHelper.runInstances(reservation);

        // assert
        assertEquals(1, reservationInstances.getInstances().size());
    }

    @Test
    public void shouldRunInstanceIfInstancesRunningInASecurityGroupAndNetmaskIsNotSet() {
        // setup
        reservation.setSecurityGroupName(VALID_SECURITY_GROUP);

        SecurityGroup sg = setupSecurityGroup(ownerId, VALID_SECURITY_GROUP, 1);
        sg.setNetmask(null);
        sg.setDnsAddress(null);

        when(blockingDhtReader.get(securityGroupId)).thenReturn(sg);

        // act
        ReservationInstances reservationInstances = runInstancesServiceHelper.runInstances(reservation);

        // assert
        assertEquals(1, reservationInstances.getInstances().size());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfNumberOfReservedInstancesPlusRunningInstancesExceedAvailableInstancesInSecurityGroup() {
        // setup
        reservation.setMinCount(5);

        // act
        runInstancesServiceHelper.runInstances(reservation);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfNumberOfRunningInstancesPlusReservedInstancesExceedAvailableInstancesInSecurityGroup() {
        // setup
        user.getSecurityGroupIds().add(VALID_SECURITY_GROUP_2);
        reservation.setSecurityGroupName(VALID_SECURITY_GROUP_2);

        when(piIdBuilder.getPId(String.format("sg:%s:%s", ownerId, VALID_SECURITY_GROUP_2))).thenReturn(securityGroupId);
        when(securityGroupId.forLocalRegion()).thenReturn(securityGroupId);
        SecurityGroup sg = setupSecurityGroup(ownerId, VALID_SECURITY_GROUP_2, 5);
        when(blockingDhtReader.get(securityGroupId)).thenReturn(sg);

        // act
        runInstancesServiceHelper.runInstances(reservation);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldAddInstanceIdsToSecurityGroupInDhtForValidReservations() {
        // setup
        reservation.setMaxCount(2);

        doAnswer(new Answer<Object>() {

            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                AddInstanceToSecurityGroupResolver resolver = (AddInstanceToSecurityGroupResolver) invocation.getArguments()[2];
                assertEquals(2, resolver.getReservationInstanceIds().size());
                SecurityGroup updatedSecurityGroup = resolver.update(new SecurityGroup(), null);
                assertEquals(2, updatedSecurityGroup.getInstances().size());
                for (Entry<String, InstanceAddress> entry : updatedSecurityGroup.getInstances().entrySet()) {
                    assertNotNull(entry.getValue());
                }
                return null;
            }
        }).when(blockingDhtWriter).update(eq(securityGroupId), (PiEntity) anyObject(), isA(UpdateResolver.class));

        // act
        runInstancesServiceHelper.runInstances(reservation);

        // assert
        verify(blockingDhtWriter).update(eq(securityGroupId), (PiEntity) anyObject(), isA(UpdateResolver.class));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionIfInstanceTypeIsNotValid() {
        // setup
        reservation.setInstanceType("x1.invalid");

        // act
        runInstancesServiceHelper.runInstances(reservation);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowIllegalStateExceptionIfInstanceTypeIsDeprecated() {
        // setup
        InstanceTypeConfiguration instanceTypeConfiguration = new InstanceTypeConfiguration("x1.deprecated", 4, 100, 12);
        instanceTypeConfiguration.setDeprecated(true);
        instanceTypes.addInstanceType(instanceTypeConfiguration);
        reservation.setInstanceType("x1.deprecated");

        // act
        runInstancesServiceHelper.runInstances(reservation);
    }

    @Test
    public void shouldRunInstanceForValidInstanceType() {
        // setup
        reservation.setInstanceType("x1.valid");

        // act
        ReservationInstances reservationInstances = runInstancesServiceHelper.runInstances(reservation);

        // assert
        assertEquals(1, reservationInstances.getInstances().size());
    }
}
