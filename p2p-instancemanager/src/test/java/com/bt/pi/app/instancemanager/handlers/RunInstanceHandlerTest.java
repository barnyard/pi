package com.bt.pi.app.instancemanager.handlers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.bt.pi.app.common.entities.Image;
import com.bt.pi.app.common.entities.Instance;
import com.bt.pi.app.common.entities.InstanceTypeConfiguration;
import com.bt.pi.app.common.entities.InstanceTypes;
import com.bt.pi.app.common.entities.KeyPair;
import com.bt.pi.app.common.entities.User;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.bt.pi.app.instancemanager.images.PlatformBuilder;
import com.bt.pi.app.instancemanager.images.PlatformBuilderFactory;
import com.bt.pi.app.networkmanager.net.VirtualNetworkBuilder;
import com.bt.pi.core.continuation.GenericContinuation;
import com.bt.pi.core.continuation.PiContinuation;
import com.bt.pi.core.continuation.UpdateResolvingPiContinuation;
import com.bt.pi.core.dht.DhtClientFactory;
import com.bt.pi.core.dht.DhtReader;
import com.bt.pi.core.dht.DhtWriter;
import com.bt.pi.core.dht.cache.DhtCache;
import com.bt.pi.core.entity.PiEntity;
import com.bt.pi.core.id.PId;
import com.bt.pi.core.testing.GenericContinuationAnswer;
import com.bt.pi.core.testing.UpdateResolvingContinuationAnswer;
import com.bt.pi.core.util.SerialExecutor;

@RunWith(MockitoJUnitRunner.class)
public class RunInstanceHandlerTest {
    @InjectMocks
    private RunInstanceHandler runInstanceHandler = new RunInstanceHandler();
    private String instanceType = "instanceType";
    private String imageId = "imageId";
    private String username = "username";
    private ImagePlatform platform = ImagePlatform.linux;
    private int vlanId = 1234;
    private Instance instance;
    private Image image;

    @Mock
    private PlatformBuilder platformBuilder;
    @Mock
    private PlatformBuilderFactory platformBuilderFactory;
    @Mock
    private DhtCache dhtCache;
    @Mock
    private DhtCache instanceTypesCache;
    @Mock
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Mock
    private DhtClientFactory dhtClientFactory;
    @Mock
    private DhtReader dhtReader;
    @Mock
    private DhtWriter dhtWriter;
    @Mock
    private VirtualNetworkBuilder virtualNetworkBuilder;
    @Mock
    private PiIdBuilder piIdBuilder;
    @Mock
    private PId userPastryId;
    @Mock
    private PId instanceTypesPastryId;
    @Mock
    private PId imagePastryId;
    @Mock
    private SerialExecutor serialExecutor;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        when(piIdBuilder.getPId(User.getUrl(username))).thenReturn(userPastryId);
        when(piIdBuilder.getPId(InstanceTypes.URL_STRING)).thenReturn(instanceTypesPastryId);
        when(piIdBuilder.getPId(Image.getUrl(imageId))).thenReturn(imagePastryId);

        UpdateResolvingContinuationAnswer imageAnswer = new UpdateResolvingContinuationAnswer(null);
        doAnswer(imageAnswer).when(dhtWriter).update(isA(PId.class), isA(UpdateResolvingPiContinuation.class));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                User user = new User();
                user.getKeyPairs().add(new KeyPair("key", "keyFingerprint", "keyMaterial"));
                ((GenericContinuation<User>) invocation.getArguments()[1]).handleResult(user);

