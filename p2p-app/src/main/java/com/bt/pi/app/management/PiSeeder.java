package com.bt.pi.app.management;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;
import javax.management.JMException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedOperationParameter;
import org.springframework.jmx.export.annotation.ManagedOperationParameters;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

import com.bt.pi.app.common.entities.AvailabilityZone;
import com.bt.pi.app.common.entities.AvailabilityZones;
import com.bt.pi.app.common.entities.Region;
import com.bt.pi.app.common.entities.Regions;
import com.bt.pi.app.common.id.PiIdBuilder;
import com.bt.pi.app.common.resource.PiQueue;
import com.bt.pi.core.dht.cache.BlockingDhtCache;
import com.bt.pi.core.id.PId;

@ManagedResource(description = "A proxy for 'seeding' the p2p substrate with pre-requisites such as DHT entries", objectName = "bean:name=piSeeder")
@Component
public class PiSeeder {
    private static final Log LOG = LogFactory.getLog(PiSeeder.class);
    private static final String APPLICATION_NAME = "applicationName";
    private static final String APPLICATION_NAME_DESCRIPTION = "Application name";
    private static final String REGION_NAMES = "regionNames";
    private static final String REGION_CODES = "regionCodes";
    private static final String REGION_NAME = "regionName";
    private static final String RESOURCES = "resources";
    private static final String SPACE = " ";
    private static final String AVAILABILITY_ZONE_NAME = "availabilityZoneName";
    private static final String AVAILABILITY_ZONE_CODE = "availabilityZoneCode";
    private static final String REGION_CODE = "regionCode";
    private static final String IMAGE_ID = "imageId";
    private static final String IMAGE_ID_DESCRIPTION = "The id for the kernal,machine, or ramdisk.";
    private static final String IMAGE_PLATFORM = "platform";
    private static final String IMAGE_PLATFORM_DESCRIPTION = "The image platform (i.e. windows/linux/opensolaris).";
    private static final String REGION_NAME_DESCRIPTION = "Name of the region to create the record in";
    private static final String AVAILABILITY_ZONE_NAME_DESCRIPTION = "Name of the availability zone to create the record in";
    private static final String AVAILABILITY_ZONE_CODE_DESCRIPTION = "Code for availability zone";
    private static final String REGION_NAMES_DESCRIPTION = "All region names, semicolon-separated";
    private static final String REGION_CODES_DESCRIPTION = "All region codes, semicolon-separated";
    private static final String REGION_CODE_DESCRIPTION = "Numeric code for this region";
    private static final String PUBLIC_IP_ADDRESS_RANGES = "publicIpAddressRanges";
    private static final String PUBLIC_IP_ADDRESS_RANGES_DESCRIPTION = "A list of address ranges separated by spaces, with each pair separated by a -";
    private static final String NETWORK_RANGES = "networks";
    private static final String NETWORK_APP_ADDRESSES = "networkAppAddresses";
    private static final String NETWORK_RANGES_DESCRIPTION = "A list of network/slashnet ranges, followed by semicolon and addresses per allocation. List items separated by spaces. Eg: 172.0.0.0/24;16 172.20.0.0/16;32";
    private static final String VLAN_RANGES = "vlanIdRanges";
    private static final String VLAN_RANGES_DESCRIPTION = "A list of ranges separated by spaces, with each pair separated by a -";
    private static final String DNS_ADDRESS = "dnsAddress";
    private static final String DNS_ADDRESS_DESCRIPTION = "DNS address to be used by instances";

    private static final String APP_RECORD_RESOURCE_DESCRIPTION = "A single integer for the number of applications or a list of resources that an active application will be associated with list items seperated by spaces.";
    private ApplicationSeeder applicationSeeder;

    private ImageSeeder imageSeeder;
    private NetworkSeeder networkSeeder;
    private InstanceSeeder instanceSeeder;
    private QueueSeeder queueSeeder;
    private RegionAndAvailabilityZoneSeeder regionAndAvailabilityZoneSeeder;
    private ManagementUsersSeeder managementUsersSeeder;
    private SuperNodeSeeder superNodeSeeder;
    private BlockingDhtCache blockingDhtCache;
    private PiIdBuilder piIdBuilder;

