package alex.band.statemachine.message;

import java.util.Optional;

import alex.band.statemachine.util.Asserts;


/**
 * Implementation of {@link StateMachineMessage}.
 *
 * @author Aliaksandr Bandarchyk
 */
public class StateMachineMessageImpl<E> implements StateMachineMessage<E> {

	private E event;
	private Object payload;


	public StateMachineMessageImpl(E event) {
		Asserts.checkNotNull(event, "Provided Event must not be null");

		this.event = event;
	}

	public StateMachineMessageImpl(E event, Object payload) {
		Asserts.checkNotNull(event, "Provided Event must not be null");

		this.event = event;
		this.payload = payload;
	}

	@Override
	public E getEvent() {
		return event;
	}

	@Override
	public Optional<Object> getPayload() {
		return Optional.ofNullable(payload);
	}

	@Override
	public String toString() {
		return "StateMachineMessageImpl [event=" + event + ", payload=" + payload + "]";
	}

}
