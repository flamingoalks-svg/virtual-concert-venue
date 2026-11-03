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
 * <p>Сложность: возврат одного билета — O(1); отмена мероприятия просматривает
 * каждый из n билетов один раз — O(n).</p>
 */
public final class RefundProcedures {

    private RefundProcedures() {
    }

    public static final long DAY_SEC = 24L * 60 * 60;

    /** Признак того, что возврат не выполнен. */
    public static final long REFUND_DENIED = -1;

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
     * Возврат оплаченного билета по заявлению зрителя.
     * Возврат возможен только покупателю билета, только для билета в состоянии «Оплачен»
     * до начала мероприятия и только если доля возврата больше нуля; иначе билет остаётся
     * действительным.
     *
     * @param requesterId кто подал заявку на возврат
     * @return сумма к возврату в копейках или REFUND_DENIED
     */
    public static long requestRefund(Model.Ticket t, Model.Event event, String requesterId, long nowSec,
                                     List<Model.Transition> history) {
        if (!requesterId.equals(t.buyerId)) {
            return REFUND_DENIED;
        }
        if (t.state != Model.PAID || event.cancelled || nowSec >= event.startEpochSec) {
            return REFUND_DENIED;
        }
        int percent = refundPercent(event.startEpochSec - nowSec);
        if (percent == 0) {
            return REFUND_DENIED;
        }
        long amount = t.priceKop * percent / 100;   // округление вниз до копейки
        TicketProcedures.changeState(t, Model.REFUNDED, nowSec,
                "возврат зрителем, " + percent + " %", amount, history);
        return amount;
    }

    /**
     * Отмена мероприятия организатором: оплаченные и использованные билеты аннулируются
     * с полным возвратом, неоплаченные брони отменяются, открытые сеансы закрываются.
     *
     * @param tickets все билеты (обрабатываются только билеты этого мероприятия)
     * @return общая сумма к возврату в копейках
     */
    public static long cancelEvent(Model.Event event, List<Model.Ticket> tickets, long nowSec,
                                   List<Model.Transition> history) {
        event.cancelled = true;
        long total = 0;
        for (Model.Ticket t : tickets) {                       // n итераций
            if (!t.eventId.equals(event.id)) {
                continue;
            }
            if (t.state == Model.PAID || t.state == Model.USED) {
                total += t.priceKop;
                TicketProcedures.changeState(t, Model.ANNULLED, nowSec,
                        "отмена мероприятия, 100 %", t.priceKop, history);
            } else if (t.state == Model.RESERVED) {
                TicketProcedures.changeState(t, Model.CANCELLED, nowSec,
                        "отмена мероприятия до оплаты", history);
            }
            t.sessionActive = false;
        }
        return total;
    }
}
