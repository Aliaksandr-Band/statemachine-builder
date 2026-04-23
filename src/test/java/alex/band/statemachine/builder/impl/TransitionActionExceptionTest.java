package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.builder.StateMachineBuilder;
import alex.band.statemachine.state.StateAction;
import alex.band.statemachine.transition.TransitionAction;

/**
 * Tests for exception handling in transition actions with ERROR level logging — issue #21.
 * <p>
 * Verifies that:
 * 1. Exceptions in transition actions are logged at ERROR level
 * 2. Exceptions are propagated to the caller
 * 3. Actions before the exception are executed, actions after are not
 * 4. State machine maintains atomicity by rolling back state on exception
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransitionActionExceptionTest {

	private static final String S1 = "S1";
	private static final String S2 = "S2";
	private static final String TRANSITION_EVENT = "TRANSITION_EVENT";

	private StateMachineImpl<String, String> stateMachine;

	@Mock
	private StateAction<String, String> s1ExitAction;
	@Mock
	private StateAction<String, String> s2EnterAction;
	@Mock
	private TransitionAction<String, String> transitionAction1;
	@Mock
	private TransitionAction<String, String> transitionAction2;
	@Mock
	private TransitionAction<String, String> transitionAction3;

	/**
	 * Scenario: single transition action throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level and propagated,
	 * state rolls back to previous state.
	 */
	@Test
	void singleTransitionActionThrowsException_shouldLogErrorAndPropagate() {
		// Arrange
		doThrow(new RuntimeException("Transition action failed"))
				.when(transitionAction1).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial().withAction(s1ExitAction);
		builder.defineState(S2).asFinal().withAction(s2EnterAction);
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT).withAction(transitionAction1);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> stateMachine.accept(TRANSITION_EVENT));
		assertEquals("Transition action failed", exception.getMessage());

		// Verify state rolled back to S1
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Verify transition action was attempted
		verify(transitionAction1, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));

		// Verify exit action was executed (before transition action)
		verify(s1ExitAction, times(1)).onExit(isA(StateMachineDetails.class));

		// Verify enter action was NOT executed (exception in transition action)
		verify(s2EnterAction, never()).onEnter(any());
	}

	/**
	 * Scenario: multiple transition actions, first throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level, state rolls back to previous state,
	 * only first transition action is executed, subsequent actions are not.
	 */
	@Test
	void multipleTransitionActionsFirstThrowsException_shouldLogErrorAndStopExecuting() {
		// Arrange
		doThrow(new RuntimeException("First transition action failed"))
				.when(transitionAction1).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial().withAction(s1ExitAction);
		builder.defineState(S2).asFinal().withAction(s2EnterAction);
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT)
				.withAction(transitionAction1)
				.withAction(transitionAction2)
				.withAction(transitionAction3);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> stateMachine.accept(TRANSITION_EVENT));
		assertEquals("First transition action failed", exception.getMessage());

		// Verify state rolled back to S1
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Verify only first transition action was attempted
		verify(transitionAction1, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));
		verify(transitionAction2, never()).execute(any(), any());
		verify(transitionAction3, never()).execute(any(), any());

		// Verify exit action was executed, enter action was not
		verify(s1ExitAction, times(1)).onExit(isA(StateMachineDetails.class));
		verify(s2EnterAction, never()).onEnter(any());
	}

	/**
	 * Scenario: multiple transition actions, second throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level, state rolls back to previous state,
	 * first two transition actions are executed, third is not.
	 */
	@Test
	void multipleTransitionActionsSecondThrowsException_shouldLogErrorAndStopExecuting() {
		// Arrange
		doThrow(new RuntimeException("Second transition action failed"))
				.when(transitionAction2).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial().withAction(s1ExitAction);
		builder.defineState(S2).asFinal().withAction(s2EnterAction);
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT)
				.withAction(transitionAction1)
				.withAction(transitionAction2)
				.withAction(transitionAction3);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> stateMachine.accept(TRANSITION_EVENT));
		assertEquals("Second transition action failed", exception.getMessage());

		// Verify state rolled back to S1
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Verify first two transition actions were attempted
		verify(transitionAction1, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));
		verify(transitionAction2, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));
		verify(transitionAction3, never()).execute(any(), any());

		// Verify exit action was executed, enter action was not
		verify(s1ExitAction, times(1)).onExit(isA(StateMachineDetails.class));
		verify(s2EnterAction, never()).onEnter(any());
	}

	/**
	 * Scenario: multiple transition actions, last throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level, state rolls back to previous state,
	 * all transition actions attempted but last failed.
	 */
	@Test
	void multipleTransitionActionsLastThrowsException_shouldLogErrorAndStopExecuting() {
		// Arrange
		doThrow(new RuntimeException("Last transition action failed"))
				.when(transitionAction3).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial().withAction(s1ExitAction);
		builder.defineState(S2).asFinal().withAction(s2EnterAction);
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT)
				.withAction(transitionAction1)
				.withAction(transitionAction2)
				.withAction(transitionAction3);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> stateMachine.accept(TRANSITION_EVENT));
		assertEquals("Last transition action failed", exception.getMessage());

		// Verify state rolled back to S1
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Verify all three transition actions were attempted
		verify(transitionAction1, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));
		verify(transitionAction2, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));
		verify(transitionAction3, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));

		// Verify exit action was executed, enter action was not
		verify(s1ExitAction, times(1)).onExit(isA(StateMachineDetails.class));
		verify(s2EnterAction, never()).onEnter(any());
	}

	/**
	 * Scenario: all transition actions complete successfully without exceptions.
	 * <p>
	 * Expected: state changes successfully, all actions executed.
	 */
	@Test
	void multipleTransitionActionsAllSucceed_shouldChangeState() {
		// Arrange
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial().withAction(s1ExitAction);
		builder.defineState(S2).asFinal().withAction(s2EnterAction);
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT)
				.withAction(transitionAction1)
				.withAction(transitionAction2)
				.withAction(transitionAction3);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Act
		stateMachine.accept(TRANSITION_EVENT);

		// Assert
		assertEquals(S2, stateMachine.getCurrentState().getId());

		// Verify all transition actions were executed
		verify(transitionAction1, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));
		verify(transitionAction2, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));
		verify(transitionAction3, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));

		// Verify exit and enter actions were executed
		verify(s1ExitAction, times(1)).onExit(isA(StateMachineDetails.class));
		verify(s2EnterAction, times(1)).onEnter(isA(StateMachineDetails.class));
	}

	/**
	 * Scenario: transition action throws exception - verify atomicity guarantee.
	 * <p>
	 * Expected: state machine rolls back to previous state, maintaining consistency.
	 * Exit action of source state is executed, but enter action of target state is not.
	 */
	@Test
	void transitionActionThrowsException_stateMustRollbackToPrevious() {
		// Arrange
		doThrow(new RuntimeException("Transition action failed"))
				.when(transitionAction1).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial().withAction(s1ExitAction);
		builder.defineState(S2).asFinal().withAction(s2EnterAction);
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT).withAction(transitionAction1);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> stateMachine.accept(TRANSITION_EVENT));
		assertEquals("Transition action failed", exception.getMessage());

		// KEY ASSERTION: state must be rolled back to S1
		assertEquals(S1, stateMachine.getCurrentState().getId(),
				"State machine should rollback to previous state on exception");

		// Verify execution order: exit -> transition (failed) -> enter (not executed)
		verify(s1ExitAction, times(1)).onExit(isA(StateMachineDetails.class));
		verify(transitionAction1, times(1)).execute(isA(alex.band.statemachine.message.StateMachineMessage.class), isA(StateMachineDetails.class));
		verify(s2EnterAction, never()).onEnter(any());
	}
}
