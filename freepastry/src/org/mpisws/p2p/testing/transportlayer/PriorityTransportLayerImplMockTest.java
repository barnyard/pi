package org.mpisws.p2p.testing.transportlayer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.mpisws.p2p.transport.MessageCallback;
import org.mpisws.p2p.transport.MessageRequestHandle;
import org.mpisws.p2p.transport.P2PSocket;
import org.mpisws.p2p.transport.P2PSocketReceiver;
import org.mpisws.p2p.transport.SocketCallback;
import org.mpisws.p2p.transport.SocketRequestHandle;
import org.mpisws.p2p.transport.liveness.LivenessTransportLayerImpl;
import org.mpisws.p2p.transport.liveness.LivenessTypes;
import org.mpisws.p2p.transport.multiaddress.MultiInetSocketAddress;
import org.mpisws.p2p.transport.priority.PriorityTransportLayerImpl;
import org.mpisws.p2p.transport.priority.PriorityTransportLayerImpl.EntityManager;
import org.mpisws.p2p.transport.proximity.ProximityProvider;
import org.springframework.util.ReflectionUtils;

import rice.environment.Environment;
import rice.environment.logging.LogManager;
import rice.environment.logging.Logger;
import rice.selector.SelectorManager;
import rice.selector.TimerTask;

@RunWith(MockitoJUnitRunner.class)
public class PriorityTransportLayerImplMockTest {
    @Rule
    public TestName testName = new TestName();

    private static final int MAX_QUEUE_SIZE = 4;
	private static final int MAX_SMALL_MSG_SIZE = 1024;
	@Mock
	private LivenessTransportLayerImpl livenessTransportLayerImpl;
	@Mock
	private ProximityProvider proximityProvider;
	@Mock
	private Environment environment;
	@Mock
	private LogManager logManager;
	@Mock
	private SelectorManager selectorManager;
	@Mock
	private MultiInetSocketAddress destination;
	@Mock
	private SocketRequestHandle socketRequestHandle;
	@Mock
	private P2PSocket socket;
	private PriorityTransportLayerImpl<MultiInetSocketAddress> priorityTransportLayer;
	private Logger logger;
	private int noofMessages = 3;
	private CountDownLatch latch = new CountDownLatch(noofMessages);
	private MessageCallback<MultiInetSocketAddress, ByteBuffer> deliverAckToMe;
	private boolean failDeliverAcktoMe = false;
	private static final int SMALL_MESSAGE_SIZE = 200;
	private static final int BIG_MESSAGE_SIZE = 20000;
	private boolean socketClosed = false;

