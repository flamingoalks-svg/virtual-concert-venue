package ru.mirea.vcv.oop.ticket;

import java.util.List;
import java.util.Optional;
import ru.mirea.vcv.oop.event.Event;

/**
 * Хранилище билетов. Службы работают с интерфейсом, поэтому хранение в памяти
 * можно заменить хранением в PostgreSQL без изменения служб.
 */
public interface TicketRepository {

    void save(Ticket ticket);

    Optional<Ticket> findByCode(String code);

    List<Ticket> findByEvent(Event event);
}
