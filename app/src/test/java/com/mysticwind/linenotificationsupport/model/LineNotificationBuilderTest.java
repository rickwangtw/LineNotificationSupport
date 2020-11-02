package com.mysticwind.linenotificationsupport.model;

import android.app.Notification;
import android.content.Context;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.utils.ChatTitleAndSenderResolver;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO test more cases
@RunWith(MockitoJUnitRunner.class)
public class LineNotificationBuilderTest {

    private static final String CHAT_ID = "ChatId";
    private static final String CONVERSATION_TITLE = "GroupName";
    private static final String ANDROID_TEXT = "Message";
    private static final String ANDROID_TITLE = "GroupName：Sender";
    private static final String TITLE = "title";
    private static final String SENDER = "sender";

    @Mock
    private Context mockedContext;

    @Mock
    private ChatTitleAndSenderResolver mockedChatTitleAndSenderResolver;

    @Mock
    private StatusBarNotification mockedStatusBarNotification;

    @Mock
    private Notification mockedNotification;

    @Mock
    private Bundle mockedExtras;

    private LineNotificationBuilder classUnderTest;

    @Before
    public void setUp() {
        when(mockedChatTitleAndSenderResolver.resolveTitleAndSender(any())).thenReturn(Pair.of(TITLE, SENDER));
        mockedNotification.extras = this.mockedExtras;

        classUnderTest = new LineNotificationBuilder(mockedContext, mockedChatTitleAndSenderResolver);
    }

    @Test
    public void testChatReturnsChatId() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(CHAT_ID, null, null));

        assertEquals(CHAT_ID, lineNotification.getChatId());
    }

    @Test
    public void testCallCategoryReturnsCallChatId() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(null, LineNotificationBuilder.CALL_CATEGORY, null));

        assertEquals(LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID, lineNotification.getChatId());
    }

    @Test
    public void testMissedCallTagReturnsCallChatId() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(null, null, LineNotificationBuilder.MISSED_CALL_TAG));

        assertEquals(LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID, lineNotification.getChatId());
    }

    @Test
    public void testNoChatIdReturnsDefaultChatId() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(null, null, null));

        assertEquals(LineNotificationBuilder.DEFAULT_CHAT_ID, lineNotification.getChatId());
    }

    // TODO add more helper methods
    private StatusBarNotification buildNotification(final String lineChatId, final String category, final String tag) {
        when(mockedExtras.getString("android.conversationTitle")).thenReturn(CONVERSATION_TITLE);
        when(mockedExtras.getString("android.text")).thenReturn(ANDROID_TEXT);
        when(mockedExtras.getString("android.title")).thenReturn(ANDROID_TITLE);
        when(mockedExtras.getString("line.chat.id")).thenReturn(lineChatId);
        when(mockedNotification.getLargeIcon()).thenReturn(null);

        mockedNotification.category = category;

        when(mockedStatusBarNotification.getNotification()).thenReturn(mockedNotification);
        when(mockedStatusBarNotification.getTag()).thenReturn(tag);

        return mockedStatusBarNotification;
    }

    @After
    public void verifyMocks() {
        verify(mockedChatTitleAndSenderResolver).resolveTitleAndSender(any());
    }

}