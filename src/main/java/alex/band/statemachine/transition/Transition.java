package alex.band.statemachine.transition;

import java.util.Optional;
import java.util.Set;

import alex.band.statemachine.StateMachine;

/**
 * Describes a transition between {@link State} states of a {@link StateMachine}.
 *
 * <p>The interface defines the following transition components:
 * <ul><li>Source state {@link #getSource()}</li>
 * <li>Target state {@link #getTarget()}</li>
 * <li>Event that triggers the transition {@link #getEvent()}</li>
 * <li>Transition guard (permission) {@link #getGuard()}</li>
 * <li>Transition actions {@link #getActions()}</li>
 * <li>Transition type {@link #isExternal()}</li>
 * </ul>
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface Transition<S, E> {

	/**
	 * Returns the source state of the transition.
	 */
	S getSource();

	/**
	 * Returns the target state of the transition.
	 */
	Optional<S> getTarget();

	/**
	 * Returns the event that triggers the transition.
	 */
	E getEvent();

	/**
	 * Returns the {@link Guard} (permission) for the transition.
	 */
	Optional<Guard<S, E>> getGuard();

	/**
	 * Returns the set of {@link TransitionAction}s associated with the transition.
	 */
	Set<TransitionAction<S, E>> getActions();

	/**
	 * Returns the set of {@link AsyncAction}s associated with the transition.
	 * Executed asynchronously after the transition completes.
	 */
	Set<AsyncAction<S, E>> getAsyncActions();

	/**
	 * Returns the transition type: external or internal.
	 */
	boolean isExternal();

}
