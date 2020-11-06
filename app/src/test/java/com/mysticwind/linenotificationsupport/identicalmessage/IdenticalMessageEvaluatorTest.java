package com.mysticwind.linenotificationsupport.identicalmessage;

import androidx.core.app.Person;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;

public class IdenticalMessageEvaluatorTest {

    private static final String MESSAGE = "message";
    private static final String DIFFERENT_MESSAGE = "differentMessage";
    private static final String CHAT_ID_1 = "chatId1";
    private static final String CHAT_ID_2 = "chatId2";

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

    private LineNotification buildNotification(String message, String chatId) {
        return LineNotification.builder()
                .title("title")
                .message(message)
                .chatId(chatId)
                .sender(new Person.Builder()
                        .setName("sender")
                        .build())
                .timestamp(new Random().nextInt())
                .build();
    }

}