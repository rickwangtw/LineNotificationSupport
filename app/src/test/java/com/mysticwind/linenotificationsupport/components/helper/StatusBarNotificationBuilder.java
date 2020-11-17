package com.mysticwind.linenotificationsupport.components.helper;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;

import java.util.Arrays;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class StatusBarNotificationBuilder {

    private final StatusBarNotification mockedStatusBarNotification;
    private final Notification mockedNotification;
    private final Bundle mockedExtras;

    public StatusBarNotificationBuilder() {
        this.mockedStatusBarNotification = mock(StatusBarNotification.class);
        this.mockedNotification = mock(Notification.class);
        this.mockedExtras = mock(Bundle.class);

        when(mockedStatusBarNotification.getNotification()).thenReturn(mockedNotification);
        mockedNotification.extras = mockedExtras;
    }

    public StatusBarNotificationBuilder withAndroidText(String androidText) {
        when(mockedExtras.getString("android.text")).thenReturn(androidText);
        return this;
    }

    public StatusBarNotificationBuilder withAndroidTitle(String androidTitle) {
        when(mockedExtras.getString("android.title")).thenReturn(androidTitle);
        return this;
    }

    public StatusBarNotificationBuilder withAndroidConversationTitle(String androidConversationTitle) {
        when(mockedExtras.getString("android.conversationTitle")).thenReturn(androidConversationTitle);
        return this;
    }

    public StatusBarNotificationBuilder withHiddenConversationTitle(String hiddenConversationTitle) {
        when(mockedExtras.getString("android.hiddenConversationTitle")).thenReturn(hiddenConversationTitle);
        return this;
    }

    public StatusBarNotificationBuilder withLineChatId(String lineChatId) {
        when(mockedExtras.getString("line.chat.id")).thenReturn(lineChatId);
        return this;
    }

    public StatusBarNotificationBuilder withLineMessageId(String lineMessageId) {
        when(mockedExtras.getString("line.message.id")).thenReturn(lineMessageId);
        return this;
    }

    public StatusBarNotificationBuilder withCategory(String category) {
        mockedNotification.category = category;
        return this;
    }

    public StatusBarNotificationBuilder withTag(String tag) {
        when(mockedStatusBarNotification.getTag()).thenReturn(tag);
        return this;
    }

    public StatusBarNotificationBuilder withActions(Notification.Action... actions) {
        mockedNotification.actions =
                Arrays.asList(actions).toArray(new Notification.Action[actions.length]);
        return this;
    }

    public StatusBarNotificationBuilder withChannelId(String channelId) {
        when(mockedNotification.getChannelId()).thenReturn(channelId);
        return this;
    }

    public StatusBarNotificationBuilder withTickerText(String tickerText) {
        mockedNotification.tickerText = tickerText;
        return this;
    }

    public StatusBarNotificationBuilder withWhen(long when) {
        mockedNotification.when = when;
        when(mockedStatusBarNotification.getPostTime()).thenReturn(when);
        return this;
    }

    public StatusBarNotification build() {
        return mockedStatusBarNotification;
    }

}
