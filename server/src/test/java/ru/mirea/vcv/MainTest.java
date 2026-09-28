package ru.mirea.vcv;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MainTest {

    @Test
    void greetingIsNotChanged() {
        assertEquals("Virtual Concert Venue server", Main.greeting());
    }
}
