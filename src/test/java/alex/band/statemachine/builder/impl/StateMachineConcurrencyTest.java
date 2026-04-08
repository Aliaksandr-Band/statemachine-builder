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
import alex.band.statemachine.listener.StateMachineListenerAdapter;

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
				sm.addListener(new StateMachineListenerAdapter<String, String>() {
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

}
