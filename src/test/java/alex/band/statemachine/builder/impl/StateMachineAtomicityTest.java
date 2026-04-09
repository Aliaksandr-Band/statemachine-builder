package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.builder.StateMachineBuilder;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.state.StateAction;
import alex.band.statemachine.transition.TransitionAction;

/**
 * Tests for atomicity of processMessage() — issue #17.
 * <p>
 * When an exception is thrown during transition actions or entry actions,
 * the state machine must rollback to the previous state instead of
 * remaining in an inconsistent state where exit actions were executed
 * but currentState was not updated.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StateMachineAtomicityTest {

	private static final String S1 = "S1";
	private static final String S2 = "S2";
	private static final String TRANSITION_EVENT = "TRANSITION_EVENT";

	private StateMachineImpl<String, String> stateMachine;

	@Mock
	private StateAction<String, String> s1ExitAction;
	@Mock
	private StateAction<String, String> s2EnterAction;
	@Mock
	private TransitionAction<String, String> normalTransitionAction;

	/**
	 * Scenario: entry action of the new state throws RuntimeException.
	 * <p>
	 * Current (buggy) behavior: S1.onExit() called, currentState changed to S2,
	 * then S2.onEnter() throws. State machine ends up in S2 with exit from S1 done,
	 * but entry to S2 failed — inconsistent state.
	 * <p>
	 * Expected (after fix): currentState must rollback to S1, exception must propagate.
	 */
	@Test
	void atomicity_enterActionThrows_stateMustRollbackToPrevious() {
		doThrow(new RuntimeException("Enter action failed"))
				.when(s2EnterAction).onEnter(isA(StateMachineDetails.class));

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial().withAction(s1ExitAction);
		builder.defineState(S2).asFinal().withAction(s2EnterAction);
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT).withAction(normalTransitionAction);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();

		assertEquals(S1, stateMachine.getCurrentState().getId());

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> stateMachine.accept(TRANSITION_EVENT));

		assertEquals("Enter action failed", exception.getMessage());

		// KEY ASSERTION: state must be rolled back to S1
		assertEquals(S1, stateMachine.getCurrentState().getId());
	}

	/**
	 * Scenario: transition completes successfully — no rollback needed.
	 */
	@Test
	void atomicity_successfulTransition_stateChangesToTarget() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial().withAction(s1ExitAction);
		builder.defineState(S2).asFinal().withAction(s2EnterAction);
		builder.defineExternalTransitionFor(S1).to(S2).by(TRANSITION_EVENT).withAction(normalTransitionAction);

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();

		assertEquals(S1, stateMachine.getCurrentState().getId());

		stateMachine.accept(TRANSITION_EVENT);

		assertEquals(S2, stateMachine.getCurrentState().getId());
	}

}
