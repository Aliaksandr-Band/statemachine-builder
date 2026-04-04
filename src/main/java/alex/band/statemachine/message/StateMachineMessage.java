package alex.band.statemachine.message;

import com.google.common.base.Optional;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.builder.StateMachineBuilder;

/**
 * A message for the {@link StateMachine}. Contains an event identifier {@link #getEvent()} and an optional payload {@link #getPayload()}.
 *
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateMachineMessage<E> {

	/**
	 * The event that the state machine should react to.
	 *
	 * <p>The event is defined in the configuration of a specific state machine using {@link StateMachineBuilder}.
	 */
	E getEvent();

	/**
	 * The optional payload that may be present in the message.
	 */
	Optional<Object> getPayload();

}
