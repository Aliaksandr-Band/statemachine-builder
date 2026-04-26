# План обработки ошибок и режима FAULT в State Machine

## Что уже реализовано ✅

### Внутренние состояния и жизненный цикл
- **Enum `Mode`**: `READY`, `RUNNING`, `STOPPED`, `FAULT` — все определены в `StateMachineDetails`
- **Валидация методов жизненного цикла**:
  - `doStart()`: проверяет `mode == Mode.READY`
  - `doStop()`: проверяет `mode == Mode.RUNNING`
  - `doReset()`: проверяет `mode != Mode.RUNNING`
  - `doAccept()`: проверяет `isRunning()`
- **Метод `reset()`**: полностью реализован с тестами (StateMachineResetTest)

### ExceptionHandler
- Интерфейсы `registerExceptionHandler()`/`unregisterExceptionHandler()` существуют
- Поле `exceptionHandler` есть в `StateMachineImpl` (но **не используется**)

### Атомарность переходов
- В `processMessage()` есть try-catch с rollback состояния к `previousState`
- Есть тест `StateMachineAtomicityTest` (проверяет rollback при исключении в `onEnter`)

### Обработка исключений в Guards
- `GuardsComposer` перехватывает исключения и логирует их с `Level.WARNING`
- При исключении guard возвращает `false` или продолжает проверку
- Есть тесты `GuardsComposerExceptionTest` и `StateImplGuardExceptionTest`

---

## Чего НЕТ ❌ (требуется реализация)

### 1. Переход в режим FAULT
- **Нет логики перевода автомата в `FAULT` при бизнес-исключении**
- Нет метода `setFault()` или подобного
- Исключения просто пробрасываются наружу из `processMessage()`

### 2. Механизм rollback для пользовательских действий
- **Нет интерфейса `Rollbackable`**
- **Нет метода `rollback(context)`** в:
  - `Guard`
  - `TransitionAction`
  - `StateAction`
  - `StateMachineStartAction`
  - `StateMachineStopAction`
  - `StateMachineListener`
- Нет трекера выполненных действий

### 3. Вызов exceptionHandler при исключениях
- Поле `exceptionHandler` есть, но **никогда не вызывается**
- Нет логики передачи исключения зарегистрированному handler-у

### 4. Логирование ошибок с SEVERE
- Используется только `Level.WARNING` в `GuardsComposer`
- Нет логирования с `Level.SEVERE` как требуется

### 5. Тесты для режима FAULT
- Нет тестов для проверки перехода в `FAULT`
- Нет тестов для вызова rollback действий
- Нет тестов для проверки exceptionHandler

---

## План реализации

### Фаза 1: Интерфейс Rollbackable и обновление действий

**1.1 Создать интерфейс Rollbackable**
```java
public interface Rollbackable<S, E> {
    void rollback(StateMachineContext context);
}
```

**1.2 Обновить точки подключения (добавить extends Rollbackable)**
- `Guard<S, E> extends Rollbackable<S, E>` (пустая реализация по умолчанию)
- `TransitionAction<S, E> extends Rollbackable<S, E>`
- `StateMachineStartAction<S, E> extends Rollbackable<S, E>`
- `StateMachineStopAction<S, E> extends Rollbackable<S, E>`
- `StateMachineListener<S, E> extends Rollbackable<S, E>`
- Для `StateAction`: решить - разделить на `OnEnterAction`/`OnExitAction` или добавить оба метода в один интерфейс

**1.3 Создать механизм отслеживания выполненных действий**
- Создать класс `TransitionExecutionTracker` внутри `StateMachineImpl`
- Хранить стек выполненных действий (LIFO)
- Обеспечить потокобезопасность (использовать `ArrayDeque` + синхронизация)

---

### Фаза 2: Обработка исключений и переход в FAULT

**2.1 Добавить метод для перевода в FAULT**
```java
private void setFault() {
    mode = Mode.FAULT;
}
```

