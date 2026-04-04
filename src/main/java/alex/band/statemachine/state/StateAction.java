package alex.band.statemachine.state;

import alex.band.statemachine.StateMachineDetails;

/**
 * Actions associated with entering and exiting a {@link State}.
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateAction<S, E> {

	/**
	 * Action executed when entering the state.
	 */
	void onEnter(StateMachineDetails<S, E> stateMachineDetails);

	/**
	 * Action executed when exiting the state.
	 */
	void onExit(StateMachineDetails<S, E> stateMachineDetails);

}
