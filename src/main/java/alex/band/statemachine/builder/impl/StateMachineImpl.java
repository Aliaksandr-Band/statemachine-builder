package alex.band.statemachine.builder.impl;

import static alex.band.statemachine.util.Asserts.checkState;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

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
	private Map<S, State<S, E>> finalStates;
	private volatile State<S, E> currentState;
	private Map<S, State<S, E>> states;

	private Set<StateMachineStartAction<S, E>> startActions;
	private Set<StateMachineStopAction<S, E>> stopActions;

	private StateMachineContext context;
	private volatile Mode mode;
	private Consumer<Throwable> exceptionHandler = (t) -> {
	};


	@Override
	protected void doStart() {
		checkState(mode == Mode.READY, "Statemachine is already running or stopped.");

		for (StateMachineStartAction<S, E> action: startActions) {
			action.onStart(this);
		}

		mode = Mode.RUNNING;
		currentState = initialState;
		currentState.onEnter(this);
	}

	@Override
	protected void doStop() {
		checkState(mode == Mode.RUNNING, "Statemachine is not running.");
		currentState.onExit(this);
		mode = Mode.STOPPED;

		for (StateMachineStopAction<S, E> action: stopActions) {
			action.onStop(this);
		}
	}

	@Override
	protected void doReset() {
		checkState(mode != Mode.RUNNING, "Statemachine cannot be reset while running.");
		context.clear();
		currentState = null;
		mode = Mode.READY;
	}

	@Override
	protected boolean doAccept(StateMachineMessage<E> message) {
		if (!isRunning()) {
			return false;
		}

		if (message == null) {
			notifyEventNotAccepted(message);
			return false;
		}

		return processMessage(message);

	}


	/**
	 * Processes a single message by executing the transition pipeline:
	 * exit current state → transition actions → enter new state → async actions.
	 *
	 * <p><b>Atomicity guarantee:</b> If any step throws an exception,
	 * {@code currentState} is rolled back to the previous state before rethrowing.
	 * The state machine never remains in an inconsistent state.
	 *
	 * <p><b>Side effects are NOT rolled back:</b> Actions executed before the failure
	 * (e.g. {@code onExit}, transition actions) have already produced their side effects.
	 * The caller is responsible for compensating those actions if needed.
	 *
	 * @param message the message to process
	 * @return {@code true} if a matching transition was found and executed
	 */
	private boolean processMessage(StateMachineMessage<E> message) {

		Optional<Transition<S, E>> transition = currentState.getSuitableTransition(message, this);
		if (transition.isPresent()) {

			State<S, E> previousState = currentState;
			try {
				doCurrentStateExit(transition);
				executeTransitionActions(message, transition.get().getActions());
				doNewStateEnter(transition, message);
			} catch (Exception e) {
				currentState = previousState;
				throw e;
			}
			return true;
		}

		notifyEventNotAccepted(message);
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

	private void doNewStateEnter(Optional<Transition<S, E>> transition, StateMachineMessage<E> message) {
		if (transition.get().isExternal()) {
			State<S, E> previousState = currentState;
			currentState = states.get(transition.get().getTarget().get());
			currentState.onEnter(this);
			notifyStateChanged(message, previousState);
		}
		if (finalStates.containsKey(currentState.getId())) {
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

	void setInitialState(State<S, E> initialState) {
		this.initialState = initialState;
	}

	void setFinalStates(Map<S, State<S, E>> finalStates) {
		this.finalStates = finalStates;
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

	void setReady() {
		mode = Mode.READY;
	}

	@Override
	public boolean isRunning() {
		return Mode.RUNNING == mode;
	}

	@Override
	public boolean isReady() {
		return Mode.READY == mode;
	}

	@Override
	public boolean isStopped() {
		return Mode.STOPPED == mode;
	}

	@Override
	public boolean isFault() {
		return Mode.FAULT == mode;
	}

	@Override
	public void registerExceptionHandler(Consumer<Throwable> exceptionsHandler) {
		this.exceptionHandler = exceptionsHandler;
	}

	@Override
	public void unregisterExceptionHandler() {
		this.exceptionHandler = (t) -> {
		};
	}

}
