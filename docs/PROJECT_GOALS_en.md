Here is the English version of the final conceptual description, structured exactly as the Russian version above, including the versioning strategy.

---

# Conceptual Specification: Lightweight Zero-Dependency State Machine Library

## 1. Philosophy and Key Characteristics

The library provides an implementation of a Finite State Machine (FSM) adhering to UML State Machine concepts. Core principles:

- **Zero dependencies** – Uses only the Java Standard Library (the `java.util.concurrent` package for asynchronous operations).
- **Declarative configuration** via a Builder API.
- **Guaranteed atomicity of synchronous transitions** (at the level of a single `sendMessage` call).
- **Clear separation** of synchronous logic, asynchronous logic, and the processor's lifecycle.
- **Predictable error handling** with user notifications via callbacks and no magical recovery mechanisms.

## 2. Configuration API (Builder)

The configuration is built using a fluent interface and includes the following elements:

| Element | Description |
| :--- | :--- |
| **Message Type** | A generic event `E` (typically a `String` or `Enum`) and a payload `P` of any type. |
| **States** | A set of states of type `S` (typically `String` or `Enum`). **One start state** and **at least one final state** must be specified. |
| **External Transitions** | A transition between different states triggered by an event `E`. May have a `guard` and an `action`. |
| **Internal Transitions** | A transition that does not change the state, triggered by an event `E`. May have a `guard` and an `action`. |
| **Guard (Condition)** | A predicate receiving the context and the event payload. The transition fires only if the guard returns `true`. If multiple transitions match, they are evaluated in declaration order (first match with a true guard wins). |
| **Actions** | User code executed synchronously: `exitActions` (when leaving a state), `transitionAction` (during the transition), `entryActions` (when entering a state). |
| **Asynchronous Actions** | `asyncActions` executed in a separate thread after the successful completion of the synchronous part of the transition. |
| **Context** | A thread-safe data store of type `Map<String, Object>` (implemented as `ConcurrentHashMap`), accessible in all actions and guards. |
| **Deferred Events** | Events that are not processed immediately in a specific state. Instead, they are placed in a shared queue and will be processed when the state changes. |

### 2.1 Configuration Validation Rules
When `build()` is called, the following checks are performed:
- At least one start state and one final state are defined.
- All transitions reference only states declared in the configuration.
- **Reachability:** All states (except the start state) are reachable from the start state. From any non-final state, there exists at least one path to a final state.
- If `asyncActions` are present in the configuration, an `ExecutorService` must be provided to the builder.

## 3. State Machine Processor Lifecycle

In addition to the business states (`S`), the state machine object itself can be in one of five meta-states that determine its readiness to process messages.

| State | Description | Allowed Operations |
| :--- | :--- | :--- |
| **READY** | Configuration is loaded; the machine is created or was reset via `reset()`. Waiting for `start()`. | `start()`, `reset()` |
| **RUNNING** | Active message processing. | `sendMessage()`, `pause()`, `stop()` |
| **PAUSED** | Temporary suspension. Calls to `sendMessage()` return `false` immediately. | `resume()`, `stop()` |
| **STOPPED** | Operation completed (a final state was reached or `stop()` was called). | `reset()` |
| **FAULTED** | **Critical:** Entered when an unhandled exception occurs in synchronous `exitActions` or `entryActions`. The machine is in an inconsistent state and can no longer process events. | Only `reset()` |

> **Note on FAULTED:** This state is introduced to maintain the invariant that "the machine cannot be stateless." Since a rollback is impossible after partial execution of `exitActions`, declaring the state as corrupted and requiring an explicit reset is the only safe recovery path.

## 4. Determinism and Error Handling Concepts

### 4.1 Synchronous Transition Sequence (Atomic Phase)
Every transition (external or internal) is executed within the synchronized `sendMessage` method in strict order:

```
Event (E, P) 
→ guard (if present) 
→ [exitActions]          // for external transitions only
→ [transitionAction] 
→ [entryActions]         // for external transitions only
→ State Mutation
```

**Exception Handling Rules for this Phase:**
- If an exception is thrown in **guard** or **transitionAction** (for any transition), or in an **internal action**, the transition is **completely aborted**, and the current state remains unchanged.
- If an exception is thrown in **exitActions** or **entryActions**, state integrity cannot be guaranteed. The machine transitions to the meta-state **FAULTED**.
- The library **does not perform automatic rollback** of actions. Responsibility for compensating side effects lies with the user code, utilizing the provided error callbacks.

