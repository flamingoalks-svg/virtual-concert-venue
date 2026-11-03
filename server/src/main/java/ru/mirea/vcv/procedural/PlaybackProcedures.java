package ru.mirea.vcv.procedural;

/**
 * Алгоритм 4. Синхронный показ записи выступления в режиме имитации прямого эфира
 * (автоматизируемая операция А4).
 *
 * <p>Все клиенты воспроизводят запись с позиции «текущее время сервера минус время
 * начала». Клиент периодически сверяет свою позицию с серверной и перематывает
 * запись, только если расхождение больше порога, чтобы не дёргать видео из-за
 * небольших сетевых задержек.</p>
 *
 * <p>Сложность: все вычисления — фиксированное число арифметических операций, O(1).</p>
 */
public final class PlaybackProcedures {

    private PlaybackProcedures() {
    }

    public static final int BEFORE_START = 0;
    public static final int PLAYING = 1;
    public static final int ENDED = 2;

    public static final String[] PHASE_NAMES = {"ожидание начала", "идёт показ", "показ завершён"};

    /** Порог расхождения, после которого клиент перематывает запись, мс. */
    public static final long SEEK_THRESHOLD_MS = 2000;

    /** Значение «перемотка не нужна». */
    public static final long NO_SEEK = -1;

    /** Фаза показа в момент nowMs. */
    public static int phase(long nowMs, long startMs, long durationMs) {
        if (nowMs < startMs) {
            return BEFORE_START;
        }
        if (nowMs >= startMs + durationMs) {
            return ENDED;
        }
        return PLAYING;
    }

    /** Позиция воспроизведения записи, мс: от 0 до durationMs включительно. */
    public static long playbackPosition(long nowMs, long startMs, long durationMs) {
        long pos = nowMs - startMs;
        if (pos < 0) {
            return 0;
        }
        return Math.min(pos, durationMs);
    }

    /**
     * Решает, нужно ли клиенту перемотать запись.
     *
     * @return позиция, на которую нужно перемотать, или NO_SEEK
     */
    public static long seekTarget(long clientPosMs, long serverPosMs, long thresholdMs) {
        if (Math.abs(clientPosMs - serverPosMs) > thresholdMs) {
            return serverPosMs;
        }
        return NO_SEEK;
    }
}
