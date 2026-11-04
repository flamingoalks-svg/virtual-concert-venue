package ru.mirea.vcv.procedural;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class SessionProceduresTest {

    private static final long START = 1_000_000;

    private static Model.Event event() {
        Model.Event e = new Model.Event();
        e.id = "E1";
        e.startEpochSec = START;
        e.durationSec = 3600;
        e.instanceCapacity = 2;
        return e;
    }

    private static Map<String, Model.Ticket> tickets(String... codes) {
        Map<String, Model.Ticket> m = new HashMap<>();
        for (String c : codes) {
            Model.Ticket t = new Model.Ticket();
            t.code = c;
            t.eventId = "E1";
            t.state = Model.PAID;
            t.priceKop = 100_000;
            t.buyerId = "b-" + c;
            m.put(c, t);
        }
        return m;
    }

    @Test
    public void chosenInstanceIsStoredInSession() {
        Map<String, Model.Ticket> byCode = tickets("A", "B", "C");
        List<Model.HallInstance> halls = new ArrayList<>();
        List<Model.Session> sessions = new ArrayList<>();
        List<Model.Transition> h = new ArrayList<>();
        Model.Event e = event();
        assertEquals(1, SessionProcedures.enterHall(byCode, e, "A", "v1", START, h, halls, sessions).instanceNumber);
        assertEquals(1, SessionProcedures.enterHall(byCode, e, "B", "v2", START, h, halls, sessions).instanceNumber);
        Model.Session c = SessionProcedures.enterHall(byCode, e, "C", "v3", START, h, halls, sessions);
        assertEquals(2, c.instanceNumber);          // первый экземпляр заполнен
        assertEquals("C", c.ticketCode);
        assertEquals("v3", c.viewerId);
        assertEquals(-1, c.endSec);
        assertEquals(3, sessions.size());
        assertEquals(Model.USED, byCode.get("C").state);
    }

    @Test
    public void deniedEntryCreatesNoSession() {
        Map<String, Model.Ticket> byCode = tickets("A");
        List<Model.HallInstance> halls = new ArrayList<>();
        List<Model.Session> sessions = new ArrayList<>();
        assertNull(SessionProcedures.enterHall(byCode, event(), "X", "v1", START, new ArrayList<>(), halls, sessions));
        assertEquals(0, sessions.size());
        assertEquals(0, halls.size());
    }

    @Test
    public void leavingFreesSeatOfBoundInstance() {
        Map<String, Model.Ticket> byCode = tickets("A", "B", "C");
        List<Model.HallInstance> halls = new ArrayList<>();
        List<Model.Session> sessions = new ArrayList<>();
        List<Model.Transition> h = new ArrayList<>();
        Model.Event e = event();
        Model.Session a = SessionProcedures.enterHall(byCode, e, "A", "v1", START, h, halls, sessions);
        SessionProcedures.enterHall(byCode, e, "B", "v2", START, h, halls, sessions);
        SessionProcedures.enterHall(byCode, e, "C", "v3", START, h, halls, sessions);
        SessionProcedures.leaveHall(a, byCode.get("A"), halls, START + 600);
        assertEquals(START + 600, a.endSec);
        assertEquals(1, halls.get(0).viewers);
        Model.Session again = SessionProcedures.enterHall(byCode, e, "A", "v1", START + 700, h, halls, sessions);
        assertEquals(1, again.instanceNumber);     // повторный вход — новый сеанс
        assertEquals(4, sessions.size());
    }
}
