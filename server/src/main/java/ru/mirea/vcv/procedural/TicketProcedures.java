package ru.mirea.vcv.procedural;

import java.util.List;
import java.util.Map;

/**
 * Алгоритм 1. Проверка и гашение билета при входе зрителя в виртуальный зал
 * (автоматизируемая операция А1).
 *
 * <p>Сложность: поиск билета по коду в хеш-таблице выполняется в среднем за O(1),
 * остальные проверки — фиксированное число сравнений, поэтому вся процедура
 * выполняется за O(1) и не зависит от числа проданных билетов n. При поиске
 * перебором списка билетов сложность была бы O(n).</p>
 */
public final class TicketProcedures {

    private TicketProcedures() {
    }

    /** Зал открывается за 30 минут до начала мероприятия. */
    public static final long OPEN_BEFORE_SEC = 30 * 60;

    // ---- Результаты проверки билета ----
    public static final int ADMITTED = 0;          // первый вход, билет погашен
    public static final int READMITTED = 1;        // повторный вход после разрыва связи
    public static final int NOT_FOUND = 2;         // билета с таким кодом нет
    public static final int WRONG_EVENT = 3;       // билет на другое мероприятие
    public static final int EVENT_CANCELLED = 4;   // мероприятие отменено
    public static final int TOO_EARLY = 5;         // зал ещё не открыт
    public static final int FINISHED = 6;          // мероприятие уже закончилось
    public static final int NOT_VALID = 7;         // билет не оплачен, возвращён или аннулирован
    public static final int OTHER_VIEWER = 8;      // билет уже закреплён за другим зрителем
    public static final int SESSION_ACTIVE = 9;    // по билету уже открыт сеанс

    public static final String[] RESULT_NAMES = {
        "допущен, билет погашен",
        "допущен повторно",
        "отказ: билет не найден",
        "отказ: билет на другое мероприятие",
        "отказ: мероприятие отменено",
        "отказ: зал ещё не открыт",
        "отказ: мероприятие завершено",
        "отказ: билет недействителен",
        "отказ: билет закреплён за другим зрителем",
        "отказ: по билету уже открыт сеанс"
    };

    /**
     * Проверяет билет и при успехе гасит его (переводит в состояние «Использован»).
     *
     * @param ticketsByCode все билеты, проиндексированные по коду (хеш-таблица)
     * @param event         мероприятие, в зал которого входит зритель
     * @param code          код билета из ссылки на вход
     * @param viewerId      идентификатор зрителя
     * @param nowSec        текущее время сервера, секунды Unix
     * @param history       журнал переходов состояний (дополняется при гашении)
     * @return код результата: ADMITTED, READMITTED или одна из причин отказа
     */
    public static int admit(Map<String, Model.Ticket> ticketsByCode, Model.Event event,
                            String code, String viewerId, long nowSec,
                            List<Model.Transition> history) {
        Model.Ticket t = ticketsByCode.get(code);            // O(1) в среднем
        if (t == null) {
            return NOT_FOUND;
        }
        if (!t.eventId.equals(event.id)) {
            return WRONG_EVENT;
        }
        if (event.cancelled) {
            return EVENT_CANCELLED;
        }
        if (nowSec < event.startEpochSec - OPEN_BEFORE_SEC) {
            return TOO_EARLY;
        }
        if (nowSec >= event.startEpochSec + event.durationSec) {
            return FINISHED;
        }
        if (t.state == Model.PAID) {                          // первый вход: гашение
            changeState(t, Model.USED, nowSec, "гашение при входе", history);
            t.viewerId = viewerId;
            t.sessionActive = true;
            return ADMITTED;
        }
        if (t.state == Model.USED) {                          // повторный вход
            if (!t.viewerId.equals(viewerId)) {
                return OTHER_VIEWER;
            }
            if (t.sessionActive) {
                return SESSION_ACTIVE;
            }
            t.sessionActive = true;
            return READMITTED;
        }
        return NOT_VALID;                                     // RESERVED, REFUNDED, ANNULLED, CANCELLED
    }

    /** Закрывает сеанс зрителя (разрыв связи или выход из зала), чтобы был возможен повторный вход. */
    public static void endSession(Model.Ticket t) {
        t.sessionActive = false;
    }

    /** Меняет состояние билета и записывает переход в журнал. */
    static void changeState(Model.Ticket t, int newState, long nowSec, String reason,
                            List<Model.Transition> history) {
        Model.Transition tr = new Model.Transition();
        tr.ticketCode = t.code;
        tr.fromState = t.state;
        tr.toState = newState;
        tr.atEpochSec = nowSec;
        tr.reason = reason;
        history.add(tr);
        t.state = newState;
    }
}
