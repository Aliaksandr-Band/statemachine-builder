package alex.band.statemachine;

import alex.band.statemachine.listener.StateMachineListener;
import alex.band.statemachine.message.StateMachineMessage;

/**
 * Interface providing the core set of methods for working with a state machine.
 *
 * <p>Contains methods for:
 * <ul><li>Starting and stopping the state machine - {@link #start()}, {@link #stop()}.</li>
 * <li>Lifecycle management via events and messages - {@link #accept(Object)}, {@link #accept(StateMachineMessage)}.</li>
 * <li>Registering state machine listeners - {@link #addListener(StateMachineListener)}, {@link #removeListener(StateMachineListener)}.</li></ul>
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateMachine<S, E> extends StateMachineDetails<S, E> {

	/**
	 * Starts the state machine.
	 */
	void start();

	/**
	 * Stops the state machine.
	 */
	void stop();

	/**
	 * Sends an event to the state machine for processing.
	 */
	boolean accept(E event);

	/**
	 * Sends a message to the state machine for processing.
	 */
	boolean accept(StateMachineMessage<E> message);

	/**
	 * Registers a state machine lifecycle listener.
	 */
	void addListener(StateMachineListener<S, E> listener);

	/**
	 * Removes a state machine lifecycle listener.
	 */
	void removeListener(StateMachineListener<S, E> listener);

}