    public PiSeeder() {
        blockingDhtCache = null;
        piIdBuilder = null;
        applicationSeeder = null;
        imageSeeder = null;
        networkSeeder = null;
        instanceSeeder = null;
        queueSeeder = null;
        regionAndAvailabilityZoneSeeder = null;
        managementUsersSeeder = null;
        superNodeSeeder = null;
    }

    @Resource(name = "generalBlockingCache")
    public void setBlockingDhtCache(BlockingDhtCache aBlockingDhtCache) {
        blockingDhtCache = aBlockingDhtCache;
    }

    @Resource
    public void setPiIdBuilder(PiIdBuilder aPiIdBuilder) {
        this.piIdBuilder = aPiIdBuilder;
    }

    @Resource
    public void setApplicationSeeder(ApplicationSeeder anApplicationSeederHandler) {
        this.applicationSeeder = anApplicationSeederHandler;
    }

    @Resource
    public void setImageSeeder(ImageSeeder anImageSeederHandler) {
        this.imageSeeder = anImageSeederHandler;
    }

    @Resource
    public void setNetworkSeeder(NetworkSeeder aNetworkSeeder) {
        this.networkSeeder = aNetworkSeeder;
    }

    @Resource
    public void setInstanceSeeder(InstanceSeeder anInstanceSeeder) {
        this.instanceSeeder = anInstanceSeeder;
    }

    @Resource
    public void setQueueSeeder(QueueSeeder aQueueSeeder) {
        this.queueSeeder = aQueueSeeder;
    }

    @Resource
    public void setRegionAndAvailabilityZoneSeeder(RegionAndAvailabilityZoneSeeder aRegionAndAvailabilityZoneSeeder) {
        this.regionAndAvailabilityZoneSeeder = aRegionAndAvailabilityZoneSeeder;
    }

    @Resource
    public void setManagementUsersSeeder(ManagementUsersSeeder theManagementUsersSeeder) {
        managementUsersSeeder = theManagementUsersSeeder;
    }

    @Resource
    public void setSuperNodeSeeder(SuperNodeSeeder aSuperNodeSeeder) {
        this.superNodeSeeder = aSuperNodeSeeder;
    }

