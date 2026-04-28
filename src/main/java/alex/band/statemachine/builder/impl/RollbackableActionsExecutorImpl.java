package alex.band.statemachine.builder.impl;

import java.util.ArrayDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import alex.band.statemachine.Rollbackable;
import alex.band.statemachine.RollbackableActionsExecutor;
import alex.band.statemachine.StateMachineDetails;

public class RollbackableActionsExecutorImpl<S, E> implements RollbackableActionsExecutor<S, E> {

	private static final Logger LOGGER = Logger.getLogger(RollbackableActionsExecutorImpl.class.getName());


	private ArrayDeque<Rollbackable<S, E>> actions = new ArrayDeque<>();

	@Override
	public synchronized void collect(Rollbackable<S, E> action) {
		actions.push(action);
	}

	@Override
	public synchronized void rollback(StateMachineDetails<S, E> context) {

		while (!actions.isEmpty()) {
			Rollbackable<S, E> action = actions.pop();
			try {
				action.rollback(context);

			} catch (Throwable th) {
				LOGGER.log(Level.SEVERE, "Failed to rollback action " + String.valueOf(action));
			}
		}
	}

	@Override
	public synchronized void clear() {
		actions.clear();
	}

}
