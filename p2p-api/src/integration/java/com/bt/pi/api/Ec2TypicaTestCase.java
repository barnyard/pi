package com.bt.pi.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import junit.textui.TestRunner;

import org.junit.Test;

import com.bt.pi.app.common.images.platform.ImagePlatform;
import com.xerox.amazonws.common.AWSError;
import com.xerox.amazonws.ec2.AddressInfo;
import com.xerox.amazonws.ec2.AttachmentInfo;
import com.xerox.amazonws.ec2.AvailabilityZone;
import com.xerox.amazonws.ec2.ConsoleOutput;
import com.xerox.amazonws.ec2.EC2Exception;
import com.xerox.amazonws.ec2.GroupDescription;
import com.xerox.amazonws.ec2.GroupDescription.IpPermission;
import com.xerox.amazonws.ec2.ImageDescription;
import com.xerox.amazonws.ec2.InstanceType;
import com.xerox.amazonws.ec2.Jec2;
import com.xerox.amazonws.ec2.KeyPairInfo;
import com.xerox.amazonws.ec2.RegionInfo;
import com.xerox.amazonws.ec2.ReservationDescription;
import com.xerox.amazonws.ec2.ReservationDescription.Instance;
import com.xerox.amazonws.ec2.SnapshotInfo;
import com.xerox.amazonws.ec2.TerminatingInstanceDescription;
import com.xerox.amazonws.ec2.VolumeInfo;

public class Ec2TypicaTestCase extends IntegrationTestBase {