    @ManagedOperation(description = "Create VLAN allocation index record in DHT")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = VLAN_RANGES, description = VLAN_RANGES_DESCRIPTION), @ManagedOperationParameter(name = REGION_NAME, description = REGION_NAME_DESCRIPTION) })
    public boolean createVlanAllocationIndex(String vlanIdRangesString, String regionName) {
        int regionCode = getRegionCodeFromRegionName(regionName);
        return networkSeeder.createVlanAllocationIndex(vlanIdRangesString, regionCode);
    }

    @ManagedOperation(description = "Create an image record in DHT")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = IMAGE_ID, description = IMAGE_ID_DESCRIPTION), @ManagedOperationParameter(name = "kernelId", description = "The default kernel for a machine image."),
            @ManagedOperationParameter(name = "ramDiskId", description = "The default ramDisk for a machine image."), @ManagedOperationParameter(name = "manifestLocation", description = "The location of the image manifest file."),
            @ManagedOperationParameter(name = "ownerId", description = "The owner of the image."), @ManagedOperationParameter(name = "architecture", description = "The image architecture (i.e. i386/x86-64)."),
            @ManagedOperationParameter(name = IMAGE_PLATFORM, description = IMAGE_PLATFORM_DESCRIPTION), @ManagedOperationParameter(name = "isPublic", description = "If the image should be for public use."),
            @ManagedOperationParameter(name = "machineType", description = "Kernel/Machine/Ramdisk"), @ManagedOperationParameter(name = REGION_NAME, description = REGION_NAME_DESCRIPTION) })
    public String createImage(String imageId, String kernelId, String ramDiskId, String manifestLocation, String ownerId, String architecture, String platform, Boolean isPublic, String machineType, String regionName) {
        int regionCode = getRegionCodeFromRegionName(regionName);
        return imageSeeder.createImage(imageId, regionCode, kernelId, ramDiskId, manifestLocation, ownerId, architecture, platform, isPublic, machineType);
    }

    @ManagedOperation(description = "Update an image record platform in DHT")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = IMAGE_ID, description = IMAGE_ID_DESCRIPTION), @ManagedOperationParameter(name = IMAGE_PLATFORM, description = IMAGE_PLATFORM_DESCRIPTION) })
    public String updateImagePlatform(String imageId, String platform) {
        imageSeeder.updateImagePlatform(imageId, platform);
        return imageId;
    }

    @ManagedOperation(description = "Create public IP address index record in DHT")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = PUBLIC_IP_ADDRESS_RANGES, description = PUBLIC_IP_ADDRESS_RANGES_DESCRIPTION), @ManagedOperationParameter(name = REGION_NAME, description = REGION_NAME_DESCRIPTION) })
    public boolean createPublicAddressAllocationIndex(String publicIpAddressRangesString, String regionName) {
        int regionCode = getRegionCodeFromRegionName(regionName);
        return networkSeeder.createPublicAddressAllocationIndex(publicIpAddressRangesString, regionCode);
    }

    @ManagedOperation(description = "Create subnet allocation index record in DHT")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = NETWORK_RANGES, description = NETWORK_RANGES_DESCRIPTION), @ManagedOperationParameter(name = DNS_ADDRESS, description = DNS_ADDRESS_DESCRIPTION),
            @ManagedOperationParameter(name = REGION_NAME, description = REGION_NAME_DESCRIPTION) })
    public boolean createSubnetAllocationIndex(String networksString, String dnsAddress, String regionName) {
        int regionCode = getRegionCodeFromRegionName(regionName);
        return networkSeeder.createSubnetAllocationIndex(networksString, dnsAddress, regionCode);
    }

    @ManagedOperation(description = "Create application record for a region in the Dht")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = APPLICATION_NAME, description = APPLICATION_NAME_DESCRIPTION), @ManagedOperationParameter(name = REGION_NAME, description = REGION_NAME_DESCRIPTION),
            @ManagedOperationParameter(name = RESOURCES, description = APP_RECORD_RESOURCE_DESCRIPTION) })
    public boolean createApplicationRecordForRegion(String applicationName, String regionName, String resources) {
        int regionCode = getRegionCodeFromRegionName(regionName);
        List<String> resourceList = Arrays.asList(resources.split(SPACE));
        return applicationSeeder.createRegionScopedApplicationRecord(applicationName, regionCode, resourceList);
    }

    @ManagedOperation(description = "Create application record for an availability zone in the Dht")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = APPLICATION_NAME, description = APPLICATION_NAME_DESCRIPTION), @ManagedOperationParameter(name = AVAILABILITY_ZONE_NAME, description = AVAILABILITY_ZONE_NAME_DESCRIPTION),
            @ManagedOperationParameter(name = RESOURCES, description = APP_RECORD_RESOURCE_DESCRIPTION) })
    public boolean createApplicationRecordForAvailabilityZone(String applicationName, String availabilityZoneName, String resources) {
        AvailabilityZone avz = getAvailabilityZoneByName(availabilityZoneName);
        List<String> resourceList = Arrays.asList(resources.split(SPACE));
        return applicationSeeder.createAvailabilityZoneScopedApplicationRecord(applicationName, avz.getRegionCode(), avz.getAvailabilityZoneCodeWithinRegion(), resourceList);
    }

    @ManagedOperation(description = "Configure all instance type resources")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = "instanceTypeNames", description = "All instance type names, semicolon-separated"),
            @ManagedOperationParameter(name = "numCores", description = "All numbers of cores, semicolon-separated"), @ManagedOperationParameter(name = "memorySizesInMB", description = "All memory sizes (in MB), semicolon-separated"),
            @ManagedOperationParameter(name = "diskSizesInGB", description = "All disk sizes (in GB), semicolon-separated") })
    public boolean configureInstanceTypes(String instanceTypeNames, String numCores, String memorySizesInMB, String diskSizesInGB) {
        return instanceSeeder.configureInstanceTypes(instanceTypeNames, numCores, memorySizesInMB, diskSizesInGB);
    }

    @ManagedOperation(description = "Add a task processing queue")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = "queueName", description = "The name of the task processing queue to be created") })
    public boolean addTaskProcessingQueue(String queueName) {
        return this.queueSeeder.addQueue(PiQueue.valueOf(queueName));
    }

    @ManagedOperation(description = "Add task processing queues for a region")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = REGION_NAME, description = REGION_NAME_DESCRIPTION) })
    public boolean addTaskProcessingQueuesForRegion(String regionName) {
        int regionCode = getRegionCodeFromRegionName(regionName);
        return this.queueSeeder.addQueuesForRegion(regionCode);
    }

    @ManagedOperation(description = "Add task processing queues for an availability zone")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = AVAILABILITY_ZONE_NAME, description = AVAILABILITY_ZONE_NAME_DESCRIPTION) })
    public boolean addTaskProcessingQueuesForAvailabilityZone(String availabilityZoneName) {
        AvailabilityZone avz = getAvailabilityZoneByName(availabilityZoneName);
        return this.queueSeeder.addQueuesForAvailabilityZone(avz.getGlobalAvailabilityZoneCode());
    }

    @ManagedOperation(description = "Configure all regions")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = REGION_NAMES, description = REGION_NAMES_DESCRIPTION), @ManagedOperationParameter(name = REGION_CODES, description = REGION_CODES_DESCRIPTION),
            @ManagedOperationParameter(name = "regionEndpoints", description = "All region endpoints, semicolon-separated"), @ManagedOperationParameter(name = "regionPisssEndpoints", description = "All region PI-SSS endpoints, semicolon-separated") })
    public boolean configureRegions(String regionNames, String regionCodes, String regionEndpoints, String regionPisssEndpoints) {
        return regionAndAvailabilityZoneSeeder.configureRegions(regionNames, regionCodes, regionEndpoints, regionPisssEndpoints);
    }

    @ManagedOperation(description = "Configure all availabilityZones")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = "availabilityZoneNames", description = "All availability zone names, semicolon-separated"),
            @ManagedOperationParameter(name = "availabilityZoneCodes", description = "All availability zone codes, semicolon-separated"), @ManagedOperationParameter(name = REGION_CODES, description = REGION_CODES_DESCRIPTION),
            @ManagedOperationParameter(name = "statuses", description = "All statuses, semicolon-separated") })
    public boolean configureAvailabilityZones(String availabilityZoneNames, String availabilityZoneCodes, String regionCodes, String statuses) {
        return regionAndAvailabilityZoneSeeder.configureAvailabilityZones(availabilityZoneNames, availabilityZoneCodes, regionCodes, statuses);
    }

    @ManagedOperation(description = "Add a management user")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = "username", description = "The username"), @ManagedOperationParameter(name = "password", description = "The md5 hash of the password"),
            @ManagedOperationParameter(name = "roles", description = "The roles, semicolon-seperated for the user") })
    public void createManagementUser(String username, String password, String roles) {
        managementUsersSeeder.addUser(username, password, roles.split(";"));
    }

    @ManagedOperation(description = "Configure number of super nodes per application")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = APPLICATION_NAME, description = "Name of the supernode application"), @ManagedOperationParameter(name = "numberOfSuperNodes", description = "number of supernodes as a power of 2"),
            @ManagedOperationParameter(name = "offset", description = "the offset from 0 that marks the 1st supernode id") })
    public void configureNumberOfSuperNodes(String applicationName, Integer numberOfSuperNodes, Integer offset) throws JMException {
        try {
            superNodeSeeder.configureNumberOfSuperNodes(applicationName, numberOfSuperNodes, offset);
        } catch (IllegalArgumentException e) {
            LOG.error(String.format("Error seeding supernodes (%d)", numberOfSuperNodes), e);
            throw new JMException(e.getMessage());
        }
    }

    @ManagedOperation(description = "Add a region")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = REGION_NAME, description = REGION_NAME_DESCRIPTION), @ManagedOperationParameter(name = REGION_CODE, description = REGION_CODE_DESCRIPTION),
            @ManagedOperationParameter(name = "regionEndpoint", description = "Region endpoint"), @ManagedOperationParameter(name = "regionPisssEndpoint", description = "Region PI-SSS endpoint"),
            @ManagedOperationParameter(name = PUBLIC_IP_ADDRESS_RANGES, description = PUBLIC_IP_ADDRESS_RANGES_DESCRIPTION), @ManagedOperationParameter(name = VLAN_RANGES, description = VLAN_RANGES_DESCRIPTION),
            @ManagedOperationParameter(name = NETWORK_RANGES, description = NETWORK_RANGES_DESCRIPTION), @ManagedOperationParameter(name = DNS_ADDRESS, description = DNS_ADDRESS_DESCRIPTION),
            @ManagedOperationParameter(name = "apiAppAddresses", description = "IP addresses for API app"), @ManagedOperationParameter(name = "storageAppAddresses", description = "IP addresses for storage app"),
            @ManagedOperationParameter(name = "opsAppAddresses", description = "IP addresses for ops app") })
    public boolean addRegion(String regionName, String regionCode, String regionEndpoint, String regionPisssEndpoint, String publicIpAddressRangesString, String vlanIdRangesString, String subnetRangesString, String dnsAddress,
            String apiAppAddresses, String storageAppAddresses, String opsAppAddresses, String volumeBackupResources) {
        boolean regionAdded = regionAndAvailabilityZoneSeeder.addRegion(regionName, regionCode, regionEndpoint, regionPisssEndpoint);
        boolean publicIpIndexAdded = createPublicAddressAllocationIndex(publicIpAddressRangesString, regionName);
        boolean vlanIndexAdded = createVlanAllocationIndex(vlanIdRangesString, regionName);
        boolean subnetIndexAdded = createSubnetAllocationIndex(subnetRangesString, dnsAddress, regionName);
        boolean apiAppRecordAdded = createApplicationRecordForRegion("pi-api-manager", regionName, apiAppAddresses);
        boolean storageAppRecordAdded = createApplicationRecordForRegion("pi-sss-manager", regionName, storageAppAddresses);
        boolean opsAppRecordAdded = createApplicationRecordForRegion("pi-ops-website", regionName, opsAppAddresses);
        boolean volumeBackupRecordAdded = createApplicationRecordForRegion("pi-volumebackup-manager", regionName, volumeBackupResources);

        LOG.info(String.format("Added region %s: %s; public ip index: %s; vlan index: %s; subnet index: %s; api app rec: %s; storage app rec: %s; ops app rec: %s, volume backup app rec: %s", regionName, regionAdded, publicIpIndexAdded,
                vlanIndexAdded, subnetIndexAdded, apiAppAddresses, storageAppAddresses, opsAppAddresses, volumeBackupResources));

        // splitting booleans due to checkstyle
        boolean exp1 = regionAdded && publicIpIndexAdded && vlanIndexAdded && subnetIndexAdded;
        boolean exp2 = apiAppRecordAdded && storageAppRecordAdded && opsAppRecordAdded && volumeBackupRecordAdded;
        return exp1 && exp2;
    }

    @ManagedOperation(description = "Add an availability zone")
    @ManagedOperationParameters( { @ManagedOperationParameter(name = AVAILABILITY_ZONE_NAME, description = AVAILABILITY_ZONE_NAME_DESCRIPTION),
            @ManagedOperationParameter(name = AVAILABILITY_ZONE_CODE, description = AVAILABILITY_ZONE_CODE_DESCRIPTION), @ManagedOperationParameter(name = REGION_CODE, description = REGION_CODE_DESCRIPTION),
            @ManagedOperationParameter(name = "availabilityZoneStatus", description = "Availability zone status"), @ManagedOperationParameter(name = NETWORK_APP_ADDRESSES, description = "IP addresses for network app") })
    public boolean addAvailabilityZone(String availabilityZoneName, String availabilityZoneCodeWithinRegion, String regionCode, String availabilityZoneStatus, String networkAppAddresses) {
        boolean availabilityZoneAdded = regionAndAvailabilityZoneSeeder.addAvailabilityZone(availabilityZoneName, availabilityZoneCodeWithinRegion, regionCode, availabilityZoneStatus);
        boolean networkAppRecordAdded = createApplicationRecordForAvailabilityZone("pi-network-manager", availabilityZoneName, networkAppAddresses);

        LOG.info(String.format("Added availability zone %s: %s; network app rec: %s", availabilityZoneName, availabilityZoneAdded, networkAppRecordAdded));
        return availabilityZoneAdded;
    }

    protected int getRegionCodeFromRegionName(String regionName) {
        PId regionsId = piIdBuilder.getRegionsId();
        Regions regions = (Regions) blockingDhtCache.getReadThrough(regionsId);
        Region region = regions.getRegion(regionName);
        if (region == null)
            throw new IllegalArgumentException("Unknown region: " + regionName);
        return region.getRegionCode();
    }

    protected AvailabilityZone getAvailabilityZoneByName(String avzName) {
        PId avzsId = piIdBuilder.getAvailabilityZonesId();
        AvailabilityZones avzs = (AvailabilityZones) blockingDhtCache.getReadThrough(avzsId);
        return avzs.getAvailabilityZoneByName(avzName);
    }
}
