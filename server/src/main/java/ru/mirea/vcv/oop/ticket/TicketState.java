package ru.mirea.vcv.oop.ticket;

import java.util.EnumSet;
import java.util.Set;

/**
 * Состояния билета и допустимые переходы между ними (диаграмма состояний, рисунок 1.1).
 * Таблица переходов хранится в самом перечислении, поэтому недопустимый переход
 * невозможно выполнить ни из одного места программы.
 */
public enum TicketState {
    RESERVED("Забронирован"),
    PAID("Оплачен"),
    USED("Использован"),
    REFUNDED("Возвращён"),
    ANNULLED("Аннулирован"),
    CANCELLED("Отменён");

    private final String title;

    TicketState(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /** Состояния, в которые можно перейти из этого. */
    public Set<TicketState> next() {
        switch (this) {
            case RESERVED:
                return EnumSet.of(PAID, CANCELLED);
            case PAID:
                return EnumSet.of(USED, REFUNDED, ANNULLED);
            case USED:
                return EnumSet.of(ANNULLED);
            default:
                return EnumSet.noneOf(TicketState.class);   // конечные состояния
        }
    }

    public boolean canMoveTo(TicketState target) {
        return next().contains(target);
    }

    /** По билету в этом состоянии можно войти в зал. */
    public boolean allowsEntry() {
        return this == PAID || this == USED;
    }
}
