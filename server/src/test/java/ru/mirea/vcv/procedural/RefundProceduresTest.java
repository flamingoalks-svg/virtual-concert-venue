package ru.mirea.vcv.procedural;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RefundProceduresTest {

    private static final long DAY = RefundProcedures.DAY_SEC;
    private static final long START = 100 * DAY;

    private static Model.Event event(String id) {
        Model.Event e = new Model.Event();
        e.id = id;
        e.startEpochSec = START;
        e.durationSec = 3600;
        return e;
    }

    private static Model.Event event() {
        return event("E1");
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
    public void refundChangesStateAndIsRecorded() {
        List<Model.Transition> h = new ArrayList<>();
        List<Model.RefundRequest> req = new ArrayList<>();
        Model.Ticket t = ticket("A", Model.PAID);
        assertEquals(75_000, RefundProcedures.requestRefund(t, event(), "buyer", START - 7 * DAY, h, req));
        assertEquals(Model.REFUNDED, t.state);
        assertEquals(1, h.size());
        assertEquals(1, req.size());
        Model.RefundRequest r = req.get(0);
        assertEquals("A", r.ticketCode);
        assertEquals(START - 7 * DAY, r.requestedAtSec);
        assertEquals(RefundProcedures.BASIS_VIEWER, r.basis);
        assertTrue(r.approved);
        assertEquals(50, r.percent);
        assertEquals(75_000, r.amountKop);
    }

    @Test
    public void ticketOfAnotherEventIsNotRefunded() {
        List<Model.Transition> h = new ArrayList<>();
        List<Model.RefundRequest> req = new ArrayList<>();
        Model.Ticket t = ticket("A", Model.PAID);               // билет на E1
        assertEquals(RefundProcedures.REFUND_DENIED,
                RefundProcedures.requestRefund(t, event("E2"), "buyer", START - 20 * DAY, h, req));
        assertEquals(Model.PAID, t.state);
        assertEquals(0, h.size());
        assertEquals(1, req.size());
        assertFalse(req.get(0).approved);
        assertEquals(0, req.get(0).amountKop);
    }

    @Test
    public void recordSurvivesLaterStateChange() {
        List<Model.Transition> h = new ArrayList<>();
        List<Model.RefundRequest> req = new ArrayList<>();
        Model.Event e = event();
        Model.Ticket t = ticket("A", Model.PAID);
        RefundProcedures.requestRefund(t, e, "buyer", START - 2 * DAY, h, req);   // отказ: поздно
        RefundProcedures.cancelEvent(e, Arrays.asList(t), START - DAY, h, req);   // затем отмена
        assertEquals(Model.ANNULLED, t.state);
        assertEquals(2, req.size());
        assertFalse(req.get(0).approved);
        assertEquals("до начала менее 3 суток", req.get(0).decision);
        assertEquals(RefundProcedures.BASIS_CANCEL, req.get(1).basis);
        assertEquals(100, req.get(1).percent);
    }

    @Test
    public void onlyBuyerCanRequestRefund() {
        Model.Ticket t = ticket("A", Model.PAID);
        assertEquals(RefundProcedures.REFUND_DENIED, RefundProcedures.requestRefund(t, event(), "stranger",
                START - 20 * DAY, new ArrayList<>(), new ArrayList<>()));
        assertEquals(Model.PAID, t.state);
    }

    @Test
    public void lateRefundIsDeniedAndTicketStaysValid() {
        List<Model.Transition> h = new ArrayList<>();
        Model.Ticket t = ticket("A", Model.PAID);
        assertEquals(RefundProcedures.REFUND_DENIED,
                RefundProcedures.requestRefund(t, event(), "buyer", START - 2 * DAY, h, new ArrayList<>()));
        assertEquals(Model.PAID, t.state);
        assertEquals(0, h.size());
    }

    @Test
    public void usedTicketCannotBeRefunded() {
        Model.Ticket t = ticket("A", Model.USED);
        assertEquals(RefundProcedures.REFUND_DENIED, RefundProcedures.requestRefund(t, event(), "buyer",
                START - 20 * DAY, new ArrayList<>(), new ArrayList<>()));
    }

    @Test
    public void cancellationAnnulsPaidAndCancelsReservations() {
        Model.Event e = event();
        Model.Ticket paid = ticket("P", Model.PAID);
        Model.Ticket used = ticket("U", Model.USED);
        used.sessionActive = true;
        Model.Ticket res = ticket("R", Model.RESERVED);
        Model.Ticket ref = ticket("F", Model.REFUNDED);
        Model.Ticket other = ticket("O", Model.PAID);
        other.eventId = "E2";
        List<Model.Transition> h = new ArrayList<>();
        List<Model.RefundRequest> req = new ArrayList<>();
        long total = RefundProcedures.cancelEvent(e, Arrays.asList(paid, used, res, ref, other),
                START - DAY, h, req);
        assertEquals(300_000, total);
        assertEquals(Model.ANNULLED, paid.state);
        assertEquals(Model.ANNULLED, used.state);
        assertFalse(used.sessionActive);
        assertEquals(Model.CANCELLED, res.state);   // бронь при отмене концерта отменяется
        assertEquals(Model.REFUNDED, ref.state);
        assertEquals(Model.PAID, other.state);
        assertEquals(3, h.size());
        assertEquals(2, req.size());                // записи о возврате только по оплаченным
    }
}
