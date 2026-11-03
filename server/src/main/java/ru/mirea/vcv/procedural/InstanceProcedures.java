package ru.mirea.vcv.procedural;

import java.util.List;

/**
 * Алгоритм 3. Распределение зрителей по экземплярам виртуального зала
 * (автоматизируемая операция А3).
 *
 * <p>Сетевая сцена одного экземпляра рассчитана не более чем на capacity участников.
 * Новый зритель направляется в самый заполненный экземпляр, где ещё есть место:
 * так зрители не «размазываются» по полупустым залам и сохраняется эффект общего
 * присутствия. Если свободных мест нет нигде, создаётся новый экземпляр.</p>
 *
 * <p>Сложность: выбор экземпляра просматривает список из k экземпляров один раз — O(k).
 * Так как k = ⌈n / capacity⌉, при последовательном входе n зрителей суммарная работа
 * составляет O(n · k) = O(n² / capacity).</p>
 */
public final class InstanceProcedures {

    private InstanceProcedures() {
    }

    /**
     * Помещает зрителя в экземпляр зала.
     *
     * @param instances текущий список экземпляров (дополняется при создании нового)
     * @param capacity  вместимость одного экземпляра
     * @return номер экземпляра, в который помещён зритель
     */
    public static int assignViewer(List<Model.HallInstance> instances, int capacity) {
        Model.HallInstance best = null;
        for (Model.HallInstance h : instances) {               // k итераций
            if (h.viewers < capacity && (best == null || h.viewers > best.viewers)) {
                best = h;
            }
        }
        if (best == null) {                                    // мест нет — новый экземпляр
            best = new Model.HallInstance();
            best.number = instances.size() + 1;
            best.viewers = 0;
            instances.add(best);
        }
        best.viewers++;
        return best.number;
    }

    /** Освобождает место, когда зритель покидает экземпляр с указанным номером. */
    public static void releaseViewer(List<Model.HallInstance> instances, int number) {
        for (Model.HallInstance h : instances) {
            if (h.number == number && h.viewers > 0) {
                h.viewers--;
                return;
            }
        }
    }

    /** Общее число зрителей мероприятия во всех экземплярах (показывается в каждом зале). */
    public static int totalViewers(List<Model.HallInstance> instances) {
        int sum = 0;
        for (Model.HallInstance h : instances) {
            sum += h.viewers;
        }
        return sum;
    }
}
