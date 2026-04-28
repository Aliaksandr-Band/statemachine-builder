package alex.band.statemachine.builder.impl;

import java.util.LinkedHashSet;

import alex.band.statemachine.builder.StatesConfigurer;
import alex.band.statemachine.state.StateEnterAction;
import alex.band.statemachine.state.StateExitAction;
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
	public StatesConfigurer<S, E> withEnterActions(LinkedHashSet<StateEnterAction<S, E>> actions) {
		state.addEnterActions(actions);
		return this;
	}

	@Override
	public StatesConfigurer<S, E> withEnterAction(StateEnterAction<S, E> action) {
		state.addEnterAction(action);
		return this;
	}

	@Override
	public StatesConfigurer<S, E> withExitActions(LinkedHashSet<StateExitAction<S, E>> actions) {
		state.addExitActions(actions);
		return this;
	}

	@Override
	public StatesConfigurer<S, E> withExitAction(StateExitAction<S, E> action) {
		state.addExitAction(action);
		return this;
	}

}
