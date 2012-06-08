package com.bt.pi.app.management;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import javax.management.JMException;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;

public class PiSeederTest {
    private static final String ROLE_PROVISIONING = "ROLE_PROVISIONING";
    private static final String ROLE_OPS = "ROLE_OPS";
    private static final String ROLE_MIS = "ROLE_MIS";
    private static final String PASSWORD = "password";
    private static final String USERNAME = "username";
    private PiSeeder piSeeder;
    private ApplicationSeeder applicationSeeder;
    private NetworkSeeder networkSeeder;
    private ImageSeeder imageSeeder;
    private InstanceSeeder instanceSeeder;
    private QueueSeeder queueSeeder;
    private RegionAndAvailabilityZoneSeeder regionAndAvailabilityZoneSeeder;
    private ManagementUsersSeeder managementUsersSeeder;
    private SuperNodeSeeder superNodeSeeder;
    private PiIdBuilder piIdBuider;
    private BlockingDhtCache blockingDhtCache;
    private Region region;
    private Regions regions;
    private AvailabilityZone avz;
    private AvailabilityZones avzs;
    private PId regionsId;
    private PId availabilityZonesId;

    @Before
    public void before() {
        region = new Region("ragin-region", 3, "endpoint", "");
        regions = new Regions();
        regions.addRegion(region);

        avz = new AvailabilityZone("twilight-zone", 9, 3, "available");
        avzs = new AvailabilityZones();
        avzs.addAvailabilityZone(avz);

        applicationSeeder = mock(ApplicationSeeder.class);
        imageSeeder = mock(ImageSeeder.class);
        networkSeeder = mock(NetworkSeeder.class);
        instanceSeeder = mock(InstanceSeeder.class);
        queueSeeder = mock(QueueSeeder.class);
        managementUsersSeeder = mock(ManagementUsersSeeder.class);
        regionAndAvailabilityZoneSeeder = mock(RegionAndAvailabilityZoneSeeder.class);
        superNodeSeeder = mock(SuperNodeSeeder.class);
        regionsId = mock(PId.class);
        availabilityZonesId = mock(PId.class);

        piIdBuider = mock(PiIdBuilder.class);
        when(piIdBuider.getRegionsId()).thenReturn(regionsId);
        when(piIdBuider.getAvailabilityZonesId()).thenReturn(availabilityZonesId);

        blockingDhtCache = mock(BlockingDhtCache.class);
        when(blockingDhtCache.getReadThrough(regionsId)).thenReturn(regions);
        when(blockingDhtCache.getReadThrough(availabilityZonesId)).thenReturn(avzs);

        piSeeder = new PiSeeder();
        piSeeder.setBlockingDhtCache(blockingDhtCache);
        piSeeder.setPiIdBuilder(piIdBuider);
        piSeeder.setApplicationSeeder(applicationSeeder);
        piSeeder.setNetworkSeeder(networkSeeder);
        piSeeder.setImageSeeder(imageSeeder);
        piSeeder.setInstanceSeeder(instanceSeeder);
        piSeeder.setQueueSeeder(queueSeeder);
        piSeeder.setRegionAndAvailabilityZoneSeeder(regionAndAvailabilityZoneSeeder);
        piSeeder.setManagementUsersSeeder(managementUsersSeeder);
        piSeeder.setSuperNodeSeeder(superNodeSeeder);
    }

    @Test
    public void shouldCallThroughToApplicationSeederHandlerForCreateApplicationRecordForRegion() {
        // act
        piSeeder.createApplicationRecordForRegion("foo", "ragin-region", "3");

        // verify
        verify(applicationSeeder).createRegionScopedApplicationRecord("foo", 3, Arrays.asList(new String[] { "3" }));
    }

    @Test
    public void shouldCallThroughToApplicationSeederHandlerForCreateRegionApplicationRecordWithRanges() {
        // setup
        String addresses = "1.1.1.1 2.2.2.2 3.3.3.3 ";
        List<String> ipaddresses = Arrays.asList(addresses.split(" "));

        // act
        piSeeder.createApplicationRecordForRegion("foo", "ragin-region", addresses);

        // verify
        verify(applicationSeeder).createRegionScopedApplicationRecord("foo", 3, ipaddresses);
    }