                return null;
            }
        }).when(dhtReader).getAsync(eq(userPastryId), isA(GenericContinuation.class));

        when(dhtClientFactory.createReader()).thenReturn(dhtReader);
        when(dhtClientFactory.createWriter()).thenReturn(dhtWriter);

        when(platformBuilderFactory.getFor(isA(ImagePlatform.class))).thenReturn(platformBuilder);

        final InstanceTypes instanceTypes = new InstanceTypes();
        instanceTypes.addInstanceType(new InstanceTypeConfiguration(instanceType, 1, 2, 3));

        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                PiContinuation continuation = (PiContinuation) invocation.getArguments()[1];
                continuation.handleResult(instanceTypes);
                return null;
            }
        }).when(instanceTypesCache).get(eq(instanceTypesPastryId), isA(PiContinuation.class));

        runInstanceHandler.setupInstanceTypesId();
    }

    @SuppressWarnings("unchecked")
    @Before
    public void setupImage() {
        image = new Image();
        image.setRamdiskId("ramDiskId");
        image.setImageId(imageId);
        image.setKernelId("kernelId");
        image.setPlatform(platform);

        GenericContinuationAnswer<Image> dhtCacheAnswer = new GenericContinuationAnswer<Image>(image);
        doAnswer(dhtCacheAnswer).when(dhtCache).get(eq(imagePastryId), (PiContinuation<PiEntity>) isA(PiContinuation.class));
    }

    @Before
    public void setupInstance() {
        instance = new Instance();
        instance.setPlatform(platform);
        instance.setVlanId(vlanId);
        instance.setImageId(imageId);
        instance.setSecurityGroupName("securityGroupName");
        instance.setInstanceId("id");
        instance.setInstanceType(instanceType);
        instance.setUserId(username);
    }

    @Test
    public void shouldUpdateInstanceWithImageInformation() {
        // act
        runInstanceHandler.startInstance(instance);

        // assert
        assertEquals(image.getKernelId(), instance.getKernelId());
        assertEquals(image.getImageId(), instance.getImageId());
        assertEquals(image.getRamdiskId(), instance.getRamdiskId());
        assertEquals(instance.getImageId(), instance.getSourceImagePath());
        assertEquals(instance.getKernelId(), instance.getSourceKernelPath());
        assertEquals(instance.getRamdiskId(), instance.getSourceRamdiskPath());
    }

    @Test
    public void shouldNotOverrideUserSetRamDiskAndKernel() {
        // setup
        String setRamdisk = "nonDefaultRamdisk";
        String setKernel = "nonDefaultKernel";
        instance.setRamdiskId(setRamdisk);
        instance.setKernelId(setKernel);

        // act
        runInstanceHandler.startInstance(instance);

        // assert
        assertNotSame(image.getKernelId(), instance.getKernelId());
        assertEquals(image.getImageId(), instance.getImageId());
        assertNotSame(image.getRamdiskId(), instance.getRamdiskId());
        assertEquals(setRamdisk, instance.getRamdiskId());
        assertEquals(setKernel, instance.getKernelId());
        assertEquals(instance.getImageId(), instance.getSourceImagePath());
        assertEquals(instance.getKernelId(), instance.getSourceKernelPath());
        assertEquals(instance.getRamdiskId(), instance.getSourceRamdiskPath());
    }

    @Test
    public void shouldUsePlatformBuilderFactoryToCreatePlatformBuilder() {
        // act
        runInstanceHandler.startInstance(instance);

        // assert
        verify(platformBuilderFactory).getFor(platform);
    }

    @Test
    public void shouldExecuteNewBuildInstanceRunnerWithNullKey() {
        // act
        runInstanceHandler.startInstance(instance);

        // assert
        verify(serialExecutor).execute(argThat(new ArgumentMatcher<BuildInstanceRunner>() {
            @Override
            public boolean matches(Object argument) {
                BuildInstanceRunner buildInstanceRunner = (BuildInstanceRunner) argument;
                assertNull(buildInstanceRunner.getKey());
                return true;
            }
        }));
    }

    @Test
    public void shouldExecuteNewBuildInstanceRunnerWithNonNullKey() {
        // setup
        instance.setKeyName("key");

        // act
        runInstanceHandler.startInstance(instance);

        // assert
        verify(serialExecutor).execute(argThat(new ArgumentMatcher<BuildInstanceRunner>() {
            @Override
            public boolean matches(Object argument) {
                BuildInstanceRunner buildInstanceRunner = (BuildInstanceRunner) argument;
                assertEquals("keyMaterial", buildInstanceRunner.getKey());
                return true;
            }
        }));
    }

    @Test
    public void shouldExecuteNewBuildInstanceRunnerWithNullKeyIfKeyDoesNotExist() {
        // setup
        instance.setKeyName("key2");

        // act
        runInstanceHandler.startInstance(instance);

        // assert
        verify(serialExecutor).execute(argThat(new ArgumentMatcher<BuildInstanceRunner>() {
            @Override
            public boolean matches(Object argument) {
                BuildInstanceRunner buildInstanceRunner = (BuildInstanceRunner) argument;
                assertNull(buildInstanceRunner.getKey());
                return true;
            }
        }));
    }

    @Test
    public void shouldInvokeNetworkCommandRunner() {
        // act
        runInstanceHandler.startInstance(instance);

        // assert
        verify(virtualNetworkBuilder).setUpVirtualNetworkForInstance(vlanId, instance.getInstanceId());
    }
}
