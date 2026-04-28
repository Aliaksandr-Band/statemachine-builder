package alex.band.statemachine.state;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.transition.Transition;

/**
 * Implementation of {@link State}.
 *
 * @author Aliaksandr Bandarchyk
 */
public class StateImpl<S, E> implements State<S, E> {

	private static final Logger LOGGER = Logger.getLogger(StateImpl.class.getName());

	private S stateId;
	private Set<StateEnterAction<S, E>> enterActions = new LinkedHashSet<>();
	private Set<StateExitAction<S, E>> exitActions = new LinkedHashSet<>();
	private Map<E, Set<Transition<S, E>>> transitions = new HashMap<>();

	public StateImpl(S stateId) {
		this.stateId = stateId;
	}

	@Override
	public Optional<Transition<S, E>> getSuitableTransition(StateMachineMessage<E> message, StateMachineDetails<S, E> context) {
		if (transitions.get(message.getEvent()) == null || transitions.get(message.getEvent()).isEmpty()) {
			return Optional.empty();
		}

		for (Transition<S, E> transition: transitions.get(message.getEvent())) {
			boolean guardPassed;
			try {
				guardPassed = !transition.getGuard().isPresent()
					|| transition.getGuard().get().evaluate(message, context);
			} catch (Exception e) {
				// Guard evaluation failed - treat as guard not passed and continue checking other transitions
				LOGGER.log(Level.WARNING, "Guard evaluation failed for transition in state " + stateId + ", treating as guard not passed", e);
				guardPassed = false;
			}
			if (guardPassed) {
				return Optional.of(transition);
			}
		}

		return Optional.empty();
	}

	@Override
	public Set<StateEnterAction<S, E>> getEnterActions(StateMachineDetails<S, E> context) {
		return Collections.unmodifiableSet(enterActions);
	}

	@Override
	public Set<StateExitAction<S, E>> getExitActions(StateMachineDetails<S, E> context) {
		return Collections.unmodifiableSet(exitActions);
	}

	@Override
	public S getId() {
		return stateId;
	}


	@Override
	public String toString() {
		return "StateImpl [stateId=" + stateId + "]";
	}

	public void addEnterActions(LinkedHashSet<StateEnterAction<S, E>> enterActions) {
		this.enterActions.addAll(enterActions);
	}

	public void addEnterAction(StateEnterAction<S, E> enterAction) {
		this.enterActions.add(enterAction);
	}

	public void addExitActions(LinkedHashSet<StateExitAction<S, E>> exitActions) {
		this.exitActions.addAll(exitActions);
	}

	public void addExitAction(StateExitAction<S, E> exitAction) {
		this.exitActions.add(exitAction);
	}

	public void addTransition(Transition<S, E> transition) {
		if (!transitions.containsKey(transition.getEvent())) {
			transitions.put(transition.getEvent(), new LinkedHashSet<>());
		}
		transitions.get(transition.getEvent()).add(transition);
	}

}
