package alex.band.statemachine.builder.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
	private static final String DEFERRED_EVENT = "Deferred";

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
	void listener_onEventDeferredCalledForDeferredEvents() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(READY).asInitial().withDeferredEvent(DEFERRED_EVENT);
		builder.defineState(PROCESSING);
		builder.defineState(STOPPED).asFinal();

		builder.defineExternalTransitionFor(READY).to(PROCESSING).by(START);
		builder.defineExternalTransitionFor(PROCESSING).to(STOPPED).by(DEFERRED_EVENT);

		StateMachineImpl<String, String> sm = (StateMachineImpl<String, String>) builder.build();

		TrackingListener<String, String> listener = new TrackingListener<>();
		sm.addListener(listener);

		sm.start();

		// Deferred event — state does not change
		sm.accept(new StateMachineMessageImpl<>(DEFERRED_EVENT));
		assertEquals(0, listener.stateChangedCount());
		assertEquals(1, listener.deferredCount());
		assertEquals(READY, listener.getLastDeferredState().getId());
		assertEquals(READY, sm.getCurrentState().getId());

		// Trigger transition — deferred event processed automatically
		sm.accept(START);
		assertEquals(2, listener.stateChangedCount()); // READY->PROCESSING + PROCESSING->STOPPED (deferred)
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

	@Test
	void listener_eachDeferredMessageTriggersStateChangeNotification() {
		StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

		builder.defineState(READY).asInitial().withDeferredEvents(Set.of("E1", "E2"));
		builder.defineState(S1).withDeferredEvent("E2");
		builder.defineState(S2);
		builder.defineState(S3).asFinal();

		builder.defineExternalTransitionFor(READY).to(S1).by("Go");
		builder.defineExternalTransitionFor(S1).to(S2).by("E1");
		builder.defineExternalTransitionFor(S2).to(S3).by("E2");

		StateMachineImpl<String, String> sm = (StateMachineImpl<String, String>) builder.build();

		TrackingListener<String, String> listener = new TrackingListener<>();
		sm.addListener(listener);

		sm.start();

		// Defer two events
		sm.accept(new StateMachineMessageImpl<>("E1"));
		sm.accept(new StateMachineMessageImpl<>("E2"));
		assertEquals(0, listener.stateChangedCount());
		assertEquals(2, listener.deferredCount());

		// "Go" triggers READY->S1, then E1 is processed S1->S2, then E2 is processed S2->S3
		sm.accept("Go");

		// Three state changes: READY->S1, S1->S2, S2->S3
		assertEquals(3, listener.stateChangedCount());
		assertEquals(S3, sm.getCurrentState().getId());
	}

	private static final String S1 = "S1";
	private static final String S2 = "S2";
	private static final String S3 = "S3";

	private static class TrackingListener<S, E> extends StateMachineListenerAdapter<S, E> {
		private int stateChangedCount = 0;
		private int deferredCount = 0;
		private int notAcceptedCount = 0;
		private State<S, E> lastPreviousState;
		private State<S, E> lastDeferredState;
		private List<StateMachineMessage<E>> deferredMessages = new ArrayList<>();
		private List<StateMachineMessage<E>> notAcceptedMessages = new ArrayList<>();

		@Override
		public void onStateChanged(StateMachineMessage<E> message, State<S, E> previousState, alex.band.statemachine.StateMachineDetails<S, E> stateMachineDetails) {
			stateChangedCount++;
			lastPreviousState = previousState;
		}

		@Override
		public void onEventDeferred(StateMachineMessage<E> message, State<S, E> currentState, alex.band.statemachine.StateMachineDetails<S, E> stateMachineDetails) {
			deferredCount++;
			lastDeferredState = currentState;
			deferredMessages.add(message);
		}

		@Override
		public void onEventNotAccepted(StateMachineMessage<E> message, alex.band.statemachine.StateMachineDetails<S, E> stateMachineDetails) {
			notAcceptedCount++;
			notAcceptedMessages.add(message);
		}

		int stateChangedCount() { return stateChangedCount; }
		int deferredCount() { return deferredCount; }
		int notAcceptedCount() { return notAcceptedCount; }
		State<S, E> getLastPreviousState() { return lastPreviousState; }
		State<S, E> getLastDeferredState() { return lastDeferredState; }
	}

}
