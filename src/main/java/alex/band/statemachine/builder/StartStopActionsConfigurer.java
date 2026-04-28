package alex.band.statemachine.builder;

import java.util.LinkedHashSet;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.StateMachineStopAction;

/**
 * Configurer for start {@link StateMachineStartAction} and stop {@link StateMachineStopAction} actions
 * of the {@link StateMachine}.
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StartStopActionsConfigurer<S, E> {

	/**
	 * Sets one {@link StateMachineStartAction} action of the state machine.
	 */
	StartStopActionsConfigurer<S, E> startAction(StateMachineStartAction<S, E> action);

	/**
	 * Sets a collections of {@link StateMachineStartAction} actions of the state
	 * machine.
	 */
	StartStopActionsConfigurer<S, E> startActions(LinkedHashSet<StateMachineStartAction<S, E>> actions);

	/**
	 * Sets one {@link StateMachineStopAction} action of the state machine.
	 */
	StartStopActionsConfigurer<S, E> stopAction(StateMachineStopAction<S, E> action);

	/**
	 * Sets a collections of {@link StateMachineStopAction} actions of the state
	 * machine.
	 */
	StartStopActionsConfigurer<S, E> stopActions(LinkedHashSet<StateMachineStopAction<S, E>> actions);

}
