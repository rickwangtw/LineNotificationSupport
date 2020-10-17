package com.mysticwind.linenotificationsupport.service;

import android.app.Notification;
import android.content.Intent;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.Person;

import com.google.common.base.MoreObjects;
import com.mysticwind.linenotificationsupport.MainActivity;
import com.mysticwind.linenotificationsupport.R;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class NotificationListenerService
        extends android.service.notification.NotificationListenerService {

    private static final String TAG = "LINE_NOTIFICATION_SUPPORT";
    private static final int NOTIFICATION_ID = 0x20;

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        // ignore messages from ourselves
        if (statusBarNotification.getPackageName().startsWith(getPackageName())) {
            return;
        }

        final String packageName = statusBarNotification.getPackageName();

        // let's just focus on Line notifications for now
        if (!packageName.equals("jp.naver.line.android")) {
            return;
        }

        // ignore summaries
        if (isSummary(statusBarNotification)) {
            return;
        }

        final String stringifiedNotification = MoreObjects.toStringHelper(statusBarNotification)
                .add("packageName", statusBarNotification.getPackageName())
                .add("groupKey", statusBarNotification.getGroupKey())
                .add("key", statusBarNotification.getKey())
                .add("id", statusBarNotification.getId())
                .add("tag", statusBarNotification.getTag())
                .add("user", statusBarNotification.getUser().toString())
                .add("overrideGroupKey", statusBarNotification.getOverrideGroupKey())
                .add("notification", ToStringBuilder.reflectionToString(statusBarNotification.getNotification()))
                .toString();
        Log.i(TAG, String.format("Notification (%s): %s",
                statusBarNotification.getPackageName(),
                stringifiedNotification)
        );

        resendNotification(statusBarNotification);
    }

    private boolean isSummary(final StatusBarNotification statusBarNotification) {
        final String summaryText = statusBarNotification.getNotification().extras
                .getString("android.summaryText");
        return StringUtils.isNotBlank(summaryText);
    }

    private void resendNotification(StatusBarNotification statusBarNotification) {
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        final Notification notification = buildNotification(statusBarNotification);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    private Notification buildNotification(StatusBarNotification statusBarNotification) {
        final String chatId = buildChatId(statusBarNotification);
        final String sender = statusBarNotification.getNotification().extras
                .getString("android.title");
        final String myName = statusBarNotification.getNotification().extras
                .getString("android.selfDisplayName");
        final String message = statusBarNotification.getNotification().extras
                .getString("android.text");
        final long timestamp = statusBarNotification.getPostTime();

        final NotificationCompat.MessagingStyle messageStyle = new NotificationCompat.MessagingStyle(new Person.Builder().setName(myName).build())
                .setConversationTitle(chatId)
                .addMessage(message, timestamp, sender);

        final Notification notification = new NotificationCompat.Builder(getApplicationContext(), MainActivity.CHANNEL_ID)
                .setStyle(messageStyle)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setGroup(getChatId(statusBarNotification))
                .build();

        return notification;
    }

    private String buildChatId(StatusBarNotification statusBarNotification) {
        // chat groups will have a conversationTitle (but not groups of people)
        final String conversationTitle = statusBarNotification.getNotification().extras
                .getString("android.conversationTitle");

        if (StringUtils.isNotBlank(conversationTitle)) {
            return conversationTitle;
        } else {
            return getChatId(statusBarNotification);
        }
    }

    private String getChatId(StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("line.chat.id");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }
}
