package ru.mirea.vcv.oop.hall;

import java.util.Optional;
import ru.mirea.vcv.oop.admission.AdmissionResult;

/** Результат попытки входа: решение о допуске и, при допуске, открытый сеанс. */
public final class Entry {

    private final AdmissionResult result;
    private final Session session;

    Entry(AdmissionResult result, Session session) {
        this.result = result;
        this.session = session;
    }

    public AdmissionResult getResult() {
        return result;
    }

    public Optional<Session> getSession() {
        return Optional.ofNullable(session);
    }
}
