package com.mysticwind.linenotificationsupport.notification.impl;

import com.mysticwind.linenotificationsupport.notification.NotificationCounter;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class SmartNotificationCounterTest {

    private static final int MAX_NOTIFICATIONS = 5;
    private static final String GROUP_1 = "group1";
    private static final String GROUP_2 = "group2";

    private NotificationCounter classUnderTest;

    @Test
    public void testTracking() {
        Assert.assertEquals(1, classUnderTest.notified(GROUP_1, 1));
        Assert.assertEquals(3, classUnderTest.notified(GROUP_1, 2));
        Assert.assertEquals(4, classUnderTest.notified(GROUP_2, 3));
        // TODO should this be an error case if it exceeds the max?
        Assert.assertEquals(6, classUnderTest.notified(GROUP_2, 4));
        Assert.assertEquals(4, classUnderTest.dismissed(GROUP_1, 2));
        Assert.assertEquals(3, classUnderTest.dismissed(GROUP_1, 1));
        Assert.assertEquals(1, classUnderTest.dismissed(GROUP_2, 4));
        Assert.assertEquals(0, classUnderTest.dismissed(GROUP_2, 3));
    }

    @Test
    public void testHasSlot() {
        Assert.assertTrue(classUnderTest.hasSlot(GROUP_1));
        Assert.assertTrue(classUnderTest.hasSlot(GROUP_2));

        classUnderTest.notified(GROUP_1, 1);
        Assert.assertTrue(classUnderTest.hasSlot(GROUP_1));
        Assert.assertTrue(classUnderTest.hasSlot(GROUP_2));

        classUnderTest.notified(GROUP_1, 2);
        Assert.assertTrue(classUnderTest.hasSlot(GROUP_1));
        Assert.assertTrue(classUnderTest.hasSlot(GROUP_2));

        classUnderTest.notified(GROUP_1, 3);
        Assert.assertFalse(classUnderTest.hasSlot(GROUP_1));
        Assert.assertTrue(classUnderTest.hasSlot(GROUP_2));

        classUnderTest.notified(GROUP_2, 4);
        Assert.assertFalse(classUnderTest.hasSlot(GROUP_1));
        Assert.assertFalse(classUnderTest.hasSlot(GROUP_2));
    }

    @Before
    public void setUp() {
        classUnderTest = new SmartNotificationCounter(MAX_NOTIFICATIONS);
    }

}