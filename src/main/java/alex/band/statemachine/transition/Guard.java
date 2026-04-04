package alex.band.statemachine.transition;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;

/**
 * Guard for a {@link Transition} between {@link State} states.
 *
 * <p>A transition is allowed if {@link #evaluate(StateMachineMessage, StateMachineDetails)} returns {@code true}.
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface Guard<S, E> {

	/**
	 * Evaluates whether the transition is allowed.
	 *
	 * <p>{@code true} — transition is allowed, {@code false} — transition is denied.
	 */
	boolean evaluate(StateMachineMessage<E> message, StateMachineDetails<S, E> context);

}
