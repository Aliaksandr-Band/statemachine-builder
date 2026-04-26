# Аудит безопасности core-логики State Machine

**Дата:** 2026-04-25
**Цель:** Проверить core-логику (без пользовательского кода) на возможные исключения: NPE, IndexOutOfBoundsException, ConcurrentModificationException и др.

---

## Обзор

Аудит проведен на основе анализа исходных файлов без их запуска. Проверены классы:
- `StateMachineImpl`
- `ListenableStateMachine`
- `StateImpl`
- `TransitionImpl`
- `StateMachineContextImpl`
- `StateMachineBuilderImpl`

---

## Критические проблемы 🔴

### ~~AUDIT-CRITICAL-1: ConcurrentModificationException в notify-методах~~ - не актуально. Все обращения к коллекции слушателей происходят из публичным synchronized методов
**Файлы:** `ListenableStateMachine.java` (строки 26-28, 40-42, 82-84, 92-95)

**Проблема:**
Методы `notifyStateChanged()`, `notifyEventNotAccepted()`, `start()`, `stop()` итерируют по `listeners` без синхронизации:
```java
// notifyStateChanged
for (StateMachineListener<S, E> listener: listeners) {
    listener.onStateChanged(message, previousState, this);
}

// start() - вызывается после doStart(), которая синхронизирована
for (StateMachineListener<S, E> listener: listeners) {
    listener.onStart(this);
}
```

Если другой поток вызовет `addListener()` или `removeListener()` во время итерации, возможен `ConcurrentModificationException`.

**Сценарий воспроизведения:**
1. Поток 1: вызов `accept()` → переход → `notifyStateChanged()` начинает итерацию
2. Поток 2: параллельно вызывает `removeListener()`
3. Поток 2 модифицирует `listeners`
4. Поток 1 получает `ConcurrentModificationException`

**Влияние на целостность состояния:** нет (это слушатели, не core-логика)
**Влияние на стабильность работы:** критическое (автомат падает)

**Рекомендация:**
Создать snapshot коллекции перед итерацией:
```java
protected void notifyStateChanged(StateMachineMessage<E> message, State<S, E> previousState) {
    List<StateMachineListener<S, E>> listenersCopy;
    synchronized (this) {
        listenersCopy = new ArrayList<>(listeners);
    }
    for (StateMachineListener<S, E> listener: listenersCopy) {
        listener.onStateChanged(message, previousState, this);
    }
}
```

**Приоритет:** P0 (высокий)

---

## Серьезные проблемы 🟠

### ~~AUDIT-SERIOUS-1: NPE в doStart() при некорректной конфигурации~~ - описанные ситуации крайне маловероятны и нарушают правила создания экземпляров (через билдер). Тем более конструктор сделан package-private
**Файлы:** `StateMachineImpl.java` (строки 62-64)

**Проблема:**
```java
mode = Mode.RUNNING;
currentState = initialState;  // Может быть null
currentState.onEnter(this);   // NPE здесь
```

Хотя builder валидирует наличие `initialState`, это происходит в отдельном методе `validateStates()`. Теоретически возможна ситуация:
- Некорректная реализация builder-а
- Модификация StateMachineImpl напрямую через reflection
- Побочные эффекты при параллельном использовании

**Влияние:** NPE, автомат перестает работать
**Рекомендация:**
```java
checkState(initialState != null, "Initial state is not set");
currentState = initialState;
```

**Приоритет:** P1

---

### AUDIT-SERIOUS-2: NPE в doStop() при некорректном состоянии
**Файлы:** `StateMachineImpl.java` (строка 70)

**Проблема:**
```java
protected void doStop() {
    checkState(mode == Mode.RUNNING, "Statemachine is not running.");
    currentState.onExit(this);  // Может быть null, если автомат был некорректно инициализирован
    ...
}
```

Сценарий: автомат создан без корректной инициализации, но `mode` был установлен в `RUNNING`.

**Влияние:** NPE при попытке остановки
**Рекомендация:**
```java
checkState(currentState != null, "Current state is null");
currentState.onExit(this);
```

**Приоритет:** P1

---

### AUDIT-SERIOUS-3: NPE в doNewStateEnter() при отсутствии целевого состояния
**Файлы:** `StateMachineImpl.java` (строки 153-154)

**Проблема:**
```java
currentState = states.get(transition.get().getTarget().get());
currentState.onEnter(this);  // NPE если get() вернул null
```

