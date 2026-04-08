package alex.band.statemachine;

import java.util.LinkedHashSet;
import java.util.Set;

import alex.band.statemachine.listener.StateMachineListener;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.message.StateMachineMessageImpl;
import alex.band.statemachine.state.State;

/**
 * Abstract implementation of {@link StateMachine} that provides the mechanism for
 * registering/removing/notifying {@link StateMachineListener}s.
 *
 * @author Aliaksandr Bandarchyk
 */
public abstract class ListenableStateMachine<S, E> implements StateMachine<S, E> {

	private Set<StateMachineListener<S, E>> listeners = new LinkedHashSet<>();


	@Override
	public synchronized void start() {
		doStart();

		for (StateMachineListener<S, E> listener: listeners) {
			listener.onStart(this);
		}
	}

	/**
	 * Actions to start the state machine.
	 */
	protected abstract void doStart();

	@Override
	public synchronized void stop() {
		doStop();

		for (StateMachineListener<S, E> listener: listeners) {
			listener.onStop(this);
		}
	}

	/**
	 * Actions to stop the state machine.
	 */
	protected abstract void doStop();

	@Override
	public boolean accept(E event) {
		return accept(new StateMachineMessageImpl<>(event));
	}

	@Override
	public boolean accept(StateMachineMessage<E> message) {
		return doAccept(message);
	}

	/**
	 * Actions to process messages by the state machine.
	 */
	protected abstract boolean doAccept(StateMachineMessage<E> message);

	/**
	 * Notifies all registered listeners that the state has changed.
	 *
	 * @param message the message that caused the state change
	 * @param previousState the state before the change
	 */
	protected void notifyStateChanged(StateMachineMessage<E> message, State<S, E> previousState) {
		for (StateMachineListener<S, E> listener: listeners) {
			listener.onStateChanged(message, previousState, this);
		}
	}

	/**
	 * Notifies all registered listeners that an event has been deferred.
	 *
	 * @param message the deferred message
	 * @param currentState the current state that deferred the event
	 */
	protected void notifyEventDeferred(StateMachineMessage<E> message, State<S, E> currentState) {
		for (StateMachineListener<S, E> listener: listeners) {
			listener.onEventDeferred(message, currentState, this);
		}
	}

	/**
	 * Notifies all registered listeners that an event was not accepted.
	 *
	 * @param message the message that was not accepted
	 */
	protected void notifyEventNotAccepted(StateMachineMessage<E> message) {
		for (StateMachineListener<S, E> listener: listeners) {
			listener.onEventNotAccepted(message, this);
		}
	}

	@Override
	public synchronized void addListener(StateMachineListener<S, E> listener) {
		listeners.add(listener);
	}

	@Override
	public synchronized void removeListener(StateMachineListener<S, E> listener) {
		listeners.remove(listener);
	}

}
