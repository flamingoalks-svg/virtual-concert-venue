package ru.mirea.vcv.oop.hall;

import java.time.Instant;
import ru.mirea.vcv.oop.ticket.Ticket;

/**
 * Сеанс — пребывание зрителя в конкретном экземпляре зала. Хранит ссылку на экземпляр,
 * выбранный при входе, поэтому при закрытии сеанса место освобождается именно в нём.
 */
public class Session {

    private final Ticket ticket;
    private final String viewerId;
    private final HallInstance instance;
    private final Instant startedAt;
    private Instant endedAt;

    Session(Ticket ticket, String viewerId, HallInstance instance, Instant startedAt) {
        this.ticket = ticket;
        this.viewerId = viewerId;
        this.instance = instance;
        this.startedAt = startedAt;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public String getViewerId() {
        return viewerId;
    }

    public HallInstance getInstance() {
        return instance;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public boolean isOpen() {
        return endedAt == null;
    }

    /** Выход зрителя или разрыв связи. */
    public void close(Instant now) {
        if (!isOpen()) {
            return;
        }
        endedAt = now;
        instance.release();
        ticket.endSession();
    }
}
