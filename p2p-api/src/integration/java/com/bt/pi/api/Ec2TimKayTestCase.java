package com.bt.pi.api;

import java.util.List;

import junit.textui.TestRunner;

import org.junit.Test;

public class Ec2TimKayTestCase extends IntegrationTestBase {

    // This is so that you can run this test individually.
    public static junit.framework.Test suite() {
        return Ec2TimKayTestCase.getSuite(Ec2TimKayTestCase.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    @Override
    public void testEc2AddGrp() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " -vvv addgrp default -d \"just testing\"");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "true");
    }

    @Test
    public void testIllegalStateException() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " -vvv addgrp default -d \"illegalStateException\"");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| SOAP-ENV:Client | oh dear! |");
    }

    @Test
    public void testEc2AddGrpClientError() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " -vvv addgrp default -d \"apiException\"");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| SOAP-ENV:Client | client error |");
    }

    @Test
    public void testEc2AddGrpServerError() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " -vvv addgrp default -d \"apiServerException\"");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| SOAP-ENV:Server | Internal Server Error |");
    }

    @Override
    public void testEc2DelGrp() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " delgrp group2Delete");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "true");
    }

    @Override
    public void testEc2AddGrpFail() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " addgrp web -d \"just testing\"");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "false");
    }

    @Override
    public void testEc2AddKey() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " addkey testKey");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(1, outputLines.size());
        assertEquals("testKey some key material", outputLines.get(0));
    }

    @Override
    public void testEc2DKey() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " dkey testKey1 testKey2");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| testKey1 | testKey1 fingerprint |");
        assertResponse(outputLines, "| testKey2 | testKey2 fingerprint |");
    }

    @Override
    public void testEc2DelKey() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " delkey testKey");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| true   |");
    }

    @Override
    public void testEc2DeleteSnapshot() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " delsnap snapshotid");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| true   |");
    }

    @Override
    public void testEc2DeleteVolume() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " delvol vol-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| true   |");
    }

    @Override
    public void testEc2AllocAddr() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " allad");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "1.2.3.4");
    }

    @Override
    public void testEc2AssocAddr() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " aad 1.2.3.4 -i i-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "true");
    }

    @Override
    public void testEc2AssocAddrFail() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " aad 1.2.3.4 -i i-999");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "false");
    }

    @Override
    public void testEc2AttVol() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " attvol v-123 -i i-123 -d /dev/sdb");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| v-123    | i-123      | /dev/sdb | in-use | 2009-11-02T14:58:30.461Z |");
    }

    @Override
    public void testEc2AttVolFail() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " attvol v-123 -i i-999 -d /dev/sdb");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| v-123    | i-999      | /dev/sdb | deleted | 2009-11-02T14:58:30.461Z |");
    }

    @Override
    public void testEc2Auth() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " auth default -P tcp -p 80 -s 1.2.3.4/16");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "true");
    }

    @Override
    public void testEc2AuthFail() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " auth web -P tcp -p 80 -s 1.2.3.4/16");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "false");
    }

    @Override
    public void testEc2DinSingle() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " din i-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals("i-123\trunning\tdns-i-123.com", outputLines.get(0));
    }

    @Override
    public void testEc2DinCrashed() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " din i-666");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals("i-666\tcrashed\tdns-i-666.com", outputLines.get(0));
    }

    @Override
    public void testEc2DinMultiple() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " din i-1 i-2 i-3");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertLine(outputLines, "i-1	running	dns-i-1.com");
        assertLine(outputLines, "i-2	running	dns-i-2.com");
        assertLine(outputLines, "i-3	running	dns-i-3.com");
    }

    @Override
    public void testEc2KillSingle() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " tin i-1");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertKillResponse(outputLines, 1);
    }

    private void assertKillResponse(List<String> lines, int i) {
        String target = "i-" + i;
        boolean found = false;
        for (String line : lines) {
            if (line.contains(target)) {
                found = true;
                assertTrue(line.contains("code=32 name=shutting-down | code=16 name=running"));
            }
        }
        assertTrue(found);
    }

    @Override
    public void testEc2KillMultiple() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " tin i-1 i-2 i-3");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertKillResponse(outputLines, 1);
        assertKillResponse(outputLines, 2);
        assertKillResponse(outputLines, 3);
    }

    @Override
    public void testEc2Run() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " run emi-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines);
    }

    private void assertResponse(List<String> lines) {
        for (String line : lines) {
            if (line.equals("i-123\tpending\tdns-123.com"))
                return;
        }
        fail();
    }

    private void assertLine(List<String> lines, String line) {
        for (String l : lines) {
            if (l.equals(line))
                return;
        }
        fail();
    }

    @Override
    public void testEc2RunBadUser() throws Exception {
        // setup
        timKayBaseCommand = "cp etc/timkay-aws/.awsrc ~;cp etc/timkay-aws/.awssecret.bad ~/.awssecret; unset http_proxy; etc/timkay-aws/aws";

        // act
        runCommand(timKayBaseCommand + " run emi-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertTrue(errorIsReported(outputLines, "AuthFailure", "User not found"));
    }

    private boolean errorIsReported(List<String> outputLines, String code, String message) {
        for (String line : outputLines)
            if (line.contains(code) && line.contains(message))
                return true;
        return false;
    }

    @Override
    public void testEc2csnap() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " csnap v-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| snap-123   | v-123    | pending | 2009-10-27T14:20:31.969Z | 10.0     |");
    }

    @Override
    public void testEc2addvol() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " cvol -size 99 -zone harmo");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| v-123    | 99   | availabilityZone=harmo status=creating createTime=20");
    }

    @Override
    public void testEc2addvolTooBig() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " cvol -size 101 -zone harmo");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "size must be less than 100");
    }

    @Override
    public void testEc2DescribeVolume() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " describe-volumes vol-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| vol-123  | 800  | snap-123   | IceCube          | in-use | 2029-01-30T05:32:43.437Z |");
    }

    @Override
    public void testEc2DescribeAddresses() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " dad 1.1.1.1 2.2.2.2");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| 1.1.1.1  | i-001      |");
        assertResponse(outputLines, "| 2.2.2.2  | i-002      |");
    }

    @Override
    public void testEc2DescribeAvailabilityZones() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " daz zone1 zone2");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| zone1    | available | US_EAST |");
        assertResponse(outputLines, "| zone2    | available | US_WEST |");
    }

    @Override
    public void testEc2DescribeRegionsEmpty() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " describe-regions");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "|            | US_EAST    |");
        assertResponse(outputLines, "|            | US_WEST    |");
        assertResponse(outputLines, "|            | UK         |");
    }

    @Override
    public void testEc2DescribeRegionsSingle() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " describe-regions US_EAST");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "|            | US_EAST    |");
    }

    @Override
    public void testEc2DescribeSecurityGroups() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " dgrp");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "|         |", "|           |", "|                  |", "| TCP        |", "| 0        |", "| 0      |", "| item= userId=otheruser groupName=othergroup |", "| item= cidrIp=0.0.0.0/0 |");
        assertResponse(outputLines, "|         |", "|           |", "|                  |", "| UDP        |", "| 1        |", "| 2      |", "|                                             |", "| item= cidrIp=0.0.0.0/0 |");
    }

    @Test
    public void testEc2DescribeSecurityGroupsMulti() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " dgrp a b");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| admin   | default");
        assertResponse(outputLines, "|         |", "|           |", "|                  |", "| TCP        |", "| 0        |", "| 0      |", "| item= userId=otheruser groupName=othergroup |", "| item= cidrIp=0.0.0.0/0 |");
        assertResponse(outputLines, "|         |", "|           |", "|                  |", "| UDP        |", "| 1        |", "| 2      |", "|                                             |", "| item= cidrIp=0.0.0.0/0 |");
        assertResponse(outputLines, "| admin1  | default1");
    }

    @Override
    public void testEc2DescribeSnapshotsSingle() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " dsnap");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| snapshotId | volumeId | pending | 2009-10-27T14:20:31.969Z | 67.4     |");
    }

    @Override
    public void testEc2DescribeSnapshotsMultiple() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " dsnap snap-123 snap-456");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertEquals(5, outputLines.size());
        assertResponse(outputLines, "| snap-456   | volumeId | pending | 2009-10-27T14:20:31.969Z | 67.4     |");
    }

    @Override
    public void testEc2DescribeImages() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " dim kmi-111 kmi-222");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| kmi-222 | manifest      | PENDING    | userid       | true     | architecture | KERNEL    | k-222    | r-111     | linux    |");
        assertResponse(outputLines, "| kmi-111 | manifest      | PENDING    | userid       | true     | architecture | KERNEL    | k-111    | r-111     | linux    |");
    }

    @Override
    public void testEc2DetachVolume() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " detach-volume vol-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| vol-123  | device= status=deleted attachTime=2029-01-30T05:32:43.437Z |");
    }

    @Override
    public void testEc2DisassociateAddress() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " disassociate-address 10.249.162.100");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "return>true</");
    }

    @Override
    public void testEc2GetConsoleOutput() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " get-console-output i-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "Did you really mean to do rm -rf?");
    }

    @Override
    public void testEc2RebootInstancesSingle() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " reboot-instances i-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| true   |");
    }

    @Override
    public void testEc2RebootInstancesMultiple() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " reboot-instances i-123 i-456");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| true   |");
    }

    @Override
    public void testEc2RegisterImage() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " register bucket/funky.img");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "imageId>kmi-123</");
    }

    @Override
    public void testEc2DeregisterImage() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " deregister kmi-123");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "return>true</");
    }

    @Override
    public void testEc2ReleaseAddress() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " rad 10.249.162.100");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| true   |");
    }

    @Override
    public void testEc2RevokeIngress() throws Exception {
        // setup

        // act
        runCommand(timKayBaseCommand + " revoke default -P tcp -f 80 -t 80 -s 0.0.0.0/0");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "| true   |");
    }

    @Override
    public void testDisableUser() throws Exception {
        // setup
        disableTestUser();

        // act
        runCommand(timKayBaseCommand + " dim kmi-111 kmi-222");

        // assert
        List<String> outputLines = commandExecutor.getOutputLines();
        assertResponse(outputLines, "User koala-robustness is not enabled");

        // cleanup
        enableTestUser();
    }
}
