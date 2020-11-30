package com.mysticwind.linenotificationsupport.notification;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotificationCounterTest {

    private static final int MAX_NOTIFICATIONS = 5;
    private static final String GROUP_1 = "group1";
    private static final String GROUP_2 = "group2";

    private NotificationCounter classUnderTest;

    @Test
    public void testTracking() {
        assertEquals(1, classUnderTest.notified(GROUP_1, 1));
        assertEquals(3, classUnderTest.notified(GROUP_1, 2));
        assertEquals(4, classUnderTest.notified(GROUP_2, 3));
        // TODO should this be an error case if it exceeds the max?
        assertEquals(6, classUnderTest.notified(GROUP_2, 4));
        assertEquals(4, classUnderTest.dismissed(GROUP_1, 2));
        assertEquals(3, classUnderTest.dismissed(GROUP_1, 1));
        assertEquals(1, classUnderTest.dismissed(GROUP_2, 4));
        assertEquals(0, classUnderTest.dismissed(GROUP_2, 3));
    }

    @Test
    public void testHasSlot() {
        assertTrue(classUnderTest.hasSlot(GROUP_1));
        assertTrue(classUnderTest.hasSlot(GROUP_2));

        classUnderTest.notified(GROUP_1, 1);
        assertTrue(classUnderTest.hasSlot(GROUP_1));
        assertTrue(classUnderTest.hasSlot(GROUP_2));

        classUnderTest.notified(GROUP_1, 2);
        assertTrue(classUnderTest.hasSlot(GROUP_1));
        assertTrue(classUnderTest.hasSlot(GROUP_2));

        classUnderTest.notified(GROUP_1, 3);
        assertFalse(classUnderTest.hasSlot(GROUP_1));
        assertTrue(classUnderTest.hasSlot(GROUP_2));

        classUnderTest.notified(GROUP_2, 4);
        assertFalse(classUnderTest.hasSlot(GROUP_1));
        assertFalse(classUnderTest.hasSlot(GROUP_2));
    }

    @Before
    public void setUp() {
        classUnderTest = new NotificationCounter(MAX_NOTIFICATIONS);
    }

}