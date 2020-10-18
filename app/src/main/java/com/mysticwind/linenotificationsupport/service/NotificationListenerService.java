package com.mysticwind.linenotificationsupport.service;

import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.google.common.base.MoreObjects;
import com.mysticwind.linenotificationsupport.utils.GroupIdResolver;
import com.mysticwind.linenotificationsupport.model.LineNotification;
import com.mysticwind.linenotificationsupport.model.LineNotificationBuilder;
import com.mysticwind.linenotificationsupport.utils.NotificationIdGenerator;
import com.mysticwind.linenotificationsupport.utils.ImageNotificationPublisherAsyncTask;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.List;

import static androidx.core.app.NotificationCompat.EXTRA_TEXT;

public class NotificationListenerService
        extends android.service.notification.NotificationListenerService {

    private static final String TAG = "LINE_NOTIFICATION_SUPPORT";

    private static final GroupIdResolver GROUP_ID_RESOLVER = new GroupIdResolver();
    private static final NotificationIdGenerator NOTIFICATION_ID_GENERATOR = new NotificationIdGenerator();

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
        final LineNotification lineNotification = new LineNotificationBuilder(this).from(notificationFromLine);
        final int groupId = GROUP_ID_RESOLVER.resolveGroupId(lineNotification.getChatId());

        List<CharSequence> currentNotificationMessages = new ArrayList<>();
        currentNotificationMessages.add(notificationFromLine.getNotification().extras.getCharSequence(EXTRA_TEXT));
        currentNotificationMessages.add(previousNotification(lineNotification));

        boolean shouldShowGroupNotification = currentNotificationMessages.size() == 2 ? true : false;

        new ImageNotificationPublisherAsyncTask(this, lineNotification,
                shouldShowGroupNotification, currentNotificationMessages,
                NOTIFICATION_ID_GENERATOR.getNextNotificationId(),
                groupId).execute();
    }

    private CharSequence previousNotification(LineNotification lineNotification) {
        if (StringUtils.isBlank(lineNotification.getChatId())) {
            return null;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return null;
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        final int groupId = GROUP_ID_RESOLVER.resolveGroupId(lineNotification.getChatId());
        for (final StatusBarNotification statusBarNotification : notificationManager.getActiveNotifications()) {
            if (statusBarNotification.getId() != groupId &&
                    lineNotification.getChatId().equalsIgnoreCase(statusBarNotification.getNotification().getGroup())) {
                return statusBarNotification.getNotification().extras.getCharSequence(EXTRA_TEXT);
            }
        }
        return null;
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
    }
}
