package ru.mirea.vcv.oop.refund;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ru.mirea.vcv.oop.event.Event;
import ru.mirea.vcv.oop.ticket.Ticket;
import ru.mirea.vcv.oop.ticket.TicketRepository;
import ru.mirea.vcv.oop.ticket.TicketState;

/**
 * Служба возвратов (операция А2). Доля возврата берётся из подключённых правил
 * {@link RefundPolicy}; каждая заявка сохраняется в журнале.
 * Мероприятие берётся из самого билета, поэтому рассчитать возврат по чужому
 * мероприятию невозможно.
 */
public class RefundService {

    public static final String ORGANIZER = "организатор";

    private final RefundPolicy viewerPolicy;
    private final RefundPolicy cancellationPolicy;
    private final List<RefundRequest> journal = new ArrayList<>();

    public RefundService(RefundPolicy viewerPolicy, RefundPolicy cancellationPolicy) {
        this.viewerPolicy = viewerPolicy;
        this.cancellationPolicy = cancellationPolicy;
    }

    /** Правила по закону: ст. 52.1 при отказе зрителя и полный возврат при отмене. */
    public static RefundService statutory() {
        return new RefundService(new StatutoryRefundPolicy(), new FullRefundPolicy());
    }

    public List<RefundRequest> getJournal() {
        return Collections.unmodifiableList(journal);
    }

    /** Заявка зрителя на возврат. Сложность O(1). */
    public RefundRequest requestRefund(Ticket t, String requesterId, Instant now) {
        Event e = t.getEvent();
        if (!t.isBoughtBy(requesterId)) {
            return reject(t, requesterId, now, "заявитель не является покупателем");
        }
        if (t.getState() != TicketState.PAID) {
            return reject(t, requesterId, now, "билет в состоянии «" + t.getState().getTitle() + "»");
        }
        if (e.isCancelled()) {
            return reject(t, requesterId, now, "мероприятие отменено, возврат выполнен при отмене");
        }
        if (e.hasStarted(now)) {
            return reject(t, requesterId, now, "мероприятие уже началось");
        }
        int percent = viewerPolicy.percent(e.timeToStart(now));
        if (percent == 0) {
            return reject(t, requesterId, now, "до начала менее 3 суток");
        }
        long amount = t.getPriceKop() * percent / 100;
        t.refund(percent, now);
        return record(RefundRequest.approved(t.getCode(), requesterId, now, RefundBasis.VIEWER_REFUSAL,
                percent, amount));
    }

    /**
     * Отмена мероприятия: оплаченные и использованные билеты аннулируются с возвратом,
     * неоплаченные брони отменяются. Сложность O(n) по числу билетов мероприятия.
     *
     * @return общая сумма к возврату, копейки
     */
    public long cancelEvent(Event event, TicketRepository tickets, Instant now) {
        event.cancel();
        long total = 0;
        for (Ticket t : tickets.findByEvent(event)) {
            if (t.getState() == TicketState.PAID || t.getState() == TicketState.USED) {
                int percent = cancellationPolicy.percent(event.timeToStart(now));
                long amount = t.getPriceKop() * percent / 100;
                t.annul(now);
                record(RefundRequest.approved(t.getCode(), ORGANIZER, now, RefundBasis.EVENT_CANCELLATION,
                        percent, amount));
                total += amount;
            } else if (t.getState() == TicketState.RESERVED) {
                t.cancelReservation(now, "отмена мероприятия до оплаты");
            }
        }
        return total;
    }

    private RefundRequest reject(Ticket t, String requesterId, Instant now, String reason) {
        return record(RefundRequest.rejected(t.getCode(), requesterId, now, reason));
    }

    private RefundRequest record(RefundRequest r) {
        journal.add(r);
        return r;
    }
}
