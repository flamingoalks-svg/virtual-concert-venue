package ru.mirea.vcv.procedural;

/**
 * Структуры данных процедурной реализации.
 *
 * <p>В процедурном стиле данные отделены от обработки: здесь описаны только
 * «записи» с открытыми полями (как struct в языке C), а все действия над ними
 * выполняют статические процедуры в классах *Procedures. Состав полей
 * соответствует логической модели данных (сущности «Мероприятие», «Билет»,
 * «Переход состояния билета», «Экземпляр зала»).</p>
 */
public final class Model {

    private Model() {
    }

    // ---- Состояния билета (таблица 1.4 и рисунок 1.1 отчёта) ----
    public static final int RESERVED = 0;   // Забронирован
    public static final int PAID = 1;       // Оплачен
    public static final int USED = 2;       // Использован (погашен)
    public static final int REFUNDED = 3;   // Возвращён
    public static final int ANNULLED = 4;   // Аннулирован (мероприятие отменено)
    public static final int CANCELLED = 5;  // Отменён (бронь истекла)

    public static final String[] STATE_NAMES = {
        "Забронирован", "Оплачен", "Использован", "Возвращён", "Аннулирован", "Отменён"
    };

    /** Мероприятие: время начала и длительность показа записи выступления. */
    public static class Event {
        public String id;
        public String title;
        public long startEpochSec;   // время начала, секунды Unix
        public long durationSec;     // длительность записи выступления
        public boolean cancelled;    // мероприятие отменено организатором
    }

    /** Билет на мероприятие. Цена хранится в копейках, чтобы не было ошибок округления. */
    public static class Ticket {
        public String code;          // уникальный код доступа из ссылки на вход
        public String eventId;
        public int state;            // одно из значений RESERVED..CANCELLED
        public long priceKop;
        public String buyerId;       // покупатель: только он может вернуть билет
        public String viewerId;      // зритель, за которым билет закреплён при гашении
        public boolean sessionActive; // открыт ли сейчас сеанс по этому билету
    }

    /** Запись истории: переход билета из одного состояния в другое. */
    public static class Transition {
        public String ticketCode;
        public int fromState;
        public int toState;
        public long atEpochSec;
        public String reason;
        public long refundKop;       // сумма возврата, если переход связан с возвратом (иначе 0)
    }

    /** Экземпляр (копия) виртуального зала с текущим числом зрителей. */
    public static class HallInstance {
        public int number;
        public int viewers;
    }
}
