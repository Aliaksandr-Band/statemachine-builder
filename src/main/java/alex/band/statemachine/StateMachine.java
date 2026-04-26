package alex.band.statemachine;

import java.util.function.Consumer;

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
	 * Resets the state machine to its initial READY state.
	 *
	 * <p>
	 * This method restores the state machine to its initial configuration:
	 * <ul><li>Clears the {@link alex.band.statemachine.context.StateMachineContext}</li>
	 * <li>Resets the current state to the initial state</li>
	 * <li>Sets the mode to {@code READY}</li></ul>
	 *
	 * <p>
	 * The reset operation is synchronous and thread-safe. It can only be performed
	 * when the state machine is not running (i.e., in {@code READY} or {@code STOPPED}
	 * mode). Attempting to reset a running state machine will result in an exception.
	 *
	 * <p>
	 * After reset, the state machine can be started again using {@link #start()}.
	 *
	 * @throws IllegalStateException if the state machine is currently running
	 */
	void reset();

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

	/**
	 * Registers exceptions handler which can be produced by user's business logic
	 */
	void registerExceptionHandler(Consumer<Throwable> handler);

	/**
	 * Unregister exceptions handler if it exists
	 */
	void unregisterExceptionHandler();

}
