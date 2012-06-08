package com.bt.pi.app.instancemanager.reporting;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ZombieInstanceReportEntityCollectionTest {
    @Test
    public void getType() throws Exception {
        // setup
        ZombieInstanceReportEntityCollection instanceReportEntityCollection = new ZombieInstanceReportEntityCollection();

        // act
        String type = instanceReportEntityCollection.getType();

        // assert
        assertThat(type, equalTo(ZombieInstanceReportEntityCollection.class.getSimpleName()));
    }
}
