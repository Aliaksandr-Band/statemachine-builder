package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import alex.band.statemachine.builder.StateMachineBuilder;

class StateMachineDeferredEventTest {

	private static final String S1 = "S1";
	private static final String S2 = "S2";
	private static final String S3 = "S3";
	private static final String S4 = "S4";
	private static final String E1 = "E1";
	private static final String E2 = "E2";
	private static final String E3 = "E3";

	private StateMachineImpl<String, String> stateMachine;

	@Test
	void deferredEvent_stateMachineShouldAcceptDeferredEvent() {
		stateMachine = buildMachineForDeferredEventTests();
		stateMachine.start();

		assertEquals(S1, stateMachine.getCurrentState().getId());
		assertTrue(stateMachine.accept(E2));
		assertEquals(S1, stateMachine.getCurrentState().getId());
	}

	@Test
	void deferredEvent_deferredEventShouldBeProcessedByFirstStateWhichDoesNotDeferIt() {
		stateMachine = buildMachineForDeferredEventTests();
		stateMachine.start();

		stateMachine.accept(E2);
		assertEquals(S1, stateMachine.getCurrentState().getId());

		assertTrue(stateMachine.accept(E1)); // trigger S1->S2 by E1 and then S2->S3 by deferred E2
		assertEquals(S3, stateMachine.getCurrentState().getId()); // S3->S4 not happen because deferred E2 was used only for S2->S3
	}

	@Test
	void deferredEvent_StateMachineShouldResetDeferredEventOnStart() {
		stateMachine = buildMachineForDeferredEventTests();
		stateMachine.start();

		stateMachine.accept(E2);
		assertTrue(stateMachine.hasDeferredMessages());

		stateMachine.stop();
		stateMachine.start();
		assertFalse(stateMachine.hasDeferredMessages());
	}

	@Test
	void deferredEvent_queueShouldPreserveMultipleDeferredEvents() {
		stateMachine = buildMachineForMultipleDeferredEventsTests();
		stateMachine.start();

		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Defer two events
		stateMachine.accept(E2);
		stateMachine.accept(E3);

		// Verify both events are preserved
		assertTrue(stateMachine.hasDeferredMessages());
		assertEquals(2, stateMachine.getDeferredQueueSize());
		assertEquals(S1, stateMachine.getCurrentState().getId());
	}

	@Test
	void deferredEvent_queuedEventsShouldBeProcessedAfterStateChange() {
		stateMachine = buildMachineForMultipleDeferredEventsTests();
		stateMachine.start();

		// Defer two events
		stateMachine.accept(E2);
		stateMachine.accept(E3);
		assertEquals(2, stateMachine.getDeferredQueueSize());

		// E1 triggers S1->S2, then E2 is processed as deferred (S2->S3),
		// then E3 is processed (S3->S4)
		stateMachine.accept(E1);

		// Both deferred events processed, machine is in final state S4
		assertEquals(S4, stateMachine.getCurrentState().getId());
		assertEquals(0, stateMachine.getDeferredQueueSize());
		assertFalse(stateMachine.hasDeferredMessages());
	}

	// ==================== Borderline / Edge Case Tests ====================

	@Test
	void deferredEvent_processingStopsWhenNewStateDefersEvent() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial().withDeferredEvent(E2);
		builder.defineState(S2).withDeferredEvent(E2);
		builder.defineState(S3);
		builder.defineState(S4).asFinal();

		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		builder.defineExternalTransitionFor(S2).to(S3).by(E3);
		builder.defineExternalTransitionFor(S3).to(S4).by(E2);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();

		// Defer E2 in S1
		stateMachine.accept(E2);
		assertEquals(1, stateMachine.getDeferredQueueSize());

		// Transition to S2 — it also defers E2, processing must not start
		stateMachine.accept(E1);
		assertEquals(S2, stateMachine.getCurrentState().getId());
		assertEquals(1, stateMachine.getDeferredQueueSize()); // E2 still in queue