    @Test
    public void shouldCallThroughToApplicationSeederHandlerForCreateAvzApplicationRecordWithRanges() {
        // setup
        String addresses = "1.1.1.1 2.2.2.2 3.3.3.3 ";
        List<String> ipaddresses = Arrays.asList(addresses.split(" "));

        // act
        piSeeder.createApplicationRecordForAvailabilityZone("foo", "twilight-zone", addresses);

        // verify
        verify(applicationSeeder).createAvailabilityZoneScopedApplicationRecord("foo", 3, 9, ipaddresses);
    }

    @Test
    public void shouldCallThroughToNetworkSeederForCreatePublicAddressAllocationIndex() {

        // act
        piSeeder.createPublicAddressAllocationIndex("wow", "ragin-region");

        // verify
        verify(networkSeeder).createPublicAddressAllocationIndex("wow", 3);
    }

    @Test
    public void shouldCallThroughtoNetworkSeederForCreateSubnetAllocIndex() {
        // act
        piSeeder.createSubnetAllocationIndex("network", "dns", "ragin-region");

        // verify
        verify(networkSeeder).createSubnetAllocationIndex("network", "dns", 3);
    }

    @Test
    public void shouldCallThroughtoNetworkSeederForCreateVlanAllocIndex() {
        // act
        piSeeder.createVlanAllocationIndex("10-11", "ragin-region");

        // verify
        verify(networkSeeder).createVlanAllocationIndex("10-11", 3);
    }

    @Test
    public void shouldCallThroughToImageSeederForCreateImage() {

        // act
        piSeeder.createImage("imageId", "kernelId", "ramDiskId", "manifestLocation", "ownerId", "architecture", "linux", true, "machineType", "ragin-region");

        // verify
        verify(imageSeeder).createImage(eq("imageId"), eq(3), eq("kernelId"), eq("ramDiskId"), eq("manifestLocation"), eq("ownerId"), eq("architecture"), eq("linux"), eq(true), eq("machineType"));
    }

    @Test
    public void shouldCallThroughToInstanceSeederForConfigureInstancetypes() {

        // act
        piSeeder.configureInstanceTypes("instanceTypeNames", "numCores", "memorySizesInMB", "diskSizesInGB");

        // verify
        verify(instanceSeeder).configureInstanceTypes(eq("instanceTypeNames"), eq("numCores"), eq("memorySizesInMB"), eq("diskSizesInGB"));
    }

    @Test
    public void shouldCallThroughToQueueSeederForAddQueue() {
        // act
        piSeeder.addTaskProcessingQueue("CREATE_VOLUME");

        // assert
        verify(queueSeeder).addQueue(eq(PiQueue.CREATE_VOLUME));
    }

    @Test
    public void shouldCallThroughToQueueSeederForAddQueuesForRegion() {
        // act
        piSeeder.addTaskProcessingQueuesForRegion("ragin-region");

        // assert
        verify(queueSeeder).addQueuesForRegion(3);
    }

    @Test
    public void shouldCallThroughToQueueSeederForAddQueuesForAvz() {
        // act
        piSeeder.addTaskProcessingQueuesForAvailabilityZone("twilight-zone");

        // assert
        verify(queueSeeder).addQueuesForAvailabilityZone(0x0309);
    }

    @Test
    public void shouldCallThroughToRegionAndAvailabilityZoneSeederForConfigureRegions() {
        // act
        piSeeder.configureRegions("regionNames", "regionCodes", "regionEndpoints", "regionPisssEndpoints");

        // assert
        verify(regionAndAvailabilityZoneSeeder).configureRegions("regionNames", "regionCodes", "regionEndpoints", "regionPisssEndpoints");
    }

