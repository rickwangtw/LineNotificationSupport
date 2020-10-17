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

    private Notification buildNotification(final StatusBarNotification statusBarNotification) {
        // individual: android.title is the sender
        // group chat: android.title is "group title：sender", android.conversationTitle is group title
        // chat with multi-folks: android.title is also the sender, no way to differentiate between individual and multi-folks :(

        final String title;
        final String sender;
        if (isChatGroup(statusBarNotification)) {
            title = getGroupChatTitle(statusBarNotification);
            final String androidTitle = getAndroidTitle(statusBarNotification);
            sender = androidTitle.replace(title + "：", "");
        } else {
            title = getAndroidTitle(statusBarNotification);
            sender = getAndroidTitle(statusBarNotification);
        }


        final String chatId = getChatId(statusBarNotification);
        final String message = statusBarNotification.getNotification().extras
                .getString("android.text");
        final String myName = statusBarNotification.getNotification().extras
                .getString("android.selfDisplayName");
        final long timestamp = statusBarNotification.getPostTime();

        final NotificationCompat.MessagingStyle messageStyle = new NotificationCompat.MessagingStyle(new Person.Builder().setName(sender).build())
                .setConversationTitle(title)
                .addMessage(message, timestamp, sender);

        final Notification notification = new NotificationCompat.Builder(getApplicationContext(), MainActivity.CHANNEL_ID)
                .setStyle(messageStyle)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setGroup(chatId)
                .build();

        return notification;
    }

    private boolean isChatGroup(final StatusBarNotification statusBarNotification) {
        final String title = statusBarNotification.getNotification().extras.getString("android.conversationTitle");
        return StringUtils.isNotBlank(title);
    }

    private String getGroupChatTitle(final StatusBarNotification statusBarNotification) {
        // chat groups will have a conversationTitle (but not groups of people)
        return statusBarNotification.getNotification().extras.getString("android.conversationTitle");
    }

    private String getAndroidTitle(final StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("android.title");
    }


    private String getChatId(final StatusBarNotification statusBarNotification) {
        return statusBarNotification.getNotification().extras.getString("line.chat.id");
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }
}
