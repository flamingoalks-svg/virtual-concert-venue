package ru.mirea.vcv.oop;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import ru.mirea.vcv.oop.admission.AdmissionResult;
import ru.mirea.vcv.oop.admission.AdmissionRules;
import ru.mirea.vcv.oop.admission.AdmissionService;
import ru.mirea.vcv.oop.admission.BlockedViewerRule;
import ru.mirea.vcv.oop.event.Event;
import ru.mirea.vcv.oop.hall.Entry;
import ru.mirea.vcv.oop.hall.HallEntrance;
import ru.mirea.vcv.oop.hall.HallInstance;
import ru.mirea.vcv.oop.hall.Session;
import ru.mirea.vcv.oop.playback.DriftCorrector;
import ru.mirea.vcv.oop.playback.PlaybackSchedule;
import ru.mirea.vcv.oop.refund.RefundRequest;
import ru.mirea.vcv.oop.refund.RefundService;
import ru.mirea.vcv.oop.ticket.IllegalTicketStateException;
import ru.mirea.vcv.oop.ticket.InMemoryTicketRepository;
import ru.mirea.vcv.oop.ticket.StateTransition;
import ru.mirea.vcv.oop.ticket.Ticket;
import ru.mirea.vcv.oop.ticket.TicketRepository;

/**
 * Демонстрация объектно-ориентированной реализации на том же контрольном примере,
 * что и ProceduralDemo, и на новом требовании (стоп-лист зрителей).
 */
public final class OopDemo {

    private OopDemo() {
    }

    private static final ZoneId MSK = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final Instant SALE = Instant.parse("2026-11-01T09:00:00Z");

