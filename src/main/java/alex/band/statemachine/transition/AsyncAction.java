package alex.band.statemachine.transition;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;

/**
 * An asynchronous action associated with a {@link Transition} between {@link State} states.
 * Executed in a separate thread via {@link java.util.concurrent.ExecutorService}.
 *
 * @author Aliaksandr Bandarchyk
 */
public interface AsyncAction<S, E> {

	/**
	 * Executes the asynchronous action associated with the transition.
	 */
	void execute(StateMachineMessage<E> message, StateMachineDetails<S, E> context);

}
