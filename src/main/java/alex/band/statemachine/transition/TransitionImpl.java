package alex.band.statemachine.transition;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;


/**
 * Implementation of {@link Transition}.
 *
 * @author Aliaksandr Bandarchyk
 */
public class TransitionImpl<S, E> implements Transition<S, E> {

	private boolean external = true;
	private S source;
	private S target;
	private E event;
	private Guard<S, E> guard;
	private Set<TransitionAction<S, E>> actions = new HashSet<>();

	@Override
	public S getSource() {
		return source;
	}

	@Override
	public Optional<S> getTarget() {
		return Optional.ofNullable(target);
	}

	@Override
	public E getEvent() {
		return event;
	}

	@Override
	public Optional<Guard<S, E>> getGuard() {
		return Optional.ofNullable(guard);
	}

	@Override
	public Set<TransitionAction<S, E>> getActions() {
		return Collections.unmodifiableSet(actions);
	}

	@Override
	public boolean isExternal() {
		return external;
	}

	@Override
	public String toString() {
		return "TransitionImpl [external=" + external + ", source=" + source + ", target=" + target + ", event=" + event + "]";
	}

	public void setSource(S state) {
		this.source = state;
	}

	public void setTarget(S state) {
		this.target = state;
	}

	public void setEvent(E event) {
		this.event = event;
	}

	public void setGuard(Guard<S, E> guard) {
		this.guard = guard;
	}

	public void addAction(TransitionAction<S, E> action) {
		actions.add(action);
	}

	public void addActions(Set<TransitionAction<S, E>> actions) {
		this.actions.addAll(actions);
	}

	public void setExternal(boolean external) {
		this.external = external;
	}

}
