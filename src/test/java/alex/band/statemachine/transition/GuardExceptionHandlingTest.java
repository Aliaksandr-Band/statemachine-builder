package alex.band.statemachine.transition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import alex.band.statemachine.StateMachine;
import alex.band.statemachine.StateMachineDetails;
import alex.band.statemachine.builder.StateMachineBuilder;
import alex.band.statemachine.builder.impl.StateMachineBuilderImpl;
import alex.band.statemachine.builder.impl.StateMachineImpl;
import alex.band.statemachine.message.StateMachineMessage;
import alex.band.statemachine.message.StateMachineMessageImpl;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GuardExceptionHandlingTest {

    private static final String S1 = "S1";
    private static final String S2 = "S2";
    private static final String S3 = "S3";
    private static final String E1 = "E1";
    private static final String E2 = "E2";

    @Mock
    private Guard<String, String> trueGuard;
    @Mock
    private Guard<String, String> falseGuard;
    @Mock
    private Guard<String, String> exceptionGuard;
    @Mock
    private StateMachineDetails<String, String> context;

    private final StateMachineMessage<String> message = new StateMachineMessageImpl<>(E1);

    @BeforeEach
    void setUp() {
        // Configure guards to work with any message - same as in existing tests
        when(trueGuard.evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class))).thenReturn(true);
        when(falseGuard.evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class))).thenReturn(false);
        doThrow(new RuntimeException("Guard exception"))
            .when(exceptionGuard)
            .evaluate(isA(StateMachineMessage.class), isA(StateMachineDetails.class));
    }

    @Test
    void considerAllGuard_shouldReturnFalseWhenGuardThrowsException() {
        Guard<String, String> composed = GuardsComposer.considerAll(trueGuard, exceptionGuard);
        
        boolean result = composed.evaluate(message, context);
        
        assertFalse(result);
    }

    @Test
    void considerAllGuard_shouldEvaluateAllGuardsAndReturnFalseWhenAnyThrowsException() {
        Guard<String, String> composed = GuardsComposer.considerAll(trueGuard, exceptionGuard, falseGuard);
        
        boolean result = composed.evaluate(message, context);
        
        assertFalse(result);
    }

    @Test
    void considerAnyGuard_shouldContinueToNextGuardWhenCurrentThrowsException() {
        Guard<String, String> composed = GuardsComposer.considerAny(exceptionGuard, trueGuard);
        
        boolean result = composed.evaluate(message, context);
        
        assertTrue(result);
    }

    @Test
    void considerAnyGuard_shouldReturnFalseWhenAllGuardsThrowException() {
        Guard<String, String> composed = GuardsComposer.considerAny(exceptionGuard, exceptionGuard);
        
        boolean result = composed.evaluate(message, context);
        
        assertFalse(result);
    }
    
    @Test
    void simpleGuard_shouldWorkNormally() {
        // This test should work to verify our basic setup is correct
        boolean trueResult = trueGuard.evaluate(message, context);
        boolean falseResult = falseGuard.evaluate(message, context);
        
        assertTrue(trueResult);
        assertFalse(falseResult);
    }

    @Test
    void stateMachine_shouldWorkNormallyAfterGuardException() {
        StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
        
        builder.defineState(S1).asInitial();
        builder.defineState(S2).asFinal();
        
        // Create transition that should work normally (not with exception guard)
        builder.defineExternalTransitionFor(S1).to(S2).by(E2).guardedBy(trueGuard);
        
        StateMachine<String, String> stateMachine = builder.build();
        
        stateMachine.start();
        assertTrue(stateMachine.isRunning());
        
        // E2 should work normally
        boolean result = stateMachine.accept(E2);
        assertTrue(result);
        assertEquals(S2, stateMachine.getCurrentState().getId());
        assertFalse(stateMachine.isRunning()); // Machine should stop because S2 is final
    }

    @Test
    void stateMachine_shouldNotTransitionWhenAllGuardsForEventThrowException() {
        StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
        
        builder.defineState(S1).asInitial();
        builder.defineState(S2).asFinal();
        
        // All transitions for E1 have guards that throw exception
        builder.defineExternalTransitionFor(S1).to(S2).by(E1).guardedBy(exceptionGuard);
        
        StateMachine<String, String> stateMachine = builder.build();
        
        stateMachine.start();
        boolean result = stateMachine.accept(E1);
        
        assertFalse(result);
        assertEquals(S1, stateMachine.getCurrentState().getId());
    }

    @Test
    void stateMachine_shouldProcessOtherEventsWhenGuardThrowsException() {
        StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();
        
        builder.defineState(S1).asInitial();
        builder.defineState(S2).asFinal();
        
        // Transition for E1 has guard that throws exception
        builder.defineExternalTransitionFor(S1).to(S2).by(E1).guardedBy(exceptionGuard);
        // Transition for E2 should work normally
        builder.defineExternalTransitionFor(S1).to(S2).by(E2).guardedBy(trueGuard);
        
        StateMachine<String, String> stateMachine = builder.build();
        
        stateMachine.start();
        
        // Check that machine is running
        assertTrue(stateMachine.isRunning());
        
        // E1 should be rejected (guard throws exception)
        boolean result1 = stateMachine.accept(E1);
        assertFalse(result1);
        assertEquals(S1, stateMachine.getCurrentState().getId());
        assertTrue(stateMachine.isRunning()); // Machine should still be running
        
        // E2 should be accepted
        boolean result2 = stateMachine.accept(E2);
        assertTrue(result2);
        assertEquals(S2, stateMachine.getCurrentState().getId());
        // Machine should not be running because S2 is final state
        assertFalse(stateMachine.isRunning());
    }
}