    public static void main(String[] args) {
        TicketRepository tickets = new InMemoryTicketRepository();
        RefundService refunds = RefundService.statutory();
        AdmissionService admission = new AdmissionService(tickets, AdmissionRules.standard());

        Event ev = new Event("E1", "Вечер камерной музыки", Instant.parse("2026-12-10T16:00:00Z"),
                Duration.ofMinutes(90), 2);
        Event other = new Event("E2", "Джазовый вечер", Instant.parse("2026-12-20T16:00:00Z"),
                Duration.ofMinutes(120), 50);
        System.out.println("Мероприятие: " + ev.getTitle() + ", начало " + time(ev.getStart())
                + ", длительность записи " + ev.getDuration().toMinutes() + " мин");

        // ---------- А2: возврат билета зрителем ----------
        System.out.println();
        System.out.println("== А2. Возврат билета (цена 1500,00 руб.) ==");
        Ticket probe = paid(tickets, "T-000", ev, 150_000);
        RefundRequest r0 = refunds.requestRefund(probe, "someone-else", ev.getStart().minus(Duration.ofDays(20)));
        System.out.println("T-000: заявка не от покупателя -> " + outcome(r0) + ", состояние «"
                + probe.getState().getTitle() + "»");
        int[] daysBefore = {12, 7, 4, 2};
        for (int i = 0; i < daysBefore.length; i++) {
            Ticket t = paid(tickets, "T-00" + (i + 1), ev, 150_000);
            RefundRequest r = refunds.requestRefund(t, t.getBuyerId(),
                    ev.getStart().minus(Duration.ofDays(daysBefore[i])));
            System.out.printf("%s: заявка за %d дн. -> %s, состояние «%s»%n", t.getCode(), daysBefore[i],
                    outcome(r), t.getState().getTitle());
        }

        // ---------- А1: проверка и гашение билета ----------
        System.out.println();
        System.out.println("== А1. Проверка и гашение билета ==");
        Ticket t5 = paid(tickets, "T-005", ev, 150_000);
        Instant open = ev.getStart().minus(Event.HALL_OPENS_BEFORE);
        admit(admission, ev, "T-005", "viewer-1", open.minus(Duration.ofMinutes(10)));
        admit(admission, ev, "T-005", "viewer-1", ev.getStart().minus(Duration.ofMinutes(10)));
        admit(admission, ev, "T-005", "viewer-1", ev.getStart().minus(Duration.ofMinutes(5)));
        admit(admission, ev, "T-005", "viewer-2", ev.getStart().minus(Duration.ofMinutes(4)));
        t5.endSession();
        System.out.println("  (сеанс viewer-1 закрыт: разрыв связи)");
        admit(admission, ev, "T-005", "viewer-1", ev.getStart().plus(Duration.ofMinutes(15)));
        admit(admission, ev, "T-002", "viewer-3", ev.getStart().plus(Duration.ofMinutes(16)));
        admit(admission, ev, "X-999", "viewer-4", ev.getStart().plus(Duration.ofMinutes(17)));
        admit(admission, ev, "T-004", "viewer-5", ev.getStart().plus(Duration.ofHours(2)));

        // ---------- А1 + А3: вход в зал, экземпляр закрепляется за сеансом ----------
        System.out.println();
        System.out.println("== Вход в зал (А1 и А3, вместимость экземпляра 2) ==");
        HallEntrance entrance = new HallEntrance(ev, tickets, admission);
        String[] codes = {"T-006", "T-007", "T-008"};
        for (String c : codes) {
            paid(tickets, c, ev, 150_000);
        }
        Instant enter = ev.getStart().minus(Duration.ofMinutes(5));
        for (int i = 0; i < codes.length; i++) {
            enter(entrance, codes[i], "viewer-" + (6 + i), enter.plus(Duration.ofMinutes(i)));
        }
        enter(entrance, "T-006", "viewer-6", enter.plus(Duration.ofMinutes(4)));
        Session first = entrance.getSessions().get(0);
        first.close(ev.getStart().plus(Duration.ofMinutes(20)));
        System.out.println(time(first.getEndedAt()) + " " + first.getTicket().getCode() + ", "
                + first.getViewerId() + " вышел из экземпляра " + first.getInstance().getNumber() + ": "
                + entrance.getHall().occupancy());
        enter(entrance, "T-006", "viewer-6", ev.getStart().plus(Duration.ofMinutes(25)));
        System.out.println("Сеансы:");
        for (Session s : entrance.getSessions()) {
            System.out.printf("  %s, %s: экземпляр %d, вход %s, выход %s%n", s.getTicket().getCode(),
                    s.getViewerId(), s.getInstance().getNumber(), time(s.getStartedAt()),
                    s.isOpen() ? "сеанс открыт" : time(s.getEndedAt()));
        }

        // ---------- А3: распределение по экземплярам ----------
        System.out.println();
        System.out.println("== А3. Распределение зрителей (вместимость экземпляра 50) ==");
        Event big = new Event("E3", "Камерный оркестр", ev.getStart(), ev.getDuration(), 50);
        HallEntrance bigEntrance = new HallEntrance(big, tickets, admission);
        for (int i = 1; i <= 121; i++) {
            paid(tickets, String.format("V-%03d", i), big, 100_000);
        }
        for (int i = 1; i <= 120; i++) {
            bigEntrance.enter(String.format("V-%03d", i), "fan-" + i, ev.getStart());
        }
        System.out.println("Вошли 120 зрителей: " + bigEntrance.getHall().occupancy());
        for (int i = 0; i < 3; i++) {
            bigEntrance.getSessions().get(i).close(ev.getStart().plus(Duration.ofMinutes(10)));
        }
        System.out.println("3 зрителя вышли из экземпляра 1: " + bigEntrance.getHall().occupancy());
        Entry e121 = bigEntrance.enter("V-121", "fan-121", ev.getStart().plus(Duration.ofMinutes(11)));
        HallInstance h = e121.getSession().get().getInstance();
        System.out.println("Новый зритель направлен в экземпляр " + h.getNumber() + ": "
                + bigEntrance.getHall().occupancy() + ", всего зрителей " + bigEntrance.getHall().totalViewers());

        // ---------- А4: синхронный показ ----------
        System.out.println();
        System.out.println("== А4. Синхронизация показа ==");
        PlaybackSchedule schedule = new PlaybackSchedule(ev);
        Duration[] offsets = {Duration.ofMinutes(-5), Duration.ofMillis(754_000),
            ev.getDuration().plus(Duration.ofMinutes(1))};
        for (Duration off : offsets) {
            Instant now = ev.getStart().plus(off);
            System.out.printf("Время сервера %s: %s, позиция записи %s%n", time(now),
                    schedule.phaseAt(now).getTitle(), mmss(schedule.positionAt(now)));
        }
        Instant now = ev.getStart().plusMillis(754_000);
        DriftCorrector corrector = new DriftCorrector(schedule, DriftCorrector.DEFAULT_THRESHOLD);
        for (long client : new long[] {751_500, 753_200}) {
            Optional<Duration> target = corrector.seekTarget(Duration.ofMillis(client), now);
            System.out.printf("Клиент на %s, сервер на %s -> %s%n", mmss(Duration.ofMillis(client)),
                    mmss(schedule.positionAt(now)),
                    target.isPresent() ? "перемотать на " + mmss(target.get()) : "перемотка не нужна");
        }

        // ---------- А2: отмена мероприятия ----------
        System.out.println();
        System.out.println("== А2. Отмена мероприятия организатором ==");
        List<Ticket> t2 = new ArrayList<>();
        t2.add(paid(tickets, "J-001", other, 120_000));
        t2.add(paid(tickets, "J-002", other, 120_000));
        Ticket reserved = new Ticket("J-003", other, "buyer-J-003", 120_000);
        tickets.save(reserved);
        t2.add(reserved);
        Ticket returned = paid(tickets, "J-004", other, 120_000);
        refunds.requestRefund(returned, returned.getBuyerId(), other.getStart().minus(Duration.ofDays(15)));
        t2.add(returned);
        long total = refunds.cancelEvent(other, tickets, other.getStart().minus(Duration.ofDays(2)));
        for (Ticket t : t2) {
            System.out.println(t.getCode() + ": «" + t.getState().getTitle() + "»");
        }
        System.out.println("Итого к возврату: " + rub(total));

        // ---------- Защита состояния билета ----------
        System.out.println();
        System.out.println("== Недопустимый переход отклоняется самим билетом ==");
        try {
            tickets.findByCode("T-005").get().refund(100, ev.getStart());
        } catch (IllegalTicketStateException ex) {
            System.out.println(ex.getMessage());
        }

        // ---------- Новое требование: стоп-лист ----------
        System.out.println();
        System.out.println("== Новое требование: стоп-лист зрителей ==");
        BlockedViewerRule stopList = new BlockedViewerRule();
        admission.addRule(stopList);
        stopList.block("viewer-7");
        Session s7 = entrance.getSessions().get(1);
        s7.close(ev.getStart().plus(Duration.ofMinutes(30)));
        enter(entrance, "T-007", "viewer-7", ev.getStart().plus(Duration.ofMinutes(31)));

        // ---------- Журналы ----------
        System.out.println();
        System.out.println("== Журнал переходов состояний билетов (без оплаты при продаже) ==");
        List<StateTransition> all = new ArrayList<>();
        for (String code : new String[] {"T-000", "T-001", "T-002", "T-003", "T-004", "T-005", "T-006",
            "T-007", "T-008", "J-001", "J-002", "J-003", "J-004"}) {
            all.addAll(tickets.findByCode(code).get().getHistory());
        }
        all.sort(Comparator.comparing(StateTransition::getAt));
        for (StateTransition tr : all) {
            if (!tr.getReason().equals("оплата")) {
                System.out.printf("%s %s: %s -> %s (%s)%n", time(tr.getAt()), tr.getTicketCode(),
                        tr.getFrom().getTitle(), tr.getTo().getTitle(), tr.getReason());
            }
        }
        System.out.println();
        System.out.println("== Заявки на возврат ==");
        for (RefundRequest r : refunds.getJournal()) {
            System.out.printf("%s %s, %s, основание «%s»: %s%n", time(r.getRequestedAt()), r.getTicketCode(),
                    r.getRequesterId(), r.getBasis().getTitle(),
                    r.isApproved() ? r.getDecision() + ", " + rub(r.getAmountKop()) : "отказ — " + r.getDecision());
        }
    }

