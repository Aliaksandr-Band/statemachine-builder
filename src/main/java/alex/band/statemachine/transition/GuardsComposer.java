package alex.band.statemachine.transition;

import java.util.logging.Level;
import java.util.logging.Logger;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;

/**
 * @author Aliaksandr Bandarchyk
 */
public class GuardsComposer {

	private static final Logger LOGGER = Logger.getLogger(GuardsComposer.class.getName());

	private GuardsComposer() {
	}
	
	@SafeVarargs
	public static <S, E> Guard<S, E> considerAll(Guard<S, E> ...guards) {
		return new ConsiderAllGuard<>(guards);
	}
	
	@SafeVarargs
	public static <S, E> Guard<S, E> considerAny(Guard<S, E> ...guards) {
		return new ConsiderAnyGuard<>(guards);
	}
	
	private static class ConsiderAllGuard<S, E> implements Guard<S, E> {
		
		private Guard<S, E>[] guards;
		
		@SafeVarargs
		ConsiderAllGuard(Guard<S, E> ...guards) {
			this.guards = guards;
		}

		@Override
		public boolean evaluate(StateMachineMessage<E> message, StateMachineDetails<S, E> context) {
			for (Guard<S, E> guard : guards) {
				try {
					if (!guard.evaluate(message, context)) {
						return false;
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Guard evaluation failed in ConsiderAllGuard, treating as false", e);
					return false;
				}
			}
			return true;
		}
	}

	private static class ConsiderAnyGuard<S, E> implements Guard<S, E> {

		private Guard<S, E>[] guards;
		
		@SafeVarargs
		ConsiderAnyGuard(Guard<S, E> ...guards) {
			this.guards = guards;
		}

		@Override
		public boolean evaluate(StateMachineMessage<E> message, StateMachineDetails<S, E> context) {
			for (Guard<S, E> guard : guards) {
				try {
					if (guard.evaluate(message, context)) {
						return true;
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Guard evaluation failed in ConsiderAnyGuard, treating as false", e);
					// Continue checking other guards
				}
			}
			return false;
		}

	}
}
