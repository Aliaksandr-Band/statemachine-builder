package alex.band.statemachine.state;

import alex.band.statemachine.Rollbackable;
import alex.band.statemachine.StateMachineDetails;

/**
 * Action associated with entering a {@link State}.
 *
 * @author Aliaksandr Bandarchyk
 */
public interface StateEnterAction<S, E> extends Rollbackable<S, E> {

	/**
	 * Action executed when entering the state.
	 */
	void execute(StateMachineDetails<S, E> context);

}
