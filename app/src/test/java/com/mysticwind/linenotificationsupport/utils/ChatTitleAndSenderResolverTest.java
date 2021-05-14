package com.mysticwind.linenotificationsupport.utils;

import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import com.mysticwind.linenotificationsupport.chatname.ChatNameManager;
import com.mysticwind.linenotificationsupport.components.helper.StatusBarNotificationBuilder;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class ChatTitleAndSenderResolverTest {

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
    private static final String TICKER_TEXT_SENDER_2_AND_SHORT_MESSAGE = "Sender2 : Message";
    private static final String TICKER_TEXT_SENDER_AND_NEW_MESSAGE = "Sender : You have a new message.";
    private static final String TICKER_TEXT_SENDER_AND_INCOMING_CALL_MESSAGE = "Sender : LINE語音通話來電中…";
    private static final String TICKER_TEXT_SENDER_AND_ONGOING_CALL_MESSAGE = "Sender : LINE通話中";
    private static final String TICKER_TEXT_MISSED_CALL_AND_SENDER = "LINE未接來電 : Sender";
    private static final String TICKER_TEXT_SENDER_AND_INCOMING_VIDEO_CALL_MESSAGE = "Sender : LINE視訊通話來電中…";
    private static final String TICKER_TEXT_INDIVIDUAL_JOINED_GROUP_CHAT = "Sender已加入「GroupName」。";
    private static final String TICKER_TEXT_SENDER_WITH_STICKER = "Sender 傳送了貼圖";
    private static final String TITLE_GROUPNAME_AND_SENDER = "GroupName：Sender";
    private static final String TITLE_SENDER = "Sender";
    private static final String TITLE_SENDER_2 = "Sender2";
    private static final String TITLE_MISSED_CALL = "LINE未接來電";
    private static final String TITLE_LINE = "LINE";

    private static final String EXPECTED_GROUP_NAME = "GroupName";
    private static final String EXPECTED_GROUP_NAME_FROM_TWO_SENDERS = "Sender,Sender2";
    private static final String EXPECTED_MESSAGE = "Message";
    private static final String EXPECTED_SENDER = "Sender";
    private static final String EXPECTED_SENDER_2 = "Sender2";
    private static final String ACTUAL_CONVERSATION_TITLE = "GroupName";
    private static final String ACTUAL_ANDROID_TEXT = "Message";
    private static final String ACTUAL_ANDROID_TITLE = "GroupName：Sender";
    private static final String ACTUAL_TICKER_TEXT_WITH_MESSAGE = "Sender : Message";

    @Mock
    StatusBarNotification statusBarNotification;

    @Mock
    private Bundle extras;

    @Mock
    private ChatNameManager chatNameManager;

    private ChatTitleAndSenderResolver classUnderTest;

    @Before
    public void setUp() {
        when(chatNameManager.getChatName(anyString(), anyString(), any())).thenReturn(EXPECTED_GROUP_NAME);

        classUnderTest = new ChatTitleAndSenderResolver(chatNameManager);
    }

    @After
    public void tearDown() {
        verifyNoMoreInteractions(
                chatNameManager
        );
    }

    @Test
    public void testHappyCase() {
        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
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
        verify(chatNameManager).getChatName(CHAT_ID, EXPECTED_SENDER, ACTUAL_CONVERSATION_TITLE);
    }

    @Test
    public void testStickerNotification() {
        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                buildGroupMessageWithStickerNotification()
        );

        assertEquals(EXPECTED_GROUP_NAME, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
        verify(chatNameManager).getChatName(CHAT_ID, EXPECTED_SENDER, EXPECTED_GROUP_NAME);
    }

    @Test
    public void testGroupChatWithGroupName() {
        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                buildGroupChatWithGroupNameNotification()
        );

        assertEquals(EXPECTED_GROUP_NAME, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
        verify(chatNameManager).getChatName(CHAT_ID, EXPECTED_SENDER, EXPECTED_GROUP_NAME);
    }

    @Test
    public void testGroupChatWithMissingGroupName() {
        when(chatNameManager.getChatName(anyString(), anyString(), any())).thenReturn(TITLE_SENDER);

        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                buildGroupChatWithMissingGroupNameNotification()
        );

        assertEquals(EXPECTED_SENDER, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
        verify(chatNameManager).getChatName(CHAT_ID, EXPECTED_SENDER, null);
    }

    @Test
    public void testGroupChatWithMissingGroupNameFromTwoSenders() {
        when(chatNameManager.getChatName(anyString(), anyString(), any()))
                .thenReturn(EXPECTED_SENDER)
                .thenReturn(EXPECTED_GROUP_NAME_FROM_TWO_SENDERS)
                .thenReturn(EXPECTED_GROUP_NAME);

        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                buildGroupChatWithMissingGroupNameNotification()
        );

        assertEquals(EXPECTED_SENDER, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());

        titleAndSender = classUnderTest.resolveTitleAndSender(
                buildGroupChatWithMissingGroupNameNotificationFromSecondSender()
        );

        assertEquals(EXPECTED_GROUP_NAME_FROM_TWO_SENDERS, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER_2, titleAndSender.getRight());

        titleAndSender = classUnderTest.resolveTitleAndSender(
                buildGroupChatWithGroupNameNotification()
        );

        assertEquals(EXPECTED_GROUP_NAME, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
        verify(chatNameManager).getChatName(CHAT_ID, EXPECTED_SENDER, null);
        verify(chatNameManager).getChatName(CHAT_ID, EXPECTED_SENDER_2, null);
        verify(chatNameManager).getChatName(CHAT_ID, EXPECTED_SENDER, EXPECTED_GROUP_NAME);
    }

    @Test
    public void testNewMessage() {
        when(chatNameManager.getChatName(anyString(), anyString(), any())).thenReturn(EXPECTED_SENDER);

        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                buildNewMessageNotification()
        );

        assertEquals(EXPECTED_SENDER, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());

        titleAndSender = classUnderTest.resolveTitleAndSender(
                buildOneOnOneMessageNotification()
        );

        assertEquals(EXPECTED_SENDER, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
        verify(chatNameManager, times(2)).getChatName(CHAT_ID, EXPECTED_SENDER, null);
    }

    @Test
    public void testIncomingCall() {
        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                buildIncomingCallNotification()
        );

        assertEquals(EXPECTED_SENDER, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
    }

    @Test
    public void testOngoingCall() {
        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                buildOngoingCallNotification()
        );

        assertEquals(EXPECTED_SENDER, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
    }

    @Test
    public void testMissedCall() {
        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                buildMissedCallNotification()
        );

        assertEquals(TITLE_MISSED_CALL, titleAndSender.getLeft());
        assertEquals(TITLE_MISSED_CALL, titleAndSender.getRight());

        // TODO should we improve this by using the sender as the title and use ticketText for the message?
        // assertEquals(EXPECTED_SENDER, titleAndSender.getLeft());
        // assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
    }

    @Test
    public void testIncomingVideoCall() {
        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                buildIncomingVideoCallNotification()
        );

        assertEquals(EXPECTED_SENDER, titleAndSender.getLeft());
        assertEquals(EXPECTED_SENDER, titleAndSender.getRight());
    }

    @Test
    public void testJoinedGroupChat() {
        Pair<String, String> titleAndSender = classUnderTest.resolveTitleAndSender(
                buildJoinedGroupChatNotification()
        );

        assertEquals("LINE", titleAndSender.getLeft());
        assertEquals("LINE", titleAndSender.getRight());
    }

    private StatusBarNotification buildGroupChatWithGroupNameNotification() {
        return buildNotification(CONVERSATION_TITLE_GROUP_NAME, TEXT_FULL_MESSAGE, TITLE_GROUPNAME_AND_SENDER, CHAT_ID, TICKER_TEXT_SENDER_AND_SHORT_MESSAGE);
    }

    private StatusBarNotification buildGroupChatWithMissingGroupNameNotification() {
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_FULL_MESSAGE, TITLE_SENDER, CHAT_ID, TICKER_TEXT_SENDER_AND_SHORT_MESSAGE);
    }

    private StatusBarNotification buildGroupChatWithMissingGroupNameNotificationFromSecondSender() {
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_FULL_MESSAGE, TITLE_SENDER_2, CHAT_ID, TICKER_TEXT_SENDER_2_AND_SHORT_MESSAGE);
    }

    private StatusBarNotification buildNewMessageNotification() {
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_NEW_MESSAGE, TITLE_SENDER, CHAT_ID, TICKER_TEXT_SENDER_AND_NEW_MESSAGE);
    }

    private StatusBarNotification buildOneOnOneMessageNotification() {
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_FULL_MESSAGE, TITLE_SENDER, CHAT_ID, TICKER_TEXT_SENDER_AND_SHORT_MESSAGE);
    }

    private StatusBarNotification buildIncomingCallNotification() {
        // android.bigText=LINE語音通話來電中…
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_INCOMING_CALL, TITLE_SENDER, CHAT_ID_NULL, TICKER_TEXT_SENDER_AND_INCOMING_CALL_MESSAGE);
    }

    private StatusBarNotification buildOngoingCallNotification() {
        // android.bigText=LINE通話中
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_ONGOING_CALL, TITLE_SENDER, CHAT_ID_NULL, TICKER_TEXT_SENDER_AND_ONGOING_CALL_MESSAGE);
    }

    private StatusBarNotification buildMissedCallNotification() {
        // android.bigText=蕭文鈺
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_SENDER, TITLE_MISSED_CALL, CHAT_ID_NULL, TICKER_TEXT_MISSED_CALL_AND_SENDER);
    }

    private StatusBarNotification buildIncomingVideoCallNotification() {
        // android.bigText=LINE視訊通話來電中…
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_INCOMING_VIDEO_CALL, TITLE_SENDER, CHAT_ID_NULL, TICKER_TEXT_SENDER_AND_INCOMING_VIDEO_CALL_MESSAGE);
    }

    private StatusBarNotification buildJoinedGroupChatNotification() {
        // android.bigText=Sender已加入「GroupName」。
        return buildNotification(CONVERSATION_TITLE_NULL, TEXT_INDIVIDUAL_JOINED_GROUP_CHAT, TITLE_LINE, CHAT_ID_NULL, TICKER_TEXT_INDIVIDUAL_JOINED_GROUP_CHAT);
    }

    private StatusBarNotification buildGroupMessageWithStickerNotification() {
        // android.bigText=Sender已加入「GroupName」。
        return buildNotification(CONVERSATION_TITLE_GROUP_NAME, TEXT_SENDER_WITH_STICKER, TITLE_GROUPNAME_AND_SENDER, CHAT_ID, TICKER_TEXT_SENDER_WITH_STICKER);
    }

    private StatusBarNotification buildNotification(final String conversationTitle,
                                                    final String androidText,
                                                    final String androidTitle,
                                                    final String lineChatId,
                                                    final String tickerText) {
        return new StatusBarNotificationBuilder()
                .withAndroidText(androidText)
                .withAndroidConversationTitle(conversationTitle)
                .withAndroidTitle(androidTitle)
                .withLineChatId(lineChatId)
                .withTickerText(tickerText)
                .build();
    }

}