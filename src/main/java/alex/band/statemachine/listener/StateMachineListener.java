package alex.band.statemachine.listener;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.state.State;

/**
 * Listener for the {@link StateMachine} lifecycle.
 *
 * <p>Provides callbacks after the following state machine stages:
 * <ul><li>state machine start - {@link #onStart(StateMachineDetails)}</li>
 * <li>state machine stop - {@link #onStop(StateMachineDetails)}</li>
 * <li>state change - {@link #onStateChanged(StateMachineMessage, State, StateMachineDetails)}</li>
 * <li>event rejection - {@link #onEventNotAccepted(StateMachineMessage, StateMachineDetails)}</li></ul>
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateMachineListener<S, E> {

	/**
	 * Called after the state machine start actions have been executed.
	 *
	 * <p>The listener can inspect the state machine state through the {@link StateMachineDetails} interface.
	 */
	void onStart(StateMachineDetails<S, E> stateMachineDetails);

	/**
	 * Called after the state machine stop actions have been executed.
	 *
	 * <p>The listener can inspect the state machine state through the {@link StateMachineDetails} interface.
	 */
	void onStop(StateMachineDetails<S, E> stateMachineDetails);

	/**
	 * Called after the state machine changes its state.
	 *
	 * <p>The listener receives information about the {@link StateMachineMessage} that caused the state change,
	 * the previous {@link State}, and the state machine details via the {@link StateMachineDetails} interface.
	 */
	void onStateChanged(StateMachineMessage<E> message, State<S, E> previousState, StateMachineDetails<S, E> stateMachineDetails);

	/**
	 * Called when an incoming {@link StateMachineMessage} cannot be processed in the current state
	 * under the current conditions.
	 *
	 * <p>The listener receives information about the {@link StateMachineMessage}
	 * and the state machine details via the {@link StateMachineDetails} interface.
	 */
	void onEventNotAccepted(StateMachineMessage<E> message, StateMachineDetails<S, E> stateMachineDetails);

	/**
	 * Called when an incoming {@link StateMachineMessage} is accepted but deferred for later processing
	 * because the current state is not ready to handle it.
	 *
	 * <p>The deferred event will be stored in the queue and automatically processed when the state machine
	 * transitions to a state that can handle it.
	 *
	 * <p>The listener receives information about the {@link StateMachineMessage} that was deferred,
	 * the current {@link State}, and the state machine details via the {@link StateMachineDetails} interface.
	 *
	 * @param message the deferred message
	 * @param currentState the current state that deferred the event
	 * @param stateMachineDetails the state machine details
	 */
	void onEventDeferred(StateMachineMessage<E> message, State<S, E> currentState, StateMachineDetails<S, E> stateMachineDetails);

}
