package com.mysticwind.linenotificationsupport.service;

import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.core.app.NotificationManagerCompat;

import com.google.common.base.MoreObjects;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;
import com.mysticwind.linenotificationsupport.utils.ChatTitleAndSenderResolver;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.utils.ImageNotificationPublisherAsyncTask;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.mutable.MutableInt;

public class NotificationListenerService
        extends android.service.notification.NotificationListenerService {

    private static final String TAG = "LINE_NOTIFICATION_SUPPORT";

    private static final GroupIdResolver GROUP_ID_RESOLVER = new GroupIdResolver();
    private static final NotificationIdGenerator NOTIFICATION_ID_GENERATOR = new NotificationIdGenerator();
    private static final ChatTitleAndSenderResolver CHAT_TITLE_AND_SENDER_RESOLVER = new ChatTitleAndSenderResolver();

    private final MutableInt incomingCallNotificationId = new MutableInt(0);
    private final Handler handler = new Handler();

    private LineNotification incomingCallLineNotification;

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

        sendNotification(statusBarNotification);
    }

    private boolean isSummary(final StatusBarNotification statusBarNotification) {
        final String summaryText = statusBarNotification.getNotification().extras
                .getString("android.summaryText");
        return StringUtils.isNotBlank(summaryText);
    }

    private void sendNotification(StatusBarNotification notificationFromLine) {
        final LineNotification lineNotification = new LineNotificationBuilder(this,
                CHAT_TITLE_AND_SENDER_RESOLVER).from(notificationFromLine);

        int notificationId = NOTIFICATION_ID_GENERATOR.getNextNotificationId();
        new ImageNotificationPublisherAsyncTask(this, lineNotification,
                notificationId, GROUP_ID_RESOLVER).execute();

        if (lineNotification.getCallState() == LineNotification.CallState.INCOMING) {
            incomingCallLineNotification = lineNotification;
            sendIncomingCallNotification();
        } else if (lineNotification.getCallState() == LineNotification.CallState.MISSED_CALL ||
                lineNotification.getCallState() == LineNotification.CallState.IN_A_CALL) {
            // clear the incoming call notification so that we stop sending more messages
            incomingCallLineNotification = null;
            cancelIncomingCallNotification();
            incomingCallNotificationId.setValue(0);
        }
    }

    private void sendIncomingCallNotification() {
        try {
            if (incomingCallLineNotification == null) {
                return;
            }

            // cancel the old one
            cancelIncomingCallNotification();

            // resend a new one
            incomingCallNotificationId.setValue(NOTIFICATION_ID_GENERATOR.getNextNotificationId());

            new ImageNotificationPublisherAsyncTask(NotificationListenerService.this, incomingCallLineNotification,
                    incomingCallNotificationId.getValue(), GROUP_ID_RESOLVER).execute();
        } catch (Exception e) {
            Log.e(TAG, "Failed to send incoming call notifications: " + e.getMessage(), e);
        }
        if (incomingCallLineNotification != null) {
            scheduleNextIncomingCallNotification();
        }
    }

    private void cancelIncomingCallNotification() {
        if (incomingCallNotificationId.getValue() != 0) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(NotificationListenerService.this);
            notificationManager.cancel(incomingCallNotificationId.getValue().intValue());
        }
    }

    private void scheduleNextIncomingCallNotification() {
        handler.postDelayed(new Runnable() {
            public void run() {
                sendIncomingCallNotification();
            }
        }, 1_500);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }
}
