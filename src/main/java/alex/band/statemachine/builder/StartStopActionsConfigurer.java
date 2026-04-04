package alex.band.statemachine.builder;

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
	 * Sets the start {@link StateMachineStartAction} actions of the state machine.
	 */
	@SuppressWarnings("unchecked")
	StartStopActionsConfigurer<S, E> onStart(StateMachineStartAction<S, E> ...actions);

	/**
	 * Sets the stop {@link StateMachineStopAction} actions of the state machine.
	 */
	@SuppressWarnings("unchecked")
	StartStopActionsConfigurer<S, E> onStop(StateMachineStopAction<S, E> ...actions);

}
