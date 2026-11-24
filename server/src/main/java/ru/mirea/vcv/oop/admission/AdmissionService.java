package ru.mirea.vcv.oop.admission;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import ru.mirea.vcv.oop.event.Event;
import ru.mirea.vcv.oop.ticket.Ticket;
import ru.mirea.vcv.oop.ticket.TicketRepository;
import ru.mirea.vcv.oop.ticket.TicketState;

/**
 * Служба допуска в зал (операция А1). Находит билет, по очереди применяет правила
 * и при допуске гасит билет или возобновляет сеанс.
 * Сложность: поиск в хранилище O(1) и фиксированное число правил — O(1).
 */
public class AdmissionService {

    private final TicketRepository tickets;
    private final List<AdmissionRule> rules;

    public AdmissionService(TicketRepository tickets, List<AdmissionRule> rules) {
        this.tickets = tickets;
        this.rules = new ArrayList<>(rules);
    }

    /** Подключение дополнительного правила, например стоп-листа. */
    public void addRule(AdmissionRule rule) {
        rules.add(rule);
    }

    public AdmissionResult admit(Event event, String code, String viewerId, Instant now) {
        Optional<Ticket> found = tickets.findByCode(code);
        if (!found.isPresent()) {
            return AdmissionResult.NOT_FOUND;
        }
        Ticket t = found.get();
        for (AdmissionRule rule : rules) {
            Optional<AdmissionResult> denial = rule.check(t, event, viewerId, now);
            if (denial.isPresent()) {
                return denial.get();
            }
        }
        if (t.getState() == TicketState.PAID) {
            t.redeem(viewerId, now);
            return AdmissionResult.ADMITTED;
        }
        t.resumeSession();
        return AdmissionResult.READMITTED;
    }
}
