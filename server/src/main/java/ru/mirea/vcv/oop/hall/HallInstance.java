package ru.mirea.vcv.oop.hall;

/** Экземпляр (копия) виртуального зала с ограниченной вместимостью. */
public class HallInstance {

    private final int number;
    private final int capacity;
    private int viewers;

    HallInstance(int number, int capacity) {
        this.number = number;
        this.capacity = capacity;
    }

    public int getNumber() {
        return number;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getViewers() {
        return viewers;
    }

    public boolean hasFreeSeat() {
        return viewers < capacity;
    }

    void occupy() {
        if (!hasFreeSeat()) {
            throw new IllegalStateException("Экземпляр " + number + " заполнен");
        }
        viewers++;
    }

    void release() {
        if (viewers > 0) {
            viewers--;
        }
    }
}
