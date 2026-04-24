package alex.band.statemachine;

import alex.band.statemachine.context.StateMachineContext;
import alex.band.statemachine.state.State;

/**
 *
 * @param <S> the type of the state identifier
 * @param <E> the type of the event identifier
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateMachineDetails<S, E> {

	enum Mode {
		READY, RUNNING, STOPPED
	};

	boolean isReady();

	boolean isRunning();

	boolean isStopped();

	State<S, E> getCurrentState();

	StateMachineContext getContext();

}
