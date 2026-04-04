package alex.band.statemachine;

/**
 * An action executed when the state machine stops {@link StateMachine}
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateMachineStopAction<S, E> {

	/**
	 * Executes the stop action of the state machine.
	 */
	void onStop(StateMachineDetails<S, E> context);

}
