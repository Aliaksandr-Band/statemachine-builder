package alex.band.statemachine.builder.impl;

import java.util.Set;

import alex.band.statemachine.builder.StatesConfigurer;
import alex.band.statemachine.state.StateAction;
import alex.band.statemachine.state.StateImpl;

/**
 * Implementation of {@link StatesConfigurer}.
 *
 * @author Aliaksandr Bandarchyk
 */
public class StatesConfigurerImpl<S, E> implements StatesConfigurer<S, E> {

	private StateMachineBuilderImpl<S, E> builder;
	private StateImpl<S, E> state;

	public StatesConfigurerImpl(StateMachineBuilderImpl<S, E> builder, StateImpl<S, E> state) {
		this.builder = builder;
		this.state = state;
	}

	@Override
	public StatesConfigurer<S, E> asInitial() {
		builder.setInitialState(state);
		return this;
	}

	@Override
	public StatesConfigurer<S, E> asFinal() {
		builder.setFinalState(state);
		return this;
	}

	@Override
	public StatesConfigurer<S, E> withActions(Set<StateAction<S, E>> actions) {
		state.addActions(actions);
		return this;
	}

	@Override
	public StatesConfigurer<S, E> withAction(StateAction<S, E> action) {
		state.addAction(action);
		return this;
	}

	@Override
	public StatesConfigurer<S, E> withDeferredEvent(E deferredEvent) {
		state.addDeferredEvent(deferredEvent);
		return this;
	}

	@Override
	public StatesConfigurer<S, E> withDeferredEvents(Set<E> deferredEvents) {
		state.addDeferredEvents(deferredEvents);
		return this;
	}

}
