package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.builder.StateMachineBuilder;

class StateMachineResetTest {

	private static final String READY = "READY";
	private static final String PROCESSING = "PROCESSING";
	private static final String STOPPED = "STOPPED";
	private static final String PROCESS = "PROCESS";
	private static final String STOP = "STOP";
	private static final String TEST_KEY = "testKey";
	private static final String TEST_VALUE = "testValue";

	@Test
	void reset_fromReadyState_shouldThrowIllegalStateException() {
		StateMachine<String, String> stateMachine = buildStateMachine();

		assertTrue(stateMachine.isReady());

		// Attempt to reset from RUNNING state should throw exception
		IllegalStateException exception = assertThrows(IllegalStateException.class, stateMachine::reset);
		assertEquals("Statemachine should be stopped before reset.", exception.getMessage());

		// State machine should still be ready after failed reset
		assertTrue(stateMachine.isReady());

		// Verify context is cleared
		assertTrue(stateMachine.isReady());
		assertFalse(stateMachine.isRunning());
		assertFalse(stateMachine.isStopped());
	}

	@Test
	void reset_fromStoppedState_shouldClearContextAndSetReady() {
		StateMachine<String, String> stateMachine = buildStateMachine();

		stateMachine.start();
		stateMachine.accept(STOP); // Transition to STOPPED (final state) - auto-stop

		assertTrue(stateMachine.isStopped());

		// Add some data to context during execution
		stateMachine.getContext().setValue(TEST_KEY, TEST_VALUE);
		assertEquals(TEST_VALUE, stateMachine.getContext().getValue(TEST_KEY));

		// Reset from STOPPED state
		stateMachine.reset();

		// Verify context is cleared
		assertNull(stateMachine.getContext().getValue(TEST_KEY));
		assertTrue(stateMachine.isReady());
		assertFalse(stateMachine.isRunning());
		assertFalse(stateMachine.isStopped());
	}

	@Test
	void reset_fromRunningState_shouldThrowIllegalStateException() {
		StateMachine<String, String> stateMachine = buildStateMachine();

		stateMachine.start();

		assertTrue(stateMachine.isRunning());

		// Attempt to reset from RUNNING state should throw exception
		IllegalStateException exception = assertThrows(IllegalStateException.class, stateMachine::reset);
		assertEquals("Statemachine should be stopped before reset.", exception.getMessage());

		// State machine should still be running after failed reset
		assertTrue(stateMachine.isRunning());
	}

	@Test
	void reset_afterMultipleTransitions_shouldClearAllContextData() {
		StateMachine<String, String> stateMachine = buildStateMachine();

		stateMachine.start();

		// Add data during transitions
		stateMachine.getContext().setValue("key1", "value1");
		stateMachine.accept(PROCESS);
		stateMachine.getContext().setValue("key2", "value2");

		// Stop before reset
		stateMachine.stop();

		// Reset
		stateMachine.reset();

		// Verify all context data is cleared
		assertNull(stateMachine.getContext().getValue("key1"));
		assertNull(stateMachine.getContext().getValue("key2"));
		assertTrue(stateMachine.isReady());
	}

	@Test
	void resetAndStart_shouldWorkCorrectly() {
		StateMachine<String, String> stateMachine = buildStateMachine();

		// First run
		stateMachine.start();
		stateMachine.accept(STOP); // Transition to final state - auto-stop

		assertTrue(stateMachine.isStopped());

		// Reset
		stateMachine.reset();
		assertTrue(stateMachine.isReady());

		// Start again - should work
		stateMachine.start();
		assertTrue(stateMachine.isRunning());
		assertEquals(READY, stateMachine.getCurrentState().getId());

		stateMachine.accept(PROCESS);
		assertEquals(PROCESSING, stateMachine.getCurrentState().getId());

		stateMachine.accept(STOP); // Auto-stop
		assertTrue(stateMachine.isStopped());
	}

	@Test
	void reset_shouldClearContextCompletely() {
		StateMachine<String, String> stateMachine = buildStateMachine();

		stateMachine.start();
		stateMachine.accept(STOP); // Auto-stop

		// Add multiple keys to context
		stateMachine.getContext().setValue("key1", "value1");
		stateMachine.getContext().setValue("key2", "value2");
		stateMachine.getContext().setValue("key3", "value3");
		stateMachine.getContext().setValue("key4", 123);

		// Reset
		stateMachine.reset();

		// All keys should be gone
		assertNull(stateMachine.getContext().getValue("key1"));
		assertNull(stateMachine.getContext().getValue("key2"));
		assertNull(stateMachine.getContext().getValue("key3"));
		assertNull(stateMachine.getContext().getValue("key4"));
	}

	private StateMachine<String, String> buildStateMachine() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(READY).asInitial();
		builder.defineState(PROCESSING);
		builder.defineState(STOPPED).asFinal();
		builder.defineExternalTransitionFor(READY).to(PROCESSING).by(PROCESS);
		builder.defineExternalTransitionFor(PROCESSING).to(STOPPED).by(STOP);
		builder.defineExternalTransitionFor(READY).to(STOPPED).by(STOP); // Direct transition from READY to STOPPED
		return builder.build();
	}

}
