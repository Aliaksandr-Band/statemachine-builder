package alex.band.statemachine.state;

import alex.band.statemachine.Rollbackable;
import alex.band.statemachine.StateMachineDetails;

/**
 * Actions associated with exiting a {@link State}.
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateExitAction<S, E> extends Rollbackable<S, E> {

	/**
	 * Action executed when exiting the state.
	 */
	void execute(StateMachineDetails<S, E> context);

}
