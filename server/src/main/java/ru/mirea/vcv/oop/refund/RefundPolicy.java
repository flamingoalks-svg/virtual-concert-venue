package ru.mirea.vcv.oop.refund;

import java.time.Duration;

/** Правило определения доли возврата в зависимости от времени до начала мероприятия. */
public interface RefundPolicy {

    /** @return доля возврата в процентах (0 — возврат не производится) */
    int percent(Duration timeToStart);
}
