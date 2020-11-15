package com.mysticwind.linenotificationsupport.identicalmessage;

import androidx.core.app.Person;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.junit.Test;

import java.util.Random;

import static com.mysticwind.linenotificationsupport.identicalmessage.IdenticalMessageEvaluator.LINE_NOTIFICATION_COMPARATOR;
import static org.junit.Assert.assertEquals;

public class IdenticalMessageEvaluatorTest {

    private static final String MESSAGE = "message";
    private static final String DIFFERENT_MESSAGE = "differentMessage";
    private static final String CHAT_ID_1 = "chatId1";
    private static final String CHAT_ID_2 = "chatId2";
    private static final String SENDER_NAME = "senderName";
    private static final String TITLE = "title";
    private static final String LINE_STICKER_URL = "lineStickerUrl";
    private static final LineNotification FILLED_LINE_NOTIFICATION = LineNotification.builder()
            .message(MESSAGE)
            .sender(new Person.Builder().setName(SENDER_NAME).build())
            .title(TITLE)
            .lineStickerUrl(LINE_STICKER_URL)
            .chatId(CHAT_ID_1)
            .callState(LineNotification.CallState.IN_A_CALL)
            .build();

    @Test
    public void testEvaluate() {
        IdenticalMessageEvaluator classUnderTest = new IdenticalMessageEvaluator();
        assertEquals(IdenticalMessageEvaluator.EvaluationResult.noDuplicate(), classUnderTest.evaluate(buildNotification(MESSAGE, CHAT_ID_1), 1));
        assertEquals(IdenticalMessageEvaluator.EvaluationResult.noDuplicate(), classUnderTest.evaluate(buildNotification(MESSAGE, CHAT_ID_2), 2));
        IdenticalMessageEvaluator.EvaluationResult result1 = classUnderTest.evaluate(buildNotification(MESSAGE, CHAT_ID_1), 3);
        assertEquals(1, result1.getNotificationId().get().intValue());
        assertEquals(2, result1.getNumberOfDuplicates());
        assertEquals(MESSAGE, result1.getPreviousLineNotification().get().getMessage());
        IdenticalMessageEvaluator.EvaluationResult result2 = classUnderTest.evaluate(buildNotification(MESSAGE, CHAT_ID_1), 4);
        assertEquals(1, result2.getNotificationId().get().intValue());
        assertEquals(3, result2.getNumberOfDuplicates());
        assertEquals(MESSAGE, result2.getPreviousLineNotification().get().getMessage());
        IdenticalMessageEvaluator.EvaluationResult result3 = classUnderTest.evaluate(buildNotification(MESSAGE, CHAT_ID_1), 5);
        assertEquals(1, result3.getNotificationId().get().intValue());
        assertEquals(4, result3.getNumberOfDuplicates());
        assertEquals(MESSAGE, result3.getPreviousLineNotification().get().getMessage());
        assertEquals(IdenticalMessageEvaluator.EvaluationResult.noDuplicate(), classUnderTest.evaluate(buildNotification(DIFFERENT_MESSAGE, CHAT_ID_1), 6));
    }

    @Test
    public void testComparisonToPreventNullPointerException() {
        LINE_NOTIFICATION_COMPARATOR.compare(FILLED_LINE_NOTIFICATION, FILLED_LINE_NOTIFICATION);
        LINE_NOTIFICATION_COMPARATOR.compare(FILLED_LINE_NOTIFICATION, FILLED_LINE_NOTIFICATION.toBuilder().message(null).build());
        LINE_NOTIFICATION_COMPARATOR.compare(FILLED_LINE_NOTIFICATION, FILLED_LINE_NOTIFICATION.toBuilder().sender(null).build());
        LINE_NOTIFICATION_COMPARATOR.compare(FILLED_LINE_NOTIFICATION, FILLED_LINE_NOTIFICATION.toBuilder().sender(new Person.Builder().build()).build());
        LINE_NOTIFICATION_COMPARATOR.compare(FILLED_LINE_NOTIFICATION, FILLED_LINE_NOTIFICATION.toBuilder().title(null).build());
        LINE_NOTIFICATION_COMPARATOR.compare(FILLED_LINE_NOTIFICATION, FILLED_LINE_NOTIFICATION.toBuilder().lineStickerUrl(null).build());
        LINE_NOTIFICATION_COMPARATOR.compare(FILLED_LINE_NOTIFICATION, FILLED_LINE_NOTIFICATION.toBuilder().chatId(null).build());
        LINE_NOTIFICATION_COMPARATOR.compare(FILLED_LINE_NOTIFICATION, FILLED_LINE_NOTIFICATION.toBuilder().callState(null).build());
    }

    private LineNotification buildNotification(String message, String chatId) {
        return FILLED_LINE_NOTIFICATION.toBuilder()
                .message(message)
                .chatId(chatId)
                .timestamp(new Random().nextInt())
                .build();
    }

}