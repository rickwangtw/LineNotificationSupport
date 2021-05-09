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

import static com.mysticwind.linenotificationsupport.model.LineNotificationBuilder.CALL_VIRTUAL_CHAT_ID;
import static com.mysticwind.linenotificationsupport.model.LineNotificationBuilder.DEFAULT_CHAT_ID;
import static com.mysticwind.linenotificationsupport.model.LineNotificationBuilder.GENERAL_NOTIFICATION_CHANNEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// TODO test more cases
@RunWith(MockitoJUnitRunner.class)
public class LineNotificationBuilderTest {

    private static final String CONVERSATION_TITLE_GROUP_NAME = "GroupName";
    private static final String CONVERSATION_TITLE_NULL = null;
    private static final String TEXT_FULL_MESSAGE = "Message - LONG";
    private static final String TEXT_NEW_MESSAGE = "You have a new message.";
    private static final String TEXT_INCOMING_CALL = "LINE語音通話來電中…";
    private static final String TEXT_ONGOING_CALL = "LINE通話中";
    private static final String TEXT_SENDER = "Sender";
    private static final String TEXT_INCOMING_VIDEO_CALL = "LINE視訊通話來電中…";
    private static final String TEXT_INDIVIDUAL_JOINED_GROUP_CHAT = "Sender已加入「GroupName」。";
    private static final String TEXT_SENDER_WITH_STICKER = "Sender 傳送了貼圖";
    private static final String CHAT_ID = "ChatId";
    private static final String CHAT_ID_NULL = null;
    private static final String TICKER_TEXT_SENDER_AND_SHORT_MESSAGE = "Sender : Message";
    private static final String TICKER_TEXT_SENDER_AND_NEW_MESSAGE = "Sender : You have a new message.";
    private static final String TICKER_TEXT_SENDER_AND_INCOMING_CALL_MESSAGE = "Sender : LINE語音通話來電中…";
    private static final String TICKER_TEXT_SENDER_AND_ONGOING_CALL_MESSAGE = "Sender : LINE通話中";
    private static final String TICKER_TEXT_MISSED_CALL_AND_SENDER = "LINE未接來電 : Sender";
    private static final String TICKER_TEXT_SENDER_AND_INCOMING_VIDEO_CALL_MESSAGE = "Sender : LINE視訊通話來電中…";
    private static final String TICKER_TEXT_INDIVIDUAL_JOINED_GROUP_CHAT = "Sender已加入「GroupName」。";
    private static final String TICKER_TEXT_SENDER_WITH_STICKER = "Sender 傳送了貼圖";
    private static final String TITLE_GROUPNAME_AND_SENDER = "GroupName：Sender";
    private static final String TITLE_SENDER = "Sender";
    private static final String TITLE_MISSED_CALL = "LINE未接來電";
    private static final String TITLE_LINE = "LINE";
    private static final String TAG_MESSAGE = "NOTIFICATION_TAG_MESSAGE";
    private static final String TAG_NULL = null;
    private static final String TAG_MISSED_CALL = "NOTIFICATION_TAG_MISSED_CALL";
    private static final String TAG_GROUP = "NOTIFICATION_TAG_GROUP";
    private static final String NOTIFICATION_CHANNEL_NEW_MESSAGES = "jp.naver.line.android.notification.NewMessages";
    private static final String NOTIFICATION_CHANNEL_CALLS = "jp.naver.line.android.notification.Calls";
    private static final String NOTIFICATION_CHANNEL_FRIEND_REQUESTS = "jp.naver.line.android.notification.FriendRequests";

    private static final String TITLE = "title";
    private static final String SENDER = "sender";
    private static final String LINE_MESSAGE_ID = "lineMessageId";

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
        when(mockedChatTitleAndSenderResolver.resolveTitleAndSender(any(StatusBarNotification.class))).thenReturn(Pair.of(TITLE, SENDER));
        when(mockedPreferenceProvider.shouldUseMergeMessageChatId()).thenReturn(false);

