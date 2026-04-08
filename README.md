# Statemachine Builder

A lightweight, zero-dependency Java library for building finite state machines using a fluent builder API.

Instead of implementing the classic **State** pattern from GoF — which requires a separate class for each state and handler methods for every event — this library lets you declaratively define states, transitions, guards, and actions in a few lines of code:

```java
StateMachineBuilder<String, String> builder = new StateMachineBuilderImpl<>();

builder.defineState("Ready").asInitial();
builder.defineState("Processing");
builder.defineState("Stopped").asFinal();

builder.defineExternalTransitionFor("Ready")
       .to("Processing")
       .by("Start");

builder.defineExternalTransitionFor("Processing")
       .to("Stopped")
       .by("Stop")
       .withAction((msg, ctx) -> System.out.println("Stopped!"));

StateMachine<String, String> sm = builder.build();
sm.start();
sm.accept("Start");   // Ready → Processing
sm.accept("Stop");    // Processing → Stopped (auto-stops)
```

## Features

### Core
- **External transitions** — change state on an event (`S1 → S2`)
- **Internal transitions** — handle an event without leaving the current state (`S1 → S1`)
- **Guards** — conditional transitions via `Guard.evaluate()`; composable with `GuardsComposer.considerAll()` (AND) and `considerAny()` (OR)
- **Event deferral** — a state can defer events; they are queued and processed automatically when the machine enters a non-deferring state (FIFO, `ArrayDeque`)

### Actions
- **Entry / Exit actions** — `StateAction` with `onEnter()` and `onExit()` callbacks
- **Transition actions** — `TransitionAction.execute()` runs during a transition
- **Start / Stop actions** — machine-level lifecycle actions

### Observability
- **Lifecycle listeners** — `onStart`, `onStop`, `onStateChanged`, `onEventNotAccepted`
- **StateMachineListenerAdapter** — no-op base for selective override

### Extended State
- **Shared context** — `StateMachineContext` (key-value store) available to all actions and guards
- **Message payload** — `StateMachineMessage` carries an optional `Object` payload

### Safety
- **Build-time validation** — 10+ rules checked at `build()`: initial/final states defined, no transitions from final, all states reachable, no duplicates, etc.
- **Auto-stop** — the machine stops automatically when a final state is entered

### Technical
- **Java 17**
- **Zero production dependencies**
- **49 unit tests** (JUnit 5 + Mockito)

## Why not Spring Statemachine?

The library was inspired by [Spring Statemachine](https://projects.spring.io/spring-statemachine/) but aims to be:

| | Spring Statemachine | statemachine-builder |
|---|---|---|
| **Size** | Large, Spring ecosystem | Tiny, single JAR, no deps |
| **Frontend-friendly** | JVM/server only | Works in GWT, Android, etc. |
| **Configuration** | XML / annotations / Java DSL | Fluent Java builder only |
| **Feature set** | Full UML 2.5 + regions, persist, etc. | Core state machine concepts |

If you need a full-featured state machine with Spring integration — use Spring Statemachine. If you need a small, embeddable state machine with a clean builder API — this library is for you.

## Requirements & Build

- **JDK 17+**
- **Maven**

```bash
# Build and run tests
mvn clean install

# Compile only
mvn clean compile

# Build without tests
mvn install -DskipTests
```

## Roadmap

### Improve existing implementation

| Priority | Task |
|---|---|
| **P0** | ~~Short-circuit evaluation in `GuardsComposer`~~ |
| **P0** | ~~Deterministic transition ordering~~ |
| **P1** | ~~Thread safety~~ |
| **P1** | ~~Correct `onStateChanged` firing for deferred events~~ |
| **P1** | Split `StateAction` into `OnEnterAction` / `OnExitAction` |
| **P2** | Typed payload and context |
| **P2** | Remove internal cast to `StateImpl` |
| **P2** | `defineStates(Set)` returning a configurer |

### Add missing UML 2.5 features

| Phase | Features |
|---|---|
| **1** | Composite states, completion transitions, shallow history |
| **2** | Orthogonal regions, choice & junction pseudostates |
| **3** | Time events, change events, call events with return values |
| **4** | Deep history, fork/join, entry/exit points, do-activity, submachine states, terminate |

## License

[TODO: add license]
