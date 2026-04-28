package alex.band.statemachine.builder;

import java.util.LinkedHashSet;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.state.StateEnterAction;
import alex.band.statemachine.state.StateExitAction;

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
	 * Sets a set of {@link StateEnterAction} for the configured state.
	 */
	StatesConfigurer<S, E> withEnterActions(LinkedHashSet<StateEnterAction<S, E>> actions);

	/**
	 * Sets a {@link StateEnterAction} for the configured state.
	 */
	StatesConfigurer<S, E> withEnterAction(StateEnterAction<S, E> action);

	/**
	 * Sets a set of {@link StateExitAction} for the configured state.
	 */
	StatesConfigurer<S, E> withExitActions(LinkedHashSet<StateExitAction<S, E>> actions);

	/**
	 * Sets a {@link StateExitAction} for the configured state.
	 */
	StatesConfigurer<S, E> withExitAction(StateExitAction<S, E> action);

}