Хотя builder валидирует переходы в `validateTransitions()`, это происходит до создания StateMachine. Возможные причины:
- Модификация `states` после build
- Некорректная реализация `equals()`/`hashCode()` у State ID
- ConcurrentModification (если `states` не защищен)

**Влияние:** NPE при выполнении перехода
**Рекомендация:**
```java
State<S, E> newState = states.get(transition.get().getTarget().get());
checkState(newState != null, "Target state not found: " + transition.get().getTarget().get());
currentState = newState;
```

**Приоритет:** P1

---

### AUDIT-SERIOUS-4: NPE в doReset() если context не установлен
**Файлы:** `StateMachineImpl.java` (строка 81)

**Проблема:**
```java
protected void doReset() {
    checkState(mode != Mode.RUNNING, "Statemachine cannot be reset while running.");
    context.clear();  // NPE если context не был установлен
    ...
}
```

Builder устанавливает context, но это может быть пропущено или заменено на null.

**Влияние:** NPE при reset
**Рекомендация:**
```java
checkState(context != null, "Context is null");
context.clear();
```

**Приоритет:** P1

---

## Заметные проблемы 🟡

### AUDIT-NOTABLE-1: Отсутствие проверок null в add-методах TransitionImpl
**Файлы:** `TransitionImpl.java` (строки 74-80)

**Проблема:**
```java
public void addAction(TransitionAction<S, E> action) {
    actions.add(action);  // NPE если action == null
}

public void addActions(Set<TransitionAction<S, E>> actions) {
    this.actions.addAll(actions);  // NPE если actions == null
}
```

Это builder-методы, вызываются во время конфигурации. Builder должен валидировать, но runtime не имеет защиты.

**Влияние:** NPE во время конфигурации
**Рекомендация:**
```java
public void addAction(TransitionAction<S, E> action) {
    checkNotNull(action, "Transition action cannot be null");
    actions.add(action);
}
```

**Приоритет:** P2

---

### AUDIT-NOTABLE-2: Отсутствие проверок null в add-методах StateImpl
**Файлы:** `StateImpl.java` (строки 81-94)

**Проблема:**
```java
public void addActions(Set<StateAction<S, E>> actions) {
    this.actions.addAll(actions);  // NPE если actions == null
}

public void addAction(StateAction<S, E> action) {
    this.actions.add(action);  // NPE если action == null
}

public void addTransition(Transition<S, E> transition) {
    if (!transitions.containsKey(transition.getEvent())) {  // NPE если transition == null
        transitions.put(transition.getEvent(), new LinkedHashSet<>());
    }
    transitions.get(transition.getEvent()).add(transition);
}
```

**Влияние:** NPE во время конфигурации
**Рекомендация:** Добавить проверки через `Asserts.checkNotNull()`

**Приоритет:** P2

---

### AUDIT-NOTABLE-3: NullPointerException в StateMachineContextImpl
**Файлы:** `StateMachineContextImpl.java`

**Проблема:**
Методы не проверяют null аргументы:
```java
public Object getValue(String key) {
    return values.get(key);  // OK для ConcurrentHashMap (возвращает null)
}

public void setValue(String key, Object value) {
    values.put(key, value);  // NPE если key == null
}

public Object removeValue(String key) {
    return values.remove(key);  // NPE если key == null
}
```

**Влияние:** NPE если пользователь передает null в качестве ключа
**Рекомендация:** Добавить проверки null для `key`

**Приоритет:** P2

---

### AUDIT-NOTABLE-4: Некорректное сообщение об ошибке в doStart()
**Файлы:** `StateMachineImpl.java` (строка 56)

**Проблема:**
```java
checkState(mode == Mode.READY, "Statemachine is already running or stopped.");
```

Сообщение "already running or stopped" не точно. Если `mode == FAULT`, пользователь увидит это сообщение, хотя автомат в режиме FAULT, а не running или stopped.

**Влияние:** Путаница при отладке
**Рекомендация:**
```java
checkState(mode == Mode.READY, "Statemachine is in %s state, expected READY", mode);
```

**Приоритет:** P2

---

### AUDIT-NOTABLE-5: ConcurrentModification в StateMachineBuilderImpl.states
**Файлы:** `StateMachineBuilderImpl.java` (строка 19)

**Проблема:**
```java
private Map<S, State<S, E>> states = new HashMap<>();
```

Builder не синхронизирован. Если builder используется в нескольких потоках, возможна `ConcurrentModificationException`.

**Влияние:** Нестабильность при параллельной конфигурации
**Рекомендация:**
- Либо задокументировать что builder не thread-safe
- Либо использовать `ConcurrentHashMap`

