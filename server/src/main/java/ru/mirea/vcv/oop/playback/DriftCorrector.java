package ru.mirea.vcv.oop.playback;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Решает, нужна ли клиенту перемотка: только если расхождение больше порога. */
public class DriftCorrector {

    public static final Duration DEFAULT_THRESHOLD = Duration.ofMillis(2000);

    private final PlaybackSchedule schedule;
    private final Duration threshold;

    public DriftCorrector(PlaybackSchedule schedule, Duration threshold) {
        this.schedule = schedule;
        this.threshold = threshold;
    }

    /** @return позиция, на которую нужно перемотать клиент, или пустое значение */
    public Optional<Duration> seekTarget(Duration clientPosition, Instant now) {
        Duration server = schedule.positionAt(now);
        if (clientPosition.minus(server).abs().compareTo(threshold) > 0) {
            return Optional.of(server);
        }
        return Optional.empty();
    }
}
