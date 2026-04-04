package alex.band.statemachine;

/**
 * An action executed when the state machine starts {@link StateMachine}
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateMachineStartAction<S, E> {

	/**
	 * Executes the start action of the state machine.
	 */
	void onStart(StateMachineDetails<S, E> context);

}