    private static Ticket paid(TicketRepository repo, String code, Event e, long priceKop) {
        Ticket t = new Ticket(code, e, "buyer-" + code, priceKop);
        t.pay(SALE);
        repo.save(t);
        return t;
    }

    private static String outcome(RefundRequest r) {
        return r.isApproved() ? "к возврату " + rub(r.getAmountKop()) : "в возврате отказано";
    }

    private static void admit(AdmissionService a, Event e, String code, String viewer, Instant now) {
        AdmissionResult r = a.admit(e, code, viewer, now);
        System.out.printf("%s %s, %s -> %s%n", time(now), code, viewer, r.getTitle());
    }

    private static void enter(HallEntrance entrance, String code, String viewer, Instant now) {
        Entry e = entrance.enter(code, viewer, now);
        System.out.printf("%s %s, %s -> %s%n", time(now), code, viewer, e.getSession().isPresent()
                ? "сеанс в экземпляре " + e.getSession().get().getInstance().getNumber() + " ("
                + entrance.getHall().occupancy() + ")"
                : e.getResult().getTitle());
    }

    private static String time(Instant t) {
        return FMT.format(t.atZone(MSK));
    }

    private static String rub(long kop) {
        return String.format("%d,%02d руб.", kop / 100, kop % 100);
    }

    private static String mmss(Duration d) {
        long s = d.getSeconds();
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
