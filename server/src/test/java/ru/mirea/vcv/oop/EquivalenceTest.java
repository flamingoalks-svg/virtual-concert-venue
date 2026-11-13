package ru.mirea.vcv.oop;

import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;
import ru.mirea.vcv.oop.admission.AdmissionRules;
import ru.mirea.vcv.oop.admission.AdmissionService;
import ru.mirea.vcv.oop.event.Event;
import ru.mirea.vcv.oop.hall.HallEntrance;
import ru.mirea.vcv.oop.refund.StatutoryRefundPolicy;
import ru.mirea.vcv.oop.ticket.InMemoryTicketRepository;
import ru.mirea.vcv.oop.ticket.Ticket;
import ru.mirea.vcv.procedural.InstanceProcedures;
import ru.mirea.vcv.procedural.Model;
import ru.mirea.vcv.procedural.RefundProcedures;
import ru.mirea.vcv.procedural.TicketProcedures;

/**
 * Процедурная и объектно-ориентированная реализации дают одинаковые результаты
 * на одних и тех же входных данных.
 */
public class EquivalenceTest {

    private static final long START = Instant.parse("2026-12-10T16:00:00Z").getEpochSecond();

    @Test
    public void refundPercentIsTheSame() {
        StatutoryRefundPolicy policy = new StatutoryRefundPolicy();
        for (long s = 0; s <= 12 * RefundProcedures.DAY_SEC; s += 3600) {
            assertEquals(RefundProcedures.refundPercent(s), policy.percent(Duration.ofSeconds(s)));
        }
    }

    @Test
    public void admissionDecisionsAreTheSame() {
        Model.Event pe = new Model.Event();
        pe.id = "E1";
        pe.startEpochSec = START;
        pe.durationSec = 90 * 60;
        Event oe = new Event("E1", "Концерт", Instant.ofEpochSecond(START), Duration.ofMinutes(90), 50);
        Map<String, Model.Ticket> byCode = new HashMap<>();
        InMemoryTicketRepository repo = new InMemoryTicketRepository();
        for (int i = 0; i < 5; i++) {
            Model.Ticket pt = new Model.Ticket();
            pt.code = "T" + i;
            pt.eventId = "E1";
            pt.state = Model.PAID;
            pt.buyerId = "b";
            byCode.put(pt.code, pt);
            Ticket ot = new Ticket("T" + i, oe, "b", 0);
            ot.pay(Instant.ofEpochSecond(START - 86_400));
            repo.save(ot);
        }
        AdmissionService admission = new AdmissionService(repo, AdmissionRules.standard());
        Random rnd = new Random(52);
        List<Model.Transition> h = new ArrayList<>();
        for (int k = 0; k < 2000; k++) {
            String code = "T" + rnd.nextInt(6);                        // T5 не существует
            String viewer = "v" + rnd.nextInt(3);
            long now = START - 3600 + rnd.nextInt(3 * 3600);
            int p = TicketProcedures.admit(byCode, pe, code, viewer, now, h);
            String o = admission.admit(oe, code, viewer, Instant.ofEpochSecond(now)).name();
            assertEquals("шаг " + k, TicketProcedures.RESULT_NAMES[p],
                    ru.mirea.vcv.oop.admission.AdmissionResult.valueOf(o).getTitle());
            if (rnd.nextInt(4) == 0 && byCode.containsKey(code)) {          // разрыв связи
                TicketProcedures.endSession(byCode.get(code));
                repo.findByCode(code).get().endSession();
            }
        }
    }

    @Test
    public void instanceAllocationIsTheSame() {
        List<Model.HallInstance> rooms = new ArrayList<>();
        Event oe = new Event("E1", "Концерт", Instant.ofEpochSecond(START), Duration.ofMinutes(90), 50);
        InMemoryTicketRepository repo = new InMemoryTicketRepository();
        HallEntrance entrance = new HallEntrance(oe, repo, new AdmissionService(repo, AdmissionRules.standard()));
        for (int i = 0; i < 230; i++) {
            Ticket t = new Ticket("V" + i, oe, "b", 0);
            t.pay(Instant.ofEpochSecond(START - 86_400));
            repo.save(t);
            int p = InstanceProcedures.assignViewer(rooms, 50);
            int o = entrance.enter("V" + i, "f" + i, Instant.ofEpochSecond(START)).getSession().get()
                    .getInstance().getNumber();
            assertEquals(p, o);
        }
    }
}
