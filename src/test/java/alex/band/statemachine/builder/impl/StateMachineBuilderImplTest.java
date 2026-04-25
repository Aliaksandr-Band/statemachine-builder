package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.StateMachineStopAction;
import alex.band.statemachine.context.StateMachineContext;

@ExtendWith(MockitoExtension.class)
class StateMachineBuilderImplTest {

	private static final String S1 = "S1";
	private static final String S2 = "S2";
	private static final String S3 = "S3";

	private static final String E1 = "E1";
	private static final String E2 = "E2";

	private StateMachineBuilderImpl<String, String> builder;

	@BeforeEach
	void setUp() {
		builder = new StateMachineBuilderImpl<>();
	}

	@Test
	void configurationWithoutStatesIsNotAllowed() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineInternalTransitionFor(S1).by(E1);
			builder.build();
		});
		assertTrue(ex.getMessage().contains(StateMachineBuilderImpl.THERE_ARE_NO_STATES_DEFINED));
	}

	@Test
	void configurationWithoutInitialStateIsNotAllowed() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1);
			builder.defineInternalTransitionFor(S1).by(E1);
			builder.build();
		});
		assertTrue(ex.getMessage().contains(StateMachineBuilderImpl.INITIAL_STATE_IS_NOT_DEFINED));
	}

	@Test
	void configurationWithoutFinalStateIsNotAllowed() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1).asInitial();
			builder.defineState(S2);
			builder.defineInternalTransitionFor(S1).by(E1);
			builder.build();
		});
		assertTrue(ex.getMessage().contains(StateMachineBuilderImpl.FINAL_STATE_IS_NOT_DEFINED));
	}

	@Test
	void transitionWithUnkownSourceStateIsNotAllowed() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1).asInitial();
			builder.defineState(S2).asFinal();
			builder.defineInternalTransitionFor(S3).by(E1);
			builder.build();
		});
		assertTrue(ex.getMessage().contains(withoutPlaceholder(StateMachineBuilderImpl.UNKOWN_SOURCE_STATES_IN_TRANSITIONS)));
		assertTrue(ex.getMessage().contains(S3));
	}

	@Test
	void transitionWithUnkownTargetStateIsNotAllowed() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1).asInitial();
			builder.defineState(S2).asFinal();
			builder.defineExternalTransitionFor(S1).to(S2).by(E1);
			builder.defineExternalTransitionFor(S1).to(S3).by(E2);
			builder.build();
		});
		assertTrue(ex.getMessage().contains(withoutPlaceholder(StateMachineBuilderImpl.UNKOWN_TARGET_STATES_IN_TRANSITIONS)));
		assertTrue(ex.getMessage().contains(S3));
	}

	@Test
	void externalTransitionsWithNotDefinedTargetStateIsNotAllowed() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1).asInitial();
			builder.defineState(S2).asFinal();
			builder.defineExternalTransitionFor(S1).by(E1);
			builder.build();
		});
		assertTrue(ex.getMessage().contains(withoutPlaceholder(StateMachineBuilderImpl.EXTERNAL_TRANSITION_HAS_NO_TARGET_STATE)));
	}

	@Test
	void externalTransitionsFromFinalStateIsNotAllowed() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1).asInitial();
			builder.defineState(S2).asFinal();
			builder.defineExternalTransitionFor(S1).to(S2).by(E1);
			builder.defineExternalTransitionFor(S2).to(S1).by(E1);
			builder.build();
		});
		assertTrue(ex.getMessage().contains(withoutPlaceholder(StateMachineBuilderImpl.ILLEGAL_TRANSITION_FROM_FINAL_STATE)));
	}

	@Test
	void internalTransitionsFromFinalStateIsNotAllowed() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1).asInitial();
			builder.defineState(S2).asFinal();
			builder.defineExternalTransitionFor(S1).to(S2).by(E1);
			builder.defineInternalTransitionFor(S2).by(E1);
			builder.build();
		});
		assertTrue(ex.getMessage().contains(withoutPlaceholder(StateMachineBuilderImpl.ILLEGAL_TRANSITION_FROM_FINAL_STATE)));
	}

	@Test
	void allStatesExceptInitialShouldHaveInboundTransition_caseWithTwoStates() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1).asInitial();
			builder.defineState(S2).asFinal();
			builder.build();
		});
		assertTrue(ex.getMessage().contains(withoutPlaceholder(StateMachineBuilderImpl.STATES_WITHOUT_INBOUND_TRANSITION)));
		assertTrue(ex.getMessage().contains(S2));
	}

	@Test
	void allStatesExceptInitialShouldHaveInboundTransition_caseWithThreeStates() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1).asInitial();
			builder.defineState(S2);
			builder.defineState(S3).asFinal();
			builder.defineExternalTransitionFor(S1).to(S3).by(E1);
			builder.defineExternalTransitionFor(S2).to(S3).by(E1);
			builder.build();
		});
		assertTrue(ex.getMessage()
				.contains(withoutPlaceholder(StateMachineBuilderImpl.STATES_WITHOUT_INBOUND_TRANSITION)));
		assertTrue(ex.getMessage().contains(S2));
	}

	@Test
	void allStatesExceptFinalShouldHaveOutboundTransition() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1).asInitial();
			builder.defineState(S2);
			builder.defineState(S3).asFinal();
			builder.defineExternalTransitionFor(S1).to(S2).by(E1);
			builder.defineExternalTransitionFor(S1).to(S3).by(E2);
			builder.build();
		});
		assertTrue(ex.getMessage().contains(withoutPlaceholder(StateMachineBuilderImpl.STATES_WITHOUT_OUTBOUND_TRANSITION)));
		assertTrue(ex.getMessage().contains(S2));
	}

	@Test
	void equalStateCannotBeDefinedTwice() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1);
			builder.defineState(S1);
		});
		assertTrue(ex.getMessage().contains(withoutPlaceholder(StateMachineBuilderImpl.STATE_ALREADY_DEFINED)));
		assertTrue(ex.getMessage().contains(S1));
	}

	@Test
	void defineStatesShouldCreateMultipleStates() {
		builder.defineStates(Set.of(S1, S2));
		builder.defineState("INITIAL").asInitial();
		builder.defineState("FINAL").asFinal();
		builder.defineExternalTransitionFor("INITIAL").to(S1).by(E1);
		builder.defineExternalTransitionFor("INITIAL").to(S2).by(E2);
		builder.defineExternalTransitionFor(S1).to("FINAL").by(E1);
		builder.defineExternalTransitionFor(S2).to("FINAL").by(E2);
		builder.build();
	}

	@Test
	void initialStateCannotBeDefinedTwice() {
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
			builder.defineState(S1).asInitial();
			builder.defineState(S2).asInitial();
		});
		assertTrue(ex.getMessage().contains(withoutPlaceholder(StateMachineBuilderImpl.INITIAL_STATE_ALREADY_DEFINED)));
	}

	@Test
	void multipleFinalStatesAreAllowed() {
		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineState(S3).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		builder.defineExternalTransitionFor(S1).to(S3).by(E2);
		builder.build();
	}

	@Test
	void validConfigurationShouldProduceStateMachine() {
		builder.defineState(S1).asInitial();
		builder.defineState(S2);
		builder.defineState(S3).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		builder.defineInternalTransitionFor(S2).by(E2);
		builder.defineExternalTransitionFor(S2).to(S3).by(E1);

		StateMachine<String, String> stateMachine = builder.build();
		stateMachine.start();
		assertEquals(S1, stateMachine.getCurrentState().getId());

		stateMachine.accept(E1);
		assertEquals(S2, stateMachine.getCurrentState().getId());

		stateMachine.accept(E2);
		assertEquals(S2, stateMachine.getCurrentState().getId());

		stateMachine.accept(E1);
		assertEquals(S3, stateMachine.getCurrentState().getId());
	}

	@Test
	void defaulStateMachineContextShouldBeAvailable() {
		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		StateMachine<String, String> stateMachine = builder.build();

		assertNotNull(stateMachine.getContext());
	}

	@Test
	void customStateMachineContextCanBeUsed() {
		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		builder.definedStateMachineContext(new TestContext());
		StateMachine<String, String> stateMachine = builder.build();

		assertTrue(stateMachine.getContext() instanceof TestContext);
	}

	@Test
	void startStopActionsShouldBeExecuted() {
		TestStartAction startAction = new TestStartAction();
		TestStopAction stopAction = new TestStopAction();

		builder.defineState(S1).asInitial();
		builder.defineState(S2).asFinal();
		builder.defineExternalTransitionFor(S1).to(S2).by(E1);
		builder.defineStartStopActions()
			.onStart(startAction)
			.onStop(stopAction);

		StateMachine<String, String> stateMachine = builder.build();
		stateMachine.start();
		assertTrue(startAction.wasExecuted());
		assertFalse(stopAction.wasExecuted());

		stateMachine.accept(E1);
		assertTrue(stopAction.wasExecuted());
	}

	private static final class TestContext implements StateMachineContext {

		@Override
		public Object getValue(String key) {
			return null;
		}

		@Override
		public void setValue(String key, Object value) {
		}

		@Override
		public Object removeValue(String key) {
			return null;
		}

		@Override
		public void clear() {
		}
	}

	private String withoutPlaceholder(String str) {
		int placeHolderIndex = str.indexOf("%s");

		if (placeHolderIndex > 0) {
			return str.substring(0, placeHolderIndex - 1);
		}
		return str;
	}

	private static final class TestStartAction implements StateMachineStartAction<String, String> {
		private boolean executed = false;

		@Override
		public void onStart(StateMachineDetails<String, String> context) {
			executed = true;
		}

		boolean wasExecuted() {
			return executed;
		}
	}

	private static final class TestStopAction implements StateMachineStopAction<String, String> {
		private boolean executed = false;

		@Override
		public void onStop(StateMachineDetails<String, String> context) {
			executed = true;
		}

		boolean wasExecuted() {
			return executed;
		}
	}

}
