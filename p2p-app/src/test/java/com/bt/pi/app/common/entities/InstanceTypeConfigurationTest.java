package com.bt.pi.app.common.entities;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.InstanceTypeConfiguration;

public class InstanceTypeConfigurationTest {
    private InstanceTypeConfiguration instanceTypeConfiguration;
    private String vmTypeName;
    private int numCores;
    private int memorySizeInMB;
    private int diskSizeInGB;

    @Before
    public void setup() {
        vmTypeName = "name";
        numCores = 1;
        memorySizeInMB = 2;
        diskSizeInGB = 3;
    }

    @Test
    public void testDefaultConstructor() throws Exception {
        // act
        instanceTypeConfiguration = new InstanceTypeConfiguration();

        // assert
        assertNull(instanceTypeConfiguration.getInstanceType());
        assertThat(instanceTypeConfiguration.getNumCores(), equalTo(0));
        assertThat(instanceTypeConfiguration.getMemorySizeInMB(), equalTo(0));
        assertThat(instanceTypeConfiguration.getDiskSizeInGB(), equalTo(0));
    }

    @Test
    public void testConstructor() throws Exception {
        // act
        instanceTypeConfiguration = new InstanceTypeConfiguration(vmTypeName, numCores, memorySizeInMB, diskSizeInGB);

        // assert
        assertThat(instanceTypeConfiguration.getInstanceType(), equalTo(vmTypeName));
        assertThat(instanceTypeConfiguration.getNumCores(), equalTo(numCores));
        assertThat(instanceTypeConfiguration.getMemorySizeInMB(), equalTo(memorySizeInMB));
        assertThat(instanceTypeConfiguration.getDiskSizeInGB(), equalTo(diskSizeInGB));
    }

    @Test
    public void testToString() {
        // setup
        instanceTypeConfiguration = new InstanceTypeConfiguration(vmTypeName, numCores, memorySizeInMB, diskSizeInGB);

        // act
        String result = instanceTypeConfiguration.toString();

        // assert
        assertTrue(result.contains(vmTypeName));
        assertTrue(result.contains(numCores + ""));
        assertTrue(result.contains(memorySizeInMB + ""));
        assertTrue(result.contains(diskSizeInGB + ""));

    }
}
