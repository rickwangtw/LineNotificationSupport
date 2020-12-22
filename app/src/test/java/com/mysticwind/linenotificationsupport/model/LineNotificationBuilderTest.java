package com.mysticwind.linenotificationsupport.model;

import android.app.Notification;
import android.content.Context;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.ImmutableList;
import com.mysticwind.linenotificationsupport.components.helper.StatusBarNotificationBuilder;
import com.mysticwind.linenotificationsupport.preference.PreferenceProvider;
import com.mysticwind.linenotificationsupport.utils.ChatTitleAndSenderResolver;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationPrinter;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

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
    private StatusBarNotificationPrinter mockedStatusBarNotificationPrinter;

    @Mock
    private PreferenceProvider mockedPreferenceProvider;

    @Mock
    private Notification.Action action1;

    @Mock
    private Notification.Action action2;

    private LineNotificationBuilder classUnderTest;

    @Before
    public void setUp() {
        when(mockedChatTitleAndSenderResolver.resolveTitleAndSender(any())).thenReturn(Pair.of(TITLE, SENDER));
        when(mockedPreferenceProvider.shouldUseMergeMessageChatId()).thenReturn(false);

        classUnderTest = new LineNotificationBuilder(mockedContext, mockedChatTitleAndSenderResolver, mockedStatusBarNotificationPrinter);
    }

    @Test
    public void testMessageWithSecondAction() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(CHAT_ID, LineNotificationBuilder.MESSAGE_CATEGORY, null, null, false));

        assertEquals(ImmutableList.of(action2), lineNotification.getActions());
    }

    @Test
    public void testIncomingCallWithTwoActions() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(CHAT_ID, LineNotificationBuilder.CALL_CATEGORY, null, null, false));

        assertEquals(ImmutableList.of(action2, action1), lineNotification.getActions());
    }

    @Test
    public void testIncomingCallWithOneActionBecauseOnlyOneActionFromLine() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(CHAT_ID, LineNotificationBuilder.CALL_CATEGORY, null, null, true));

        assertEquals(ImmutableList.of(action1), lineNotification.getActions());
    }

    @Test
    public void testMissedCallWithFirstAction() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(CHAT_ID, null, LineNotificationBuilder.MISSED_CALL_TAG, null, false));

        assertEquals(ImmutableList.of(action2), lineNotification.getActions());
    }

    @Test
    public void testMissedCallWithNoActionsBecauseOnlyOneActionFromLine() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(CHAT_ID, null, LineNotificationBuilder.MISSED_CALL_TAG, null, true));

        assertEquals(Collections.EMPTY_LIST, lineNotification.getActions());
    }

    @Test
    public void testInACallWithFirstAction() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(CHAT_ID, LineNotificationBuilder.MESSAGE_CATEGORY, null, LineNotificationBuilder.GENERAL_NOTIFICATION_CHANNEL, false));

        assertEquals(ImmutableList.of(action1), lineNotification.getActions());
    }

    @Test
    public void testChatReturnsChatId() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(CHAT_ID, null, null, null, false));

        assertEquals(CHAT_ID, lineNotification.getChatId());
    }

    @Test
    public void testCallCategoryReturnsCallChatId() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(null, LineNotificationBuilder.CALL_CATEGORY, null, null, false));

        assertEquals(LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID, lineNotification.getChatId());
    }

    @Test
    public void testMissedCallTagReturnsCallChatId() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(null, null, LineNotificationBuilder.MISSED_CALL_TAG, null, false));

        assertEquals(LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID, lineNotification.getChatId());
    }

    @Test
    public void testNoChatIdReturnsDefaultChatId() {
        LineNotification lineNotification = classUnderTest.from(buildNotification(null, null, null, null, false));

        assertEquals(LineNotificationBuilder.DEFAULT_CHAT_ID, lineNotification.getChatId());
    }

    @Test
    public void testBuilderWithSender() {
        when(mockedChatTitleAndSenderResolver.resolveTitleAndSender(any(StatusBarNotification.class))).thenReturn(Pair.of(TITLE, SENDER));

        LineNotification lineNotification = classUnderTest.from(buildNotification(CHAT_ID, null, null, null, false));

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CHAT_ID, lineNotification.getChatId());
    }

    @Test
    public void testNoSenderReturnsDefaultSender() {
        when(mockedChatTitleAndSenderResolver.resolveTitleAndSender(any(StatusBarNotification.class))).thenReturn(Pair.of(TITLE, null));

        LineNotification lineNotification = classUnderTest.from(buildNotification(CHAT_ID, null, null, null, false));

        assertEquals(LineNotificationBuilder.DEFAULT_SENDER_NAME, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CHAT_ID, lineNotification.getChatId());
    }

    private StatusBarNotification buildNotification(final String lineChatId, final String category, final String tag, String notificationChannel, boolean hasSingleAction) {
        StatusBarNotificationBuilder statusBarNotificationBuilder = new StatusBarNotificationBuilder()
                .withAndroidText(ANDROID_TEXT)
                .withAndroidConversationTitle(CONVERSATION_TITLE)
                .withAndroidTitle(ANDROID_TITLE)
                .withLineChatId(lineChatId)
                .withChannelId(notificationChannel)
                .withCategory(category)
                .withTag(tag);

        if (hasSingleAction) {
            statusBarNotificationBuilder.withActions(action1);
        } else {
            statusBarNotificationBuilder.withActions(action1, action2);
        }
        return statusBarNotificationBuilder.build();
    }

    @After
    public void verifyMocks() {
        verify(mockedChatTitleAndSenderResolver).resolveTitleAndSender(any());
    }

}