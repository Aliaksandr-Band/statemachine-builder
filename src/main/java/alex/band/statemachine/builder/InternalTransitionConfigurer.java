package alex.band.statemachine.builder;

import java.util.Set;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.transition.Guard;
import alex.band.statemachine.transition.Transition;
import alex.band.statemachine.transition.TransitionAction;

/**
 * Configurer for internal {@link Transition} of a {@link State} in the {@link StateMachine} S1->S1.
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface InternalTransitionConfigurer<S, E> {

	/**
	 * Sets the event that triggers the transition.
	 */
	InternalTransitionConfigurer<S, E> by(E event);

	/**
	 * Sets a {@link Guard} for evaluating the transition.
	 */
	InternalTransitionConfigurer<S, E> guardedBy(Guard<S, E> guard);

	/**
	 * Sets a {@link TransitionAction} to be executed during the transition.
	 */
	InternalTransitionConfigurer<S, E> withAction(TransitionAction<S, E> action);

	/**
	 * Sets a set of {@link TransitionAction}s to be executed during the transition.
	 */
	InternalTransitionConfigurer<S, E> withActions(Set<TransitionAction<S, E>> actions);

}
