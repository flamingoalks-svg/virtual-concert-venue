package ru.mirea.vcv.oop.admission;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import ru.mirea.vcv.oop.event.Event;
import ru.mirea.vcv.oop.ticket.Ticket;

/**
 * Новое требование, добавленное без изменения существующего кода: организатор может
 * запретить вход зрителю, нарушившему правила поведения в зале (стоп-лист).
 */
public class BlockedViewerRule implements AdmissionRule {

    private final Set<String> blocked = new HashSet<>();

    public void block(String viewerId) {
        blocked.add(viewerId);
    }

    @Override
    public Optional<AdmissionResult> check(Ticket ticket, Event event, String viewerId, Instant now) {
        return blocked.contains(viewerId) ? Optional.of(AdmissionResult.BLOCKED) : Optional.empty();
    }
}
