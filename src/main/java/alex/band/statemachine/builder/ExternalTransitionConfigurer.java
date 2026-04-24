package alex.band.statemachine.builder;

import java.util.Set;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.transition.Guard;
import alex.band.statemachine.transition.Transition;
import alex.band.statemachine.transition.TransitionAction;

/**
 * Configurer for external {@link Transition} between {@link State} states of the {@link StateMachine}. S1->S2.
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface ExternalTransitionConfigurer<S, E> {

	/**
	 * Sets the target state of the transition.
	 */
	ExternalTransitionConfigurer<S, E> to(S state);

	/**
	 * Sets the event that triggers the transition.
	 */
	ExternalTransitionConfigurer<S, E> by(E event);

	/**
	 * Sets a {@link Guard} for evaluating the transition.
	 */
	ExternalTransitionConfigurer<S, E> guardedBy(Guard<S, E> guard);

	/**
	 * Sets a {@link TransitionAction} to be executed during the transition.
	 */
	ExternalTransitionConfigurer<S, E> withAction(TransitionAction<S, E> action);

	/**
	 * Sets a set of {@link TransitionAction}s to be executed during the transition.
	 */
	ExternalTransitionConfigurer<S, E> withActions(Set<TransitionAction<S, E>> actions);

}
