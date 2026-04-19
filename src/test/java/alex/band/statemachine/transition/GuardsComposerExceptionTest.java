package alex.band.statemachine.transition;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.message.StateMachineMessageImpl;

@ExtendWith(MockitoExtension.class)
class GuardsComposerExceptionTest {

	private static final String EVENT = "TestEvent";

	@Mock
	private Guard<String, String> guard1;
	@Mock
	private Guard<String, String> guard2;
	@Mock
	private Guard<String, String> guard3;

	private final StateMachineMessage<String> message = new StateMachineMessageImpl<>(EVENT);
	private final StateMachineDetails<String, String> context = mock(StateMachineDetails.class);

	@Test
	void considerAll_whenGuardThrowsException_shouldReturnFalse() {
		// Arrange
		when(guard1.evaluate(message, context)).thenReturn(true);
		when(guard2.evaluate(message, context)).thenThrow(new RuntimeException("Guard2 failed"));
		
		// Act
		Guard<String, String> composed = GuardsComposer.considerAll(guard1, guard2, guard3);
		boolean result = composed.evaluate(message, context);
		
		// Assert
		assertFalse(result);
		
		// Verify that guards were evaluated until the exception
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, never()).evaluate(message, context);
	}

	@Test
	void considerAll_whenFirstGuardThrowsException_shouldReturnFalse() {
		// Arrange
		when(guard1.evaluate(message, context)).thenThrow(new RuntimeException("Guard1 failed"));
		
		// Act
		Guard<String, String> composed = GuardsComposer.considerAll(guard1, guard2, guard3);
		boolean result = composed.evaluate(message, context);
		
		// Assert
		assertFalse(result);
		
		// Verify that only first guard was evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, never()).evaluate(message, context);
		verify(guard3, never()).evaluate(message, context);
	}

	@Test
	void considerAll_whenAllGuardsReturnTrue_shouldReturnTrue() {
		// Arrange
		when(guard1.evaluate(message, context)).thenReturn(true);
		when(guard2.evaluate(message, context)).thenReturn(true);
		when(guard3.evaluate(message, context)).thenReturn(true);
		
		// Act
		Guard<String, String> composed = GuardsComposer.considerAll(guard1, guard2, guard3);
		boolean result = composed.evaluate(message, context);
		
		// Assert
		assertTrue(result);
		
		// Verify that all guards were evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, times(1)).evaluate(message, context);
	}

	@Test
	void considerAll_whenSecondGuardReturnsFalse_shouldReturnFalseWithoutEvaluatingThird() {
		// Arrange
		when(guard1.evaluate(message, context)).thenReturn(true);
		when(guard2.evaluate(message, context)).thenReturn(false);
		
		// Act
		Guard<String, String> composed = GuardsComposer.considerAll(guard1, guard2, guard3);
		boolean result = composed.evaluate(message, context);
		
		// Assert
		assertFalse(result);
		
		// Verify that only first two guards were evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, never()).evaluate(message, context);
	}

	@Test
	void considerAny_whenGuardThrowsException_shouldContinueCheckingOtherGuards() {
		// Arrange
		when(guard1.evaluate(message, context)).thenReturn(false);
		when(guard2.evaluate(message, context)).thenThrow(new RuntimeException("Guard2 failed"));
		when(guard3.evaluate(message, context)).thenReturn(true);
		
		// Act
		Guard<String, String> composed = GuardsComposer.considerAny(guard1, guard2, guard3);
		boolean result = composed.evaluate(message, context);
		
		// Assert
		assertTrue(result);
		
		// Verify that all guards were evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, times(1)).evaluate(message, context);
	}

	@Test
	void considerAny_whenAllGuardsThrowException_shouldReturnFalse() {
		// Arrange
		when(guard1.evaluate(message, context)).thenThrow(new RuntimeException("Guard1 failed"));
		when(guard2.evaluate(message, context)).thenThrow(new RuntimeException("Guard2 failed"));
		when(guard3.evaluate(message, context)).thenThrow(new RuntimeException("Guard3 failed"));
		
		// Act
		Guard<String, String> composed = GuardsComposer.considerAny(guard1, guard2, guard3);
		boolean result = composed.evaluate(message, context);
		
		// Assert
		assertFalse(result);
		
		// Verify that all guards were evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, times(1)).evaluate(message, context);
	}

	@Test
	void considerAny_whenFirstGuardReturnsTrue_shouldReturnTrueWithoutEvaluatingOthers() {
		// Arrange
		when(guard1.evaluate(message, context)).thenReturn(true);
		
		// Act
		Guard<String, String> composed = GuardsComposer.considerAny(guard1, guard2, guard3);
		boolean result = composed.evaluate(message, context);
		
		// Assert
		assertTrue(result);
		
		// Verify that only first guard was evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, never()).evaluate(message, context);
		verify(guard3, never()).evaluate(message, context);
	}

	@Test
	void considerAny_whenSecondGuardThrowsAndThirdReturnsTrue_shouldReturnTrue() {
		// Arrange
		when(guard1.evaluate(message, context)).thenReturn(false);
		when(guard2.evaluate(message, context)).thenThrow(new RuntimeException("Guard2 failed"));
		when(guard3.evaluate(message, context)).thenReturn(true);
		
		// Act
		Guard<String, String> composed = GuardsComposer.considerAny(guard1, guard2, guard3);
		boolean result = composed.evaluate(message, context);
		
		// Assert
		assertTrue(result);
		
		// Verify that all guards were evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, times(1)).evaluate(message, context);
	}

	@Test
	void considerAny_whenAllGuardsReturnFalse_shouldReturnFalse() {
		// Arrange
		when(guard1.evaluate(message, context)).thenReturn(false);
		when(guard2.evaluate(message, context)).thenReturn(false);
		when(guard3.evaluate(message, context)).thenReturn(false);
		
		// Act
		Guard<String, String> composed = GuardsComposer.considerAny(guard1, guard2, guard3);
		boolean result = composed.evaluate(message, context);
		
		// Assert
		assertFalse(result);
		
		// Verify that all guards were evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, times(1)).evaluate(message, context);
	}
}