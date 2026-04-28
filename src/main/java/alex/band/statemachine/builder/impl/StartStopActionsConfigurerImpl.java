package alex.band.statemachine.builder.impl;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.StateMachineStopAction;
import alex.band.statemachine.builder.StartStopActionsConfigurer;

/**
 * Implementation of {@link StartStopActionsConfigurer}.
 *
 * @author Aliaksandr Bandarchyk
 */
public class StartStopActionsConfigurerImpl<S, E> implements StartStopActionsConfigurer<S, E> {

	private Set<StateMachineStartAction<S, E>> startActions = new LinkedHashSet<>();
	private Set<StateMachineStopAction<S, E>> stopActions = new LinkedHashSet<>();


	@Override
	public StartStopActionsConfigurer<S, E> startAction(StateMachineStartAction<S, E> action) {
		startActions.add(action);
		return this;
	}

	@Override
	public StartStopActionsConfigurer<S, E> startActions(LinkedHashSet<StateMachineStartAction<S, E>> actions) {
		startActions.addAll(actions);
		return this;
	}

	@Override
	public StartStopActionsConfigurer<S, E> stopAction(StateMachineStopAction<S, E> action) {
		stopActions.add(action);
		return this;
	}

	@Override
	public StartStopActionsConfigurer<S, E> stopActions(LinkedHashSet<StateMachineStopAction<S, E>> actions) {
		stopActions.addAll(actions);
		return this;
	}

	Set<StateMachineStartAction<S, E>> getStartActions() {
		return Collections.unmodifiableSet(startActions);
	}

	Set<StateMachineStopAction<S, E>> getStopActions() {
		return Collections.unmodifiableSet(stopActions);
	}

}
