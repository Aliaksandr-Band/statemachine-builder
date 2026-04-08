package alex.band.statemachine.builder.impl;

import java.util.Set;

import alex.band.statemachine.builder.InternalTransitionConfigurer;
import alex.band.statemachine.transition.AsyncAction;
import alex.band.statemachine.transition.Guard;
import alex.band.statemachine.transition.Transition;
import alex.band.statemachine.transition.TransitionAction;
import alex.band.statemachine.transition.TransitionImpl;

/**
 * Implementation of {@link InternalTransitionConfigurer}.
 *
 * @author Aliaksandr Bandarchyk
 */
public class InternalTransitionConfigurerImpl<S, E> implements InternalTransitionConfigurer<S, E> {

	private TransitionImpl<S, E> transition;

	public InternalTransitionConfigurerImpl(S state, boolean external) {
		transition = new TransitionImpl<>();
		transition.setExternal(external);
		transition.setSource(state);
	}

	@Override
	public InternalTransitionConfigurer<S, E> by(E event) {
		transition.setEvent(event);
		return this;
	}

	@Override
	public InternalTransitionConfigurer<S, E> guardedBy(Guard<S, E> guard) {
		transition.setGuard(guard);
		return this;
	}

	@Override
	public InternalTransitionConfigurer<S, E> withAction(TransitionAction<S, E> action) {
		transition.addAction(action);
		return this;
	}

	@Override
	public InternalTransitionConfigurer<S, E> withActions(Set<TransitionAction<S, E>> actions) {
		transition.addActions(actions);
		return this;
	}

	@Override
	public InternalTransitionConfigurer<S, E> withAsyncAction(AsyncAction<S, E> asyncAction) {
		transition.addAsyncAction(asyncAction);
		return this;
	}

	@Override
	public InternalTransitionConfigurer<S, E> withAsyncActions(Set<AsyncAction<S, E>> asyncActions) {
		transition.addAsyncActions(asyncActions);
		return this;
	}

	Transition<S, E> getTransition() {
		return transition;
	}

}
