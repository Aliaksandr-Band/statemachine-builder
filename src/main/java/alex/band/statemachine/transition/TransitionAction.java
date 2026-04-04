package alex.band.statemachine.transition;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;

/**
 * An action associated with a {@link Transition} between {@link State} states.
 *
 * @author Aliaksandr Bandarchyk
 */
public interface TransitionAction<S, E> {

	/**
	 * Executes the action associated with the transition.
	 */
	void execute(StateMachineMessage<E> message, StateMachineDetails<S, E> context);

}
