package alex.band.statemachine;

public interface Rollbackable<S, E> {

	void rollback(StateMachineDetails<S, E> context);

}
