package ru.mirea.vcv.oop.ticket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import ru.mirea.vcv.oop.event.Event;

/**
 * Билет на мероприятие. Состояние меняется только методами билета, каждый из которых
 * проверяет допустимость перехода по {@link TicketState} и записывает переход
 * в собственную историю билета.
 */
public class Ticket {

    private final String code;
    private final Event event;
    private final String buyerId;
    private final long priceKop;
    private TicketState state = TicketState.RESERVED;
    private String viewerId;
    private boolean sessionActive;
    private Consumer<Instant> sessionCloser;
    private final List<StateTransition> history = new ArrayList<>();

    public Ticket(String code, Event event, String buyerId, long priceKop) {
        this.code = Objects.requireNonNull(code);
        this.event = Objects.requireNonNull(event);
        this.buyerId = Objects.requireNonNull(buyerId);
        this.priceKop = priceKop;
    }

    public String getCode() {
        return code;
    }

    public Event getEvent() {
        return event;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public long getPriceKop() {
        return priceKop;
    }

    public TicketState getState() {
        return state;
    }

    public String getViewerId() {
        return viewerId;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public List<StateTransition> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public boolean isFor(Event e) {
        return event.equals(e);
    }

    public boolean isBoughtBy(String personId) {
        return buyerId.equals(personId);
    }

    public void pay(Instant now) {
        moveTo(TicketState.PAID, now, "оплата");
    }

    /** Гашение при первом входе: билет закрепляется за зрителем, открывается сеанс. */
    public void redeem(String viewer, Instant now) {
        moveTo(TicketState.USED, now, "гашение при входе");
        this.viewerId = viewer;
        this.sessionActive = true;
    }

    /** Повторный вход по уже погашенному билету. */
    public void resumeSession() {
        if (state != TicketState.USED || sessionActive) {
            throw new IllegalStateException("Билет " + code + ": повторный вход невозможен");
        }
        sessionActive = true;
    }

    /** После допуска зал привязывает освобождение занятого места к текущему сеансу. */
    public void bindSessionCloser(Consumer<Instant> closer) {
        if (!sessionActive || sessionCloser != null) {
            throw new IllegalStateException("Билет " + code + ": невозможно привязать сеанс");
        }
        sessionCloser = Objects.requireNonNull(closer);
    }

    public void endSession(Instant now) {
        Objects.requireNonNull(now);
        if (sessionCloser != null) {
            sessionCloser.accept(now);
            sessionCloser = null;
        }
        sessionActive = false;
    }

    public void endSession() {
        endSession(Instant.now());
    }

    public void refund(int percent, Instant now) {
        moveTo(TicketState.REFUNDED, now, "возврат по заявке, " + percent + " %");
    }

    public void annul(Instant now) {
        moveTo(TicketState.ANNULLED, now, "отмена мероприятия, 100 %");
        endSession(now);
    }

    public void cancelReservation(Instant now, String reason) {
        moveTo(TicketState.CANCELLED, now, reason);
    }

    private void moveTo(TicketState target, Instant now, String reason) {
        if (!state.canMoveTo(target)) {
            throw new IllegalTicketStateException(code, state, target);
        }
        history.add(new StateTransition(code, state, target, now, reason));
        state = target;
    }
}
