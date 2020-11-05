package com.mysticwind.linenotificationsupport.utils;

import androidx.core.app.Person;

import com.mysticwind.linenotificationsupport.model.LineNotification;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class MessageDeduperTest {

    private static final String SAME_MESSAGE = "sameMessage";
    private static final String SAME_MESSAGE_TWO = "sameMessage (2)";
    private static final String SAME_MESSAGE_THREE = "sameMessage (3)";
    private static final String DIFFERENT_MESSAGE = "differentMessage";
    private static final String CHAT_ID_1 = "chatId1";
    private static final String CHAT_ID_2 = "chatId2";

    @Test
    public void testEvaluate() {
        MessageDeduper classUnderTest = new MessageDeduper();
        assertFalse(classUnderTest.evaluate(buildNotification(SAME_MESSAGE, CHAT_ID_1), 1).isPresent());
        assertFalse(classUnderTest.evaluate(buildNotification(SAME_MESSAGE, CHAT_ID_2), 2).isPresent());
        MessageDeduper.DedupeResult dedupeResult1 = classUnderTest.evaluate(buildNotification(SAME_MESSAGE, CHAT_ID_1), 3).get();
        assertEquals(1, dedupeResult1.getNotificationId());
        assertEquals(SAME_MESSAGE_TWO, dedupeResult1.getReplacedMessage());
        MessageDeduper.DedupeResult dedupeResult2 = classUnderTest.evaluate(buildNotification(SAME_MESSAGE, CHAT_ID_1), 4).get();
        assertEquals(1, dedupeResult2.getNotificationId());
        assertEquals(SAME_MESSAGE_THREE, dedupeResult2.getReplacedMessage());
        assertFalse(classUnderTest.evaluate(buildNotification(DIFFERENT_MESSAGE, CHAT_ID_1), 5).isPresent());
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