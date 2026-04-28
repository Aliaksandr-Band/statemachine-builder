package alex.band.statemachine.state;

import java.util.Optional;
import java.util.Set;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.transition.Transition;

/**
 * A state of the {@link StateMachine}.
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface State<S, E> {

	/**
	 * Returns the {@link Transition} for the current state if the given
	 * {@link StateMachineMessage} is supported and the {@link Guard} associated
	 * with the transition evaluates to {@code true}.
	 */
	Optional<Transition<S, E>> getSuitableTransition(StateMachineMessage<E> message, StateMachineDetails<S, E> context);

	/**
	 * Returns the set of {@link StateAction}s associated with the state enter.
	 */
	Set<StateEnterAction<S, E>> getEnterActions(StateMachineDetails<S, E> context);

	/**
	 * Returns the set of {@link StateAction}s associated with the state exit.
	 */
	Set<StateExitAction<S, E>> getExitActions(StateMachineDetails<S, E> context);

	/**
	 * Returns the state identifier.
	 */
	S getId();

}
