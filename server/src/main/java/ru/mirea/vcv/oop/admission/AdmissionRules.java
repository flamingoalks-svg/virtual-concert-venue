package ru.mirea.vcv.oop.admission;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import ru.mirea.vcv.oop.ticket.TicketState;

/**
 * Стандартные правила допуска (алгоритм 1, рисунок 2.6) в том порядке,
 * в котором они проверяются. Правила без собственных данных записаны лямбда-выражениями.
 */
public final class AdmissionRules {

    private AdmissionRules() {
    }

    public static AdmissionRule ticketForEvent() {
        return (t, e, v, now) -> t.isFor(e) ? Optional.empty() : Optional.of(AdmissionResult.WRONG_EVENT);
    }

    public static AdmissionRule eventNotCancelled() {
        return (t, e, v, now) -> e.isCancelled() ? Optional.of(AdmissionResult.EVENT_CANCELLED) : Optional.empty();
    }

    public static AdmissionRule hallOpen() {
        return (t, e, v, now) -> e.isHallOpen(now) ? Optional.empty() : Optional.of(AdmissionResult.TOO_EARLY);
    }

    public static AdmissionRule eventNotFinished() {
        return (t, e, v, now) -> e.hasFinished(now) ? Optional.of(AdmissionResult.FINISHED) : Optional.empty();
    }

    public static AdmissionRule ticketValid() {
        return (t, e, v, now) -> t.getState().allowsEntry() ? Optional.empty()
                : Optional.of(AdmissionResult.NOT_VALID);
    }

    public static AdmissionRule sameViewer() {
        return (t, e, v, now) -> t.getState() == TicketState.USED && !v.equals(t.getViewerId())
                ? Optional.of(AdmissionResult.OTHER_VIEWER) : Optional.empty();
    }

    public static AdmissionRule noActiveSession() {
        return (t, e, v, now) -> t.isSessionActive() ? Optional.of(AdmissionResult.SESSION_ACTIVE)
                : Optional.empty();
    }

    /** Набор правил алгоритма 1. */
    public static List<AdmissionRule> standard() {
        return Arrays.asList(ticketForEvent(), eventNotCancelled(), hallOpen(), eventNotFinished(),
                ticketValid(), sameViewer(), noActiveSession());
    }
}
