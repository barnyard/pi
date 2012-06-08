package com.bt.pi.app.networkmanager.addressing.resolution;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AddressDeleteQueueTest {
    private static final int PRIORITY_THRESHOLD = 1;

    private AddressDeleteQueueItem addressDeleteQueueItem;
    private AddressDeleteQueue addressDeleteQueue;
    private ScheduledExecutorService scheduledExecutorService;

    @Before
    public void setup() {
        scheduledExecutorService = mock(ScheduledExecutorService.class);

        addressDeleteQueueItem = setupAddressDeleteQueueItem("test", 0);

        addressDeleteQueue = new AddressDeleteQueue();
        addressDeleteQueue.setPriorityThreshold(PRIORITY_THRESHOLD);
        addressDeleteQueue.setBatchSize(90);
        addressDeleteQueue.setAddressDeleteSchedulerIntervalSeconds(60);
        addressDeleteQueue.setScheduledExecutorService(scheduledExecutorService);
    }

    public AddressDeleteQueueItem setupAddressDeleteQueueItem(final String name, int priority) {
        AddressDeleteQueueItem addressDeleteQueueItem = new AddressDeleteQueueItem(priority) {
            @Override
            public void delete() {
            }

            @Override
            public boolean equals(Object obj) {
                return obj.toString().equals(toString());
            }

            @Override
            public int hashCode() {
                return 0;
            }

            @Override
            public String toString() {
                return name;
            }
        };

        return addressDeleteQueueItem;
    }

    @Test
    public void testPutInsertsNewItemIntoQueue() throws Exception {
        // act
        addressDeleteQueue.add(addressDeleteQueueItem);

        // assert
        assertThat(addressDeleteQueue.getDeleteQueue().contains(addressDeleteQueueItem), is(true));
        assertThat(addressDeleteQueueItem.getPriority(), equalTo(0));
    }

    @Test
    public void testPutSameItemTwiceInsertsOnlyOnceInQueueWithIncreasedPriority() throws Exception {
        // act
        addressDeleteQueue.add(addressDeleteQueueItem);
        addressDeleteQueue.add(addressDeleteQueueItem);

        // assert
        assertThat(addressDeleteQueue.getDeleteQueue().size(), equalTo(1));
        assertThat(addressDeleteQueue.getDeleteQueue().contains(addressDeleteQueueItem), is(true));
        assertThat(addressDeleteQueueItem.getPriority(), equalTo(1));
    }

    @Test
    public void deleteShouldGetRidOfAllHighPriorityItemsInQueue() throws Exception {
        // setup
        AddressDeleteQueueItem item1 = setupAddressDeleteQueueItem("test1", PRIORITY_THRESHOLD + 2);
        AddressDeleteQueueItem item2 = setupAddressDeleteQueueItem("test2", PRIORITY_THRESHOLD + 1);
        AddressDeleteQueueItem item3 = setupAddressDeleteQueueItem("test3", PRIORITY_THRESHOLD);
        AddressDeleteQueueItem item4 = setupAddressDeleteQueueItem("test4", PRIORITY_THRESHOLD - 1);
        AddressDeleteQueueItem item5 = setupAddressDeleteQueueItem("test5", PRIORITY_THRESHOLD - 2);
        addressDeleteQueue.add(item1);
        addressDeleteQueue.add(item2);
        addressDeleteQueue.add(item3);
        addressDeleteQueue.add(item4);
        addressDeleteQueue.add(item5);
        System.err.println(addressDeleteQueue.getDeleteQueue());

        // act
        addressDeleteQueue.deleteAddressItems();

        // assert
        assertThat(addressDeleteQueue.getDeleteQueue().size(), equalTo(3));
        assertThat(addressDeleteQueue.getDeleteQueue().contains(item3), is(true));
        assertThat(addressDeleteQueue.getDeleteQueue().contains(item4), is(true));
        assertThat(addressDeleteQueue.getDeleteQueue().contains(item5), is(true));
    }

    @Test
    public void deleteShouldGetRidOfAllHighPriorityItemsInQueue2() throws Exception {
        // setup
        AddressDeleteQueueItem item1 = setupAddressDeleteQueueItem("test1", PRIORITY_THRESHOLD + 1);
        AddressDeleteQueueItem item2 = setupAddressDeleteQueueItem("test2", PRIORITY_THRESHOLD + 1);
        AddressDeleteQueueItem item3 = setupAddressDeleteQueueItem("test3", PRIORITY_THRESHOLD);
        AddressDeleteQueueItem item4 = setupAddressDeleteQueueItem("test4", PRIORITY_THRESHOLD - 1);
        AddressDeleteQueueItem item5 = setupAddressDeleteQueueItem("test5", PRIORITY_THRESHOLD - 1);
        addressDeleteQueue.add(item1);
        addressDeleteQueue.add(item2);
        addressDeleteQueue.add(item3);
        addressDeleteQueue.add(item4);
        addressDeleteQueue.add(item5);

        // act
        addressDeleteQueue.deleteAddressItems();

        // assert
        assertThat(addressDeleteQueue.getDeleteQueue().size(), equalTo(3));
        assertThat(addressDeleteQueue.getDeleteQueue().contains(item3), is(true));
        assertThat(addressDeleteQueue.getDeleteQueue().contains(item4), is(true));
        assertThat(addressDeleteQueue.getDeleteQueue().contains(item5), is(true));
    }

    @Ignore
    @Test
    public void multiThreadedTest() throws InterruptedException {
        final CopyOnWriteArrayList<String> names = new CopyOnWriteArrayList<String>();
        names.add("Nauman");
        names.add("Josh");
        names.add("Juan");
        names.add("Uros");
        names.add("Rags");
        names.add("Nauman");

        final AtomicInteger cycles = new AtomicInteger(600);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < cycles.get(); i++) {
                    addressDeleteQueue.add(setupAddressDeleteQueueItem(names.get(i % names.size()), PRIORITY_THRESHOLD - 1));
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        Thread adder = new Thread(r);
        Thread adder2 = new Thread(r);

        Thread deleter = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < cycles.get() / 10; i++) {
                    // System.err.println("deleting");
                    addressDeleteQueue.deleteAddressItems();
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        // act
        // run adder and deleter so that things get nuts!
        adder.start();
        adder2.start();
        deleter.start();
        adder.join();
        adder2.join();
        deleter.join();
        // delete one more time
        addressDeleteQueue.deleteAddressItems();

        assertEquals(0, addressDeleteQueue.getDeleteQueue().size());

    }

    @Ignore
    @Test
    public void moreSubstantialMultiThreadedTest() throws Throwable {
        for (int i = 0; i < 100; i++) {
            multiThreadedTest();
        }

    }

    @Test
    public void shouldStartSchedulerOnPostConstruct() throws SecurityException, NoSuchMethodException {
        // setup
        Method method = AddressDeleteQueue.class.getMethod("startAddressDeleteScheduledTask");

        // act
        addressDeleteQueue.startAddressDeleteScheduledTask();

        // assert
        assertTrue(method.getAnnotations()[0] instanceof javax.annotation.PostConstruct);
        verify(scheduledExecutorService).scheduleWithFixedDelay(isA(Runnable.class), eq(0L), eq(60L), eq(TimeUnit.SECONDS));
    }

    @Test
    public void shouldDeleteAllAddressDeleteQueueItemsOnShuttingDown() {
        // setup
        AddressDeleteQueueItem item1 = setupAddressDeleteQueueItem("test1", PRIORITY_THRESHOLD - 1);
        AddressDeleteQueueItem item2 = setupAddressDeleteQueueItem("test2", PRIORITY_THRESHOLD - 2);
        AddressDeleteQueueItem item3 = setupAddressDeleteQueueItem("test3", PRIORITY_THRESHOLD);
        AddressDeleteQueueItem item4 = setupAddressDeleteQueueItem("test4", PRIORITY_THRESHOLD - 1);
        AddressDeleteQueueItem item5 = setupAddressDeleteQueueItem("test5", PRIORITY_THRESHOLD - 1);
        addressDeleteQueue.add(item1);
        addressDeleteQueue.add(item2);
        addressDeleteQueue.add(item3);
        addressDeleteQueue.add(item4);
        addressDeleteQueue.add(item5);

        // act
        addressDeleteQueue.removeAllAddressesInQueueOnShuttingDown();

        // assert
        assertThat(addressDeleteQueue.getDeleteQueue().size(), equalTo(0));
    }

    @Test
    public void shouldDeleteAllItemsInOneGoEvenIfBatchSizeIsSmallerThanNumberOfItemsInQueue() {
        // setup
        addressDeleteQueue.setBatchSize(2);

        AddressDeleteQueueItem item1 = setupAddressDeleteQueueItem("test1", PRIORITY_THRESHOLD - 1);
        AddressDeleteQueueItem item2 = setupAddressDeleteQueueItem("test2", PRIORITY_THRESHOLD - 2);
        AddressDeleteQueueItem item3 = setupAddressDeleteQueueItem("test3", PRIORITY_THRESHOLD);
        AddressDeleteQueueItem item4 = setupAddressDeleteQueueItem("test4", PRIORITY_THRESHOLD - 1);
        AddressDeleteQueueItem item5 = setupAddressDeleteQueueItem("test5", PRIORITY_THRESHOLD - 1);
        addressDeleteQueue.add(item1);
        addressDeleteQueue.add(item2);
        addressDeleteQueue.add(item3);
        addressDeleteQueue.add(item4);
        addressDeleteQueue.add(item5);

        // act
        addressDeleteQueue.removeAllAddressesInQueueOnShuttingDown();

        // assert
        assertThat(addressDeleteQueue.getDeleteQueue().size(), equalTo(0));
    }
}
