package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.StateMachineStopAction;
import alex.band.statemachine.state.StateEnterAction;
import alex.band.statemachine.state.StateExitAction;

public class StateMachineRollbackTest {

	private static final String S1 = "S1";
	private static final String S2 = "S2";
	private static final String S3 = "S3";

	private static final String E1 = "E1";
	private static final String E2 = "E2";

	private StateMachineBuilderImpl<String, String> builder;

	private StateMachine<String, String> sm;

	private StateMachineStartAction<String, String> startAction1, startAction2;
	private StateMachineStopAction<String, String> stopAction1, stopAction2;
	private StateEnterAction<String, String> stateEnterAction1, stateEnterAction2;
	private StateExitAction<String, String> stateExitAction1, stateExitAction2;

	private Consumer<Throwable> exceptionHandler;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void setUp() {
		builder = new StateMachineBuilderImpl<>();

		startAction1 = mock(StateMachineStartAction.class);
		startAction2 = mock(StateMachineStartAction.class);

		stopAction1 = mock(StateMachineStopAction.class);
		stopAction2 = mock(StateMachineStopAction.class);

		stateEnterAction1 = mock(StateEnterAction.class);
		stateEnterAction2 = mock(StateEnterAction.class);

		stateExitAction1 = mock(StateExitAction.class);
		stateExitAction2 = mock(StateExitAction.class);

		exceptionHandler = mock(Consumer.class);
	}

	@Test
	void testRollbackActionsOnStart() {

		builder.defineStartStopActions().startActions(new LinkedHashSet<>(Arrays.asList(startAction1, startAction2)));
		builder.defineState(S1).asInitial()
				.withEnterActions(new LinkedHashSet<>(Arrays.asList(stateEnterAction1, stateEnterAction2)));
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		sm = builder.build();
		sm.registerExceptionHandler(exceptionHandler);

		RuntimeException exc = new RuntimeException();
		doThrow(exc).when(stateEnterAction2).execute(any());

		InOrder mockOrder = inOrder(startAction1, startAction2, stateEnterAction1, stateEnterAction2, stateEnterAction1,
				startAction2, startAction1, exceptionHandler);

		sm.start();

		mockOrder.verify(startAction1).onStart(sm);
		mockOrder.verify(startAction2).onStart(sm);
		mockOrder.verify(stateEnterAction1).execute(sm);
		mockOrder.verify(stateEnterAction2).execute(sm);
		mockOrder.verify(stateEnterAction1).rollback(sm);
		mockOrder.verify(startAction2).rollback(sm);
		mockOrder.verify(startAction1).rollback(sm);
		mockOrder.verify(exceptionHandler).accept(exc);

		assertTrue(sm.isFault());
	}

	@Test
	void testRollbackActionsOnTransition() {

		builder.defineState(S1).asInitial()
				.withExitActions(new LinkedHashSet<>(Arrays.asList(stateExitAction1, stateExitAction2)));
		builder.defineState(S2).asFinal()
				.withEnterActions(new LinkedHashSet<>(Arrays.asList(stateEnterAction1, stateEnterAction2)));
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		sm = builder.build();
		sm.registerExceptionHandler(exceptionHandler);

		RuntimeException exc = new RuntimeException();
		doThrow(exc).when(stateEnterAction2).execute(any());

		InOrder mockOrder = inOrder(stateExitAction1, stateExitAction2, stateEnterAction1, stateEnterAction2,
				exceptionHandler);

		sm.start();

		assertFalse(sm.accept(E1));
		mockOrder.verify(stateExitAction1).execute(sm);
		mockOrder.verify(stateExitAction2).execute(sm);
		mockOrder.verify(stateEnterAction1).execute(sm);
		mockOrder.verify(stateEnterAction2).execute(sm);
		mockOrder.verify(stateEnterAction1).rollback(sm);
		mockOrder.verify(stateExitAction2).rollback(sm);
		mockOrder.verify(stateExitAction1).rollback(sm);
		mockOrder.verify(exceptionHandler).accept(exc);

		assertTrue(sm.isFault());
	}

	@Test
	void testRollbackActionsOnFinalEvent() {

		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal().withExitAction(stateExitAction1);
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		builder.defineStartStopActions().stopActions(
				new LinkedHashSet<>(Arrays.asList(stopAction1, stopAction2)));
		sm = builder.build();
		sm.registerExceptionHandler(exceptionHandler);

		RuntimeException exc = new RuntimeException();
		doThrow(exc).when(stopAction2).onStop(sm);

		InOrder mockOrder = inOrder(stateExitAction1, stopAction1, stopAction2, exceptionHandler);

		sm.start();

		assertFalse(sm.accept(E1));
		mockOrder.verify(stateExitAction1).execute(sm);
		mockOrder.verify(stopAction1).onStop(sm);
		mockOrder.verify(stopAction2).onStop(sm);
		mockOrder.verify(stopAction1).rollback(sm);
		mockOrder.verify(stateExitAction1).rollback(sm);
		mockOrder.verify(exceptionHandler).accept(exc);

		assertTrue(sm.isFault());
	}

	@Test
	void testRollbackActionsOnStop() {

		builder.defineState(S1).asInitial().withExitAction(stateExitAction1);
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		builder.defineStartStopActions().stopActions(new LinkedHashSet<>(Arrays.asList(stopAction1, stopAction2)));
		sm = builder.build();
		sm.registerExceptionHandler(exceptionHandler);

		RuntimeException exc = new RuntimeException();
		doThrow(exc).when(stopAction2).onStop(sm);

		InOrder mockOrder = inOrder(stateExitAction1, stopAction1, stopAction2, exceptionHandler);

		sm.start();
		sm.stop();

		mockOrder.verify(stateExitAction1).execute(sm);
		mockOrder.verify(stopAction1).onStop(sm);
		mockOrder.verify(stopAction2).onStop(sm);
		mockOrder.verify(stopAction1).rollback(sm);
		mockOrder.verify(stateExitAction1).rollback(sm);
		mockOrder.verify(exceptionHandler).accept(exc);

		assertTrue(sm.isFault());
	}

}
