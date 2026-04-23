package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.builder.StateMachineBuilder;
import alex.band.statemachine.state.StateAction;

/**
 * Tests for exception handling in state entry/exit actions with ERROR level logging — issue #21.
 * <p>
 * Verifies that:
 * 1. Exceptions in onEnter/onExit are logged at ERROR level
 * 2. Exceptions are propagated to the caller
 * 3. Actions before the exception are executed, actions after are not
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StateActionExceptionTest {

	private static final String S1 = "S1";
	private static final String S2 = "S2";
	private static final String S3 = "S3";
	private static final String TRANSITION_EVENT_1 = "TRANSITION_EVENT_1";
	private static final String TRANSITION_EVENT_2 = "TRANSITION_EVENT_2";

	private StateMachineImpl<String, String> stateMachine;

	@Mock
	private StateAction<String, String> s1EnterAction1;
	@Mock
	private StateAction<String, String> s1EnterAction2;
	@Mock
	private StateAction<String, String> s1EnterAction3;
	@Mock
	private StateAction<String, String> s2EnterAction;
	@Mock
	private StateAction<String, String> s2ExitAction1;
	@Mock
	private StateAction<String, String> s2ExitAction2;

	/**
	 * Scenario: single onEnter action throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level and propagated.
	 */
	@Test
	void onEnter_singleActionThrowsException_shouldLogErrorAndPropagate() {
		// Arrange
		doThrow(new RuntimeException("Enter action failed"))
				.when(s1EnterAction1).onEnter(isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial().withAction(s1EnterAction1);
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT_1);

		stateMachine = (StateMachineImpl<String, String>) builder.build();

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, stateMachine::start);
		assertEquals("Enter action failed", exception.getMessage());

		// Verify action was attempted
		verify(s1EnterAction1, times(1)).onEnter(isA(StateMachineDetails.class));
	}

	/**
	 * Scenario: multiple onEnter actions, first throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level, first action executed,
	 * subsequent actions are not executed.
	 */
	@Test
	void onEnter_multipleActionsFirstThrowsException_shouldLogErrorAndStopExecuting() {
		// Arrange
		doThrow(new RuntimeException("First enter action failed"))
				.when(s1EnterAction1).onEnter(isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial()
				.withAction(s1EnterAction1)
				.withAction(s1EnterAction2)
				.withAction(s1EnterAction3);
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT_1);

		stateMachine = (StateMachineImpl<String, String>) builder.build();

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, stateMachine::start);
		assertEquals("First enter action failed", exception.getMessage());

		// Verify only first action was attempted
		verify(s1EnterAction1, times(1)).onEnter(isA(StateMachineDetails.class));
		verify(s1EnterAction2, never()).onEnter(any());
		verify(s1EnterAction3, never()).onEnter(any());
	}

	/**
	 * Scenario: multiple onEnter actions, second throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level, first two actions executed,
	 * third action is not executed.
	 */
	@Test
	void onEnter_multipleActionsSecondThrowsException_shouldLogErrorAndStopExecuting() {
		// Arrange
		doThrow(new RuntimeException("Second enter action failed"))
				.when(s1EnterAction2).onEnter(isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial()
				.withAction(s1EnterAction1)
				.withAction(s1EnterAction2)
				.withAction(s1EnterAction3);
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT_1);

		stateMachine = (StateMachineImpl<String, String>) builder.build();

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, stateMachine::start);
		assertEquals("Second enter action failed", exception.getMessage());

		// Verify first two actions were attempted
		verify(s1EnterAction1, times(1)).onEnter(isA(StateMachineDetails.class));
		verify(s1EnterAction2, times(1)).onEnter(isA(StateMachineDetails.class));
		verify(s1EnterAction3, never()).onEnter(any());
	}

	/**
	 * Scenario: multiple onEnter actions, last throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level, all actions executed
	 * before exception occurs.
	 */
	@Test
	void onEnter_multipleActionsLastThrowsException_shouldLogErrorAndStopExecuting() {
		// Arrange
		doThrow(new RuntimeException("Last enter action failed"))
				.when(s1EnterAction3).onEnter(isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial()
				.withAction(s1EnterAction1)
				.withAction(s1EnterAction2)
				.withAction(s1EnterAction3);
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT_1);

		stateMachine = (StateMachineImpl<String, String>) builder.build();

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class, stateMachine::start);
		assertEquals("Last enter action failed", exception.getMessage());

		// Verify first two actions were attempted, third was attempted but threw exception
		verify(s1EnterAction1, times(1)).onEnter(isA(StateMachineDetails.class));
		verify(s1EnterAction2, times(1)).onEnter(isA(StateMachineDetails.class));
		verify(s1EnterAction3, times(1)).onEnter(isA(StateMachineDetails.class));
	}

	/**
	 * Scenario: single onExit action throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level and propagated,
	 * state rolls back to previous state.
	 */
	@Test
	void onExit_singleActionThrowsException_shouldLogErrorAndPropagate() {
		// Arrange
		doThrow(new RuntimeException("Exit action failed"))
				.when(s1EnterAction1).onExit(isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial().withAction(s1EnterAction1);
		builder.defineState(S2).asFinal().withAction(s2EnterAction);
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT_1);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> stateMachine.accept(TRANSITION_EVENT_1));
		assertEquals("Exit action failed", exception.getMessage());

		// Verify state rolled back to S1
		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Verify exit action was attempted
		verify(s1EnterAction1, times(1)).onExit(isA(StateMachineDetails.class));
	}

	/**
	 * Scenario: multiple onExit actions, first throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level, state rolls back to previous state,
	 * only first exit action is executed.
	 */
	@Test
	void onExit_multipleActionsFirstThrowsException_shouldLogErrorAndStopExecuting() {
		// Arrange
		doThrow(new RuntimeException("First exit action failed"))
				.when(s2ExitAction1).onExit(isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial();
		builder.defineState(S2)
				.withAction(s2EnterAction)
				.withAction(s2ExitAction1)
				.withAction(s2ExitAction2);
		builder.defineState(S3).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT_1);
		builder.defineExternalTransitionFor(S2).to(S3).by(TRANSITION_EVENT_2);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();
		stateMachine.accept(TRANSITION_EVENT_1);
		assertEquals(S2, stateMachine.getCurrentState().getId());

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> stateMachine.accept(TRANSITION_EVENT_2));
		assertEquals("First exit action failed", exception.getMessage());

		// Verify state rolled back to S2
		assertEquals(S2, stateMachine.getCurrentState().getId());

		// Verify only first exit action was attempted
		verify(s2ExitAction1, times(1)).onExit(isA(StateMachineDetails.class));
		verify(s2ExitAction2, never()).onExit(any());
	}

	/**
	 * Scenario: multiple onExit actions, second throws exception.
	 * <p>
	 * Expected: exception is logged at ERROR level, state rolls back to previous state,
	 * first two exit actions are executed, third is not.
	 */
	@Test
	void onExit_multipleActionsSecondThrowsException_shouldLogErrorAndStopExecuting() {
		// Arrange
		doThrow(new RuntimeException("Second exit action failed"))
				.when(s2ExitAction2).onExit(isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
		builder.defineState(S1).asInitial();
		builder.defineState(S2)
				.withAction(s2EnterAction)
				.withAction(s2ExitAction1)
				.withAction(s2ExitAction2);
		builder.defineState(S3).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT_1);
		builder.defineExternalTransitionFor(S2).to(S3).by(TRANSITION_EVENT_2);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();
		stateMachine.accept(TRANSITION_EVENT_1);
		assertEquals(S2, stateMachine.getCurrentState().getId());

		// Act & Assert
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> stateMachine.accept(TRANSITION_EVENT_2));
		assertEquals("Second exit action failed", exception.getMessage());

		// Verify state rolled back to S2
		assertEquals(S2, stateMachine.getCurrentState().getId());

		// Verify first two exit actions were attempted
		verify(s2ExitAction1, times(1)).onExit(isA(StateMachineDetails.class));
		verify(s2ExitAction2, times(1)).onExit(isA(StateMachineDetails.class));
	}
}
