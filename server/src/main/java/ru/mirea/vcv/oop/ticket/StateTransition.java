package ru.mirea.vcv.oop.ticket;

import java.time.Instant;

/** Неизменяемая запись истории: переход билета из одного состояния в другое. */
public final class StateTransition {

    private final String ticketCode;
    private final TicketState from;
    private final TicketState to;
    private final Instant at;
    private final String reason;

    public StateTransition(String ticketCode, TicketState from, TicketState to, Instant at, String reason) {
        this.ticketCode = ticketCode;
        this.from = from;
        this.to = to;
        this.at = at;
        this.reason = reason;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public TicketState getFrom() {
        return from;
    }

    public TicketState getTo() {
        return to;
    }

    public Instant getAt() {
        return at;
    }

    public String getReason() {
        return reason;
    }
}
