package com.mysticwind.linenotificationsupport.model;

import static org.junit.Assert.assertTrue;

import androidx.core.app.Person;

import org.junit.Test;

import java.time.Instant;

public class LineNotificationTest {

    private static final String TITLE = "title";
    private static final String MESSAGE = "message";
    private static final String CHAT_ID = "chatId";

    @Test
    public void testSelfResponse() {
        LineNotification lineNotification = LineNotification.builder()
                .lineMessageId(String.valueOf(Instant.now().toEpochMilli())) // just generate a fake one
                .title(TITLE)
                .message(MESSAGE)
                .sender(new Person.Builder().setName("You").build()) // TODO localization
                .chatId(CHAT_ID)
                .timestamp(Instant.now().toEpochMilli())
                .isSelfResponse(true)
                .build();

        assertTrue(lineNotification.isSelfResponse());
    }

}