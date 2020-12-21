package com.mysticwind.linenotificationsupport.notification;

import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BigNotificationSplittingNotificationPublisherDecoratorTest {

    @Mock
    private NotificationPublisher notificationPublisher;

    @Mock
    private PreferenceProvider preferenceProvider;

    @Captor
    private ArgumentCaptor<LineNotification> lineNotificationCaptor;

    private BigNotificationSplittingNotificationPublisherDecorator classUnderTest;

    @Before
    public void setUp() {
        when(preferenceProvider.getMessageSizeLimit()).thenReturn(15);
        when(preferenceProvider.getMaxPageCount()).thenReturn(3);

        classUnderTest = new BigNotificationSplittingNotificationPublisherDecorator(notificationPublisher, preferenceProvider);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(
                notificationPublisher,
                preferenceProvider
        );
    }

    @Test
    public void testSingleNotification() {
        classUnderTest.publishNotification(buildNotification("123456789012345"), 1);

        verify(notificationPublisher).publishNotification(lineNotificationCaptor.capture(), eq(1));
        assertEquals("123456789012345", lineNotificationCaptor.getValue().getMessage());
        verify(preferenceProvider).getMessageSizeLimit();
    }

    @Test
    public void testTwoNotifications() {
        classUnderTest.publishNotification(buildNotification("1234567890123456"), 1);

        verify(notificationPublisher).publishNotification(lineNotificationCaptor.capture(), eq(1));
        LineNotification lineNotification = lineNotificationCaptor.getValue();
        assertEquals("1234567890(...)", lineNotification.getMessages().get(0));
        assertEquals("(...)123456", lineNotification.getMessages().get(1));
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
    }

    @Test
    public void testTwoNotificationsOnEdge() {
        classUnderTest.publishNotification(buildNotification("12345678901234567890"), 1);

        verify(notificationPublisher).publishNotification(lineNotificationCaptor.capture(), eq(1));
        LineNotification lineNotification = lineNotificationCaptor.getValue();
        assertEquals("1234567890(...)", lineNotification.getMessages().get(0));
        assertEquals("(...)1234567890", lineNotification.getMessages().get(1));
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
    }

    @Test
    public void testThreeNotifications() {
        classUnderTest.publishNotification(buildNotification("123456789012345678901"), 1);

        verify(notificationPublisher).publishNotification(lineNotificationCaptor.capture(), eq(1));
        LineNotification lineNotification = lineNotificationCaptor.getValue();
        assertEquals("1234567890(...)", lineNotification.getMessages().get(0));
        assertEquals("(...)12345(...)", lineNotification.getMessages().get(1));
        assertEquals("(...)678901", lineNotification.getMessages().get(2));
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
    }

    @Test
    public void testThreeNotificationsOnEdge() {
        classUnderTest.publishNotification(buildNotification("1234567890123456789012345"), 1);

        verify(notificationPublisher).publishNotification(lineNotificationCaptor.capture(), eq(1));
        LineNotification lineNotification = lineNotificationCaptor.getValue();
        assertEquals("1234567890(...)", lineNotification.getMessages().get(0));
        assertEquals("(...)12345(...)", lineNotification.getMessages().get(1));
        assertEquals("(...)6789012345", lineNotification.getMessages().get(2));
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
    }

    @Test
    public void testThreeNotificationsExceedingMaxPages() {
        classUnderTest.publishNotification(buildNotification("12345678901234567890123456"), 1);

        verify(notificationPublisher).publishNotification(lineNotificationCaptor.capture(), eq(1));
        LineNotification lineNotification = lineNotificationCaptor.getValue();
        assertEquals("1234567890(...)", lineNotification.getMessages().get(0));
        assertEquals("(...)12345(...)", lineNotification.getMessages().get(1));
        assertEquals("(...)67890(...)", lineNotification.getMessages().get(2));
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
    }

    @Test
    public void testTwoNotificationsWithEnglishWords() {
        classUnderTest.publishNotification(buildNotification("i am testing a sentence"), 1);

        verify(notificationPublisher).publishNotification(lineNotificationCaptor.capture(), eq(1));
        LineNotification lineNotification = lineNotificationCaptor.getValue();
        assertEquals("i am(...)", lineNotification.getMessages().get(0));
        assertEquals("(...)testi(...)", lineNotification.getMessages().get(1));
        assertEquals("(...)ng a(...)", lineNotification.getMessages().get(2));
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
    }

    @Test
    public void testMultipleNotificationsWithUrl() {
        classUnderTest.publishNotification(buildNotification("123456789012345 http://www.google.com"), 1);

        verify(notificationPublisher).publishNotification(lineNotificationCaptor.capture(), eq(1));
        LineNotification lineNotification = lineNotificationCaptor.getValue();
        assertEquals("1234567890(...)", lineNotification.getMessages().get(0));
        assertEquals("(...)12345 http://www.google.com", lineNotification.getMessages().get(1));
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
    }

    @Test
    public void testMultipleNotificationsStartingWithUrl() {
        classUnderTest.publishNotification(buildNotification("http://google.com 1234567890"), 1);

        verify(notificationPublisher).publishNotification(lineNotificationCaptor.capture(), eq(1));
        LineNotification lineNotification = lineNotificationCaptor.getValue();
        assertEquals("http://google.com (...)", lineNotification.getMessages().get(0));
        assertEquals("(...)1234567890", lineNotification.getMessages().get(1));
        verify(preferenceProvider).getMessageSizeLimit();
        verify(preferenceProvider).getMaxPageCount();
    }

    private LineNotification buildNotification(String message) {
        return LineNotification.builder()
                .message(message)
                .build();
    }

}