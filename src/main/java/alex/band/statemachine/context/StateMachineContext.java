package alex.band.statemachine.context;

import java.util.concurrent.ConcurrentHashMap;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.StateMachineStopAction;
import alex.band.statemachine.state.StateAction;
import alex.band.statemachine.transition.TransitionAction;

/**
 * Stores contextual information as {@code (key, value)} pairs during
 * {@link StateMachine} execution.
 *
 * <p>
 * The content depends on the specific task requirements.
 *
 * <p>
 * {@code StateMachineContext} is a shared object available to all components
 * that define the state machine's behavior: {@link StateMachineStartAction},
 * {@link StateMachineStopAction}, {@link Guard}, {@link StateAction},
 * {@link TransitionAction}.
 *
 * <p>
 * The default implementation {@link StateMachineContextImpl} uses
 * {@link ConcurrentHashMap} for basic concurrency support. If you provide a
 * custom implementation of {@code StateMachineContext}, it is strongly
 * recommended to support concurrency as well.
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
