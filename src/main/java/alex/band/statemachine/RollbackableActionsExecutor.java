package alex.band.statemachine;

public interface RollbackableActionsExecutor<S, E> {

	void collect(Rollbackable<S, E> action);

	void rollback(StateMachineDetails<S, E> context);

	void clear();

}
