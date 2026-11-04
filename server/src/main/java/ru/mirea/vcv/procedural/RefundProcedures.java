package ru.mirea.vcv.procedural;

import java.util.List;

/**
 * Алгоритм 2. Возврат билета по инициативе зрителя и отмена мероприятия
 * организатором (автоматизируемая операция А2).
 *
 * <p>Доля возврата определяется по статье 52.1 Основ законодательства РФ о культуре:
 * не позднее чем за 10 дней — 100 %, менее 10, но не позднее 5 дней — 50 %,
 * менее 5, но не позднее 3 дней — 30 %, позднее — возврат не производится.
 * При отмене мероприятия возвращается полная стоимость.</p>
 *
 * <p>Каждая заявка — и удовлетворённая, и отклонённая — сохраняется отдельной записью
 * {@link Model.RefundRequest} с временем, основанием и результатом.</p>
 *
 * <p>Сложность: возврат одного билета — O(1); отмена мероприятия просматривает
 * каждый из n билетов один раз — O(n).</p>
 */
public final class RefundProcedures {

    private RefundProcedures() {
    }

    public static final long DAY_SEC = 24L * 60 * 60;

    /** Признак того, что возврат не выполнен. */
    public static final long REFUND_DENIED = -1;

    public static final String BASIS_VIEWER = "отказ зрителя";
    public static final String BASIS_CANCEL = "отмена мероприятия";
    public static final String ORGANIZER = "организатор";

    /**
     * Доля возврата в процентах в зависимости от времени, оставшегося до начала.
     *
     * @param secondsToStart сколько секунд осталось до начала мероприятия
     * @return 100, 50, 30 или 0
     */
    public static int refundPercent(long secondsToStart) {
        if (secondsToStart >= 10 * DAY_SEC) {
            return 100;
        }
        if (secondsToStart >= 5 * DAY_SEC) {
            return 50;
        }
        if (secondsToStart >= 3 * DAY_SEC) {
            return 30;
        }
        return 0;
    }

    /**
     * Возврат оплаченного билета по заявке зрителя.
     * Сначала проверяется, что билет относится к этому мероприятию, — только тогда срок
     * и право на возврат считаются по нужному мероприятию. Далее: заявку подал покупатель,
     * билет в состоянии «Оплачен», мероприятие не отменено и ещё не началось, доля возврата
     * больше нуля. При отказе билет остаётся в прежнем состоянии.
     *
     * @param requesterId кто подал заявку
     * @param requests    журнал заявок на возврат (дополняется в любом случае)
     * @return сумма к возврату в копейках или REFUND_DENIED
     */
    public static long requestRefund(Model.Ticket t, Model.Event event, String requesterId, long nowSec,
                                     List<Model.Transition> history, List<Model.RefundRequest> requests) {
        if (!t.eventId.equals(event.id)) {
            return reject(t, requesterId, nowSec, "билет относится к другому мероприятию", requests);
        }
        if (!requesterId.equals(t.buyerId)) {
            return reject(t, requesterId, nowSec, "заявитель не является покупателем", requests);
        }
        if (t.state != Model.PAID) {
            return reject(t, requesterId, nowSec,
                    "билет в состоянии «" + Model.STATE_NAMES[t.state] + "»", requests);
        }
        if (event.cancelled) {
            return reject(t, requesterId, nowSec, "мероприятие отменено, возврат выполнен при отмене", requests);
        }
        if (nowSec >= event.startEpochSec) {
            return reject(t, requesterId, nowSec, "мероприятие уже началось", requests);
        }
        int percent = refundPercent(event.startEpochSec - nowSec);
        if (percent == 0) {
            return reject(t, requesterId, nowSec, "до начала менее 3 суток", requests);
        }
        long amount = t.priceKop * percent / 100;   // округление вниз до копейки
        record(t, requesterId, nowSec, BASIS_VIEWER, true, percent, amount,
                "возврат " + percent + " %", requests);
        TicketProcedures.changeState(t, Model.REFUNDED, nowSec, "возврат по заявке, " + percent + " %", history);
        return amount;
    }

    /**
     * Отмена мероприятия организатором: оплаченные и использованные билеты аннулируются
     * с полным возвратом (по каждому создаётся запись о возврате), неоплаченные брони
     * переходят в состояние «Отменён», открытые сеансы закрываются.
     *
     * @param tickets все билеты (обрабатываются только билеты этого мероприятия)
     * @return общая сумма к возврату в копейках
     */
    public static long cancelEvent(Model.Event event, List<Model.Ticket> tickets, long nowSec,
                                   List<Model.Transition> history, List<Model.RefundRequest> requests) {
        event.cancelled = true;
        long total = 0;
        for (Model.Ticket t : tickets) {                       // n итераций
            if (!t.eventId.equals(event.id)) {
                continue;
            }
            if (t.state == Model.PAID || t.state == Model.USED) {
                total += t.priceKop;
                record(t, ORGANIZER, nowSec, BASIS_CANCEL, true, 100, t.priceKop, "возврат 100 %", requests);
                TicketProcedures.changeState(t, Model.ANNULLED, nowSec, "отмена мероприятия, 100 %", history);
            } else if (t.state == Model.RESERVED) {
                TicketProcedures.changeState(t, Model.CANCELLED, nowSec, "отмена мероприятия до оплаты", history);
            }
            t.sessionActive = false;
        }
        return total;
    }

    private static long reject(Model.Ticket t, String requesterId, long nowSec, String why,
                               List<Model.RefundRequest> requests) {
        record(t, requesterId, nowSec, BASIS_VIEWER, false, 0, 0, why, requests);
        return REFUND_DENIED;
    }

    private static void record(Model.Ticket t, String requesterId, long nowSec, String basis, boolean approved,
                               int percent, long amountKop, String decision, List<Model.RefundRequest> requests) {
        Model.RefundRequest r = new Model.RefundRequest();
        r.ticketCode = t.code;
        r.requesterId = requesterId;
        r.requestedAtSec = nowSec;
        r.basis = basis;
        r.approved = approved;
        r.percent = percent;
        r.amountKop = amountKop;
        r.decision = decision;
        requests.add(r);
    }
}
