package com.mysticwind.linenotificationsupport.components;

import android.content.Context;
import android.service.notification.StatusBarNotification;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class LineNotificationBuilderTest {

    private ChatTitleAndSenderResolver chatTitleAndSenderResolver = new ChatTitleAndSenderResolver();

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