**2.2 Обернуть выполнение действий в processMessage()**
```java
try {
    // ... выполнение действий с трекингом
} catch (Exception e) {
    // 2.2.1 Перевести в FAULT
    setFault();
    
    // 2.2.2 Вызвать rollback выполненных действий (в обратном порядке)
    tracker.rollbackExecutedActions(context);
    
    // 2.2.3 Вызвать exceptionHandler
    try {
        exceptionHandler.accept(e);
    } catch (Exception handlerException) {
        LOGGER.log(Level.SEVERE, "Exception handler failed", handlerException);
    }
    
    // 2.2.4 Логировать с SEVERE
    LOGGER.log(Level.SEVERE, "Transition failed, state machine moved to FAULT", e);
}
```

**2.3 Добавить валидацию для методов жизненного цикла**
- `start()`: нельзя запустить из `FAULT` → `IllegalStateException`
- `stop()`: нельзя остановить из `FAULT` → `IllegalStateException`
- `accept()`: нельзя принимать события из `FAULT` → `IllegalStateException`
- `reset()`: можно только из `FAULT` или `STOPPED`

**2.4 Обернуть выполнение в doStart() и doStop()**
- Добавить try-catch для `startActions` и `stopActions`
- При исключении → переход в `FAULT`, rollback, exceptionHandler, логирование SEVERE

---

### Фаза 3: Тестирование

**3.1 Тесты валидации жизненного цикла с FAULT**
- Нельзя `start()` из `FAULT`
- Нельзя `stop()` из `FAULT`
- Нельзя `accept()` из `FAULT`
- Можно `reset()` из `FAULT`

**3.2 Тесты обработки исключений**
- Исключение в Guard → FAULT + rollback + handler вызван
- Исключение в TransitionAction → FAULT + rollback предыдущих действий
- Исключение в StateAction (onEnter) → FAULT + rollback
- Исключение в StartAction → FAULT + rollback
- Исключение в StopAction → FAULT + rollback

**3.3 Тест rollback-логики**
- Проверка порядка вызова rollback (LIFO)
- Rollback с исключениями (они должны перехватываться и логироваться)
- Проверка что все выполненные действия откачены

**3.4 Тест exceptionHandler**
- Handler вызывается при исключении
- Исключение в handler-е не ломает автомат (перехватывается и логируется)

---

### Фаза 4: Аудит безопасности core-логики

**4.1 Проверить все методы на возможные исключения**
- Просмотреть `StateMachineImpl`, `TransitionImpl`, `StateImpl`
- Идентифицировать возможные NPE, IndexOutOfBoundsException и т.д.
- Добавить проверки через `Asserts`

**4.2 Тесты core-логики без пользовательского кода**
- Создать автомат без действий/guards
- Прогнать все сценарии жизненного цикла
- Убедиться что исключений нет

---

## Порядок выполнения

1. **Фаза 1** (Rollbackable + tracker)
2. **Фаза 2** (обработка исключений → FAULT)
3. **Фаза 3** (тесты — параллельно с разработкой)
4. **Фаза 4** (аудит безопасности)

---

## Требования из notes.md

### Тезисы обработки ошибок
- исключения внутренней логики конечного автомата могут возникать только для предотвращения его неправильного использования
- варианты использования конечного автомата зависят от его внутреннего состояния
- внутренние состояния: READY, RUNNING, STOPPED, FAULT
- логика конечного автомата без подключенного пользовательского кода не должна содержать ошибок и возможностей для возникновения исключений
- исключения возникающие в пользовательском коде должны переводить автомат в режим FAULT
- при переходе в режим FAULT вызываются методы отката и зарегистрированный обработчик исключения
- точки подключения пользовательского кода: Guard, TransitionAction, StateAction, StateMachineStartAction, StateMachineStopAction, StateMachineListener

### Задачи
1. ✅ (базовая валидация уже есть) удостовериться, что логика конечного автомата, без подключенной пользовательской логики не приводит к исключениям
2. добавить rollback(context) во все точки подключения пользовательской логики
3. ✅ (базовая валидация уже есть) убедиться что все методы управления жизненным циклом делают проверку на возможность выполнения
4. для метода `acceptMessage()` обеспечить перехват пользовательских бизнес исключений и выполнение действий (4.1-4.4)
