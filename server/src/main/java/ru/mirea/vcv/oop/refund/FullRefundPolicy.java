package ru.mirea.vcv.oop.refund;

import java.time.Duration;

/** Полный возврат независимо от срока: применяется при отмене мероприятия. */
public class FullRefundPolicy implements RefundPolicy {

    @Override
    public int percent(Duration timeToStart) {
        return 100;
    }
}
