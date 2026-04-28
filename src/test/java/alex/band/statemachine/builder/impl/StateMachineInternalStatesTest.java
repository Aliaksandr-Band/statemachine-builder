package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.message.StateMachineMessage;

public class StateMachineInternalStatesTest {

	StateMachineBuilderImpl<String, String> builder = new StateMachineBuilderImpl<>();

	String S1 = "s1";
	String S2 = "s2";
	String E1 = "e1";
	String E2 = "e2";

	@BeforeEach
	void setUp() {
		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineInternalTransitionFor(S1).by(E2);
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
	}

	@Test
	void testStateMachineConstraintsInReadyState() {

		StateMachine<String, String> sm = builder.build();

		assertTrue(sm.isReady());
		assertFalse(sm.isFault());
		assertFalse(sm.isRunning());
		assertFalse(sm.isStopped());

		assertThrows(IllegalStateException.class, () -> sm.accept(E1));
		assertThrows(IllegalStateException.class, () -> sm.stop());
		assertThrows(IllegalStateException.class, () -> sm.reset());

		// same state after exceptions
		assertTrue(sm.isReady());
		assertFalse(sm.isFault());
		assertFalse(sm.isRunning());
		assertFalse(sm.isStopped());
	}

	@Test
	void testStateMachineConstraintsInRunningState() {

		StateMachine<String, String> sm = builder.build();
		sm.start();

		assertTrue(sm.isRunning());
		assertFalse(sm.isFault());
		assertFalse(sm.isReady());
		assertFalse(sm.isStopped());

		assertTrue(sm.accept(E2));

		String nullEvent = null;
		assertThrows(NullPointerException.class, () -> sm.accept(nullEvent));

		StateMachineMessage<String> nullMessage = null;
		assertThrows(NullPointerException.class, () -> sm.accept(nullMessage));

		assertThrows(IllegalStateException.class, () -> sm.reset());

		// same state after exceptions
		assertTrue(sm.isRunning());
		assertFalse(sm.isFault());
		assertFalse(sm.isReady());
		assertFalse(sm.isStopped());

		sm.stop();
	}

	@Test
	void testStateMachineConstraintsInStoppedState() {

		StateMachine<String, String> sm = builder.build();
		sm.start();
		sm.stop();

		assertTrue(sm.isStopped());
		assertFalse(sm.isRunning());
		assertFalse(sm.isFault());
		assertFalse(sm.isReady());

		assertThrows(IllegalStateException.class, () -> sm.accept(E1));
		assertThrows(IllegalStateException.class, () -> sm.start());

		// same state after exceptions
		assertTrue(sm.isStopped());
		assertFalse(sm.isRunning());
		assertFalse(sm.isFault());
		assertFalse(sm.isReady());

		sm.reset();
	}

	@SuppressWarnings("unchecked")
	@Test
	void testStateMachineConstraintsInFaultState() {

		StateMachineStartAction<String, String> startAction = mock(StateMachineStartAction.class);
		builder.defineStartStopActions().startAction(startAction);
		
		StateMachine<String, String> sm = builder.build();

		RuntimeException exc = new RuntimeException();
		doThrow(exc).when(startAction).onStart(sm);

		sm.start();

		// state after exception
		assertTrue(sm.isFault());
		assertFalse(sm.isStopped());
		assertFalse(sm.isRunning());
		assertFalse(sm.isReady());

		assertNull(sm.getCurrentState());

		assertThrows(IllegalStateException.class, () -> sm.accept(E1));
	}

}
