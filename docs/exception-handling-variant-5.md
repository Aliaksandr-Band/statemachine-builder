# Обработка исключений в State Machine - Вариант 5 (Комбинированный подход)

## Проблема

Если действие (`StateAction` или `TransitionAction`) выбрасывает исключение:
- Предыдущие действия остаются выполненными (отсутствует rollback побочных эффектов)
- Последующие действия не выполняются
- Отсутствует подробное логирование и контекст ошибки
- Автомат остаётся в корректном состоянии благодаря существующему механизму отката, но пользователь не имеет контроля над обработкой ошибок

## Решение: Комбинированный подход

### Архитектура решения

Решение состоит из трёх слоёв:

1. **Базовый слой (Core)** — минимальная защита в коде автомата
2. **API слой** — гибкие механизмы для пользователя
3. **Документация** — ясное описание ограничений и best practices

---

## 1. Базовый слой (Core)

### 1.1. Обработка в `StateImpl`

Методы `onEnter()` и `onExit()`:

```java
@Override
public void onEnter(StateMachineDetails<S, E> context) {
    for (int i = 0; i < actions.size(); i++) {
        StateAction<S, E> action = actions.get(i);
        try {
            action.onEnter(context);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format(
                "State action #%d (onEnter) failed in state %s: %s",
                i, stateId, e.getMessage()), e);
            throw e; // Пробрасываем для обработки в StateMachineImpl
        }
    }
}
```

**Ключевые изменения:**
- Используем индекс цикла вместо итератора для идентификации действия
- Детальное логирование: индекс действия, тип операции, состояние
- Пробрасывание исключения для обработки на верхнем уровне

### 1.2. Обработка в `StateMachineImpl`

Методы `executeTransitionActions()`:

```java
private void executeTransitionActions(StateMachineMessage<E> message, Set<TransitionAction<S, E>> actions) {
    int index = 0;
    for (TransitionAction<S, E> action : actions) {
        try {
            action.execute(message, this);
            index++;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, String.format(
                "Transition action #%d failed for event %s: %s",
                index, message.getEvent(), e.getMessage()), e);
            throw e;
        }
    }
}
```

**Ключевые изменения:**
- Индексация действий для идентификации
- Логирование с контекстом событи

### 1.3. Обработка в `processMessage()`

Существующий механизм отката сохраняется, но добавляется уведомление через callback:

```java
private boolean processMessage(StateMachineMessage<E> message) {
    Optional<Transition<S, E>> transition = currentState.getSuitableTransition(message, this);
    if (transition.isPresent()) {
        State<S, E> previousState = currentState;
        try {
            doCurrentStateExit(transition);
            executeTransitionActions(message, transition.get().getActions());
            doNewStateEnter(transition, message);
            executeAsyncActions(message, transition.get().getAsyncActions());
        } catch (Exception e) {
            currentState = previousState; // Атомарность состояния
            
            // Создаём контекст ошибки
            ExceptionContext<S, E> context = createExceptionContext(
                e, message, transition.get(), previousState);
            
            // Уведомляем слушателей
            notifyTransitionFailed(message, e, context);
            
            throw e; // Пробрасываем пользователю
        }
        return true;
    }
    notifyEventNotAccepted(message);
    return false;
}
```

### 1.4. Класс `ExceptionContext`

```java
package alex.band.statemachine;

public class ExceptionContext<S, E> {
    public enum FailureStage {
        STATE_EXIT,
        TRANSITION_ACTION,
        STATE_ENTER,
        GUARD_EVALUATION
    }

    private final FailureStage stage;
    private final State<S, E> state;
    private final int actionIndex;
    private final StateMachineMessage<E> message;

    // constructor, getters...
}
```

---

## 2. API слой

### 2.1. Расширение `StateMachineListener`

```java
public interface StateMachineListener<S, E> {
    // ... существующие методы ...

    /**
     * Вызывается когда переход состояния не удался из-за исключения.
     *
     * @param message сообщение вызвавшее переход
     * @param exception исключение которое возникло
     * @param context детальный контекст ошибки (стадия, индекс действия, состояние)
     */
    default void onTransitionFailed(StateMachineMessage<E> message,
                                    Exception exception,
                                    ExceptionContext<S, E> context) {
        // default implementation - no-op
    }
}
```

### 2.2. Интерфейс `StateMachineExceptionHandler`

```java
package alex.band.statemachine;

/**
 * Глобальный обработчик исключений для state machine.
 * Позволяет пользователю перехватывать и обрабатывать исключения,
 * прежде чем они будут проброшены вызывающему коду.
 */
public interface StateMachineExceptionHandler<S, E> {

    /**
     * Обрабатывает исключение возникшее при выполнении перехода.
     *
     * @param exception исходное исключение
     * @param message сообщение вызвавшее переход
     * @param context детальный контекст ошибки
     * @return true для подавления исключения (продолжить работу),
     *         false для проброса исключения вызывающему коду
     */
    boolean handleException(Exception exception,
                           StateMachineMessage<E> message,
                           ExceptionContext<S, E> context);
}
```

Добавление в `StateMachine<S, E>`:

