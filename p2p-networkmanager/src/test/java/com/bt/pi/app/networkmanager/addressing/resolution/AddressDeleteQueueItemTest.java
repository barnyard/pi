package com.bt.pi.app.networkmanager.addressing.resolution;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class AddressDeleteQueueItemTest {
    private AddressDeleteQueueItem addressDeleteQueueItem;

    @Before
    public void setup() {
        addressDeleteQueueItem = new AddressDeleteQueueItem() {
            @Override
            public void delete() {
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return "test";
            }
        };
    }

    @Test
    public void testLowerPriority() throws Exception {
        // setup
        AddressDeleteQueueItem newItem = new AddressDeleteQueueItem() {
            @Override
            public void delete() {
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return "new";
            }
        };
        newItem.incrementPriority();

        // act
        int result = addressDeleteQueueItem.compareTo(newItem);

        // assert
        assertThat(result, equalTo(1));
    }

    @Test
    public void testEqualPriorityReturns1ToAvoidMissingElementsInSet() throws Exception {
        // setup
        AddressDeleteQueueItem newItem = new AddressDeleteQueueItem() {
            @Override
            public void delete() {
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return "new";
            }
        };

        // act
        int result = addressDeleteQueueItem.compareTo(newItem);

        // assert
        assertThat(result, equalTo(1));
    }

    @Test
    public void testEqualPriorityReturns0IfElementsAreEqual() throws Exception {
        // setup
        AddressDeleteQueueItem newItem = new AddressDeleteQueueItem() {
            @Override
            public void delete() {
            }

            @Override
            public boolean equals(Object obj) {
                return true;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return "new";
            }
        };

        // act
        int result = addressDeleteQueueItem.compareTo(newItem);

        // assert
        assertThat(result, equalTo(0));
    }

    @Test
    public void testHigherPriority() throws Exception {
        // setup
        AddressDeleteQueueItem newItem = new AddressDeleteQueueItem() {
            @Override
            public void delete() {
            }

            @Override
            public boolean equals(Object obj) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return "new";
            }
        };
        addressDeleteQueueItem.incrementPriority();

        // act
        int result = addressDeleteQueueItem.compareTo(newItem);

        // assert
        assertThat(result, equalTo(-1));
    }
}
