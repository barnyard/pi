package com.bt.pi.api;

import junit.framework.Test;
import junit.textui.TestRunner;

public class Ec2RightScaleTestCase extends IntegrationTestBase {

    private String rightscaleBaseCommand20081201 = "export EC2_API_VERSION=2008-12-01;" + rightscaleBaseCommand;

    // This is so that you can run this test individually.
    public static Test suite() {
        return Ec2RightScaleTestCase.getSuite(Ec2RightScaleTestCase.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    @Override
    public void testEc2AddGrp() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " create_security_group default \"hello\"");

        // assert
        assertResponseAbsent(commandExecutor.getErrorLines(), "Parameter Timestamp not in correct format");
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2AddGrp20081201() throws Exception {
        runCommand(rightscaleBaseCommand20081201 + " create_security_group default \"hello\"");

        // assert
        assertResponseAbsent(commandExecutor.getErrorLines(), "Parameter Timestamp not in correct format");
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2AddGrp20081202() throws Exception {
        String rightscaleBaseCommand20081202 = "export EC2_API_VERSION=2008-12-02;" + rightscaleBaseCommand;
        runCommand(rightscaleBaseCommand20081202 + " create_security_group default \"hello\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "version 2008-12-02 is not supported");
    }

    @Override
    public void testEc2AddGrpFail() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " create_security_group web \"just testing\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: false");
    }

    @Override
    public void testEc2AddKey() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "aws_fingerprinttestKey fingerprint", "aws_materialtestKey some key material", "aws_key_nametestKey" };

