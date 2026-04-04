package alex.band.statemachine.builder;

import java.util.Set;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.StateMachineStopAction;
import alex.band.statemachine.transition.Transition;

/**
 * {@code Builder} for constructing a {@link StateMachine} with a given configuration.
 *
 * <p>Allows configuring the following state machine components:
 * <ul><li>Start and stop actions - {@link #defineStartStopActions()}</li>
 * <li>States - {@link #defineState(Object)}, {@link #defineStates(Set)}</li>
 * <li>External and internal transitions - {@link #defineExternalTransitionFor(Object)}, {@link #defineInternalTransitionFor(Object)}</li></ul>
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateMachineBuilder<S, E> {

	/**
	 * Configures start {@link StateMachineStartAction} and stop {@link StateMachineStopAction}
	 * actions of the {@link StateMachine}.
	 */
	StartStopActionsConfigurer<S, E> defineStartStopActions();

	/**
	 * Configures a new {@link State} of the {@link StateMachine}.
	 */
	StatesConfigurer<S, E> defineState(S state);

	/**
	 * Defines a set of {@link State} for the {@link StateMachine}.
	 */
	void defineStates(Set<S> states);

	/**
	 * Configures external {@link Transition} of the {@link StateMachine}.
	 */
	ExternalTransitionConfigurer<S, E> defineExternalTransitionFor(S sourceState);

	/**
	 * Configures internal {@link Transition} of the {@link StateMachine}.
	 */
	InternalTransitionConfigurer<S, E>  defineInternalTransitionFor(S sourceState);

	/**
	 * Builds the {@link StateMachine} with the given configuration.
	 */
	StateMachine<S, E> build();

}
