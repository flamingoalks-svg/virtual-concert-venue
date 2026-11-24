package ru.mirea.vcv.oop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;
import ru.mirea.vcv.oop.event.Event;
import ru.mirea.vcv.oop.ticket.IllegalTicketStateException;
import ru.mirea.vcv.oop.ticket.Ticket;
import ru.mirea.vcv.oop.ticket.TicketState;

public class TicketTest {

    private static final Instant NOW = Instant.parse("2026-12-01T10:00:00Z");
    private final Event event = new Event("E1", "Концерт", NOW.plus(Duration.ofDays(9)), Duration.ofMinutes(90), 50);

    @Test
    public void transitionTableMatchesStateDiagram() {
        assertTrue(TicketState.RESERVED.canMoveTo(TicketState.PAID));
        assertTrue(TicketState.RESERVED.canMoveTo(TicketState.CANCELLED));
        assertTrue(TicketState.PAID.canMoveTo(TicketState.USED));
        assertTrue(TicketState.PAID.canMoveTo(TicketState.REFUNDED));
        assertTrue(TicketState.PAID.canMoveTo(TicketState.ANNULLED));
        assertTrue(TicketState.USED.canMoveTo(TicketState.ANNULLED));
        assertFalse(TicketState.USED.canMoveTo(TicketState.REFUNDED));
        assertTrue(TicketState.REFUNDED.next().isEmpty());
        assertTrue(TicketState.ANNULLED.next().isEmpty());
        assertTrue(TicketState.CANCELLED.next().isEmpty());
    }

    @Test
    public void redeemRecordsTransitionAndBindsViewer() {
        Ticket t = new Ticket("A", event, "buyer", 100_000);
        t.pay(NOW);
        t.redeem("viewer", NOW);
        assertEquals(TicketState.USED, t.getState());
        assertEquals("viewer", t.getViewerId());
        assertTrue(t.isSessionActive());
        assertEquals(2, t.getHistory().size());
        assertEquals(TicketState.PAID, t.getHistory().get(1).getFrom());
    }

    @Test(expected = IllegalTicketStateException.class)
    public void usedTicketCannotBeRefunded() {
        Ticket t = new Ticket("A", event, "buyer", 100_000);
        t.pay(NOW);
        t.redeem("viewer", NOW);
        t.refund(100, NOW);
    }

    @Test(expected = IllegalTicketStateException.class)
    public void reservationCannotBeRedeemed() {
        new Ticket("A", event, "buyer", 100_000).redeem("viewer", NOW);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void historyIsReadOnly() {
        new Ticket("A", event, "buyer", 100_000).getHistory().clear();
    }
}
