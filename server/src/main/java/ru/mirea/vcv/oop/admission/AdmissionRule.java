package ru.mirea.vcv.oop.admission;

import java.time.Instant;
import java.util.Optional;
import ru.mirea.vcv.oop.event.Event;
import ru.mirea.vcv.oop.ticket.Ticket;

/**
 * Одно правило допуска в зал. Возвращает причину отказа или пустое значение,
 * если правило выполнено. Новая проверка добавляется новой реализацией интерфейса,
 * без изменения службы допуска и других правил.
 */
@FunctionalInterface
public interface AdmissionRule {

    Optional<AdmissionResult> check(Ticket ticket, Event event, String viewerId, Instant now);
}
