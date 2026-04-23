package alex.band.statemachine.builder;

import java.util.Set;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.state.StateAction;

/**
 * Configurer for {@link State} of the {@link StateMachine}.
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StatesConfigurer<S, E> {

	/**
	 * Marks the state as initial. Only one initial state is allowed in the state machine configuration.
	 */
	StatesConfigurer<S, E> asInitial();

	/**
	 * Marks the state as final (terminal). Only one final state is allowed in the state machine configuration.
	 */
	StatesConfigurer<S, E> asFinal();

	/**
	 * Sets a set of {@link StateAction} for the configured state.
	 */
	StatesConfigurer<S, E> withActions(Set<StateAction<S, E>> actions);

	/**
	 * Sets a {@link StateAction} for the configured state.
	 */
	StatesConfigurer<S, E> withAction(StateAction<S, E> action);

}