        // act
        runCommand(rightscaleBaseCommand + " create_key_pair testKey");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2AddKey20081201() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "aws_fingerprinttestKey fingerprint", "aws_materialtestKey some key material", "aws_key_nametestKey" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " create_key_pair testKey");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2AllocAddr() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " allocate_address");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: 1.2.3.4");
    }

    @org.junit.Test
    public void testEc2AllocAddr20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " allocate_address");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: 1.2.3.4");
    }

    @Override
    public void testEc2AssocAddr() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " associate_address \"i-123\" \"1.2.3.4\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2AssocAddr20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " associate_address \"i-123\" \"1.2.3.4\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2AssocAddrFail() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " associate_address \"i-999\" \"1.2.3.4\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: false");
    }

    @Override
    public void testEc2AttVol() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_device/dev/sdb", "aws_idv-123", "aws_attachment_statusin-use", "aws_attached_atMon Nov 02 14:58:30 UTC 2009", "aws_instance_idi-123" };

        // act
        runCommand(rightscaleBaseCommand + " attach_volume \"v-123\" \"i-123\" \"/dev/sdb\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2AttVol20081201() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_device/dev/sdb", "aws_idv-123", "aws_attachment_statusin-use", "aws_attached_atMon Nov 02 14:58:30 UTC 2009", "aws_instance_idi-123" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " attach_volume \"v-123\" \"i-123\" \"/dev/sdb\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2AttVolFail() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_device/dev/sdb", "aws_idv-123", "aws_attachment_statusdeleted", "aws_attached_atMon Nov 02 14:58:30 UTC 2009", "aws_instance_idi-999" };

        // act
        runCommand(rightscaleBaseCommand + " attach_volume \"v-123\" \"i-999\" \"/dev/sdb\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2Auth() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " authorize_security_group_IP_ingress \"default\" \"80\" \"80\" \"tcp\" \"1.2.3.4/16\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2Auth20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " authorize_security_group_IP_ingress \"default\" \"80\" \"80\" \"tcp\" \"1.2.3.4/16\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2AuthFail() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " authorize_security_group_IP_ingress \"web\" \"80\" \"80\" \"tcp\" \"1.2.3.4/16\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: false");
    }

    @Override
    public void testEc2DKey() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "aws_fingerprinttestKey1 fingerprint", "aws_key_nametestKey1" };

        // act

        runCommand(rightscaleBaseCommand + " describe_key_pairs \"testKey1\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DKey20081201() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "aws_fingerprinttestKey1 fingerprint", "aws_key_nametestKey1" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " describe_key_pairs \"testKey1\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DelGrp() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " delete_security_group group2Delete");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2DelGrp20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " delete_security_group group2Delete");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2DelKey() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " delete_key_pair testKey");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2DelKey20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " delete_key_pair testKey");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2DeleteSnapshot() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " delete_snapshot snapshotid");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2DeleteSnapshot20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " delete_snapshot snapshotid");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2DeleteVolume() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " delete_volume vol-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2DeleteVolume20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " delete_volume vol-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2DeregisterImage() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " deregister_image  kmi-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2DeregisterImage20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " deregister_image  kmi-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2DescribeAddresses() throws Exception {
        // setup
        String[] expectedResults = { "result:", "public_ip1.1.1.1", "instance_idi-001", "public_ip2.2.2.2", "instance_idi-002" };

        // act
        runCommand(rightscaleBaseCommand + " describe_addresses \"1.1.1.1\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DescribeAddresses20081201() throws Exception {
        // setup
        String[] expectedResults = { "result:", "public_ip1.1.1.1", "instance_idi-001", "public_ip2.2.2.2", "instance_idi-002" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " describe_addresses \"1.1.1.1\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DescribeAvailabilityZones() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "zone_namezone1", "zone_stateavailable", "region_nameUS_EAST", "zone_namezone2", " zone_stateavailable", "region_nameUS_WEST" };

        // act
        runCommand(rightscaleBaseCommand + " describe_availability_zones  \"zone1\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DescribeAvailabilityZones20081201() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "zone_namezone1", "zone_stateavailable", "region_nameUS_EAST", "zone_namezone2", " zone_stateavailable", "region_nameUS_WEST" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " describe_availability_zones  \"zone1\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DescribeImages() throws Exception {
        // setup
        String[] expectedResults = { "aws_is_publictrue", "aws_architecturearchitecture", "aws_idkmi-111", "aws_image_typeKERNEL", "aws_owneruserid", "aws_locationmanifest", "aws_kernel_idk-111", "platformlinux", "aws_statePENDING",
                "aws_ramdisk_idr-111", "aws_is_publictrue", "aws_architecturearchitecture", "aws_idkmi-222", " aws_image_typeKERNEL", "aws_owneruserid", "aws_locationmanifest", "aws_kernel_idk-222", "platformlinux", "aws_statePENDING",
                "aws_ramdisk_idr-111" };
        // act
        runCommand(rightscaleBaseCommand + " describe_images \"kmi-111\" \"kmi-222\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DescribeImages20081201() throws Exception {
        // setup
        String[] expectedResults = { "aws_is_publictrue", "aws_architecturearchitecture", "aws_idkmi-111", "aws_image_typeKERNEL", "aws_owneruserid", "aws_locationmanifest", "aws_kernel_idk-111", "platformlinux", "aws_statePENDING",
                "aws_ramdisk_idr-111", "aws_is_publictrue", "aws_architecturearchitecture", "aws_idkmi-222", " aws_image_typeKERNEL", "aws_owneruserid", "aws_locationmanifest", "aws_kernel_idk-222", "platformlinux", "aws_statePENDING",
                "aws_ramdisk_idr-111" };
        // act
        runCommand(rightscaleBaseCommand20081201 + " describe_images \"kmi-111\" \"kmi-222\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DescribeRegionsEmpty() throws Exception {
        // Unsupported by rightscale.
    }

    @Override
    public void testEc2DescribeRegionsSingle() throws Exception {
        // Unsupported by rightscale.
    }

    @Override
    public void testEc2DescribeSecurityGroups() throws Exception {
        // setup
        String[] expectedResults = new String[] { "aws_group_namedefault", "aws_descriptiondescription", " aws_owneradmin", "aws_perms", "owner", "other", "user", "group", "other", "group", "to_port0", "cidr_ips0.0.0.0/0", "protocolTCP",
                "from_port0", "to_port2", "cidr_ips0.0.0.0/0", "protocolUDP", "from_port1" };

        // act
        runCommand(rightscaleBaseCommand + " describe_security_groups");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DescribeSecurityGroups20081201() throws Exception {
        // setup
        String[] expectedResults = new String[] { "aws_group_namedefault", "aws_descriptiondescription", " aws_owneradmin", "aws_perms", "owner", "other", "user", "group", "other", "group", "to_port0", "cidr_ips0.0.0.0/0", "protocolTCP",
                "from_port0", "to_port2", "cidr_ips0.0.0.0/0", "protocolUDP", "from_port1" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " describe_security_groups");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DescribeSnapshotsMultiple() throws Exception {
        // setup
        String[] expectedResults = new String[] { "result:", "aws_volume_idvolumeId", "aws_idsnapshotId", "aws_started_atTue Oct 27 14:20:31 UTC 2009", "aws_progress67.4", "aws_statuspending" };

        // act
        runCommand(rightscaleBaseCommand + " describe_snapshots");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DescribeSnapshotsMultiple20081201() throws Exception {
        // setup
        String[] expectedResults = new String[] { "result:", "aws_volume_idvolumeId", "aws_idsnapshotId", "aws_started_atTue Oct 27 14:20:31 UTC 2009", "aws_progress67.4", "aws_statuspending" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " describe_snapshots");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DescribeSnapshotsSingle() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "aws_volume_idvolumeId", "aws_idsnap-123", "aws_started_atTue Oct 27 14:20:31 UTC 2009", "aws_progress67.4", "aws_statuspending" };

        // act
        runCommand(rightscaleBaseCommand + " describe_snapshots \"snap-123\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DescribeSnapshotsSingle20081201() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "aws_volume_idvolumeId", "aws_idsnap-123", "aws_started_atTue Oct 27 14:20:31 UTC 2009", "aws_progress67.4", "aws_statuspending" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " describe_snapshots \"snap-123\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DescribeVolume() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "aws_created_atTue Jan 30 05:32:43 UTC 2029", "zoneIceCube", "aws_size800", "snapshot_idsnap-123", "aws_idvol-123", "aws_statusin-use" };

        // act
        runCommand(rightscaleBaseCommand + " describe_volumes vol-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DescribeVolume20081201() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "aws_created_atTue Jan 30 05:32:43 UTC 2029", "zoneIceCube", "aws_size800", "snapshot_idsnap-123", "aws_idvol-123", "aws_statusin-use" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " describe_volumes vol-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DetachVolume() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_device", "aws_idvol-123", "aws_attachment_statusdeleted", " aws_attached_atTue Jan 30 05:32:43 UTC 2029", "aws_instance_id" };

        // act
        runCommand(rightscaleBaseCommand + " detach_volume vol-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DetachVolume20081201() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_device", "aws_idvol-123", "aws_attachment_statusdeleted", " aws_attached_atTue Jan 30 05:32:43 UTC 2029", "aws_instance_id" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " detach_volume vol-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DinMultiple() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "aws_availability_zonezone-i-1", "aws_image_idemi-i-1", "aws_product_codes", "dns_namedns-i-1.com", "aws_state_code16", " private_dns_nameprivate.dns-i-1.com", "aws_reasonreason-i-1",
                "aws_instance_typetype-i-1", "aws_ownerowner-i-1", "ami_launch_index1", "aws_launch_time2009-10-29T16:34:23.405Z", "aws_reservation_idr-i-1", "aws_kernel_idkernel-i-1", " ssh_key_namekey-i-1", "aws_platformlinux", "aws_staterunning",
                "aws_groupsg-i-1", "aws_ramdisk_idramdisk-i-1", "aws_instance_idi-1", "aws_availability_zonezone-i-2", "aws_image_idemi-i-2", "aws_product_codes", "dns_namedns-i-2.com", "aws_state_code16", "private_dns_nameprivate.dns-i-2.com",
                "aws_reasonreason-i-2", "aws_instance_typetype-i-2", "aws_ownerowner-i-2", "ami_launch_index2", "aws_launch_time2009-10-29T16:34:23.405Z", "aws_reservation_idr-i-2", "aws_kernel_idkernel-i-2", "ssh_key_namekey-i-2",
                "aws_platformlinux", "aws_staterunning", "aws_groupsg-i-2", "aws_ramdisk_idramdisk-i-2", "aws_instance_idi-2" };

        // act
        runCommand(rightscaleBaseCommand + " describe_instances \"['i-1','i-2']\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DinMultiple20081201() throws Exception {
        // setup
        String[] expectedResults = { "result: ", "aws_availability_zonezone-i-1", "aws_image_idemi-i-1", "aws_product_codes", "dns_namedns-i-1.com", "aws_state_code16", " private_dns_nameprivate.dns-i-1.com", "aws_reasonreason-i-1",
                "aws_instance_typetype-i-1", "aws_ownerowner-i-1", "ami_launch_index1", "aws_launch_time2009-10-29T16:34:23.405Z", "aws_reservation_idr-i-1", "aws_kernel_idkernel-i-1", " ssh_key_namekey-i-1", "aws_platformlinux", "aws_staterunning",
                "aws_groupsg-i-1", "aws_ramdisk_idramdisk-i-1", "aws_instance_idi-1", "aws_availability_zonezone-i-2", "aws_image_idemi-i-2", "aws_product_codes", "dns_namedns-i-2.com", "aws_state_code16", "private_dns_nameprivate.dns-i-2.com",
                "aws_reasonreason-i-2", "aws_instance_typetype-i-2", "aws_ownerowner-i-2", "ami_launch_index2", "aws_launch_time2009-10-29T16:34:23.405Z", "aws_reservation_idr-i-2", "aws_kernel_idkernel-i-2", "ssh_key_namekey-i-2",
                "aws_platformlinux", "aws_staterunning", "aws_groupsg-i-2", "aws_ramdisk_idramdisk-i-2", "aws_instance_idi-2" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " describe_instances \"['i-1','i-2']\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DinCrashed() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_availability_zonezone-i-666", "aws_image_idemi-i-666", "aws_product_codes", "dns_namedns-i-666.com", "aws_state_code24", "private_dns_nameprivate.dns-i-666.com", "aws_reasonreason-i-666",
                "aws_instance_typetype-i-666", "aws_ownerowner-i-666", " ami_launch_index666", "aws_launch_time2009-10-29T16:34:23.405Z", "aws_reservation_idr-i-666", "aws_kernel_idkernel-i-666", "ssh_key_namekey-i-666", "aws_platformlinux",
                "aws_statecrashed", "aws_groupsg-i-666", "aws_ramdisk_idramdisk-i-666", "aws_instance_idi-666" };

        // act
        runCommand(rightscaleBaseCommand + " describe_instances  \"i-666\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DinSingle() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_availability_zonezone-i-123", "aws_image_idemi-i-123", "aws_product_codes", "dns_namedns-i-123.com", "aws_state_code16", "private_dns_nameprivate.dns-i-123.com", "aws_reasonreason-i-123",
                "aws_instance_typetype-i-123", "aws_ownerowner-i-123", " ami_launch_index123", "aws_launch_time2009-10-29T16:34:23.405Z", "aws_reservation_idr-i-123", "aws_kernel_idkernel-i-123", "ssh_key_namekey-i-123", "aws_platformlinux",
                "aws_staterunning", "aws_groupsg-i-123", "aws_ramdisk_idramdisk-i-123", "aws_instance_idi-123" };

        // act
        runCommand(rightscaleBaseCommand + " describe_instances  \"i-123\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2DinSingle20081201() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_availability_zonezone-i-123", "aws_image_idemi-i-123", "aws_product_codes", "dns_namedns-i-123.com", "aws_state_code16", "private_dns_nameprivate.dns-i-123.com", "aws_reasonreason-i-123",
                "aws_instance_typetype-i-123", "aws_ownerowner-i-123", " ami_launch_index123", "aws_launch_time2009-10-29T16:34:23.405Z", "aws_reservation_idr-i-123", "aws_kernel_idkernel-i-123", "ssh_key_namekey-i-123", "aws_platformlinux",
                "aws_staterunning", "aws_groupsg-i-123", "aws_ramdisk_idramdisk-i-123", "aws_instance_idi-123" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " describe_instances  \"i-123\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2DisassociateAddress() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " disassociate_address \"10.249.162.100\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2DisassociateAddress20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " disassociate_address \"10.249.162.100\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2GetConsoleOutput() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_outputDid you really mean to do rm -rf?", "timestampTue Jan 30 05:32:43 UTC 2029", "aws_timestamp2029-01-30T05:32:43.437Z", "aws_instance_idi-123" };

        // act
        runCommand(rightscaleBaseCommand + " get_console_output i-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2GetConsoleOutput20081201() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_outputDid you really mean to do rm -rf?", "timestampTue Jan 30 05:32:43 UTC 2029", "aws_timestamp2029-01-30T05:32:43.437Z", "aws_instance_idi-123" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " get_console_output i-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2KillMultiple() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_shutdown_state_code32", "aws_prev_state_code16", "aws_shutdown_stateshutting-down", "aws_prev_staterunning", "aws_instance_idi-1", "aws_shutdown_state_code32", "aws_prev_state_code16",
                "aws_shutdown_stateshutting-down", "aws_prev_staterunning", "aws_instance_idi-2", "aws_shutdown_state_code32", "aws_prev_state_code16", "aws_shutdown_stateshutting-down", "aws_prev_staterunning", "aws_instance_idi-3" };

        // act
        runCommand(rightscaleBaseCommand + " terminate_instances \"['i-1','i-2','i-3']\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2KillMultiple20081201() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_shutdown_state_code32", "aws_prev_state_code16", "aws_shutdown_stateshutting-down", "aws_prev_staterunning", "aws_instance_idi-1", "aws_shutdown_state_code32", "aws_prev_state_code16",
                "aws_shutdown_stateshutting-down", "aws_prev_staterunning", "aws_instance_idi-2", "aws_shutdown_state_code32", "aws_prev_state_code16", "aws_shutdown_stateshutting-down", "aws_prev_staterunning", "aws_instance_idi-3" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " terminate_instances \"['i-1','i-2','i-3']\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2KillSingle() throws Exception {
        // setup
        String[] expectedResults = { "aws_shutdown_state_code32", "aws_prev_state_code16", "aws_shutdown_stateshutting-down", "aws_prev_staterunning", "aws_instance_idi-1" };

        // act
        runCommand(rightscaleBaseCommand + " terminate_instances i-1");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2KillSingle20081201() throws Exception {
        // setup
        String[] expectedResults = { "aws_shutdown_state_code32", "aws_prev_state_code16", "aws_shutdown_stateshutting-down", "aws_prev_staterunning", "aws_instance_idi-1" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " terminate_instances i-1");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2RebootInstancesMultiple() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " reboot_instances \"['i-123','i-456']\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2RebootInstancesMultiple20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " reboot_instances \"['i-123','i-456']\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2RebootInstancesSingle() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " reboot_instances i-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2RebootInstancesSingle20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " reboot_instances i-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2RegisterImage() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " register_image bucket/funky.img");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: kmi-123");
    }

    @org.junit.Test
    public void testEc2RegisterImage20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " register_image bucket/funky.img");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: kmi-123");
    }

    @Override
    public void testEc2ReleaseAddress() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " release_address 10.249.162.100");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2ReleaseAddress20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " release_address 10.249.162.100");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2RevokeIngress() throws Exception {
        // act
        runCommand(rightscaleBaseCommand + " revoke_security_group_IP_ingress \"default\" \"80\" \"80\" \"tcp\" \"0.0.0.0/0\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @org.junit.Test
    public void testEc2RevokeIngress20081201() throws Exception {
        // act
        runCommand(rightscaleBaseCommand20081201 + " revoke_security_group_IP_ingress \"default\" \"80\" \"80\" \"tcp\" \"0.0.0.0/0\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "result: true");
    }

    @Override
    public void testEc2Run() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_availability_zonezone-123", "aws_image_idemi-123", "aws_product_codes", "dns_namedns-123.com", "aws_state_code0", "private_dns_namedns-123.private.com", "aws_reason", "aws_instance_typelarge-123",
                "aws_ownerowner-123", "ami_launch_index1", "aws_launch_time", "aws_reservation_idr-123", "aws_kernel_idkernel-123", "ssh_key_namekey-123", "aws_platformwindows", "aws_statepending", "aws_groupsdefault-123",
                "aws_ramdisk_idramdisk-123", "aws_instance_idi-123" };

        // act
        runCommand(rightscaleBaseCommand + " run_instances \"emi-123\" \"1\" \"1\" \"['default']\" \"myKey\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2Run20081201() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_availability_zonezone-123", "aws_image_idemi-123", "aws_product_codes", "dns_namedns-123.com", "aws_state_code0", "private_dns_namedns-123.private.com", "aws_reason", "aws_instance_typelarge-123",
                "aws_ownerowner-123", "ami_launch_index1", "aws_launch_time", "aws_reservation_idr-123", "aws_kernel_idkernel-123", "ssh_key_namekey-123", "aws_platformwindows", "aws_statepending", "aws_groupsdefault-123",
                "aws_ramdisk_idramdisk-123", "aws_instance_idi-123" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " run_instances \"emi-123\" \"1\" \"1\" \"['default']\" \"myKey\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2RunBadUser() throws Exception {
        // setup
        String rightscaleBaseCommandWithBadKey = "etc/rightscaleclient.rb " + "xxxxxxxxxxxxxxxxxxxxxx" + " " + Ec2Setup.SECRET_KEY;

        // act
        runCommand(rightscaleBaseCommandWithBadKey + " run_instances \"emi-123\" \"1\" \"1\" \"['default']\" \"myKey\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), "<Message>User not found</Message>");
    }

    @Override
    public void testEc2addvol() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_created_at", "zoneharmo", "aws_size99", "snapshot_id", "aws_idv-123", "aws_statuscreating" };

        // act
        runCommand(rightscaleBaseCommand + " create_volume \"harmo\" \" \" \"99\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2addvol20081201() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_created_at", "zoneharmo", "aws_size99", "snapshot_id", "aws_idv-123", "aws_statuscreating" };

        // act
        runCommand(rightscaleBaseCommand20081201 + " create_volume \"harmo\" \" \" \"99\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @Override
    public void testEc2addvolTooBig() throws Exception {
        // setup
        String expectedResult = "<Message>size must be less than 100</Message>";

        // act
        runCommand(rightscaleBaseCommand20081201 + " create_volume \"harmo\" \" \" \"101\"");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResult);
    }

    @Override
    public void testEc2csnap() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_volume_idv-123", "aws_idsnap-123", "aws_started_atTue Oct 27 14:20:31 UTC 2009", "aws_progress10.0", "aws_statuspending" };

        // act
        runCommand(rightscaleBaseCommand + "   create_snapshot v-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testEc2csnap20081201() throws Exception {
        // setup
        String[] expectedResults = { "result:", "aws_volume_idv-123", "aws_idsnap-123", "aws_started_atTue Oct 27 14:20:31 UTC 2009", "aws_progress10.0", "aws_statuspending" };

        // act
        runCommand(rightscaleBaseCommand20081201 + "   create_snapshot v-123");

        // assert
        assertResponse(commandExecutor.getOutputLines(), expectedResults);
    }

    @org.junit.Test
    public void testDisableUser() throws Exception {
        // setup
        disableTestUser();

        // act

        runCommand(rightscaleBaseCommand + " describe_volumes vol-123");
        assertResponse(commandExecutor.getErrorLines(), "403: Forbidden");
        // cleanup
        enableTestUser();

    }
}
