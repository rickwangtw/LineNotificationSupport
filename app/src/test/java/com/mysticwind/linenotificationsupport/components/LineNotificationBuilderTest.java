package com.mysticwind.linenotificationsupport.components;

import android.content.Context;
import android.service.notification.StatusBarNotification;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mysticwind.linenotificationsupport.chatname.ChatNameManager;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.CachingGroupChatNameDataAccessorDecorator;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.CachingMultiPersonChatNameDataAccessorDecorator;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.GroupChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.chatname.dataaccessor.MultiPersonChatNameDataAccessor;
import com.mysticwind.linenotificationsupport.components.helper.StatusBarNotificationBuilder;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;
import com.mysticwind.linenotificationsupport.utils.ChatTitleAndSenderResolver;
import com.mysticwind.linenotificationsupport.utils.StatusBarNotificationPrinter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class LineNotificationBuilderTest {

    private GroupChatNameDataAccessor groupChatNameDataAccessor = new CachingGroupChatNameDataAccessorDecorator(
            new GroupChatNameDataAccessor() {
                private final Map<String, String> chatIdToChatGroupNameMap = new HashMap<>();

                @Override
                public void persistRelationship(String chatId, String chatGroupName) {
                    chatIdToChatGroupNameMap.put(chatId, chatGroupName);
                }

                @Override
                public Optional<String> getChatGroupName(String chatId) {
                    return Optional.ofNullable(chatIdToChatGroupNameMap.get(chatId));
                }

                @Override
                public Map<String, String> getAllChatGroups() {
                    return chatIdToChatGroupNameMap;
                }
            });

    private MultiPersonChatNameDataAccessor multiPersonChatNameDataAccessor = new CachingMultiPersonChatNameDataAccessorDecorator(
            new MultiPersonChatNameDataAccessor() {
                Multimap<String, String> chatIdToSenderMultimap = HashMultimap.create();
                @Override
                public String addRelationshipAndGetChatGroupName(String chatId, String sender) {
                    chatIdToSenderMultimap.put(chatId, sender);
                    return sender;
                }

                @Override
                public Multimap<String, String> getAllChatIdToSenders() {
                    return chatIdToSenderMultimap;
                }
            });

    private ChatNameManager chatNameManager = new ChatNameManager(groupChatNameDataAccessor, multiPersonChatNameDataAccessor);
    private ChatTitleAndSenderResolver chatTitleAndSenderResolver = new ChatTitleAndSenderResolver(chatNameManager);

    @Mock
    private Context mockedContext;

    @Mock
    private StatusBarNotificationPrinter mockedStatusBarNotificationPrinter;

    private LineNotificationBuilder classUnderTest;

    @Before
    public void setUp() {
        classUnderTest = new LineNotificationBuilder(mockedContext, chatTitleAndSenderResolver, mockedStatusBarNotificationPrinter);
    }

    @Test
    public void testVideoMessage() {
        StatusBarNotification statusBarNotification = new StatusBarNotificationBuilder()
                .withTag("NOTIFICATION_TAG_MESSAGE")
                .withCategory("msg")
                .withAndroidTitle("GroupName：\uD83C\uDF1ESenderName")
                .withAndroidConversationTitle("GroupName")
                .withHiddenConversationTitle("GroupName")
                .withLineChatId("cffc56f3a90a8fef933ade4213fe2286f")
                .withLineMessageId("13044851534123")
                .withAndroidText("\uD83C\uDF1ESenderName傳送了影片")
                .withChannelId("jp.naver.line.android.notification.NewMessages")
                .withTickerText("\uD83C\uDF1ESenderName傳送了影片")
                .withWhen(1605586932474L)
                .build();

        LineNotification lineNotification = classUnderTest.from(statusBarNotification);

        System.out.println("LineNotification: " + lineNotification);

        assertEquals("\uD83C\uDF1ESenderName傳送了影片", lineNotification.getMessage());
        assertEquals("\uD83C\uDF1ESenderName", lineNotification.getSender().getName());
        assertEquals("GroupName", lineNotification.getTitle());
        assertNull(lineNotification.getCallState());
        assertEquals("cffc56f3a90a8fef933ade4213fe2286f", lineNotification.getChatId());
        assertEquals(1605586932474L, lineNotification.getTimestamp());
    }

}