```java
public interface StateMachine<S, E> {
    // ... существующие методы ...

    /**
     * Устанавливает глобальный обработчик исключений.
     * Обработчик вызывается до того как исключение будет проброшено
     * вызывающему коду.
     */
    void setExceptionHandler(StateMachineExceptionHandler<S, E> handler);

    Optional<StateMachineExceptionHandler<S, E>> getExceptionHandler();
}
```

Реализация в `StateMachineImpl`:

```java
private StateMachineExceptionHandler<S, E> exceptionHandler;

@Override
public void setExceptionHandler(StateMachineExceptionHandler<S, E> handler) {
    this.exceptionHandler = handler;
}

@Override
public Optional<StateMachineExceptionHandler<S, E>> getExceptionHandler() {
    return Optional.ofNullable(exceptionHandler);
}
```

Интеграция в `processMessage()`:

```java
} catch (Exception e) {
    currentState = previousState;

    ExceptionContext<S, E> context = createExceptionContext(e, message, transition.get(), previousState);
    notifyTransitionFailed(message, e, context);

    // Позволяем exception handler подавить исключение
    if (exceptionHandler != null && exceptionHandler.handleException(e, message, context)) {
        return true; // Продолжаем работу без проброса исключения
    }

    throw e;
}
```

---

## 3. Документация

### 3.1. Атомарность состояния

**Гарантируется:**
- Переменная `currentState` всегда остаётся в валидном состоянии
- При ошибке автомат откатывается к предыдущему состоянию
- Исключение пробрасывается вызывающему коду

**Не гарантируется:**
- Откат побочных эффектов (side effects) действий
- Действия выполненные до ошибки остаются выполненными

### 3.2. Обработка исключений пользователем

**Варианты реакции пользователя:**

1. **Игнорировать и продолжить:**
   ```java
   sm.setExceptionHandler((e, msg, ctx) -> {
       logger.warn("Ignoring exception: " + e.getMessage());
       return true; // Подавить исключение
   });
   ```

2. **Логировать и пробросить:**
   ```java
   sm.addListener(new StateMachineListenerAdapter<>() {
       @Override
       public void onTransitionFailed(StateMachineMessage<E> msg,
                                       Exception e,
                                       ExceptionContext<S, E> ctx) {
           errorReporter.report(ctx.getStage(), ctx.getActionIndex(), e);
       }
   });
   ```

3. **Ручной rollback (для критичных операций):**
   ```java
   sm.addListener(new StateMachineListenerAdapter<>() {
       @Override
       public void onTransitionFailed(...) {
           // Выполнить компенсирующие действия
           rollbackManager.compensate(ctx.getMessage());
       }
   });
   ```

### 3.3. Best Practices

1. **Минимизируйте side effects в действиях:**
   - Делайте действия идемпотентными когда это возможно
   - Используйте transactions для изменения состояния БД

2. **Используйте компенсирующие действия:**
   - Если действие создаёт ресурс, добавьте cleanup в onTransitionFailed
   - Если действие отправляет сообщение, используйте outbox pattern

3. **Логируйте ошибки:**
   - Используйте listener или exception handler для централизованного логирования
   - Включайте контекст: этап, индекс действия, состояние

4. **Минимизируйте количество действий:**
   - Разделяйте сложные логики на несколько переходов
   - Это уменьшает область атомарности

---

## Пример использования

```java
StateMachineBuilder<String, String> builder = StateMachineBuilder.builder();

// 1. Настройка состояний и действий
builder.defineState("Ready").onEnter((context) -> {
    database.beginTxn();
}).onExit((context) -> {
    database.commitTxn();
});

// 2. Настройка exception handler
StateMachine<String, String> sm = builder.build();
sm.setExceptionHandler((exception, message, context) -> {
    if (context.getStage() == FailureStage.STATE_EXIT) {
        // Rollback транзакции
        database.rollback();
        return true; // Подавить исключение, автомат остаётся в предыдущем состоянии
    }
    return false; // Пробросить исключение
});

// 3. Настройка listener для логирования
sm.addListener(new StateMachineListenerAdapter<String, String>() {
    @Override
    public void onTransitionFailed(StateMachineMessage<String> message,
                                    Exception e,
                                    ExceptionContext<String, String> context) {
        logger.error("Transition failed at stage {} with action index {}: {}",
            context.getStage(), context.getActionIndex(), e.getMessage());
    }
});

// 4. Запуск
sm.start();
```

---

## Преимущества подхода

1. **Атомарность состояния:** Гарантируется существующим механизмом
2. **Гибкость:** Пользователь сам выбирает стратегию обработки
3. **Прозрачность:** Детальное логирование и контекст ошибок
4. **Обратная совместимость:** Default методы не ломают существующий API
5. **Простота:** Минимальные изменения в core коде

---

## Ограничения

1. **Нет автоматического rollback:** Пользователь сам должен реализовать компенсацию если она нужна
2. **Partial execution:** Действия выполненные до ошибки остаются выполненными
3. **Async actions:** Исключения в async действиях обрабатываются ExecutorService'ом

---

## Roadmap реализации

| Приоритет | Задача |
|-----------|--------|
| P1 | Реализовать базовый слой (try-catch + логирование) |
| P1 | Добавить `onTransitionFailed` в listener |
| P2 | Добавить `StateMachineExceptionHandler` |
| P2 | Написать примеры использования |
| P3 | Добавить компенсирующие действия (опционально) |
