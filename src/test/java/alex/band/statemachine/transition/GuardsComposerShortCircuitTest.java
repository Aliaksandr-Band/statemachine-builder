package alex.band.statemachine.transition;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.message.StateMachineMessageImpl;

@ExtendWith(MockitoExtension.class)
class GuardsComposerShortCircuitTest {

	private static final String EVENT = "E1";

	@Mock
	private Guard<String, String> guard1;
	@Mock
	private Guard<String, String> guard2;
	@Mock
	private Guard<String, String> guard3;

	private final StateMachineMessage<String> message = new StateMachineMessageImpl<>(EVENT);
	private final StateMachineDetails<String, String> context = mock(StateMachineDetails.class);

	@Test
	void considerAll_shortCircuitsOnFirstFalse() {
		when(guard1.evaluate(message, context)).thenReturn(false);

		Guard<String, String> composed = GuardsComposer.considerAll(guard1, guard2, guard3);
		composed.evaluate(message, context);

		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, never()).evaluate(any(), any());
		verify(guard3, never()).evaluate(any(), any());
	}

	@Test
	void considerAll_evaluatesAllWhenAllTrue() {
		when(guard1.evaluate(message, context)).thenReturn(true);
		when(guard2.evaluate(message, context)).thenReturn(true);
		when(guard3.evaluate(message, context)).thenReturn(true);

		Guard<String, String> composed = GuardsComposer.considerAll(guard1, guard2, guard3);
		composed.evaluate(message, context);

		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, times(1)).evaluate(message, context);
	}

	@Test
	void considerAll_stopsAtSecondGuard() {
		when(guard1.evaluate(message, context)).thenReturn(true);
		when(guard2.evaluate(message, context)).thenReturn(false);

		Guard<String, String> composed = GuardsComposer.considerAll(guard1, guard2, guard3);
		composed.evaluate(message, context);

		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, never()).evaluate(any(), any());
	}

	@Test
	void considerAny_shortCircuitsOnFirstTrue() {
		when(guard1.evaluate(message, context)).thenReturn(true);

		Guard<String, String> composed = GuardsComposer.considerAny(guard1, guard2, guard3);
		composed.evaluate(message, context);

		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, never()).evaluate(any(), any());
		verify(guard3, never()).evaluate(any(), any());
	}

	@Test
	void considerAny_evaluatesAllWhenAllFalse() {
		when(guard1.evaluate(message, context)).thenReturn(false);
		when(guard2.evaluate(message, context)).thenReturn(false);
		when(guard3.evaluate(message, context)).thenReturn(false);

		Guard<String, String> composed = GuardsComposer.considerAny(guard1, guard2, guard3);
		composed.evaluate(message, context);

		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, times(1)).evaluate(message, context);
	}

	@Test
	void considerAny_stopsAtSecondGuard() {
		when(guard1.evaluate(message, context)).thenReturn(false);
		when(guard2.evaluate(message, context)).thenReturn(true);

		Guard<String, String> composed = GuardsComposer.considerAny(guard1, guard2, guard3);
		composed.evaluate(message, context);

		verify(guard1, times(1)).evaluate(message, context);
		verify(guard2, times(1)).evaluate(message, context);
		verify(guard3, never()).evaluate(any(), any());
	}
}
