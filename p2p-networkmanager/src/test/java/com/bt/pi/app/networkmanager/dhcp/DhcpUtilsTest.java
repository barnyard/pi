package com.bt.pi.app.networkmanager.dhcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

public class DhcpUtilsTest {
    private static final String DHCP_CONFIG_STRING = "some-dhcp-config-string";
    private String netRuntimePath = System.getProperty("java.io.tmpdir");
    private File leasesFile;
    private File leasesFileCopy;
    private Field dhcpRefreshField;

    private DhcpUtils dhcpUtils;

    @Before
    public void setup() {
        leasesFile = new File(netRuntimePath + "/pi-dhcp.leases");
        leasesFile.delete();

        leasesFileCopy = new File(netRuntimePath + "/pi-dhcp.leases-2");
        leasesFileCopy.delete();

        dhcpUtils = new DhcpUtils();
        dhcpRefreshField = ReflectionUtils.findField(DhcpUtils.class, "dhcpRefreshQueue");
        ReflectionUtils.makeAccessible(dhcpRefreshField);
    }

    @After
    public void tearDown() {
        leasesFile = new File(netRuntimePath + "/pi-dhcp.leases");
        leasesFile.delete();

        leasesFileCopy = new File(netRuntimePath + "/pi-dhcp.leases-2");
        leasesFileCopy.delete();

        new File(netRuntimePath + "/pi-dhcp.conf").delete();
    }

    @Test
    public void shouldWriteGeneratedConfigToFileAndCloseOnExit() throws Exception {
        // act
        dhcpUtils.writeConfigToFile(netRuntimePath + "/pi-dhcp.conf", DHCP_CONFIG_STRING);

        // assert
        assertEquals(DHCP_CONFIG_STRING, FileUtils.readFileToString(new File(netRuntimePath + "/pi-dhcp.conf")));
    }

    @Test
    public void touchFileShouldCreateAFile() {
        // act
        dhcpUtils.touchFile(netRuntimePath + "/pi-dhcp.leases");

        // assert
        assertTrue(new File(netRuntimePath + "/pi-dhcp.leases").exists());
    }

    @Test
    public void secondTouchFileShouldLeaveExistingFile() {
        // act
        dhcpUtils.touchFile(leasesFile.getAbsolutePath());
        dhcpUtils.touchFile(leasesFile.getAbsolutePath());

        // assert
        assertTrue(leasesFile.exists());
    }

    @Test
    public void testFileExists() throws Exception {
        // setup
        dhcpUtils.touchFile(leasesFile.getAbsolutePath());

        // act
        File result = dhcpUtils.getFileIfExists(leasesFile.getAbsolutePath());

        // assert
        assertTrue(result.exists());
    }

    @Test
    public void testFileDoesNotExist() throws Exception {
        // act
        File result = dhcpUtils.getFileIfExists(leasesFile.getAbsolutePath());

        // assert
        assertNull(result);
    }

    @Test
    public void testDeleteFileIfExists() throws Exception {
        // setup
        dhcpUtils.touchFile(leasesFile.getAbsolutePath());

        // act
        dhcpUtils.deleteDhcpFileIfExists(leasesFile.getAbsolutePath());

        // assert
        assertFalse(leasesFile.exists());
    }

    @Test
    public void testFileCopy() throws Exception {
        // setup
        FileUtils.writeStringToFile(leasesFile, "copy this");

        // act
        dhcpUtils.copyFile(leasesFile.getAbsolutePath(), leasesFileCopy.getAbsolutePath());

        // assert
        assertEquals("copy this", FileUtils.readFileToString(leasesFileCopy));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddDhcpRefreshToken() throws Exception {
        // setup
        DhcpRefreshToken dhcpRefreshToken = new DhcpRefreshToken("", "", null);

        // act
        dhcpUtils.addToDhcpRefreshQueue(dhcpRefreshToken);

        // assert
        assertTrue(((LinkedBlockingQueue<DhcpRefreshToken>) ReflectionUtils.getField(dhcpRefreshField, dhcpUtils)).contains(dhcpRefreshToken));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPollDhcpRefreshToken() throws Exception {
        // setup
        DhcpRefreshToken dhcpRefreshToken = new DhcpRefreshToken("", "", null);
        ((LinkedBlockingQueue<DhcpRefreshToken>) ReflectionUtils.getField(dhcpRefreshField, dhcpUtils)).add(dhcpRefreshToken);

        // act
        DhcpRefreshToken result = dhcpUtils.pollDhcpRefreshQueue();

        // assert
        assertEquals(result, dhcpRefreshToken);
    }
}
