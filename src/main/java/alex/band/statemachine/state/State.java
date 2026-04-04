package alex.band.statemachine.state;

import com.google.common.base.Optional;

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
	 * Executes entry actions when entering this state.
	 */
	void onEnter(StateMachineDetails<S, E> context);

	/**
	 * Executes exit actions when leaving this state.
	 */
	void onExit(StateMachineDetails<S, E> context);

	/**
	 * Returns the state identifier.
	 */
	S getId();

	/**
	 * Returns {@code true} if the given {@link StateMachineMessage} can be deferred in the current state.
	 */
	boolean canBeDeferred(StateMachineMessage<E> message);

}
