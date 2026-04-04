package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
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
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.message.StateMachineMessageImpl;
import alex.band.statemachine.state.StateAction;
import alex.band.statemachine.transition.Guard;
import alex.band.statemachine.transition.GuardsComposer;
import alex.band.statemachine.transition.TransitionAction;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StateMachineImplTest {

	// States for tests
	private static final String INITIAL_STATE = "INITIAL_STATE";
	private static final String FINAL_STATE = "FINAL_STATE";
	private static final String S1 = "S1";
	private static final String S2 = "S2";
	private static final String S3 = "S3";
	private static final String S4 = "S4";

	// Events for tests
	private static final String STOP_EVENT = "STOP_EVENT";
	private static final String TRUE_EVENT = "TRUE_EVENT";
	private static final String FALSE_EVENT = "FALSE_EVENT";
	private static final String E1 = "E1";
	private static final String E2 = "E2";

	private StateMachineImpl<String, String> stateMachine;

	@Mock
	private Guard<String, String> trueGuard;
	@Mock
	private Guard<String, String> falseGuard;
	@Mock
	private TransitionAction<String, String> transitionAction;
	@Mock
	private StateAction<String, String> initialStateAction;
	@Mock
	private StateAction<String, String> finalStateAction;
	@Mock
	private StateAction<String, String> stateAction;
	@Mock
	private StateMachineStartAction<String, String> startAction;
	@Mock
	private StateMachineStopAction<String, String> stopAction;


	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		when(trueGuard.evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class))).thenReturn(true);
		when(falseGuard.evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class))).thenReturn(false);
	}

	@SuppressWarnings("unchecked")
	@Test
	void startStop_stateMachineShouldExecuteStartActionOnStart() {
		stateMachine = buildMachineForStartStopTests();
		stateMachine.start();

		verify(startAction).onStart(isA(StateMachineDetails.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	void startStop_stateMachineShouldExecuteStopActionOnStop() {
		stateMachine = buildMachineForStartStopTests();
		stateMachine.start();
		stateMachine.stop();

		verify(stopAction).onStop(isA(StateMachineDetails.class));
	}

	@SuppressWarnings("unchecked")
	@Test
	void startStop_stateMachineShouldExecuteOnEnterActionForInitialStateOnStart() {
		stateMachine = buildMachineForStartStopTests();
		stateMachine.start();

		verify(initialStateAction).onEnter(isA(StateMachineDetails.class));
	}

	@SuppressWarnings("unchecked")
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
		assertFalse(stateMachine.isRunning());

		stateMachine.start();
		assertTrue(stateMachine.isRunning());

		stateMachine.stop();
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
	void startStop_exceptionShouldBeThrownOnSendingEventToStoppedStateMachine() {
		stateMachine = buildMachineForStartStopTests();

		assertThrows(IllegalStateException.class, () -> stateMachine.accept(STOP_EVENT));
	}


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
		assertTrue(stateMachine.hasDeferredMessage());

		stateMachine.stop();
		stateMachine.start();
		assertFalse(stateMachine.hasDeferredMessage());
	}

	@Test
	void eventProcessing_stateMachineShouldReturnTrueIfTransitionHappend() {
		stateMachine = buildMachineForEventProcessingTests();
		stateMachine.start();

		assertTrue(stateMachine.accept(E1));
	}

	@Test
	void eventProcessing_stateMachineShouldReturnFalseIfTransitionNotHappend() {
		stateMachine = buildMachineForEventProcessingTests();
		stateMachine.start();

		assertFalse(stateMachine.accept(E2));
	}

	@Test
	void eventProcessing_stateMachineShouldReturnFalseIfTransitionNotHappendDueToUnconfiguredEvent() {
		stateMachine = buildMachineForEventProcessingTests();
		stateMachine.start();

		assertFalse(stateMachine.accept("unconfiguredEvent"));
	}

	@Test
	void eventProcessing_stateMachineShouldPerformTransitionOnGuardEvaluatedToTrue() {
		stateMachine = buildMachineForEventProcessingTests();
		stateMachine.start();

		stateMachine.accept(E1);

		assertEquals(S2, stateMachine.getCurrentState().getId());
	}

	@Test
	void eventProcessing_stateMachineCanAcceptEventsAndMessages() {
		stateMachine = buildMachineForEventProcessingTests();
		stateMachine.start();

		assertTrue(stateMachine.accept(E1));
		assertTrue(stateMachine.accept(new StateMachineMessageImpl<>(TRUE_EVENT)));
	}

	@Test
	void eventProcessing_stateMachineShouldNotAcceptNullEvent() {
		stateMachine = buildMachineForEventProcessingTests();
		stateMachine.start();

		String nullEvent = null;
		assertFalse(stateMachine.accept(nullEvent));
	}

	@Test
	void eventProcessing_stateMachineShouldNotAcceptNullMessage() {
		stateMachine = buildMachineForEventProcessingTests();
		stateMachine.start();

		StateMachineMessage<String> nullMessage = null;
		assertFalse(stateMachine.accept(nullMessage));
	}

	@Test
	@SuppressWarnings("unchecked")
	void externalTransition_testExecutionFlowOnTrueGuard() {
		stateMachine = buildMachineForExternalTransitionTests();
		stateMachine.start();
		stateMachine.accept(TRUE_EVENT);

		verify(trueGuard, times(1)).evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class));

		verify(initialStateAction, times(1)).onExit(isA(StateMachineDetails.class));
		verify(transitionAction, times(1)).execute(isA(StateMachineMessage.class), isA(StateMachineDetails.class));
		verify(stateAction, times(1)).onEnter(isA(StateMachineDetails.class));
		verify(stateAction, never()).onExit(isA(StateMachineDetails.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void externalTransition_testExecutionFlowOnFalseGuard() {
		stateMachine = buildMachineForExternalTransitionTests();
		stateMachine.start();
		stateMachine.accept(FALSE_EVENT);

		verify(falseGuard, times(1)).evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class));

		verify(initialStateAction, never()).onExit(isA(StateMachineDetails.class));
		verify(transitionAction, never()).execute(isA(StateMachineMessage.class), isA(StateMachineDetails.class));
		verify(stateAction, never()).onEnter(isA(StateMachineDetails.class));
		verify(stateAction, never()).onExit(isA(StateMachineDetails.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void internalTransition_testExecutionFlowOnTrueGuard() {
		stateMachine = buildMachineForInternalTransitionTests();
		stateMachine.start();
		stateMachine.accept(TRUE_EVENT);

		verify(trueGuard, times(1)).evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class));

		verify(initialStateAction, never()).onExit(isA(StateMachineDetails.class));
		verify(transitionAction, times(1)).execute(isA(StateMachineMessage.class), isA(StateMachineDetails.class));
	}

	@Test
	@SuppressWarnings("unchecked")
	void internalTransition_testExecutionFlowOnFalseGuard() {
		stateMachine = buildMachineForInternalTransitionTests();
		stateMachine.start();
		stateMachine.accept(FALSE_EVENT);

		verify(falseGuard, times(1)).evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class));

		verify(initialStateAction, never()).onExit(isA(StateMachineDetails.class));
		verify(transitionAction, never()).execute(isA(StateMachineMessage.class), isA(StateMachineDetails.class));
	}

	@Test
	void guardsAnyCompositionNegativeTest() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1).guardedBy(GuardsComposer.considerAny(falseGuard, falseGuard));
		StateMachineImpl<String, String> sm = (StateMachineImpl<String, String>) builder.build();

		sm.start();
		sm.accept(E1);

		assertNotEquals(S2, sm.getCurrentState().getId());
	}

	@Test
	void guardsAnyCompositionPositiveTest() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1).guardedBy(GuardsComposer.considerAny(falseGuard, trueGuard));
		StateMachineImpl<String, String> sm = (StateMachineImpl<String, String>) builder.build();

		sm.start();
		sm.accept(E1);

		assertEquals(S2, sm.getCurrentState().getId());
	}

	@Test
	void guardsAndCompositionNegativeTest() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1).guardedBy(GuardsComposer.considerAll(trueGuard, falseGuard));
		StateMachineImpl<String, String> sm = (StateMachineImpl<String, String>) builder.build();

		sm.start();
		sm.accept(E1);

		assertNotEquals(S2, sm.getCurrentState().getId());
	}

	@Test
	void guardsAndCompositionPositiveTest() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1).guardedBy(GuardsComposer.considerAll(trueGuard, trueGuard));
		StateMachineImpl<String, String> sm = (StateMachineImpl<String, String>) builder.build();

		sm.start();
		sm.accept(E1);

		assertEquals(S2, sm.getCurrentState().getId());
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

	private StateMachineImpl<String, String> buildMachineForEventProcessingTests() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(INITIAL_STATE).asInitial();
		builder.defineState(FINAL_STATE).asFinal();
		builder.defineStates(Set.of(S1, S2));

		builder.defineExternalTransitionFor(INITIAL_STATE).to(S1).by(E1).guardedBy(falseGuard);
		builder.defineExternalTransitionFor(INITIAL_STATE).to(S2).by(E1).guardedBy(trueGuard);

		builder.defineExternalTransitionFor(S1).to(FINAL_STATE).by(TRUE_EVENT).guardedBy(trueGuard);
		builder.defineExternalTransitionFor(S2).to(FINAL_STATE).by(TRUE_EVENT).guardedBy(trueGuard);
		builder.defineExternalTransitionFor(S2).to(FINAL_STATE).by(FALSE_EVENT).guardedBy(falseGuard);

		return (StateMachineImpl<String, String>) builder.build();
	}

	private StateMachineImpl<String, String> buildMachineForExternalTransitionTests() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial().withAction(initialStateAction);
		builder.defineState(S2).withAction(stateAction);
		builder.defineState(S3).asFinal();
		builder.defineExternalTransitionFor(S2).to(S3).by(E1);

		builder.defineExternalTransitionFor(S1).to(S2).by(TRUE_EVENT).guardedBy(trueGuard).withAction(transitionAction);
		builder.defineExternalTransitionFor(S1).to(S2).by(FALSE_EVENT).guardedBy(falseGuard).withAction(transitionAction);

		return (StateMachineImpl<String, String>) builder.build();
	}

	private StateMachineImpl<String, String> buildMachineForInternalTransitionTests() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial().withAction(initialStateAction);
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);

		builder.defineInternalTransitionFor(S1).by(TRUE_EVENT).guardedBy(trueGuard).withAction(transitionAction);
		builder.defineInternalTransitionFor(S1).by(FALSE_EVENT).guardedBy(falseGuard).withAction(transitionAction);

		return (StateMachineImpl<String, String>) builder.build();
	}

}
