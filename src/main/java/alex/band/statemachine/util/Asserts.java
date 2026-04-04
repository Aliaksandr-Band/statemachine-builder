package alex.band.statemachine.util;

/**
 * Minimal utility class for state validation, replacing Google Guava's Preconditions.
 *
 * <p>Provides {@code checkState} methods that throw {@link IllegalStateException}
 * when the given condition is false.
 */
public final class Asserts {

	private Asserts() {
		// utility class
	}

	/**
	 * Validates that the given condition is true.
	 *
	 * @param expression the condition to check
	 * @throws IllegalStateException if expression is false
	 */
	public static void checkState(boolean expression) {
		if (!expression) {
			throw new IllegalStateException();
		}
	}

	/**
	 * Validates that the given condition is true, with a detail message.
	 *
	 * @param expression the condition to check
	 * @param message the detail message
	 * @throws IllegalStateException if expression is false
	 */
	public static void checkState(boolean expression, String message) {
		if (!expression) {
			throw new IllegalStateException(message);
		}
	}

	/**
	 * Validates that the given condition is true, with a formatted detail message.
	 *
	 * @param expression the condition to check
	 * @param messageTemplate the message template using {@link String#format} syntax
	 * @param args the arguments referenced by the format specifiers
	 * @throws IllegalStateException if expression is false
	 */
	public static void checkState(boolean expression, String messageTemplate, Object... args) {
		if (!expression) {
			throw new IllegalStateException(String.format(messageTemplate, args));
		}
	}
}
