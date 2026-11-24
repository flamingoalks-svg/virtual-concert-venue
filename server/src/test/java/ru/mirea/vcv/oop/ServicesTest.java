package ru.mirea.vcv.oop;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.Test;
import ru.mirea.vcv.oop.admission.AdmissionResult;
import ru.mirea.vcv.oop.admission.AdmissionRules;
import ru.mirea.vcv.oop.admission.AdmissionService;
import ru.mirea.vcv.oop.admission.BlockedViewerRule;
import ru.mirea.vcv.oop.event.Event;
import ru.mirea.vcv.oop.hall.Entry;
import ru.mirea.vcv.oop.hall.HallEntrance;
import ru.mirea.vcv.oop.hall.Session;
import ru.mirea.vcv.oop.playback.DriftCorrector;
import ru.mirea.vcv.oop.playback.PlaybackPhase;
import ru.mirea.vcv.oop.playback.PlaybackSchedule;
import ru.mirea.vcv.oop.refund.RefundBasis;
import ru.mirea.vcv.oop.refund.RefundRequest;
import ru.mirea.vcv.oop.refund.RefundService;
import ru.mirea.vcv.oop.ticket.InMemoryTicketRepository;
import ru.mirea.vcv.oop.ticket.Ticket;
import ru.mirea.vcv.oop.ticket.TicketRepository;
import ru.mirea.vcv.oop.ticket.TicketState;

public class ServicesTest {

    private static final Instant START = Instant.parse("2026-12-10T16:00:00Z");

    private final TicketRepository repo = new InMemoryTicketRepository();
    private final Event event = new Event("E1", "Концерт", START, Duration.ofMinutes(90), 2);

    private Ticket paid(String code) {
        Ticket t = new Ticket(code, event, "buyer-" + code, 150_000);
        t.pay(START.minus(Duration.ofDays(30)));
        repo.save(t);
        return t;
    }

    @Test
    public void refundUsesTicketsOwnEventAndIsJournaled() {
        RefundService refunds = RefundService.statutory();
        Ticket t = paid("A");
        RefundRequest r = refunds.requestRefund(t, "buyer-A", START.minus(Duration.ofDays(7)));
        assertTrue(r.isApproved());
        assertEquals(50, r.getPercent());
        assertEquals(75_000, r.getAmountKop());
        assertEquals(TicketState.REFUNDED, t.getState());
        assertEquals(1, refunds.getJournal().size());
    }

    @Test
    public void lateRefundIsRejectedAndTicketStaysPaid() {
        RefundService refunds = RefundService.statutory();
        Ticket t = paid("A");
        RefundRequest r = refunds.requestRefund(t, "buyer-A", START.minus(Duration.ofDays(2)));
        assertFalse(r.isApproved());
        assertEquals("до начала менее 3 суток", r.getDecision());
        assertEquals(TicketState.PAID, t.getState());
    }

    @Test
    public void cancellationAnnulsPaidAndCancelsReservation() {
        RefundService refunds = RefundService.statutory();
        Ticket p = paid("P");
        Ticket reserved = new Ticket("R", event, "buyer-R", 150_000);
        repo.save(reserved);
        long total = refunds.cancelEvent(event, repo, START.minus(Duration.ofDays(1)));
        assertEquals(150_000, total);
        assertEquals(TicketState.ANNULLED, p.getState());
        assertEquals(TicketState.CANCELLED, reserved.getState());
        assertEquals(RefundBasis.EVENT_CANCELLATION, refunds.getJournal().get(0).getBasis());
    }

    @Test
    public void entranceBindsChosenInstanceToSession() {
        AdmissionService admission = new AdmissionService(repo, AdmissionRules.standard());
        HallEntrance entrance = new HallEntrance(event, repo, admission);
        paid("A");
        paid("B");
        paid("C");
        entrance.enter("A", "v1", START);
        entrance.enter("B", "v2", START);
        Entry c = entrance.enter("C", "v3", START);
        assertEquals(AdmissionResult.ADMITTED, c.getResult());
        assertEquals(2, c.getSession().get().getInstance().getNumber());
        Session a = entrance.getSessions().get(0);
        a.close(START.plus(Duration.ofMinutes(10)));
        assertEquals(1, a.getInstance().getViewers());
        Entry again = entrance.enter("A", "v1", START.plus(Duration.ofMinutes(11)));
        assertEquals(AdmissionResult.READMITTED, again.getResult());
        assertSame(a.getInstance(), again.getSession().get().getInstance());
    }

    @Test
    public void deniedEntryOpensNoSession() {
        AdmissionService admission = new AdmissionService(repo, AdmissionRules.standard());
        HallEntrance entrance = new HallEntrance(event, repo, admission);
        Entry e = entrance.enter("X", "v1", START);
        assertEquals(AdmissionResult.NOT_FOUND, e.getResult());
        assertFalse(e.getSession().isPresent());
        assertTrue(entrance.getHall().getInstances().isEmpty());
    }

    @Test
    public void newRuleIsAddedWithoutChangingService() {
        AdmissionService admission = new AdmissionService(repo, AdmissionRules.standard());
        BlockedViewerRule stop = new BlockedViewerRule();
        admission.addRule(stop);
        paid("A");
        stop.block("v1");
        assertEquals(AdmissionResult.BLOCKED, admission.admit(event, "A", "v1", START));
        assertEquals(TicketState.PAID, repo.findByCode("A").get().getState());
    }

    @Test
    public void playbackPositionAndSeek() {
        PlaybackSchedule s = new PlaybackSchedule(event);
        Instant now = START.plusMillis(754_000);
        assertEquals(PlaybackPhase.PLAYING, s.phaseAt(now));
        assertEquals(Duration.ofMillis(754_000), s.positionAt(now));
        assertEquals(Duration.ZERO, s.positionAt(START.minus(Duration.ofMinutes(5))));
        DriftCorrector c = new DriftCorrector(s, DriftCorrector.DEFAULT_THRESHOLD);
        assertTrue(c.seekTarget(Duration.ofMillis(751_500), now).isPresent());
        assertFalse(c.seekTarget(Duration.ofMillis(753_200), now).isPresent());
    }
}
