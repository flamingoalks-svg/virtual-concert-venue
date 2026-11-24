package ru.mirea.vcv.procedural;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Демонстрация работы процедурной реализации на контрольном примере.
 * Запуск: метод main из IntelliJ IDEA или команды из раздела README «Как проверить работу».
 */
public final class ProceduralDemo {

    private ProceduralDemo() {
    }

    private static final ZoneId MSK = ZoneId.of("Europe/Moscow");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final long HOUR = 60 * 60;
    private static final long DAY = RefundProcedures.DAY_SEC;

    public static void main(String[] args) {
        List<Model.Transition> history = new ArrayList<>();
        List<Model.RefundRequest> requests = new ArrayList<>();
        List<Model.Session> sessions = new ArrayList<>();

        Model.Event ev = event("E1", "Вечер камерной музыки",
                Instant.parse("2026-12-10T16:00:00Z").getEpochSecond(), 90 * 60);
        ev.instanceCapacity = 2;
        Model.Event other = event("E2", "Джазовый вечер",
                Instant.parse("2026-12-20T16:00:00Z").getEpochSecond(), 120 * 60);
        System.out.println("Мероприятие: " + ev.title + ", начало " + time(ev.startEpochSec)
                + ", длительность записи " + ev.durationSec / 60 + " мин");

        // ---------- Алгоритм 2: возврат билета зрителем ----------
        System.out.println();
        System.out.println("== Алгоритм 2. Возврат билета (цена 1500,00 руб.) ==");
        Model.Ticket probe = ticket("T-000", ev.id, Model.PAID, 150_000);
        long denied = RefundProcedures.requestRefund(probe, ev, "someone-else",
                ev.startEpochSec - 20 * DAY, history, requests);
        System.out.println("T-000: заявка не от покупателя -> "
                + (denied == RefundProcedures.REFUND_DENIED ? "в возврате отказано" : rub(denied))
                + ", состояние «" + Model.STATE_NAMES[probe.state] + "»");
        long wrong = RefundProcedures.requestRefund(probe, other, probe.buyerId,
                ev.startEpochSec - 20 * DAY, history, requests);
        System.out.println("T-000: билет на E1, заявка по E2 -> "
                + (wrong == RefundProcedures.REFUND_DENIED ? "в возврате отказано" : rub(wrong))
                + ", состояние «" + Model.STATE_NAMES[probe.state] + "»");
        long[] daysBefore = {12, 7, 4, 2};
        List<Model.Ticket> all = new ArrayList<>();
        for (int i = 0; i < daysBefore.length; i++) {
            Model.Ticket t = ticket("T-00" + (i + 1), ev.id, Model.PAID, 150_000);
            all.add(t);
            long now = ev.startEpochSec - daysBefore[i] * DAY;
            long amount = RefundProcedures.requestRefund(t, ev, t.buyerId, now, history, requests);
            System.out.printf("%s: заявка за %d дн. -> %s, состояние «%s»%n", t.code, daysBefore[i],
                    amount == RefundProcedures.REFUND_DENIED ? "в возврате отказано"
                            : "к возврату " + rub(amount),
                    Model.STATE_NAMES[t.state]);
        }

        // ---------- Алгоритм 1: проверка и гашение билета ----------
        System.out.println();
        System.out.println("== Алгоритм 1. Проверка и гашение билета ==");
        Model.Ticket t5 = ticket("T-005", ev.id, Model.PAID, 150_000);
        all.add(t5);
        Map<String, Model.Ticket> byCode = new HashMap<>();
        for (Model.Ticket t : all) {
            byCode.put(t.code, t);
        }
        long open = ev.startEpochSec - TicketProcedures.OPEN_BEFORE_SEC;
        admit(byCode, ev, "T-005", "viewer-1", open - 10 * 60, history);
        admit(byCode, ev, "T-005", "viewer-1", ev.startEpochSec - 10 * 60, history);
        admit(byCode, ev, "T-005", "viewer-1", ev.startEpochSec - 5 * 60, history);
        admit(byCode, ev, "T-005", "viewer-2", ev.startEpochSec - 4 * 60, history);
        TicketProcedures.endSession(t5);
        System.out.println("  (сеанс viewer-1 закрыт: разрыв связи)");
        admit(byCode, ev, "T-005", "viewer-1", ev.startEpochSec + 15 * 60, history);
        admit(byCode, ev, "T-002", "viewer-3", ev.startEpochSec + 16 * 60, history);
        admit(byCode, ev, "X-999", "viewer-4", ev.startEpochSec + 17 * 60, history);
        admit(byCode, ev, "T-004", "viewer-5", ev.startEpochSec + 2 * HOUR, history);

        // ---------- Вход в зал: экземпляр закрепляется за сеансом ----------
        System.out.println();
        System.out.println("== Вход в зал (алгоритмы 1 и 3, вместимость экземпляра 2) ==");
        List<Model.HallInstance> rooms = new ArrayList<>();
        String[] codes = {"T-006", "T-007", "T-008"};
        for (int i = 0; i < codes.length; i++) {
            Model.Ticket t = ticket(codes[i], ev.id, Model.PAID, 150_000);
            byCode.put(t.code, t);
        }
        long enter = ev.startEpochSec - 5 * 60;
        for (int i = 0; i < codes.length; i++) {
            enter(byCode, ev, codes[i], "viewer-" + (6 + i), enter + i * 60, history, rooms, sessions);
        }
        enter(byCode, ev, "T-006", "viewer-6", enter + 4 * 60, history, rooms, sessions);
        Model.Session first = sessions.get(0);
        SessionProcedures.leaveHall(first, byCode.get(first.ticketCode), rooms, ev.startEpochSec + 20 * 60);
        System.out.println(time(first.endSec) + " " + first.ticketCode + ", " + first.viewerId
                + " вышел из экземпляра " + first.instanceNumber + ": " + occupancy(rooms));
        enter(byCode, ev, "T-006", "viewer-6", ev.startEpochSec + 25 * 60, history, rooms, sessions);
        System.out.println("Сеансы:");
        for (Model.Session s : sessions) {
            System.out.printf("  %s, %s: экземпляр %d, вход %s, выход %s%n", s.ticketCode, s.viewerId,
                    s.instanceNumber, time(s.startSec), s.endSec < 0 ? "сеанс открыт" : time(s.endSec));
        }

        // ---------- Алгоритм 3: распределение по экземплярам ----------
        System.out.println();
        System.out.println("== Алгоритм 3. Распределение зрителей (вместимость экземпляра 50) ==");
        List<Model.HallInstance> halls = new ArrayList<>();
        for (int i = 0; i < 120; i++) {
            InstanceProcedures.assignViewer(halls, 50);
        }
        System.out.println("Вошли 120 зрителей: " + occupancy(halls));
        for (int i = 0; i < 3; i++) {
            InstanceProcedures.releaseViewer(halls, 1);
        }
        System.out.println("3 зрителя вышли из экземпляра 1: " + occupancy(halls));
        int n = InstanceProcedures.assignViewer(halls, 50);
        System.out.println("Новый зритель направлен в экземпляр " + n + ": " + occupancy(halls)
                + ", всего зрителей " + InstanceProcedures.totalViewers(halls));

        // ---------- Алгоритм 4: синхронный показ ----------
        System.out.println();
        System.out.println("== Алгоритм 4. Синхронизация показа ==");
        long startMs = ev.startEpochSec * 1000;
        long durMs = ev.durationSec * 1000;
        long[] nowOffsets = {-5 * 60_000, 754_000, durMs + 60_000};
        for (long off : nowOffsets) {
            long now = startMs + off;
            System.out.printf("Время сервера %s: %s, позиция записи %s%n", time(now / 1000),
                    PlaybackProcedures.PHASE_NAMES[PlaybackProcedures.phase(now, startMs, durMs)],
                    mmss(PlaybackProcedures.playbackPosition(now, startMs, durMs)));
        }
        long server = PlaybackProcedures.playbackPosition(startMs + 754_000, startMs, durMs);
        long[] clientPositions = {751_500, 753_200};
        for (long client : clientPositions) {
            long target = PlaybackProcedures.seekTarget(client, server,
                    PlaybackProcedures.SEEK_THRESHOLD_MS);
            System.out.printf("Клиент на %s, сервер на %s -> %s%n", mmss(client), mmss(server),
                    target == PlaybackProcedures.NO_SEEK ? "перемотка не нужна"
                            : "перемотать на " + mmss(target));
        }

        // ---------- Алгоритм 2: отмена мероприятия ----------
        System.out.println();
        System.out.println("== Алгоритм 2. Отмена мероприятия организатором ==");
        Model.Event ev2 = other;
        List<Model.Ticket> t2 = new ArrayList<>();
        t2.add(ticket("J-001", ev2.id, Model.PAID, 120_000));
        t2.add(ticket("J-002", ev2.id, Model.PAID, 120_000));
        t2.add(ticket("J-003", ev2.id, Model.RESERVED, 120_000));
        t2.add(ticket("J-004", ev2.id, Model.REFUNDED, 120_000));
        long total = RefundProcedures.cancelEvent(ev2, t2, ev2.startEpochSec - 2 * DAY, history, requests);
        for (Model.Ticket t : t2) {
            System.out.println(t.code + ": «" + Model.STATE_NAMES[t.state] + "»");
        }
        System.out.println("Итого к возврату: " + rub(total));

        // ---------- Журнал переходов ----------
        System.out.println();
        System.out.println("== Журнал переходов состояний билетов ==");
        for (Model.Transition tr : history) {
            System.out.printf("%s %s: %s -> %s (%s)%n", time(tr.atEpochSec), tr.ticketCode,
                    Model.STATE_NAMES[tr.fromState], Model.STATE_NAMES[tr.toState], tr.reason);
        }

        // ---------- Журнал заявок на возврат ----------
        System.out.println();
        System.out.println("== Заявки на возврат ==");
        for (Model.RefundRequest r : requests) {
            System.out.printf("%s %s, %s, основание «%s»: %s%n", time(r.requestedAtSec), r.ticketCode,
                    r.requesterId, r.basis,
                    r.approved ? r.decision + ", " + rub(r.amountKop) : "отказ — " + r.decision);
        }
    }

