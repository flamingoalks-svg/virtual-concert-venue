package ru.mirea.vcv.procedural;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public class TicketProceduresTest {

    private static final long START = 1_000_000;
    private Model.Event ev;
    private Map<String, Model.Ticket> byCode;
    private List<Model.Transition> history;

    @Before
    public void setUp() {
        ev = new Model.Event();
        ev.id = "E1";
        ev.startEpochSec = START;
        ev.durationSec = 3600;
        byCode = new HashMap<>();
        history = new ArrayList<>();
        add("PAID", Model.PAID);
        add("RES", Model.RESERVED);
        add("REF", Model.REFUNDED);
    }

    private void add(String code, int state) {
        Model.Ticket t = new Model.Ticket();
        t.code = code;
        t.eventId = "E1";
        t.state = state;
        t.priceKop = 100_000;
        byCode.put(code, t);
    }

    private int admit(String code, String viewer, long now) {
        return TicketProcedures.admit(byCode, ev, code, viewer, now, history);
    }

    @Test
    public void firstEntryRedeemsTicketAndWritesHistory() {
        assertEquals(TicketProcedures.ADMITTED, admit("PAID", "v1", START));
        Model.Ticket t = byCode.get("PAID");
        assertEquals(Model.USED, t.state);
        assertEquals("v1", t.viewerId);
        assertTrue(t.sessionActive);
        assertEquals(1, history.size());
        assertEquals(Model.PAID, history.get(0).fromState);
        assertEquals(Model.USED, history.get(0).toState);
    }

    @Test
    public void hallOpensExactlyThirtyMinutesBeforeStart() {
        long open = START - TicketProcedures.OPEN_BEFORE_SEC;
        assertEquals(TicketProcedures.TOO_EARLY, admit("PAID", "v1", open - 1));
        assertEquals(TicketProcedures.ADMITTED, admit("PAID", "v1", open));
    }

    @Test
    public void entryAfterEndIsDenied() {
        assertEquals(TicketProcedures.FINISHED, admit("PAID", "v1", START + 3600));
    }

    @Test
    public void secondSessionAndOtherViewerAreDenied() {
        admit("PAID", "v1", START);
        assertEquals(TicketProcedures.SESSION_ACTIVE, admit("PAID", "v1", START + 10));
        assertEquals(TicketProcedures.OTHER_VIEWER, admit("PAID", "v2", START + 20));
    }

    @Test
    public void reEntryAfterDisconnectIsAllowedWithoutNewRedeem() {
        admit("PAID", "v1", START);
        TicketProcedures.endSession(byCode.get("PAID"));
        assertEquals(TicketProcedures.READMITTED, admit("PAID", "v1", START + 600));
        assertEquals(1, history.size());
    }

    @Test
    public void unpaidRefundedUnknownAndForeignTicketsAreDenied() {
        assertEquals(TicketProcedures.NOT_VALID, admit("RES", "v1", START));
        assertEquals(TicketProcedures.NOT_VALID, admit("REF", "v1", START));
        assertEquals(TicketProcedures.NOT_FOUND, admit("NOPE", "v1", START));
        byCode.get("PAID").eventId = "E2";
        assertEquals(TicketProcedures.WRONG_EVENT, admit("PAID", "v1", START));
    }

    @Test
    public void cancelledEventDeniesEntry() {
        ev.cancelled = true;
        assertEquals(TicketProcedures.EVENT_CANCELLED, admit("PAID", "v1", START));
        assertFalse(byCode.get("PAID").sessionActive);
    }
}
