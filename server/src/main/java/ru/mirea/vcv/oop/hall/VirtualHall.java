package ru.mirea.vcv.oop.hall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Виртуальный зал мероприятия: набор экземпляров одинаковой вместимости (операция А3).
 * Зритель направляется в самый заполненный экземпляр со свободным местом,
 * при отсутствии мест создаётся новый экземпляр. Сложность O(k) по числу экземпляров.
 */
public class VirtualHall {

    private final int instanceCapacity;
    private final List<HallInstance> instances = new ArrayList<>();

    public VirtualHall(int instanceCapacity) {
        this.instanceCapacity = instanceCapacity;
    }

    public List<HallInstance> getInstances() {
        return Collections.unmodifiableList(instances);
    }

    public HallInstance allocateSeat() {
        HallInstance best = null;
        for (HallInstance h : instances) {
            if (h.hasFreeSeat() && (best == null || h.getViewers() > best.getViewers())) {
                best = h;
            }
        }
        if (best == null) {
            best = new HallInstance(instances.size() + 1, instanceCapacity);
            instances.add(best);
        }
        best.occupy();
        return best;
    }

    public int totalViewers() {
        int sum = 0;
        for (HallInstance h : instances) {
            sum += h.getViewers();
        }
        return sum;
    }

    /** Строка заполненности вида «№1 — 2, №2 — 1». */
    public String occupancy() {
        StringBuilder sb = new StringBuilder();
        for (HallInstance h : instances) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append('№').append(h.getNumber()).append(" — ").append(h.getViewers());
        }
        return sb.toString();
    }
}
