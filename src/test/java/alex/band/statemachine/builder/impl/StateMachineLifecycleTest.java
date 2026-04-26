package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.StateMachineStopAction;
import alex.band.statemachine.builder.StateMachineBuilder;
import alex.band.statemachine.state.StateAction;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StateMachineLifecycleTest {

	private static final String INITIAL_STATE = "INITIAL_STATE";
	private static final String FINAL_STATE = "FINAL_STATE";
	private static final String STOP_EVENT = "STOP_EVENT";

	private StateMachineImpl<String, String> stateMachine;

	@Mock
	private StateAction<String, String> initialStateAction;
	@Mock
	private StateAction<String, String> finalStateAction;
	@Mock
	private StateMachineStartAction<String, String> startAction;
	@Mock
	private StateMachineStopAction<String, String> stopAction;

	@Test
	void startStop_stateMachineShouldExecuteStartActionOnStart() {
		stateMachine = buildMachineForStartStopTests();
		stateMachine.start();

		verify(startAction).onStart(isA(StateMachineDetails.class));
	}

	@Test
	void startStop_stateMachineShouldExecuteStopActionOnStop() {
		stateMachine = buildMachineForStartStopTests();
		stateMachine.start();
		stateMachine.stop();

		verify(stopAction).onStop(isA(StateMachineDetails.class));
	}

	@Test
	void startStop_stateMachineShouldExecuteOnEnterActionForInitialStateOnStart() {
		stateMachine = buildMachineForStartStopTests();
		stateMachine.start();

		verify(initialStateAction).onEnter(isA(StateMachineDetails.class));
	}

	@Test
	void startStop_stateMachineShouldExecuteOnExitActionForCurrentStateOnStop() {
		stateMachine = buildMachineForStartStopTests();
		stateMachine.start();
		stateMachine.stop();

		verify(initialStateAction).onExit(isA(StateMachineDetails.class));
	}

	@Test
	void startStop_stateMachineShouldBeRunningOnlyAfterStart() {
		stateMachine = buildMachineForStartStopTests();
		assertTrue(stateMachine.isReady());
		assertFalse(stateMachine.isRunning());

		stateMachine.start();
		assertFalse(stateMachine.isReady());
		assertTrue(stateMachine.isRunning());

		stateMachine.stop();
		assertFalse(stateMachine.isReady());
		assertFalse(stateMachine.isRunning());
	}

	@SuppressWarnings("unchecked")
	@Test
	void startStop_stateMachineShouldBeStoppedInFinalState() {
		stateMachine = buildMachineForStartStopTests();

		stateMachine.start();
		assertTrue(stateMachine.isRunning());
		assertEquals(INITIAL_STATE, stateMachine.getCurrentState().getId());

		stateMachine.accept(STOP_EVENT);
		assertFalse(stateMachine.isRunning());
		assertEquals(FINAL_STATE, stateMachine.getCurrentState().getId());

		verify(finalStateAction).onExit(isA(StateMachineDetails.class));
		verify(stopAction).onStop(isA(StateMachineDetails.class));
	}

	@Test
	void startStop_exceptionShouldBeThrownOnAttempToStartRunningStateMachine() {
		stateMachine = buildMachineForStartStopTests();
		stateMachine.start();

		assertThrows(IllegalStateException.class, () -> stateMachine.start());
	}

	@Test
	void startStop_exceptionShouldBeThrownOnAttempToAcceptEventWhenStateMachineIsStopped() {
		stateMachine = buildMachineForStartStopTests();

		stateMachine.start();
		stateMachine.stop();

		assertTrue(stateMachine.isStopped());
		assertThrows(IllegalStateException.class, () -> stateMachine.accept(STOP_EVENT));
	}

	@Test
	void lifecycle_newStateMachineShouldBeReady() {
		stateMachine = buildMachineForStartStopTests();

		assertTrue(stateMachine.isReady());
		assertFalse(stateMachine.isRunning());
		assertFalse(stateMachine.isStopped());
	}

	@Test
	void lifecycle_isReadyShouldReturnCorrectState() {
		stateMachine = buildMachineForStartStopTests();

		assertTrue(stateMachine.isReady());

		stateMachine.start();
		assertFalse(stateMachine.isReady());

		stateMachine.stop();
		assertFalse(stateMachine.isReady());
	}

	@Test
	void lifecycle_isStoppedShouldReturnTrueAfterStop() {
		stateMachine = buildMachineForStartStopTests();

		assertFalse(stateMachine.isStopped());

		stateMachine.start();
		assertFalse(stateMachine.isStopped());

		stateMachine.stop();
		assertTrue(stateMachine.isStopped());
	}

	@Test
	void lifecycle_stopShouldNotBeCalledBeforeStart() {
		stateMachine = buildMachineForStartStopTests();

		assertThrows(IllegalStateException.class, () -> stateMachine.stop());
	}

	@Test
	void lifecycle_allThreeStatesShouldBeMutuallyExclusive() {
		stateMachine = buildMachineForStartStopTests();

		assertTrue(stateMachine.isReady());
		assertFalse(stateMachine.isRunning());
		assertFalse(stateMachine.isStopped());

		stateMachine.start();
		assertFalse(stateMachine.isReady());
		assertTrue(stateMachine.isRunning());
		assertFalse(stateMachine.isStopped());

		stateMachine.stop();
		assertFalse(stateMachine.isReady());
		assertFalse(stateMachine.isRunning());
		assertTrue(stateMachine.isStopped());
	}

	@SuppressWarnings("unchecked")
	private StateMachineImpl<String, String> buildMachineForStartStopTests() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineStartStopActions().onStart(startAction).onStop(stopAction);

		builder.defineState(INITIAL_STATE).asInitial().withAction(initialStateAction);
		builder.defineState(FINAL_STATE).asFinal().withAction(finalStateAction);

		builder.defineExternalTransitionFor(INITIAL_STATE).to(FINAL_STATE).by(STOP_EVENT);

		return (StateMachineImpl<String, String>) builder.build();
	}

}
