package ru.mirea.vcv.procedural;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RefundProceduresTest {

    private static final long DAY = RefundProcedures.DAY_SEC;
    private static final long START = 100 * DAY;

    private static Model.Event event() {
        Model.Event e = new Model.Event();
        e.id = "E1";
        e.startEpochSec = START;
        e.durationSec = 3600;
        return e;
    }

    private static Model.Ticket ticket(String code, int state) {
        Model.Ticket t = new Model.Ticket();
        t.code = code;
        t.eventId = "E1";
        t.state = state;
        t.priceKop = 150_000;
        t.buyerId = "buyer";
        return t;
    }

    @Test
    public void percentBoundariesFollowArticle52_1() {
        assertEquals(100, RefundProcedures.refundPercent(10 * DAY));
        assertEquals(50, RefundProcedures.refundPercent(10 * DAY - 1));
        assertEquals(50, RefundProcedures.refundPercent(5 * DAY));
        assertEquals(30, RefundProcedures.refundPercent(5 * DAY - 1));
        assertEquals(30, RefundProcedures.refundPercent(3 * DAY));
        assertEquals(0, RefundProcedures.refundPercent(3 * DAY - 1));
    }

    @Test
    public void refundChangesStateAndReturnsAmount() {
        List<Model.Transition> h = new ArrayList<>();
        Model.Ticket t = ticket("A", Model.PAID);
        assertEquals(75_000, RefundProcedures.requestRefund(t, event(), "buyer", START - 7 * DAY, h));
        assertEquals(Model.REFUNDED, t.state);
        assertEquals(1, h.size());
        assertEquals(75_000, h.get(0).refundKop);
    }

    @Test
    public void onlyBuyerCanRequestRefund() {
        Model.Ticket t = ticket("A", Model.PAID);
        assertEquals(RefundProcedures.REFUND_DENIED,
                RefundProcedures.requestRefund(t, event(), "stranger", START - 20 * DAY, new ArrayList<>()));
        assertEquals(Model.PAID, t.state);
    }

    @Test
    public void lateRefundIsDeniedAndTicketStaysValid() {
        List<Model.Transition> h = new ArrayList<>();
        Model.Ticket t = ticket("A", Model.PAID);
        assertEquals(RefundProcedures.REFUND_DENIED,
                RefundProcedures.requestRefund(t, event(), "buyer", START - 2 * DAY, h));
        assertEquals(Model.PAID, t.state);
        assertEquals(0, h.size());
    }

    @Test
    public void usedTicketCannotBeRefunded() {
        Model.Ticket t = ticket("A", Model.USED);
        assertEquals(RefundProcedures.REFUND_DENIED,
                RefundProcedures.requestRefund(t, event(), "buyer", START - 20 * DAY, new ArrayList<>()));
    }

    @Test
    public void cancellationAnnulsPaidAndUsedWithFullRefund() {
        Model.Event e = event();
        Model.Ticket paid = ticket("P", Model.PAID);
        Model.Ticket used = ticket("U", Model.USED);
        used.sessionActive = true;
        Model.Ticket res = ticket("R", Model.RESERVED);
        Model.Ticket ref = ticket("F", Model.REFUNDED);
        Model.Ticket other = ticket("O", Model.PAID);
        other.eventId = "E2";
        List<Model.Transition> h = new ArrayList<>();
        long total = RefundProcedures.cancelEvent(e, Arrays.asList(paid, used, res, ref, other),
                START - DAY, h);
        assertEquals(300_000, total);
        assertEquals(Model.ANNULLED, paid.state);
        assertEquals(Model.ANNULLED, used.state);
        assertFalse(used.sessionActive);
        assertEquals(Model.CANCELLED, res.state);
        assertEquals(Model.REFUNDED, ref.state);
        assertEquals(Model.PAID, other.state);
        assertEquals(3, h.size());
    }
}