### 4.2 Asynchronous Actions and Listeners
- **asyncActions** are submitted to the provided `ExecutorService` **after** the successful completion of the synchronous transition phase and run **in parallel** with the start of deferred event processing (see Section 5).
- Exceptions in `asyncActions` **do not affect** the state machine and do not interrupt other asynchronous tasks.
- Exceptions from `asyncActions` and listeners are caught and forwarded to the corresponding **error callbacks** (e.g., `onAsyncError`).

### 4.3 Listeners
- Listener notifications (`onStateChanged`, `onEventIgnored`, `onDeferred`, `onStart`, `onStop`) are dispatched **asynchronously** using the same `ExecutorService`.
- **The order of notification delivery is not guaranteed** (due to the use of a shared thread pool).
- Exceptions in listeners are suppressed and logged (if a logger is configured) but **do not affect** the machine's operation.

## 5. Deferred Events Semantics

### 5.1 Storage and Processing
- Deferred events are stored in a shared FIFO queue at the state machine instance level.
- An event is marked as `deferred` for a specific state in the configuration. If the machine is in that state and receives the event, it is added to the queue instead of being processed immediately.
- Queue processing is triggered **strictly after** the completion of the synchronous transition phase (`entryActions`) but **does not wait** for the completion of any `asyncActions` launched by that transition.

### 5.2 Queue Consumption Rules
1. Processing occurs one event at a time (while maintaining the external synchronization lock of `sendMessage`).
2. An event is pulled from the queue. If, in the current state, this event is **again** marked as `deferred`, it is placed back at the **end** of the queue.
3. A deferred event can trigger either an external transition (changing state) or an internal transition.
4. **User Warning:** An internal transition that consistently returns an event to the queue as `deferred` will result in an infinite loop unless a `guard` condition is used for single execution or the context is modified to break the cycle.

## 6. Additional Notes and Constraints

- **Synchronization:** Public management methods (`sendMessage`, `start`, `pause`, `resume`, `stop`, `reset`) are synchronized on the instance level to ensure atomicity in a multi-threaded environment.
- **ExecutorService Management:** The library **does not manage** the lifecycle of the provided `ExecutorService`. Calls to `stop()` or `reset()` prevent new tasks from being submitted, but already executing asynchronous tasks will run to completion.
- **Context:** While `ConcurrentHashMap` provides basic thread safety for put/get operations, users must ensure the atomicity of compound operations (e.g., `get` -> modify -> `put`) themselves.

---

## 7. Versioning Strategy and Functional Boundaries

To prevent the library from evolving into a heavyweight framework, a clear functional split is defined by version.

### 7.1 Version 1.0 (MVP – Minimal Viable Product)

**Goal:** Provide a strict, atomic synchronous state machine with zero external dependencies, suitable for single-threaded environments (including GWT-compatible code) and simple server applications.

**Scope:**
- [x] Builder API and topology validation.
- [x] External transitions (`guard`, `transitionAction`, `exit/entry`).
- [x] Internal transitions (no `exit/entry`).
- [x] Thread-safe context.
- [x] Lifecycle: `READY`, `RUNNING`, `STOPPED`.
- [x] `FAULTED` state for critical errors in `exit/entry`.
- [x] Error callbacks for synchronous actions.
- [ ] **Excluded:** Asynchronous listeners.
- [ ] **Excluded:** `asyncActions`.
- [ ] **Excluded:** Deferred events.
- [ ] **Excluded:** `PAUSED` lifecycle state.

**Rationale:**  
Version 1.0 must be impeccably reliable and simple. The absence of asynchrony and deferred queues eliminates the most complex race condition scenarios and allows the core engine to be hardened.

### 7.2 Version 2.0 (Target Full Specification)

**Goal:** Implement the full set of features described in this document while remaining lightweight and zero-dependency.

**Additions to 1.0:**
- [x] Support for `asyncActions` with user-provided `ExecutorService`.
- [x] Asynchronous lifecycle and transition listeners.
- [x] Full FIFO `deferred` event queue with semantics described in Section 5.
- [x] `PAUSED` state in the processor lifecycle.
- [x] Refined behavior guarantees regarding shutdown/reset with active asynchronous tasks.

### 7.3 Out of Scope (Anti-goals)
To preserve the identity of a "lightweight library," the following features **will never be implemented**:
- Hierarchical (nested) states.
- Orthogonal regions (parallel states).
- History pseudostates (deep/shallow).
- Built-in state persistence (database serialization).
- Automatic thread pool management.

---

This document serves as the definitive target specification for development. Any deviation from the rules described herein must be explicitly documented in the code or treated as a defect.
