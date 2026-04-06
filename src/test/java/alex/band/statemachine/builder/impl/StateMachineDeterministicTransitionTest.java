package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

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
import alex.band.statemachine.transition.Guard;

/**
 * Tests for deterministic transition ordering.
 * When multiple transitions share the same event and their guards can evaluate to true,
 * the first defined transition should always win.
 *
 * @author Aliaksandr Bandarchyk
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StateMachineDeterministicTransitionTest {

	private static final String S1 = "S1";
	private static final String S2 = "S2";
	private static final String S3 = "S3";
	private static final String FINAL = "FINAL";
	private static final String SHARED_EVENT = "SHARED_EVENT";

	private StateMachineImpl<String, String> stateMachine;

	@Mock
	private Guard<String, String> alwaysTrueGuard1;
	@Mock
	private Guard<String, String> alwaysTrueGuard2;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		when(alwaysTrueGuard1.evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class))).thenReturn(true);
		when(alwaysTrueGuard2.evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class))).thenReturn(true);
	}

	/**
	 * Verifies that when multiple transitions are defined for the same event
	 * and both guards evaluate to true, the first defined transition wins.
	 */
	@Test
	void deterministicTransition_firstDefinedTransitionWins() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial();
		builder.defineState(S2);
		builder.defineState(S3);
		builder.defineState(FINAL).asFinal();

		// First transition: S1 -> S2 with always-true guard
		builder.defineExternalTransitionFor(S1).to(S2).by(SHARED_EVENT).guardedBy(alwaysTrueGuard1);
		// Second transition: S1 -> S3 with always-true guard (same event)
		builder.defineExternalTransitionFor(S1).to(S3).by(SHARED_EVENT).guardedBy(alwaysTrueGuard2);

		builder.defineExternalTransitionFor(S2).to(FINAL).by("FINISH");
		builder.defineExternalTransitionFor(S3).to(FINAL).by("FINISH");

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();

		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Both guards return true, but first transition (S1 -> S2) should win
		stateMachine.accept(SHARED_EVENT);

		assertEquals(S2, stateMachine.getCurrentState().getId());
	}

	/**
	 * Verifies that reversing the order of transition definition changes which one wins.
	 */
	@Test
	void deterministicTransition_orderMatters_whenReversed() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial();
		builder.defineState(S2);
		builder.defineState(S3);
		builder.defineState(FINAL).asFinal();

		// First transition: S1 -> S3 with always-true guard (reversed order)
		builder.defineExternalTransitionFor(S1).to(S3).by(SHARED_EVENT).guardedBy(alwaysTrueGuard2);
		// Second transition: S1 -> S2 with always-true guard
		builder.defineExternalTransitionFor(S1).to(S2).by(SHARED_EVENT).guardedBy(alwaysTrueGuard1);

		builder.defineExternalTransitionFor(S2).to(FINAL).by("FINISH");
		builder.defineExternalTransitionFor(S3).to(FINAL).by("FINISH");

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();

		assertEquals(S1, stateMachine.getCurrentState().getId());

		// Now S1 -> S3 should win because it was defined first
		stateMachine.accept(SHARED_EVENT);

		assertEquals(S3, stateMachine.getCurrentState().getId());
	}

	/**
	 * Verifies that when only the second transition's guard matches, it is selected.
	 */
	@Test
	void deterministicTransition_secondTransitionWins_whenFirstGuardFails() {
		Guard<String, String> falseGuard = new Guard<>() {
			@Override
			public boolean evaluate(StateMachineMessage<String> message, StateMachineDetails<String, String> context) {
				return false;
			}
		};

		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(S1).asInitial();
		builder.defineState(S2);
		builder.defineState(S3);
		builder.defineState(FINAL).asFinal();

		// First transition: guard always returns false
		builder.defineExternalTransitionFor(S1).to(S2).by(SHARED_EVENT).guardedBy(falseGuard);
		// Second transition: guard always returns true
		builder.defineExternalTransitionFor(S1).to(S3).by(SHARED_EVENT).guardedBy(alwaysTrueGuard1);

		builder.defineExternalTransitionFor(S2).to(FINAL).by("FINISH");
		builder.defineExternalTransitionFor(S3).to(FINAL).by("FINISH");

		stateMachine = (StateMachineImpl<String, String>) builder.build();
		stateMachine.start();

		assertEquals(S1, stateMachine.getCurrentState().getId());

		// First guard fails, second should win
		stateMachine.accept(SHARED_EVENT);

		assertEquals(S3, stateMachine.getCurrentState().getId());
	}

}
