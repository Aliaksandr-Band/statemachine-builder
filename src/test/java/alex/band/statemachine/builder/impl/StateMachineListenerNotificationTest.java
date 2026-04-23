package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import alex.band.statemachine.builder.StateMachineBuilder;
import alex.band.statemachine.listener.StateMachineListenerAdapter;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.message.StateMachineMessageImpl;
import alex.band.statemachine.state.State;

class StateMachineListenerNotificationTest {

	private static final String READY = "Ready";
	private static final String PROCESSING = "Processing";
	private static final String STOPPED = "Stopped";
	private static final String START = "Start";
	private static final String STOP = "Stop";

	@Test
	void listener_onStateChangedCalledOnlyForExternalTransitions() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(READY).asInitial();
		builder.defineState(PROCESSING);
		builder.defineState(STOPPED).asFinal();

		builder.defineExternalTransitionFor(READY).to(PROCESSING).by(START);
		builder.defineExternalTransitionFor(PROCESSING).to(STOPPED).by(STOP);

		StateMachineImpl<String, String> sm = (StateMachineImpl<String, String>) builder.build();

		TrackingListener<String, String> listener = new TrackingListener<>();
		sm.addListener(listener);

		sm.start();
		assertEquals(READY, sm.getCurrentState().getId());
		assertEquals(0, listener.stateChangedCount());

		sm.accept(START);
		assertEquals(1, listener.stateChangedCount());
		assertEquals(READY, listener.getLastPreviousState().getId());
		assertEquals(PROCESSING, sm.getCurrentState().getId());

		sm.accept(STOP);
		assertEquals(2, listener.stateChangedCount());
		assertEquals(PROCESSING, listener.getLastPreviousState().getId());
		assertEquals(STOPPED, sm.getCurrentState().getId());
	}

	@Test
	void listener_onStateChangedNotCalledForInternalTransitions() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(READY).asInitial();
		builder.defineState(STOPPED).asFinal();

		builder.defineExternalTransitionFor(READY).to(STOPPED).by(STOP);
		builder.defineInternalTransitionFor(READY).by(START);

		StateMachineImpl<String, String> sm = (StateMachineImpl<String, String>) builder.build();

		TrackingListener<String, String> listener = new TrackingListener<>();
		sm.addListener(listener);

		sm.start();
		assertEquals(0, listener.stateChangedCount());

		// Internal transition does not change state
		sm.accept(START);
		assertEquals(0, listener.stateChangedCount());
		assertEquals(READY, sm.getCurrentState().getId());

		// External transition changes state
		sm.accept(STOP);
		assertEquals(1, listener.stateChangedCount());
		assertEquals(READY, listener.getLastPreviousState().getId());
		assertEquals(STOPPED, sm.getCurrentState().getId());
	}

	@Test
	void listener_onEventNotAcceptedCalledWhenNoTransitionMatches() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(READY).asInitial();
		builder.defineState(STOPPED).asFinal();

		builder.defineExternalTransitionFor(READY).to(STOPPED).by(STOP);

		StateMachineImpl<String, String> sm = (StateMachineImpl<String, String>) builder.build();

		TrackingListener<String, String> listener = new TrackingListener<>();
		sm.addListener(listener);

		sm.start();

		// Unknown event — no transition matches
		boolean result = sm.accept(new StateMachineMessageImpl<>("Unknown"));
		assertFalse(result);
		assertEquals(0, listener.stateChangedCount());
		assertEquals(1, listener.notAcceptedCount());
	}


	private static class TrackingListener<S, E> extends StateMachineListenerAdapter<S, E> {
		private int stateChangedCount = 0;
		private int notAcceptedCount = 0;
		private State<S, E> lastPreviousState;
		private List<StateMachineMessage<E>> notAcceptedMessages = new ArrayList<>();

		@Override
		public void onStateChanged(StateMachineMessage<E> message, State<S, E> previousState, alex.band.statemachine.StateMachineDetails<S, E> stateMachineDetails) {
			stateChangedCount++;
			lastPreviousState = previousState;
		}

		@Override
		public void onEventNotAccepted(StateMachineMessage<E> message, alex.band.statemachine.StateMachineDetails<S, E> stateMachineDetails) {
			notAcceptedCount++;
			notAcceptedMessages.add(message);
		}

		int stateChangedCount() { return stateChangedCount; }
		int notAcceptedCount() { return notAcceptedCount; }
		State<S, E> getLastPreviousState() { return lastPreviousState; }
	}

}
