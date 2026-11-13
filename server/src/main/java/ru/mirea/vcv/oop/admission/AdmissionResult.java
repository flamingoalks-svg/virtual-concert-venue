package ru.mirea.vcv.oop.admission;

/** Результат проверки билета при входе в зал. */
public enum AdmissionResult {
    ADMITTED("допущен, билет погашен"),
    READMITTED("допущен повторно"),
    NOT_FOUND("отказ: билет не найден"),
    WRONG_EVENT("отказ: билет на другое мероприятие"),
    EVENT_CANCELLED("отказ: мероприятие отменено"),
    TOO_EARLY("отказ: зал ещё не открыт"),
    FINISHED("отказ: мероприятие завершено"),
    NOT_VALID("отказ: билет недействителен"),
    OTHER_VIEWER("отказ: билет закреплён за другим зрителем"),
    SESSION_ACTIVE("отказ: по билету уже открыт сеанс"),
    BLOCKED("отказ: зрителю запрещён вход");

    private final String title;

    AdmissionResult(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public boolean isAdmitted() {
        return this == ADMITTED || this == READMITTED;
    }
}
