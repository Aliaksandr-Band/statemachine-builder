package alex.band.statemachine.context;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.StateMachineStopAction;
import alex.band.statemachine.state.StateAction;
import alex.band.statemachine.transition.TransitionAction;

/**
 * Data structure for storing contextual information required during the execution of the {@link StateMachine}.
 *
 * <p>Data is stored as {@code (key, value)} pairs. The content is determined by the needs of a specific task.
 *
 * <p>{@code StateMachineContext} acts as a {@code shared} object
 * available to all components that define the behavior of the state machine.
 * Namely: {@link StateMachineStartAction}, {@link StateMachineStopAction}, {@link Guard}, {@link StateAction}, {@link TransitionAction}
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateMachineContext {

	/**
	 * Returns the value for the given key, or {@code null} if not found.
	 */
	Object getValue(String key);

	/**
	 * Sets a value for the given key.
	 */
	void setValue(String key, Object value);

	/**
	 * Removes the value for the given key.
	 *
	 * @return the removed value, or {@code null} if not found
	 */
	Object removeValue(String key);

}