**Приоритет:** P2

---

## Некритичные проблемы 🟢

### AUDIT-MINOR-1: Отсутствие логирования исключений из пользовательского кода
**Файлы:** `StateMachineImpl.java` (строки 58-60, 70)

**Проблема:**
Методы `doStart()` и `doStop()` выполняют пользовательский код (`startActions`, `stopActions`, `state actions`) без try-catch. Исключения пробрасываются наружу.

**Влияние:** Необнаруженные исключения в пользовательском коде
**Рекомендация:** Будет решено в Фазе 2 (переход в FAULT)

**Приоритет:** P2 (будет решено вместе с FAULT)

---

### AUDIT-MINOR-2: getSuitableTransition() может вернуть null transition
**Файлы:** `StateImpl.java` (строки 29-48)

**Проблема:**
Метод возвращает `Optional<Transition>`, но внутри использует `transitions.get(message.getEvent())` который может вернуть null. Однако код проверяет `isEmpty()`, так что это безопасно.

**Влияние:** Нет
**Статус:** Ложная тревога, код корректен

---

## Потенциальные улучшения (не баги) 📝

### IMPROVE-1: Использование unmodifiable collections для builder полей
**Файлы:** `TransitionImpl.java` (строка 48)

**Наблюдение:**
```java
@Override
public Set<TransitionAction<S, E>> getActions() {
    return Collections.unmodifiableSet(actions);
}
```

Хорошая практика защиты от внешней модификации. Можно применить аналогично в других местах (например, `StateImpl.getTransitions()`).

---

### IMPROVE-2: Добавить checkState для режимов в doAccept()
**Файлы:** `StateMachineImpl.java` (строки 88-90)

**Наблюдение:**
```java
if (!isRunning()) {
    return false;
}
```

Используется мягкая проверка (return false) вместо `checkState()`. Это корректно для API, но можно добавить warning логирование для не-RUNNING режимов.

---

### IMPROVE-3: Защита от null в listener callbacks
**Файлы:** `ListenableStateMachine.java`

**Наблюдение:**
Методы `addListener()`/`removeListener()` не проверяют null аргумент. `LinkedHashSet` допускает null, но слушатель null не имеет смысла.

**Рекомендация:** Добавить проверки null для `listener`

---

## Сводка по приоритетам

| Приоритет | Проблема | Количество |
|-----------|-----------|------------|
| **P0** | AUDIT-CRITICAL-1: ConcurrentModification в notify | 1 |
| **P1** | NPE в doStart, doStop, doNewStateEnter, doReset | 4 |
| **P2** | Null checks в add-методах, ConcurrentModification в builder | 4 |
| **P2** | AUDIT-MINOR-1: Логирование пользовательских исключений | 1 (будет решено в Фазе 2) |

---

## Рекомендации по порядку исправления

1. ~~**Срочно:** AUDIT-CRITICAL-1 (ConcurrentModification в notify-методах)~~ - обращение к слушателям синхронзированы
   - Создать snapshot перед итерацией
   - Добавить тест на одновременное addListener/removeListener + уведомление

2. **Важно:** AUDIT-SERIOUS-1..4 (NPE проверки)
   - Добавить `checkState()` для null значений
   - Улучшить сообщения об ошибках
   - Добавить тесты для некорректных конфигураций

3. **Полезно:** AUDIT-NOTABLE-1..5 (null checks в builder методах)
   - Добавить `checkNotNull()` в add-методы
   - Рассмотреть `ConcurrentHashMap` для builder

4. **В рамках Фазы 2:** AUDIT-MINOR-1
   - Будет решено при добавлении try-catch и перехода в FAULT

---

## Заключение

Core-логика в целом защищена от исключений благодаря:
- Использованию `Asserts.checkState()` для валидации состояний
- Optional для возвращаемых значений
- ConcurrentHashMap для контекста
- Валидациями в builder перед созданием StateMachine

Основные проблемы:
1. ~~**ConcurrentModification** в notify-методах (P0)~~ - не актуально
2. **Отсутствие защитных проверок** для null в runtime (P1)

После исправления критических проблем, логика конечного автомата без пользовательского кода будет практически гарантированно свободна от исключений.

---

## Связанные задачи

- AUDIT-CRITICAL-1 связано с EXCEPTIONS-P1-3 из PLAN-EXCEPTIONS-HANDLING.md
- AUDIT-SERIOUS-1..4 связаны с требованием "безопасность core-логики"
- AUDIT-MINOR-1 будет решен в Фазе 2 текущего плана