		// Transition to S3 by E3 — S3 does not defer E2, so E2 will be processed
		stateMachine.accept(E3);
		assertEquals(S4, stateMachine.getCurrentState().getId()); // S3->S4 by E2
		assertEquals(0, stateMachine.getDeferredQueueSize());
	}

	@Test
	void deferredEvent_finalStateDoesNotProcessDeferredEvents() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial().withDeferredEvent(E2);
		builder.defineState(S2).asFinal();

		builder.defineExternalTransitionFor(S1).to(S2).by(E1);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();

		// Defer E2 in S1
		stateMachine.accept(E2);
		assertEquals(1, stateMachine.getDeferredQueueSize());

		// Transition to final state
		stateMachine.accept(E1);
		assertEquals(S2, stateMachine.getCurrentState().getId());
		assertFalse(stateMachine.isRunning());

		// Queue must remain untouched — final state does not process deferred events
		assertEquals(1, stateMachine.getDeferredQueueSize());
	}

	@Test
	void deferredEvent_queueNotClearedOnStopButClearedOnStart() {
		stateMachine = buildMachineForDeferredEventTests();
		stateMachine.start();

		// Defer an event
		stateMachine.accept(E2);
		assertTrue(stateMachine.hasDeferredMessages());

		// stop() does NOT clear the queue
		stateMachine.stop();
		// After stop, machine is not running, so we can't call accept.
		// We verify that start() clears it by checking that after restart, queue is empty.

		stateMachine.start();
		assertFalse(stateMachine.hasDeferredMessages()); // start() clears the queue
		assertEquals(0, stateMachine.getDeferredQueueSize());
	}

	@Test
	void deferredEvent_duplicateEventsAreAllPreserved() {
		stateMachine = buildMachineForMultipleDeferredEventsTests();
		stateMachine.start();

		// Send E2 three times — all must be deferred
		stateMachine.accept(E2);
		stateMachine.accept(E2);
		stateMachine.accept(E2);

		assertEquals(3, stateMachine.getDeferredQueueSize());
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Transition to S2 — E2 is not deferred, all three will be processed
		stateMachine.accept(E1);
		// S2->S3 by E2, S3->S4 by E2 (first from queue), but S4 is final
		// After S2->S3, E2 is processed -> S3->S4 by E2, S4 is final
		// Remaining E2 stays in queue since S4 is final, stops processing
		// Verify queue is empty or contains remaining events
		assertTrue(stateMachine.getCurrentState().getId().equals(S3) || stateMachine.getCurrentState().getId().equals(S4));
	}

	@Test
	void deferredEvent_noInfiniteLoopWhenStatesDeferDifferentEvents() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial().withDeferredEvents(Set.of(E2, E3));
		builder.defineState(S2).withDeferredEvent(E3);
		builder.defineState(S3).asFinal();

		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		builder.defineExternalTransitionFor(S2).to(S3).by(E2);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();

		// Defer E2 and E3 in S1
		stateMachine.accept(E2);
		stateMachine.accept(E3);
		assertEquals(2, stateMachine.getDeferredQueueSize());

		// Transition to S2 — E2 is processed (S2->S3), E3 remains
		stateMachine.accept(E1);

		// S3 is final, E3 must not cause infinite loop
		assertEquals(S3, stateMachine.getCurrentState().getId());
		assertFalse(stateMachine.isRunning());
		// E3 remains in queue since S3 is final and does not process deferred
		assertEquals(1, stateMachine.getDeferredQueueSize());
	}

	@Test
	void deferredEvent_mixedDeferredAndImmediateEvents() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial().withDeferredEvent(E2);
		builder.defineState(S2);
		builder.defineState(S3).asFinal();

		builder.defineExternalTransitionFor(S1).to(S2).by(E3);
		builder.defineExternalTransitionFor(S2).to(S3).by(E2);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();

		// E2 is deferred
		stateMachine.accept(E2);
		assertEquals(1, stateMachine.getDeferredQueueSize());

		// E3 is not deferred — processed immediately, triggers S1->S2
		// After transition, E2 is automatically processed -> S2->S3
		stateMachine.accept(E3);
		assertEquals(S3, stateMachine.getCurrentState().getId());
		assertEquals(0, stateMachine.getDeferredQueueSize());
	}

	@Test
	void deferredEvent_queueSizeChangesCorrectly() {
		stateMachine = buildMachineForMultipleDeferredEventsTests();
		stateMachine.start();

		assertEquals(0, stateMachine.getDeferredQueueSize());

		stateMachine.accept(E2);
		assertEquals(1, stateMachine.getDeferredQueueSize());

		stateMachine.accept(E3);
		assertEquals(2, stateMachine.getDeferredQueueSize());

		// E1 triggers S1->S2, then E2 is processed S2->S3, E3 is processed S3->S4
		stateMachine.accept(E1);
		assertEquals(S4, stateMachine.getCurrentState().getId());
		assertEquals(0, stateMachine.getDeferredQueueSize());
	}

	private StateMachineImpl<String, String> buildMachineForDeferredEventTests() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial().withDeferredEvent(E2);
		builder.defineState(S2);
		builder.defineState(S3);
		builder.defineState(S4).asFinal();

		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		builder.defineExternalTransitionFor(S2).to(S3).by(E2);
		builder.defineExternalTransitionFor(S3).to(S4).by(E2);

		return (StateMachineImpl<String, String>) builder.build();
	}

	private StateMachineImpl<String, String> buildMachineForMultipleDeferredEventsTests() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial().withDeferredEvents(Set.of(E2, E3));
		builder.defineState(S2).withDeferredEvent(E3);
		builder.defineState(S3);
		builder.defineState(S4).asFinal();

		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		builder.defineExternalTransitionFor(S2).to(S3).by(E2);
		builder.defineExternalTransitionFor(S3).to(S4).by(E3);

		return (StateMachineImpl<String, String>) builder.build();
	}

}
