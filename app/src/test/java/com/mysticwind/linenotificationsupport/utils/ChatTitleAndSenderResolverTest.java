package com.mysticwind.linenotificationsupport.utils;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.components.helper.StatusBarNotificationBuilder;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class ChatTitleAndSenderResolverTest {

    private static final ChatTitleAndSenderResolver CLASS_UNDER_TEST = new ChatTitleAndSenderResolver();

    private static final String EXPECTED_GROUP_NAME = "GroupName";
    private static final String EXPECTED_MESSAGE = "Message";
    private static final String EXPECTED_SENDER = "Sender";
    private static final String CHAT_ID = "ChatId";
    private static final String ACTUAL_CONVERSATION_TITLE = "GroupName";
    private static final String ACTUAL_ANDROID_TEXT = "Message";
    private static final String ACTUAL_ANDROID_TITLE = "GroupName：Sender";
    private static final String ACTUAL_TICKER_TEXT_WITH_MESSAGE = "Sender : Message";
    private static final String ACTUAL_ANDROID_TITLE_WITH_STICKER = "Sender 傳送了貼圖";
    private static final String ACTUAL_TICKER_TEXT_WITH_STICKER = "Sender 傳送了貼圖";

    @Mock
    StatusBarNotification statusBarNotification;

    @Mock
    private Bundle extras;

    @Test
    public void testHappyCase() {
        Pair<String, String> titleAndSender = CLASS_UNDER_TEST.resolveTitleAndSender(
                buildNotification(
                        ACTUAL_CONVERSATION_TITLE,
                        ACTUAL_ANDROID_TEXT,
                        ACTUAL_ANDROID_TITLE,
                        CHAT_ID,
                        ACTUAL_TICKER_TEXT_WITH_MESSAGE
                )
        );

        assertEquals(EXPECTED_GROUP_NAME, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
    }

    @Test
    @Ignore("TODO: fix the Log.w in resolveTitleAndSender()")
    public void testGroupTitleCaching() {
        final ChatTitleAndSenderResolver classUnderTest = new ChatTitleAndSenderResolver();
        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                new StatusBarNotificationBuilder()
                        .withAndroidConversationTitle(ACTUAL_CONVERSATION_TITLE)
                        .withAndroidText(ACTUAL_ANDROID_TEXT)
                        .withAndroidTitle(ACTUAL_ANDROID_TITLE)
                        .withLineChatId(CHAT_ID)
                        .withTickerText(ACTUAL_TICKER_TEXT_WITH_MESSAGE)
                        .build()
        );
        assertEquals(EXPECTED_GROUP_NAME, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());

        // the real test - this should get the same chat ID even if not provided
        Pair<String, String> titleAndSenderWithoutChatId = classUnderTest.resolveTitleAndSender(
                new StatusBarNotificationBuilder()
                        .withAndroidText(ACTUAL_ANDROID_TEXT)
                        .withAndroidTitle(EXPECTED_SENDER)
                        .withTickerText(ACTUAL_TICKER_TEXT_WITH_MESSAGE)
                        .withLineChatId(CHAT_ID)
                        .build()
        );
        assertEquals(EXPECTED_GROUP_NAME, titleAndSenderWithoutChatId.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSenderWithoutChatId.getRight());
    }

    @Test
    public void testStickerShouldNotBlowUpResolver() {
        Pair<String, String> titleAndSender = CLASS_UNDER_TEST.resolveTitleAndSender(
                buildNotification(
                        ACTUAL_CONVERSATION_TITLE,
                        ACTUAL_ANDROID_TITLE_WITH_STICKER,
                        ACTUAL_ANDROID_TITLE,
                        CHAT_ID,
                        ACTUAL_TICKER_TEXT_WITH_STICKER
                )
        );

        assertEquals(EXPECTED_GROUP_NAME, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
    }

    private StatusBarNotification buildNotification(final String conversationTitle,
                                                    final String androidText,
                                                    final String androidTitle,
                                                    final String lineChatId,
                                                    final String tickerText) {
        Notification notification = new Notification();
        notification.extras = this.extras;
        when(extras.getString("android.conversationTitle")).thenReturn(conversationTitle);
        when(extras.getString("android.text")).thenReturn(androidText);
        when(extras.getString("android.title")).thenReturn(androidTitle);
        when(extras.getString("line.chat.id")).thenReturn(lineChatId);
        notification.tickerText = tickerText;

        when(statusBarNotification.getNotification()).thenReturn(notification);
        return statusBarNotification;
    }

}