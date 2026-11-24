package ru.mirea.vcv.oop.refund;

import java.time.Duration;

/**
 * Возврат по инициативе зрителя по статье 52.1 Основ законодательства РФ о культуре:
 * не позднее чем за 10 дней — 100 %, за 5 дней — 50 %, за 3 дня — 30 %, позднее — 0.
 */
public class StatutoryRefundPolicy implements RefundPolicy {

    @Override
    public int percent(Duration timeToStart) {
        if (timeToStart.compareTo(Duration.ofDays(10)) >= 0) {
            return 100;
        }
        if (timeToStart.compareTo(Duration.ofDays(5)) >= 0) {
            return 50;
        }
        if (timeToStart.compareTo(Duration.ofDays(3)) >= 0) {
            return 30;
        }
        return 0;
    }
}
