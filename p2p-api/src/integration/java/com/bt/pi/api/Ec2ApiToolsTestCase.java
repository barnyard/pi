package com.bt.pi.api;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import junit.framework.Test;
import junit.textui.TestRunner;

import com.bt.pi.app.common.images.platform.ImagePlatform;

public class Ec2ApiToolsTestCase extends IntegrationTestBase {

    // This is so that you can run this test individually.
    public static Test suite() {
        return Ec2ApiToolsTestCase.getSuite(Ec2ApiToolsTestCase.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    @Override
    public void testEc2AddGrp() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2addgrp default -d \"just testing\"", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertEquals("GROUP\tdefault\tjust testing", outputLines.get(0).trim());
    }

    @org.junit.Test
    public void testEc2AddGrpUnsupportedVersion() throws Exception {
        // setup
        String baseCommand = String.format("source etc/eucarc.good/eucarc; JAVA_HOME=%s EC2_HOME=etc/ec2-api-tools-1.3-53907 etc/ec2-api-tools-1.3-53907/bin/", javaHome);

        // act
        runCommand(String.format("%sec2addgrp -v default -d \"just testing\"", baseCommand));

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertEquals("Client: unsupported AWS version", errorLines.get(0));
    }

    @org.junit.Test
    public void testEc2AddGrpClientError() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2addgrp -v default -d \"apiException\"", javaBaseCommand));

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertEquals("Client: client error", errorLines.get(0));
    }

    @org.junit.Test
    public void testEc2AddGrpServerError() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2addgrp -v default -d \"apiServerException\"", javaBaseCommand));

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertEquals("Server: Internal Server Error", errorLines.get(0));
    }

    @Override
    public void testEc2DelGrp() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2delgrp group2Delete", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertTrue(outputLines.get(0).trim().contains("GROUP"));
        assertTrue(outputLines.get(0).trim().contains("group2Delete"));
    }

    @Override
    public void testEc2AddGrpFail() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2addgrp web -d \"just testing\"", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(0, outputLines.size());
        // TODO: be nice to assert non zero return code
    }

    @Override
    public void testEc2AddKey() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2addkey testKey", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertEquals("KEYPAIR\ttestKey\ttestKey fingerprint", outputLines.get(0).trim());
        assertEquals("testKey some key material", outputLines.get(1).trim());
    }

    @Override
    public void testEc2DKey() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2dkey testKey1 testKey2", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertEquals("KEYPAIR\ttestKey1\ttestKey1 fingerprint", outputLines.get(0).trim());
        assertEquals("KEYPAIR\ttestKey2\ttestKey2 fingerprint", outputLines.get(1).trim());
    }

    @Override
    public void testEc2DelKey() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2delkey testKey", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertEquals("KEYPAIR\ttestKey", outputLines.get(0).trim());
    }

    @Override
    public void testEc2DeleteSnapshot() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2delsnap snapshotid", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertEquals("SNAPSHOT	snapshotid", outputLines.get(0).trim());
    }

    @Override
    public void testEc2DeleteVolume() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2delvol vol-123", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertEquals("VOLUME	vol-123", outputLines.get(0).trim());
    }

    @Override
    public void testEc2AllocAddr() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2allocaddr", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertEquals("ADDRESS\t1.2.3.4", outputLines.get(0).trim());
    }

    @Override
    public void testEc2AssocAddr() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2assocaddr 1.2.3.4 -i i-123", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertEquals("ADDRESS\t1.2.3.4\ti-123", outputLines.get(0).trim());
    }

    @Override
    public void testEc2AssocAddrFail() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2assocaddr 1.2.3.4 -i i-999", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(0, outputLines.size());
        // TODO: be nice to assert non zero return code
    }

    @Override
    public void testEc2AttVol() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2attvol v-123 -i i-123 -d /dev/sdb", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertTrue(outputLines.get(0).trim().contains("ATTACHMENT\tv-123\ti-123\t/dev/sdb\tin-use\t20"));
    }

    @Override
    public void testEc2AttVolFail() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2attvol v-123 -i i-999 -d /dev/sdb", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertTrue(outputLines.get(0).trim().contains("ATTACHMENT\tv-123\ti-999\t/dev/sdb\tdeleted\t20"));
    }

    @Override
    public void testEc2Auth() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2auth default -P tcp -p 80", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertEquals("GROUP\t\tdefault", outputLines.get(0).trim());
        assertEquals("PERMISSION\t\tdefault\tALLOWS\ttcp\t80\t80\tFROM\tCIDR\t0.0.0.0/0", outputLines.get(1).trim());
    }

    @Override
    public void testEc2AuthFail() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2auth web -P tcp -p 80", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        System.out.println(outputLines);
        // TODO: the integration handler returns false here, but the ec2 tools don't seem to care?
    }

    @Override
    public void testEc2DinSingle() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2din i-123", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertReservation(outputLines.get(0), "i-123");
        assertInstance(outputLines.get(1), "i-123");
    }

    @Override
    public void testEc2DinCrashed() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2din i-666", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertReservation(outputLines.get(0), "i-666");
        assertInstance(outputLines.get(1), "i-666");
    }

    private void assertReservation(String line, String instanceId) {
        String[] split = line.split("\t");
        assertEquals("RESERVATION", split[0]);
        assertEquals("r-" + instanceId, split[1]);
        assertEquals("owner-" + instanceId, split[2]);
        assertEquals("g-" + instanceId, split[3]);
    }

    private void assertInstance(String line, String instanceId) {
        String index = instanceId.split("-")[1];
        String[] split = line.split("\t");
        assertEquals(15, split.length);
        assertEquals("INSTANCE", split[0]);
        assertEquals(instanceId, split[1]);
        assertEquals("emi-" + instanceId, split[2]);
        assertEquals("dns-" + instanceId + ".com", split[3]);
        assertEquals("private.dns-" + instanceId + ".com", split[4]);
        if ("i-666".equals(instanceId))
            assertEquals("crashed", split[5]);
        else
            assertEquals("running", split[5]);
        assertEquals("key-" + instanceId, split[6]);
        assertEquals(index, split[7]);
        assertEquals("type-" + instanceId, split[9]);
        assertTrue(split[10].startsWith("2009-"));
        assertEquals("zone-" + instanceId, split[11]);
        assertEquals("kernel-" + instanceId, split[12]);
        assertEquals("ramdisk-" + instanceId, split[13]);
        assertEquals(ImagePlatform.linux.toString(), split[14]);
    }

    @Override
    public void testEc2DinMultiple() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2din i-123 i-456", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(4, outputLines.size());
        assertResponse(outputLines, "RESERVATION    r-i-123 owner-i-123 g-i-123");
        assertResponse(outputLines, "INSTANCE   i-123   emi-i-123 dns-i-123.com private.dns-i-123.com running key-i-123 123     type-i-123    2009-10-29T16:34:23+0000    zone-i-123    kernel-i-123  ramdisk-i-123 linux");
        assertResponse(outputLines, "RESERVATION    r-i-456 owner-i-456 g-i-456");
        assertResponse(outputLines, "INSTANCE	i-456	emi-i-456	dns-i-456.com	private.dns-i-456.com	running	key-i-456	456		type-i-456	2009-10-29T16:34:23+0000	zone-i-456	kernel-i-456	ramdisk-i-456	linux");
    }

    @Override
    public void testEc2KillSingle() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2kill i-123", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertKilledInstance(outputLines.get(0), "i-123");
    }

    @Override
    public void testEc2KillMultiple() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2kill i-1 i-2 i-3", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(3, outputLines.size());
        assertKilledInstance(outputLines.get(0), "i-1");
        assertKilledInstance(outputLines.get(1), "i-2");
        assertKilledInstance(outputLines.get(2), "i-3");
    }

    private void assertKilledInstance(String line, String instanceId) {
        String[] split = line.split("\t");
        assertEquals(4, split.length);
        assertEquals("INSTANCE", split[0]);
        assertEquals(instanceId, split[1]);
        assertEquals("running", split[2]);
        assertEquals("shutting-down", split[3]);
    }

    @Override
    public void testEc2Run() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2run emi-123", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertReservation(outputLines.get(0));
        assertInstance(outputLines.get(1));
    }

    @Override
    public void testEc2RunBadUser() throws Exception {
        // setup
        javaBaseCommand = String.format("source etc/eucarc.bad/eucarc; JAVA_HOME=%s EC2_HOME=etc/ec2-api-tools-1.3-30349 etc/ec2-api-tools-1.3-30349/bin/", javaHome);

        // act
        runCommand(String.format("%sec2run emi-123", javaBaseCommand));

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        String targetErrorMessage = "Server returned error code = 400";
        assertTrue(linesContainTarget(errorLines, targetErrorMessage));
    }

    private boolean linesContainTarget(List<String> lines, String target) {
        for (String line : lines)
            if (line.contains(target))
                return true;
        return false;
    }

    private void assertInstance(String line) {
        String[] split = line.split("\t");
        assertEquals(15, split.length);
        assertEquals("INSTANCE", split[0]);
        assertEquals("i-123", split[1]);
        assertEquals("emi-123", split[2]);
        assertEquals("dns-123.com", split[3]);
        assertEquals("dns-123.private.com", split[4]);
        assertEquals("pending", split[5]);
        assertEquals("key-123", split[6]);
        assertEquals("1", split[7]);
        assertEquals("large-123", split[9]);
        Date today = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-");
        assertTrue(split[10].startsWith(formatter.format(today)));
        assertEquals("zone-123", split[11]);
        assertEquals("kernel-123", split[12]);
        assertEquals("ramdisk-123", split[13]);
        assertEquals(ImagePlatform.windows.toString(), split[14]);
    }

    private void assertReservation(String line) {
        String[] split = line.split("\t");
        assertEquals(4, split.length);
        assertEquals("RESERVATION", split[0]);
        assertEquals("r-123", split[1]);
        assertEquals("owner-123", split[2]);
        assertEquals("default-123", split[3]);
    }

    @Override
    public void testEc2csnap() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2addsnap v-123", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());

        String[] split = outputLines.get(0).split("\t");
        System.out.println(Arrays.toString(split));
        assertEquals("SNAPSHOT", split[0]);
        assertEquals("snap-123", split[1]);
        assertEquals("v-123", split[2]);
        assertEquals("pending", split[3]);
        assertTrue(split[4].startsWith("20"));
        assertEquals("10.0", split[5]);
    }

    @Override
    public void testEc2addvol() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2addvol -s 100 -z harmo", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertTrue(outputLines.get(0).startsWith("VOLUME\tv-123\t100\t\tharmo\tcreating\t20"));
    }

    @Override
    public void testEc2addvolTooBig() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2addvol -s 2512 -z harmo", javaBaseCommand));

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        assertEquals(1, errorLines.size());
        assertEquals("Client: size must be less than 100", errorLines.get(0));
    }

    @Override
    public void testEc2DescribeVolume() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-describe-volumes vol-123", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertTrue(outputLines.get(0).startsWith("VOLUME\tvol-123\t800\tsnap-123\tIceCube\tin-use\t2029-01-30T05:32:43+0000"));
    }

    public void testEc2DescribeVolumeMultiple() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-describe-volumes vol-123 vol-456", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertTrue(outputLines.get(0).startsWith("VOLUME\tvol-123\t800\tsnap-123\tIceCube\tin-use\t2029-01-30T05:32:43+0000"));
        assertTrue(outputLines.get(1).startsWith("VOLUME\tvol-456\t800\tsnap-123\tIceCube\tin-use\t2029-01-30T05:32:43+0000"));
    }

    @Override
    public void testEc2DescribeAddresses() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-describe-addresses 1.1.1.1 2.2.2.2", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertEquals("ADDRESS\t1.1.1.1\ti-001", outputLines.get(0));
        assertEquals("ADDRESS\t2.2.2.2\ti-002", outputLines.get(1));
    }

    @Override
    public void testEc2DescribeAvailabilityZones() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2daz zone1 zone2", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertEquals("AVAILABILITYZONE\tzone1\tavailable\tUS_EAST", outputLines.get(0));
        assertEquals("AVAILABILITYZONE\tzone2\tavailable\tUS_WEST", outputLines.get(1));
    }

    @Override
    public void testEc2DescribeImages() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2dim kmi-111 kmi-222", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        System.out.println(Arrays.toString(outputLines.get(0).split("\t")));
        System.out.println(Arrays.toString(outputLines.get(1).split("\t")));
        assertEquals("IMAGE	kmi-111	manifest	userid	PENDING	public		architecture	KERNEL	k-111	r-111	linux", outputLines.get(0));
        assertEquals("IMAGE	kmi-222	manifest	userid	PENDING	public		architecture	KERNEL	k-222	r-111	linux", outputLines.get(1));
    }

    @Override
    public void testEc2DetachVolume() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-detach-volume vol-123 -i i-123 -d /dev/sdb -f", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertTrue(outputLines.get(0).startsWith("ATTACHMENT\tvol-123\ti-123\t/dev/sdb\tdeleted\t2029-01-30T05:32:43+0000"));
    }

    @Override
    public void testEc2DisassociateAddress() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-disassociate-address 10.249.162.100", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertTrue(outputLines.get(0).startsWith("ADDRESS\t10.249.162.100"));
    }

    @Override
    public void testEc2GetConsoleOutput() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-get-console-output i-123", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "2029-01-30T05:32:43+0000");
        assertResponse(outputLines, "Did you really mean to do rm -rf?");
    }

    @Override
    public void testEc2RebootInstancesSingle() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-reboot-instances i-123 -v", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "return>true</");
    }

    @Override
    public void testEc2RebootInstancesMultiple() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-reboot-instances i-123 i-456 -v", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "return>true</");
    }

    @Override
    public void testEc2RegisterImage() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2reg bucket/funky.img", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertTrue(outputLines.get(0).equals("IMAGE\tkmi-123"));
    }

    @Override
    public void testEc2DeregisterImage() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2dereg kmi-123", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertTrue(outputLines.get(0).equals("IMAGE\tkmi-123"));
    }

    @Override
    public void testEc2ReleaseAddress() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2reladdr 10.249.162.100", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertTrue(outputLines.get(0).startsWith("ADDRESS"));
        assertTrue(outputLines.get(0).contains("10.249.162.100"));
    }

    @Override
    public void testEc2RevokeIngress() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-revoke -Ptcp -p80 default", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertTrue(outputLines.get(1).equals("PERMISSION		default	ALLOWS	tcp	80	80	FROM	CIDR	0.0.0.0/0"));
    }

    @Override
    public void testEc2DescribeRegionsEmpty() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-describe-regions", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(3, outputLines.size());
    }

    @Override
    public void testEc2DescribeRegionsSingle() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2-describe-regions US_EAST", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
    }

    @Override
    public void testEc2DescribeSecurityGroups() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2dgrp", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(4, outputLines.size());
        assertResponse(outputLines, "PERMISSION	admin	default	ALLOWS	TCP	0	0	FROM	USER	otheruser	GRPNAME	othergroup");
        assertResponse(outputLines, "PERMISSION	admin	default	ALLOWS	TCP	0	0	FROM	CIDR	0.0.0.0/0");
        assertResponse(outputLines, "PERMISSION	admin	default	ALLOWS	UDP	1	2	FROM	CIDR	0.0.0.0/0");
    }

    @org.junit.Test
    public void testEc2DescribeSecurityGroupsMulti() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2dgrp a b", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(8, outputLines.size());
        assertResponse(outputLines, "GROUP admin  default    description");
        assertResponse(outputLines, "PERMISSION admin   default ALLOWS  TCP 0   0   FROM    USER    otheruser   GRPNAME othergroup");
        assertResponse(outputLines, "PERMISSION admin   default ALLOWS  TCP 0   0   FROM    CIDR    0.0.0.0/0");
        assertResponse(outputLines, "PERMISSION admin   default ALLOWS  UDP 1   2   FROM    CIDR    0.0.0.0/0");
        assertResponse(outputLines, "GROUP admin1  default1    description");
        assertResponse(outputLines, "PERMISSION admin1  default1    ALLOWS  TCP 0   0   FROM    USER    otheruser   GRPNAME othergroup");
        assertResponse(outputLines, "PERMISSION admin1  default1    ALLOWS  TCP 0   0   FROM    CIDR    0.0.0.0/0");
        assertResponse(outputLines, "PERMISSION admin1  default1    ALLOWS  UDP 1   2   FROM    CIDR    0.0.0.0/0");
    }

    @Override
    public void testEc2DescribeSnapshotsSingle() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2dsnap", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertResponse(outputLines, "SNAPSHOT	snapshotId	volumeId	pending	2009-10-27T14:20:31+0000	67.4");
    }

    @Override
    public void testEc2DescribeSnapshotsMultiple() throws Exception {
        // setup

        // act
        runCommand(String.format("%sec2dsnap snap-123 snap-345", javaBaseCommand));

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(2, outputLines.size());
        assertResponse(outputLines, "SNAPSHOT	snap-123	volumeId	pending	2009-10-27T14:20:31+0000	67.4");
        assertResponse(outputLines, "SNAPSHOT	snap-345	volumeId	pending	2009-10-27T14:20:31+0000	67.4");
    }

    @Override
    public void testDisableUser() throws Exception {
        // setup
        disableTestUser();

        // act
        runCommand(String.format("%sec2dgrp", javaBaseCommand));

        // assert
        List<String> errorLines = commandExecutor.getErrorLines();
        String targetErrorMessage = "Server returned error code = 403";
        assertTrue(linesContainTarget(errorLines, targetErrorMessage));
        enableTestUser();
    }
}
