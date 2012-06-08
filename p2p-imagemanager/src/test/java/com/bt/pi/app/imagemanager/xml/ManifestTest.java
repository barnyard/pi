package com.bt.pi.app.imagemanager.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class ManifestTest {
    private static final String ENCRYPTED_KEY = "encryptedKey";
    private static final String ENCRYPTED_I_V = "encryptedIV";
    private static final String SIGNATURE = "signature";
    private static final String MACHINE_CONFIGURATION = "machineConfiguration";
    private static final String IMAGE = "image";
    private static final String ARCH = "windows";
    private static final String KERNEL_ID = "k123";
    private static final String RAMDISK_ID = "r123";

    private List<String> partFilenames;
    private Manifest manifest;

    @Before
    public void setUp() {
        partFilenames = new ArrayList<String>();
        partFilenames.add("filename1");
        partFilenames.add("filename2");
        partFilenames.add("filename3");

        manifest = buildManifest(ENCRYPTED_KEY, ENCRYPTED_I_V, SIGNATURE, MACHINE_CONFIGURATION, IMAGE, partFilenames, ARCH, KERNEL_ID, RAMDISK_ID);
    }

    @Test
    public void shouldConstructAManifestWithGivenParameters() {
        assertEquals(ENCRYPTED_KEY, manifest.getEncryptedKey());
        assertEquals(ENCRYPTED_I_V, manifest.getEncryptedIV());
        assertEquals(SIGNATURE, manifest.getSignature());
        assertEquals(MACHINE_CONFIGURATION, manifest.getMachineConfiguration());
        assertEquals(IMAGE, manifest.getImage());
        assertEquals(partFilenames, manifest.getPartFilenames());
        assertEquals(ARCH, manifest.getArch());
        assertEquals(KERNEL_ID, manifest.getKernelId());
        assertEquals(RAMDISK_ID, manifest.getRamdiskId());
    }

    @Test
    public void shouldHonourEqualsAndHashCodeContracts() {
        Manifest equalManifest1 = buildManifest(ENCRYPTED_KEY, ENCRYPTED_I_V, SIGNATURE, MACHINE_CONFIGURATION, IMAGE, partFilenames, ARCH, KERNEL_ID, RAMDISK_ID);
        Manifest equalManifest2 = buildManifest(ENCRYPTED_KEY, ENCRYPTED_I_V, SIGNATURE, MACHINE_CONFIGURATION, IMAGE, partFilenames, ARCH, KERNEL_ID, RAMDISK_ID);
        Manifest unequalManifest = buildManifest("", ENCRYPTED_I_V, SIGNATURE, MACHINE_CONFIGURATION, IMAGE, partFilenames, ARCH, KERNEL_ID, RAMDISK_ID);

        assertFalse("equality failed", manifest.equals(unequalManifest));
        assertTrue("reflexivity failed", manifest.equals(manifest));
        assertTrue("symetricity failed", manifest.equals(equalManifest1) && equalManifest1.equals(manifest));
        assertTrue("transitivity failed", manifest.equals(equalManifest1) && equalManifest1.equals(equalManifest2) && manifest.equals(equalManifest2));
        assertTrue("consistency failed", manifest.equals(manifest) && manifest.equals(manifest));
        assertFalse("Object types should be different failed", manifest.equals(new Object()));

        assertTrue("equal objects should have same hashcode", manifest.hashCode() == equalManifest1.hashCode() && manifest.hashCode() == equalManifest2.hashCode());
    }

    @Test
    public void shouldReturnAValidStringRepresentationOfTheManifest() {
        assertTrue(manifest.toString().contains(ENCRYPTED_KEY));
        assertTrue(manifest.toString().contains(ENCRYPTED_I_V));
        assertTrue(manifest.toString().contains(SIGNATURE));
        assertTrue(manifest.toString().contains(MACHINE_CONFIGURATION));
        assertTrue(manifest.toString().contains(IMAGE));
        assertTrue(manifest.toString().contains(partFilenames.toString()));
    }

    private Manifest buildManifest(String encryptedKey, String encryptedIV, String signature, String machineConfiguration, String image, List<String> thePartFilenames, String arch2, String kernelId, String ramdiskId) {
        return new Manifest(encryptedKey, encryptedIV, signature, machineConfiguration, image, thePartFilenames, arch2, kernelId, ramdiskId);
    }
}
