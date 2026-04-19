package alex.band.statemachine.transition;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;

/**
 * @author Aliaksandr Bandarchyk
 */
public class GuardsComposer {
	
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
		
		private static final Logger logger = LoggerFactory.getLogger(ConsiderAllGuard.class);
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
					logger.error("Guard evaluation failed in ConsiderAllGuard: {}", e.getMessage(), e);
					return false;
				}
			}
			return true;
		}
	}

	private static class ConsiderAnyGuard<S, E> implements Guard<S, E> {

		private static final Logger logger = LoggerFactory.getLogger(ConsiderAnyGuard.class);
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
					logger.error("Guard evaluation failed in ConsiderAnyGuard: {}", e.getMessage(), e);
					// Continue to next guard
				}
			}
			return false;
		}

	}
}