    @Test
    public void shouldCallThroughToRegionAndAvailabilityZoneSeederForConfigureAvailabilityZones() {
        // act
        piSeeder.configureAvailabilityZones("availabilityZoneNames", "availabilityZoneCodes", "regionNames", "statuses");

        // assert
        verify(regionAndAvailabilityZoneSeeder).configureAvailabilityZones("availabilityZoneNames", "availabilityZoneCodes", "regionNames", "statuses");
    }

    @Test
    public void shouldCallUpdateImagePlatformToChangePlatform() {
        // act
        piSeeder.updateImagePlatform("pmi-AAA", "windows");

        // assert
        verify(imageSeeder).updateImagePlatform("pmi-AAA", "windows");
    }

    @Test
    public void shouldCallThroughToManagementUsersSeeder() {
        // act
        piSeeder.createManagementUser(USERNAME, PASSWORD, ROLE_PROVISIONING + ";" + ROLE_OPS + ";" + ROLE_MIS);

        // assert
        verify(managementUsersSeeder).addUser(USERNAME, PASSWORD, ROLE_PROVISIONING, ROLE_OPS, ROLE_MIS);
    }

    @Test
    public void shouldCallSuperNodeSeeder() throws Exception {
        // act
        piSeeder.configureNumberOfSuperNodes("test", 23, 0);

        // assert
        verify(superNodeSeeder).configureNumberOfSuperNodes("test", 23, 0);
    }

    @Test(expected = JMException.class)
    public void shouldThrowJmxExceptionIfExceptionThrown() throws Exception {
        // setup
        doThrow(new IllegalArgumentException()).when(superNodeSeeder).configureNumberOfSuperNodes("test", 23, 0);

        // act
        piSeeder.configureNumberOfSuperNodes("test", 23, 0);
    }

    @Test
    public void shouldBeAbleToSeedRegion() {
        // setup
        when(regionAndAvailabilityZoneSeeder.addRegion("ragin-region", "regionCode", "regionEndpoint", "regionPisssEndpoint")).thenReturn(true);
        when(networkSeeder.createPublicAddressAllocationIndex("public-address-ranges", 3)).thenReturn(true);
        when(networkSeeder.createVlanAllocationIndex("vlan-ranges", 3)).thenReturn(true);
        when(networkSeeder.createSubnetAllocationIndex("subnet-ranges", "dns-address", 3)).thenReturn(true);
        when(applicationSeeder.createRegionScopedApplicationRecord("pi-api-manager", 3, Arrays.asList(new String[] { "api-addrs" }))).thenReturn(true);
        when(applicationSeeder.createRegionScopedApplicationRecord("pi-sss-manager", 3, Arrays.asList(new String[] { "storage-addrs" }))).thenReturn(true);
        when(applicationSeeder.createRegionScopedApplicationRecord("pi-ops-website", 3, Arrays.asList(new String[] { "ops-addrs" }))).thenReturn(true);
        when(applicationSeeder.createRegionScopedApplicationRecord("pi-volumebackup-manager", 3, Arrays.asList(new String[] { "vol-bkup" }))).thenReturn(true);

        // act
        boolean res = piSeeder.addRegion("ragin-region", "regionCode", "regionEndpoint", "regionPisssEndpoint", "public-address-ranges", "vlan-ranges", "subnet-ranges", "dns-address", "api-addrs", "storage-addrs", "ops-addrs", "vol-bkup");

        // assert
        assertTrue(res);
    }

    @Test
    public void shouldBeAbleToSeedAvailabilityZone() {
        // setup
        when(regionAndAvailabilityZoneSeeder.addAvailabilityZone("twilight-zone", "availabilityZoneCode", "regionCode", "availabilityZoneStatus")).thenReturn(true);
        when(applicationSeeder.createAvailabilityZoneScopedApplicationRecord("pi-network-manager", 3, 9, Arrays.asList(new String[] { "net-addrs" }))).thenReturn(true);

        // act
        boolean res = piSeeder.addAvailabilityZone("twilight-zone", "availabilityZoneCode", "regionCode", "availabilityZoneStatus", "net-addrs");

        // assert
        assertTrue(res);
    }
}
