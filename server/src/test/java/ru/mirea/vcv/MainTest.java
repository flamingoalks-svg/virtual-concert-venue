package ru.mirea.vcv;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MainTest {

    @Test
    public void greetingIsNotChanged() {
        assertEquals("Virtual Concert Venue server", Main.greeting());
    }
}