    // ---------- вспомогательные процедуры ----------

    private static Model.Event event(String id, String title, long start, long duration) {
        Model.Event e = new Model.Event();
        e.id = id;
        e.title = title;
        e.startEpochSec = start;
        e.durationSec = duration;
        return e;
    }

    private static Model.Ticket ticket(String code, String eventId, int state, long priceKop) {
        Model.Ticket t = new Model.Ticket();
        t.code = code;
        t.eventId = eventId;
        t.state = state;
        t.priceKop = priceKop;
        t.buyerId = "buyer-" + code;
        return t;
    }

    private static void admit(Map<String, Model.Ticket> byCode, Model.Event ev, String code,
                              String viewer, long now, List<Model.Transition> history) {
        int r = TicketProcedures.admit(byCode, ev, code, viewer, now, history);
        System.out.printf("%s %s, %s -> %s%n", time(now), code, viewer,
                TicketProcedures.RESULT_NAMES[r]);
    }

    private static void enter(Map<String, Model.Ticket> byCode, Model.Event ev, String code, String viewer,
                              long now, List<Model.Transition> history, List<Model.HallInstance> rooms,
                              List<Model.Session> sessions) {
        Model.Session s = SessionProcedures.enterHall(byCode, ev, code, viewer, now, history, rooms, sessions);
        System.out.printf("%s %s, %s -> %s%n", time(now), code, viewer,
                s == null ? "во входе отказано" : "сеанс в экземпляре " + s.instanceNumber + " (" + occupancy(rooms) + ")");
    }

    private static String occupancy(List<Model.HallInstance> halls) {
        StringBuilder sb = new StringBuilder();
        for (Model.HallInstance h : halls) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append('№').append(h.number).append(" — ").append(h.viewers);
        }
        return sb.toString();
    }

    private static String time(long epochSec) {
        return FMT.format(Instant.ofEpochSecond(epochSec).atZone(MSK));
    }

    private static String rub(long kop) {
        return String.format("%d,%02d руб.", kop / 100, kop % 100);
    }

    private static String mmss(long ms) {
        long s = ms / 1000;
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}
