package com.bt.pi.api;

import junit.framework.Test;
import junit.textui.TestRunner;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;

/*
 * this API will not yet work as we don't currently support version 2010-08-31
 */
public class AwsJavaSdkTestCase extends IntegrationTestBase {
    private AmazonEC2 ec2;

    // This is so that you can run this test individually.
    public static Test suite() {
        return AwsJavaSdkTestCase.getSuite(AwsJavaSdkTestCase.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        AWSCredentials credentials = new BasicAWSCredentials(Ec2Setup.ACCESS_KEY, Ec2Setup.SECRET_KEY);
        ec2 = new AmazonEC2Client(credentials);
        ec2.setEndpoint("http://127.0.0.1:8773");
    }

    @Override
    public void testEc2AddGrp() throws Exception {
        // setup
        CreateSecurityGroupRequest createSecurityGroupRequest = new CreateSecurityGroupRequest("default", "just testing");

        // act
        try {
            ec2.createSecurityGroup(createSecurityGroupRequest);
            fail("expected Amazon Service Exception");
        } catch (AmazonServiceException e) {
            assertTrue(e.getMessage().contains("version 2010-08-31 is not supported"));
        }
    }

    @Override
    public void testEc2DelGrp() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2AddGrpFail() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2AddKey() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DKey() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DelKey() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DeleteSnapshot() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DeleteVolume() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2AllocAddr() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2AssocAddr() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2AssocAddrFail() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2AttVol() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2AttVolFail() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2Auth() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2AuthFail() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DinSingle() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DinMultiple() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2KillSingle() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2KillMultiple() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2Run() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2RunBadUser() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2csnap() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2addvol() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2addvolTooBig() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DescribeVolume() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DescribeAddresses() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DescribeAvailabilityZones() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DescribeRegionsEmpty() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DescribeRegionsSingle() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DescribeSecurityGroups() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DescribeSnapshotsSingle() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DescribeSnapshotsMultiple() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DescribeImages() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DetachVolume() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DisassociateAddress() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2GetConsoleOutput() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2RebootInstancesSingle() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2RebootInstancesMultiple() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2RegisterImage() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DeregisterImage() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2ReleaseAddress() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2RevokeIngress() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testDisableUser() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void testEc2DinCrashed() throws Exception {
        // TODO Auto-generated method stub
    }
}
