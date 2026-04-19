package alex.band.statemachine.transition;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.NOPLogger;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;

/**
 * @author Aliaksandr Bandarchyk
 */
public class GuardsComposer {

	private static final Logger logger = LoggerFactory.getLogger(GuardsComposer.class);

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
			return Arrays.stream(guards)
				.allMatch(guard -> {
					try {
						return guard.evaluate(message, context);
					} catch (Exception e) {
						String errorMsg = "Guard evaluation failed in considerAll: " + e.getMessage();
						if (logger instanceof NOPLogger) {
							System.err.println("[GuardsComposer] " + errorMsg);
						} else {
							logger.warn(errorMsg);
						}
						return false;
					}
				});
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
			return Arrays.stream(guards)
				.anyMatch(guard -> {
					try {
						return guard.evaluate(message, context);
					} catch (Exception e) {
						String errorMsg = "Guard evaluation failed in considerAny: " + e.getMessage();
						if (logger instanceof NOPLogger) {
							System.err.println("[GuardsComposer] " + errorMsg);
						} else {
							logger.warn(errorMsg);
						}
						return false;
					}
				});
		}

	}
}
