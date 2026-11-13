package ru.mirea.vcv.oop.playback;

/** Фаза показа записи выступления. */
public enum PlaybackPhase {
    BEFORE_START("ожидание начала"),
    PLAYING("идёт показ"),
    ENDED("показ завершён");

    private final String title;

    PlaybackPhase(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }
}
