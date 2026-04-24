package alex.band.statemachine.builder.impl;

import static alex.band.statemachine.util.Asserts.checkState;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineStartAction;
import alex.band.statemachine.StateMachineStopAction;
import alex.band.statemachine.builder.ExternalTransitionConfigurer;
import alex.band.statemachine.builder.InternalTransitionConfigurer;
import alex.band.statemachine.builder.StartStopActionsConfigurer;
import alex.band.statemachine.builder.StateMachineBuilder;
import alex.band.statemachine.builder.StatesConfigurer;
import alex.band.statemachine.context.StateMachineContext;
import alex.band.statemachine.context.StateMachineContextImpl;
import alex.band.statemachine.state.State;
import alex.band.statemachine.state.StateImpl;
import alex.band.statemachine.transition.Transition;

/**
 * Implementation of {@link StateMachineBuilder}.
 *
 * @author Aliaksandr Bandarchyk
 */
public class StateMachineBuilderImpl<S, E> implements StateMachineBuilder<S, E> {

	static final String FINAL_STATE_ALREADY_DEFINED = "Final State with equal ID already defined: %s";
	static final String INITIAL_STATE_ALREADY_DEFINED = "Initial State already defined. Defined State %s, new State %s";
	static final String STATE_ALREADY_DEFINED = "State with equal ID already defined: %s";
	static final String STATES_WITHOUT_OUTBOUND_TRANSITION = "There are States which don't have outbound transition: %s";
	static final String STATES_WITHOUT_INBOUND_TRANSITION = "There are States which don't have inbound transition: %s";
	static final String ILLEGAL_TRANSITION_FROM_FINAL_STATE = "Final State should not be used as source of Transition: %s";
	static final String EXTERNAL_TRANSITION_HAS_NO_TARGET_STATE = "External Transition doesn't have target State defined: %s";
	static final String UNKOWN_SOURCE_STATES_IN_TRANSITIONS = "Transitions have unkown source States: %s";
	static final String UNKOWN_TARGET_STATES_IN_TRANSITIONS = "Transitions have unkown target States: %s";
	static final String FINAL_STATE_IS_NOT_DEFINED = "Final State is not defined.";
	static final String INITIAL_STATE_IS_NOT_DEFINED = "Initial State is not defined.";
	static final String THERE_ARE_NO_STATES_DEFINED = "There are no States defined.";
	static final String ASYNC_ACTIONS_WITHOUT_EXECUTOR = "AsyncActions are defined but ExecutorService is not set.";

	private State<S, E> initialState;
	private Map<S, State<S, E>> finalStates = new HashMap<>();
	private Map<S, State<S, E>> states = new HashMap<>();
	private Map<S, Set<Transition<S, E>>> transitions = new HashMap<>();
	private Set<StateMachineStartAction<S, E>> startActions = new LinkedHashSet<>();
	private Set<StateMachineStopAction<S, E>> stopActions = new LinkedHashSet<>();
	private StateMachineContext context = new StateMachineContextImpl();

	@Override
	public StartStopActionsConfigurer<S, E> defineStartStopActions() {
		StartStopActionsConfigurerImpl<S, E> startStopConfigurer = new StartStopActionsConfigurerImpl<>();
		startActions = startStopConfigurer.getStartActions();
		stopActions = startStopConfigurer.getStopActions();
		return startStopConfigurer;
	}

	@Override
	public StatesConfigurer<S, E> defineState(S stateId) {
		StateImpl<S, E> state = new StateImpl<>(stateId);
		addState(state);

		return new StatesConfigurerImpl<>(this, state);
	}

	@Override
	public void defineStates(Set<S> states) {
		for (S state: states) {
			addState(new StateImpl<>(state));
		}
	}

	@Override
	public ExternalTransitionConfigurer<S, E> defineExternalTransitionFor(S sourceState) {
		ExternalTransitionConfigurerImpl<S, E> transitionConfigurer = new ExternalTransitionConfigurerImpl<>(sourceState, true);
		addTransition(sourceState, transitionConfigurer.getTransition());

		return transitionConfigurer;
	}

	@Override
	public InternalTransitionConfigurer<S, E> defineInternalTransitionFor(S sourceState) {
		InternalTransitionConfigurerImpl<S, E> transitionConfigurer = new InternalTransitionConfigurerImpl<>(sourceState, false);
		addTransition(sourceState, transitionConfigurer.getTransition());

		return transitionConfigurer;
	}

	@Override
	public void definedStateMachineContext(StateMachineContext conext) {
		this.context = conext;
	}

	@Override
	public StateMachine<S, E> build() {
		validateStates();
		validateTransitions();
		validateTopology();
		return createStateMachine();
	}

