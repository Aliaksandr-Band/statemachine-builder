package alex.band.statemachine.state;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.message.StateMachineMessageImpl;
import alex.band.statemachine.transition.Guard;
import alex.band.statemachine.transition.Transition;

@ExtendWith(MockitoExtension.class)
class StateImplGuardExceptionTest {

	private static final String STATE_ID = "TestState";
	private static final String EVENT = "TestEvent";

	@Mock
	private Guard<String, String> guard1;
	@Mock
	private Guard<String, String> guard2;
	@Mock
	private Guard<String, String> guard3;
	@Mock
	private Transition<String, String> transition1;
	@Mock
	private Transition<String, String> transition2;
	@Mock
	private Transition<String, String> transition3;
	
	private final StateMachineMessage<String> message = new StateMachineMessageImpl<>(EVENT);
	private final StateMachineDetails<String, String> context = mock(StateMachineDetails.class);

	@Test
	void getSuitableTransition_whenGuardThrowsException_shouldContinueCheckingOtherTransitions() {
		// Arrange
		StateImpl<String, String> state = new StateImpl<>(STATE_ID);
		
		// Setup transition1 with guard that throws exception
		when(transition1.getEvent()).thenReturn(EVENT);
		when(transition1.getGuard()).thenReturn(Optional.of(guard1));
		when(guard1.evaluate(message, context)).thenThrow(new RuntimeException("Guard1 failed"));
		
		// Setup transition2 with guard that returns false
		when(transition2.getEvent()).thenReturn(EVENT);
		when(transition2.getGuard()).thenReturn(Optional.of(guard2));
		when(guard2.evaluate(message, context)).thenReturn(false);
		
		// Setup transition3 with guard that returns true
		when(transition3.getEvent()).thenReturn(EVENT);
		when(transition3.getGuard()).thenReturn(Optional.of(guard3));
		when(guard3.evaluate(message, context)).thenReturn(true);
		
		state.addTransition(transition1);
		state.addTransition(transition2);
		state.addTransition(transition3);
		
		// Act
		Optional<Transition<String, String>> result = state.getSuitableTransition(message, context);
		
		// Assert
		assertTrue(result.isPresent());
		assertEquals(transition3, result.get());
		
		// Verify that all guards were evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, times(1)).evaluate(message, context);
	}

	@Test
	void getSuitableTransition_whenAllGuardsThrowException_shouldReturnEmpty() {
		// Arrange
		StateImpl<String, String> state = new StateImpl<>(STATE_ID);
		
		// Setup all transitions with guards that throw exceptions
		when(transition1.getEvent()).thenReturn(EVENT);
		when(transition1.getGuard()).thenReturn(Optional.of(guard1));
		when(guard1.evaluate(message, context)).thenThrow(new RuntimeException("Guard1 failed"));
		
		when(transition2.getEvent()).thenReturn(EVENT);
		when(transition2.getGuard()).thenReturn(Optional.of(guard2));
		when(guard2.evaluate(message, context)).thenThrow(new RuntimeException("Guard2 failed"));
		
		state.addTransition(transition1);
		state.addTransition(transition2);
		
		// Act
		Optional<Transition<String, String>> result = state.getSuitableTransition(message, context);
		
		// Assert
		assertFalse(result.isPresent());
		
		// Verify that all guards were evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
	}

	@Test
	void getSuitableTransition_whenFirstGuardThrowsAndSecondPasses_shouldReturnSecondTransition() {
		// Arrange
		StateImpl<String, String> state = new StateImpl<>(STATE_ID);
		
		// Setup transition1 with guard that throws exception
		when(transition1.getEvent()).thenReturn(EVENT);
		when(transition1.getGuard()).thenReturn(Optional.of(guard1));
		when(guard1.evaluate(message, context)).thenThrow(new RuntimeException("Guard1 failed"));
		
		// Setup transition2 with guard that returns true
		when(transition2.getEvent()).thenReturn(EVENT);
		when(transition2.getGuard()).thenReturn(Optional.of(guard2));
		when(guard2.evaluate(message, context)).thenReturn(true);
		
		state.addTransition(transition1);
		state.addTransition(transition2);
		
		// Act
		Optional<Transition<String, String>> result = state.getSuitableTransition(message, context);
		
		// Assert
		assertTrue(result.isPresent());
		assertEquals(transition2, result.get());
		
		// Verify that both guards were evaluated
		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
	}

	@Test
	void getSuitableTransition_whenTransitionWithoutGuard_shouldReturnTransition() {
		// Arrange
		StateImpl<String, String> state = new StateImpl<>(STATE_ID);
		
		// Setup transition without guard
		when(transition1.getEvent()).thenReturn(EVENT);
		when(transition1.getGuard()).thenReturn(Optional.empty());
		
		state.addTransition(transition1);
		
		// Act
		Optional<Transition<String, String>> result = state.getSuitableTransition(message, context);
		
		// Assert
		assertTrue(result.isPresent());
		assertEquals(transition1, result.get());
	}
}