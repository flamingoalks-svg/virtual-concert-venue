package ru.mirea.vcv.procedural;

import java.util.List;
import java.util.Map;

/**
 * Вход зрителя в зал и выход из него: связывает алгоритмы 1 и 3 и закрепляет выбранный
 * экземпляр зала за сеансом зрителя (операции А1 и А3).
 *
 * <p>Сложность входа: проверка билета — O(1), выбор экземпляра — O(k), создание сеанса — O(1);
 * итого O(k), где k — число экземпляров зала.</p>
 */
public final class SessionProcedures {

    private SessionProcedures() {
    }

    /**
     * Впускает зрителя в зал: проверяет и гасит билет (алгоритм 1), выбирает экземпляр зала
     * (алгоритм 3) и создаёт сеанс, в котором записан номер выбранного экземпляра.
     *
     * @return созданный сеанс или null, если во входе отказано
     */
    public static Model.Session enterHall(Map<String, Model.Ticket> ticketsByCode, Model.Event event,
                                          String code, String viewerId, long nowSec,
                                          List<Model.Transition> history,
                                          List<Model.HallInstance> instances,
                                          List<Model.Session> sessions) {
        int result = TicketProcedures.admit(ticketsByCode, event, code, viewerId, nowSec, history);
        if (result != TicketProcedures.ADMITTED && result != TicketProcedures.READMITTED) {
            return null;
        }
        int number = InstanceProcedures.assignViewer(instances, event.instanceCapacity);
        Model.Session s = new Model.Session();
        s.ticketCode = code;
        s.viewerId = viewerId;
        s.instanceNumber = number;              // экземпляр закрепляется за сеансом
        s.startSec = nowSec;
        sessions.add(s);
        return s;
    }

    /**
     * Завершает сеанс: фиксирует время выхода, освобождает место в закреплённом
     * экземпляре зала и разрешает повторный вход по билету.
     */
    public static void leaveHall(Model.Session s, Model.Ticket t, List<Model.HallInstance> instances,
                                 long nowSec) {
        s.endSec = nowSec;
        InstanceProcedures.releaseViewer(instances, s.instanceNumber);
        TicketProcedures.endSession(t);
    }
}