    // This is so that you can run this test individually.
    public static junit.framework.Test suite() {
        return Ec2TypicaTestCase.getSuite(Ec2TypicaTestCase.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    @Override
    public void testEc2AddGrp() throws Exception {
        // setup

        // act
        ec2.createSecurityGroup("default", "just testing");

        // assert
        // no exception
    }

    @Override
    public void testEc2DelGrp() throws Exception {
        // act
        ec2.deleteSecurityGroup("group2Delete");
    }

    public void testEc2AddGrpFail() throws Exception {
        // setup
        String exceptionMessage = "";
        // act
        try {
            ec2.createSecurityGroup("web", "just testing");
        } catch (EC2Exception e) {
            // assert
            exceptionMessage = e.getMessage();

        }

        assertTrue(exceptionMessage.startsWith("Could not create security group : web"));
    }

    @Override
    public void testEc2AddKey() throws Exception {
        // setup

        // act
        KeyPairInfo result = ec2.createKeyPair("testKey");

        // assert
        assertEquals("testKey", result.getKeyName());
        assertEquals("testKey some key material", result.getKeyMaterial());
        assertEquals("testKey fingerprint", result.getKeyFingerprint());
    }

    @Override
    public void testEc2DKey() throws Exception {
        // setup
        String[] arg0 = new String[] { "testKey1", "testKey2" };

        // act
        List<KeyPairInfo> result = ec2.describeKeyPairs(arg0);

        // assert
        assertEquals(2, result.size());
        assertEquals("testKey1", result.get(0).getKeyName());
        assertEquals("testKey1 fingerprint", result.get(0).getKeyFingerprint());
        assertEquals("testKey2", result.get(1).getKeyName());
        assertEquals("testKey2 fingerprint", result.get(1).getKeyFingerprint());
    }

    @Override
    public void testEc2DelKey() throws Exception {
        // setup

        // act
        ec2.deleteKeyPair("testKey");

        // assert
        // no exception
    }

    @Override
    public void testEc2DeleteSnapshot() throws Exception {
        // setup

        // act
        ec2.deleteSnapshot("snapshotId");
        // assert
        // no exception
    }

    @Override
    public void testEc2DeleteVolume() throws Exception {
        // setup

        // act
        ec2.deleteVolume("vol-123");
        // assert
        // no exception
    }

    @Override
    public void testEc2AllocAddr() throws Exception {
        // setup

        // act
        String result = ec2.allocateAddress();

        // assert
        assertEquals("1.2.3.4", result);
    }

    @Override
    public void testEc2AssocAddr() throws Exception {
        // setup

        // act
        ec2.associateAddress("i-123", "1.2.3.4");

        // assert
        // no exception
    }

    @Override
    public void testEc2AssocAddrFail() throws Exception {
        // setup
        String exceptionMessage = "";
        // act
        try {
            ec2.associateAddress("i-999", "1.2.3.4");
        } catch (EC2Exception e) {
            // assert
            exceptionMessage = e.getMessage();
        }

        assertTrue(exceptionMessage.contains("Could not associate address with instance"));
    }

    @Override
    public void testEc2AttVol() throws Exception {
        // setup

        // act
        AttachmentInfo result = ec2.attachVolume("v-123", "i-123", "/dev/sdb");

        // assert
        assertEquals("/dev/sdb", result.getDevice());
        assertEquals("i-123", result.getInstanceId());
        assertEquals("v-123", result.getVolumeId());
        assertEquals("in-use", result.getStatus());
        assertEquals(2009, result.getAttachTime().get(Calendar.YEAR));
    }

    @Override
    public void testEc2AttVolFail() throws Exception {
        // setup

        // act
        AttachmentInfo result = ec2.attachVolume("v-123", "i-999", "/dev/sdb");

        // assert
        assertEquals("/dev/sdb", result.getDevice());
        assertEquals("i-999", result.getInstanceId());
        assertEquals("v-123", result.getVolumeId());
        assertEquals("deleted", result.getStatus());
        assertEquals(2009, result.getAttachTime().get(Calendar.YEAR));
    }

    @Override
    public void testEc2Auth() throws Exception {
        // setup

        // act
        ec2.authorizeSecurityGroupIngress("default", "tcp", 8080, 9090, "1.2.3.4/16");

        // assert
        // no exception
    }

    @Override
    public void testEc2AuthFail() throws Exception {
        // setup
        String exceptionMessage = "";

        // act
        try {
            ec2.authorizeSecurityGroupIngress("web", "tcp", 8080, 9090, "1.2.3.4/16");
        } catch (EC2Exception e) {
            // assert
            exceptionMessage = e.getMessage();
        }
        assertTrue(exceptionMessage.startsWith("Could not authorize security ingress : web"));
    }

    @Override
    public void testEc2DinSingle() throws Exception {
        // setup
        List<String> instances = new ArrayList<String>();
        instances.add("i-123");

        // act
        List<ReservationDescription> result = ec2.describeInstances(instances);
        System.out.println(result);

        // assert
        assertReservation(result.get(0), "i-123");
    }

    @Override
    public void testEc2DinCrashed() throws Exception {
        // setup
        List<String> instances = new ArrayList<String>();
        instances.add("i-666");

        // act
        List<ReservationDescription> result = ec2.describeInstances(instances);
        System.out.println(result);

        // assert
        assertReservation(result.get(0), "i-666");
    }

    private void assertReservation(ReservationDescription reservationDescription, String instanceId) {
        Instance instance = reservationDescription.getInstances().get(0);
        assertEquals(instanceId, instance.getInstanceId());
        assertEquals("zone-" + instanceId, instance.getAvailabilityZone());
        assertEquals("dns-" + instanceId + ".com", instance.getDnsName());
        assertEquals("emi-" + instanceId, instance.getImageId());
        assertEquals("kernel-" + instanceId, instance.getKernelId());
        assertEquals("key-" + instanceId, instance.getKeyName());
        assertEquals(ImagePlatform.linux.toString(), instance.getPlatform());
        assertEquals("private.dns-" + instanceId + ".com", instance.getPrivateDnsName());
        assertEquals("ramdisk-" + instanceId, instance.getRamdiskId());
        assertEquals("reason-" + instanceId, instance.getReason());
        if ("i-666".equals(instanceId)) {
            assertEquals("crashed", instance.getState());
            assertEquals(24, instance.getStateCode());
        } else {
            assertEquals("running", instance.getState());
            assertEquals(16, instance.getStateCode());
        }
        List<String> groups = reservationDescription.getGroups();
        assertEquals(1, groups.size());
        assertEquals("g-" + instanceId, groups.get(0));

        assertEquals("owner-" + instanceId, reservationDescription.getOwner());
        assertEquals("r-" + instanceId, reservationDescription.getReservationId());
    }

    @Override
    public void testEc2DinMultiple() throws Exception {
        // setup
        List<String> instances = new ArrayList<String>();
        instances.add("i-123");
        instances.add("i-456");

        // act
        List<ReservationDescription> result = ec2.describeInstances(instances);
        System.out.println(result);

        // assert
        assertEquals(1, result.get(0).getInstances().size());
        assertEquals(1, result.get(1).getInstances().size());
        assertTrue(reservationContainsImage(result, "emi-i-123"));
        assertTrue(reservationContainsImage(result, "emi-i-456"));
    }

    private boolean reservationContainsImage(List<ReservationDescription> result, String imageId) {
        for (ReservationDescription reservationDescription : result) {
            for (Instance instance : reservationDescription.getInstances()) {
                if (instance.getImageId().equals(imageId))
                    return true;
            }
        }
        return false;
    }

    @Override
    public void testEc2KillSingle() throws Exception {
        // setup
        List<String> instances = new ArrayList<String>();
        instances.add("i-123");

        // act
        List<TerminatingInstanceDescription> terminatingDescriptions = ec2.terminateInstances(instances);

        // assert
        assertEquals("i-123", terminatingDescriptions.get(0).getInstanceId());
        assertEquals("shutting-down", terminatingDescriptions.get(0).getShutdownState());
    }

    @Override
    public void testEc2KillMultiple() throws Exception {
        // setup
        List<String> instances = new ArrayList<String>();
        instances.add("i-123");
        instances.add("i-456");
        // act
        List<TerminatingInstanceDescription> terminatingDescriptions = ec2.terminateInstances(instances);
        // assert
        assertEquals(2, terminatingDescriptions.size());
        assertEquals("i-123", terminatingDescriptions.get(0).getInstanceId());
        assertEquals("shutting-down", terminatingDescriptions.get(0).getShutdownState());
        assertEquals("i-456", terminatingDescriptions.get(1).getInstanceId());
        assertEquals("shutting-down", terminatingDescriptions.get(1).getShutdownState());
    }

    @Override
    public void testEc2Run() throws Exception {
        // setup
        List<String> groups = new ArrayList<String>();
        groups.add("default");

        // act
        ReservationDescription reservation = ec2.runInstances("i-123", 1, 1, groups, "", "", InstanceType.DEFAULT);

        // assert
        Instance instance = reservation.getInstances().get(0);
        assertEquals(instance.getInstanceId(), "i-123");
    }

    @Override
    public void testEc2RunBadUser() throws Exception {
        // setup
        ec2 = new Jec2(Ec2Setup.ACCESS_KEY + "BOGUS", Ec2Setup.SECRET_KEY, false, "localhost", 8773);
        List<String> groups = new ArrayList<String>();
        List<? extends AWSError> errors = new ArrayList<AWSError>();
        groups.add("default");

        // act
        try {
            ec2.runInstances("i-123", 1, 1, groups, "", "", InstanceType.DEFAULT);
        } catch (EC2Exception e) {
            // assert
            errors = e.getErrors();
        }

        // assert
        assertTrue(errors.size() > 0);
        assertEquals("AuthFailure", errors.get(0).getCode());
        assertEquals("User not found", errors.get(0).getMessage());
    }

    @Override
    public void testEc2csnap() throws Exception {
        // setup

        // act
        SnapshotInfo result = ec2.createSnapshot("v-123");

        // assert
        assertEquals("10.0", result.getProgress());
        assertEquals("pending", result.getStatus());
        assertEquals("v-123", result.getVolumeId());
        assertEquals("snap-123", result.getSnapshotId());
        assertEquals(2009, result.getStartTime().get(Calendar.YEAR));
        assertEquals(9, result.getStartTime().get(Calendar.MONTH));
        assertEquals(27, result.getStartTime().get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void testEc2addvol() throws Exception {
        // setup
        String size = "100";
        String zone = "harmondsworth";

        // act
        VolumeInfo result = ec2.createVolume(size, null, zone);

        // assert
        assertEquals(size, result.getSize());
        assertEquals("creating", result.getStatus());
        assertEquals("v-123", result.getVolumeId());
        assertEquals(zone, result.getZone());
        assertEquals(Calendar.getInstance().get(Calendar.YEAR), result.getCreateTime().get(Calendar.YEAR));
        assertEquals(Calendar.getInstance().get(Calendar.MONTH), result.getCreateTime().get(Calendar.MONTH));
        assertEquals(Calendar.getInstance().get(Calendar.DAY_OF_MONTH), result.getCreateTime().get(Calendar.DAY_OF_MONTH));
    }

    @Override
    public void testEc2addvolTooBig() throws Exception {
        // setup
        String size = "2000";
        String zone = "harmondsworth";
        String exceptionMessage = "";

        // act
        try {
            ec2.createVolume(size, null, zone);
        } catch (EC2Exception e) {

            exceptionMessage = e.getMessage();
        }

        // assert
        assertTrue(exceptionMessage.contains("size must be less than 100"));
    }

    @Override
    public void testEc2DescribeVolume() throws Exception {
        // setup
        List<String> arg0 = new ArrayList<String>();
        arg0.add("vol-123");
        // act
        List<VolumeInfo> volumeInfos = ec2.describeVolumes(arg0);
        // assert
        assertEquals(1, volumeInfos.size());
        assertEquals("vol-123", volumeInfos.get(0).getVolumeId());
    }

    @Override
    public void testEc2DescribeRegionsSingle() throws Exception {
        // act
        List<RegionInfo> regionInfos = ec2.describeRegions(Arrays.asList(new String[] { "US_EAST" }));

        // assert
        assertEquals(1, regionInfos.size());
        assertEquals("US_EAST", regionInfos.get(0).getName());
    }

    @Override
    public void testEc2DescribeRegionsEmpty() throws Exception {
        // act
        List<RegionInfo> regionInfos = ec2.describeRegions(new ArrayList<String>());

        // assert
        assertEquals(3, regionInfos.size());
        assertEquals("US_EAST", regionInfos.get(0).getName());
        assertEquals("US_WEST", regionInfos.get(1).getName());
        assertEquals("UK", regionInfos.get(2).getName());
    }

    @Override
    public void testEc2DescribeSecurityGroups() throws Exception {
        // act
        List<GroupDescription> groupDescriptions = ec2.describeSecurityGroups(new ArrayList<String>());
        // assert
        assertEquals(1, groupDescriptions.size());
        assertEquals("description", groupDescriptions.get(0).getDescription());
        assertEquals("default", groupDescriptions.get(0).getName());
        assertEquals("admin", groupDescriptions.get(0).getOwner());
        List<IpPermission> permissions = groupDescriptions.get(0).getPermissions();
        assertEquals(2, permissions.size());
        for (IpPermission permission : permissions) {
            if (permission.getProtocol().equals("TCP")) {
                assertEquals("0.0.0.0/0", permission.getIpRanges().get(0));
                assertEquals(1, permission.getUidGroupPairs().size());
                assertEquals("otheruser", permission.getUidGroupPairs().get(0)[0]);
                assertEquals("othergroup", permission.getUidGroupPairs().get(0)[1]);
            } else if (permission.getProtocol().equals("UDP")) {
                assertEquals(1, permission.getFromPort());
                assertEquals(2, permission.getToPort());
            } else
                fail("Unknown protocol: " + permission.getProtocol());
        }
    }

    @Override
    public void testEc2DescribeSnapshotsSingle() throws Exception {
        // setup
        List<String> snapshotIds = new ArrayList<String>();
        // act
        List<SnapshotInfo> snapshotInfos = ec2.describeSnapshots(snapshotIds);
        // assert
        assertEquals(1, snapshotInfos.size());
        SnapshotInfo snapshotInfo = snapshotInfos.get(0);
        assertEquals("snapshotId", snapshotInfo.getSnapshotId());
        assertEquals("volumeId", snapshotInfo.getVolumeId());
        assertEquals("pending", snapshotInfo.getStatus());
        assertEquals(1256653231969l, snapshotInfo.getStartTime().getTimeInMillis());
        assertEquals("67.4", snapshotInfo.getProgress());
    }

    @Override
    public void testEc2DescribeSnapshotsMultiple() throws Exception {
        // setup
        List<String> snapshotIds = new ArrayList<String>();
        snapshotIds.add("snap-123");
        snapshotIds.add("snap-456");
        // act
        List<SnapshotInfo> snapshotInfos = ec2.describeSnapshots(snapshotIds);
        // assert
        assertEquals(2, snapshotInfos.size());
        SnapshotInfo snapshotInfo = snapshotInfos.get(0);
        assertEquals("snap-123", snapshotInfo.getSnapshotId());
        assertEquals("volumeId", snapshotInfo.getVolumeId());
        assertEquals("pending", snapshotInfo.getStatus());
        assertEquals(1256653231969l, snapshotInfo.getStartTime().getTimeInMillis());
        assertEquals("67.4", snapshotInfo.getProgress());
    }

    @Test
    public void testEc2DescribeVolumesMulitple() throws Exception {
        // setup
        List<String> arg0 = new ArrayList<String>();
        arg0.add("vol-123");
        arg0.add("vol-456");
        // act
        List<VolumeInfo> volumeInfos = ec2.describeVolumes(arg0);
        // assert
        assertEquals(2, volumeInfos.size());
        assertEquals("vol-123", volumeInfos.get(0).getVolumeId());
        assertEquals("vol-456", volumeInfos.get(1).getVolumeId());
    }

    @Override
    public void testEc2DescribeAddresses() throws Exception {
        // setup
        List<String> arg0 = new ArrayList<String>();
        arg0.add("1.1.1.1");
        arg0.add("2.2.2.2");

        // act
        List<AddressInfo> result = ec2.describeAddresses(arg0);

        // assert
        assertEquals(2, result.size());
        assertEquals("1.1.1.1", result.get(0).getPublicIp());
        assertEquals("i-001", result.get(0).getInstanceId());
        assertEquals("2.2.2.2", result.get(1).getPublicIp());
        assertEquals("i-002", result.get(1).getInstanceId());
    }

    @Override
    public void testEc2DescribeAvailabilityZones() throws Exception {
        // setup
        List<String> arg0 = new ArrayList<String>();
        arg0.add("zone1");
        arg0.add("zone2");

        // act
        List<AvailabilityZone> result = ec2.describeAvailabilityZones(arg0);

        // assert
        assertEquals(2, result.size());
        assertEquals("zone1", result.get(0).getName());
        assertEquals("available", result.get(0).getState());
        assertEquals("zone2", result.get(1).getName());
        assertEquals("available", result.get(1).getState());
    }

    @Override
    public void testEc2DescribeImages() throws Exception {
        // setup
        String[] images = new String[] { "kmi-111", "kmi-222" };
        List<String> arg0 = Arrays.asList(images);

        // act
        List<ImageDescription> result = ec2.describeImages(arg0);

        // assert
        assertEquals(2, result.size());
        assertListContainsStrings(Arrays.asList(new String[] { result.get(0).getImageId(), result.get(1).getImageId() }), images);
        assertListContainsStrings(Arrays.asList(new String[] { result.get(0).getKernelId(), result.get(1).getKernelId() }), new String[] { "k-111", "k-222" });

    }

    private void assertListContainsStrings(List<String> list, String[] strings) {
        assertTrue(list.contains(strings[0]));
        assertTrue(list.contains(strings[1]));
    }

    @Override
    public void testEc2DetachVolume() throws Exception {
        // act
        AttachmentInfo attachmentInfo = ec2.detachVolume("vol-123", "i-123", "/dev/sdb", true);
        // assert
        assertEquals(1864445563437l, attachmentInfo.getAttachTime().getTimeInMillis());
        assertEquals("/dev/sdb", attachmentInfo.getDevice());
        assertEquals("i-123", attachmentInfo.getInstanceId());
        assertEquals("deleted", attachmentInfo.getStatus());
        assertEquals("vol-123", attachmentInfo.getVolumeId());
    }

    @Override
    public void testEc2DisassociateAddress() throws Exception {
        ec2.disassociateAddress("10.249.162.100");
    }

    // @Override
    @Test(expected = EC2Exception.class)
    public void testEc2DisassociateAddressException() throws Exception {
        try {

            ec2.disassociateAddress("10.249.162.999");
            fail("Exception expected");
        } catch (EC2Exception e) {
            // a bit redundant.. but yeah..
            assertTrue(e instanceof EC2Exception);
        }
    }

    @Override
    public void testEc2GetConsoleOutput() throws Exception {
        // setup
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(1864445563437l);
        // act
        ConsoleOutput consoleOutput = ec2.getConsoleOutput("i-123");
        // assert
        assertEquals("Did you really mean to do rm -rf?", consoleOutput.getOutput());
        assertEquals(calendar.getTimeInMillis(), consoleOutput.getTimestamp().getTimeInMillis());
        assertEquals("i-123", consoleOutput.getInstanceId());
    }

    @Override
    public void testEc2RebootInstancesSingle() throws Exception {
        // setup
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add("i-123");
        // act
        ec2.rebootInstances(instanceIds);
    }

    @Override
    public void testEc2RebootInstancesMultiple() throws Exception {
        // setup
        List<String> instanceIds = new ArrayList<String>();
        instanceIds.add("i-123");
        instanceIds.add("i-456");
        // act
        ec2.rebootInstances(instanceIds);
    }

    @Override
    public void testEc2RegisterImage() throws Exception {
        // act
        String id = ec2.registerImage("bucket/funky.img");
        // assert
        assertEquals("kmi-123", id);
    }

    @Override
    public void testEc2DeregisterImage() throws Exception {
        // act
        ec2.deregisterImage("kmi-123");

        // assert
        // no exception
    }

    @Override
    public void testEc2ReleaseAddress() throws Exception {
        // act
        ec2.releaseAddress("10.249.162.100");
    }

    public void testEc2ReleaseAddressException() throws Exception {
        // act
        try {
            ec2.releaseAddress("10.249.162.999");
            fail("Exception expected");
        } catch (EC2Exception e) {
            // a bit redundant.. but yeah..
            assertTrue(e instanceof EC2Exception);
        }
    }

    @Override
    public void testEc2RevokeIngress() throws Exception {
        ec2.revokeSecurityGroupIngress("default", "tcp", 80, 80, "0.0.0.0/0");

    }

    public void testEc2RevokeIngressError() throws Exception {
        try {
            ec2.revokeSecurityGroupIngress("web", "tcp", 80, 80, "0.0.0.0/0");
            fail("Exception expected");
        } catch (EC2Exception e) {
            // a bit redundant.. but yeah..
            assertTrue(e instanceof EC2Exception);
        }
    }

    @Override
    public void testDisableUser() throws Exception {
        // setup
        disableTestUser();

        // act
        try {
            ec2.describeVolumes(new String[] {});
            fail("Should have thrown and exception here");
        } catch (EC2Exception e) {
            assertTrue(e.getMessage().contains("User koala-robustness is not enabled"));
        } catch (Exception e) {
            fail("unexpected exception " + e.getMessage());
        }

        // cleanup
        enableTestUser();
    }
}
