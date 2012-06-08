package com.bt.pi.app.instancemanager.images;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import com.bt.pi.app.common.entities.Instance;

public class PlatformBuilderTest {
    private PlatformBuilder platformBuilder;

    @Before
    public void setup() {
        platformBuilder = new PlatformBuilder() {
            @Override
            public void build(Instance instance, String key) {
            }
        };
    }

    @Test
    public void testSetterAndGetter() throws Exception {
        // setup
        InstanceImageManager instanceImageManager = mock(InstanceImageManager.class);

        // act
        platformBuilder.setInstanceImageManager(instanceImageManager);

        // assert
        assertThat(platformBuilder.getInstanceImageManager(), equalTo(instanceImageManager));
    }
}