	@SuppressWarnings("unchecked")
	@Before
	public void before() throws IOException {
		System.err.println("*********************");
		System.err.println(testName.getMethodName());
		System.err.println("*********************");
		logger = new Logger() {
			@Override
			public void log(String message) {
				System.out
						.println(System.currentTimeMillis() + " : " + message);
			}

			@Override
			public void logException(String message, Throwable exception) {
				System.out.println(System.currentTimeMillis() + " : " + message
						+ " " + exception);
			}
		};
		when(logManager.getLogger(PriorityTransportLayerImpl.class, null))
				.thenReturn(logger);
		when(environment.getLogManager()).thenReturn(logManager);
		when(environment.getSelectorManager()).thenReturn(selectorManager);
		priorityTransportLayer = new PriorityTransportLayerImpl<MultiInetSocketAddress>(
				livenessTransportLayerImpl, livenessTransportLayerImpl,
				proximityProvider, environment, MAX_SMALL_MSG_SIZE,
				MAX_QUEUE_SIZE, null);

		deliverAckToMe = new MessageCallback<MultiInetSocketAddress, ByteBuffer>() {
			@Override
			public void ack(
					MessageRequestHandle<MultiInetSocketAddress, ByteBuffer> msg) {
				System.err.println("ack");
				latch.countDown();
				if (failDeliverAcktoMe)
					throw new RuntimeException(
							"unit test fail deliver ack to me");
			}

			@Override
			public void sendFailed(
					MessageRequestHandle<MultiInetSocketAddress, ByteBuffer> msg,
					Exception reason) {
				System.err.println("sendFailed: " + reason);
			}
		};

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				if (socketClosed)
					throw new SocketException("socket is closed");
				Thread.sleep(500);
				ByteBuffer byteBuffer = (ByteBuffer) invocation.getArguments()[0];
				int size = byteBuffer.remaining();
				System.err.println("size: " + size);
				while (byteBuffer.remaining() > 0) {
					byteBuffer.get();
				}
				return size;
			}
		}).when(socket).write(isA(ByteBuffer.class));

		when(socket.getIdentifier()).thenReturn(destination);

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				final P2PSocketReceiver p2pSocketReceiver = (P2PSocketReceiver) invocation
						.getArguments()[2];
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							p2pSocketReceiver.receiveSelectResult(socket,
									false, true);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
				t.start();
				return null;
			}
		}).when(socket).register(eq(false), eq(true),
				isA(P2PSocketReceiver.class));

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation)
					throws Throwable {
				SocketCallback socketCallback = (SocketCallback) invocation
						.getArguments()[1];
				socketClosed = false;
				socketCallback.receiveResult(socketRequestHandle, socket);
				return null;
			}
		}).when(livenessTransportLayerImpl).openSocket(any(),
				(SocketCallback) any(), (Map) any());

		when(selectorManager.isSelectorThread()).thenReturn(true);
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Runnable r = (Runnable) invocation.getArguments()[0];
				Thread t = new Thread(r);
				t.start();
				return null;
			}
		}).when(selectorManager).invoke(isA(Runnable.class));

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				final TimerTask tt = (TimerTask) invocation.getArguments()[0];
				final long delay = (Long) invocation.getArguments()[1];
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							System.err.println("sleeping for " + delay);
							Thread.sleep(delay);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						System.err.println("RUNNING TIMERTASK");
						tt.run();
					}
				});
				t.start();
				return null;
			}
		}).when(selectorManager).schedule(isA(TimerTask.class), anyLong());

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				System.err.println("SOCKET IS CLOSED");
				socketClosed = true;
				return null;
			}
		}).when(socket).close();
	}
	
	@Test
	public void testBigMessageOK() throws InterruptedException {
		// setup

		// act
		sendMessages(BIG_MESSAGE_SIZE);

		// assert
		assertTrue(latch.await(6, TimeUnit.SECONDS));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBigMessageOpenSocketFails() throws InterruptedException {
		// setup
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation)
					throws Throwable {
				SocketCallback socketCallback = (SocketCallback) invocation
						.getArguments()[1];
				socketCallback.receiveException(socketRequestHandle,
						new RuntimeException("unit test fail simulation"));
				return null;
			}
		}).when(livenessTransportLayerImpl).openSocket(any(),
				(SocketCallback) any(), (Map) any());

		// act
		sendMessages(BIG_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		assertPendingBigMessagesClear(destination);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBigMessageOpenSocketDoesNotCallback()
			throws InterruptedException {
		// setup
		final AtomicInteger counter = new AtomicInteger();
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation)
					throws Throwable {
				counter.incrementAndGet();
				if (counter.get() != 1) {
					SocketCallback socketCallback = (SocketCallback) invocation
							.getArguments()[1];
					socketClosed = false;
					socketCallback.receiveResult(socketRequestHandle, socket);
				}
				return null;
			}
		}).when(livenessTransportLayerImpl).openSocket(any(),
				(SocketCallback) any(), (Map) any());

		// act
		sendMessages(BIG_MESSAGE_SIZE);

		// assert
		assertTrue(latch.await(12, TimeUnit.SECONDS));
		assertEquals(2, counter.get());
		Thread.sleep(100);
		assertPendingBigMessagesClear(destination);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testBigMessageOpenSocketDoesNotCallbackAndThenCallsBackWithException()
			throws InterruptedException {
		// setup
		final AtomicInteger counter = new AtomicInteger();
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation)
					throws Throwable {
				counter.incrementAndGet();
				if (counter.get() != 1) {
					SocketCallback socketCallback = (SocketCallback) invocation
							.getArguments()[1];
					socketCallback.receiveException(socketRequestHandle,
							new RuntimeException("unit test fail simulation"));
				}
				return null;
			}
		}).when(livenessTransportLayerImpl).openSocket(any(),
				(SocketCallback) any(), (Map) any());

		// act
		sendMessages(BIG_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(12, TimeUnit.SECONDS));
		assertEquals(2, counter.get());
		assertPendingBigMessagesClear(destination);
	}

	@Test
	public void testBigMessageWriteSocketFailsOnSecondMessage()
			throws Exception {
		// setup
		final AtomicInteger socketMessageCount = new AtomicInteger();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(500);
				ByteBuffer byteBuffer = (ByteBuffer) invocation.getArguments()[0];
				int size = byteBuffer.remaining();
				System.err.println("size: " + size);
				int count = -1;
				if (size > 10) {
					count = socketMessageCount.incrementAndGet();
				}
				while (byteBuffer.remaining() > 0) {
					byteBuffer.get();
				}
				if (count == 2)
					return -1;
				return size;
			}
		}).when(socket).write(isA(ByteBuffer.class));

		// act
		sendMessages(BIG_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		assertEquals(2, latch.getCount());
		assertPendingBigMessagesClear(destination);
	}

	@Test
	public void testBigMessageWriteSocketThrowsIOExceptionOnSecondMessage()
			throws Exception {
		// setup
		final AtomicInteger socketMessageCount = new AtomicInteger();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(500);
				ByteBuffer byteBuffer = (ByteBuffer) invocation.getArguments()[0];
				int size = byteBuffer.remaining();
				System.err.println("size: " + size);
				int count = -1;
				if (size > 10) {
					count = socketMessageCount.incrementAndGet();
				}
				if (count == 2)
					throw new IOException("unit test simulated error");
				while (byteBuffer.remaining() > 0) {
					byteBuffer.get();
				}
				return size;
			}
		}).when(socket).write(isA(ByteBuffer.class));

		// act
		sendMessages(BIG_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		assertEquals(2, latch.getCount());
		assertPendingBigMessagesClear(destination);
	}

	@Test
	public void testBigMessageWriteSocketThrowsIOExceptionOnFirstMessage()
			throws Exception {
		// setup
		final AtomicInteger socketMessageCount = new AtomicInteger();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(500);
				ByteBuffer byteBuffer = (ByteBuffer) invocation.getArguments()[0];
				int size = byteBuffer.remaining();
				System.err.println("size: " + size);
				int count = -1;
				if (size > 10) {
					count = socketMessageCount.incrementAndGet();
				}
				if (count == 1)
					throw new IOException("unit test simulated error");
				while (byteBuffer.remaining() > 0) {
					byteBuffer.get();
				}
				return size;
			}
		}).when(socket).write(isA(ByteBuffer.class));

		// act
		sendMessages(BIG_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		assertEquals(3, latch.getCount());
		assertPendingBigMessagesClear(destination);
	}

	@Test
	public void testBigMessagesShouldContinueWhenDeliverAckToMeFails()
			throws InterruptedException {
		// setup
		failDeliverAcktoMe = true;

		// act
		sendMessages(BIG_MESSAGE_SIZE);

		// assert
		assertTrue(latch.await(6, TimeUnit.SECONDS));
	}

	@Test
	public void testSmallMessageOK() throws InterruptedException {
		// setup

		// act
		sendMessages(SMALL_MESSAGE_SIZE);

		// assert
		assertTrue(latch.await(6, TimeUnit.SECONDS));
	}

	@Ignore("work in progress")
	@SuppressWarnings("unchecked")
	@Test
	public void testSmallMessageReadSocketReturnsNegative() throws Exception {
		// setup
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				final P2PSocketReceiver receiver = (P2PSocketReceiver) invocation
						.getArguments()[2];
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						try {
							Thread.sleep(500);
							receiver.receiveSelectResult(socket, true, false);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
				t.start();
				return null;
			}
		}).when(socket).register(eq(true), eq(false),
				isA(P2PSocketReceiver.class));

		when(socket.read(isA(ByteBuffer.class))).thenReturn(-1L);

		// act
		sendMessages(SMALL_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		Thread.sleep(2000);
		priorityTransportLayer.printMemStats(0);

		latch = new CountDownLatch(noofMessages);
		testSmallMessageOK();
	}

	@Test
	public void testSmallMessageWriteSocketFailsOnSecondMessage()
			throws Exception {
		// setup
		final AtomicInteger socketMessageCount = new AtomicInteger();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(500);
				ByteBuffer byteBuffer = (ByteBuffer) invocation.getArguments()[0];
				int size = byteBuffer.remaining();
				System.err.println("size: " + size);
				int count = -1;
				if (size > 10) {
					count = socketMessageCount.incrementAndGet();
				}
				while (byteBuffer.remaining() > 0) {
					byteBuffer.get();
				}
				if (count == 2)
					return -1;
				return size;
			}
		}).when(socket).write(isA(ByteBuffer.class));

		// act
		sendMessages(SMALL_MESSAGE_SIZE);

		// assert
		assertTrue(latch.await(6, TimeUnit.SECONDS));
		priorityTransportLayer.printMemStats(0);
	}

	@Ignore
	@SuppressWarnings("unchecked")
	@Test
	public void testSmallMessageOpenSocketFailsOnSecondMessage()
			throws Exception {
		// setup
		final AtomicInteger socketWriteCount = new AtomicInteger();
		final AtomicInteger socketOpenCount = new AtomicInteger();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation)
					throws Throwable {
				System.err.println("OPEN");
				int count = socketOpenCount.incrementAndGet();
				SocketCallback socketCallback = (SocketCallback) invocation
						.getArguments()[1];

				if (count != 2)
					socketCallback.receiveResult(socketRequestHandle, socket);
				else
					socketCallback.receiveException(socketRequestHandle,
							new RuntimeException("unit test open socket fail"));
				return null;
			}
		}).when(livenessTransportLayerImpl).openSocket(any(),
				(SocketCallback) any(), (Map) any());

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(500);
				ByteBuffer byteBuffer = (ByteBuffer) invocation.getArguments()[0];
				int size = byteBuffer.remaining();
				System.err.println("size: " + size);
				int count = -1;
				if (size > 10) {
					count = socketWriteCount.incrementAndGet();
				}
				while (byteBuffer.remaining() > 0) {
					byteBuffer.get();
				}
				if (count == 2)
					return -1;
				return size;
			}
		}).when(socket).write(isA(ByteBuffer.class));

		// act
		sendMessages(SMALL_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		priorityTransportLayer.printMemStats(0);
		sendMessages(SMALL_MESSAGE_SIZE);
		Thread.sleep(5000);
		priorityTransportLayer.printMemStats(0);
	}

	@Test
	public void testSmallMessageWriteSocketThrowsIOExceptionOnSecondMessage()
			throws Exception {
		// setup
		final AtomicInteger socketMessageCount = new AtomicInteger();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(500);
				ByteBuffer byteBuffer = (ByteBuffer) invocation.getArguments()[0];
				int size = byteBuffer.remaining();
				System.err.println("size: " + size);
				int count = -1;
				if (size > 10) {
					count = socketMessageCount.incrementAndGet();
				}
				if (count == 2)
					throw new IOException("unit test simulated error");
				while (byteBuffer.remaining() > 0) {
					byteBuffer.get();
				}
				return size;
			}
		}).when(socket).write(isA(ByteBuffer.class));

		// act
		sendMessages(SMALL_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		assertEquals(2, latch.getCount());
		priorityTransportLayer.printMemStats(0);

		// send more messages to re-start the process
		latch = new CountDownLatch((noofMessages * 2) - 1);
		sendMessages(SMALL_MESSAGE_SIZE);
		assertTrue(latch.await(6, TimeUnit.SECONDS));
		priorityTransportLayer.printMemStats(0);
	}

	@Test
	public void testSmallMessageWriteSocketThrowsIOExceptionWhenQueueHasOverflowed()
			throws Exception {
		// setup
		noofMessages = MAX_QUEUE_SIZE + 4; // expect 4 to get
		// QueueOverflowException
		latch = new CountDownLatch(MAX_QUEUE_SIZE);
		final AtomicInteger socketMessageCount = new AtomicInteger();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(500);
				ByteBuffer byteBuffer = (ByteBuffer) invocation.getArguments()[0];
				int size = byteBuffer.remaining();
				System.err.println("size: " + size);
				int count = -1;
				if (size > 10) {
					count = socketMessageCount.incrementAndGet();
				}
				if (count == 1)
					throw new IOException("unit test simulated error");
				while (byteBuffer.remaining() > 0) {
					byteBuffer.get();
				}
				return size;
			}
		}).when(socket).write(isA(ByteBuffer.class));

		// act
		sendMessages(SMALL_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		assertEquals(MAX_QUEUE_SIZE, latch.getCount());
		priorityTransportLayer.printMemStats(0);

		// send more messages to re-start the process
		latch = new CountDownLatch(MAX_QUEUE_SIZE + 1); // the original 4 plus
		// one of the second
		// batch
		sendMessages(SMALL_MESSAGE_SIZE);
		assertTrue("" + latch.getCount(), latch.await(6, TimeUnit.SECONDS));
		priorityTransportLayer.printMemStats(0);
	}

	@Test
	public void markDeadWhileSendingAndQueueIsFull() throws Exception {
		// setup
		noofMessages = MAX_QUEUE_SIZE * 2;
		latch = new CountDownLatch(MAX_QUEUE_SIZE);

		// act
		sendMessages(SMALL_MESSAGE_SIZE);
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(1200);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				System.err.println("MARK DEAD");
				EntityManager entityManager = getEntityManager(destination);
				entityManager.markDead();
			}
		});
		t.start();

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		assertEquals(3, latch.getCount());
		assertEquals(0, getEntityManager(destination).queueLength());

		// make sure new messages still get sent
		testSmallMessageOK();
	}

	@Ignore("work in progress")
	@SuppressWarnings("unchecked")
	@Test
	public void testSmallMessageDestinationGoesDeadWhenQueueIsFull()
			throws Exception {
		// setup
		noofMessages = MAX_QUEUE_SIZE + 4; // expect 4 to get
		// QueueOverflowException
		latch = new CountDownLatch(MAX_QUEUE_SIZE);

		final AtomicInteger livenessCheckCount = new AtomicInteger();

		doAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(InvocationOnMock invocation) throws Throwable {
				int count = livenessCheckCount.incrementAndGet();
				System.err.println("getLiveness(..)");
				if (count == 9) {
					System.err.println("returning "
							+ LivenessTypes.LIVENESS_DEAD);
					return LivenessTypes.LIVENESS_DEAD;
				}
				System.err.println("returning " + LivenessTypes.LIVENESS_ALIVE);
				return LivenessTypes.LIVENESS_ALIVE;
			}
		}).when(livenessTransportLayerImpl).getLiveness(any(), (Map) any());

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation)
					throws Throwable {
				SocketCallback socketCallback = (SocketCallback) invocation
						.getArguments()[1];
				Thread.sleep(6000);
				socketCallback.receiveResult(socketRequestHandle, socket);
				return null;
			}
		}).when(livenessTransportLayerImpl).openSocket(any(),
				(SocketCallback) any(), (Map) any());

		// act
		sendMessages(SMALL_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		assertEquals(MAX_QUEUE_SIZE, latch.getCount());
		priorityTransportLayer.printMemStats(0);

		// send more messages to re-start the process
		latch = new CountDownLatch(MAX_QUEUE_SIZE * 2);
		sendMessages(SMALL_MESSAGE_SIZE);
		assertTrue("" + latch.getCount(), latch.await(6, TimeUnit.SECONDS));
		priorityTransportLayer.printMemStats(0);
	}

	@SuppressWarnings( { "unchecked", "rawtypes" })
	@Test
	public void socketShouldNotBeCachedIfOpenFails() throws Exception {
		// setup
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(final InvocationOnMock invocation)
					throws Throwable {
				SocketCallback socketCallback = (SocketCallback) invocation
						.getArguments()[1];
				socketClosed = false;
				socketCallback.receiveException(socketRequestHandle,
						new IOException());
				return null;
			}
		}).when(livenessTransportLayerImpl).openSocket(any(),
				(SocketCallback) any(), (Map) any());
		// act
		sendMessages(SMALL_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		assertSocketsSize(0);
		assertWritingSocketIsNull();
	}

	@SuppressWarnings( { "unchecked", "rawtypes" })
	@Test
	public void socketShouldBeRemovedFromCacheIfRegisterFails()
			throws Exception {
		// setup
		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				final P2PSocketReceiver p2pSocketReceiver = (P2PSocketReceiver) invocation
						.getArguments()[2];
				Thread t = new Thread(new Runnable() {
					@Override
					public void run() {
						p2pSocketReceiver.receiveException(socket,
								new IOException("test"));
					}
				});
				t.start();
				return null;
			}
		}).when(socket).register(eq(false), eq(true),
				isA(P2PSocketReceiver.class));
		// act
		sendMessages(SMALL_MESSAGE_SIZE);

		// assert
		assertFalse(latch.await(6, TimeUnit.SECONDS));
		assertSocketsSize(0);
		assertWritingSocketIsNull();
	}

	@SuppressWarnings( { "unchecked" })
	@Test
	public void socketShouldBeRemovedFromCachedIfReadFails() throws Exception {
		// setup
		testSmallMessageOK();

		// assert
		assertSocketsSize(1);
		assertWritingSocketIsNotNull();

		// act
		getEntityManager(destination).closeMe(socket);

		// assert
		assertSocketsSize(0);
		assertWritingSocketIsNull();
	}

	@Test
	public void socketShouldBeRemovedFromCacheWhenSocketFailsOnLastMessage()
			throws Exception {
		// setup
		final AtomicInteger socketMessageCount = new AtomicInteger();

		doAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				Thread.sleep(500);
				ByteBuffer byteBuffer = (ByteBuffer) invocation.getArguments()[0];
				int size = byteBuffer.remaining();
				System.err.println("size: " + size);
				int count = -1;
				if (size > 10) {
					count = socketMessageCount.incrementAndGet();
				}
				while (byteBuffer.remaining() > 0) {
					byteBuffer.get();
				}
				if (count == noofMessages)
					return -1;
				return size;
			}
		}).when(socket).write(isA(ByteBuffer.class));

		// act
		sendMessages(SMALL_MESSAGE_SIZE);

		// assert
		assertTrue(latch.await(6, TimeUnit.SECONDS));
		assertSocketsSize(1);
		assertWritingSocketIsNotNull();
	}

	private void assertWritingSocketIsNull() {
		assertNull(getWritingSocket());
	}

	private void assertWritingSocketIsNotNull() {
		assertNotNull(getWritingSocket());
	}

	@SuppressWarnings("rawtypes")
	private P2PSocket getWritingSocket() {
		EntityManager entityManager = getEntityManager(destination);
		Field writingSocketField = ReflectionUtils.findField(
				EntityManager.class, "writingSocket");
		ReflectionUtils.makeAccessible(writingSocketField);
		return (P2PSocket) ReflectionUtils.getField(writingSocketField,
				entityManager);
	}

	@SuppressWarnings("rawtypes")
	private void assertSocketsSize(int i) {
		EntityManager entityManager = getEntityManager(destination);
		Field socketsField = ReflectionUtils.findField(EntityManager.class,
				"sockets");
		ReflectionUtils.makeAccessible(socketsField);
		Collection sockets = (Collection) ReflectionUtils.getField(
				socketsField, entityManager);
		assertEquals(i, sockets.size());
	}

	@SuppressWarnings("rawtypes")
	private PriorityTransportLayerImpl.EntityManager getEntityManager(
			MultiInetSocketAddress dest) {
		Field entityManagersField = ReflectionUtils.findField(
				PriorityTransportLayerImpl.class, "entityManagers");
		ReflectionUtils.makeAccessible(entityManagersField);
		Map entityManagers = (Map) ReflectionUtils.getField(
				entityManagersField, priorityTransportLayer);
		PriorityTransportLayerImpl.EntityManager em = (EntityManager) entityManagers
				.get(dest);

		return em;
	}

	@SuppressWarnings("rawtypes")
	private void assertPendingBigMessagesClear(MultiInetSocketAddress dest) {
		PriorityTransportLayerImpl.EntityManager em = getEntityManager(dest);

		Field pendingBigMessagesField = ReflectionUtils.findField(
				PriorityTransportLayerImpl.EntityManager.class,
				"pendingBigMessages");
		ReflectionUtils.makeAccessible(pendingBigMessagesField);
		Map pendingBigMessages = (Map) ReflectionUtils.getField(
				pendingBigMessagesField, em);
		assertFalse(pendingBigMessages.containsKey(dest));
	}

	private void sendMessages(final int messageSize) {
		for (int i = 0; i < noofMessages; i++) {
			final int x = i;
			Thread t = new Thread(new Runnable() {
				@Override
				public void run() {
					ByteBuffer messageBuffer = ByteBuffer
							.wrap(new byte[messageSize + x]);

					MessageRequestHandle<MultiInetSocketAddress, ByteBuffer> result = priorityTransportLayer
							.sendMessage(destination, messageBuffer,
									deliverAckToMe,
									new HashMap<String, Object>());
					System.err.println(result);
				}
			});
			t.start();
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
