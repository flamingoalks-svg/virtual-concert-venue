package ru.mirea.vcv.oop.event;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Мероприятие: показ записи выступления в виртуальном зале.
 * Объект сам отвечает за вопросы о своём времени: открыт ли зал, началось ли
 * и закончилось ли мероприятие, сколько осталось до начала.
 */
public class Event {

    /** Зал открывается за 30 минут до начала. */
    public static final Duration HALL_OPENS_BEFORE = Duration.ofMinutes(30);

    private final String id;
    private final String title;
    private final Instant start;
    private final Duration duration;
    private final int instanceCapacity;
    private boolean cancelled;

    public Event(String id, String title, Instant start, Duration duration, int instanceCapacity) {
        if (instanceCapacity <= 0) {
            throw new IllegalArgumentException("Вместимость экземпляра должна быть положительной");
        }
        this.id = Objects.requireNonNull(id);
        this.title = Objects.requireNonNull(title);
        this.start = Objects.requireNonNull(start);
        this.duration = Objects.requireNonNull(duration);
        this.instanceCapacity = instanceCapacity;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public Instant getStart() {
        return start;
    }

    public Duration getDuration() {
        return duration;
    }

    public Instant getEnd() {
        return start.plus(duration);
    }

    public int getInstanceCapacity() {
        return instanceCapacity;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    /** Отмена мероприятия организатором. */
    public void cancel() {
        cancelled = true;
    }

    public boolean isHallOpen(Instant now) {
        return !now.isBefore(start.minus(HALL_OPENS_BEFORE));
    }

    public boolean hasStarted(Instant now) {
        return !now.isBefore(start);
    }

    public boolean hasFinished(Instant now) {
        return !now.isBefore(getEnd());
    }

    public Duration timeToStart(Instant now) {
        return Duration.between(now, start);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Event && ((Event) o).id.equals(id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
