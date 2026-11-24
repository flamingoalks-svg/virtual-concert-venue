# Виртуальная концертная площадка

Учебная практика, РТУ МИРЭА, Институт информационных технологий, кафедра игровой индустрии, 2026 г.

Программное решение, позволяющее организатору создать виртуальную концертную площадку, продать доступ на мероприятие и провести его для зрителей, подключающихся через браузер, в том числе с устройств виртуальной реальности (WebGL / WebXR).

## Структура репозитория

| Каталог или файл | Назначение |
|---|---|
| `unity-client/` | Unity-проект клиентского приложения виртуальной площадки |
| `server/` | Серверный компонент на языке Java |
| `db/` | SQL-скрипты и модели данных |
| `docs/` | Отчёт, схемы и материалы исследования |
| `README.md` | Описание проекта и инструкция по запуску |
| `.gitignore`, `.gitattributes` | Правила исключения служебных файлов и настройки Git LFS |

## Используемые средства

- Unity 6.3 LTS — клиентская сцена, сборка под WebGL и WebXR
- Java (JDK 11), IntelliJ IDEA, Maven — серверный компонент
- PostgreSQL — физическая модель данных
- Blender — трёхмерные модели сцены и зала
- Git, Git LFS — контроль версий

## Как начать работу

```bash
git lfs install          # один раз на компьютере
git clone <ссылка на репозиторий>
```

### Сервер

```bash
cd server
mvn test                 # запуск тестов
mvn package              # сборка
java -jar target/virtual-concert-server-0.1.0.jar
```

### База данных

```bash
createdb vcv
psql -d vcv -f db/schema.sql
```

### Клиент

Откройте каталог `unity-client/` в Unity Hub (Unity 6.3 LTS). Перед первым коммитом Unity-проекта проверьте настройки редактора (см. `unity-client/README.md`).

## Как проверить работу (аналитический этап)

Процедурная реализация алгоритмов находится в пакете `server/src/main/java/ru/mirea/vcv/procedural`:

| Файл | Что делает |
|---|---|
| `Model.java` | Структуры данных: мероприятие, билет, переход состояния билета, заявка на возврат, сеанс, экземпляр зала |
| `TicketProcedures.java` | Алгоритм 1 — проверка и гашение билета при входе (А1) |
| `RefundProcedures.java` | Алгоритм 2 — возврат билета и отмена мероприятия (А2) |
| `InstanceProcedures.java` | Алгоритм 3 — распределение зрителей по экземплярам зала (А3) |
| `PlaybackProcedures.java` | Алгоритм 4 — синхронный показ записи выступления (А4) |
| `SessionProcedures.java` | Вход в зал и выход: проверка билета, выбор экземпляра и закрепление его за сеансом (А1, А3) |
| `ProceduralDemo.java` | Контрольный пример: входные данные и результаты всех алгоритмов |

Запуск тестов (JUnit 4):

```bash
cd server
mvn test
```

Запуск контрольного примера: в IntelliJ IDEA открыть `ProceduralDemo.java` и нажать Run, либо

```bash
cd server
mvn -q compile
java -cp target/classes ru.mirea.vcv.procedural.ProceduralDemo
```

Модели и схемы лежат в `docs/`:

- `docs/bpmn/*.bpmn` — модель основного бизнес-процесса в BPMN 2.0 (верхний уровень и три декомпозиции); файлы в формате BPMN 2.0 XML для импорта в редакторы BPMN (Stormbpmn, bpmn.io, Camunda Modeler);
- `docs/diagrams/er.puml` — логическая модель данных (PlantUML);
- `docs/diagrams/ticket.puml` — диаграмма состояний билета;
- `docs/diagrams/alg*.svg` — схемы алгоритмов по ГОСТ 19.701-90.

Контрольный пример для замечания о возврате: билет `T-000` относится к мероприятию E1; заявка на возврат по мероприятию E2 отклоняется, состояние билета не меняется, заявка сохраняется в журнале с причиной отказа (тест `ticketOfAnotherEventIsNotRefunded`).

## Объектно-ориентированная реализация

Те же операции А1–А4 реализованы в объектно-ориентированном стиле в пакете `server/src/main/java/ru/mirea/vcv/oop`:

| Пакет | Классы | Что делают |
|---|---|---|
| `event` | `Event` | Мероприятие; само отвечает на вопросы о времени: открыт ли зал, началось ли, сколько осталось до начала |
| `ticket` | `Ticket`, `TicketState`, `StateTransition`, `TicketRepository`, `InMemoryTicketRepository`, `IllegalTicketStateException` | Билет меняет состояние только своими методами; допустимые переходы заданы в `TicketState` |
| `admission` | `AdmissionService`, `AdmissionRule`, `AdmissionRules`, `BlockedViewerRule`, `AdmissionResult` | Допуск в зал (А1) как последовательность правил; новое правило — новый класс |
| `refund` | `RefundService`, `RefundPolicy`, `StatutoryRefundPolicy`, `FullRefundPolicy`, `RefundRequest`, `RefundBasis` | Возвраты (А2) с подключаемыми правилами расчёта доли и журналом заявок |
| `hall` | `HallEntrance`, `VirtualHall`, `HallInstance`, `Session`, `Entry` | Вход в зал (А1 + А3), экземпляр закрепляется за сеансом |
| `playback` | `PlaybackSchedule`, `DriftCorrector`, `PlaybackPhase` | Синхронный показ записи (А4) |

Контрольный пример — `OopDemo.java` (тот же сценарий, что в `ProceduralDemo`, плюс новое требование «стоп-лист зрителей»):

```bash
cd server
mvn -q compile
java -cp target/classes ru.mirea.vcv.oop.OopDemo
```

`EquivalenceTest` проверяет, что процедурная и объектно-ориентированная реализации дают одинаковые результаты на одних и тех же данных. UML-диаграммы классов и последовательности — в `docs/diagrams/oop/`.

## Правила работы с репозиторием

- Стабильная версия хранится в ветке `main`.
- Новые функции разрабатываются в отдельных ветках (`feature/<название>`) и вливаются в `main` через pull request.
- Сообщения коммитов краткие и описывают суть изменения.
