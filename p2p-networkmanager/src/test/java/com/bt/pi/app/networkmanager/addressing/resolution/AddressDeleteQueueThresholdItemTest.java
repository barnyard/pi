package com.bt.pi.app.networkmanager.addressing.resolution;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

public class AddressDeleteQueueThresholdItemTest {
    private AddressDeleteQueueThresholdItem addressDeleteQueueThresholdItem;

    @Before
    public void setup() {
        addressDeleteQueueThresholdItem = new AddressDeleteQueueThresholdItem(2);
    }

    @Test
    public void testEqualsReturnsTrueIfSame() throws Exception {
        // assert
        assertThat(addressDeleteQueueThresholdItem, equalTo(addressDeleteQueueThresholdItem));
    }

    @Test
    public void testThatEqualsReturnsFalseForDifferentElement() throws Exception {
        // setup
        AddressDeleteQueueItem addressDeleteQueueItem = mock(AddressDeleteQueueItem.class);

        // assert
        assertFalse(addressDeleteQueueThresholdItem.equals(addressDeleteQueueItem));
    }

    @Test
    public void testHashcodeAndToString() throws Exception {
        // assert
        assertThat(addressDeleteQueueThresholdItem.hashCode(), equalTo(0));
        assertThat(addressDeleteQueueThresholdItem.toString(), equalTo("threshold queue item, priority: 2"));
    }
}
