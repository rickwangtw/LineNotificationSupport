package com.mysticwind.linenotificationsupport.notification;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.NotificationHistoryEntry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class HistoryProvidingNotificationPublisherDecoratorTest {

    private static final int NOTIFICATION_ID = 1;
    private static final int NOTIFICATION_ID_2 = 2;
    private static final String CHAT_ID = "chatId";
    private static final String LINE_MESSAGE_ID = "messageId";
    private static final String LINE_MESSAGE_ID_2 = "messageId2";
    private static final String MESSAGE = "message";
    private static final String UPDATED_MESSAGE = "updatedMessage";

    @Mock
    private NotificationPublisher notificationPublisher;

    private HistoryProvidingNotificationPublisherDecorator classUnderTest;

    @Before
    public void setUp() throws Exception {
        classUnderTest = new HistoryProvidingNotificationPublisherDecorator(notificationPublisher);
    }

    @Test
    public void publishNotificationReplacingPreviousNotification() {
        classUnderTest.publishNotification(
                LineNotification.builder()
                        .lineMessageId(LINE_MESSAGE_ID)
                        .message(MESSAGE)
                        .chatId(CHAT_ID)
                        .timestamp(1L)
                        .build(),
                NOTIFICATION_ID);

        verify(notificationPublisher).publishNotification(
                LineNotification.builder()
                        .lineMessageId(LINE_MESSAGE_ID)
                        .message(MESSAGE)
                        .chatId(CHAT_ID)
                        .timestamp(1L)
                        .history(Collections.EMPTY_LIST)
                        .build(),
                NOTIFICATION_ID);

        classUnderTest.publishNotification(
                LineNotification.builder()
                        .lineMessageId(LINE_MESSAGE_ID)
                        .message(UPDATED_MESSAGE)
                        .chatId(CHAT_ID)
                        .timestamp(2L)
                        .build(),
                NOTIFICATION_ID_2);

        verify(notificationPublisher).publishNotification(
                LineNotification.builder()
                        .lineMessageId(LINE_MESSAGE_ID)
                        .message(UPDATED_MESSAGE)
                        .chatId(CHAT_ID)
                        .timestamp(2L)
                        .history(Collections.EMPTY_LIST)
                        .build(),
                NOTIFICATION_ID);

        classUnderTest.publishNotification(
                LineNotification.builder()
                        .lineMessageId(LINE_MESSAGE_ID_2)
                        .message(MESSAGE)
                        .chatId(CHAT_ID)
                        .timestamp(3L)
                        .build(),
                NOTIFICATION_ID_2);

        verify(notificationPublisher).publishNotification(
                LineNotification.builder()
                        .lineMessageId(LINE_MESSAGE_ID_2)
                        .message(MESSAGE)
                        .chatId(CHAT_ID)
                        .timestamp(3L)
                        .history(ImmutableList.of(
                                new NotificationHistoryEntry(LINE_MESSAGE_ID, UPDATED_MESSAGE, null, 2L, null)
                        ))
                        .build(),
                NOTIFICATION_ID);

        classUnderTest.publishNotification(
                LineNotification.builder()
                        .lineMessageId(LINE_MESSAGE_ID_2)
                        .message(UPDATED_MESSAGE)
                        .chatId(CHAT_ID)
                        .timestamp(4L)
                        .build(),
                NOTIFICATION_ID_2);

        verify(notificationPublisher).publishNotification(
                LineNotification.builder()
                        .lineMessageId(LINE_MESSAGE_ID_2)
                        .message(UPDATED_MESSAGE)
                        .chatId(CHAT_ID)
                        .timestamp(4L)
                        .history(ImmutableList.of(
                                new NotificationHistoryEntry(LINE_MESSAGE_ID, UPDATED_MESSAGE, null, 2L, null)
                        ))
                        .build(),
                NOTIFICATION_ID);
    }

}