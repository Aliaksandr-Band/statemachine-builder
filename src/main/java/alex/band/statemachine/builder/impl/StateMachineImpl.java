package alex.band.statemachine.builder.impl;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;

import static alex.band.statemachine.util.Asserts.checkState;

import alex.band.statemachine.ListenableStateMachine;
import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.StateMachineStopAction;
import alex.band.statemachine.context.StateMachineContext;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.state.State;
import alex.band.statemachine.transition.Transition;
import alex.band.statemachine.transition.TransitionAction;

/**
 * Base implementation of {@link StateMachine}.
 *
 * <p>Implements the core concepts described in <a href="https://en.wikipedia.org/wiki/UML_state_machine">UML StateMachine</a>. Namely:
 * <ul><li>Events</li>
 * <li>States</li>
 * <li>Extended States</li>
 * <li>Guard conditions</li>
 * <li>Actions</li>
 * <li>Transitions</li></ul>
 *
 * <p>Extensions to the traditional model include:
 * <ul><li>Entry and exit actions</li>
 * <li>Internal transitions</li>
 * <li>Events deferral</li></ul>
 *
 * @author Aliaksandr Bandarchyk
 */
public class StateMachineImpl<S, E> extends ListenableStateMachine<S, E> {

	private State<S, E> initialState;
	private State<S, E> finalState;
	private State<S, E> currentState;
	private Map<S, State<S, E>> states;

	private Set<StateMachineStartAction<S, E>> startActions;
	private Set<StateMachineStopAction<S, E>> stopActions;

	private boolean running;
	private StateMachineContext context;

	private Queue<StateMachineMessage<E>> deferredQueue = new ArrayDeque<>();

	@Override
	protected void doStart() {
		checkState(!running, "Statemachine is already running.");

		deferredQueue.clear();
		for (StateMachineStartAction<S, E> action: startActions) {
			action.onStart(this);
		}

		running = true;
		currentState = initialState;
		currentState.onEnter(this);
	}

	@Override
	protected void doStop() {
		currentState.onExit(this);
		running = false;

		for (StateMachineStopAction<S, E> action: stopActions) {
			action.onStop(this);
		}
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	@Override
	protected boolean doAccept(StateMachineMessage<E> message) {
		checkState(running, "Statemachine is not running yet.");

		if (message == null) {
			return false;
		}

		if (currentState.canBeDeferred(message)) {
			deferredQueue.offer(message);
			return true;
		}

		boolean messageAccepted = processMessage(message);

		processDeferredQueue();

		return messageAccepted;
	}

	private void processDeferredQueue() {
		while (!deferredQueue.isEmpty() && !currentState.equals(finalState)) {
			StateMachineMessage<E> deferredMessage = deferredQueue.peek();
			if (!currentState.canBeDeferred(deferredMessage)) {
				processMessage(deferredMessage);
				deferredQueue.poll();
			} else {
				break;
			}
		}
	}

	private boolean processMessage(StateMachineMessage<E> message) {

		Optional<Transition<S, E>> transition = currentState.getSuitableTransition(message, this);
		if (transition.isPresent()) {

			doCurrentStateExit(transition);
			executeTransitionActions(message, transition.get().getActions());
			doNewStateEnter(transition);
			return true;
		}

		return false;
	}

	private void doCurrentStateExit(Optional<Transition<S, E>> transition) {
		if (transition.get().isExternal()) {
			currentState.onExit(this);
		}
	}

	private void executeTransitionActions(StateMachineMessage<E> message, Set<TransitionAction<S, E>> actions) {
		for (TransitionAction<S, E> action:actions) {
			action.execute(message, this);
		}
	}

	private void doNewStateEnter(Optional<Transition<S, E>> transition) {
		if (transition.get().isExternal()) {
			currentState = states.get(transition.get().getTarget().get());
			currentState.onEnter(this);
		}
		if (finalState.equals(currentState)) {
			stop();
		}
	}

	@Override
	public State<S, E> getCurrentState() {
		return currentState;
	}

	@Override
	public StateMachineContext getContext() {
		return context;
	}

	boolean hasDeferredMessages() {
		return !deferredQueue.isEmpty();
	}

	int getDeferredQueueSize() {
		return deferredQueue.size();
	}

	void setInitialState(State<S, E> initialState) {
		this.initialState = initialState;
	}

	void setFinalState(State<S, E> finalState) {
		this.finalState = finalState;
	}

	void setStates(Map<S, State<S, E>> states) {
		this.states = states;
	}

	void setContext(StateMachineContext context) {
		this.context = context;
	}

	void setStartActions(Set<StateMachineStartAction<S, E>> startActions) {
		this.startActions = startActions;
	}

	void setStopActions(Set<StateMachineStopAction<S, E>> stopActions) {
		this.stopActions = stopActions;
	}

}
