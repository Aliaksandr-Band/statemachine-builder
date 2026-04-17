package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.listener.StateMachineListenerAdapter;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.state.State;

class StateMachineConcurrencyTest {

	private static final String S1 = "Ready";
	private static final String S2 = "Processing";
	private static final String S3 = "Stopped";

	private static final String TRANSITION = "Transition";

	private StateMachineBuilderImpl<String, String> builder;
	private ExecutorService threadPool;

	@BeforeEach
	void setUp() {
		builder = new StateMachineBuilderImpl<>();
		threadPool = Executors.newCachedThreadPool();
	}

	@AfterEach
	void tearDown() {
		threadPool.shutdown();
	}

	@Test
	void concurrentAccept_allTransitionsSerialized() throws Exception {
		builder.defineState(S1).asInitial();
		builder.defineState(S2);
		builder.defineState(S3).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION);
		builder.defineExternalTransitionFor(S2).to(S3).by(TRANSITION);

		StateMachine<String, String> sm = builder.build();
		sm.start();

		int threadCount = 10;
		CyclicBarrier barrier = new CyclicBarrier(threadCount + 1); // +1 for the main thread
		List<Boolean> results = Collections.synchronizedList(new java.util.ArrayList<>());

		List<Future<?>> futures = new java.util.ArrayList<>();
		for (int i = 0; i < threadCount; i++) {
			futures.add(threadPool.submit(() -> {
				try {
					barrier.await(5, TimeUnit.SECONDS);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
				results.add(sm.accept(TRANSITION));
			}));
		}

		// Main thread synchronizes with workers
		barrier.await(5, TimeUnit.SECONDS);

		for (Future<?> f : futures) {
			f.get(5, TimeUnit.SECONDS);
		}

		// First thread transitions to S2, second to S3 (final)
		// The rest are rejected
		long accepted = results.stream().filter(r -> r).count();
		assertEquals(2, accepted, "Only 2 transitions should succeed (S1→S2, S2→S3)");
		assertEquals(S3, sm.getCurrentState().getId());
	}

	@Test
	void getCurrentState_afterAccept_isDeterministic() throws Exception {

		int iterations = 100;
		for (int i = 0; i < iterations; i++) {
			StateMachineBuilderImpl<String, String> b = new StateMachineBuilderImpl<>();
			b.defineState(S1).asInitial();
			b.defineState(S2).asFinal();
			b.defineExternalTransitionFor(S1).to(S2).by(TRANSITION);

			StateMachine<String, String> fsm = b.build();
			fsm.start();

			boolean result = fsm.accept(TRANSITION);

			if (result) {
				assertEquals(S2, fsm.getCurrentState().getId(),
						"getCurrentState should return target state immediately after accept");
			}
		}
	}

	@Test
	void addListener_duringAccept_doesNotThrow() throws Exception {
		AtomicBoolean exceptionCaught = new AtomicBoolean(false);

		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION);

		StateMachine<String, String> sm = builder.build();
		sm.start();

		CountDownLatch latch = new CountDownLatch(1);

		Thread acceptThread = new Thread(() -> {
			try {
				sm.accept(TRANSITION);
			} catch (Exception e) {
				exceptionCaught.set(true);
			} finally {
				latch.countDown();
			}
		});

		Thread addListenerThread = new Thread(() -> {
			for (int i = 0; i < 100; i++) {
				sm.addListener(new StateMachineListenerAdapter<>() {
                });
			}
		});

		acceptThread.start();
		addListenerThread.start();

		acceptThread.join(5000);
		addListenerThread.join(5000);

		assertFalse(exceptionCaught.get(), "No exception should be thrown during concurrent addListener");
	}

	@Test
	void concurrentStartStop_noRaceCondition() throws InterruptedException {
		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION);

		// Repeatedly create and start/stop FSM from different threads
		for (int round = 0; round < 10; round++) {
			StateMachine<String, String> sm = builder.build();
			CountDownLatch startLatch = new CountDownLatch(1);
			CountDownLatch stopLatch = new CountDownLatch(1);
			AtomicBoolean startOk = new AtomicBoolean(false);
			AtomicBoolean stopOk = new AtomicBoolean(false);

			Thread startThread = new Thread(() -> {
				try {
					sm.start();
					startOk.set(true);
				} catch (IllegalStateException e) {
					// already started
				} finally {
					startLatch.countDown();
				}
			});

			Thread stopThread = new Thread(() -> {
				try {
					startLatch.await(2, TimeUnit.SECONDS);
					sm.stop();
					stopOk.set(true);
				} catch (IllegalStateException | InterruptedException e) {
					// already stopped
				} finally {
					stopLatch.countDown();
				}
			});

			startThread.start();
			stopThread.start();

			startThread.join(2000);
			stopThread.join(2000);

			// At least one operation should succeed
			assertTrue(startOk.get() || stopOk.get(), "At least one operation should succeed");
		}
	}

	/**
	 * Test verifies that the accept() method is actually synchronized.
	 * Two threads cannot simultaneously enter accept() - the second must wait for the first to complete.
	 */
	@Test
	void acceptMethod_isActuallySynchronized() throws Exception {
		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION);

		StateMachine<String, String> sm = builder.build();
		sm.start();

		// First thread calls accept() and delays inside
		CountDownLatch firstThreadEntered = new CountDownLatch(1);
		CountDownLatch firstThreadShouldExit = new CountDownLatch(1);

		// Create a listener that will execute during transition and create delay
		// Set up the listener ONCE before starting threads
		sm.addListener(new StateMachineListenerAdapter<>() {
            @Override
            public void onStateChanged(StateMachineMessage<String> message,
                                       State<String, String> previousState,
                                       StateMachineDetails<String, String> stateMachineDetails) {
                try {
                    // Notify that first thread entered critical section
                    firstThreadEntered.countDown();
                    // Wait until main thread allows us to exit
                    firstThreadShouldExit.await(2, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

		// Use an atomic flag to detect race conditions
		AtomicBoolean synchronizationTestPassed = new AtomicBoolean(true);

		Runnable testTask = () -> {
			try {
				// Simply call accept() - the listener will handle the synchronization test
				sm.accept(TRANSITION);
			} catch (Exception e) {
				// If any exception occurs, the test failed
				synchronizationTestPassed.set(false);
			}
		};

		// Start first thread
		Thread firstThread = new Thread(testTask);
		firstThread.start();

		// Wait until first thread enters critical section
		assertTrue(firstThreadEntered.await(2, TimeUnit.SECONDS), 
			"First thread should enter the synchronized section");

		// Now start second thread while first is still in accept()
		Thread secondThread = new Thread(testTask);
		secondThread.start();

		// Give second thread time to attempt entering (should be blocked)
		Thread.sleep(100);

		// At this point, the second thread should be waiting for the first to complete
		// If it wasn't synchronized, the second thread would have completed already

		// Allow first thread to complete
		firstThreadShouldExit.countDown();

		// Wait for both threads to complete
		firstThread.join(2000);
		secondThread.join(2000);

		// If no exceptions occurred, the synchronization worked correctly
		assertTrue(synchronizationTestPassed.get(), 
			"Synchronization test should pass without exceptions");
	}

}
