package com.bt.pi.app.instancemanager.reporting;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class InstanceReportEntityCollectionTest {
    @Test
    public void getType() throws Exception {
        // setup
        InstanceReportEntityCollection instanceReportEntityCollection = new InstanceReportEntityCollection();

        // act
        String type = instanceReportEntityCollection.getType();

        // assert
        assertThat(type, equalTo(InstanceReportEntityCollection.class.getSimpleName()));
    }
}
