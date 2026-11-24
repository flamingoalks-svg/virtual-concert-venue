package ru.mirea.vcv.oop.refund;

/** Основание возврата. */
public enum RefundBasis {
    VIEWER_REFUSAL("отказ зрителя"),
    EVENT_CANCELLATION("отмена мероприятия");

    private final String title;

    RefundBasis(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
