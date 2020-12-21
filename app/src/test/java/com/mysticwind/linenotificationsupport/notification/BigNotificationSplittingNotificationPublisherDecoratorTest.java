package com.mysticwind.linenotificationsupport.notification;

import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BigNotificationSplittingNotificationPublisherDecoratorTest {

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private NotificationIdGenerator notificationIdGenerator;

    @Mock
    private PreferenceProvider preferenceProvider;

    @Captor
    private ArgumentCaptor<LineNotification> lineNotificationCaptor;

    @Captor
    private ArgumentCaptor<Integer> notificationIdCaptor;

    private BigNotificationSplittingNotificationPublisherDecorator classUnderTest;

    @Before
    public void setUp() {
        when(preferenceProvider.getMessageSizeLimit()).thenReturn(15);
        when(preferenceProvider.getMaxPageCount()).thenReturn(3);
        when(notificationIdGenerator.getNextNotificationId())
                .thenReturn(2)
                .thenReturn(3)
                .thenReturn(4);

        classUnderTest = new BigNotificationSplittingNotificationPublisherDecorator(notificationPublisher, notificationIdGenerator, preferenceProvider);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(
                notificationPublisher,
                notificationIdGenerator,
                preferenceProvider
        );
    }

    @Test
    public void testSingleNotification() {
        classUnderTest.publishNotification(buildNotification("123456789012345"), 1);

        verify(notificationPublisher).publishNotification(lineNotificationCaptor.capture(), notificationIdCaptor.capture());
        assertEquals("123456789012345", lineNotificationCaptor.getValue().getMessage());
        assertEquals(1, notificationIdCaptor.getValue().intValue());
        verify(preferenceProvider).getMessageSizeLimit();
    }

    @Test
    public void testTwoNotifications() {
        classUnderTest.publishNotification(buildNotification("1234567890123456"), 1);

        verify(notificationPublisher, times(2)).publishNotification(lineNotificationCaptor.capture(), notificationIdCaptor.capture());
        List<LineNotification> lineNotifications = lineNotificationCaptor.getAllValues();
        List<Integer> notificationIds = notificationIdCaptor.getAllValues();
        assertEquals("1234567890(...)", lineNotifications.get(0).getMessage());
        assertEquals(2, notificationIds.get(0).intValue());
        assertEquals("(...)123456", lineNotifications.get(1).getMessage());
        assertEquals(3, notificationIds.get(1).intValue());
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
        verify(notificationIdGenerator, times(2)).getNextNotificationId();
    }

    @Test
    public void testTwoNotificationsOnEdge() {
        classUnderTest.publishNotification(buildNotification("12345678901234567890"), 1);

        verify(notificationPublisher, times(2)).publishNotification(lineNotificationCaptor.capture(), notificationIdCaptor.capture());
        List<LineNotification> lineNotifications = lineNotificationCaptor.getAllValues();
        List<Integer> notificationIds = notificationIdCaptor.getAllValues();
        assertEquals("1234567890(...)", lineNotifications.get(0).getMessage());
        assertEquals(2, notificationIds.get(0).intValue());
        assertEquals("(...)1234567890", lineNotifications.get(1).getMessage());
        assertEquals(3, notificationIds.get(1).intValue());
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
        verify(notificationIdGenerator, times(2)).getNextNotificationId();
    }

    @Test
    public void testThreeNotifications() {
        classUnderTest.publishNotification(buildNotification("123456789012345678901"), 1);

        verify(notificationPublisher, times(3)).publishNotification(lineNotificationCaptor.capture(), notificationIdCaptor.capture());
        List<LineNotification> lineNotifications = lineNotificationCaptor.getAllValues();
        List<Integer> notificationIds = notificationIdCaptor.getAllValues();
        assertEquals("1234567890(...)", lineNotifications.get(0).getMessage());
        assertEquals(2, notificationIds.get(0).intValue());
        assertEquals("(...)12345(...)", lineNotifications.get(1).getMessage());
        assertEquals(3, notificationIds.get(1).intValue());
        assertEquals("(...)678901", lineNotifications.get(2).getMessage());
        assertEquals(4, notificationIds.get(2).intValue());
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
        verify(notificationIdGenerator, times(3)).getNextNotificationId();
    }

    @Test
    public void testThreeNotificationsOnEdge() {
        classUnderTest.publishNotification(buildNotification("1234567890123456789012345"), 1);

        verify(notificationPublisher, times(3)).publishNotification(lineNotificationCaptor.capture(), notificationIdCaptor.capture());
        List<LineNotification> lineNotifications = lineNotificationCaptor.getAllValues();
        List<Integer> notificationIds = notificationIdCaptor.getAllValues();
        assertEquals("1234567890(...)", lineNotifications.get(0).getMessage());
        assertEquals(2, notificationIds.get(0).intValue());
        assertEquals("(...)12345(...)", lineNotifications.get(1).getMessage());
        assertEquals(3, notificationIds.get(1).intValue());
        assertEquals("(...)6789012345", lineNotifications.get(2).getMessage());
        assertEquals(4, notificationIds.get(2).intValue());
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
        verify(notificationIdGenerator, times(3)).getNextNotificationId();
    }

    @Test
    public void testThreeNotificationsExceedingMaxPages() {
        classUnderTest.publishNotification(buildNotification("12345678901234567890123456"), 1);

        verify(notificationPublisher, times(3)).publishNotification(lineNotificationCaptor.capture(), notificationIdCaptor.capture());
        List<LineNotification> lineNotifications = lineNotificationCaptor.getAllValues();
        List<Integer> notificationIds = notificationIdCaptor.getAllValues();
        assertEquals("1234567890(...)", lineNotifications.get(0).getMessage());
        assertEquals(2, notificationIds.get(0).intValue());
        assertEquals("(...)12345(...)", lineNotifications.get(1).getMessage());
        assertEquals(3, notificationIds.get(1).intValue());
        assertEquals("(...)67890(...)", lineNotifications.get(2).getMessage());
        assertEquals(4, notificationIds.get(2).intValue());
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
        verify(notificationIdGenerator, times(3)).getNextNotificationId();
    }

    @Test
    public void testTwoNotificationsWithEnglishWords() {
        classUnderTest.publishNotification(buildNotification("i am testing a sentence"), 1);

        verify(notificationPublisher, times(3)).publishNotification(lineNotificationCaptor.capture(), notificationIdCaptor.capture());
        List<LineNotification> lineNotifications = lineNotificationCaptor.getAllValues();
        List<Integer> notificationIds = notificationIdCaptor.getAllValues();
        assertEquals("i am(...)", lineNotifications.get(0).getMessage());
        assertEquals(2, notificationIds.get(0).intValue());
        assertEquals("(...)testi(...)", lineNotifications.get(1).getMessage());
        assertEquals(3, notificationIds.get(1).intValue());
        assertEquals("(...)ng a(...)", lineNotifications.get(2).getMessage());
        assertEquals(4, notificationIds.get(2).intValue());
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
        verify(notificationIdGenerator, times(3)).getNextNotificationId();
    }

    @Test
    public void testMultipleNotificationsWithUrl() {
        classUnderTest.publishNotification(buildNotification("123456789012345 http://www.google.com"), 1);

        verify(notificationPublisher, times(2)).publishNotification(lineNotificationCaptor.capture(), notificationIdCaptor.capture());
        List<LineNotification> lineNotifications = lineNotificationCaptor.getAllValues();
        List<Integer> notificationIds = notificationIdCaptor.getAllValues();
        assertEquals("1234567890(...)", lineNotifications.get(0).getMessage());
        assertEquals(2, notificationIds.get(0).intValue());
        assertEquals("(...)12345 http://www.google.com", lineNotifications.get(1).getMessage());
        assertEquals(3, notificationIds.get(1).intValue());
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
        verify(notificationIdGenerator, times(2)).getNextNotificationId();
    }

    @Test
    public void testMultipleNotificationsStartingWithUrl() {
        classUnderTest.publishNotification(buildNotification("http://google.com 1234567890"), 1);

        verify(notificationPublisher, times(2)).publishNotification(lineNotificationCaptor.capture(), notificationIdCaptor.capture());
        List<LineNotification> lineNotifications = lineNotificationCaptor.getAllValues();
        List<Integer> notificationIds = notificationIdCaptor.getAllValues();
        assertEquals("http://google.com(...)", lineNotifications.get(0).getMessage());
        assertEquals(2, notificationIds.get(0).intValue());
        assertEquals("(...)1234567890", lineNotifications.get(1).getMessage());
        assertEquals(3, notificationIds.get(1).intValue());
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
        verify(notificationIdGenerator, times(2)).getNextNotificationId();
    }

    private LineNotification buildNotification(String message) {
        return LineNotification.builder()
                .message(message)
                .build();
    }

}