package ru.mirea.vcv.oop.ticket;

/** Попытка выполнить переход, которого нет на диаграмме состояний билета. */
public class IllegalTicketStateException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public IllegalTicketStateException(String ticketCode, TicketState from, TicketState to) {
        super("Билет " + ticketCode + ": переход «" + from.getTitle() + "» -> «" + to.getTitle()
                + "» недопустим");
    }
}
