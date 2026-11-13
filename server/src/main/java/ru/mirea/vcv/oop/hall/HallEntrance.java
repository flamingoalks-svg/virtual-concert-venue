package ru.mirea.vcv.oop.hall;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import ru.mirea.vcv.oop.admission.AdmissionResult;
import ru.mirea.vcv.oop.admission.AdmissionService;
import ru.mirea.vcv.oop.event.Event;
import ru.mirea.vcv.oop.ticket.TicketRepository;

/**
 * Вход в зал мероприятия: объединяет допуск (А1) и выбор экземпляра (А3)
 * и закрепляет выбранный экземпляр за сеансом. Сложность входа O(k).
 */
public class HallEntrance {

    private final Event event;
    private final TicketRepository tickets;
    private final AdmissionService admission;
    private final VirtualHall hall;
    private final List<Session> sessions = new ArrayList<>();

    public HallEntrance(Event event, TicketRepository tickets, AdmissionService admission) {
        this.event = event;
        this.tickets = tickets;
        this.admission = admission;
        this.hall = new VirtualHall(event.getInstanceCapacity());
    }

    public VirtualHall getHall() {
        return hall;
    }

    public List<Session> getSessions() {
        return Collections.unmodifiableList(sessions);
    }

    public Entry enter(String code, String viewerId, Instant now) {
        AdmissionResult result = admission.admit(event, code, viewerId, now);
        if (!result.isAdmitted()) {
            return new Entry(result, null);
        }
        HallInstance instance = hall.allocateSeat();
        Session s = new Session(tickets.findByCode(code).get(), viewerId, instance, now);
        sessions.add(s);
        return new Entry(result, s);
    }
}
