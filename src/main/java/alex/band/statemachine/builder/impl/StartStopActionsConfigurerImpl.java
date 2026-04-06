package alex.band.statemachine.builder.impl;

import java.util.Arrays;
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
	public StartStopActionsConfigurer<S, E> onStart(StateMachineStartAction<S, E>... actions) {
		startActions.addAll(Arrays.asList(actions));
		return this;
	}

	@Override
	public StartStopActionsConfigurer<S, E> onStop(StateMachineStopAction<S, E>... actions) {
		stopActions.addAll(Arrays.asList(actions));
		return this;
	}

	Set<StateMachineStartAction<S, E>> getStartActions() {
		return startActions;
	}

	Set<StateMachineStopAction<S, E>> getStopActions() {
		return stopActions;
	}

}
