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

	/**
	 * Validates that the given reference is not null.
	 *
	 * @param <T> the type of the reference
	 * @param reference the reference to check
	 * @return the non-null reference that was validated
	 * @throws NullPointerException if reference is null
	 */
	public static <T> T checkNotNull(T reference) {
		if (reference == null) {
			throw new NullPointerException();
		}
		return reference;
	}

	/**
	 * Validates that the given reference is not null, with a detail message.
	 *
	 * @param <T> the type of the reference
	 * @param reference the reference to check
	 * @param errorMessage the detail message
	 * @return the non-null reference that was validated
	 * @throws NullPointerException if reference is null
	 */
	public static <T> T checkNotNull(T reference, Object errorMessage) {
		if (reference == null) {
			throw new NullPointerException(String.valueOf(errorMessage));
		}
		return reference;
	}

	/**
	 * Validates that the given reference is not null, with a formatted detail message.
	 *
	 * @param <T> the type of the reference
	 * @param reference the reference to check
	 * @param messageTemplate the message template using {@link String#format} syntax
	 * @param args the arguments referenced by the format specifiers
	 * @return the non-null reference that was validated
	 * @throws NullPointerException if reference is null
	 */
	public static <T> T checkNotNull(T reference, String messageTemplate, Object... args) {
		if (reference == null) {
			throw new NullPointerException(String.format(messageTemplate, args));
		}
		return reference;
	}
}
