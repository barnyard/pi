package com.bt.pi.api;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import com.bt.pi.api.service.UserManagementService;
import com.ragstorooks.testrr.cli.CommandExecutor;
import com.xerox.amazonws.ec2.Jec2;

public abstract class IntegrationTestBase extends TestCase {
    private static final Log LOG = LogFactory.getLog(IntegrationTestBase.class);
    protected CommandExecutor commandExecutor;
    protected String javaHome;
    protected String javaBaseCommand;
    protected String timKayBaseCommand = "cp etc/timkay-aws/.awsrc ~;cp etc/timkay-aws/.awssecret ~; unset http_proxy; etc/timkay-aws/aws";
    protected String rightscaleBaseCommand = "etc/rightscaleclient.rb " + Ec2Setup.ACCESS_KEY + " " + Ec2Setup.SECRET_KEY;
    protected Jec2 ec2;
    private UserManagementService userManagementService;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        LOG.info("\n\nSTARTING " + getClass().getSimpleName() + "." + getName() + "\n\n");
        commandExecutor = new CommandExecutor(new ScheduledThreadPoolExecutor(20));
        javaHome = System.getProperty("java.home", "/usr");
        javaBaseCommand = String.format("source etc/eucarc.good/eucarc; JAVA_HOME=%s EC2_HOME=etc/ec2-api-tools-1.3-30349 etc/ec2-api-tools-1.3-30349/bin/", javaHome);
        ec2 = new Jec2(Ec2Setup.ACCESS_KEY, Ec2Setup.SECRET_KEY, false, "localhost", 8773);
        if (userManagementService == null)
            userManagementService = (UserManagementService) Ec2Setup.ctx.getBean("userManagementService");
    }

    public static Test getSuite(Class<? extends IntegrationTestBase> clazz) {
        TestSuite ts = new TestSuite(clazz);
        return new Ec2Setup(ts);
    }

    protected String getXmlFromLines(List<String> lines) {
        StringBuffer buffer = new StringBuffer();
        for (String line : lines) {
            buffer.append(line);
        }
        return buffer.toString();
    }

    protected void assertResponse(List<String> lines, String target) {
        for (String line : lines)
            if (StringUtils.trimAllWhitespace(line).contains(StringUtils.trimAllWhitespace(target)))
                return;
        fail();
    }

    protected void assertResponseAbsent(List<String> lines, String target) {
        for (String line : lines)
            if (StringUtils.trimAllWhitespace(line).contains(StringUtils.trimAllWhitespace(target)))
                fail(target + " was found in output: " + lines);
    }

    protected void assertResponse(List<String> lines, String... fields) {
        for (String line : lines) {
            boolean isLine = true;
            String lineToCheck = StringUtils.trimAllWhitespace(line);
            for (int i = 0; i < fields.length; i++) {
                if (!lineToCheck.contains(StringUtils.trimAllWhitespace(fields[i]))) {
                    isLine = false;
                    break;
                }
            }
            if (isLine)
                return;
        }

        fail();
    }

    protected void runCommand(String command) throws Exception {
        String[] commands = new String[] { "/bin/bash", "-c", command };
        commandExecutor.executeScript(commands, Runtime.getRuntime());
    }

    protected void disableTestUser() {
        userManagementService.setUserEnabledFlag(Ec2Setup.USER_ID, false);
    }

    protected void enableTestUser() {
        userManagementService.setUserEnabledFlag(Ec2Setup.USER_ID, true);
    }

    public abstract void testEc2AddGrp() throws Exception;

    public abstract void testEc2DelGrp() throws Exception;

    public abstract void testEc2AddGrpFail() throws Exception;

    public abstract void testEc2AddKey() throws Exception;

    public abstract void testEc2DKey() throws Exception;

    public abstract void testEc2DelKey() throws Exception;

    public abstract void testEc2DeleteSnapshot() throws Exception;

    public abstract void testEc2DeleteVolume() throws Exception;

    public abstract void testEc2AllocAddr() throws Exception;

    public abstract void testEc2AssocAddr() throws Exception;

    public abstract void testEc2AssocAddrFail() throws Exception;

    public abstract void testEc2AttVol() throws Exception;

    public abstract void testEc2AttVolFail() throws Exception;

    public abstract void testEc2Auth() throws Exception;

    public abstract void testEc2AuthFail() throws Exception;

    public abstract void testEc2DinSingle() throws Exception;

    public abstract void testEc2DinCrashed() throws Exception;

    public abstract void testEc2DinMultiple() throws Exception;

    public abstract void testEc2KillSingle() throws Exception;

    public abstract void testEc2KillMultiple() throws Exception;

    public abstract void testEc2Run() throws Exception;

    public abstract void testEc2RunBadUser() throws Exception;

    public abstract void testEc2csnap() throws Exception;

    public abstract void testEc2addvol() throws Exception;

    public abstract void testEc2addvolTooBig() throws Exception;

    public abstract void testEc2DescribeVolume() throws Exception;

    public abstract void testEc2DescribeAddresses() throws Exception;

    public abstract void testEc2DescribeAvailabilityZones() throws Exception;

    public abstract void testEc2DescribeRegionsEmpty() throws Exception;

    public abstract void testEc2DescribeRegionsSingle() throws Exception;

    public abstract void testEc2DescribeSecurityGroups() throws Exception;

    public abstract void testEc2DescribeSnapshotsSingle() throws Exception;

    public abstract void testEc2DescribeSnapshotsMultiple() throws Exception;

    public abstract void testEc2DescribeImages() throws Exception;

    public abstract void testEc2DetachVolume() throws Exception;

    public abstract void testEc2DisassociateAddress() throws Exception;

    public abstract void testEc2GetConsoleOutput() throws Exception;

    public abstract void testEc2RebootInstancesSingle() throws Exception;

    public abstract void testEc2RebootInstancesMultiple() throws Exception;

    public abstract void testEc2RegisterImage() throws Exception;

    public abstract void testEc2DeregisterImage() throws Exception;

    public abstract void testEc2ReleaseAddress() throws Exception;

    public abstract void testEc2RevokeIngress() throws Exception;

    public abstract void testDisableUser() throws Exception;
}