        classUnderTest = new LineNotificationBuilder(mockedContext, mockedChatTitleAndSenderResolver, mockedStatusBarNotificationPrinter);
    }

    @Test
    public void testGroupChatWithGroupName() {
        LineNotification lineNotification = classUnderTest.from(buildGroupChatWithGroupNameNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CHAT_ID, lineNotification.getChatId());
        assertNull(lineNotification.getCallState());
        assertEquals(ImmutableList.of(action2), lineNotification.getActions());
        assertEquals(LINE_MESSAGE_ID, lineNotification.getLineMessageId());
    }

    @Test
    public void testGroupChatWithMissingGroupNameNotification() {
        LineNotification lineNotification = classUnderTest.from(buildGroupChatWithMissingGroupNameNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CHAT_ID, lineNotification.getChatId());
        assertNull(lineNotification.getCallState());
        assertEquals(ImmutableList.of(action2), lineNotification.getActions());
        assertEquals(LINE_MESSAGE_ID, lineNotification.getLineMessageId());
    }

    @Test
    public void testNewMessageNotification() {
        LineNotification lineNotification = classUnderTest.from(buildNewMessageNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CHAT_ID, lineNotification.getChatId());
        assertNull(lineNotification.getCallState());
        assertTrue(lineNotification.getActions().isEmpty());
        assertEquals(LINE_MESSAGE_ID, lineNotification.getLineMessageId());
    }

    @Test
    public void testReplacingNewMessageNotification() {
        LineNotification lineNotification = classUnderTest.from(buildOneOnOneMessageNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CHAT_ID, lineNotification.getChatId());
        assertNull(lineNotification.getCallState());
        assertEquals(ImmutableList.of(action2), lineNotification.getActions());
        assertEquals(LINE_MESSAGE_ID, lineNotification.getLineMessageId());
    }

    @Test
    public void testOneOnOneMessageNotification() {
        LineNotification lineNotification = classUnderTest.from(buildOneOnOneMessageNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CHAT_ID, lineNotification.getChatId());
        assertNull(lineNotification.getCallState());
        assertEquals(ImmutableList.of(action2), lineNotification.getActions());
        assertEquals(LINE_MESSAGE_ID, lineNotification.getLineMessageId());
    }

    @Test
    public void testIncomingCallNotification() {
        LineNotification lineNotification = classUnderTest.from(buildIncomingCallNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CALL_VIRTUAL_CHAT_ID, lineNotification.getChatId());
        assertEquals(LineNotification.CallState.INCOMING, lineNotification.getCallState());
        assertEquals(ImmutableList.of(action2, action1), lineNotification.getActions());
        assertNull(lineNotification.getLineMessageId());
    }

    @Test
    public void testOngoingCallNotification() {
        LineNotification lineNotification = classUnderTest.from(buildOngoingCallNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CALL_VIRTUAL_CHAT_ID, lineNotification.getChatId());
        assertEquals(LineNotification.CallState.IN_A_CALL, lineNotification.getCallState());
        assertEquals(ImmutableList.of(action1), lineNotification.getActions());
        assertNull(lineNotification.getLineMessageId());
    }

    @Test
    public void testMissedCallNotification() {
        LineNotification lineNotification = classUnderTest.from(buildMissedCallNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CALL_VIRTUAL_CHAT_ID, lineNotification.getChatId());
        assertEquals(LineNotification.CallState.MISSED_CALL, lineNotification.getCallState());
        assertEquals(ImmutableList.of(action2), lineNotification.getActions());
        assertNull(lineNotification.getLineMessageId());
    }

    @Test
    public void testIncomingVideoCallNotification() {
        LineNotification lineNotification = classUnderTest.from(buildIncomingVideoCallNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CALL_VIRTUAL_CHAT_ID, lineNotification.getChatId());
        assertEquals(LineNotification.CallState.INCOMING, lineNotification.getCallState());
        assertEquals(ImmutableList.of(action2, action1), lineNotification.getActions());
        assertNull(lineNotification.getLineMessageId());
    }

    @Test
    public void testJoinedGroupChatNotification() {
        LineNotification lineNotification = classUnderTest.from(buildJoinedGroupChatNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(DEFAULT_CHAT_ID, lineNotification.getChatId());
        assertTrue(lineNotification.getActions().isEmpty());
        assertNull(lineNotification.getLineMessageId());
    }

    @Test
    public void testGroupMessageWithStickerNotification() {
        LineNotification lineNotification = classUnderTest.from(buildGroupMessageWithStickerNotification());

        assertEquals(SENDER, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CHAT_ID, lineNotification.getChatId());
        assertNull(lineNotification.getCallState());
        assertEquals(ImmutableList.of(action2), lineNotification.getActions());
        assertEquals(LINE_MESSAGE_ID, lineNotification.getLineMessageId());
    }

    @Test
    public void testNoSenderReturnsDefaultSender() {
        when(mockedChatTitleAndSenderResolver.resolveTitleAndSender(any(StatusBarNotification.class))).thenReturn(Pair.of(TITLE, null));

        LineNotification lineNotification = classUnderTest.from(buildOneOnOneMessageNotification());

        assertEquals(LineNotificationBuilder.DEFAULT_SENDER_NAME, lineNotification.getSender().getName());
        assertEquals(TITLE, lineNotification.getTitle());
        assertEquals(CHAT_ID, lineNotification.getChatId());
        assertNull(lineNotification.getCallState());
        assertEquals(ImmutableList.of(action2), lineNotification.getActions());
        assertEquals(LINE_MESSAGE_ID, lineNotification.getLineMessageId());
    }

    private StatusBarNotification buildGroupChatWithGroupNameNotification() {
        return buildNotification(CONVERSATION_TITLE_GROUP_NAME, TEXT_FULL_MESSAGE, TITLE_GROUPNAME_AND_SENDER, CHAT_ID, TICKER_TEXT_SENDER_AND_SHORT_MESSAGE,
                LineNotificationBuilder.MESSAGE_CATEGORY, TAG_MESSAGE, NOTIFICATION_CHANNEL_NEW_MESSAGES, 2, true);
    }

    private StatusBarNotification buildGroupChatWithMissingGroupNameNotification() {
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_FULL_MESSAGE, TITLE_SENDER, CHAT_ID, TICKER_TEXT_SENDER_AND_SHORT_MESSAGE,
                LineNotificationBuilder.MESSAGE_CATEGORY, TAG_MESSAGE, NOTIFICATION_CHANNEL_NEW_MESSAGES, 2, true);
    }

    private StatusBarNotification buildReplacingNewMessageNotification() {
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_FULL_MESSAGE, TITLE_SENDER, CHAT_ID, TICKER_TEXT_SENDER_AND_SHORT_MESSAGE,
                LineNotificationBuilder.MESSAGE_CATEGORY, TAG_MESSAGE, GENERAL_NOTIFICATION_CHANNEL, 2, true);
    }

    private StatusBarNotification buildNewMessageNotification() {
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_NEW_MESSAGE, TITLE_SENDER, CHAT_ID, TICKER_TEXT_SENDER_AND_NEW_MESSAGE,
                LineNotificationBuilder.MESSAGE_CATEGORY, TAG_NULL, NOTIFICATION_CHANNEL_NEW_MESSAGES, 0, true);
    }

    private StatusBarNotification buildOneOnOneMessageNotification() {
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_FULL_MESSAGE, TITLE_SENDER, CHAT_ID, TICKER_TEXT_SENDER_AND_SHORT_MESSAGE,
                LineNotificationBuilder.MESSAGE_CATEGORY, TAG_MESSAGE, NOTIFICATION_CHANNEL_NEW_MESSAGES, 2, true);
    }

    private StatusBarNotification buildIncomingCallNotification() {
        // android.bigText=LINE語音通話來電中…
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_INCOMING_CALL, TITLE_SENDER, CHAT_ID_NULL, TICKER_TEXT_SENDER_AND_INCOMING_CALL_MESSAGE,
                LineNotificationBuilder.CALL_CATEGORY, TAG_NULL, NOTIFICATION_CHANNEL_CALLS, 2, false);
    }

    private StatusBarNotification buildOngoingCallNotification() {
        // android.bigText=LINE通話中
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_ONGOING_CALL, TITLE_SENDER, CHAT_ID_NULL, TICKER_TEXT_SENDER_AND_ONGOING_CALL_MESSAGE,
                LineNotificationBuilder.MESSAGE_CATEGORY, TAG_NULL, GENERAL_NOTIFICATION_CHANNEL, 1, false);
    }

    private StatusBarNotification buildMissedCallNotification() {
        // android.bigText=蕭文鈺
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_SENDER, TITLE_MISSED_CALL, CHAT_ID_NULL, TICKER_TEXT_MISSED_CALL_AND_SENDER,
                LineNotificationBuilder.MESSAGE_CATEGORY, TAG_MISSED_CALL, NOTIFICATION_CHANNEL_NEW_MESSAGES, 2, false);
    }

    private StatusBarNotification buildIncomingVideoCallNotification() {
        // android.bigText=LINE視訊通話來電中…
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_INCOMING_VIDEO_CALL, TITLE_SENDER, CHAT_ID_NULL, TICKER_TEXT_SENDER_AND_INCOMING_VIDEO_CALL_MESSAGE,
                LineNotificationBuilder.CALL_CATEGORY, TAG_NULL, NOTIFICATION_CHANNEL_CALLS, 2 /* TODO confirm */, false);
    }

    private StatusBarNotification buildJoinedGroupChatNotification() {
        // android.bigText=Sender已加入「GroupName」。
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_INDIVIDUAL_JOINED_GROUP_CHAT, TITLE_LINE, CHAT_ID_NULL, TICKER_TEXT_INDIVIDUAL_JOINED_GROUP_CHAT,
                LineNotificationBuilder.MESSAGE_CATEGORY, TAG_GROUP, NOTIFICATION_CHANNEL_FRIEND_REQUESTS, 0, false);
    }
    private StatusBarNotification buildGroupMessageWithStickerNotification() {
        return buildNotification(CONVERSATION_TITLE_GROUP_NAME, TEXT_SENDER_WITH_STICKER, TITLE_GROUPNAME_AND_SENDER, CHAT_ID, TICKER_TEXT_SENDER_WITH_STICKER,
                LineNotificationBuilder.MESSAGE_CATEGORY, TAG_MESSAGE, NOTIFICATION_CHANNEL_NEW_MESSAGES, 2, true);
    }

    private StatusBarNotification buildNotification(final String conversationTitle,
                                                    final String androidText,
                                                    final String androidTitle,
                                                    final String lineChatId,
                                                    final String tickerText,
                                                    final String category,
                                                    final String tag,
                                                    String notificationChannel,
                                                    int actionCount,
                                                    boolean hasMessageId) {
        final StatusBarNotificationBuilder statusBarNotificationBuilder = new StatusBarNotificationBuilder()
                .withAndroidText(androidText)
                .withAndroidConversationTitle(conversationTitle)
                .withAndroidTitle(androidTitle)
                .withLineChatId(lineChatId)
                .withChannelId(notificationChannel)
                .withTickerText(tickerText)
                .withCategory(category)
                .withTag(tag);

        if (actionCount == 1) {
            statusBarNotificationBuilder.withActions(action1);
        } else if (actionCount == 2) {
            statusBarNotificationBuilder.withActions(action1, action2);
        }

        if (hasMessageId) {
            statusBarNotificationBuilder.withLineMessageId(LINE_MESSAGE_ID);
        }

        return statusBarNotificationBuilder.build();
    }

    @After
    public void verifyMocks() {
        verify(mockedChatTitleAndSenderResolver).resolveTitleAndSender(any());
    }

}