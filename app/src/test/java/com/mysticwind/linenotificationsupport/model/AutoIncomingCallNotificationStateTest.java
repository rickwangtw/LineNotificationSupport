package com.mysticwind.linenotificationsupport.model;

import com.google.common.collect.ImmutableSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class AutoIncomingCallNotificationStateTest {

    private static final int NOTIFICATION_ID = 100;

    private LineNotification lineNotification = LineNotification.builder()
            .build();

    @Test
    public void testHappyCase() {
        final AutoIncomingCallNotificationState classUnderTest = AutoIncomingCallNotificationState.builder()
                .lineNotification(lineNotification)
                .timeoutInSeconds(1)
                .build();
        assertEquals(Collections.EMPTY_SET, classUnderTest.getIncomingCallNotificationIds());

        assertTrue(classUnderTest.shouldNotify());

        classUnderTest.notified(NOTIFICATION_ID);
        sleepInSeconds(1);

        assertFalse(classUnderTest.shouldNotify());
        assertEquals(ImmutableSet.of(NOTIFICATION_ID), classUnderTest.getIncomingCallNotificationIds());
        assertEquals(lineNotification, classUnderTest.getLineNotification());
    }

    @Test
    public void testMissedCall() {
        final AutoIncomingCallNotificationState classUnderTest = AutoIncomingCallNotificationState.builder()
                .lineNotification(lineNotification)
                .timeoutInSeconds(1)
                .build();

        assertTrue(classUnderTest.shouldNotify());

        classUnderTest.setMissedCall();

        assertFalse(classUnderTest.shouldNotify());
    }

    @Test
    public void testCancel() {
        final AutoIncomingCallNotificationState classUnderTest = AutoIncomingCallNotificationState.builder()
                .lineNotification(lineNotification)
                .timeoutInSeconds(1)
                .build();

        assertTrue(classUnderTest.shouldNotify());

        classUnderTest.cancel();

        assertFalse(classUnderTest.shouldNotify());
    }

    @Test
    public void testAccepted() {
        final AutoIncomingCallNotificationState classUnderTest = AutoIncomingCallNotificationState.builder()
                .lineNotification(lineNotification)
                .timeoutInSeconds(1)
                .build();

        assertTrue(classUnderTest.shouldNotify());

        classUnderTest.setAccepted();

        assertFalse(classUnderTest.shouldNotify());
    }

    private void sleepInSeconds(int seconds) {
        try {
            Thread.sleep(1000 * seconds);
        } catch (InterruptedException e) {
        }
    }

}