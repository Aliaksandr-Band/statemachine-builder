package alex.band.statemachine.message;

import com.google.common.base.Optional;


/**
 * Implementation of {@link StateMachineMessage}.
 *
 * @author Aliaksandr Bandarchyk
 */
public class StateMachineMessageImpl<E> implements StateMachineMessage<E> {

	private E event;
	private Object payload;


	public StateMachineMessageImpl(E event) {
		this.event = event;
	}

	public StateMachineMessageImpl(E event, Object payload) {
		this.event = event;
		this.payload = payload;
	}

	@Override
	public E getEvent() {
		return event;
	}

	@Override
	public Optional<Object> getPayload() {
		return Optional.fromNullable(payload);
	}

	@Override
	public String toString() {
		return "StateMachineMessageImpl [event=" + event + ", payload=" + payload + "]";
	}

}
