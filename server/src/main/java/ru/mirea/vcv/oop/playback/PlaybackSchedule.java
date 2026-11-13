package ru.mirea.vcv.oop.playback;

import java.time.Duration;
import java.time.Instant;
import ru.mirea.vcv.oop.event.Event;

/**
 * Расписание показа записи (операция А4): позиция воспроизведения однозначно
 * определяется временем сервера, поэтому у всех зрителей она одинакова. Сложность O(1).
 */
public class PlaybackSchedule {

    private final Event event;

    public PlaybackSchedule(Event event) {
        this.event = event;
    }

    public PlaybackPhase phaseAt(Instant now) {
        if (!event.hasStarted(now)) {
            return PlaybackPhase.BEFORE_START;
        }
        return event.hasFinished(now) ? PlaybackPhase.ENDED : PlaybackPhase.PLAYING;
    }

    /** Позиция воспроизведения, ограниченная отрезком [0; длительность]. */
    public Duration positionAt(Instant now) {
        Duration pos = Duration.between(event.getStart(), now);
        if (pos.isNegative()) {
            return Duration.ZERO;
        }
        return pos.compareTo(event.getDuration()) > 0 ? event.getDuration() : pos;
    }
}
