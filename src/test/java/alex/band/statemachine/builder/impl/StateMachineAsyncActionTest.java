package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import alex.band.statemachine.StateMachine;

class StateMachineAsyncActionTest {

	private static final String READY = "Ready";
	private static final String PROCESSING = "Processing";
	private static final String STOPPED = "Stopped";

	private static final String START = "Start";
	private static final String STOP = "Stop";

	private StateMachineBuilderImpl<String, String> builder;
	private ExecutorService executorService;

	@BeforeEach
	void setUp() {
		builder = new StateMachineBuilderImpl<>();
		executorService = Executors.newCachedThreadPool();
	}

	@AfterEach
	void tearDown() {
		executorService.shutdown();
	}

	@Test
	void asyncAction_executedAfterTransition() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);

		builder.withExecutorService(executorService);
		builder.defineState(READY).asInitial();
		builder.defineState(PROCESSING);
		builder.defineState(STOPPED).asFinal();
		builder.defineExternalTransitionFor(READY).to(PROCESSING).by(START)
				.withAsyncAction((msg, ctx) -> latch.countDown());
		builder.defineExternalTransitionFor(PROCESSING).to(STOPPED).by(STOP);

		StateMachine<String, String> sm = builder.build();
		sm.start();

		assertTrue(sm.accept(START));
		assertTrue(latch.await(2, TimeUnit.SECONDS));
	}

	@Test
	void asyncAction_executedInSeparateThread() throws InterruptedException {
		AtomicReference<String> acceptThread = new AtomicReference<>();
		AtomicReference<String> asyncThread = new AtomicReference<>();

		builder.withExecutorService(executorService);
		builder.defineState(READY).asInitial();
		builder.defineState(PROCESSING);
		builder.defineState(STOPPED).asFinal();
		builder.defineExternalTransitionFor(READY).to(PROCESSING).by(START)
				.withAsyncAction((msg, ctx) -> asyncThread.set(Thread.currentThread().getName()));
		builder.defineExternalTransitionFor(PROCESSING).to(STOPPED).by(STOP);

		StateMachine<String, String> sm = builder.build();
		sm.start();

		acceptThread.set(Thread.currentThread().getName());
		sm.accept(START);

		// Allow async action some time to complete
		Thread.sleep(200);

		assertNotEquals(acceptThread.get(), asyncThread.get(),
				"Async action should run in a different thread");
	}

	@Test
	void asyncAction_doesNotBlockAccept() throws InterruptedException {
		CountDownLatch asyncStarted = new CountDownLatch(1);
		CountDownLatch asyncCanFinish = new CountDownLatch(1);

		builder.withExecutorService(executorService);
		builder.defineState(READY).asInitial();
		builder.defineState(PROCESSING);
		builder.defineState(STOPPED).asFinal();
		builder.defineExternalTransitionFor(READY).to(PROCESSING).by(START)
				.withAsyncAction((msg, ctx) -> {
					asyncStarted.countDown();
					try {
						asyncCanFinish.await(2, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				});
		builder.defineExternalTransitionFor(PROCESSING).to(STOPPED).by(STOP);

		StateMachine<String, String> sm = builder.build();
		sm.start();

		// accept should return control quickly
		boolean accepted = sm.accept(START);
		assertTrue(accepted);

		// State already changed even though async action is still running
		assertEquals(PROCESSING, sm.getCurrentState().getId());

		// Let async action complete
		asyncCanFinish.countDown();
		assertTrue(asyncStarted.await(2, TimeUnit.SECONDS));
	}

	@Test
	void asyncAction_receivesCorrectMessageAndContext() throws InterruptedException {
		AtomicReference<String> receivedEvent = new AtomicReference<>();
		AtomicReference<String> receivedState = new AtomicReference<>();
		CountDownLatch latch = new CountDownLatch(1);

		builder.withExecutorService(executorService);
		builder.defineState(READY).asInitial();
		builder.defineState(PROCESSING);
		builder.defineState(STOPPED).asFinal();
		builder.defineExternalTransitionFor(READY).to(PROCESSING).by(START)
				.withAsyncAction((msg, ctx) -> {
					receivedEvent.set(msg.getEvent());
					receivedState.set(ctx.getCurrentState().getId());
					latch.countDown();
				});
		builder.defineExternalTransitionFor(PROCESSING).to(STOPPED).by(STOP);

		StateMachine<String, String> sm = builder.build();
		sm.start();
		sm.accept(START);

		assertTrue(latch.await(2, TimeUnit.SECONDS));
		assertEquals(START, receivedEvent.get());
		assertEquals(PROCESSING, receivedState.get());
	}

	@Test
	void multipleAsyncActions_allExecuted() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(3);

		builder.withExecutorService(executorService);
		builder.defineState(READY).asInitial();
		builder.defineState(PROCESSING);
		builder.defineState(STOPPED).asFinal();
		builder.defineExternalTransitionFor(READY).to(PROCESSING).by(START)
				.withAsyncActions(Set.of(
						(msg, ctx) -> latch.countDown(),
						(msg, ctx) -> latch.countDown(),
						(msg, ctx) -> latch.countDown()
				));
		builder.defineExternalTransitionFor(PROCESSING).to(STOPPED).by(STOP);

		StateMachine<String, String> sm = builder.build();
		sm.start();
		sm.accept(START);

		assertTrue(latch.await(2, TimeUnit.SECONDS));
	}

	@Test
	void asyncAction_withInternalTransition() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);

		builder.withExecutorService(executorService);
		builder.defineState(READY).asInitial();
		builder.defineState(PROCESSING);
		builder.defineState(STOPPED).asFinal();
		builder.defineExternalTransitionFor(READY).to(PROCESSING).by(START);
		builder.defineInternalTransitionFor(PROCESSING).by(STOP)
				.withAsyncAction((msg, ctx) -> latch.countDown());
		builder.defineExternalTransitionFor(PROCESSING).to(STOPPED).by("Finish");

		StateMachine<String, String> sm = builder.build();
		sm.start();
		sm.accept(START);
		assertEquals(PROCESSING, sm.getCurrentState().getId());

		sm.accept(STOP);
		assertTrue(latch.await(2, TimeUnit.SECONDS));
		assertEquals(PROCESSING, sm.getCurrentState().getId());
	}

}