	private void validateStates() {
		checkState(!states.isEmpty(), THERE_ARE_NO_STATES_DEFINED);
		checkState(initialState != null, INITIAL_STATE_IS_NOT_DEFINED);
		checkState(!finalStates.isEmpty(), FINAL_STATE_IS_NOT_DEFINED);
	}

	private void validateTransitions() {
		Set<S> diff = difference(transitions.keySet(), states.keySet());
		checkState(diff.isEmpty(), UNKOWN_SOURCE_STATES_IN_TRANSITIONS, diff);

		Set<S> transitionsTargetStates = validateAndGetTargetStatesFromTransitions();
		diff = difference(transitionsTargetStates, states.keySet());
		checkState(diff.isEmpty(), UNKOWN_TARGET_STATES_IN_TRANSITIONS, diff);
	}

	private Set<S> validateAndGetTargetStatesFromTransitions() {
		Set<S> targetStates = new LinkedHashSet<>();
		for (Set<Transition<S, E>> transitionsBySource: transitions.values()) {
			for (Transition<S, E> transition: transitionsBySource) {

				checkState((transition.isExternal() == transition.getTarget().isPresent()),
						EXTERNAL_TRANSITION_HAS_NO_TARGET_STATE, transition);

				for (State<S, E> finalState : finalStates.values()) {
					checkState(!transition.getSource().equals(finalState.getId()), ILLEGAL_TRANSITION_FROM_FINAL_STATE,
							transition);
				}

				if (transition.getTarget().isPresent()) {
					targetStates.add(transition.getTarget().get());
				}
			}
		}
		return targetStates;
	}

	private void validateTopology() {
		Set<S> enteredStates = new LinkedHashSet<>(states.keySet());
		Set<S> exitedStates = new LinkedHashSet<>(states.keySet());
		enteredStates.remove(initialState.getId());
		exitedStates.removeAll(finalStates.keySet());

		for (Set<Transition<S, E>> stateTransitions: transitions.values()) {
			for (Transition<S, E> transition: stateTransitions) {
				excludeStatesOfExternalTransition(enteredStates, exitedStates, transition);
			}
		}

		checkState(enteredStates.isEmpty(), STATES_WITHOUT_INBOUND_TRANSITION, enteredStates);
		checkState(exitedStates.isEmpty(), STATES_WITHOUT_OUTBOUND_TRANSITION, exitedStates);
	}

	private Set<S> difference(Set<S> set1, Set<S> set2) {
		Set<S> result = new LinkedHashSet<>(set1);
		result.removeAll(set2);
		return result;
	}

	private void excludeStatesOfExternalTransition(Set<S> enteredStates, Set<S> exitedStates, Transition<S, E> transition) {
		if (transition.isExternal()) {
			exitedStates.remove(transition.getSource());
			if (transition.getTarget().isPresent()) {
				enteredStates.remove(transition.getTarget().get());
			}
		}
	}

	private StateMachine<S, E> createStateMachine() {
		connectTransitionsWithSourceStates();
		StateMachineImpl<S, E> stateMachine = new StateMachineImpl<>();
		stateMachine.setInitialState(initialState);
		stateMachine.setFinalStates(finalStates);
		stateMachine.setStates(states);
		stateMachine.setStartActions(startActions);
		stateMachine.setStopActions(stopActions);
		stateMachine.setContext(context);
		stateMachine.setReady();

		return stateMachine;
	}

	private void connectTransitionsWithSourceStates() {
		for (Set<Transition<S, E>> transitionsBySource: transitions.values()) {
			for (Transition<S, E> transition: transitionsBySource) {
				StateImpl<S, E> state = (StateImpl<S, E>) states.get(transition.getSource());
				state.addTransition(transition);
			}
		}
	}

	private void addTransition(S sourceState, Transition<S, E> transition) {
		if (!transitions.containsKey(sourceState)) {
			transitions.put(sourceState, new LinkedHashSet<>());
		}
		transitions.get(sourceState).add(transition);
	}

	private void addState(State<S, E> state) {
		checkState(!states.containsKey(state.getId()), STATE_ALREADY_DEFINED, state.getId());
		states.put(state.getId(), state);
	}

	void setInitialState(State<S, E> state) {
		checkState(initialState == null, INITIAL_STATE_ALREADY_DEFINED, initialState, state);
		initialState = state;
	}

	void setFinalState(State<S, E> state) {
		checkState(!finalStates.containsKey(state.getId()), FINAL_STATE_ALREADY_DEFINED, state);
		finalStates.put(state.getId(), state);
	}

}
