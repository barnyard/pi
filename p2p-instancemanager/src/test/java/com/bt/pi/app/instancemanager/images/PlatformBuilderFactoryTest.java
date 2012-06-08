package com.bt.pi.app.instancemanager.images;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.images.platform.ImagePlatform;

public class PlatformBuilderFactoryTest {

    private PlatformBuilderFactory platformBuilderFactory;

    @Before
    public void setUp() throws Exception {
        platformBuilderFactory = new PlatformBuilderFactory();
        platformBuilderFactory.setLinuxPlatformBuilder(new LinuxPlatformBuilder());
        platformBuilderFactory.setWindowsPlatformBuilder(new WindowsPlatformBuilder());
        platformBuilderFactory.setSolarisPlatformBuilder(new SolarisPlatformBuilder());
    }

    @Test
    public void shouldReturnLinuxPlatformBuilderIfPlatformIsLinux() {

        // act
        PlatformBuilder platformBuilder = platformBuilderFactory.getFor(ImagePlatform.linux);

        // assert
        assertTrue(platformBuilder instanceof LinuxPlatformBuilder);
    }

    @Test
    public void shouldReturnLinuxPlatformBuilderIfPlatformIsNull() {
        // act
        PlatformBuilder platformBuilder = platformBuilderFactory.getFor(null);

        // assert
        assertTrue(platformBuilder instanceof LinuxPlatformBuilder);
    }

    @Test
    public void shouldReturnWindowsPlatformBuilderIfPlatformIsWindows() {
        // act
        PlatformBuilder platformBuilder = platformBuilderFactory.getFor(ImagePlatform.windows);

        // assert
        assertTrue(platformBuilder instanceof WindowsPlatformBuilder);
    }

    @Test
    public void shouldReturnSolarisPlatformBuilderIfPlatformIsOpenSolaris() {
        // act
        PlatformBuilder platformBuilder = platformBuilderFactory.getFor(ImagePlatform.opensolaris);

        // assert
        assertTrue(platformBuilder instanceof SolarisPlatformBuilder);
    }
}
