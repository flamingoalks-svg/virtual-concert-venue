package ru.mirea.vcv.oop.ticket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import ru.mirea.vcv.oop.event.Event;

/** Хранилище билетов в памяти: хеш-таблица по коду, поиск за O(1) в среднем. */
public class InMemoryTicketRepository implements TicketRepository {

    private final Map<String, Ticket> byCode = new LinkedHashMap<>();

    @Override
    public void save(Ticket ticket) {
        byCode.put(ticket.getCode(), ticket);
    }

    @Override
    public Optional<Ticket> findByCode(String code) {
        return Optional.ofNullable(byCode.get(code));
    }

    @Override
    public List<Ticket> findByEvent(Event event) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : byCode.values()) {
            if (t.isFor(event)) {
                result.add(t);
            }
        }
        return result;
    }
}
