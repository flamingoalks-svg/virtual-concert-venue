package ru.mirea.vcv.oop.refund;

import java.time.Instant;

/**
 * Неизменяемая запись о заявке на возврат и результате её рассмотрения
 * (сущность «Заявка на возврат» логической модели).
 */
public final class RefundRequest {

    private final String ticketCode;
    private final String requesterId;
    private final Instant requestedAt;
    private final RefundBasis basis;
    private final boolean approved;
    private final int percent;
    private final long amountKop;
    private final String decision;

    private RefundRequest(String ticketCode, String requesterId, Instant requestedAt, RefundBasis basis,
                          boolean approved, int percent, long amountKop, String decision) {
        this.ticketCode = ticketCode;
        this.requesterId = requesterId;
        this.requestedAt = requestedAt;
        this.basis = basis;
        this.approved = approved;
        this.percent = percent;
        this.amountKop = amountKop;
        this.decision = decision;
    }

    static RefundRequest approved(String ticketCode, String requesterId, Instant at, RefundBasis basis,
                                  int percent, long amountKop) {
        return new RefundRequest(ticketCode, requesterId, at, basis, true, percent, amountKop,
                "возврат " + percent + " %");
    }

    static RefundRequest rejected(String ticketCode, String requesterId, Instant at, String reason) {
        return new RefundRequest(ticketCode, requesterId, at, RefundBasis.VIEWER_REFUSAL, false, 0, 0, reason);
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public RefundBasis getBasis() {
        return basis;
    }

    public boolean isApproved() {
        return approved;
    }

    public int getPercent() {
        return percent;
    }

    public long getAmountKop() {
        return amountKop;
    }

    public String getDecision() {
        return decision;
    }
}
